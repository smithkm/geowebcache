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
 * @author Marius Suta / The Open Planning Project 2008 (original code from SeedRestlet)
 * @author Arne Kepp / The Open Planning Project 2009 (original code from SeedRestlet)
 * @author Gabriel Roldan / OpenGeo 2010
 * @author Kevin Smith / OpenGeo 2012
 */

package org.geowebcache.seed;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.SRS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.geowebcache.seed.threaded.ThreadedTileBreeder;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileRange;
import org.geowebcache.storage.TileRangeIterator;
import org.geowebcache.util.GWCVars;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.springframework.util.Assert;

/**
 * Class in charge of dispatching seed/truncate tasks.
 * <p>
 * As of version 1.2.4a+, it is possible to control how GWC behaves in the event that a backend (WMS
 * for example) request fails during seeding, using the following environment variables:
 * <ul>
 * <li>{@code GWC_SEED_RETRY_COUNT}: specifies how many times to retry a failed request for each
 * tile being seeded. Use {@code 0} for no retries, or any higher number. Defaults to {@code 0}
 * retry meaning no retries are performed. It also means that the defaults to the other two
 * variables do not apply at least you specify a higher value for GWC_SEED_RETRY_COUNT;
 * <li>{@code GWC_SEED_RETRY_WAIT}: specifies how much to wait before each retry upon a failure to
 * seed a tile, in milliseconds. Defaults to {@code 100ms};
 * <li>{@code GWC_SEED_ABORT_LIMIT}: specifies the aggregated number of failures that a group of
 * seeding threads should reach before aborting the seeding operation as a whole. This value is
 * shared by all the threads launched as a single thread group; so if the value is {@code 10} and
 * you launch a seed task with four threads, when {@code 10} failures are reached by all or any of
 * those four threads the four threads will abort the seeding task. The default is {@code 1000}.
 * </ul>
 * These environment variables can be established by any of the following ways, in order of
 * precedence:
 * <ol>
 * <li>As a Java environment variable: for example {@code java -DGWC_SEED_RETRY_COUNT=5 ...};
 * <li>As a Servlet context parameter: for example
 * 
 * <pre>
 * <code>
 *   &lt;context-param&gt;
 *    &lt;!-- milliseconds between each retry upon a backend request failure --&gt;
 *    &lt;param-name&gt;GWC_SEED_RETRY_WAIT&lt;/param-name&gt;
 *    &lt;param-value&gt;500&lt;/param-value&gt;
 *   &lt;/context-param&gt;
 * </code>
 * </pre>
 * 
 * In the web application's {@code WEB-INF/web.xml} configuration file;
 * <li>As a System environment variable:
 * {@code export GWC_SEED_ABORT_LIMIT=2000; <your usual command to run GWC here>}
 * </ol>
 * </p>
 * 
 * @author Kevin Smith, based on Gabriel Roldan's class, now renamed {@link ThreadedTileBreeder}
 */
public abstract class TileBreeder {
    private static final String GWC_SEED_ABORT_LIMIT = "GWC_SEED_ABORT_LIMIT";

    private static final String GWC_SEED_RETRY_WAIT = "GWC_SEED_RETRY_WAIT";

    private static final String GWC_SEED_RETRY_COUNT = "GWC_SEED_RETRY_COUNT";

    private TileLayerDispatcher layerDispatcher;
    
    private StorageBroker storageBroker;

    protected Map<Long, Job> jobs = new HashMap<Long,Job>();

    /**
     * How many retries per failed tile. 0 = don't retry, 1 = retry once if failed, etc
     */
    protected int tileFailureRetryCount = 0;

    /**
     * How much (in milliseconds) to wait before trying again a failed tile
     */
    protected long tileFailureRetryWaitTime = 100;

    /**
     * How many failures to tolerate before aborting the seed task. Value is shared between all the
     * threads of the same run.
     */
    protected long totalFailuresBeforeAborting = 1000;
    
    public static Log log = LogFactory.getLog(ThreadedTileBreeder.class);

