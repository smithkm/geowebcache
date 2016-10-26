package org.geowebcache.retiler;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class PNGManipulator implements TileManipulator<GridCoverage2D> {
    
    GridCoverageFactory gcf = new GridCoverageFactory();
    
    @Override
    public GridCoverage2D load(Resource res, ReferencedEnvelope extent) throws IOException {
        try (
            InputStream is = res.getInputStream()
        ) {
            BufferedImage image = ImageIO.read(is);
            return gcf.create("Tile", image, extent);
        }
    }
    
    @Override
    public Resource save(GridCoverage2D tile) throws IOException {
        tile.getRenderedImage();
        return ByteArrayResource.capture(os->ImageIO.write(tile.getRenderedImage(), "png", os));
    }

    @Override
    public TileBounds bounds(GridCoverage2D tile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GridCoverage2D merge(GridCoverage2D[][] tiles) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GridCoverage2D reproject(GridCoverage2D tile, ReferencedEnvelope worldBounds, TileBounds pixelBounds) {
        return null;
    }

    @Override
    public GridCoverage2D crop(GridCoverage2D tile, TileBounds bounds) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
