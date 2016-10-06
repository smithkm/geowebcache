package org.geowebcache.retiler;

import org.geowebcache.io.Resource;

public interface TileManipulator<TileType, Size extends Number> {
    /**
     * Load a tile from a resource
     * @param res
     * @return
     */
    TileType load(Resource res);
    /**
     * Save a tile into a resource
     * @param tile
     * @return
     */
    Resource save(TileType tile);
    
    /**
     * Find the bounds of the internal space of the tile.  (For instance, pixels for a raster tile)
     * @param tile
     * @return
     */
    Bounds<Size> bounds(TileType tile);
    
    /**
     * 
     * @param tiles
     * @return
     */
    TileType merge(TileType[][] tiles);
}