    /**
     * Find the tile range for a Seed Request.
     * @param req
     * @param tl 
     * @return
     * @throws GeoWebCacheException
     */
    // FIXME Might make more sense to be on SeedRequest?
    public static TileRange createTileRange(SeedRequest req, TileLayer tl)
            throws GeoWebCacheException {
        int zoomStart = req.getZoomStart().intValue();
        int zoomStop = req.getZoomStop().intValue();
    
        MimeType mimeType = null;
        String format = req.getMimeFormat();
        if (format == null) {
            mimeType = tl.getMimeTypes().get(0);
        } else {
            try {
                mimeType = MimeType.createFromFormat(format);
            } catch (MimeException e4) {
                e4.printStackTrace();
            }
        }
    
        String gridSetId = req.getGridSetId();
    
        if (gridSetId == null) {
            SRS srs = req.getSRS();
            List<GridSubset> crsMatches = tl.getGridSubsetsForSRS(srs);
            if (!crsMatches.isEmpty()) {
                if (crsMatches.size() == 1) {
                    gridSetId = crsMatches.get(0).getName();
                } else {
                    throw new IllegalArgumentException(
                            "More than one GridSubet matches the requested SRS " + srs
                                    + ". gridSetId must be specified");
                }
            }
        }
        if (gridSetId == null) {
            gridSetId = tl.getGridSubsets().iterator().next();
        }
    
        GridSubset gridSubset = tl.getGridSubset(gridSetId);
    
        if (gridSubset == null) {
            throw new GeoWebCacheException("Unknown grid set " + gridSetId);
        }
    
        long[][] coveredGridLevels;
    
        BoundingBox bounds = req.getBounds();
        if (bounds == null) {
            coveredGridLevels = gridSubset.getCoverages();
        } else {
            coveredGridLevels = gridSubset.getCoverageIntersections(bounds);
        }
    
        int[] metaTilingFactors = tl.getMetaTilingFactors();
    
        coveredGridLevels = gridSubset.expandToMetaFactors(coveredGridLevels, metaTilingFactors);
    
        String layerName = tl.getName();
        Map<String, String> parameters = req.getParameters();
        return new TileRange(layerName, gridSetId, zoomStart, zoomStop, coveredGridLevels,
                mimeType, parameters);
    }

    /**
     * Create and dispatch tasks to fulfil a seed request
     * 
     * @param layerName
     * @param sr
     * @throws GeoWebCacheException
     */
    // TODO: The SeedRequest specifies a layer name. Would it make sense to use that instead of including one as a separate parameter?
    public void seed(String layerName, SeedRequest sr)
            throws GeoWebCacheException{
        TileLayer tl = findTileLayer(layerName);
    
        TileRange tr = createTileRange(sr, tl);
    
        Job job = createJob(tr, tl, sr.getType(), sr.getThreadCount(),
                sr.getFilterUpdate());
    
        dispatchJob(job);
    }
    
    /**
     * Create tasks to manipulate the cache (Seed, truncate, etc)  They will still need to be dispatched.
     * 
     * @param tr The range of tiles to work on.
     * @param type The type of task(s) to create
     * @param threadCount The number of threads to use, forced to 1 if type is TRUNCATE
     * @param filterUpdate // TODO: What does this do?
     * @return Array of tasks.  Will have length threadCount or 1.
     * @throws GeoWebCacheException
     */
    public Job createJob(TileRange tr, GWCTask.TYPE type,
            int threadCount, boolean filterUpdate) throws GeoWebCacheException{

        String layerName = tr.getLayerName();
        TileLayer tileLayer = layerDispatcher.getTileLayer(layerName);
        return createJob(tr, tileLayer, type, threadCount, filterUpdate);
    }
    

    /**
     * Dispatches a Job
     * 
     * @param job
     */
    public abstract void dispatchJob(Job job);
    
    /**
     * Method returns List of Strings representing the status of the currently running and scheduled
     * threads
     * 
     * @return array of {@code [[tilesDone, tilesTotal, tilesRemaining, taskID, taskStatus],...]}
     *         where {@code taskStatus} is one of:
     *         {@code 0 = PENDING, 1 = RUNNING, 2 = DONE, -1 = ABORTED}
     * @deprecated
     */
    public abstract long[][] getStatusList();
    
