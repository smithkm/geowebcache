package org.geowebcache.retiler;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.*;

import java.lang.annotation.Annotation;

import org.geotools.referencing.CRS;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestAuthorityRuleTest {
    
    @Rule public ExpectedException exception = ExpectedException.none();
    
    @Test
    public void testAddsAuthority() throws Throwable {
        TestAuthorityRule rule = new TestAuthorityRule(TestAuthorityRuleTest.class.getResource("epsg.properties"));
        
        rule.apply(new Statement(){
            @Override
            public void evaluate() throws Throwable {
                CoordinateReferenceSystem crs = CRS.decode("EPSG:6042101");
                assertThat(crs, hasProperty("name",hasProperty("code",equalTo("WGS 84 / LCC Canada TEST"))));
            }
            
        }, Description.createTestDescription("TEST", "TEST", new Annotation[]{}))
        .evaluate();
        
    }
    
    @Test
    public void testRemovesAuthority() throws Throwable {
        TestAuthorityRule rule = new TestAuthorityRule(TestAuthorityRuleTest.class.getResource("epsg.properties"));
        
        rule.apply(new Statement(){
            @Override
            public void evaluate() throws Throwable {
                CoordinateReferenceSystem crs = CRS.decode("EPSG:6042101");
                assertThat(crs, hasProperty("name",hasProperty("code",equalTo("WGS 84 / LCC Canada TEST"))));
            }
            
        }, Description.createTestDescription("TEST", "TEST", new Annotation[]{}))
        .evaluate();
        exception.expect(NoSuchAuthorityCodeException.class);
        CRS.decode("EPSG:6042101");
    }
    
    @Test
    public void testAuthorityNotAddedEarly() throws Exception {
        TestAuthorityRule rule = new TestAuthorityRule(TestAuthorityRuleTest.class.getResource("epsg.properties"));
        
        rule.apply(new Statement(){
            @Override
            public void evaluate() throws Throwable {
                fail();
            }
            
        }, Description.createTestDescription("TEST", "TEST", new Annotation[]{}));
        
        // Don't run evaluate() here
        
        exception.expect(NoSuchAuthorityCodeException.class);
        CRS.decode("EPSG:6042101");
    }
    
    @Test
    public void testDoesntSuppressDefaults() throws Throwable {
        TestAuthorityRule rule = new TestAuthorityRule(TestAuthorityRuleTest.class.getResource("epsg.properties"));
        
        CoordinateReferenceSystem crs = CRS.decode("CRS:84");
        assertThat(crs, hasProperty("name",hasProperty("code",equalTo("WGS84"))));
        
        rule.apply(new Statement(){
            @Override
            public void evaluate() throws Throwable {
                CoordinateReferenceSystem crs = CRS.decode("CRS:84");
                assertThat(crs, hasProperty("name",hasProperty("code",equalTo("WGS84"))));
            }
            
        }, Description.createTestDescription("TEST", "TEST", new Annotation[]{}))
        .evaluate();
        
        crs = CRS.decode("CRS:84");
        assertThat(crs, hasProperty("name",hasProperty("code",equalTo("WGS84"))));
    }
}
