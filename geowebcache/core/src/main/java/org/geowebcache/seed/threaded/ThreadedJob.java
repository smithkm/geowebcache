package org.geowebcache.seed.threaded;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.collections.iterators.ArrayIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.filter.request.RequestFilter;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.GWCTask.STATE;
import org.geowebcache.seed.Job;
import org.geowebcache.seed.JobStatus;
import org.geowebcache.seed.JobUtils;
import org.geowebcache.seed.TaskStatus;
import org.geowebcache.seed.TileBreeder;
import org.geowebcache.seed.TileRequest;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.TileRangeIterator;
import org.springframework.util.Assert;


/**
 * Base class for seeding jobs
 * 
 * @author Kevin Smith, OpenGeo
 *
 */
abstract class ThreadedJob implements Job {

    private static final Log log = LogFactory.getLog(GWCTask.class);
    
    final private AtomicLong activeThreads = new AtomicLong();
    final protected TileBreeder breeder;
    final protected int threadCount;
    final protected long id;
    final protected TileRangeIterator tri;
    final protected TileLayer tl;
    
    public static final long TIME_NOT_STARTED=-1;
    private long groupStartTime=TIME_NOT_STARTED;
    
    private final boolean doFilterUpdate;
    
    /**
     * 
     * @param id unique ID of the Job
     * @param breeder the TileBreeder that created this job
     * @param tl the layer this job is operating on
     * @param threadCount the number of threads to try to use
     * @param tri iterator over the tiles to be handled
     * @param doFilterUpdate update relevant filters on the layer after the job completes
     */
    protected ThreadedJob(long id, TileBreeder breeder, TileLayer tl, int threadCount, 
            TileRangeIterator tri, boolean doFilterUpdate) {

        Assert.notNull(breeder);
        Assert.notNull(tl);
        Assert.notNull(tri);
        Assert.isTrue(threadCount>0,"threadCount must be positive");
        Assert.isTrue(id>=0,"Job id must be non-negative");
        
        this.breeder = breeder;
        this.threadCount = threadCount;
        this.id = id;
        this.tri = tri;
        this.tl = tl;
        this.doFilterUpdate = doFilterUpdate;
        
        createThreads();
        Assert.state(threads!=null);
        for(GWCTask thread: threads){
            Assert.state(thread!=null);
        }
    }

    protected abstract void createThreads();
    
    protected GWCTask[] threads;

    public TileBreeder getBreeder() {
        return breeder;
    }

    public long getId() {
        return id;
    }

    GWCTask[] getTasks() {
        for(GWCTask task: threads){
            Assert.state(task!=null);
        }
        return threads;
    }

    
    public TileRequest getNextLocation() throws InterruptedException {
        long gridLoc[] = tri.nextMetaGridLocation();
        if (gridLoc == null) return null;  // We're done
        return new ThreadedTileRequest(gridLoc[0], gridLoc[1], gridLoc[2]);
    }
    
    protected void myTask(GWCTask task) {
        Assert.isTrue(task.getJob()==this, "Task does not belong to this Job");
    }
    
    public long getThreadCount() {
        return activeThreads.get();
    }

    public void threadStarted(GWCTask thread) {
        myTask(thread);
        
        activeThreads.incrementAndGet();
        synchronized(this) {
            if(this.groupStartTime==TIME_NOT_STARTED){
                this.groupStartTime=System.currentTimeMillis();
            }
        }
    }

    public void threadStopped(GWCTask thread) {
        myTask(thread);
        
        long membersRemaining = activeThreads.decrementAndGet();
        if (0 == membersRemaining) {
            finished();
        } else if (membersRemaining<0) {
            throw new IllegalStateException("A job can not have fewer than 0 active threads.");
        }
    }
    
    public TileRange getRange() {
        return tri.getTileRange();
    }

    public TileLayer getLayer() {
        return tl;
    }

    /**
     * Called when all the tasks in the job have finished running.
     */
    protected void finished(){
        if (doFilterUpdate) {
            runFilterUpdates();
        }
        double groupTotalTimeSecs = (System.currentTimeMillis() - (double) groupStartTime) / 1000;
        log.info("Job "+id+" finished " /*+ parsedType*/ + " after "
                + groupTotalTimeSecs + " seconds");
    }
    
    /**
     * Updates any request filters
     */
    private void runFilterUpdates() {
        // We will assume that all filters that can be updated should be updated
        List<RequestFilter> reqFilters = tl.getRequestFilters();
        if (reqFilters != null && !reqFilters.isEmpty()) {
            for(RequestFilter reqFilter: reqFilters) {
                if (reqFilter.update(tl, getRange().getGridSetId())) {
                    log.debug("Updated request filter " + reqFilter.getName());
                } else {
                    log.debug("Request filter " + reqFilter.getName()
                            + " returned false on update.");
                }
            }
        }
    }

    public void terminate() {
        for(GWCTask task: threads){
            synchronized(task) {
                if(task.getState()!=STATE.DEAD && task.getState()!=STATE.DONE){
                    task.terminateNicely();
                }
            }
        }
    }

    protected class ThreadedTileRequest implements TileRequest {
    
        public long[] getGridLoc() {
            return gridLoc;
        }
    
        final long gridLoc[];
        
        long retryAt;
        long failures;
        
    
        
        ThreadedTileRequest(long x, long y, long zoom) {
            super();
            this.gridLoc = new long[] {x, y, zoom};
        }
    
        public int compareTo(TileRequest o) {
            return Long.signum(this.retryAt - o.getRetryAt());
        }
    
        public long getX() {
            return gridLoc[0];
        }
    
        public long getY() {
            return gridLoc[1];
        }
    
        public long getZoom() {
            return gridLoc[2];
        }
    
        public long getRetryAt() {
            return retryAt;
        }
    
        public long getFailures() {
            return failures;
        }
    
        public Job getJob() {
            return ThreadedJob.this;
        }
        
    }

    public Collection<TaskStatus> getTaskStatus(){
        Collection<TaskStatus> taskStatuses = new ArrayList<TaskStatus>(threads.length);
        for(GWCTask task: threads) {
            taskStatuses.add(task.getStatus());
        }
        return taskStatuses;
    }
    
    /**
     * Returns the current status of the job.  Does not do any locking.
     */
    public JobStatus getStatus() {
        return new JobStatus(this);
    }
    
    /**
     * Iterates over the tasks and returns their states.
     */
    class StateIterator implements Iterator<GWCTask.STATE> {
    
        ArrayIterator it = new ArrayIterator(threads);

        public boolean hasNext() {
            return it.hasNext();
        }

        public STATE next() {
            return ((GWCTask)it.next()).getState();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    
    }
    
    public STATE getState() {
        Assert.state(threads.length>0, "Job should have at least one task.");
        return JobUtils.combineState(new StateIterator());
    }
    
    
}