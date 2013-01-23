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
    
    
    public JobStatus(Collection<TaskStatus> taskStatuses, long time, long jobId, long threadCount, String layerName, GWCTask.TYPE type) {
        super();
        this.taskStatuses = taskStatuses;
        this.time = time;
        this.jobId = jobId;
        this.threadCount = threadCount;
        this.layerName = layerName;
        this.type = type;
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
}
