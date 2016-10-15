package org.geowebcache.retiler;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.FileResource;
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
    
    @FunctionalInterface
    static interface IOConsumer<T extends Closeable> {
        void accept(T t) throws IOException;
        
        default IOConsumer<T> andThen(IOConsumer<? super T> after) throws IOException {
            Objects.requireNonNull(after);
            return (T t) -> { accept(t); after.accept(t); };
        }
        
        default Consumer<T> makeSafe(Consumer<IOException> handler) {
            return os -> {
                try {
                    accept(os);
                } catch (IOException ex) {
                    handler.accept(ex);
                }
            };
        }
    }
    
    Resource captureResource(IOConsumer<OutputStream> body) throws IOException {
        File temp = File.createTempFile("tile", ".png");
        try {
            try (
                OutputStream os = new FileOutputStream(temp)
            ) {
                body.accept(os);
            }
            try (
                InputStream is = new FileInputStream(temp)
            ) {
                return new ByteArrayResource(IOUtils.toByteArray(is));
            }
        } finally {
            temp.delete();
        }
    }
    
    @Override
    public Resource save(GridCoverage2D tile) throws IOException {
        tile.getRenderedImage();
        return captureResource(os->ImageIO.write(tile.getRenderedImage(), "png", os));
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
    public GridCoverage2D reproject(GridCoverage2D tile, CoordinateReferenceSystem dest) {
        GridCoverageFactory gcf = new GridCoverageFactory();
        gcf.create("", tile, gridEnvelope)
        org.geotools.coverage.grid.io.AbstractGridCoverage2DReader.
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GridCoverage2D crop(GridCoverage2D tile, TileBounds bounds) {
        // TODO Auto-generated method stub
        return null;
    }
    
}
