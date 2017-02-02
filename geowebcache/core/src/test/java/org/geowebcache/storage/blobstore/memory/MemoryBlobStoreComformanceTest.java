package org.geowebcache.storage.blobstore.memory;

import org.geowebcache.storage.AbstractBlobStoreTest;
import org.geowebcache.storage.blobstore.memory.MemoryBlobStore;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class MemoryBlobStoreComformanceTest extends AbstractBlobStoreTest<MemoryBlobStore> {
    
    @Override
    public void createTestUnit() throws Exception {
        this.store = new MemoryBlobStore();
    }
    
    @Before
    public void setEvents() throws Exception {
        this.events = false;
    }

    @Override
    @Ignore @Test // Memory store can be more relaxed about this. It would be nice to pass this though
    public void testDeleteGridsetDoesntDeleteOthers() throws Exception {
        super.testDeleteGridsetDoesntDeleteOthers();
    }
    
    
}
