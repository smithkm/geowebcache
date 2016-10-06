package org.geowebcache.retiler;

import java.io.IOException;
import java.net.URL;

import org.geotools.factory.Hints;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.PropertyAuthorityFactory;
import org.geotools.referencing.factory.ReferencingFactoryContainer;
import org.opengis.referencing.AuthorityFactory;

public class TestAuthorityRule extends org.junit.rules.ExternalResource {
    
    public TestAuthorityRule(URL url) {
        resource = url;
    }
    
    AuthorityFactory authFactory;
    URL resource;
    
    @Override
    protected void before() throws IOException {
        Hints hints = new Hints(Hints.CRS_AUTHORITY_FACTORY, PropertyAuthorityFactory.class);
        ReferencingFactoryContainer referencingFactoryContainer =
                   ReferencingFactoryContainer.instance(hints);
        
        authFactory = new PropertyAuthorityFactory(
                          referencingFactoryContainer, Citations.fromName("EPSG"), resource);
        
        ReferencingFactoryFinder.addAuthorityFactory(authFactory);
        ReferencingFactoryFinder.scanForPlugins();
    }
    
    @Override
    protected void after() {
        ReferencingFactoryFinder.removeAuthorityFactory(authFactory);
        ReferencingFactoryFinder.scanForPlugins();
    }
}