    /**
     * Method returns List of Strings representing the status of the currently running and scheduled
     * threads for a specific layer.
     * 
     * @return array of {@code [[tilesDone, tilesTotal, tilesRemaining, taskID, taskStatus],...]}
     *         where {@code taskStatus} is one of:
     *         {@code 0 = PENDING, 1 = RUNNING, 2 = DONE, -1 = ABORTED}
     * @param layerName the name of the layer.  null for all layers.
     * @return
     * @deprecated
     */
    public abstract long[][] getStatusList(String layerName);
    
    /**
     * Get the status of currently running or scheduled Jobs.
     * @return
     */
    public abstract Collection<JobStatus> getJobStatusList();
    
    /**
     * Get the status of currently running or scheduled Jobs on a specific layer.
     * @param layerName the name of the layer.  null for all layers.
     * @return
     */
    public abstract Collection<JobStatus> getJobStatusList(String layerName);
    
    /**
     * Get the status of currently running or scheduled Tasks.
     * @return
     */
    public abstract Collection<TaskStatus> getTaskStatusList();
    
    /**
     * Get the status of currently running or scheduled Tasks on a specific layer.
     * @param layerName the name of the layer.  null for all layers.
     * @return
     */
    public abstract Collection<TaskStatus> getTaskStatusList(String layerName);
    
    public void setTileLayerDispatcher(
            TileLayerDispatcher tileLayerDispatcher){
        this.layerDispatcher = tileLayerDispatcher;
    }
    
    /**
     * @return
     */
    public TileLayerDispatcher getTileLayerDispatcher(){
        return this.layerDispatcher;
    }
    
    public abstract void setThreadPoolExecutor(SeederThreadPoolExecutor stpe);
    
    /**
     * Set the StorageBroker this Breeder is seeding into.
     */
    public void setStorageBroker(StorageBroker sb){
        this.storageBroker = sb;
    }
    
    /**
     * Get the StorageBroker this Breeder is seeding into.
     */
    public StorageBroker getStorageBroker(){
        return storageBroker;
    }
    
    /**
     * Find a layer by name.
     * @param layerName
     * @return
     * @throws GeoWebCacheException
     */
    public TileLayer findTileLayer(String layerName) throws GeoWebCacheException {
        TileLayer layer = null;
    
        layer = getTileLayerDispatcher().getTileLayer(layerName);
    
        if (layer == null) {
            throw new GeoWebCacheException("Uknown layer: " + layerName);
        }
    
        return layer;
    }
    
    /**
     * Get all tasks that are running
     * @return
     */
    public abstract Iterator<GWCTask> getRunningTasks();
    
    /**
     * Get all tasks that are running or waiting to run.
     * @return
     */
    public abstract Iterator<GWCTask> getRunningAndPendingTasks();
    
    /**
     * Get all tasks that are waiting to run.
     * @return
     */
    public abstract Iterator<GWCTask> getPendingTasks();
    
    /**
     * Terminate a running or pending task
     * @param id
     * @return
     */
    public abstract boolean terminateGWCTask(long id);
    
    /**
     * Get an iterator over the layers.
     * @return
     */
    public abstract Iterable<TileLayer> getLayers();
    
    protected Callable<GWCTask> wrapTask(GWCTask task) {
        Assert.isTrue(this == task.parentJob.getBreeder());
        return new MTSeeder(task);
    }
    
    protected abstract SeedJob createSeedJob(int threadCount, boolean reseed,
            TileRangeIterator trIter, TileLayer tl, boolean filterUpdate);

    protected abstract TruncateJob createTruncateJob(TileRangeIterator trIter, TileLayer tl,
            boolean filterUpdate);

    protected void setTaskState(GWCTask task, GWCTask.STATE state){
        Assert.isTrue(this == task.parentJob.getBreeder());
        task.state = state;
    }
    
    /**
     * Returns a unique ID for a new task.
     * @return
     */
    protected abstract long getNextTaskId();
    
