package org.geowebcache.seed;

import org.geowebcache.GeoWebCacheException;

/**
 * Thrown when a job lookup fails.
 *
 */
public class JobNotFoundException extends GeoWebCacheException {

    public JobNotFoundException(long id) {
        super(String.format("No job found with id %d.", id));
    }

}
