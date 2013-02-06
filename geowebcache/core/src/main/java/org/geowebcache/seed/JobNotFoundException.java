package org.geowebcache.seed;

/**
 * Thrown when a job lookup fails.
 *
 */
public class JobNotFoundException extends SeederException {

    public JobNotFoundException(long id) {
        super(String.format("No job found with id %d.", id));
    }

}
