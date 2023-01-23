package de.rwth.imi.flare.api;

import java.io.Serializable;
import java.util.Set;

public class FlareCacheEntry implements Serializable {
    public Set<FlareIdDateWrap> idDateWraps;
    public String cacheEntryTime;
}
