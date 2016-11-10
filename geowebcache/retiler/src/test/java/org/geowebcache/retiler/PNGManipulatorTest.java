package org.geowebcache.retiler;

import static org.geotools.image.test.ImageAssert.looksLike;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.image.WorldImageReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.test.ImageAssert;
import org.geotools.image.test.ImageDialog;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.FileResource;
import org.geowebcache.io.Resource;
import org.geowebcache.util.PropertyRule;
import org.h2.util.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

public class PNGManipulatorTest {
    
    @Rule 
    public TemporaryFolder temp = new TemporaryFolder(); 
    
    @Rule public TestAuthorityRule customCRS = new TestAuthorityRule(
            PNGManipulatorTest.class.getResource("epsg.properties"));
    
    Resource imageResource;
    
    @Before
    public void resources() throws Exception {
        try( 
            InputStream is = PNGManipulatorTest.class.getResourceAsStream("512x256.png");
        ) {
            imageResource = ByteArrayResource.capture(os->IOUtils.copy(is, os));
        }
    }
    
    @Test
    public void testReproject() throws Exception {
        
        TileManipulator<GridCoverage2D> manip = new PNGManipulator();
        
        CoordinateReferenceSystem crsSource = CRS.decode("EPSG:4326",true);
        CoordinateReferenceSystem crsDest = CRS.decode("EPSG:6042101",true);
        ReferencedEnvelope envStart = new ReferencedEnvelope(-120.938, -115.312, 61.875, 64.6875, crsSource);
        ReferencedEnvelope envProjected = new ReferencedEnvelope(-1287680.3280200420413166,-924201.5628860244760290,156231.5644834840204567 ,537562.9212252628058195, crsDest);
        
        GridCoverage2DReader reader = new WorldImageReader(PNGManipulatorTest.class.getResource("2tiles-proj-crop.png"));
        
        GridCoverage2D expected = reader.read(null);
        
        GridEnvelope wantedPixels = expected.getGridGeometry().getGridRange();
        envProjected = new ReferencedEnvelope(expected.getEnvelope());
        
        GridCoverage2D tile = manip.load(imageResource, envStart);
        
        GridCoverage2D projected = manip.reproject(tile, envProjected, wantedPixels);
        
        assertThat(projected.getRenderedImage(), looksLike(expected.getRenderedImage(), 6000));
    }
    
}
