package org.geowebcache.retiler;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.opengis.coverage.grid.GridEnvelope;
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
    public GridEnvelope bounds(GridCoverage2D tile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GridCoverage2D merge(GridCoverage2D[][] tiles) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GridCoverage2D reproject(GridCoverage2D tile, ReferencedEnvelope worldBounds, GridEnvelope pixelBounds) {
        GeneralGridGeometry geom = new GeneralGridGeometry(pixelBounds, worldBounds);
        GridCoverage2D projected = (GridCoverage2D) Operations.DEFAULT.resample(tile, worldBounds.getCoordinateReferenceSystem(), geom, null);
        
        return projected;
    }

    @Override
    public GridCoverage2D crop(GridCoverage2D tile, GridEnvelope bounds) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
