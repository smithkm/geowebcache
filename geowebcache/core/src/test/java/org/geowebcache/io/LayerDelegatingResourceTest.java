package org.geowebcache.io;

import static org.junit.Assert.*;
import static org.easymock.classextension.EasyMock.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.InputStream;
import java.nio.channels.WritableByteChannel;

import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.layer.TileLayer;
import org.junit.Before;
import org.junit.Test;

public class LayerDelegatingResourceTest {

    TileLayer mockLayer;
    ConveyorTile mockTile;
    Resource mockResource;
    
    @Before
    public void setUp() throws Exception {
        mockLayer = createMock(TileLayer.class);
        mockTile = createMock(ConveyorTile.class);
        mockResource = createMock(Resource.class);
    }
    
    @Test
    public void testTransferTo() throws Exception  {
        WritableByteChannel mockChannel = createMock(WritableByteChannel.class);
        
        expect(mockTile.getLayer()).andStubReturn(mockLayer);
        expect(mockLayer.getResourceForTile(mockTile)).andReturn(mockResource);
        expect(mockResource.transferTo(mockChannel)).andReturn(1l);
        
        replay(mockLayer, mockTile, mockResource, mockChannel);
        
        Resource delegate = new LayerDelegatingResource(mockTile, 0);
        delegate.transferTo(mockChannel);
        
        verify(mockLayer, mockTile, mockResource, mockChannel);
    }
    
    @Test
    public void testGetInputStream() throws Exception  {
        InputStream mockStream = createMock(InputStream.class);
        
        expect(mockTile.getLayer()).andStubReturn(mockLayer);
        expect(mockLayer.getResourceForTile(mockTile)).andReturn(mockResource);
        expect(mockResource.getInputStream()).andReturn(mockStream);
        
        replay(mockLayer, mockTile, mockResource, mockStream);
        
        Resource delegate = new LayerDelegatingResource(mockTile, 0);
        assertThat(delegate.getInputStream(), is(mockStream));
        
        verify(mockLayer, mockTile, mockResource, mockStream);
    }


}
