package org.geowebcache.seed.threaded;

import static org.junit.Assert.*;
import static org.easymock.classextension.EasyMock.*;
import static org.hamcrest.Matchers.*;

import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.seed.SeederThreadPoolExecutor;
import org.geowebcache.seed.TileBreederTest;
import org.geowebcache.storage.StorageBroker;
import org.junit.Before;

public class ThreadedTileBreederTest extends TileBreederTest {

    @Override
    protected void initTld(){
    }
    @Override
    protected void initExecutor(){
    }
    @Override
    protected void initBroker(){
    }
    
    @Before
    public void setUp() throws Exception {
        tld = createMock(TileLayerDispatcher.class);{
            initTld();
        } replay(tld);
        
        executor = createMock(SeederThreadPoolExecutor.class);{
            initExecutor();
        } replay(executor);
        
        broker = createMock(StorageBroker.class);{
            initBroker();
        } replay(broker);
        
        breeder = new ThreadedTileBreeder();
        breeder.setTileLayerDispatcher(tld);
        breeder.setStorageBroker(broker);
        breeder.setThreadPoolExecutor(executor);
    }
}
