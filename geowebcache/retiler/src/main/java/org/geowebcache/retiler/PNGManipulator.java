package org.geowebcache.retiler;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.Operations;
import org.geotools.coverage.processing.operation.Mosaic;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class PNGManipulator implements TileManipulator<GridCoverage2D> {
    
    GridCoverageFactory gcf = new GridCoverageFactory();
    
    private final CoverageProcessor processor = CoverageProcessor.getInstance(GeoTools
            .getDefaultHints());
    
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
        int width = tiles[0][0].getGridGeometry().gridDimensionX*tiles[0].length;
        int height = tiles[0][0].getGridGeometry().gridDimensionY*tiles.length;
        GridEnvelope gridEnv = new GeneralGridEnvelope(new int[]{0, 0}, new int[]{width, height});
        
        // Get the envelopes of tiles from opposite corners and find the overall envelope
        // Assumes tiles are arranged in a rectangular matrix
        ReferencedEnvelope worldEnv = new ReferencedEnvelope(tiles[0][0].getEnvelope());
        worldEnv.expandToInclude(new ReferencedEnvelope(tiles[tiles.length-1][tiles[tiles.length-1].length-1].getEnvelope()));
        
        return merge(tiles, worldEnv, gridEnv);
    }
    
    public GridCoverage2D merge(GridCoverage2D[][] tiles, ReferencedEnvelope worldBounds, GridEnvelope pixelBounds) {
        ParameterValueGroup param = processor.getOperation("Mosaic").getParameters();
        
        param.parameter(Mosaic.SOURCES_NAME).setValue(
                Arrays.stream(tiles)
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList()));
        GeneralGridGeometry geom = new GeneralGridGeometry(pixelBounds, worldBounds);
        param.parameter(Mosaic.GEOMETRY).setValue(geom);
        
        return (GridCoverage2D) processor.doOperation(param);
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
