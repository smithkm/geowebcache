package org.geowebcache.blobstore.file;

import org.geowebcache.storage.AbstractBlobStoreTest;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.blobstore.file.FileBlobStore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import org.easymock.EasyMock;

public class FileBlobStoreComformanceTest extends AbstractBlobStoreTest<FileBlobStore> {
    
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    
    @Override
    public void createTestUnit() throws Exception {
        this.store = new FileBlobStore(temp.getRoot().getAbsolutePath());
    }

    @Override
    protected void mockStorageSize(TileObject to, int size) {
        EasyMock.expect(to.getBlobSize()).andReturn(size).once();
        int padded = (size/4096 + size%4096>0 ?1:0)*4096;
        to.setBlobSize(padded);EasyMock.expectLastCall().once();
        EasyMock.expect(to.getBlobSize()).andStubReturn(padded);
    }
    
    
}
