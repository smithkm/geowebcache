package org.geowebcache.storage;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.geq;
import static org.geowebcache.util.FileMatchers.resource;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.geowebcache.filter.parameters.ParametersUtils;
import org.geowebcache.io.ByteArrayResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.easymock.Capture;
import org.easymock.EasyMock;

/**
 * Test to do 
 */
public abstract class AbstractBlobStoreTest<TestClass extends BlobStore> {
    
    protected TestClass store;
    
    protected boolean events = true;
    
    /**
     * Set up the test store in {@link store}.
     */
    @Before
    public abstract void createTestUnit() throws Exception;
    
    /**
     * Override and add tear down assertions after calling super
     * @throws Exception
     */
    @After
    public void destroyTestUnit() throws Exception {
        store.destroy();
    }
    
    @Test
    public void testEmpty() throws Exception {
        TileObject fromCache = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null);
        assertThat(store.get(fromCache), equalTo(false));
        //assertThat(fromCache, hasProperty("status", is(Status.MISS)));
    }
    
    @Test
    public void testStoreTile() throws Exception {
        BlobStoreListener listener = EasyMock.createMock(BlobStoreListener.class);
        store.addListener(listener);
        TileObject toCache = TileObject.createCompleteTileObject("testLayer",  new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null, new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)));
        final long size = toCache.getBlobSize();
        TileObject fromCache = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null);
        
        if(events) {
            listener.tileStored(eq("testLayer"), eq("testGridSet"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), 
                geq(size) // Some stores have minimum block sizes and so have to pad this
                );EasyMock.expectLastCall();
        }
        
        EasyMock.replay(listener);
        
        store.put(toCache);
        
        EasyMock.verify(listener);
        
        assertThat(store.get(fromCache), is(true));
        //assertThat(fromCache, hasProperty("status", is(Status.HIT)));
        assertThat(fromCache, hasProperty("blobSize", is((int)size)));
        
        assertThat(fromCache, hasProperty("blob",resource(new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)))));
    }
    
    @Test
    public void testStoreTilesInMultipleLayers() throws Exception {
        BlobStoreListener listener = EasyMock.createMock(BlobStoreListener.class);
        store.addListener(listener);
        TileObject toCache1 = TileObject.createCompleteTileObject("testLayer1",  new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null, new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)));
        TileObject toCache2 = TileObject.createCompleteTileObject("testLayer2",  new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null, new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)));
        final long size1 = toCache1.getBlobSize();
        final long size2 = toCache2.getBlobSize();
        TileObject fromCache1 = TileObject.createQueryTileObject("testLayer1", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null);
        TileObject fromCache2_1 = TileObject.createQueryTileObject("testLayer2", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null);
        TileObject fromCache2_2 = TileObject.createQueryTileObject("testLayer2", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null);
        
        if(events) {
            listener.tileStored(eq("testLayer1"), eq("testGridSet"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), geq(size1));
            listener.tileStored(eq("testLayer2"), eq("testGridSet"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), geq(size2));
        }
        
        EasyMock.replay(listener);
        
        store.put(toCache1);
        assertThat(store.get(fromCache2_1), is(false));
        assertThat(fromCache2_1, hasProperty("blobSize", is(0)));
        
        store.put(toCache2);
        EasyMock.verify(listener);
        
        assertThat(store.get(fromCache1), is(true));
        assertThat(fromCache1, hasProperty("blobSize", is((int)size1)));
        assertThat(fromCache1, hasProperty("blob",resource(new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)))));
        assertThat(store.get(fromCache2_2), is(true));
        assertThat(fromCache2_2, hasProperty("blobSize", is((int)size2)));
        assertThat(fromCache2_2, hasProperty("blob",resource(new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)))));
    }
    
    @Test
    public void testDeleteTile() throws Exception {
        BlobStoreListener listener = EasyMock.createMock(BlobStoreListener.class);
        store.addListener(listener);
        TileObject toCache = TileObject.createCompleteTileObject("testLayer",  new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null, new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)));
        TileObject remove = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null);
        TileObject fromCache = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null);
        
        Capture<Long> sizeCapture = new Capture<>();
        if(events) {
            listener.tileStored(eq("testLayer"), eq("testGridSet"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), 
                capture(sizeCapture)
                );EasyMock.expectLastCall();
        }
        
        EasyMock.replay(listener);
        
        store.put(toCache);
        EasyMock.verify(listener);
        long storedSize = 0;
        if(events) {
            storedSize=sizeCapture.getValue();
        }
        EasyMock.reset(listener);
        if(events) {
            listener.tileDeleted(eq("testLayer"), eq("testGridSet"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), 
                eq(storedSize)
                );EasyMock.expectLastCall();
        }
        EasyMock.replay(listener);
        store.delete(remove);
        EasyMock.verify(listener);
        assertThat(store.get(fromCache), is(false));
        assertThat(fromCache, hasProperty("blobSize", is(0)));
    }
    
    @Test
    public void testUpdateTile() throws Exception {
        BlobStoreListener listener = EasyMock.createMock(BlobStoreListener.class);
        store.addListener(listener);
        TileObject toCache1 = TileObject.createCompleteTileObject("testLayer",  new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null, new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)));
        TileObject toCache2 = TileObject.createCompleteTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null, new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)));
        final long size2 = toCache2.getBlobSize();
        TileObject fromCache = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", null);
        
        Capture<Long> sizeCapture = new Capture<>();
        if(events){
            listener.tileStored(eq("testLayer"), eq("testGridSet"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), 
                capture(sizeCapture)
                );EasyMock.expectLastCall();
        }
        EasyMock.replay(listener);
        
        store.put(toCache1);
        EasyMock.verify(listener);
        long storedSize = 0;
        if(events){
            storedSize = sizeCapture.getValue();
        }
        EasyMock.reset(listener);
        if(events){
            listener.tileUpdated(eq("testLayer"), eq("testGridSet"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0),
                geq(size2),
                eq(storedSize)
                );EasyMock.expectLastCall();
        }
        EasyMock.replay(listener);
        store.put(toCache2);
        EasyMock.verify(listener);
        assertThat(store.get(fromCache), is(true));
        assertThat(fromCache, hasProperty("blobSize", is((int)size2)));
        assertThat(fromCache, hasProperty("blob",resource(new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)))));
    }
    
    @Test
    public void testGridsets() throws Exception {
        BlobStoreListener listener = EasyMock.createMock(BlobStoreListener.class);
        store.addListener(listener);
        TileObject toCache1 = TileObject.createCompleteTileObject("testLayer",  new long[]{0L, 0L, 0L}, "testGridSet1", "image/png", null, new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)));
        TileObject toCache2 = TileObject.createCompleteTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet2", "image/png", null, new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)));
        final long size1 = toCache1.getBlobSize();
        final long size2 = toCache2.getBlobSize();
        TileObject remove = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet1", "image/png", null);
        TileObject fromCache1_1 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet1", "image/png", null);
        TileObject fromCache2_1 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet2", "image/png", null);
        TileObject fromCache1_2 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet1", "image/png", null);
        TileObject fromCache2_2 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet2", "image/png", null);
        TileObject fromCache2_3 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet2", "image/png", null);
        
        Capture<Long> sizeCapture1 = new Capture<>();
        Capture<Long> sizeCapture2 = new Capture<>();
        if(events) {
            listener.tileStored(eq("testLayer"), eq("testGridSet1"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), 
                capture(sizeCapture1)
                );EasyMock.expectLastCall();
            listener.tileStored(eq("testLayer"), eq("testGridSet2"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), 
                capture(sizeCapture2)
                );EasyMock.expectLastCall();
        }
        
        EasyMock.replay(listener);
        
        store.put(toCache1);
        assertThat(store.get(fromCache2_1), is(false));
        store.put(toCache2);
        EasyMock.verify(listener);
        long storedSize1 = 0;
        if(events) {
            storedSize1 = sizeCapture1.getValue();
        }
        assertThat(store.get(fromCache1_1), is(true));
        assertThat(fromCache1_1, hasProperty("blobSize", is((int)size1)));
        assertThat(fromCache1_1, hasProperty("blob",resource(new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)))));
        assertThat(store.get(fromCache2_2), is(true));
        assertThat(fromCache2_2, hasProperty("blobSize", is((int)size2)));
        assertThat(fromCache2_2, hasProperty("blob",resource(new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)))));
        EasyMock.reset(listener);
        if(events) {
            listener.tileDeleted(eq("testLayer"), eq("testGridSet1"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), 
                eq(storedSize1)
                );EasyMock.expectLastCall();
        }
        EasyMock.replay(listener);
        store.delete(remove);
        EasyMock.verify(listener);
        assertThat(store.get(fromCache1_2), is(false));
        assertThat(fromCache1_2, hasProperty("blobSize", is(0)));
        assertThat(store.get(fromCache2_3), is(true));
        assertThat(fromCache2_3, hasProperty("blobSize", is((int)size2)));
        assertThat(fromCache2_3, hasProperty("blob",resource(new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)))));
    }
    
    @Test
    public void testDeleteGridset() throws Exception {
        BlobStoreListener listener = EasyMock.createMock(BlobStoreListener.class);
        store.addListener(listener);
        TileObject toCache1 = TileObject.createCompleteTileObject("testLayer",  new long[]{0L, 0L, 0L}, "testGridSet1", "image/png", null, new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)));
        TileObject toCache2 = TileObject.createCompleteTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet2", "image/png", null, new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)));
        final long size1 = toCache1.getBlobSize();
        final long size2 = toCache2.getBlobSize();
        
        TileObject fromCache1_1 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet1", "image/png", null);
        TileObject fromCache2_1 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet2", "image/png", null);
        TileObject fromCache1_2 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet1", "image/png", null);
        TileObject fromCache2_2 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet2", "image/png", null);
        TileObject fromCache2_3 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet2", "image/png", null);
        
        if(events) {
            listener.tileStored(eq("testLayer"), eq("testGridSet1"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), 
                geq(size1)
                );EasyMock.expectLastCall();
            listener.tileStored(eq("testLayer"), eq("testGridSet2"), eq("image/png"), eq(null), eq(0L), eq(0L), eq(0), 
                geq(size2)
                );EasyMock.expectLastCall();
        }
        
        EasyMock.replay(listener);
        
        store.put(toCache1);
        assertThat(store.get(fromCache2_1), is(false));
        store.put(toCache2);
        EasyMock.verify(listener);
        assertThat(store.get(fromCache1_1), is(true));
        assertThat(fromCache1_1, hasProperty("blobSize", is((int)size1)));
        assertThat(fromCache1_1, hasProperty("blob",resource(new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)))));
        assertThat(store.get(fromCache2_2), is(true));
        assertThat(fromCache2_2, hasProperty("blobSize", is((int)size2)));
        assertThat(fromCache2_2, hasProperty("blob",resource(new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)))));
        EasyMock.reset(listener);
        if(events) {
            listener.gridSubsetDeleted(eq("testLayer"), eq("testGridSet1"));EasyMock.expectLastCall();
        }
        EasyMock.replay(listener);
        store.deleteByGridsetId("testLayer", "testGridSet1");
        EasyMock.verify(listener);
        assertThat(store.get(fromCache1_2), is(false));
        assertThat(fromCache1_2, hasProperty("blobSize", is(0)));
        assertThat(store.get(fromCache2_3), is(true));
        assertThat(fromCache2_3, hasProperty("blobSize", is((int)size2)));
        assertThat(fromCache2_3, hasProperty("blob",resource(new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)))));
    }
    
    @Test
    public void testParameters() throws Exception {
        BlobStoreListener listener = EasyMock.createMock(BlobStoreListener.class);
        store.addListener(listener);
        Map<String, String> params1 = Collections.singletonMap("testKey", "testValue1");
        Map<String, String> params2 = Collections.singletonMap("testKey", "testValue2");
        TileObject toCache1 = TileObject.createCompleteTileObject("testLayer",  new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params1, new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)));
        TileObject toCache2 = TileObject.createCompleteTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params2, new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)));
        final long size1 = toCache1.getBlobSize();
        final long size2 = toCache2.getBlobSize();
        
        TileObject remove = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params1);
        TileObject fromCache1_1 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params1);
        TileObject fromCache2_1 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params2);
        TileObject fromCache1_2 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params1);
        TileObject fromCache2_2 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params2);
        TileObject fromCache2_3 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params2);
        
        Capture<Long> sizeCapture1 = new Capture<>();
        Capture<Long> sizeCapture2 = new Capture<>();
        Capture<String> pidCapture1 = new Capture<>();
        Capture<String> pidCapture2 = new Capture<>();
        if(events){
            listener.tileStored(eq("testLayer"), eq("testGridSet"), eq("image/png"), capture(pidCapture1), eq(0L), eq(0L), eq(0), 
                capture(sizeCapture1)
                );EasyMock.expectLastCall();
            listener.tileStored(eq("testLayer"), eq("testGridSet"), eq("image/png"), capture(pidCapture2), eq(0L), eq(0L), eq(0), 
                capture(sizeCapture2)
                );EasyMock.expectLastCall();
        }
        
        EasyMock.replay(listener);
        
        store.put(toCache1);
        assertThat(store.get(fromCache2_1), is(false));
        store.put(toCache2);
        EasyMock.verify(listener);
        long storedSize1 = 0;
        if(events) {
            storedSize1 = sizeCapture1.getValue();
            // parameter id strings should be non-null and not equal to one another
            assertThat(pidCapture1.getValue(), notNullValue());
            assertThat(pidCapture2.getValue(), notNullValue());
            assertThat(pidCapture2.getValue(), not(pidCapture1.getValue()));
        }
        
        assertThat(store.get(fromCache1_1), is(true));
        assertThat(fromCache1_1, hasProperty("blobSize", is((int)size1)));
        assertThat(fromCache1_1, hasProperty("blob",resource(new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)))));
        assertThat(store.get(fromCache2_2), is(true));
        assertThat(fromCache2_2, hasProperty("blobSize", is((int)size2)));
        assertThat(fromCache2_2, hasProperty("blob",resource(new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)))));
        EasyMock.reset(listener);
        if(events) {
            listener.tileDeleted(eq("testLayer"), eq("testGridSet"), eq("image/png"), eq(pidCapture1.getValue()), eq(0L), eq(0L), eq(0), 
                eq(storedSize1)
                );
        }
        EasyMock.replay(listener);
        store.delete(remove);
        EasyMock.verify(listener);
        assertThat(store.get(fromCache1_2), is(false));
        assertThat(fromCache1_2, hasProperty("blobSize", is(0)));
        assertThat(store.get(fromCache2_3), is(true));
        assertThat(fromCache2_3, hasProperty("blobSize", is((int)size2)));
        assertThat(fromCache2_3, hasProperty("blob",resource(new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)))));
    }
    
    @Test
    public void testMetadata() throws Exception {
        assertThat(store.getLayerMetadata("testLayer", "testKey"), nullValue());
        store.putLayerMetadata("testLayer", "testKey", "testValue");
        assertThat(store.getLayerMetadata("testLayer", "testKey"), equalTo("testValue"));
    }
    
    @Test
    public void testMetadataWithEqualsInKey() throws Exception {
        assertThat(store.getLayerMetadata("testLayer", "test=Key"), nullValue());
        store.putLayerMetadata("testLayer", "test=Key", "testValue");
        assertThat(store.getLayerMetadata("testLayer", "test=Key"), equalTo("testValue"));
    }
    
    @Test
    public void testMetadataWithEqualsInValue() throws Exception {
        assertThat(store.getLayerMetadata("testLayer", "testKey"), nullValue());
        store.putLayerMetadata("testLayer", "testKey", "test=Value");
        assertThat(store.getLayerMetadata("testLayer", "testKey"), equalTo("test=Value"));
    }
    
    @Test
    public void testMetadataWithAmpInKey() throws Exception {
        assertThat(store.getLayerMetadata("testLayer", "test&Key"), nullValue());
        store.putLayerMetadata("testLayer", "test&Key", "testValue");
        assertThat(store.getLayerMetadata("testLayer", "test&Key"), equalTo("testValue"));
    }
    
    @Test
    public void testMetadataWithAmpInValue() throws Exception {
        assertThat(store.getLayerMetadata("testLayer", "testKey"), nullValue());
        store.putLayerMetadata("testLayer", "testKey", "test&Value");
        assertThat(store.getLayerMetadata("testLayer", "testKey"), equalTo("test&Value"));
    }
    
    @Test
    public void testMetadataWithPercentInKey() throws Exception {
        assertThat(store.getLayerMetadata("testLayer", "test%Key"), nullValue());
        store.putLayerMetadata("testLayer", "test%Key", "testValue");
        assertThat(store.getLayerMetadata("testLayer", "test%Key"), equalTo("testValue"));
    }
    
    @Test
    public void testMetadataWithPercentInValue() throws Exception {
        assertThat(store.getLayerMetadata("testLayer", "testKey"), nullValue());
        store.putLayerMetadata("testLayer", "testKey", "test%Value");
        assertThat(store.getLayerMetadata("testLayer", "testKey"), equalTo("test%Value"));
    }
    
    @Test
    public void testParameterList() throws Exception {
        Map<String, String> params1 = Collections.singletonMap("testKey", "testValue1");
        Map<String, String> params2 = Collections.singletonMap("testKey", "testValue2");
        TileObject toCache1 = TileObject.createCompleteTileObject("testLayer",  new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params1, new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)));
        TileObject toCache2 = TileObject.createCompleteTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params2, new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)));
        
        assertThat(store.getParameters("testLayer"), empty());
        store.put(toCache1);
        assertThat(store.getParameters("testLayer"), containsInAnyOrder(params1));
        store.put(toCache2);
        assertThat(store.getParameters("testLayer"), containsInAnyOrder(params1, params2));
    }
    
    @Test
    public void testDeleteByParametersId() throws Exception {
        Map<String, String> params1 = Collections.singletonMap("testKey", "testValue1");
        String paramID1 = ParametersUtils.getId(params1);
        Map<String, String> params2 = Collections.singletonMap("testKey", "testValue2");
        String paramID2 = ParametersUtils.getId(params2);
        TileObject toCache1 = TileObject.createCompleteTileObject("testLayer",  new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params1, new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)));
        TileObject toCache2 = TileObject.createCompleteTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params2, new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)));
        BlobStoreListener listener = EasyMock.createMock(BlobStoreListener.class);
        store.addListener(listener);
        final long size1 = toCache1.getBlobSize();
        final long size2 = toCache2.getBlobSize();
        
        TileObject fromCache1_1 = TileObject.createQueryTileObject("testLayer",  new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params1);
        TileObject fromCache2_1 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params2);
        TileObject fromCache1_2 = TileObject.createQueryTileObject("testLayer",  new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params1);
        TileObject fromCache2_2 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params2);
        TileObject fromCache2_3 = TileObject.createQueryTileObject("testLayer", new long[]{0L, 0L, 0L}, "testGridSet", "image/png", params2);
        
        if(events) {
            listener.tileStored(eq("testLayer"), eq("testGridSet"), eq("image/png"), eq(paramID1), eq(0L), eq(0L), eq(0), 
                geq(size1)
                );
            listener.tileStored(eq("testLayer"), eq("testGridSet"), eq("image/png"), eq(paramID2), eq(0L), eq(0L), eq(0), 
                geq(size2)
                );
        }
        
        EasyMock.replay(listener);
        
        store.put(toCache1);
        assertThat(store.get(fromCache2_1), is(false));
        store.put(toCache2);
        EasyMock.verify(listener);
        assertThat(store.get(fromCache1_1), is(true));
        assertThat(fromCache1_1, hasProperty("blobSize", is((int)size1)));
        assertThat(fromCache1_1, hasProperty("blob",resource(new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)))));
        assertThat(store.get(fromCache2_2), is(true));
        assertThat(fromCache2_2, hasProperty("blobSize", is((int)size2)));
        assertThat(fromCache2_2, hasProperty("blob",resource(new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)))));
        EasyMock.reset(listener);
        if(events) {
            listener.parametersDeleted(eq("testLayer"), eq(paramID1));
        }
        EasyMock.replay(listener);
        store.deleteByParametersId("testLayer", paramID1);
        EasyMock.verify(listener);
        assertThat(store.get(fromCache1_2), is(false));
        assertThat(fromCache1_2, hasProperty("blobSize", is(0)));
        assertThat(store.get(fromCache2_3), is(true));
        assertThat(fromCache2_3, hasProperty("blobSize", is((int)size2)));
        assertThat(fromCache2_3, hasProperty("blob",resource(new ByteArrayResource("7,8,9,10 test".getBytes(StandardCharsets.UTF_8)))));
    }
}