    /**
     * Creates a new SeedTask for the specified Job
     * @param job
     * @return
     */
    protected SeedTask createSeedTask(SeedJob job) {
        return new SeedTask(getNextTaskId(), job);
    }
    
    /**
     * Creates a new TruncateTask for the specified Job
     * @param job
     * @return
     */
    protected TruncateTask createTruncateTask(TruncateJob job) {
        return new TruncateTask(getNextTaskId(), job);
    }

    /**
     * Create a job to manipulate the cache (Seed, truncate, etc).  In will still need to be dispatched.
     * 
     * @param tr The range of tiles to work on.
     * @param tl The layer to work on.  Overrides any layer specified on tr.
     * @param type The type of task(s) to create
     * @param threadCount The number of threads to use, forced to 1 if type is TRUNCATE
     * @param filterUpdate // TODO: What does this do?
     * @return Array of tasks.  Will have length threadCount or 1.
     * @throws GeoWebCacheException
     */
    public Job createJob(TileRange tr, TileLayer tl, GWCTask.TYPE type,
            int threadCount, boolean filterUpdate) throws GeoWebCacheException {
            
                if (type == GWCTask.TYPE.TRUNCATE || threadCount < 1) {
                    log.trace("Forcing thread count to 1");
                    threadCount = 1;
                }
                TileRangeIterator trIter = tr.iterator(tl.getMetaTilingFactors());
                Job job;
                switch (type) {
                case TRUNCATE:
                    job = createTruncateJob(trIter, tl, filterUpdate);
                    break;
                case SEED:
                    job = createSeedJob(threadCount, false, trIter, tl, filterUpdate);
                    break;
                case RESEED:
                    job = createSeedJob(threadCount, true, trIter, tl, filterUpdate);
                    break;
                default:
                    throw new IllegalArgumentException("Only SEED, RESEED, and TRUNCATE job types can be created.");
                }
                jobs.put(job.getId(), job);
                return job;
            }

    /**
     * Initializes the seed task failure control variables either with the provided environment
     * variable values or their defaults.
     * 
     * @see {@link ThreadedTileBreeder class' javadocs} for more information
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String retryCount = GWCVars.findEnvVar(applicationContext, GWC_SEED_RETRY_COUNT);
        String retryWait = GWCVars.findEnvVar(applicationContext, GWC_SEED_RETRY_WAIT);
        String abortLimit = GWCVars.findEnvVar(applicationContext, GWC_SEED_ABORT_LIMIT);
    
        tileFailureRetryCount = (int) toLong(GWC_SEED_RETRY_COUNT, retryCount, 0);
        tileFailureRetryWaitTime = toLong(GWC_SEED_RETRY_WAIT, retryWait, 100);
        totalFailuresBeforeAborting = toLong(GWC_SEED_ABORT_LIMIT, abortLimit, 1000);
    
        checkPositive(tileFailureRetryCount, GWC_SEED_RETRY_COUNT);
        checkPositive(tileFailureRetryWaitTime, GWC_SEED_RETRY_WAIT);
        checkPositive(totalFailuresBeforeAborting, GWC_SEED_ABORT_LIMIT);
    }

    @SuppressWarnings("serial")
    private void checkPositive(long value, String variable) {
        if (value < 0) {
            throw new BeanInitializationException(
                    "Invalid configuration value for environment variable " + variable
                            + ". It should be a positive integer.") {
            };
        }
    }

    private long toLong(String varName, String paramVal, long defaultVal) {
        if (paramVal == null) {
            return defaultVal;
        }
        try {
            return Long.valueOf(paramVal);
        } catch (NumberFormatException e) {
            log.warn("Invalid environment parameter for " + varName + ": '" + paramVal
                    + "'. Using default value: " + defaultVal);
        }
        return defaultVal;
    }

    /**
     * Get the job with the given ID
     * @param id
     * @return
     * @throws JobNotFoundException if the ID is not being used.
     */
    public Job getJobByID(long id) throws JobNotFoundException {
        Job j = jobs.get(id);
        if (j==null) throw new JobNotFoundException(id);
        return j;
    }
}