package org.geowebcache.storage;

public interface TileRangeIterator {

/**
 * Returns the underlying tile range
 * 
 * @return
 */
public abstract TileRange getTileRange();

/**
 * This loops over all the possible metatile locations and returns a tile location within each
 * metatile.
 * 
 * If the TileRange object provided is a DiscontinuousTileRange implementation, each location is
 * checked against the filter of that class.
 * 
 * @return {@code null} if there're no more tiles to return, the next grid location in the
 *         iterator otherwise. The array has three elements: {x,y,z}
 */
public abstract long[] nextMetaGridLocation();

/**
 * Get the meta tiling factors for this iterator
 * @return
 */
public abstract int[] getMetaTilingFactors();

}