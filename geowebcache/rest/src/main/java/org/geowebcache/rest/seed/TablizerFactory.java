package org.geowebcache.rest.seed;

import org.geowebcache.layer.TileLayer;

public class TablizerFactory {
    public JobTablizer getJobTablizer(Appendable doc, TileLayer tl){
        return new JobTablizer(doc, tl);
    }
}
