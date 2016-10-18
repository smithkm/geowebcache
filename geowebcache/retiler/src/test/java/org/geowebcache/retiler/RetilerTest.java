package org.geowebcache.retiler;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.integration.EasyMock2Adapter.adapt;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.grid.SRS;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.ImageMime;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.hamcrest.BaseMatcher;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import org.easymock.IMocksControl;
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
    public void testReprojectBboxSameSRS() throws Exception {
        SRS srs = createMock("srs", SRS.class);
        BoundingBox bbox =  new BoundingBox(1.0d, 2.0d, 3.0d, 4.0d);
        
        expect(srs.getNumber()).andStubReturn(404000);
        
        replay(srs);
        
        BoundingBox result = Retiler.reprojectBbox(bbox, srs, srs);
        
        
        assertThat(result, equalTo(bbox));
        
        verify(srs);
    }
    
    @Test
    public void testReprojectBboxDifferentSRS() throws Exception {
        SRS srs1 = createMock("srs1", SRS.class);
        SRS srs2 = createMock("srs2", SRS.class);
        
        BoundingBox bbox =  new BoundingBox(-118.38499060756d, 60.8600064154413d, -109.090105760518d, 63.0800001084841d);
        BoundingBox expected =  new BoundingBox(-1206518.69444956d, -35513.5681811823d, -684791.054153968d, 330791.957493111d);
        
        expect(srs1.getNumber()).andStubReturn(6004326);
        expect(srs2.getNumber()).andStubReturn(6042101);
        
        replay(srs1, srs2);
        
        BoundingBox result = Retiler.reprojectBbox(bbox, srs1, srs2);
        
        
        assertThat(result, equalTo(expected));
        
        verify(srs1, srs2);
    }
    
    @Test
    public void testReprojectBboxDifferentSRS2() throws Exception {
        SRS srs1 = createMock("srs1", SRS.class);
        SRS srs2 = createMock("srs2", SRS.class);
        
        BoundingBox bbox =  new BoundingBox(-1185205.40442546d, 48360.7622934747d, -691105.548875919d, 260986.86576161d);
        BoundingBox expected =  new BoundingBox(-119.090148363291d, 60.4247730085304d, -108.566718091162d, 63.6290747405827d);
        
        expect(srs1.getNumber()).andStubReturn(6042101);
        expect(srs2.getNumber()).andStubReturn(6004326);
        
        replay(srs1, srs2);
        
        BoundingBox result = Retiler.reprojectBbox(bbox, srs1, srs2);
        
        
        assertThat(result, equalTo(expected));
        
        verify(srs1, srs2);
    }
    
    @Test
    public void testNeededTiles() throws Exception {
        SRS srs1 = createMock("srs1", SRS.class);
        SRS srs2 = createMock("srs2", SRS.class);
        
        GridSubset subset = createMock("subset", GridSubset.class);
        
        BoundingBox bbox =  new BoundingBox(-1185205.40442546d, 48360.7622934747d, -691105.548875919d, 260986.86576161d);
        BoundingBox expectedBbox =  new BoundingBox(-119.090148363291d, 60.4247730085304d, -108.566718091162d, 63.6290747405827d);

        expect(srs1.getNumber()).andStubReturn(6042101);
        expect(srs2.getNumber()).andStubReturn(6004326);
        expect(subset.getSRS()).andStubReturn(srs2);
        
        expect(subset.getCoverageIntersection(4, expectedBbox)).andReturn(new long[]{5L,13L,6L,13L,4L});
        
        replay(srs1, srs2, subset);
        
        long[] tiles = Retiler.neededTiles(bbox, srs1, subset, 4);
        
        assertThat(tiles, equalTo(new long[]{5L,13L,6L,13L,4L}));
        
        verify(srs1, srs2, subset);
    }
    
    @Test
    public void testNeededTilesRealGridset() throws Exception {
        SRS srs1 = createMock("srs1", SRS.class);
        
        GridSetBroker broker = new GridSetBroker(false, false);
        GridSubset subset = GridSubsetFactory.createGridSubSet(broker.WORLD_EPSG4326);
        
        BoundingBox bbox =  new BoundingBox(-1185205.40442546d, 48360.7622934747d, -691105.548875919d, 260986.86576161d);
        
        expect(srs1.getNumber()).andStubReturn(6042101);
        
        replay(srs1);
        
        long[] tiles = Retiler.neededTiles(bbox, srs1, subset, 6);
        
        assertThat(tiles, equalTo(new long[]{21L,53L,25L,54L,6L}));
        
        verify(srs1);
    }
    
    static Matcher<long[]> tileIndex(final long x, final long y, final long z) {
        return tileIndex(is(x), is(y), is(z));
    }
    
    static Matcher<long[]> tileIndex(final long[] idx) {
        return tileIndex(idx[0], idx[1], idx[2]);
    }
    
    static Matcher<long[]> tileIndex(final Matcher<Long> x, final Matcher<Long> y, final Matcher<Long> z) {
        return new BaseMatcher<long[]>() {
            
            @Override
            public boolean matches(Object item) {
                if(item instanceof long[]) {
                    return ((long[]) item).length==3 
                            && x.matches(((long[]) item)[0])
                            && y.matches(((long[]) item)[1])
                            && z.matches(((long[]) item)[2]);
                } else {
                    return false;
                }
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("Tile index x: ")
                    .appendDescriptionOf(x)
                    .appendText(" y: ")
                    .appendDescriptionOf(y)
                    .appendText(" zoom: ")
                    .appendDescriptionOf(z);
            }
            
        };
    }
    
    ConveyorTile tile(Matcher<Object> baseConveyorMatcher, long[] tileIndex) {
        adapt(
                both(baseConveyorMatcher)
                .and(hasProperty("tileIndex",
                        tileIndex(tileIndex))));
        return null;
    }
    
    static Long[][] tileMatrix(final Long[][] matrix) {
        adapt(arrayContaining(
                Arrays.stream(matrix)
                    .map(row->
                        arrayContaining(
                            Arrays.stream(row)
                                .map(Matchers::is)
                                .collect(Collectors.toList())))
                    .collect(Collectors.toList())));
        return null;
    }
    
    @Test
    public void testGetTile() throws Exception {
        IMocksControl control = EasyMock.createControl();
        SRS srs1 = control.createMock("srs1", SRS.class);
        SRS srs2 = control.createMock("srs2", SRS.class);
        @SuppressWarnings("rawtypes")
        TileManipulator manip = control.createMock("manip", TileManipulator.class);
        TileLayer layer = control.createMock("layer", TileLayer.class);
        
        GridSubset subset = control.createMock("subset", GridSubset.class);
        GridSet gridset = control.createMock("gridset", GridSet.class);
        
        BoundingBox bbox =  new BoundingBox(-1185205.40442546d, 48360.7622934747d, -691105.548875919d, 260986.86576161d);
        BoundingBox expectedBbox =  new BoundingBox(-119.090148363291d, 60.4247730085304d, -108.566718091162d, 63.6290747405827d);

        expect(srs1.getNumber()).andStubReturn(6042101);
        expect(srs2.getNumber()).andStubReturn(6004326);
        expect(subset.getSRS()).andStubReturn(srs2);
        expect(subset.getName()).andStubReturn("WGS84Test");
        expect(gridset.getName()).andStubReturn("WGS84Test");
        expect(gridset.getSrs()).andStubReturn(srs2);
        expect(layer.getGridSubset("WGS84Test")).andStubReturn(subset);
        expect(layer.getId()).andStubReturn("TestLayer");
        expect(subset.getGridSet()).andStubReturn(gridset);
        
        expect(subset.getCoverageIntersection(6, expectedBbox))
            .andStubReturn(new long[]{21L,53L,25L,54L,6L});
        
        final Matcher<Object> baseConveyorMatcher = allOf(
                hasProperty("gridSetId", is("WGS84Test")),
                hasProperty("layerId", is("TestLayer")),
                hasProperty("mimeType", is(ImageMime.png)),
                hasProperty("fullParameters", is(Collections.emptyMap()))
                );
        double[] xWorld = {-120.9375, -118.1250, -115.3125, -112.5000, -109.6875, -106.8750};
        double[] yWorld = {61.8750, 64.6875, 59.0625};
        LongStream.rangeClosed(53,54)
            .boxed()
            .flatMap(y->LongStream.rangeClosed(21,25)
                    .mapToObj(x->new long[]{x,y,6L})
                    )
            .forEach(tileIndex->{
                final Long n = tileIndex[0]*1000+tileIndex[1];
                final ConveyorTile t = control.createMock(String.format("tile_%d_%d", tileIndex[0], tileIndex[1]), ConveyorTile.class);
                final Resource r = control.createMock(String.format("resource_%d_%d", tileIndex[0], tileIndex[1]), Resource.class);
                expect(t.getBlob()).andStubReturn(r);
                expect(subset.boundsFromIndex(aryEq(tileIndex)))
                    .andStubReturn(new BoundingBox(
                            xWorld[(int) (tileIndex[0]-21)], 
                            yWorld[(int) (tileIndex[1]-53)], 
                            xWorld[(int) (tileIndex[0]-20)], 
                            yWorld[(int) (tileIndex[1]-52)]));
                try {
                    expect(manip.load(same(r), eq(new ReferencedEnvelope(
                            xWorld[(int) (tileIndex[0]-21)],
                            xWorld[(int) (tileIndex[0]-20)],
                            yWorld[(int) (tileIndex[1]-53)],
                            yWorld[(int) (tileIndex[1]-52)],
                            CRS.decode("EPSG:6004326")
                            )))).andReturn(n);
                    expect(layer.getTile(tile(baseConveyorMatcher, tileIndex))).andReturn(t);
                } catch (GeoWebCacheException | IOException | FactoryException e) {
                    fail("This should not happen");
                }
            });
        
        final Long merged = 100L;
        final Long projected = 101L;
        final Long cropped = 102L;
        final int tileSize = 256;
        
        expect(manip.bounds(merged)).andStubReturn(new TileBounds(0,0, tileSize*2, tileSize*5));
        expect(manip.merge(tileMatrix(new Long[][]{
            {21053L, 22053L, 23053L, 24053L, 25053L},
            {21054L, 22054L, 23054L, 24054L, 25054L}})))
            .andReturn(merged);
        
        expect(manip.reproject(eq(merged), eq(CRS.decode("EPSG:6042101"))))
            .andReturn(projected);
        
        expect(manip.crop(eq(projected), eq(new TileBounds(0,0,1,1))))
            .andReturn(cropped);
        
        control.replay();
        
        Retiler retiler = new Retiler(manip);
        
        Resource res = retiler.getTile(bbox, srs1, layer, subset, ImageMime.png, Collections.emptyMap(), 6);
        
        assertThat(res, equalTo(null));
        
        control.verify();
    }

}
