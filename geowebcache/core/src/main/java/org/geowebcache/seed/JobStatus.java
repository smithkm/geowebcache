package org.geowebcache.seed;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * Status of a seeding job at a particular time
 *
 */
public class JobStatus implements Serializable{

    final private Collection<TaskStatus> taskStatuses;
    final private long time;
    final private long jobId;
    final private String layerName;
    final private long threadCount;
    final private GWCTask.TYPE type;
    final private long tilesDone;
    final private long tilesTotal;
    final private GWCTask.STATE state;
    
    /**
     * create a timestamped record of the given job.
     * @param job
     */
    public JobStatus(Job job) {
        super();
        
        // FIXME requires iterating over the tasks twice.  Should try to optimise.
        this.taskStatuses = job.getTaskStatus();
        this.state = job.getState();

        this.jobId = job.getId();
        this.threadCount = job.getThreadCount();
        this.layerName = job.getLayer().getName();
        this.type = job.getType();
        this.tilesTotal = job.getRange().tileCount();
        
        long tilesDone = 0;
        for(TaskStatus task: this.taskStatuses){
            tilesDone += task.getTilesDone();
        }
        this.tilesDone = tilesDone;

        // Do this last
        this.time = System.currentTimeMillis();
    }
    
    
    public Collection<TaskStatus> getTaskStatuses() {
        return Collections.unmodifiableCollection(taskStatuses);
    }
    
    public long getTime() {
        return time;
    }
    
    public long getJobId() {
        return jobId;
    }
    
    public String getLayerName() {
        return layerName;
    }
    
    public GWCTask.TYPE getType() {
        return type;
    }
    
    public long getThreadCount() {
        return threadCount;
    }
    
    public long getTilesDone() {
        return tilesDone;
    }
    
    public long getTilesTotal() {
        return tilesTotal;
    }
    
    public GWCTask.STATE getState() {
        return state;
    }
}
