package org.geowebcache.storage.blobstore.memory;

import org.geowebcache.storage.AbstractBlobStoreTest;
import org.geowebcache.storage.blobstore.memory.MemoryBlobStore;
import org.junit.Before;

public class MemoryBlobStoreComformanceTest extends AbstractBlobStoreTest<MemoryBlobStore> {
    
    @Override
    public void createTestUnit() throws Exception {
        this.store = new MemoryBlobStore();
    }
    
    @Before
    public void setEvents() throws Exception {
        this.events = false;
    }
}
