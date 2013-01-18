package org.geowebcache.seed;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.reset;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.easymock.EasyMock;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.Job;
import org.geowebcache.seed.SeedJob;
import org.geowebcache.seed.SeederThreadPoolExecutor;
import org.geowebcache.seed.threaded.ThreadedTileBreeder;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.TileRangeIterator;
import org.hamcrest.Matchers;
import org.junit.Test;

public abstract class TileBreederTest {

protected ThreadedTileBreeder breeder;

    protected abstract void initBroker();

    protected abstract void initExecutor();

    protected abstract void initTld();

    protected TileLayerDispatcher tld;
    protected SeederThreadPoolExecutor executor;
    protected StorageBroker broker;

public TileBreederTest() {
    super();
}

@Test
public void testCreateJob() throws Exception {
    TileLayer tl = createMock(TileLayer.class);{
        expect(tl.getMetaTilingFactors()).andStubReturn(new int[]{1,1});
    } replay(tl);

    reset(tld);{
        initTld();
        expect(tld.getTileLayer("testLayer")).andStubReturn(tl);
    } replay(tld);
    
    TileRangeIterator tri = createMock(TileRangeIterator.class);
    
    TileRange tr = createMock(TileRange.class);{
        expect(tr.getLayerName()).andStubReturn("testLayer");
        expect(tr.iterator(EasyMock.aryEq(new int[]{1,1}))).andStubReturn(tri);
    } replay(tr);
    
    expect(tri.getTileRange()).andStubReturn(tr);    
    replay(tri);
    
    GWCTask.TYPE type = GWCTask.TYPE.SEED;
    
    int threadCount = 1;
    
    boolean filterUpdate = false;
    
    SeedJob j = (SeedJob) breeder.createJob(tr, type, threadCount, filterUpdate);
    
    assertThat(breeder.getJobByID(j.getId()), equalTo((Job)j));
    
    assertThat(j.getRange(), equalTo(tr));
    assertThat(j.getState(), equalTo(GWCTask.STATE.READY));
    assertThat(j.getTasks(), Matchers.arrayWithSize(1));
    assertThat(j.isReseed(), equalTo(false));
}

}