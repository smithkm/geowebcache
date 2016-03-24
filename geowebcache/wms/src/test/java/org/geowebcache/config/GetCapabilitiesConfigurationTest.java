package org.geowebcache.config;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;
import static org.easymock.classextension.EasyMock.*;

import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.easymock.Capture;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.ows.WMSRequest;
import org.geotools.data.wms.WebMapServer;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridCoverage;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.SRS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

public class GetCapabilitiesConfigurationTest {
    
    @Before
    public void setUp() throws Exception {
    }
    
    @Test
    public void testDelegateInitializingLayers() throws Exception {
        GridSetBroker broker = new GridSetBroker(false, false);
        String url = "http://test/wms";
        String mimeTypes = "image/png";
        
        final WebMapServer server = createMock(WebMapServer.class);
        WMSCapabilities cap = createMock(WMSCapabilities.class);
        WMSRequest req = createMock(WMSRequest.class);
        OperationType gcOpType = createMock(OperationType.class);
        XMLConfiguration globalConfig = createMock(XMLConfiguration.class);
        Capture<TileLayer> layerCapture = new Capture<TileLayer>();
        
        GetCapabilitiesConfiguration config = 
                new GetCapabilitiesConfiguration(broker, url, mimeTypes, "3x3", "false"){
                    
                    @Override
                    WebMapServer getWMS() {
                        return server;
                    }
            
        };
        
        expect(server.getCapabilities()).andStubReturn(cap);
        expect(cap.getRequest()).andStubReturn(req);
        expect(req.getGetCapabilities()).andStubReturn(gcOpType);
        expect(gcOpType.getGet()).andStubReturn(new URL("http://test/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=getcapabilities"));
        
        expect(cap.getVersion()).andStubReturn("1.1.1");
        
        List<Layer> layers = new LinkedList<Layer>();
        
        Layer l = new Layer();
        l.setName("Foo");
        l.setLatLonBoundingBox(new CRSEnvelope());
        layers.add(l);
        
        globalConfig.setDefaultValues(capture(layerCapture)); expectLastCall().times(layers.size());
        
        expect(cap.getLayerList()).andReturn(layers);
        
        replay(server, cap, req, gcOpType, globalConfig);
        
        config.setPrimaryConfig(globalConfig);
        
        config.initialize(broker);
        
        // Check that the XMLConfiguration's setDefaultValues method has been called on each of the layers returened.
        assertThat(Sets.newHashSet(config.getLayers()), Matchers.is(Sets.newHashSet(layerCapture.getValues())));
        
        verify(server, cap, req, gcOpType, globalConfig);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testDelegateInitializingLayersWithCRSSpecificAutoGridset() throws Exception {
        GridSetBroker broker = new GridSetBroker(false, false);
        String url = "http://test/wms";
        String mimeTypes = "image/png";
        
        final WebMapServer server = createMock(WebMapServer.class);
        WMSCapabilities cap = createMock(WMSCapabilities.class);
        WMSRequest req = createMock(WMSRequest.class);
        OperationType gcOpType = createMock(OperationType.class);
        XMLConfiguration globalConfig = createMock(XMLConfiguration.class);
        Capture<TileLayer> layerCapture = new Capture<TileLayer>();
        
        GetCapabilitiesConfiguration config = 
                new GetCapabilitiesConfiguration(broker, url, mimeTypes, "3x3", "false"){
                    
                    @Override
                    WebMapServer getWMS() {
                        return server;
                    }
            
        };
        
        expect(server.getCapabilities()).andStubReturn(cap);
        expect(cap.getRequest()).andStubReturn(req);
        expect(req.getGetCapabilities()).andStubReturn(gcOpType);
        expect(gcOpType.getGet()).andStubReturn(new URL("http://test/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=getcapabilities"));
        
        expect(cap.getVersion()).andStubReturn("1.1.1");
        
        List<Layer> layers = new LinkedList<Layer>();
        
        Layer l = new Layer();
        l.setName("Foo");
        l.setLatLonBoundingBox(new CRSEnvelope());
        l.setTitle("A Test Layer");
        l.set_abstract("Test Abstract");
        l.setBoundingBoxes(new CRSEnvelope("EPSG:3348", 4_000_000.0, 3_000_000.0, 5_000_000.0, 4_000_000.0));
        layers.add(l);
        
        globalConfig.setDefaultValues(capture(layerCapture)); expectLastCall().times(layers.size());
        
        expect(cap.getLayerList()).andReturn(layers);
        
        replay(server, cap, req, gcOpType, globalConfig);
        
        config.setPrimaryConfig(globalConfig);
        config.setLayerSpecificGridsets(false);
        
        config.initialize(broker);
        
        // Gridset was added to broker, Bounds taken from SpatialReference.org.  Actual bounds may 
        // be significantly wider due to lack of a proper bounding polygon in the GeoTools CRS 
        // database.
        assertThat(broker.get("WMSAutoConfig:EPSG:3348"), hasProperty("bounds", containsBbox(2_365_553.3321, 1_851_486.3210, 9_683_442.0814, 5_606_232.2754)));
        
        assertThat(config.getLayers(), contains(
                allOf(
                    instanceOf(WMSLayer.class),
                    hasProperty("metaInformation", 
                        allOf(
                            hasProperty("title", equalTo("A Test Layer")),
                            hasProperty("description", equalTo("Test Abstract"))
                        )
                    ),
                    hasProperty("gridSubsets",
                        containsInAnyOrder(equalTo("WMSAutoConfig:EPSG:3348"), equalTo("GlobalCRS84Geometric"), equalTo("GoogleMapsCompatible"))
                    ),
                    hasGridSubset("WMSAutoConfig:EPSG:3348", subsetCoversExactly(4_000_000.0, 3_000_000.0, 5_000_000.0, 4_000_000.0))
                )));
        
        verify(server, cap, req, gcOpType, globalConfig);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testDelegateInitializingLayersWithLayerSpecificAutoGridset() throws Exception {
        GridSetBroker broker = new GridSetBroker(false, false);
        String url = "http://test/wms";
        String mimeTypes = "image/png";
        
        final WebMapServer server = createMock(WebMapServer.class);
        WMSCapabilities cap = createMock(WMSCapabilities.class);
        WMSRequest req = createMock(WMSRequest.class);
        OperationType gcOpType = createMock(OperationType.class);
        XMLConfiguration globalConfig = createMock(XMLConfiguration.class);
        Capture<TileLayer> layerCapture = new Capture<TileLayer>();
        
        GetCapabilitiesConfiguration config = 
                new GetCapabilitiesConfiguration(broker, url, mimeTypes, "3x3", "false"){
                    
                    @Override
                    WebMapServer getWMS() {
                        return server;
                    }
            
        };
        
        expect(server.getCapabilities()).andStubReturn(cap);
        expect(cap.getRequest()).andStubReturn(req);
        expect(req.getGetCapabilities()).andStubReturn(gcOpType);
        expect(gcOpType.getGet()).andStubReturn(new URL("http://test/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=getcapabilities"));
        
        expect(cap.getVersion()).andStubReturn("1.1.1");
        
        List<Layer> layers = new LinkedList<Layer>();
        
        Layer l = new Layer();
        l.setName("Foo");
        l.setLatLonBoundingBox(new CRSEnvelope());
        l.setTitle("A Test Layer");
        l.set_abstract("Test Abstract");
        l.setBoundingBoxes(new CRSEnvelope("EPSG:3348", 4_000_000.0, 3_000_000.0, 5_000_000.0, 4_000_000.0));
        layers.add(l);
        
        globalConfig.setDefaultValues(capture(layerCapture)); expectLastCall().times(layers.size());
        
        expect(cap.getLayerList()).andReturn(layers);
        
        replay(server, cap, req, gcOpType, globalConfig);
        
        config.setPrimaryConfig(globalConfig);
        config.setLayerSpecificGridsets(true);
        
        config.initialize(broker);
        
        // Gridset was added to broker, bounds should be exactly those of the layer
        assertThat(broker.get("Foo:EPSG:3348"), hasProperty("bounds", equalTo(new BoundingBox(4_000_000.0, 3_000_000.0, 5_000_000.0, 4_000_000.0))));
        
        assertThat(config.getLayers(), contains(
                allOf(
                    instanceOf(WMSLayer.class),
                    hasProperty("metaInformation", 
                        allOf(
                            hasProperty("title", equalTo("A Test Layer")),
                            hasProperty("description", equalTo("Test Abstract"))
                        )
                    ),
                    hasProperty("gridSubsets",
                        containsInAnyOrder(equalTo("Foo:EPSG:3348"), equalTo("GlobalCRS84Geometric"), equalTo("GoogleMapsCompatible"))
                    ),
                    hasGridSubset("Foo:EPSG:3348", subsetCoversExactly(4_000_000.0, 3_000_000.0, 5_000_000.0, 4_000_000.0))
                )));
        
        verify(server, cap, req, gcOpType, globalConfig);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testDelegateInitializingLayersWithFoundGridset() throws Exception {
        GridSetBroker broker = new GridSetBroker(false, false);
        String url = "http://test/wms";
        String mimeTypes = "image/png";
        
        final WebMapServer server = createMock(WebMapServer.class);
        WMSCapabilities cap = createMock(WMSCapabilities.class);
        WMSRequest req = createMock(WMSRequest.class);
        OperationType gcOpType = createMock(OperationType.class);
        XMLConfiguration globalConfig = createMock(XMLConfiguration.class);
        Capture<TileLayer> layerCapture = new Capture<TileLayer>();
        
        GetCapabilitiesConfiguration config = 
                new GetCapabilitiesConfiguration(broker, url, mimeTypes, "3x3", "false"){
                    
                    @Override
                    WebMapServer getWMS() {
                        return server;
                    }
            
        };
        
        config.setLayerSpecificGridsets(false);
        
        expect(server.getCapabilities()).andStubReturn(cap);
        expect(cap.getRequest()).andStubReturn(req);
        expect(req.getGetCapabilities()).andStubReturn(gcOpType);
        expect(gcOpType.getGet()).andStubReturn(new URL("http://test/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=getcapabilities"));
        
        expect(cap.getVersion()).andStubReturn("1.1.1");
        
        List<Layer> layers = new LinkedList<Layer>();
        
        Layer l = new Layer();
        l.setName("Foo");
        l.setLatLonBoundingBox(new CRSEnvelope());
        l.setTitle("A Test Layer");
        l.set_abstract("Test Abstract");
        l.setBoundingBoxes(new CRSEnvelope("EPSG:3348", 4_000_000.0, 3_000_000.0, 5_000_000.0, 4_000_000.0));
        layers.add(l);
        
        globalConfig.setDefaultValues(capture(layerCapture)); expectLastCall().times(layers.size());
        
        GridSet expectedGridset = GridSetFactory.createGridSet("TestGridset", SRS.getSRS(3348), new BoundingBox(2_365_553.3321, 1_851_486.3210, 9_683_442.0814, 5_606_232.2754), false, 11, 1.0d, GridSetFactory.DEFAULT_PIXEL_SIZE_METER, 256, 256, false);
        broker.put(expectedGridset);
        
        expect(cap.getLayerList()).andReturn(layers);
        
        replay(server, cap, req, gcOpType, globalConfig);
        
        config.setPrimaryConfig(globalConfig);
        
        config.initialize(broker);
        
        // Check that the gridset we added was used.
        assertThat(config.getLayers(), contains(
                allOf(
                    instanceOf(WMSLayer.class),
                    hasProperty("metaInformation", 
                        allOf(
                            hasProperty("title", equalTo("A Test Layer")),
                            hasProperty("description", equalTo("Test Abstract"))
                        )
                    ),
                    hasProperty("gridSubsets",
                        containsInAnyOrder(equalTo("TestGridset"), equalTo("GlobalCRS84Geometric"), equalTo("GoogleMapsCompatible"))
                    ),
                    hasGridSubset("TestGridset", subsetCoversExactly(4_000_000.0, 3_000_000.0, 5_000_000.0, 4_000_000.0))
                )));
        
        verify(server, cap, req, gcOpType, globalConfig);
    }
    
    /**
     * Check that a grid subset minimaly covers a given bounding box
     * @param bbox
     * @return
     */
    Matcher<GridSubset> subsetCoversExactly(double minx, double miny, double maxx, double maxy) {
        return subsetCoversExactly(new BoundingBox(minx, miny, maxx, maxy));
    }
    
    /**
     * Check that a grid subset minimaly covers a given bounding box
     * @param bbox
     * @return
     */
    Matcher<GridSubset> subsetCoversExactly(final BoundingBox bbox) {
        return new BaseMatcher<GridSubset> () {
            @Override
            public boolean matches(Object item) {
                if(item instanceof GridSubset) {
                    GridSubset gss = (GridSubset) item;
                    return Arrays.stream(gss.getCoverages())
                        .allMatch(coverage->{
                            boolean match = true;
                            // are the expected bounds inside those of the subset at this zoom level
                            match &= gss.boundsFromRectangle(coverage).contains(bbox);
                            long[] inset = Arrays.copyOf(coverage, coverage.length);
                            
                            // Try to inset by one all around
                            boolean doInset=false;
                            if(inset[GridCoverage.MAX+GridCoverage.X]-inset[GridCoverage.MIN+GridCoverage.X]>=3) {
                                inset[GridCoverage.MIN+GridCoverage.X] += 1;
                                inset[GridCoverage.MAX+GridCoverage.X] -= 1;
                                doInset=true;
                            }
                            if(inset[GridCoverage.MAX+GridCoverage.Y]-inset[GridCoverage.MIN+GridCoverage.Y]>=3) {
                                inset[GridCoverage.MIN+GridCoverage.Y]+=1;
                                inset[GridCoverage.MAX+GridCoverage.Y]-=1;
                                doInset=true;
                            }
                            // If inset worked, check that the inset bounds don't contain the expected bounds
                            if(doInset){
                                match &= !gss.boundsFromRectangle(inset).contains(bbox);
                            }
                            return match;
                        });
                    
                } else {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("GridSubset minimally covering ").appendValue(bbox);
            }
            
        };
    }
    
    /**
     * Check that a tile layer has a grid subset for the given gridset name and matching the given matcher
     * @param bbox
     * @return
     */
    Matcher<TileLayer> hasGridSubset(final String name, final Matcher<? super GridSubset> match) {
        return new BaseMatcher<TileLayer> () {

            @Override
            public boolean matches(Object item) {
                if(item instanceof TileLayer) {
                    GridSubset subset = ((TileLayer) item).getGridSubset(name);
                    if(subset==null) {
                        return false;
                    }
                    return match.matches(subset);
                } else {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("TileLayer with grid subset for grid set ");
                description.appendValue(name);
                description.appendText(" that ");
                description.appendDescriptionOf(match);
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                if(!(item instanceof TileLayer)) {
                    description.appendValue(item);
                    description.appendText(" is not a TileLayer");
                } else if (((TileLayer) item).getGridSubset(name)==null) {
                    description.appendValue(item);
                    description.appendText(" does not have a grid subset for");
                    description.appendValue(name);
                } else {
                    description.appendValue(name);
                    description.appendText(" is present but ");
                    match.describeMismatch(((TileLayer) item).getGridSubset(name), description);
                }
            }
            
        };
    }
    
    /**
     * Check that a bounding box contains the given bounding box
     * @param bbox
     * @return
     */
    Matcher<BoundingBox> containsBbox(final BoundingBox bbox) {
        return new CustomMatcher<BoundingBox>("A bounding box containing <"+bbox+">") {

            @Override
            public boolean matches(Object item) {
                if(item instanceof BoundingBox) {
                    return ((BoundingBox) item).contains(bbox);
                } else {
                    return false;
                }
            }
            
        };
    }
    
    /**
     * Check that a bounding box contains the given bounding box
     * @param bbox
     * @return
     */
    Matcher<BoundingBox> containsBbox(double minx, double miny, double maxx, double maxy) {
        return containsBbox(new BoundingBox(minx, miny, maxx, maxy));
    }
}
