package org.geowebcache.storage.blobstore.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.filter.request.GreenTileException;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.storage.DefaultStorageFinder;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.StorageObject.Status;
import org.geowebcache.storage.TileObject;

public class DummyBlobStore extends FileBlobStore {
    private static Log log = LogFactory
    .getLog(org.geowebcache.storage.blobstore.file.DummyBlobStore.class);

    public DummyBlobStore(DefaultStorageFinder defStoreFinder)
            throws StorageException, ConfigurationException {
        super(defStoreFinder);
    }

    public DummyBlobStore(String rootPath) throws StorageException {
        super(rootPath);
    }

    
    @Override
    public boolean get(TileObject stObj) throws StorageException {
        File fh = getFileHandleTile(stObj, false);
        if(!fh.exists()) {
            stObj.setStatus(Status.MISS);
            return false;
        } else {
            Resource resource = getBlankTile(); // Use a blank tile rather than the file contents
            stObj.setBlob(resource);
            stObj.setCreated(fh.lastModified()); // Use the modification time from the empty file
            stObj.setBlobSize((int) resource.getSize());
            return true;
        }
    }

    @Override
    protected void writeFile(File target, TileObject stObj, boolean existed)
            throws StorageException {
        if(!target.exists()) {
            try{
                target.createNewFile();
            } catch (IOException ex) {
                throw new StorageException("Could not create file "+ target.getName() ,ex);
            }
        } else {
            target.setLastModified(System.currentTimeMillis());
        }
    }
    
    // Copied from RasterFilter
    private Resource getBlankTile() {
        // Use the built-in one: 
        InputStream is = null;   
        try {
            //is = GeoWebCacheDispatcher.class.getResourceAsStream("blank.png");
            is = GreenTileException.class.getResourceAsStream("green.png");
            byte[] blankTile = new byte[425];
            int ret = is.read(blankTile);
            log.info("Read " + ret + " from blank PNG file (expected 425).");
            
            return new ByteArrayResource(blankTile);
        } catch (IOException ioe) {
            log.error(ioe.getMessage());
        } finally {
            try {
                if(is != null) 
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
       return null;
    }

}
