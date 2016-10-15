package org.geowebcache.retiler;

/**
 * A bounding box within the internal coordinate space of a tile.  Pixels for a raster or pseudo-pixels for a vector tile. 
 * @author Kevin Smith, Boundless
 *
 */
public class TileBounds {
    
    final int minX;
    final int minY;
    final int maxX;
    final int maxY;
    
    public TileBounds(int minX, int minY, int maxX, int maxY) {
        super();
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public int getMinX() {
        return minX;
    }
    
    public int getMinY() {
        return minY;
    }
    
    public int getMaxX() {
        return maxX;
    }
    
    public int getMaxY() {
        return maxY;
    }
    
    public int getWidth() {
        return getMaxX()-getMinX();
    }
    
    public int getHeight() {
        return getMaxY()-getMinY();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + maxX;
        result = prime * result + maxY;
        result = prime * result + minX;
        result = prime * result + minY;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TileBounds other = (TileBounds) obj;
        if (maxX != other.maxX)
            return false;
        if (maxY != other.maxY)
            return false;
        if (minX != other.minX)
            return false;
        if (minY != other.minY)
            return false;
        return true;
    }
}
