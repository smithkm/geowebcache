package org.geowebcache.retiler;

import java.io.IOException;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geowebcache.io.Resource;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public interface TileManipulator<TileType> {
    /**
     * Load a tile from a resource
     * @param res
     * @return
     */
    TileType load(Resource res, ReferencedEnvelope extent) throws IOException;
    /**
     * Save a tile into a resource
     * @param tile
     * @return
     */
    Resource save(TileType tile) throws IOException;
    
    /**
     * Find the bounds of the internal space of the tile.  (For instance, pixels for a raster tile)
     * @param tile
     * @return
     */
    TileBounds bounds(TileType tile);
    
    /**
     * 
     * @param tiles
     * @return
     */
    TileType merge(TileType[][] tiles);
    
    /**
     * Reproject a tile
     * @param tile
     * @param source
     * @param dest
     * @return
     */
    TileType reproject(TileType tile, CoordinateReferenceSystem dest);
    
    TileType crop(TileType tile, TileBounds bounds);
}
