package de.rwth.imi.flare.requestor;

public interface CacheConfig {
    int getCacheSizeInMb();
    int getEntryRefreshTimeHours();
}
