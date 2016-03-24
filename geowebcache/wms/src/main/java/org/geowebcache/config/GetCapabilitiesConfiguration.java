/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * @author Arne Kepp, The Open Planning Project, Copyright 2008
 *  
 */
package org.geowebcache.config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.measure.quantity.Length;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.StyleImpl;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.xml.Dimension;
import org.geotools.data.wms.xml.Extent;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.config.meta.ServiceInformation;
import org.geowebcache.filter.parameters.NaiveWMSDimensionFilter;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.grid.SRS;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.meta.MetadataURL;
import org.geowebcache.layer.wms.WMSHttpHelper;
import org.geowebcache.layer.wms.WMSLayer;
import org.geowebcache.util.Optionals;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


public class GetCapabilitiesConfiguration implements Configuration {
    private static Log log = LogFactory
            .getLog(org.geowebcache.config.GetCapabilitiesConfiguration.class);

    private GridSetBroker gridSetBroker;

    private String url = null;

    private int backendTimeout = 120;

    private String mimeTypes = null;

    private String metaTiling = null;

    private String vendorParameters = null;

    private boolean allowCacheBypass = false;

    private final HashMap<String, TileLayer> layers;

    private XMLConfiguration primaryConfig;

    private boolean layerSpecificGridsets = true;

    public GetCapabilitiesConfiguration(GridSetBroker gridSetBroker, String url, String mimeTypes,
            String metaTiling, String allowCacheBypass) {
        this.gridSetBroker = gridSetBroker;
        this.url = url;
        this.mimeTypes = mimeTypes;
        this.metaTiling = metaTiling;
        this.layers = new HashMap<String, TileLayer>();
        if (Boolean.parseBoolean(allowCacheBypass)) {
            this.allowCacheBypass = true;
        }
        log.info("Constructing from url " + url);
    }

    public GetCapabilitiesConfiguration(GridSetBroker gridSetBroker, String url, String mimeTypes,
            String metaTiling, String vendorParameters, String allowCacheBypass) {
        this.gridSetBroker = gridSetBroker;
        this.url = url;
        this.mimeTypes = mimeTypes;
        this.metaTiling = metaTiling;
        this.vendorParameters = vendorParameters;
        this.layers = new HashMap<String, TileLayer>();
        if (Boolean.parseBoolean(allowCacheBypass)) {
            this.allowCacheBypass = true;
        }
        log.info("Constructing from url " + url);
    }

    /**
     * Optionally used by Spring
     * 
     * @param backendTimeout
     */
    public void setBackendTimeout(int backendTimeout) {
        this.backendTimeout = backendTimeout;
    }
    
    public void setLayerSpecificGridsets(boolean layerSpecificGridsets) {
        this.layerSpecificGridsets = layerSpecificGridsets;
    }
    /**
     * Identifier for this Configuration instance
     * 
     * @return the URL given to the constructor
     */
    public String getIdentifier() {
        return url;
    }

    /**
     * Gets the XML document and parses it, creates WMSLayers for the relevant
     * 
     * @return the layers described at the given URL
     */
    private synchronized List<TileLayer> getTileLayers(boolean reload) throws GeoWebCacheException {
        List<TileLayer> layers = null;

        WebMapServer wms = getWMS();
        if (wms == null) {
            throw new ConfigurationException("Unable to connect to " + this.url);
        }

        String wmsUrl = getWMSUrl(wms);
        log.info("Using " + wmsUrl + " to generate URLs for WMS requests");

        String urlVersion = parseVersion(url);

        layers = getLayers(wms, wmsUrl, urlVersion);

        if (layers == null || layers.size() < 1) {
            log.error("Unable to find any layers based on " + url);
        } else {
            log.info("Loaded " + layers.size() + " layers from " + url);
        }

        return layers;
    }

    public ServiceInformation getServiceInformation() {
        return null;
    }

