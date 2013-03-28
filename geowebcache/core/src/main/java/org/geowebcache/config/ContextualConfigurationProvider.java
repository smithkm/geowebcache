package org.geowebcache.config;

/**
 * An XMLConfigurationProvider that applies only when the serialization occurs in a particular
 * context
 * 
 * @author Kevin Smith, OpenGeo
 *
 */
public interface ContextualConfigurationProvider extends
        XMLConfigurationProvider {

    /**
     * The contexts within which serialization can occur.
     *
     */
    static public enum Context {
        /**
         * Serialization is for persistence of configuration to storage
         */
        PERSIST,
        
        /**
         * Serialization is for use as the over the wire format for REST
         */
        REST
    }
    
    /**
     * Does the provider apply to the given context
     * @param ctxt The context
     * @return true of applicable, false otherwise
     */
    public boolean appliesTo(Context ctxt);
}
