package org.geowebcache.retiler;

public interface Bounds<Size extends Number> {
    Size getMinX();
    Size getMinY();
    Size getMaxX();
    Size getMaxY();
}
