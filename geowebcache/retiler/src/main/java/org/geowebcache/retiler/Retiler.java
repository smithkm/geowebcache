package org.geowebcache.retiler;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.OutsideCoverageException;
import org.geowebcache.grid.SRS;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileObject;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

public class Retiler {
    private static Log log = LogFactory.getLog(Retiler.class);
    @SuppressWarnings("rawtypes")
    private TileManipulator manip;
    
    public Retiler(@SuppressWarnings("rawtypes") TileManipulator manip) {
        super();
        this.manip = manip;
    }

    public void retile(ConveyorTile conv, TileLayer layer) {
        BoundingBox bbox = conv.getGridSubset().boundsFromIndex(conv.getTileIndex());
        SRS requestSrs = conv.getGridSubset().getSRS();
        Set<GridSubset> layerGridSubsets = layer.getGridSubsets().stream()
                .map(layer::getGridSubset)
                .collect(Collectors.toSet());
        
        try {
            CoordinateReferenceSystem crs = CRS.decode(requestSrs.toString(), true);
        } catch (NoSuchAuthorityCodeException e) {
            log.error(String.format("Unknown SRS: %s", requestSrs.toString()), e);
        } catch (FactoryException e) {
            log.error(String.format("Could not create SRS: %s", requestSrs.toString()), e);
        }
        
    }
    
    public static CoordinateReferenceSystem srs2crs (SRS srs) 
    throws NoSuchAuthorityCodeException, FactoryException {
        return CRS.decode(String.format("EPSG:%d",srs.getNumber()), true);
    }
    
    public static CoordinateReferenceSystem gridset2crs (GridSet gridSet) 
    throws NoSuchAuthorityCodeException, FactoryException {
        return srs2crs(gridSet.getSrs());
    }
    
    public static ReferencedEnvelope reference(BoundingBox bbox, SRS srs) 
    throws NoSuchAuthorityCodeException, FactoryException {
        CoordinateReferenceSystem crs;
        
        crs = srs2crs(srs);
        
        return reference(bbox, crs);
    }
    
    public static ReferencedEnvelope reference(BoundingBox bbox, CoordinateReferenceSystem crs) 
    throws NoSuchAuthorityCodeException, FactoryException {
        
        return new ReferencedEnvelope(
                bbox.getMinX(), 
                bbox.getMaxX(), 
                bbox.getMinY(), 
                bbox.getMaxY(), 
                crs);
    }
    
    public static BoundingBox unreference(ReferencedEnvelope env) {
        return new BoundingBox(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
    }
    
    public static BoundingBox reprojectBbox(BoundingBox bbox, SRS source, SRS dest) 
    throws NoSuchAuthorityCodeException, FactoryException, TransformException {
        ReferencedEnvelope env = reference(bbox, source);
        return unreference(env.transform(srs2crs(dest), false));
    }
    
    public static BoundingBox reprojectBbox(BoundingBox bbox, CoordinateReferenceSystem source, CoordinateReferenceSystem dest) 
    throws NoSuchAuthorityCodeException, FactoryException, TransformException {
        ReferencedEnvelope env = reference(bbox, source);
        return unreference(env.transform(dest, false));
    }
    
    public static long[] neededTiles(BoundingBox bbox, SRS srs, GridSubset gridSubset, int zoom)
    throws NoSuchAuthorityCodeException, FactoryException, TransformException {
        BoundingBox neededBbox = reprojectBbox(bbox, srs, gridSubset.getSRS());
        return gridSubset.getCoverageIntersection(zoom, neededBbox);
    }
    public static long[] neededTiles(BoundingBox bbox, CoordinateReferenceSystem dest, GridSubset gridSubset, int zoom)
    throws NoSuchAuthorityCodeException, FactoryException, TransformException {
        BoundingBox neededBbox = reprojectBbox(bbox, dest, srs2crs(gridSubset.getSRS()));
        return gridSubset.getCoverageIntersection(zoom, neededBbox);
    }
    public static long[] neededTiles(ReferencedEnvelope bbox, GridSubset gridSubset, int zoom)
    throws NoSuchAuthorityCodeException, FactoryException, TransformException {
        BoundingBox neededBbox = unreference(bbox.transform(srs2crs(gridSubset.getSRS()), false));
        return gridSubset.getCoverageIntersection(zoom, neededBbox);
    }
    
    private ConveyorTile makeConveyor(String layerId, String gridSetId, long x, 
            long y, int zoom, MimeType mimeType, Map<String, String> parameters) {
        HttpServletRequest servletReq = null;
        HttpServletResponse servletResp = null;
        StorageBroker sb = null;
        ConveyorTile conv = new ConveyorTile(
                sb,
                layerId, 
                gridSetId,
                new long[]{x,y,zoom}, 
                mimeType,
                parameters,
                servletReq,
                servletResp
                );
        return conv;
    }
    
    public Resource getTile(BoundingBox bbox, GridEnvelope size, SRS srs, TileLayer layer, GridSubset gridSubset, MimeType mime, Map<String, String> parameters, int zoom)
    throws NoSuchAuthorityCodeException, FactoryException, TransformException, GeoWebCacheException, IOException {
        if(Objects.isNull(layer.getGridSubset(gridSubset.getName()))) {
            throw new IllegalArgumentException(String.format("Layer %s does not have a GridSubset %s", layer.getName(), gridSubset.getName()));
        }
        CoordinateReferenceSystem crs = srs2crs(srs);
        ReferencedEnvelope env = reference(bbox, crs);
        final long[] needed = neededTiles(bbox, crs, gridSubset, zoom);
        try{
            Object[][] tileGrid = LongStream.rangeClosed(needed[1], needed[3])
                .parallel()
                .mapToObj(y -> LongStream.rangeClosed(needed[0], needed[2])
                    .mapToObj(x -> makeConveyor(
                        layer.getId(), 
                        gridSubset.getGridSet().getName(), 
                        x, y, zoom, 
                        mime, 
                        parameters))
                    .map(t -> {
                        try {
                            return manip.load(layer.getTile(t).getBlob(),
                                    reference(
                                        gridSubset.boundsFromIndex(t.getTileIndex()),
                                        gridSubset.getSRS()));
                        } catch (GeoWebCacheException | IOException | FactoryException e) {
                            throw new RuntimeException(e); // TODO do something more specific here
                        }
                    })
                    .toArray(Object[]::new))
                .toArray(Object[][]::new);
            Object combinedTile = manip.merge(tileGrid);
            
            Object projectedTile = manip.reproject(combinedTile, env, size);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof GeoWebCacheException) {
                throw (GeoWebCacheException) e.getCause();
            } else if(e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else {
                throw e;
            }
        }
        return null;
    }
}
