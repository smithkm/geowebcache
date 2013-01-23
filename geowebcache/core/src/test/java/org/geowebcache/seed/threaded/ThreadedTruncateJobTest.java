package org.geowebcache.seed.threaded;

import org.easymock.classextension.EasyMock;
import org.geowebcache.TestHelpers;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;
import org.geowebcache.seed.AbstractJobTest;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.seed.GWCTask.TYPE;
import org.geowebcache.seed.Job;
import org.geowebcache.seed.SeedRequest;
import org.geowebcache.seed.SeedTestUtils;
import org.geowebcache.seed.TileBreeder;
import org.geowebcache.seed.TruncateTask;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.TileRangeIterator;
import org.junit.Assume;

import static org.junit.Assert.*;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.*;
import static org.geowebcache.TestHelpers.createRequest;

public class ThreadedTruncateJobTest extends AbstractJobTest {

@Override
protected Job initNextLocation(TileRangeIterator tri) throws Exception {
    final TileBreeder breeder = createMock(ThreadedTileBreeder.class);
    final TruncateTask task = SeedTestUtils.createMockTruncateTask(breeder);
    replay(task);
    replay(breeder);
    
    TileLayer tl = createMock(TileLayer.class);
    replay(tl);
    
    ThreadedTruncateJob job = new ThreadedTruncateJob(1, breeder, tri, tl, false);
    
    job.threads[0] = task;

    return job;
}

@Override
protected void assertGetState(STATE expected, STATE... states) throws Exception {
    if(states.length<=1){
        super.assertGetState(expected, states);
    } else {
        // Don't test as this job can never have more than one task.
    }
}

@Override
public void testTerminate() throws Exception {
    // Test uses multiple jobs so skip.
}

/**
 * Make sure that the correct calls are made when Truncating
 * 
 * @throws Exception
 */
public void testSeedStoredTiles() throws Exception {

    StorageBroker storageBroker;
    WMSLayer tl;

    tl = TestHelpers.createWMSLayer("image/png");

    final String gridSetId = tl.getGridSubsets().iterator().next();
    final int zoomLevel = 3;
    SeedRequest req = createRequest(tl, TYPE.TRUNCATE, zoomLevel, zoomLevel);

    final GridSubset gridSubset = tl.getGridSubset(gridSetId);

    storageBroker = createMock(StorageBroker.class);
    
    TileRange tr = ThreadedTileBreeder.createTileRange(req, tl);
    TileRangeIterator trIter = tr.iterator(tl.getMetaTilingFactors());

    
    final TileBreeder breeder = createMock(ThreadedTileBreeder.class);
    final TruncateTask task = SeedTestUtils.createMockTruncateTask(breeder);
    expect(task.getState()).andStubReturn(STATE.READY);
    expectDoActionInternal(task);
    expectDispose(task);
    replay(task);
    replay(breeder);
    replay(storageBroker);
  

    ThreadedTruncateJob job = new ThreadedTruncateJob(1,breeder, trIter, tl, false);
    assertEquals(job.getTasks()[0], task);
    
    job.runSynchronously();

    verify(storageBroker);
}

@Override
protected TileBreeder createMockTileBreeder() {
    return createMock(ThreadedTileBreeder.class);
}

@Override
protected Job createTestJob(TileBreeder breeder, int threads) {
    Assume.assumeTrue(threads==1);
    TileLayer tl = createMock(TileLayer.class);
    expect(tl.getName()).andStubReturn("testLayer");
    replay(tl);
    TileRange tr = createMock(TileRange.class);
    expect(tr.tileCount()).andStubReturn(10l);
    replay(tr);
    TileRangeIterator tri = createMock(TileRangeIterator.class);
    expect(tri.getTileRange()).andStubReturn(tr);
    replay(tri);
    return new ThreadedTruncateJob(threads, breeder, tri, tl, false);
}

@Override
protected GWCTask createMockTask(TileBreeder mockBreeder) throws Exception {
    return SeedTestUtils.createMockTruncateTask(mockBreeder);
}


}
