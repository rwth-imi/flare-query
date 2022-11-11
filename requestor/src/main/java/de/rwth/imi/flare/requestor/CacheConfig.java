package de.rwth.imi.flare.requestor;

public interface CacheConfig {
    int getCleanCycleMS();
    int getEntryLifetimeMS();
    int getMaxCacheEntries();
    boolean getUpdateExpiryAtAccess();
    boolean getDeleteAllEntriesOnCleanup();
}
