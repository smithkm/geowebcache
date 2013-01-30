package org.geowebcache.seed.threaded;

import org.geowebcache.GeoWebCacheException;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.TileBreeder;
import org.geowebcache.seed.TruncateJob;
import org.geowebcache.storage.TileRangeIterator;

public class ThreadedTruncateJob extends ThreadedJob implements TruncateJob {

    // Package visible, Jobs should be created by calling the factory method on the TileBreeder
    ThreadedTruncateJob(long id, TileBreeder breeder,TileRangeIterator tri, TileLayer tl, boolean doFilterUpdates) {
        super(id, breeder, tl, 1, tri, doFilterUpdates);
    }

    @Override
    protected void createThreads() {
        threads = new GWCTask[1];
        threads[0] = ((ThreadedTileBreeder)breeder).createTruncateTask(this);
    }
    
    public void runSynchronously() throws GeoWebCacheException,
            InterruptedException {
        threads[0].doAction();
    }
    
    
    public GWCTask.TYPE getType(){
        return GWCTask.TYPE.TRUNCATE;
    }
}