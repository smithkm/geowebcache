/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Arne Kepp / The Open Planning Project 2008 
 */
package org.geowebcache.seed;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheException;
import org.springframework.util.Assert;

/**
 * 
 */
public abstract class GWCTask {

    /**
     * An unknown duration
     */
    public static final long TIME_UNKNOWN = -2;

    private static final Log log = LogFactory.getLog(GWCTask.class);

    public static enum TYPE {
        UNSET, SEED, RESEED, TRUNCATE
    };

    public static enum STATE {
        /**
         * Not ready to run yet
         */
        INITIALIZING(false), 
        /**
         * Ready to run
         */
        READY(false), 
        /**
         * Currently running
         */
        RUNNING(false), 
        /**
         * Completed successfully
         */
        DONE(true), 
        /**
         * Stopped due to failure while running
         */
        FAILED(true), 
        /**
         * Stopped externally
         */
        ABORTED(true);
        
        /**
         * A state where the task is no longer running or able to run
         */
        public boolean isStopped(){
            return stopped;
        }
        
        private final boolean stopped;
        
        STATE(boolean stopped) {
            this.stopped=stopped;
        }
    };

    //protected int threadOffset = 0;

    protected final Job parentJob;
    
    protected final long taskId;

    protected final TYPE parsedType; // = TYPE.UNSET; // TODO Do we need the UNSET type?

    protected STATE state = STATE.INITIALIZING;

    // protected final String layerName;

    protected long timeSpent = -1;

    protected long timeRemaining = -1;

    protected long tilesDone = -1;

    protected long tilesTotal = -1;

    protected boolean terminate = false;
    
    //private long groupStartTime;

    
    /**
     * Create a new GWCTask.  Should only be called by implementations of {@link Job}.
     * @param taskId unique ID for the job
     * @param parentJob the job this task belongs to
     * @param parsedType the type of the job
     */
    public GWCTask(long taskId, Job parentJob, TYPE parsedType) {
        super();
        this.taskId = taskId;
        this.parentJob = parentJob;
        this.parsedType = parsedType;
    }
    
    /**
     * Marks this task as active in the Job, delegates to {@link #doActionInternal()}, and notifies
     * the Job when it stops.
     */
    public final void doAction() throws GeoWebCacheException, InterruptedException {
        Assert.state(getState()==STATE.READY, "Task can not be started as it is "+state+" rather than READY");
        if(log.isTraceEnabled()) log.trace(parsedType+" Task "+taskId+" starting.");
        state=STATE.RUNNING;
        getJob().threadStarted(this);
        try {
            doActionInternal();
        } finally {
            if(log.isTraceEnabled()) log.trace("Task "+taskId+" ending.");
            // If it's not DONE or ABORTED, it should be DEAD at this point.
            if(!getState().isStopped()){
                log.info("Task "+Long.toString(taskId)+"reached end but was still in state "+getState()+".  Setting to DEAD.");
                state=STATE.FAILED;
            }
            dispose();
            getJob().threadStopped(this);
        }
    }


    /**
     * Called when task completes, successfully or unsuccessfully
     */
    protected abstract void dispose();

    /**
     * Extension point for subclasses to do what they do
     */
    protected abstract void doActionInternal() throws GeoWebCacheException, InterruptedException;

    /**
     * Unique id of the task.  
     * 
     * This is unique across all Jobs and should not be assumed to be sequential or coupled to the job ID in any way.
     * 
     * @return
     */
    public long getTaskId() {
        return taskId;
    }

    /**
     * Get the job this task belongs to.
     * @return
     */
    public Job getJob() {
        return parentJob;
    }

    /**
     * Get the name of the layer this task is working on
     * @return
     */
    public String getLayerName() {
        return parentJob.getLayer().getName();
    }

    /**
     * @return total number of tiles (in the whole task group), or < 0 if too many to count
     */
    public long getTilesTotal() {
        return tilesTotal;
    }

    /**
     * Get the number of tiles this task has processed
     * @return
     */
    public long getTilesDone() {
        return tilesDone;
    }

    /**
     * @return estimated remaining time in seconds, or {@code TIME_UNKNOWN} if unknown
     */
    public long getTimeRemaining() {
        if (tilesTotal > 0) {
            return timeRemaining;
        } else {
            return TIME_UNKNOWN;
        }
    }

    /**
     * @return task time spent in seconds
     */
    public long getTimeSpent() {
        return timeSpent;
    }

    /**
     * Terminate this task
     */
    public void terminateNicely() {
        this.terminate = true;
    }

    /**
     * Get the type of this task
     * @return
     */
    public TYPE getType() {
        return parsedType;
    }

    /**
     * Get the current state of this task
     * @return
     */
    public STATE getState() {
        Assert.state(state!=null);
        return state;
    }

    /**
     * Check if the thread was interrupted and handle it if so.
     * @throws InterruptedException if the thread was interrupted
     */
    protected void checkInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            this.state = STATE.FAILED;
            parentJob.threadStopped(this);
            throw new InterruptedException();
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("[Task").append(getTaskId()).append(": ").append(getLayerName())
                .append(", ").append(getType()).append(", ").append(getState()).append("]")
                .toString();
    }
    
    /**
     * Get a report on the status of the task.
     * @return
     */
    public TaskStatus getStatus(){
       return new TaskStatus(this);
    }
}
