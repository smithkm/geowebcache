package org.geowebcache.seed;

/**
 * Thrown when waiting for a job to complete if the job fails.
 */
public class JobFailedException extends SeederException {
    final GWCTask.STATE state;
    final long id;
    
    public JobFailedException(long id, GWCTask.STATE state) {
        super("Job "+id+" "+state); // TODO should show the final state
        this.state = state;
        this.id=id;
    }
    
    /**
     * Get the final status of the job.
     * @return
     */
    public GWCTask.STATE getState() {
        return state;
    }
    
    /**
     * Get the final status of the job.
     * @return
     */
    public long getId(){
        return id;
    }
}
