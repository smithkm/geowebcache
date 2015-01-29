package org.geowebcache.diskquota;

import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.easymock.classextension.EasyMock;
import org.geowebcache.config.Configuration;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.config.XMLConfigurationBackwardsCompatibilityTest;
import org.geowebcache.diskquota.bdb.BDBQuotaStore;
import org.geowebcache.diskquota.storage.PageStats;
import org.geowebcache.diskquota.storage.PageStatsPayload;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.diskquota.storage.StorageUnit;
import org.geowebcache.diskquota.storage.SystemUtils;
import org.geowebcache.diskquota.storage.TilePage;
import org.geowebcache.diskquota.storage.TilePageCalculator;
import org.geowebcache.diskquota.storage.TileSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.storage.blobstore.file.FilePathGenerator;
import org.hamcrest.Matcher;
import org.junit.Assert;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class BDBQuotaStoreTest extends TestCase {

    private BDBQuotaStore store;

    private TilePageCalculator tilePageCalculator;

    private TileSet testTileSet;

    TileLayerDispatcher layerDispatcher;

    DefaultStorageFinder cacheDirFinder;

    File targetDir;

    @Override
    public void setUp() throws Exception {
        targetDir = new File("target", "mockStore" + Math.random());
        FileUtils.deleteDirectory(targetDir);
        targetDir.mkdirs();

        cacheDirFinder = EasyMock.createMock(DefaultStorageFinder.class);
        EasyMock.expect(cacheDirFinder.getDefaultPath()).andReturn(targetDir.getAbsolutePath())
                .anyTimes();
        EasyMock.expect(
                cacheDirFinder.findEnvVar(EasyMock.eq(DiskQuotaMonitor.GWC_DISKQUOTA_DISABLED)))
                .andReturn(null).anyTimes();
        EasyMock.replay(cacheDirFinder);

        XMLConfiguration xmlConfig = loadXMLConfig();
        LinkedList<Configuration> configList = new LinkedList<Configuration>();
        configList.add(xmlConfig);

        layerDispatcher = new TileLayerDispatcher(new GridSetBroker(true, true), configList);

        tilePageCalculator = new TilePageCalculator(layerDispatcher);

        store = new BDBQuotaStore(cacheDirFinder, tilePageCalculator);
        store.startUp();
        testTileSet = tilePageCalculator.getTileSetsFor("topp:states2").iterator().next();
    }

    public void tearDown() {
        try {
            store.close();
            FileUtils.deleteDirectory(targetDir);
        } catch (Exception e) {
        }
    }

    private XMLConfiguration loadXMLConfig() {
        InputStream is = XMLConfiguration.class
                .getResourceAsStream(XMLConfigurationBackwardsCompatibilityTest.LATEST_FILENAME);
        XMLConfiguration xmlConfig = null;
        try {
            xmlConfig = new XMLConfiguration(is);
        } catch (Exception e) {
            // Do nothing
        }

        return xmlConfig;
    }

    public void testInitialization() throws Exception {
        Set<TileSet> tileSets = store.getTileSets();
        assertNotNull(tileSets);
        assertEquals(10, tileSets.size());

        TileSet tileSet = new TileSet("topp:states", "EPSG:900913", "image/png", null);
        assertTrue(tileSets.contains(tileSet));

        tileSet = new TileSet("topp:states", "EPSG:900913", "image/jpeg", null);
        assertTrue(tileSets.contains(tileSet));

        tileSet = new TileSet("topp:states", "EPSG:900913", "image/gif", null);
        assertTrue(tileSets.contains(tileSet));

        tileSet = new TileSet("topp:states", "EPSG:900913", "application/vnd.google-earth.kml+xml",
                null);
        assertTrue(tileSets.contains(tileSet));

        tileSet = new TileSet("topp:states", "EPSG:4326", "image/png", null);
        assertTrue(tileSets.contains(tileSet));

        tileSet = new TileSet("topp:states", "EPSG:4326", "image/jpeg", null);
        assertTrue(tileSets.contains(tileSet));

        tileSet = new TileSet("topp:states", "EPSG:4326", "image/gif", null);
        assertTrue(tileSets.contains(tileSet));

        tileSet = new TileSet("topp:states", "EPSG:4326", "application/vnd.google-earth.kml+xml",
                null);
        assertTrue(tileSets.contains(tileSet));

        tileSet = new TileSet("topp:states2", "EPSG:2163", "image/png", null);
        assertTrue(tileSets.contains(tileSet));

        tileSet = new TileSet("topp:states2", "EPSG:2163", "image/jpeg", null);
        assertTrue(tileSets.contains(tileSet));

        // remove one layer from the dispatcher
        Configuration configuration = layerDispatcher.removeLayer("topp:states");
        configuration.save();
        // and make sure at the next startup the store catches up (note this behaviour is just a
        // startup consistency check in case the store got out of sync for some reason. On normal
        // situations the store should have been notified through store.deleteLayer(layerName) if
        // the layer was removed programmatically through StorageBroker.deleteLayer
        store.close();
        store.startUp();

        tileSets = store.getTileSets();
        assertNotNull(tileSets);
        assertEquals(2, tileSets.size());
        tileSet = new TileSet("topp:states2", "EPSG:2163", "image/png", null);
        assertTrue(tileSets.contains(tileSet));

        tileSet = new TileSet("topp:states2", "EPSG:2163", "image/jpeg", null);
        assertTrue(tileSets.contains(tileSet));
    }

    /**
     * Combined test for {@link BDBQuotaStore#addToQuotaAndTileCounts(TileSet, Quota, Collection)}
     * and {@link BDBQuotaStore#addHitsAndSetAccesTime(Collection)}
     * 
     * @throws Exception
     */
    public void testPageStatsGathering() throws Exception {
        final MockSystemUtils sysUtils = new MockSystemUtils();
        sysUtils.setCurrentTimeMinutes(10);
        sysUtils.setCurrentTimeMillis(10 * 60 * 1000);
        SystemUtils.set(sysUtils);

        TileSet tileSet = testTileSet;

        TilePage page = new TilePage(tileSet.getId(), 0, 0, (byte) 0);

        PageStatsPayload payload = new PageStatsPayload(page);
        int numHits = 100;
        payload.setLastAccessTime(sysUtils.currentTimeMillis() - 1 * 60 * 1000);
        payload.setNumHits(numHits);
        payload.setNumTiles(1);

        store.addToQuotaAndTileCounts(tileSet, new Quota(1, StorageUnit.MiB),
                Collections.singleton(payload));

        Future<List<PageStats>> result = store.addHitsAndSetAccesTime(Collections
                .singleton(payload));
        List<PageStats> allStats = result.get();
        PageStats stats = allStats.get(0);
        float fillFactor = stats.getFillFactor();
        assertEquals(1.0f, fillFactor, 1e-6);

        int lastAccessTimeMinutes = stats.getLastAccessTimeMinutes();
        assertEquals(sysUtils.currentTimeMinutes(), lastAccessTimeMinutes);

        float frequencyOfUsePerMinute = stats.getFrequencyOfUsePerMinute();
        assertEquals(100f, frequencyOfUsePerMinute);

        // now 1 minute later...
        sysUtils.setCurrentTimeMinutes(sysUtils.currentTimeMinutes() + 2);
        sysUtils.setCurrentTimeMillis(sysUtils.currentTimeMillis() + 2 * 60 * 1000);

        numHits = 10;
        payload.setLastAccessTime(sysUtils.currentTimeMillis() - 1 * 60 * 1000);
        payload.setNumHits(numHits);

        result = store.addHitsAndSetAccesTime(Collections.singleton(payload));
        allStats = result.get();
        stats = allStats.get(0);

        lastAccessTimeMinutes = stats.getLastAccessTimeMinutes();
        assertEquals(11, lastAccessTimeMinutes);

        frequencyOfUsePerMinute = stats.getFrequencyOfUsePerMinute();
        float expected = 55.0f;// the 100 previous + the 10 added now / the 2 minutes that elapsed
        assertEquals(expected, frequencyOfUsePerMinute, 1e-6f);
    }

    public void testGetGloballyUsedQuota() throws InterruptedException {
        Quota usedQuota = store.getGloballyUsedQuota();
        assertNotNull(usedQuota);
        assertEquals(0, usedQuota.getBytes().intValue());

        String layerName = tilePageCalculator.getLayerNames().iterator().next();
        TileSet tileSet = tilePageCalculator.getTileSetsFor(layerName).iterator().next();

        final String tileSetId = tileSet.getId();

        Quota quotaDiff = new Quota(BigInteger.valueOf(1000));
        Collection<PageStatsPayload> tileCountDiffs = Collections.emptySet();
        store.addToQuotaAndTileCounts(tileSet, quotaDiff, tileCountDiffs);

        usedQuota = store.getGloballyUsedQuota();
        assertNotNull(usedQuota);
        assertEquals(1000, usedQuota.getBytes().intValue());

        quotaDiff = new Quota(BigInteger.valueOf(-500));
        store.addToQuotaAndTileCounts(tileSet, quotaDiff, tileCountDiffs);

        usedQuota = store.getGloballyUsedQuota();
        assertNotNull(usedQuota);
        assertEquals(500, usedQuota.getBytes().intValue());
    }

    public void testDeleteLayer() throws InterruptedException {
        String layerName = tilePageCalculator.getLayerNames().iterator().next();
        // make sure the layer is there and has stuff
        Quota usedQuota = store.getUsedQuotaByLayerName(layerName);
        assertNotNull(usedQuota);

        TileSet tileSet = tilePageCalculator.getTileSetsFor(layerName).iterator().next();
        TilePage page = new TilePage(tileSet.getId(), 0, 0, (byte) 0);
        store.addHitsAndSetAccesTime(Collections.singleton(new PageStatsPayload(page)));
        // page.setNumHits(10);
        // page.setLastAccessTime(System.currentTimeMillis());
        // store.addHitsAndSetAccesTime(page);

        assertNotNull(store.getTileSetById(tileSet.getId()));

        store.deleteLayer(layerName);

        // cascade deleted?
        assertNull(store.getLeastRecentlyUsedPage(Collections.singleton(layerName)));
        usedQuota = store.getUsedQuotaByLayerName(layerName);
        assertNotNull(usedQuota);
        assertEquals(0L, usedQuota.getBytes().longValue());
    }
    
    
    public void testDeleteTileSet() throws InterruptedException {
        // put some data into the layers
        String paramHash1 = FilePathGenerator.getParametersId(Collections.singletonMap("foo", "bar"));
        String paramHash2 = FilePathGenerator.getParametersId(Collections.singletonMap("foo", "baz"));
        String layerName1 = "topp:states";
        String layerName2 = "topp:states2";
        String gridsetId1 = "EPSG:4326";
        String gridsetId2 = "EPSG:2163";
        TileSet tset1p1 = new TileSet(layerName1, gridsetId1, "image/png", paramHash1);
        TileSet tset1p2 = new TileSet(layerName1, gridsetId1, "image/png", paramHash2);
        TileSet tset2p1 = new TileSet(layerName2, gridsetId2, "image/jpeg", paramHash1);
        TileSet tset2p2 = new TileSet(layerName2, gridsetId2, "image/jpeg", paramHash2);
        
        addToQuotaStore(tset1p1, 1);
        addToQuotaStore(tset1p2, 2);
        addToQuotaStore(tset2p1, 3);
        addToQuotaStore(tset2p2, 4);
       
        Quota tset1p1Quota = store.getUsedQuotaByTileSetId(tset1p1.getId());
        Quota tset1p2Quota = store.getUsedQuotaByTileSetId(tset1p2.getId());
        Quota tset2p1Quota = store.getUsedQuotaByTileSetId(tset2p1.getId());
        Quota tset2p2Quota = store.getUsedQuotaByTileSetId(tset2p2.getId());
        Quota globalQuota = store.getGloballyUsedQuota();
        Quota sum = new Quota();
        sum.add(tset1p1Quota);
        sum.add(tset1p2Quota);
        sum.add(tset2p1Quota);
        sum.add(tset2p2Quota);
        assertEquals(globalQuota.getBytes(), sum.getBytes());
        
        assertThat(tset1p1Quota.getBytes(), equalTo(StorageUnit.MiB.toBytes(1)));
        assertThat(tset1p2Quota.getBytes(), equalTo(StorageUnit.MiB.toBytes(2)));
        assertThat(tset2p1Quota.getBytes(), equalTo(StorageUnit.MiB.toBytes(3)));
        assertThat(tset2p2Quota.getBytes(), equalTo(StorageUnit.MiB.toBytes(4)));
        
        store.deleteTileSet(tset1p1.getLayerName(), tset1p1.getGridsetId(), tset1p1.getBlobFormat(), tset1p1.getParametersId());
        
        Quota tset1p1QuotaPost = store.getUsedQuotaByTileSetId(tset1p1.getId());
        Quota tset1p2QuotaPost = store.getUsedQuotaByTileSetId(tset1p2.getId());
        Quota tset2p1QuotaPost = store.getUsedQuotaByTileSetId(tset2p1.getId());
        Quota tset2p2QuotaPost = store.getUsedQuotaByTileSetId(tset2p2.getId());
        Quota globalQuotaPost = store.getGloballyUsedQuota();
        Quota sumPost = new Quota();
        sumPost.add(tset1p2QuotaPost);
        sumPost.add(tset2p1QuotaPost);
        sumPost.add(tset2p2QuotaPost);

        assertThat(tset1p1QuotaPost, anyOf(nullValue(), hasProperty("bytes", equalTo(BigInteger.ZERO))));
        assertEquals(globalQuotaPost.getBytes(), globalQuota.getBytes().subtract(tset1p1Quota.getBytes()));
        assertEquals(globalQuotaPost.getBytes(), sumPost.getBytes());
        assertEquals(tset1p2Quota.getBytes(), tset1p2Quota.getBytes());
        assertEquals(tset2p1Quota.getBytes(), tset2p1Quota.getBytes());
        assertEquals(tset2p2Quota.getBytes(), tset2p2Quota.getBytes());
    }
    public void testDeleteTileSetMultipleFormats() throws InterruptedException {
        // put some data into the layer
        String paramHash1 = FilePathGenerator.getParametersId(Collections.singletonMap("foo", "bar"));
        String paramHash2 = FilePathGenerator.getParametersId(Collections.singletonMap("foo", "baz"));
        String layerName1 = "topp:states";
        String gridsetId1 = "EPSG:4326";
        String gridsetId2 = "EPSG:900913";
        TileSet tset111 = new TileSet(layerName1, gridsetId1, "image/jpeg", paramHash1);
        TileSet tset121 = new TileSet(layerName1, gridsetId1, "image/jpeg", paramHash2);
        TileSet tset211 = new TileSet(layerName1, gridsetId1, "image/png", paramHash1);
        TileSet tset221 = new TileSet(layerName1, gridsetId1, "image/png", paramHash2);
        TileSet tset112 = new TileSet(layerName1, gridsetId2, "image/jpeg", paramHash1);
        TileSet tset122 = new TileSet(layerName1, gridsetId2, "image/jpeg", paramHash2);
        TileSet tset212 = new TileSet(layerName1, gridsetId2, "image/png", paramHash1);
        TileSet tset222 = new TileSet(layerName1, gridsetId2, "image/png", paramHash2);

        addToQuotaStore(tset111, 1);
        addToQuotaStore(tset121, 2);
        addToQuotaStore(tset211, 3);
        addToQuotaStore(tset221, 4);
        addToQuotaStore(tset112, 5);
        addToQuotaStore(tset122, 6);
        addToQuotaStore(tset212, 7);
        addToQuotaStore(tset222, 8);
        
        Quota expectFreed = new Quota();
        expectFreed.add(store.getUsedQuotaByTileSetId(tset111.getId()));
        expectFreed.add(store.getUsedQuotaByTileSetId(tset211.getId()));
        Quota globalQuota = store.getGloballyUsedQuota();
        Quota sum = new Quota();
        sum.add(store.getUsedQuotaByTileSetId(tset111.getId()));
        sum.add(store.getUsedQuotaByTileSetId(tset121.getId()));
        sum.add(store.getUsedQuotaByTileSetId(tset211.getId()));
        sum.add(store.getUsedQuotaByTileSetId(tset221.getId()));
        sum.add(store.getUsedQuotaByTileSetId(tset112.getId()));
        sum.add(store.getUsedQuotaByTileSetId(tset122.getId()));
        sum.add(store.getUsedQuotaByTileSetId(tset212.getId()));
        sum.add(store.getUsedQuotaByTileSetId(tset222.getId()));
        assertEquals(globalQuota.getBytes(), sum.getBytes());
        
        store.deleteTileSet(tset111.getLayerName(), tset111.getGridsetId(), null, tset111.getParametersId());
        
        Quota globalQuotaPost = store.getGloballyUsedQuota();

        assertThat(store.getUsedQuotaByTileSetId(tset111.getId()), anyOf(nullValue(), hasProperty("bytes", equalTo(BigInteger.ZERO))));
        assertThat(store.getUsedQuotaByTileSetId(tset121.getId()), hasProperty("bytes", equalTo(StorageUnit.MiB.toBytes(2))));
        assertThat(store.getUsedQuotaByTileSetId(tset211.getId()), anyOf(nullValue(), hasProperty("bytes", equalTo(BigInteger.ZERO))));
        assertThat(store.getUsedQuotaByTileSetId(tset221.getId()), hasProperty("bytes", equalTo(StorageUnit.MiB.toBytes(4))));
        assertThat(store.getUsedQuotaByTileSetId(tset112.getId()), hasProperty("bytes", equalTo(StorageUnit.MiB.toBytes(5))));
        assertThat(store.getUsedQuotaByTileSetId(tset122.getId()), hasProperty("bytes", equalTo(StorageUnit.MiB.toBytes(6))));
        assertThat(store.getUsedQuotaByTileSetId(tset212.getId()), hasProperty("bytes", equalTo(StorageUnit.MiB.toBytes(7))));
        assertThat(store.getUsedQuotaByTileSetId(tset222.getId()), hasProperty("bytes", equalTo(StorageUnit.MiB.toBytes(8))));

        assertEquals(globalQuotaPost.getBytes(), globalQuota.getBytes().subtract(expectFreed.getBytes()));
    }

    private void addToQuotaStore(TileSet tset, int size) throws InterruptedException {
        Quota quotaDiff = new Quota(size, StorageUnit.MiB);
        PageStatsPayload stats = new PageStatsPayload(new TilePage(tset.getId(), 0, 0, 3));
        stats.setNumTiles(10*size);
        store.addToQuotaAndTileCounts(tset, quotaDiff, Collections.singletonList(stats));
    }
    
    public void testRenameLayer() throws InterruptedException {
        final String oldLayerName = tilePageCalculator.getLayerNames().iterator().next();
        final String newLayerName = "renamed_layer";

        // make sure the layer is there and has stuff
        Quota usedQuota = store.getUsedQuotaByLayerName(oldLayerName);
        assertNotNull(usedQuota);

        TileSet tileSet = tilePageCalculator.getTileSetsFor(oldLayerName).iterator().next();
        TilePage page = new TilePage(tileSet.getId(), 0, 0, (byte) 0);
        store.addHitsAndSetAccesTime(Collections.singleton(new PageStatsPayload(page)));
        store.addToQuotaAndTileCounts(tileSet, new Quota(BigInteger.valueOf(1024)),
                Collections.EMPTY_LIST);

        Quota expectedQuota = store.getUsedQuotaByLayerName(oldLayerName);
        assertEquals(1024L, expectedQuota.getBytes().longValue());

        assertNotNull(store.getTileSetById(tileSet.getId()));

        store.renameLayer(oldLayerName, newLayerName);

        // cascade deleted old layer?
        assertNull(store.getLeastRecentlyUsedPage(Collections.singleton(oldLayerName)));
        usedQuota = store.getUsedQuotaByLayerName(oldLayerName);
        assertNotNull(usedQuota);
        assertEquals(0L, usedQuota.getBytes().longValue());

        // created new layer?
        Quota newLayerUsedQuota = store.getUsedQuotaByLayerName(newLayerName);
        assertEquals(expectedQuota.getBytes(), newLayerUsedQuota.getBytes());
    }

    public void testGetLeastFrequentlyUsedPage() throws Exception {
        final String layerName = testTileSet.getLayerName();
        Set<String> layerNames = Collections.singleton(layerName);

        TilePage lfuPage;
        lfuPage = store.getLeastFrequentlyUsedPage(layerNames);
        assertNull(lfuPage);

        TilePage page1 = new TilePage(testTileSet.getId(), 0, 1, 2);
        TilePage page2 = new TilePage(testTileSet.getId(), 1, 1, 2);

        PageStatsPayload payload1 = new PageStatsPayload(page1);
        PageStatsPayload payload2 = new PageStatsPayload(page2);

        payload1.setNumHits(100);
        payload2.setNumHits(10);
        Collection<PageStatsPayload> statsUpdates = Arrays.asList(payload1, payload2);
        store.addHitsAndSetAccesTime(statsUpdates).get();

        TilePage leastFrequentlyUsedPage = store.getLeastFrequentlyUsedPage(layerNames);
        assertEquals(page2, leastFrequentlyUsedPage);

        payload2.setNumHits(1000);
        store.addHitsAndSetAccesTime(statsUpdates).get();

        leastFrequentlyUsedPage = store.getLeastFrequentlyUsedPage(layerNames);
        assertEquals(page1, leastFrequentlyUsedPage);
    }

    public void testGetLeastRecentlyUsedPage() throws Exception {
        MockSystemUtils mockSystemUtils = new MockSystemUtils();
        mockSystemUtils.setCurrentTimeMinutes(1000);
        mockSystemUtils.setCurrentTimeMillis(mockSystemUtils.currentTimeMinutes() * 60 * 1000);
        SystemUtils.set(mockSystemUtils);

        final String layerName = testTileSet.getLayerName();
        Set<String> layerNames = Collections.singleton(layerName);

        TilePage leastRecentlyUsedPage;
        leastRecentlyUsedPage = store.getLeastRecentlyUsedPage(layerNames);
        assertNull(leastRecentlyUsedPage);

        TilePage page1 = new TilePage(testTileSet.getId(), 0, 1, 2);
        TilePage page2 = new TilePage(testTileSet.getId(), 1, 1, 2);

        PageStatsPayload payload1 = new PageStatsPayload(page1);
        PageStatsPayload payload2 = new PageStatsPayload(page2);

        payload1.setLastAccessTime(mockSystemUtils.currentTimeMillis() + 1 * 60 * 1000);
        payload2.setLastAccessTime(mockSystemUtils.currentTimeMillis() + 2 * 60 * 1000);

        Collection<PageStatsPayload> statsUpdates = Arrays.asList(payload1, payload2);
        store.addHitsAndSetAccesTime(statsUpdates).get();

        leastRecentlyUsedPage = store.getLeastRecentlyUsedPage(layerNames);
        assertEquals(page1, leastRecentlyUsedPage);

        payload1.setLastAccessTime(mockSystemUtils.currentTimeMillis() + 10 * 60 * 1000);
        store.addHitsAndSetAccesTime(statsUpdates).get();

        leastRecentlyUsedPage = store.getLeastRecentlyUsedPage(layerNames);
        assertEquals(page2, leastRecentlyUsedPage);
    }

    public void testGetTileSetById() throws Exception {

        TileSet tileSet = store.getTileSetById(testTileSet.getId());
        assertNotNull(tileSet);
        assertEquals(testTileSet, tileSet);

        try {
            store.getTileSetById("NonExistentTileSetId");
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    public void testGetTilesForPage() throws Exception {
        TilePage page = new TilePage(testTileSet.getId(), 0, 0, 0);

        long[][] expected = tilePageCalculator.toGridCoverage(testTileSet, page);
        long[][] tilesForPage = store.getTilesForPage(page);

        assertTrue(Arrays.equals(expected[0], tilesForPage[0]));

        page = new TilePage(testTileSet.getId(), 0, 0, 1);

        expected = tilePageCalculator.toGridCoverage(testTileSet, page);
        tilesForPage = store.getTilesForPage(page);

        assertTrue(Arrays.equals(expected[1], tilesForPage[1]));

    }

    @SuppressWarnings("unchecked")
    public void testGetUsedQuotaByLayerName() throws Exception {
        String layerName = "topp:states2";
        List<TileSet> tileSets;
        tileSets = new ArrayList<TileSet>(tilePageCalculator.getTileSetsFor(layerName));

        Quota expected = new Quota();
        for (TileSet tset : tileSets) {
            Quota quotaDiff = new Quota(10, StorageUnit.MiB);
            expected.add(quotaDiff);
            store.addToQuotaAndTileCounts(tset, quotaDiff, Collections.EMPTY_SET);
        }

        Quota usedQuotaByLayerName = store.getUsedQuotaByLayerName(layerName);
        assertEquals(expected.getBytes(), usedQuotaByLayerName.getBytes());
    }

    @SuppressWarnings("unchecked")
    public void testGetUsedQuotaByTileSetId() throws Exception {
        String layerName = "topp:states2";
        List<TileSet> tileSets;
        tileSets = new ArrayList<TileSet>(tilePageCalculator.getTileSetsFor(layerName));

        Map<String, Quota> expectedById = new HashMap<String, Quota>();

        for (TileSet tset : tileSets) {
            Quota quotaDiff = new Quota(10D * Math.random(), StorageUnit.MiB);
            store.addToQuotaAndTileCounts(tset, quotaDiff, Collections.EMPTY_SET);
            store.addToQuotaAndTileCounts(tset, quotaDiff, Collections.EMPTY_SET);
            Quota tsetQuota = new Quota(quotaDiff);
            tsetQuota.add(quotaDiff);
            expectedById.put(tset.getId(), tsetQuota);
        }

        for (Map.Entry<String, Quota> expected : expectedById.entrySet()) {
            BigInteger expectedValaue = expected.getValue().getBytes();
            String tsetId = expected.getKey();
            assertEquals(expectedValaue, store.getUsedQuotaByTileSetId(tsetId).getBytes());
        }
    }

    public void testSetTruncated() throws Exception {
        String tileSetId = testTileSet.getId();
        TilePage page = new TilePage(tileSetId, 0, 0, 2);

        PageStatsPayload payload = new PageStatsPayload(page);
        int numHits = 100;
        payload.setNumHits(numHits);
        payload.setNumTiles(5);

        store.addToQuotaAndTileCounts(testTileSet, new Quota(1, StorageUnit.MiB),
                Collections.singleton(payload));
        List<PageStats> stats = store.addHitsAndSetAccesTime(Collections.singleton(payload)).get();
        assertTrue(stats.get(0).getFillFactor() > 0f);
        PageStats pageStats = store.setTruncated(page);
        assertEquals(0f, pageStats.getFillFactor());
    }
    
    public void testCreatesVersion() throws Exception {
        File versionFile = new File(targetDir, "diskquota_page_store/version.txt");
        assertTrue(versionFile.exists());
    }

}
