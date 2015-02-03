/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Arne Kepp / The Open Planning Project 2009
 *  
 */
package org.geowebcache.storage.blobstore.file;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.io.IOUtils;
import org.easymock.classextension.EasyMock;
import org.geowebcache.config.Configuration;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.SRS;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.ImageMime;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.BlobStoreListener;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.blobstore.file.FileBlobStore;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class FileBlobStoreTest {
    @Rule 
    public TemporaryFolder cacheDir = new TemporaryFolder();;
    @Rule 
    public ExpectedException expectedException = ExpectedException.none();
    
    
    ExecutorService s;
    // A bit of a hack here
    void waitForPendingDeletes() throws InterruptedException, ExecutionException {
        Future<?> f = s.submit(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                
            }});
        f.get();
    }

    @Before
    public void setup() throws Exception {
        fbs = new FileBlobStore(cacheDir.getRoot().getAbsolutePath()){

            @Override
            ExecutorService createDeleteExecutorService() {
                s = super.createDeleteExecutorService();
                return s;
            }
            
        };
    }
    
    FileBlobStore fbs;
    
    @Test
    public void testTile() throws Exception {

        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = { 1L, 2L, 3L };
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        TileObject to = TileObject.createCompleteTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters, bytes);
        to.setId(11231231);

        fbs.put(to);

        TileObject to2 = TileObject.createQueryTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters);
        to2.setId(11231231);
        fbs.get(to2);

        assertThat(to2.getBlobFormat(), equalTo(to.getBlobFormat()));
        try(
            InputStream is = to.getBlob().getInputStream();
            InputStream is2 = to2.getBlob().getInputStream();
        ){
            assertTrue(IOUtils.contentEquals(is, is2));
        }
    }

    @Test
    public void testTileDelete() throws Exception {

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");

        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = { 5L, 6L, 7L };
        TileObject to = TileObject.createCompleteTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters, bytes);
        to.setId(11231231);

        fbs.put(to);

        TileObject to2 = TileObject.createQueryTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters);
        to2.setId(11231231);
        fbs.get(to2);

        try(
            InputStream is = to2.getBlob().getInputStream();
            InputStream is2 = bytes.getInputStream();
        ){
            assertTrue(IOUtils.contentEquals(is, is2));
        } 

        TileObject to3 = TileObject.createQueryTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters);
        fbs.delete(to3);

        TileObject to4 = TileObject.createQueryTileObject("test:123123 112", xyz, "EPSG:4326",
                "image/jpeg", parameters);
        assertFalse(fbs.get(to4));
    }

    @Test
    public void testTilRangeDelete() throws Exception {

        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        MimeType mime = ImageMime.png;
        SRS srs = SRS.getEPSG4326();
        String layerName = "test:123123 112";

        int zoomLevel = 7;
        int x = 25;
        int y = 6;

        // long[] origXYZ = {x,y,zoomLevel};

        TileObject[] tos = new TileObject[6];

        for (int i = 0; i < tos.length; i++) {
            long[] xyz = { x + i - 1, y, zoomLevel };
            tos[i] = TileObject.createCompleteTileObject(layerName, xyz, srs.toString(),
                    mime.getFormat(), parameters, bytes);
            fbs.put(tos[i]);
        }

        long[][] rangeBounds = new long[zoomLevel + 2][5];
        int zoomStart = zoomLevel - 1;
        int zoomStop = zoomLevel + 1;

        long[] range = { x, y, x + tos.length - 3, y, zoomLevel};
        rangeBounds[zoomLevel] = range;

        TileRange trObj = new TileRange(layerName, srs.toString(), zoomStart, zoomStop,
                rangeBounds, mime, parameters);

        fbs.delete(trObj);

        // starting x and x + tos.length should have data, the remaining should not
        TileObject firstTO = TileObject.createQueryTileObject(layerName, tos[0].getXYZ(),
                srs.toString(), mime.getFormat(), parameters);
        fbs.get(firstTO);
        try(
            InputStream is = firstTO.getBlob().getInputStream();
            InputStream is2 = bytes.getInputStream();
        ) {
            assertTrue(IOUtils.contentEquals(is, is2));
        }

        TileObject lastTO = TileObject.createQueryTileObject(layerName, tos[tos.length - 1].getXYZ(),
                srs.toString(), mime.getFormat(), parameters);
        fbs.get(lastTO);
        try(
            InputStream is = lastTO.getBlob().getInputStream();
            InputStream is2 = bytes.getInputStream();
        ){
            assertTrue(IOUtils.contentEquals(is, is2));
        }

        TileObject midTO = TileObject.createQueryTileObject(layerName,
                tos[(tos.length - 1) / 2].getXYZ(), srs.toString(), mime.getFormat(), parameters);
        fbs.get(midTO);
        Resource res = midTO.getBlob();

        assertThat(res, nullValue());
    }

    @Test
    public void testRenameLayer() throws Exception {
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        MimeType mime = ImageMime.png;
        SRS srs = SRS.getEPSG4326();
        final String layerName = "test:123123 112";

        int zoomLevel = 7;
        int x = 25;
        int y = 6;

        // long[] origXYZ = {x,y,zoomLevel};

        TileObject[] tos = new TileObject[6];

        for (int i = 0; i < tos.length; i++) {
            long[] xyz = { x + i - 1, y, zoomLevel };
            tos[i] = TileObject.createCompleteTileObject(layerName, xyz, srs.toString(),
                    mime.getFormat(), parameters, bytes);
            fbs.put(tos[i]);
        }

        final String newLayerName = "modifiedLayerName";
        BlobStoreListener listener = EasyMock.createNiceMock(BlobStoreListener.class);
        listener.layerRenamed(EasyMock.eq(layerName), EasyMock.eq(newLayerName));
        EasyMock.replay(listener);

        fbs.addListener(listener);

        boolean renamed = fbs.rename(layerName, newLayerName);
        assertTrue(renamed);

        EasyMock.verify(listener);

        expectedException.expect(StorageException.class);
        fbs.rename(layerName, newLayerName);
     }

    @Test
    public void testLayerMetadata() throws Exception {

        final String layerName = "TestLayer";
        final String key1 = "Test.Metadata.Property_1";
        final String key2 = "Test.Metadata.Property_2";

        assertThat(fbs.getLayerMetadata(layerName, key1), nullValue());
        assertThat(fbs.getLayerMetadata(layerName, key2), nullValue());

        fbs.putLayerMetadata(layerName, key1, "value 1");
        fbs.putLayerMetadata(layerName, key2, "value 2");
        assertThat(fbs.getLayerMetadata(layerName, key1), equalTo("value 1"));
        assertThat(fbs.getLayerMetadata(layerName, key2), equalTo("value 2"));

        fbs.putLayerMetadata(layerName, key1, "value 1_1");
        fbs.putLayerMetadata(layerName, key2, null);
        assertThat(fbs.getLayerMetadata(layerName, key1), equalTo("value 1_1"));
        assertThat(fbs.getLayerMetadata(layerName, key2), nullValue());
    }
    
    
    
    @Test
    public void testLayerNameLookup() throws Exception {

        final String layerName = "test:layer";
        
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = { 1L, 2L, 3L };
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        TileObject to = TileObject.createCompleteTileObject(layerName, xyz, "EPSG:4326",
                "image/jpeg", parameters, bytes);
        to.setId(11231231);

        fbs.put(to);
        
        File layerDir = new File(cacheDir.getRoot(), "test_layer");
        assertThat(layerDir, exists());
        assertThat(fbs.getLayerNameFromDirectory(layerDir), equalTo(layerName));
    }
    
    @Test
    public void testLayerNameLookupMetadataOnly() throws Exception {

        final String layerName = "test:layer";
        
        final String key1 = "Test.Metadata.Property_1";

        fbs.putLayerMetadata(layerName, key1, "value 1");
        
        File layerDir = new File(cacheDir.getRoot(), "test_layer");
        assertThat(layerDir, exists());
        assertThat(fbs.getLayerNameFromDirectory(layerDir), equalTo(layerName));
    }
    
    @Test
    public void testGridsetLookup() throws Exception {

        final String layerName = "test:layer";
        final String gridsetName = "EPSG:4326";
        
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = { 1L, 2L, 3L };
        Map<String, String> parameters = new HashMap<String, String>();
        TileObject to = TileObject.createCompleteTileObject(layerName, xyz, gridsetName,
                "image/jpeg", parameters, bytes);
        to.setId(11231231);

        fbs.put(to);
        
        File layerDir = new File(cacheDir.getRoot(), "test_layer");
        File tilesetDir = new File(layerDir, "EPSG_4326_03");
        assertThat(tilesetDir, exists());
        assertThat(fbs.getGridsetFromDirectory(tilesetDir), equalTo(gridsetName));
    }
    @Test
    public void testParametersLookup() throws Exception {

        final String layerName = "test:layer";
        final String gridsetName = "EPSG:4326";
        
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = { 1L, 2L, 3L };
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("A", "x");
        parameters.put("B", "ø");
        TileObject to = TileObject.createCompleteTileObject(layerName, xyz, gridsetName,
                "image/jpeg", parameters, bytes);
        to.setId(11231231);
        
        fbs.put(to);
        
        File layerDir = new File(cacheDir.getRoot(), "test_layer");
        File tilesetDir = new File(layerDir, "EPSG_4326_03_"+to.getParametersId());
        assertThat(tilesetDir, exists());
        assertThat(fbs.getParametersFromDirectory(tilesetDir), equalTo(parameters));
    }
    @Test
    public void testParametersLookupCase() throws Exception {

        final String layerName = "test:layer";
        final String gridsetName = "EPSG:4326";
        
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = { 1L, 2L, 3L };
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("A", "x");
        expected.put("B", "ø");
        TileObject to = TileObject.createCompleteTileObject(layerName, xyz, gridsetName,
                "image/jpeg", parameters, bytes);
        to.setId(11231231);
        
        fbs.put(to);
        
        File layerDir = new File(cacheDir.getRoot(), "test_layer");
        File tilesetDir = new File(layerDir, "EPSG_4326_03_"+to.getParametersId());
        assertThat(tilesetDir, exists());
        assertThat(fbs.getParametersFromDirectory(tilesetDir), equalTo(expected));
    }
    
    @Test
    public void testPurge() throws Exception {

        final String layerName = "test:layer";
        final String gridsetName = "EPSG:4326";
        
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = { 1L, 2L, 3L };
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("a", "x");
        parameters.put("b", "ø");
        TileObject to = TileObject.createCompleteTileObject(layerName, xyz, gridsetName,
                "image/jpeg", parameters, bytes);
        to.setId(11231231);
        
        fbs.put(to);
        
        File layerDir = new File(cacheDir.getRoot(), "test_layer");
        File tilesetDir = new File(layerDir, "EPSG_4326_03_"+to.getParametersId());
        
        Configuration config = EasyMock.createMock(Configuration.class);
        
        EasyMock.replay(config);
        
        fbs.purgeTilesetDirs(config);
        
        assertThat(tilesetDir, not(exists()));
        
        EasyMock.verify(config);
    }
    
    @Test
    public void testPurgeZoomedTileSet() throws Exception {

        final String layerName = "test:layer";
        final String gridsetName = "EPSG:4326";
        
        
        Resource bytes = new ByteArrayResource("1 2 3 4 5 6 test".getBytes());
        long[] xyz = { 1L, 2L, 3L };
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("A", "x");
        parameters.put("B", "ø");
        TileObject to = TileObject.createCompleteTileObject(layerName, xyz, gridsetName,
                "image/jpeg", parameters, bytes);
        to.setId(11231231);
        
        fbs.put(to);
        
        File layerDir = new File(cacheDir.getRoot(), "test_layer");
        File tilesetDir = new File(layerDir, "EPSG_4326_03_"+to.getParametersId());
        
        TileLayer layer = EasyMock.createMock(TileLayer.class);
        GridSubset subset = EasyMock.createMock(GridSubset.class);
        ParameterFilter filterA = EasyMock.createMock(ParameterFilter.class);
        ParameterFilter filterB = EasyMock.createMock(ParameterFilter.class);
        BlobStoreListener listener = EasyMock.createMock(BlobStoreListener.class);
        
        
        EasyMock.expect(layer.getName()).andStubReturn(layerName);
        EasyMock.expect(layer.getGridSubset(gridsetName)).andStubReturn(subset);
        EasyMock.expect(layer.getParameterFilters()).andStubReturn(Arrays.asList(filterA, filterB));
        
        EasyMock.expect(filterA.getKey()).andStubReturn("A");
        EasyMock.expect(filterB.getKey()).andStubReturn("B");
        
        EasyMock.expect(filterA.apply("x")).andStubReturn("x");// OK
        EasyMock.expect(filterB.apply("ø")).andStubReturn("o");// Changed
        
        listener.tileSetDeleted(layerName, gridsetName, null, to.getParametersId());EasyMock.expectLastCall().once();
        
        EasyMock.replay(layer, subset, filterA, filterB, listener);
        
        fbs.addListener(listener); 
        
        fbs.purgeZoomedTileSetDir(tilesetDir, layer);
        
        waitForPendingDeletes();
        assertThat(tilesetDir, not(exists()));
        
        EasyMock.verify(layer, subset, filterA, filterB, listener);
    }
   
    Matcher<File> exists() {
        return new BaseMatcher<File>(){

            @Override
            public boolean matches(Object item) {
                return ((File)item).exists();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("file that exists");
            }
            
        };
    }
}