    /**
     * Finds URL to WMS service and attempts to slice away the service parameter, since we will add
     * that anyway.
     * 
     * @param wms
     * @return
     */
    private String getWMSUrl(WebMapServer wms) {
        // // http://sigma.openplans.org:8080/geoserver/wms?SERVICE=WMS&
        String wmsUrl = wms.getCapabilities().getRequest().getGetCapabilities().getGet().toString();
        int queryStart = wmsUrl.lastIndexOf("?");
        if (queryStart > 0) {

            String preQuery = wmsUrl.substring(queryStart);
            if (preQuery.equalsIgnoreCase("?service=wms&")) {
                wmsUrl = wmsUrl.substring(0, wmsUrl.lastIndexOf("?"));
            }
        }
        return wmsUrl;
    }

    private List<TileLayer> getLayers(WebMapServer wms, String wmsUrl, String urlVersion)
            throws GeoWebCacheException {
        List<TileLayer> layers = new LinkedList<TileLayer>();
        
        WMSCapabilities capabilities = wms.getCapabilities();
        if (capabilities == null) {
            throw new ConfigurationException("Unable to get capabitilies from " + wmsUrl);
        }
        
        for(Layer layer: capabilities.getLayerList()) {
            
            String name = layer.getName();

            boolean queryable = layer.isQueryable();

            if (name != null) {
                String stylesStr = getStyles(layer);

                double minX = layer.getLatLonBoundingBox().getMinX();
                double minY = layer.getLatLonBoundingBox().getMinY();
                double maxX = layer.getLatLonBoundingBox().getMaxX();
                double maxY = layer.getLatLonBoundingBox().getMaxY();

                BoundingBox bounds4326 = new BoundingBox(minX, minY, maxX, maxY);

                log.info("Found layer: " + layer.getName() + " with LatLon bbox "
                        + bounds4326.toString());

                BoundingBox bounds3785 = new BoundingBox(longToSphericalMercatorX(minX),
                        latToSphericalMercatorY(minY), longToSphericalMercatorX(maxX),
                        latToSphericalMercatorY(maxY));

                String[] wmsUrls = { wmsUrl };

                LinkedList<ParameterFilter> paramFilters = getParameterFilters(layer);

                WMSLayer wmsLayer = null;
                try {
                    wmsLayer = getLayer(name, wmsUrls, bounds4326, bounds3785, stylesStr,
                            queryable, layer.getBoundingBoxes(), paramFilters);
                } catch (GeoWebCacheException gwc) {
                    log.error("Error creating " + layer.getName() + ": " + gwc.getMessage());
                }

                if (wmsLayer != null) {

                    // Finalize with some defaults
                    wmsLayer.setCacheBypassAllowed(allowCacheBypass);
                    wmsLayer.setBackendTimeout(backendTimeout);

                    wmsLayer.setMetaInformation(getLayerMetaInformation(layer));

                    getVersion(urlVersion, capabilities).ifPresent(wmsLayer::setVersion);
                    
                    wmsLayer.setSourceHelper(new WMSHttpHelper());

                    getMetadataUrls(layer).ifPresent(wmsLayer::setMetadataURLs);

                    layers.add(wmsLayer);
                }
            }
        }

        return layers;
    }

    /**
     * @param layer
     * @return
     */
    private LayerMetaInformation getLayerMetaInformation(Layer layer) {
        String title = layer.getTitle();

        String description = layer.get_abstract();

        LayerMetaInformation layerMetaInfo = null;
        if (title != null || description != null) {
            layerMetaInfo = new LayerMetaInformation(title, description, null, null);
        }
        return layerMetaInfo;
    }

    /**
     * @param layer
     * @return
     */
    private Optional<List<MetadataURL>> getMetadataUrls(Layer layer) {
        return Optional.ofNullable(layer.getMetadataURL())
            .map(l->l.stream()
                    .map(metadataURL->
                        new MetadataURL(
                                metadataURL.getType(), 
                                metadataURL.getFormat(), 
                                metadataURL.getUrl()))
                    .collect(Collectors.toList()));
    }

    /**
     * @param urlVersion
     * @param capabilities
     * @return
     */
    private Optional<String> getVersion(String urlVersion,
            WMSCapabilities capabilities) {
        final Optional<String> version;
        if (urlVersion != null) {
            version = Optional.of(urlVersion);
        } else {
            version = Optional.ofNullable(capabilities.getVersion());
        }
        version.filter((((Predicate<String>)String::isEmpty).negate()));
        return version;
    }

