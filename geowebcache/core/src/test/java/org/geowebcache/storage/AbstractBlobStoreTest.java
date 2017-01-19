package org.geowebcache.storage;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.geowebcache.util.FileMatchers.whileRunning;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.storage.StorageObject.Status;
import org.geowebcache.util.FileMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import org.easymock.classextension.EasyMock;

/**
 * Test to do 
 */
public abstract class AbstractBlobStoreTest<TestClass extends BlobStore> {
    
    protected TestClass store;
    
    /**
     * Set up the test store in {@link store}.
     */
    @Before
    public abstract void createTestUnit() throws Exception;
    
    @After
    public void destroyTestUnit() throws Exception {
        store.destroy();
    }
    
    protected TileObject mockTileObject(String name, 
            String format, 
            long x, long y, long z, 
            String layerName, 
            String gridSet,
            String parametersID, Map<String, String> parameters) {
        
        TileObject to = EasyMock.createMock(name, TileObject.class);
        expect(to.getBlobFormat()).andStubReturn(format);
        expect(to.getXYZ()).andStubReturn(new long[]{x,y,z});
        expect(to.getLayerName()).andStubReturn(layerName);
        expect(to.getGridSetId()).andStubReturn(gridSet);
        expect(to.getParametersId()).andStubReturn(parametersID);
        expect(to.getParameters()).andStubReturn(parameters);
        return to;
    }
    
    @Test
    public void testEmpty() throws Exception {
        TileObject fromCache = mockTileObject("fromCache", "image/png", 0,0,0, "testLayer", "testGridSet", null, null);
        fromCache.setStatus(Status.MISS); expectLastCall().once();
        replay(fromCache);
        assertThat(store.get(fromCache), equalTo(false));
        verify(fromCache);
    }
    
    protected void mockStorageSize(TileObject to, int size) {
        expect(to.getBlobSize()).andStubReturn(size);
    }
    
    @Test
    public void testStoreTile() throws Exception {
        long time = 1_000_000_000L;
        int size = 14;
        TileObject toCache = mockTileObject("toCache", "image/png", 0,0,0, "testLayer", "testGridSet", null, null);
        TileObject fromCache = mockTileObject("fromCache", "image/png", 0,0,0, "testLayer", "testGridSet", null, null);
        
        expect(toCache.getBlob()).andStubReturn(new ByteArrayResource("1,2,4,5,6 test".getBytes(StandardCharsets.UTF_8)));
        expect(toCache.getCreated()).andStubReturn(time);
        mockStorageSize(toCache, size);
        fromCache.setBlob(EasyMock.anyObject()); expectLastCall().once();
        fromCache.setCreated(eq(time)); expectLastCall().once();
        fromCache.setBlobSize(eq(size)); expectLastCall().once();
        fromCache.setStatus(Status.HIT); EasyMock.expectLastCall().once();
        
        replay(toCache, fromCache);
        
        store.put(toCache);
        assertThat(store.get(fromCache), is(true));
        
        verify(toCache, fromCache);
    }
}
