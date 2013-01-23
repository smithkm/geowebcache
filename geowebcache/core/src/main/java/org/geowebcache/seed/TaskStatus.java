package org.geowebcache.seed;

import java.io.Serializable;

import org.geowebcache.seed.GWCTask.STATE;

/**
 * Status of a seeding task at a particular time
 *
 */
public class TaskStatus implements Serializable {
    final private long time;
    final private long tilesDone;
    final private long tilesTotal;
    final private long timeRemaining;
    final private long timeSpent;
    final private long taskId;
    final private GWCTask.STATE state;

    /**
     * Create a timestamped record of the state of the given task.
     * @param task
     */
    public TaskStatus(GWCTask task) {
        super();
        this.time = System.currentTimeMillis();
        this.tilesDone = task.getTilesDone();
        this.tilesTotal = task.getTilesTotal();
        this.timeRemaining = task.getTimeRemaining();
        this.timeSpent = task.getTimeSpent();
        this.taskId = task.getTaskId();
        this.state = task.getState();
    }

    /**
     * The time that the status was recorded
     * @return
     */
    public long getTime() {
        return time;
    }

    /**
     * The number of tiles the task has processed
     * @return
     */
    public long getTilesDone() {
        return tilesDone;
    }

    /**
     * The number of tiles assigned to the task
     * @return
     */
    public long getTilesTotal() {
        return tilesTotal;
    }

    /**
     * The time remaining for the task
     * @return
     */
    public long getTimeRemaining() {
        return timeRemaining;
    }
    
    /**
     * The time taken so far for the task
     * @return
     */
    public long getTimeSpent() {
        return timeSpent;
    }


    /**
     * The id of the task
     * @return
     */
   public long getTaskId() {
        return taskId;
    }

   /**
    * The state of the task
    * @return
    */
    public GWCTask.STATE getState() {
        return state;
    }
    
    
}