    /**
     * @param layer
     * @return
     */
    private LinkedList<ParameterFilter> getParameterFilters(Layer layer) {
        LinkedList<ParameterFilter> paramFilters = new LinkedList<ParameterFilter>();
        for (Dimension dimension : layer.getDimensions().values()) {
            Extent dimExtent = layer.getExtent(dimension.getName());
            paramFilters.add(new NaiveWMSDimensionFilter(dimension, dimExtent));
        }
        return paramFilters;
    }

    /**
     * @param layer
     * @param stylesStr
     * @return
     */
    public String getStyles(Layer layer) {
        List<StyleImpl> styles = layer.getStyles();

        StringBuffer buf = new StringBuffer();
        if (styles != null) {
            Iterator<StyleImpl> iter = styles.iterator();
            boolean hasOne = false;
            while (iter.hasNext()) {
                if (hasOne) {
                    buf.append(",");
                }
                buf.append(iter.next().getName());
                hasOne = true;
            }
            return buf.toString();
        } else {
            return "";
        }
    }

    private WMSLayer getLayer(String name, String[] wmsurl, BoundingBox bounds4326,
            BoundingBox bounds3785, String stylesStr, boolean queryable,
            Map<String, CRSEnvelope> additionalBounds, List<ParameterFilter> paramFilters)
            throws GeoWebCacheException {

        Hashtable<String, GridSubset> grids = getGrids(name, bounds4326,
                bounds3785, additionalBounds);

        List<String> mimeFormats = getMimeFormats();

        String[] metaStrings = this.metaTiling.split("x");

        int[] metaWidthHeight = { Integer.parseInt(metaStrings[0]),
                Integer.parseInt(metaStrings[1]) };

        return new WMSLayer(name, wmsurl, stylesStr, name, mimeFormats, grids, paramFilters,
                metaWidthHeight, this.vendorParameters, queryable, null);
    }
    
    /**
     * @param name
     * @param bounds4326
     * @param bounds3785
     * @param additionalBounds
     * @return
     * @throws GeoWebCacheException
     */
    private Hashtable<String, GridSubset> getGrids(String name,
            BoundingBox bounds4326, BoundingBox bounds3785,
            Map<String, CRSEnvelope> additionalBounds)
            throws GeoWebCacheException {
        Hashtable<String, GridSubset> grids = new Hashtable<String, GridSubset>(2);
        grids.put(gridSetBroker.WORLD_EPSG4326.getName(),
                GridSubsetFactory.createGridSubSet(gridSetBroker.WORLD_EPSG4326, bounds4326, 0, 30));
        grids.put(gridSetBroker.WORLD_EPSG3857.getName(),
                GridSubsetFactory.createGridSubSet(gridSetBroker.WORLD_EPSG3857, bounds3785, 0, 30));
        
        if (additionalBounds != null && additionalBounds.size() > 0) {
            for(CRSEnvelope env : additionalBounds.values()) {
                final SRS srs;
                if (env.getEPSGCode() != null) {
                    srs = SRS.getSRS(env.getEPSGCode());
                } else {
                    srs = null;
                }

                if (srs == null) {
                    log.error(env.toString() + " has no EPSG code");
                } else if (srs.getNumber() == 4326 || srs.getNumber() == 900913
                        || srs.getNumber() == 3857) {
                    log.debug("Skipping " + srs.toString() + " for " + name);
                } else {
                    BoundingBox extent = new BoundingBox(env.getMinX(), env.getMinY(),
                            env.getMaxX(), env.getMaxY());
                    Optional<GridSet> gridSet;
                    if(layerSpecificGridsets) {
                        gridSet = Optionals.firstPresent(
                                ()->findPreferredGridset(srs, extent),
                                ()->getLayerSpecificGridset(name, srs, extent)
                                );
                    } else {
                        gridSet = Optionals.firstPresent(
                                ()->findPreferredGridset(srs, extent),
                                ()->findGridset(srs, extent),
                                ()->createGridset(srs)
                                );
                    }
                    
                    gridSet.ifPresent(gs->{
                        grids.put(gs.getName(),GridSubsetFactory.createGridSubSet(gs,extent));
                    });
                }
            }
            
        }
        return grids;
    }
    
    
    /**
     * @param layerName
     * @param srs
     * @param extent
     */
    private Optional<GridSet> getLayerSpecificGridset(String layerName, final SRS srs,
            BoundingBox extent) {
        String gridSetName = layerName + ":" + srs.toString();
        
        GridSet gridSet = GridSetFactory.createGridSet(gridSetName, srs, extent, false,
                25, null, GridSetFactory.DEFAULT_PIXEL_SIZE_METER, 256, 256, false);
        
        gridSetBroker.put(gridSet);
        log.warn("Automatically created gridset "+gridSet.getName()+" for CRS "+srs.toString()+
                " For best results, configure an appropriate gridset yourself and add it to the"
                + " preffered mappings for WMS automatic layer configuration.");
        
        return Optional.of(gridSet);
    }
    
