package org.geowebcache.grid;

import java.util.Arrays;

/**
 * Represents the extent of the data for a particular level
 */
public class GridCoverage {
    // The extent of the data. {minx,miny,maxx,maxy,zoomlevel}
    protected long[] coverage;

    public GridCoverage(long[] coverage) {
        // TODO: should check that the coverage has exactly 5 elements.
        this.coverage = coverage;
    }
    
    public static final int X = 0;
    public static final int Y = 1;
    public static final int MIN = 0;
    public static final int MAX = 2;
    public static final int Z = 4;

    /**
     * Find the intersection of the given rectangle with the coverage
     * @param rectangle Array of long, minx,miny,maxx,maxy,level, in tile coordinates.
     * @return Array of long representing the intersection, minx,miny,maxx,maxy,level, in tile coordinates 
     */
    protected long[] getIntersection(long[] rectangle) {
        // TODO: should check that the rectangle has 5 elements and that the fifth (level) matches that of the coverage
        long[] ret = { Math.min(Math.max(coverage[0], rectangle[0]), coverage[2]),
                Math.min(Math.max(coverage[1], rectangle[1]), coverage[3]),
                Math.min(Math.max(coverage[0], rectangle[2]), coverage[2]),
                Math.min(Math.max(coverage[1], rectangle[3]), coverage[3]), rectangle[4] };

        return ret;
    }

    public String toString() {
        return Arrays.toString(coverage);
    }
    
    public long getMinX() {
        return coverage[MIN+X];
    }
    public long getMinY() {
        return coverage[MIN+Y];
    }
    public long getMaxX() {
        return coverage[MAX+X];
    }
    public long getMaxY() {
        return coverage[MAX+Y];
    }
    public long getZoom() {
        return coverage[Z];
    }
    
    public long[] toArray() {
        return Arrays.copyOf(coverage, coverage.length);
    }
    public long[] toArrayNoZoom() {
        return Arrays.copyOf(coverage, coverage.length-1);
    }
}
