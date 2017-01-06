package org.geowebcache.jetty;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.custommonkey.xmlunit.XMLUnit;
import org.hamcrest.Matcher;
import org.hamcrest.BaseMatcher;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.matchers.Times;
import org.mockserver.mockserver.MockServer;
import org.mockserver.verify.VerificationTimes;
import org.slf4j.LoggerFactory;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Integration test for the REST API in a full GWC instance
 * 
 * @author Kevin Smith, Boundless
 *
 */
public class StorageIT {
    @ClassRule
    static public JettyRule jetty = new JettyRule();
    
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this);
    
    private MockServerClient mockServerClient;
    
    @Rule
    public HttpClientRule anonymous = HttpClientRule.anonymous();
    @Rule
    public HttpClientRule admin = new HttpClientRule("geowebcache", "secured", "admin");
    
    
    private SimpleNamespaceContext nsContext;
    
    @Before
    public void setUp() {
        nsContext = new SimpleNamespaceContext();
        nsContext.bindNamespaceUri("atom", "http://www.w3.org/2005/Atom");
        nsContext.bindNamespaceUri("wmts", "http://www.opengis.net/wmts/1.0");
        nsContext.bindNamespaceUri("ows", "http://www.opengis.net/ows/1.1");
        assertThat(LoggerFactory.getLogger(MockServer.class).isInfoEnabled(), is(false));
    }
    
    Matcher<Node> hasXPath(final String xpathExpr, final Matcher<String> matcher) {
        return Matchers.hasXPath(xpathExpr, nsContext, matcher);
        
    }
    
    Matcher<Node> hasXPath(final String xpathExpr) {
        return Matchers.hasXPath(xpathExpr, nsContext);
    }
    
    Matcher<File> exists() {
        return new CustomMatcher<File>("File that exists"){
            
            @Override
            public boolean matches(Object item) {
                return ((File)item).exists();
            }
            
        };
    }
    
    @Test
    public void testSimpleCaching() throws Exception {
        final String layerName = "testLayer";
        final String url = "http://localhost:"+mockServerRule.getPort()+"/wms?";
        final String layers = "remoteLayer";
        final String format = "image/png";
        final String gridset = "EPSG:4326"; 
        
        createLayer(layerName, url, layers, response->{
            assertThat(response, status(200));
        });
        getCapabilities(response->{
            assertThat(response, status(200));
        });
        
        // Mock the back end WMS service
        try(InputStream metatileIS = StorageIT.class.getResourceAsStream("metatile.png")) {
            mockServerClient
                .when(request()
                    .withMethod("GET")
                    .withPath("/wms")
                    .withQueryStringParameter("REQUEST", "GetMap")
                    .withQueryStringParameter("FORMAT", format)
                    .withQueryStringParameter("WIDTH", "768")
                    .withQueryStringParameter("HEIGHT", "768")
                    )
                .respond(response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", "image/png")
                    .withBody(IOUtils.toByteArray(metatileIS)));
        }
        
        getTileWMTS(layerName, format, gridset, 6,3,3, response->{
            assertThat(response, status(200));
            assertThat(response, header("geowebcache-cache-result", equalTo("MISS")));
            assertThat(response, header("Content-Type",equalTo(format)));
        });
        
        mockServerClient
        .verify(request()
            .withMethod("GET")
            .withPath("/wms")
            .withQueryStringParameter("REQUEST", "GetMap")
            .withQueryStringParameter("FORMAT", format)
            .withQueryStringParameter("WIDTH", "768")
            .withQueryStringParameter("HEIGHT", "768")
            , VerificationTimes.once());
        mockServerClient.clear(null);
        
        File layerCacheDir = new File(jetty.getCacheDir(), layerName);
        assertThat(layerCacheDir, exists());
        
        // The tile requested
        File tileFile1 = new File(layerCacheDir, "EPSG_4326_06/00_03/0003_0060.png");
        assertThat(tileFile1, exists());
        // Another tile in the same metatile
        File tileFile2 = new File(layerCacheDir, "EPSG_4326_06/00_03/0004_0060.png");
        assertThat(tileFile2, exists());
        
        // Request that tile again
        getTileWMTS(layerName, format, gridset, 6,3,3, response->{
            assertThat(response, status(200));
            assertThat(response, header("geowebcache-cache-result", equalTo("HIT")));
            assertThat(response, header("Content-Type", equalTo(format)));
        });
        
        // The preceding two request should not have caused interaction with the back end.
        mockServerClient.verifyZeroInteractions(); 
        
        // Request another tile in the same metatile
        getTileWMTS(layerName, format, gridset, 6,4,3, response->{
            assertThat(response, status(200));
            assertThat(response, header("geowebcache-cache-result", equalTo("HIT")));
            assertThat(response, header("Content-Type",equalTo(format)));
        });
        
        // The preceding request should not have caused interaction with the back end.
        mockServerClient.verifyZeroInteractions(); 
        
    }
    
    @Test
    public void testAddParameterFilter() throws Exception {
        final String layerName = "testLayer2";
        final String url = "http://localhost:"+mockServerRule.getPort()+"/wms?";
        final String layers = "remoteLayer2";
        final String format = "image/png";
        final String gridset = "EPSG:4326"; 
        
        createLayer(layerName, url, layers, response->{
            assertThat(response, status(200));
        });
        getCapabilities(response->{
            assertThat(response, status(200));
        });
        
        // Mock the back end WMS service
        try(InputStream metatileIS = StorageIT.class.getResourceAsStream("metatile.png")) {
            mockServerClient
                .when(request()
                    .withMethod("GET")
                    .withPath("/wms")
                    .withQueryStringParameter("REQUEST", "GetMap")
                    .withQueryStringParameter("FORMAT", format)
                    .withQueryStringParameter("WIDTH", "768")
                    .withQueryStringParameter("HEIGHT", "768")
                    )
                .respond(response()
                    .withStatusCode(200)
                    .withHeader("Content-Type", "image/png")
                    .withBody(IOUtils.toByteArray(metatileIS)));
        }
        
        getTileWMTS(layerName, format, gridset, 6,3,3, response->{
            assertThat(response, status(200));
            assertThat(response, header("geowebcache-cache-result", equalTo("MISS")));
            assertThat(response, header("Content-Type",equalTo(format)));
        });
        
        mockServerClient
        .verify(request()
            .withMethod("GET")
            .withPath("/wms")
            .withQueryStringParameter("REQUEST", "GetMap")
            .withQueryStringParameter("FORMAT", format)
            .withQueryStringParameter("WIDTH", "768")
            .withQueryStringParameter("HEIGHT", "768")
            , VerificationTimes.once());
        mockServerClient.clear(null);
        
        File layerCacheDir = new File(jetty.getCacheDir(), layerName);
        assertThat(layerCacheDir, exists());
        
        // The tile requested
        File tileFile1 = new File(layerCacheDir, "EPSG_4326_06/00_03/0003_0060.png");
        assertThat(tileFile1, exists());
        // Another tile in the same metatile
        File tileFile2 = new File(layerCacheDir, "EPSG_4326_06/00_03/0004_0060.png");
        assertThat(tileFile2, exists());
        
        // Update
        {
            final HttpPost request = new HttpPost(jetty.getUri().resolve("rest/layers/").resolve(layerName+".xml"));
            request.setEntity(new StringEntity("<wmsLayer><name>"+layerName+"</name><wmsUrl><string>"+url+"</string></wmsUrl><wmsLayers>"+layers+"</wmsLayers><parameterFilters><stringParameterFilter><key>STYLES</key><defaultValue>foo</defaultValue><values><string>bar</string></values></stringParameterFilter></parameterFilters></wmsLayer>", ContentType.APPLICATION_XML));
            try( CloseableHttpResponse response = admin.getClient().execute(request)) {
                assertThat(response, status(200));
            }
        }
        
        // The preceding request should not have caused interaction with the back end.
        mockServerClient.verifyZeroInteractions();
        
        // The tile requested
        assertThat(tileFile1, not(exists()));
        // Another tile in the same metatile
        assertThat(tileFile2, not(exists()));
    }
    
    protected void getTileWMTS(final String layerName, String format, String tileMatrixSet, int zoom, int x, int y, Assertions<HttpResponse> assertions) throws Exception {
        doGet("service/wmts?REQUEST=GetTile&LAYER="+layerName+"&FORMAT="+format+"&TILEMATRIXSET="+tileMatrixSet+"&TILEMATRIX="+tileMatrixSet+":"+zoom+"&TILEROW="+y+"&TILECOL="+x, 
                anonymous.getClient(), 
                assertions);
    }

    protected void getCapabilities(Assertions<HttpResponse> assertions) throws Exception {
        doGet("service/wmts?REQUEST=GetCapabilities", 
                anonymous.getClient(), 
                assertions);
    }

    protected void createLayer(final String layerName, final String url, final String layers, Assertions<HttpResponse> assertions)
            throws Exception {
        final HttpPut request = new HttpPut(jetty.getUri().resolve("rest/layers/").resolve(layerName+".xml"));
        request.setEntity(new StringEntity("<wmsLayer><name>"+layerName+"</name><wmsUrl><string>"+url+"</string></wmsUrl><wmsLayers>"+layers+"</wmsLayers></wmsLayer>", ContentType.APPLICATION_XML));
        try( CloseableHttpResponse response = admin.getClient().execute(request)) {
            assertions.accept(response);
        }
    }
    
    interface Assertions<T> {
        public void accept(T result) throws Exception;
    }
    
    Matcher<HttpResponse> status(int code) {
        return Matchers.describedAs("HttpResponse with status code %0", 
                    hasProperty("statusLine", 
                            hasProperty("statusCode", 
                                    equalTo(code))), 
                    code);
    }
    Matcher<HttpResponse> header(String name, Matcher<String> value) {
        return new BaseMatcher<HttpResponse>() {
            
            @Override
            public boolean matches(Object item) {
                if(item instanceof HttpResponse) {
                    if(!((HttpResponse) item).containsHeader(name)) {
                        return false;
                    }
                    // Multiple headers of the same name are equivalent to their comma separated 
                    // joined values 
                    return value.matches(mergeHeaders(name, item));
                } else {
                    return false;
                }
            }
            
            protected String mergeHeaders(String name, Object item) {
                return Arrays.stream(((HttpResponse) item).getHeaders(name))
                    .map(Header::getValue)
                    .collect(Collectors.joining(","));
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("HttpResponse with header ")
                    .appendValue(name)
                    .appendText(" with value ")
                    .appendDescriptionOf(value);
            }
            
            @Override
            public void describeMismatch(Object item, Description description) {
                if(item instanceof HttpResponse) {
                    if(!((HttpResponse) item).containsHeader(name)) {
                        description.appendText("does not have header ").appendValue(name);
                    } else {
                        description.appendText("the value of ").appendValue(name).appendText(" ");
                        value.describeMismatch(mergeHeaders(name, item), description);
                    }
                } else {
                    description.appendText("not an HttpResonse");
                } 
            }
        }; 
    }
    
    void doGetXML(String uri, CloseableHttpClient client, Matcher<Integer> statusMatcher, Assertions<Document> body) throws Exception {
        doGetXML(URI.create(uri), client, statusMatcher, body);
    }
    
    void doGet(String uri, CloseableHttpClient client, Assertions<HttpResponse> body) throws Exception {
        doGet(URI.create(uri), client, body);
    }
    
    void doGet(URI uri, CloseableHttpClient client, Assertions<HttpResponse> body) throws Exception {
        final HttpGet request = new HttpGet(jetty.getUri().resolve(uri));
        try( CloseableHttpResponse response = client.execute(request);) {
            body.accept(response);
        }
    }
    
    void doGetXML(URI uri, CloseableHttpClient client, Matcher<Integer> statusMatcher, Assertions<Document> body) throws Exception {
        final HttpGet request = new HttpGet(jetty.getUri().resolve(uri));
        final Document doc;
        try( CloseableHttpResponse response = client.execute(request);
             InputStream in = response.getEntity().getContent()) {
            doc = XMLUnit.buildTestDocument(new InputSource(in));
            assertThat(response.getStatusLine(), hasProperty("statusCode",statusMatcher));
        }
        body.accept(doc);
    }
}
