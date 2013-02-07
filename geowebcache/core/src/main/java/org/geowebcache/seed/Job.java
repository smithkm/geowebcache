package org.geowebcache.seed;

import java.util.Collection;

import org.geowebcache.layer.TileLayer;
import org.geowebcache.storage.TileRange;

public interface Job {

    /**
     * Get the job ID number of this job.
     * 
     * This is unique, but may not be sequential or or otherwise meaningful.
     * @return
     */
    public long getId();

    /**
     * Get the next location to work on.
     * @return 
     */
    public TileRequest getNextLocation() throws InterruptedException ;
    

    
    /**
     * Get the worker tasks for this job.
     * @return
     */
    //public GWCTask[] getTasks();
    
    /**
     * Stop this job
     */
    public void terminate();
    
    /**
     * Get the number of tasks working on this job
     * @return
     */
    public long getThreadCount();
    
    /**
     * Notify the job that one of its threads has started
     * @param thread
     * @throws IllegalArgumentException the task is not a ready task from this Job.
     */
    public void threadStarted(GWCTask thread);
    
    /**
     * Notify the job that one of its threads has ended
     * @param thread
     * @throws IllegalArgumentException the task is not a running task from this Job.
     */
    public void threadStopped(GWCTask thread);
    

    /**
     * Get the tile breeder for this job.
     * @return
     */
    public TileBreeder getBreeder();
    
    
    /**
     * Get the layer this Job is working on.
     * @return
     */
    public TileLayer getLayer();
    
    /**
     * Get the range of tiles this Job is working on.
     * @return
     */
    public TileRange getRange();

    /**
     * Get a report of the status of the job
     * @return
     */
    public JobStatus getStatus();
    
    /**
     * Get a report of the status of the job
     * @param maxAge Age in milliseconds.  If the status has been generated within this time, use it
     * rather than generating again.
     * @return
     */
    public JobStatus getStatus(long maxAge);
    
    /**
     * Get a report of the status of each task in the job
     * @return
     */
    public Collection<TaskStatus> getTaskStatus();
    
    /**
     * Get the current state of the job, based on the states of its respective tasks.
     * @return
     */
    public GWCTask.STATE getState();
    
    /**
     * Get the type of the job.  All its tasks will also have this type.
     * @return
     */
    public GWCTask.TYPE getType();
    
    /**
     * Wait for the job to stop
     * @return A JobStatus reporting the state of the job when it stopped.
     * @throws InterruptedException if the waiting thread was interrupted
     */
    public JobStatus waitForStop() throws InterruptedException;
    
    /**
     * Wait for the job to complete successfully
     * @return A JobStatus reporting the state of the job when it stopped.
     * @throws InterruptedException if the waiting thread was interrupted
     * @throws InterruptedException if the waiting thread was interrupted
     */
    public JobStatus waitForComplete() throws InterruptedException, JobFailedException;
}
