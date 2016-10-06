package org.geowebcache.retiler;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.SRS;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.hamcrest.Matchers;

import org.easymock.classextension.EasyMock;

public class RetilerTest {
    
    @Rule public TestAuthorityRule customCRS = new TestAuthorityRule(
            RetilerTest.class.getResource("epsg.properties"));
    
    @Test
    public void testGridset2crs() throws Exception {
        GridSet gs = createMock("gs", GridSet.class);
        SRS srs = createMock("srs", SRS.class);
        
        expect(gs.getSrs()).andStubReturn(srs);
        expect(srs.getNumber()).andStubReturn(404000);
        
        replay(gs, srs);
        
        CoordinateReferenceSystem crs = Retiler.gridset2crs(gs);
        
        assertThat(crs, equalTo(CRS.decode("EPSG:404000")));
        
        verify(gs, srs);
    }
    
    @Test
    public void testSrs2crs() throws Exception {
        SRS srs = createMock("srs", SRS.class);
        
        expect(srs.getNumber()).andStubReturn(404000);
        
        replay(srs);
        
        CoordinateReferenceSystem crs = Retiler.srs2crs(srs);
        
        assertThat(crs, equalTo(CRS.decode("EPSG:404000")));
        
        verify(srs);
    }
    
    @Test
    public void testReference() throws Exception {
        SRS srs = createMock("srs", SRS.class);
        BoundingBox bbox =  new BoundingBox(1.0d, 2.0d, 3.0d, 4.0d);
        
        expect(srs.getNumber()).andStubReturn(404000);
        
        replay(srs);
        
        ReferencedEnvelope env = Retiler.reference(bbox, srs);
        
        ReferencedEnvelope expected = new ReferencedEnvelope(1.0d, 3.0d, 2.0d, 4.0d, CRS.decode("EPSG:404000"));
        
        assertThat(env, equalTo(expected));
        
        verify(srs);
    }
    @Test
    public void testUnreference() throws Exception {
        ReferencedEnvelope env = new ReferencedEnvelope(1.0d, 3.0d, 2.0d, 4.0d, CRS.decode("EPSG:404000"));
        
        SRS srs = createMock("srs", SRS.class);
        BoundingBox expected =  new BoundingBox(1.0d, 2.0d, 3.0d, 4.0d);
        
        replay(srs);
        
        BoundingBox result = Retiler.unreference(env);
        
        assertThat(result, equalTo(expected));
        
        verify(srs);
    }
    @Test
    public void testUnreference2() throws Exception {
        ReferencedEnvelope env = new ReferencedEnvelope(1.0d, 3.0d, 2.0d, 4.0d, CRS.decode("EPSG:6042101"));
        
        SRS srs = createMock("srs", SRS.class);
        BoundingBox expected =  new BoundingBox(1.0d, 2.0d, 3.0d, 4.0d);
        
        replay(srs);
        
        BoundingBox result = Retiler.unreference(env);
        
        assertThat(result, equalTo(expected));
        
        verify(srs);
    }

}
