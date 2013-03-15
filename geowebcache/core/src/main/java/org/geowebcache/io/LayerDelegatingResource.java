package org.geowebcache.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.layer.TileLayer;
import org.springframework.util.Assert;

/**
 * Lazy delegate that retrieves a resource from a layer and then delegates to it.
 * 
 * @author Kevin Smith
 *
 */
public class LayerDelegatingResource implements Resource {

    private final ConveyorTile tile;
    private final TileLayer layer;
    private final long lastModified;
    
    private Resource delegate=null;
    
    /**
     * Create a new delegating resource
     * @param tile The tile to request
     * @param lastModified The time to report the resource was last changed
     */
    public LayerDelegatingResource(ConveyorTile tile, long lastModified) {
        this(tile, tile.getLayer(), lastModified);
    }
    
    /**
     * Create a new delegating resource
     * @param tile The tile to request
     * @param layer The layer to retreive the tile from
     * @param lastModified The time to report the resource was last changed
     */
    public LayerDelegatingResource(ConveyorTile tile, TileLayer layer,
            long lastModified) {
        super();
        Assert.notNull(tile);
        Assert.notNull(layer);
        this.tile = tile;
        this.layer = layer;
        this.lastModified = lastModified;
    }

    public long getSize() {
        if(delegate!=null) return delegate.getSize();
        return -1;
    }
    
    public long transferTo(WritableByteChannel channel) throws IOException {
        getDelegate();
        return delegate.transferTo(channel);
    }
    
    public long transferFrom(ReadableByteChannel channel) throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public InputStream getInputStream() throws IOException {
        getDelegate();
        return delegate.getInputStream();
    }
    
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    /**
     * Get the tile data from the back end layer.
     * @throws IOException
     */
    private void getDelegate() throws IOException {
        try {
            if(delegate==null) delegate = layer.getResourceForTile(tile);
        } catch (GeoWebCacheException ex) {
            throw new IOException("Could not retreive tile from back end.", ex);
        }
    }
}
