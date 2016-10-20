package org.geowebcache.retiler;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.io.FileResource;
import org.geowebcache.io.Resource;
import org.h2.util.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class PNGManipulatorTest {
    
    @Rule 
    public TemporaryFolder temp = new TemporaryFolder(); 
    
    @Rule public TestAuthorityRule customCRS = new TestAuthorityRule(
            PNGManipulatorTest.class.getResource("epsg.properties"));
    
    Resource imageResource;
    
    @Before
    public void resources() throws Exception {
        File imageFile = temp.newFile("512x256.png");
        try( 
            InputStream is = PNGManipulatorTest.class.getResourceAsStream("512x256.png");
            OutputStream os = new FileOutputStream(imageFile);
        ) {
            IOUtils.copy(is, os);
        }
        imageResource = new FileResource(imageFile);
    }
    
    @Test
    public void test() throws Exception {
        
        TileManipulator<GridCoverage2D> manip = new PNGManipulator();
        
        CoordinateReferenceSystem crsSource = CRS.decode("EPSG:4326",true);
        CoordinateReferenceSystem crsDest = CRS.decode("EPSG:6042101",true);
        ReferencedEnvelope env = new ReferencedEnvelope(-120.938, -115.312, 61.875, 64.6875, crsSource);
        
        GridCoverage2D tile = manip.load(imageResource, env);
        GridCoverage2D projected = (GridCoverage2D) Operations.DEFAULT.resample(tile, crsDest);
        System.out.println(projected.getEnvelope2D());
    }
    
}
