package org.geowebcache.config;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.ows.WMSRequest;
import org.geotools.data.wms.WebMapServer;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.wms.WMSLayer;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Before;
import org.opengis.filter.IncludeFilter;

public class GetCapabilitiesLayerConfigurationConformanceTest extends LayerConfigurationTest {
    
    private GridSetBroker broker;
    
    @Before
    public void setupBroker() {
        if( broker==null) {
            broker = new GridSetBroker(false, false);
        }
    }
    
    @Override
    protected void doModifyInfo(TileLayer info, int rand) throws Exception {
        ((WMSLayer)info).setWmsLayers(Integer.toString(rand));
    }

    @Override
    protected TileLayer getGoodInfo(String id, int rand) throws Exception {
        return new WMSLayer(id, new String[] {"http://foo"}, "style", Integer.toString(rand),
                Collections.<String>emptyList(), Collections.<String,GridSubset>singletonMap("EPSG:4326", GridSubsetFactory.createGridSubSet(broker.getWorldEpsg4326())),
                Collections.<ParameterFilter>emptyList(), new int[] {3,3}, "",
                false, null);
    }
    /*(String layerName, String[] wmsURL, String wmsStyles, String wmsLayers,
            List<String> mimeFormats, Map<String, GridSubset> subSets,
            List<ParameterFilter> parameterFilters, int[] metaWidthHeight, String vendorParams,
            boolean queryable, String wmsQueryLayers)*/
    
    @Override
    protected TileLayer getBadInfo(String id, int rand) throws Exception {
        Assume.assumeFalse(true);
        return null;
    }

    @Override
    protected String getExistingInfo() {
        return "testExisting";
    }

    @Override
    protected TileLayerConfiguration getConfig() throws Exception {
        WebMapServer server;
        WMSCapabilities cap;
        WMSRequest req;
        OperationType gcOpType;
        DefaultingConfiguration globalConfig;
        server = createNiceMock(WebMapServer.class);
        cap = createNiceMock(WMSCapabilities.class);
        req = createNiceMock(WMSRequest.class);
        gcOpType = createNiceMock(OperationType.class);
        globalConfig = createNiceMock(DefaultingConfiguration.class);
        setupBroker();
        
        Layer l = new Layer();
        l.setName("testExisting");
        l.setLatLonBoundingBox(new CRSEnvelope());
        List<Layer> layers = new LinkedList<Layer>();
        layers.add(l);
        expect(cap.getLayerList()).andReturn(layers);

        expect(server.getCapabilities()).andStubReturn(cap);
        expect(cap.getRequest()).andStubReturn(req);
        expect(req.getGetCapabilities()).andStubReturn(gcOpType);
        expect(gcOpType.getGet()).andStubReturn(new URL("http://test/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=getcapabilities"));
        expect(cap.getVersion()).andStubReturn("1.1.1");
        EasyMock.replay(server, cap, req, gcOpType, globalConfig);
        
        GetCapabilitiesConfiguration config =
                new GetCapabilitiesConfiguration(broker,  "http://test/wms", "image/png", "3x3", "", null, "false"){

                    @Override
                    WebMapServer getWMS() {
                        return server;
                    }

                };
        config.setGridSetBroker(broker);
        config.initialize();
        
        return config;
    }

    @Override
    protected Matcher<TileLayer> infoEquals(TileLayer expected) {
        return Matchers.allOf(
                Matchers.hasProperty("name", equalTo(expected.getName())),
                Matchers.hasProperty("wmsLayers", equalTo(((WMSLayer) expected).getWmsLayers()))
                );
    }
    @Override
    protected Matcher<TileLayer> infoEquals(int expected) {
        return Matchers.hasProperty("wmsLayers", equalTo(expected));
    }

    @Override
    public void failNextRead() {
        Assume.assumeFalse(true);
    }

    @Override
    public void failNextWrite() {
        Assume.assumeFalse(true);
    }

    @Override
    protected void renameInfo(TileLayerConfiguration config, String name1, String name2)
            throws Exception {
        Assume.assumeFalse(true);
    }

    @Override
    protected void addInfo(TileLayerConfiguration config, TileLayer info) throws Exception {
        // TODO Auto-generated method stub
        Assume.assumeFalse(true);
    }

    @Override
    protected void removeInfo(TileLayerConfiguration config, String name) throws Exception {
        Assume.assumeFalse(true);
    }

    @Override
    protected void modifyInfo(TileLayerConfiguration config, TileLayer info) throws Exception {
        Assume.assumeFalse(true);
    }

    @Override
    public void testCanSaveGoodInfo() throws Exception {
        // Should not be able to save anything as it is read only
        assertThat(config.canSave(getGoodInfo("test", 1)), equalTo(false));
    }
}