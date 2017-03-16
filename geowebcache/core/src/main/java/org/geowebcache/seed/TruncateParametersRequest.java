package org.geowebcache.seed;

import java.util.Map;

import org.geowebcache.config.Configuration;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.StorageException;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.NamedMapConverter;

@XStreamAlias("truncateParameters")
public class TruncateParametersRequest implements MassTruncateRequest {
    String layerName;
    
    @XStreamConverter(value = NamedMapConverter.class, strings = { "parameter", "key", "value" }, booleans = { false, false }, types = { String.class, String.class })
    Map<String, String> parameters;
    
    @Override
    public boolean doTruncate(StorageBroker sb, Configuration config) throws StorageException {
        return sb.deleteByParameters(layerName, parameters);
    }

}
