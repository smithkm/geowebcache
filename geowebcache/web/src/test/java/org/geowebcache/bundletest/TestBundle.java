package org.geowebcache.bundletest;

import java.net.URL;

import org.geowebcache.rest.webresources.WebResourceBundle;

public class TestBundle implements WebResourceBundle {

	@Override
	public URL apply(String t) {
		return TestBundle.class.getResource(t);
	}

}