    /**
     * Create a new gridset for the given SRS.
     * @param srs
     * @return
     */
    @SuppressWarnings("unchecked")
    Optional<GridSet> createGridset(SRS srs) {
        CoordinateReferenceSystem crs;
        try {
            crs = CRS.decode(srs.toString());
        } catch (FactoryException ex) {
            log.error("Could not find CRS definition for "+srs.toString(), ex);
            return Optional.empty();
        }
        
        Envelope validEnvelope = CRS.getEnvelope(crs);
        
        double minx=Double.NaN;
        double maxx=Double.NaN;
        double miny=Double.NaN;
        double maxy=Double.NaN;
        final Unit<Length> unit = (Unit<Length>) CRS.getHorizontalCRS(crs).getCoordinateSystem().getAxis(0).getUnit();
        
        minx = validEnvelope.getMinimum(0);
        maxx = validEnvelope.getMaximum(0);
        miny = validEnvelope.getMinimum(1);
        maxy = validEnvelope.getMaximum(1);
        
        if(!Arrays.asList(minx, maxx, miny, maxy).stream().allMatch(Double::isFinite)){
            log.error("Extent for CRS "+srs.toString() +" is not finite");
            return Optional.empty();
        }
        
        final BoundingBox bbox = new BoundingBox(minx, miny, maxx, maxy);
        final double metresPerUnit = unit.getConverterTo(SI.METRE).convert(1.0);
        
        final boolean yAxisFirst = CRS.getAxisOrder(crs)==AxisOrder.NORTH_EAST;
        
        final GridSet gridSet = GridSetFactory.createGridSet("WMSAutoConfig:"+srs.toString(), srs, bbox, false,
                25, metresPerUnit, GridSetFactory.DEFAULT_PIXEL_SIZE_METER, 256, 256, yAxisFirst);
        
        gridSetBroker.put(gridSet);
        log.warn("Automatically created gridset "+gridSet.getName()+" for CRS "+srs.toString()+
                " For best results, configure an appropriate gridset yourself and add it to the"
                + " preffered mappings for WMS automatic layer configuration.");
        
        return Optional.of(gridSet);
        
    }
    
    /**
     * Find a gridset for an SRS using the preferred SRS mapping
     * @param srs
     * @return
     */
    Optional<GridSet> findPreferredGridset(SRS srs, BoundingBox extent) {
        return getPreferredGridset(srs)
                .flatMap(gridSetId->{
                    
            if(gridSetId==null){
                return Optional.empty();
            }
            GridSet gs = gridSetBroker.get(gridSetId);
            if(gs==null) {
                log.error("Preferred gridset "+gridSetId+" for SRS "+srs.toString()+" does not exist.");
            }
            if(gs.getSrs()!=srs) {
                log.error("Preferred gridset "+gridSetId+" for SRS "+srs.toString()+" is defined for a different SRS ("+gs.getSrs().toString()+").");
                return Optional.empty();
            }
            if(!gs.getBounds().contains(extent)) {
                log.warn("Preferred gridset "+gridSetId+" for SRS "+srs.toString()+" is not valid for extent "+extent.toString());
                return Optional.empty();
            }
            return Optional.of(gs);
        });
    }
    
