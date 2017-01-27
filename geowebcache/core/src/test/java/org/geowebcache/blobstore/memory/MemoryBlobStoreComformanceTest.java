package org.geowebcache.blobstore.memory;

import org.geowebcache.storage.AbstractBlobStoreTest;
import org.geowebcache.storage.blobstore.memory.MemoryBlobStore;

public class MemoryBlobStoreComformanceTest extends AbstractBlobStoreTest<MemoryBlobStore> {
    
    @Override
    public void createTestUnit() throws Exception {
        this.store = new MemoryBlobStore();
    }
}
