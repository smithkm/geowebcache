package org.geowebcache.seed;

import org.geowebcache.seed.GWCTask.STATE;

/**
 * Status of a seeding task at a particular time
 *
 */
public class TaskStatus {
    final private long time;
    final private long tilesDone;
    final private long tilesTotal;
    final private long timeRemaining;
    final private long timeSpent;
    final private long taskId;
    final private GWCTask.STATE state;

    /**
     * 
     * @param time
     * @param tilesDone
     * @param tilesTotal
     * @param timeRemaining
     * @param taskId
     * @param state
     */
    public TaskStatus(long time, long tilesDone, long tilesTotal,
            long timeRemaining, long timeSpent, long taskId, STATE state) {
        super();
        this.time = time;
        this.tilesDone = tilesDone;
        this.tilesTotal = tilesTotal;
        this.timeRemaining = timeRemaining;
        this.timeSpent = timeSpent;
        this.taskId = taskId;
        this.state = state;
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