    /**
     * Find a gridset valid for the given SRS and extent.
     * @param srs
     * @return
     */
    Optional<GridSet> findGridset(SRS srs, BoundingBox extent) {
        Optional<GridSet> result = gridSetBroker.getGridSets().stream()
                .filter(gs->gs.getSrs().equals(srs))
                .filter(gs->gs.getBounds().contains(extent))
                .findAny();
        result.ifPresent(gs->{
            log.warn("WMS based layer configuration for SRS "+srs+
                    " using automatically detected gridset "+gs.getName() + 
                    ". Setting a preferred mapping from SRS to Gridset will "+
                    "ensure a more stable configuration.");
        });
        return result;
    }
    
    Map<SRS, String> preferredGridsets = new HashMap<>();
    
    /**
     * Get the name of the preferred gridset for an SRS.
     * @return
     */
    public Optional<String> getPreferredGridset(SRS srs) {
        return Optional.ofNullable(preferredGridsets.get(srs));
    }
    
    /**
     * Add a gridset to the preferred gridset map.  If another gridset with the same SRS has been added, it is replaced.
     * @return
     */
    public void addPreferredGridset(GridSet gs) {
        String old = preferredGridsets.put(gs.getSrs(), gs.getName());
        if(old!=null && !old.equals(gs.getName())) {
            if(log.isInfoEnabled()){
                log.info("Changing prefered SRS-Gridset mapping for "+gs.getSrs()+" from "+old+" to "+gs.getName()+".");
            }
        }
    }
    
    public void setPreferredGridsets(Map<SRS, String> preferences) {
        if(gridSetBroker!=null) {
            preferences.forEach((srs, name)->{
                GridSet gs = gridSetBroker.get(name);
                if(gs==null) {
                    throw new IllegalArgumentException("GridSet "+name+" does not exist.");
                }
                if(!gs.getSrs().equals(srs)) {
                    throw new IllegalArgumentException("GridSet "+name+" has SRS "+gs.getSrs()+" but was mapped to "+srs);
                }
            });
        } else {
            log.info("Gridset Broker not yet available to check that provided gridset preferences are safe.");
        }
        preferredGridsets.clear();
        preferredGridsets.putAll(preferences);
    }
    
    public void setPreferredGridsets(Set<String> preferences) {
        setPreferredGridsets(preferences.stream()
                .collect(Collectors.toMap(
                        name->gridSetBroker.get(name).getSrs(),
                        Function.identity())));
    }
    
    /**
     * @return
     */
    private List<String> getMimeFormats() {
        List<String> mimeFormats = null;
        if (this.mimeTypes != null) {
            mimeFormats = Arrays.asList(this.mimeTypes.split(","));
        } else {
            mimeFormats = Arrays.asList("image/png", "image/png8", "image/jpeg");
        }
        return mimeFormats;
    }

    WebMapServer getWMS() {
        try {
            return new WebMapServer(new URL(url));
        } catch (IOException ioe) {
            log.error(url + " -> " + ioe.getMessage());
        } catch (ServiceException se) {
            log.error(se.getMessage());
        }
        return null;
    }

    private String parseVersion(String url) {
        String tmp = url.toLowerCase();
        int start = tmp.indexOf("version=");
        if (start == -1) {
            return null;
        }

        start += "version=".length();

        int stop = tmp.indexOf("&", start);
        if (stop > 0) {
            return tmp.substring(start, stop);
        } else {
            return tmp.substring(start);
        }
    }

    private double longToSphericalMercatorX(double x) {
        return (x / 180.0) * 20037508.34;
    }

    private double latToSphericalMercatorY(double y) {
        if (y > 85.05112) {
            y = 85.05112;
        }

        if (y < -85.05112) {
            y = -85.05112;
        }

        y = (Math.PI / 180.0) * y;
        double tmp = Math.PI / 4.0 + y / 2.0;
        return 20037508.34 * Math.log(Math.tan(tmp)) / Math.PI;
    }

    /**
     * @see org.geowebcache.config.Configuration#isRuntimeStatsEnabled()
     */
    public boolean isRuntimeStatsEnabled() {
        return false;
    }

    /**
     * @see org.geowebcache.config.Configuration#initialize(org.geowebcache.grid.GridSetBroker)
     */
    public int initialize(GridSetBroker gridSetBroker) throws GeoWebCacheException {
        this.gridSetBroker = gridSetBroker;
        List<TileLayer> tileLayers = getTileLayers(true);
        this.layers.clear();
        
        tileLayers.stream().forEachOrdered(layer->{
            layer.initialize(gridSetBroker);
            if(primaryConfig!=null) {
                primaryConfig.setDefaultValues(layer);
            } else if (log.isErrorEnabled()) {
                log.error("GetCapabilitiesConfiguration could not initialize a layer with default "+
                        "values as it does not have a global configuration to delegate to.");
            }
            layers.put(layer.getName(), layer);
        });
        
        return layers.size();
    }

    /**
     * @see org.geowebcache.config.Configuration#getTileLayers()
     * @deprecated
     */
    public List<TileLayer> getTileLayers() {
        return Collections.unmodifiableList(new ArrayList<TileLayer>(layers.values()));
    }

    /**
     * @see org.geowebcache.config.Configuration#getLayers()
     */
    public Iterable<? extends TileLayer> getLayers() {
        return Collections.unmodifiableList(new ArrayList<TileLayer>(layers.values()));
    }

    /**
     * @see org.geowebcache.config.Configuration#getTileLayerNames()
     */
    public Set<String> getTileLayerNames() {
        return new HashSet<String>(layers.keySet());
    }

    /**
     * @see org.geowebcache.config.Configuration#containsLayer(java.lang.String)
     */
    public boolean containsLayer(String tileLayerId) {
        return getTileLayerById(tileLayerId) != null;
    }

    /**
     * @see org.geowebcache.config.Configuration#getTileLayerById(java.lang.String)
     */
    public TileLayer getTileLayerById(String layerId) {
        // this configuration does not differentiate between layer identifier and identity
        return getTileLayer(layerId);
    }

    /**
     * @see org.geowebcache.config.Configuration#getTileLayer(java.lang.String)
     */
    public TileLayer getTileLayer(String layerName) {
        return layers.get(layerName);
    }

    /**
     * @see org.geowebcache.config.Configuration#getTileLayerCount()
     */
    public int getTileLayerCount() {
        return layers.size();
    }

    /**
     * @see org.geowebcache.config.Configuration#removeLayer(java.lang.String)
     */
    public boolean removeLayer(String layerName) {
        return layers.remove(layerName) != null;
    }

    /**
     * @see org.geowebcache.config.Configuration#modifyLayer(org.geowebcache.layer.TileLayer)
     */
    public void modifyLayer(TileLayer tl) throws NoSuchElementException {
        throw new UnsupportedOperationException("modifyLayer is not supported by "
                + getClass().getSimpleName());
    }

    /**
     * @see org.geowebcache.config.Configuration#save()
     */
    public void save() throws IOException {
        // silently do nothing
    }

    /**
     * @return {@code false}
     * @see org.geowebcache.config.Configuration#canSave(org.geowebcache.layer.TileLayer)
     */
    public boolean canSave(TileLayer tl) {
        return false;
    }

    /**
     * @see org.geowebcache.config.Configuration#addLayer(org.geowebcache.layer.TileLayer)
     */
    public void addLayer(TileLayer tl) throws IllegalArgumentException {
        if (tl == null) {
            throw new NullPointerException();
        }
        throw new IllegalArgumentException(
                "This is a read only configuration object, can't add tile layer " + tl.getName());
    }
    
    /**
     * Get the global configuration delegated to.
     */
    protected XMLConfiguration getPrimaryConfig() {
        return primaryConfig;
    }

    /**
     * Set the global configuration object to delegate to.
     */
     public void setPrimaryConfig(XMLConfiguration primaryConfig) {
        this.primaryConfig = primaryConfig;
    }
    
}
