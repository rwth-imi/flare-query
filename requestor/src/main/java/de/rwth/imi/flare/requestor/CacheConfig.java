package de.rwth.imi.flare.requestor;

import java.io.File;

public interface CacheConfig {

    /**
     * Number of cache entries stored in memory.
     *
     * @return the number of cache entries stored in memory
     */
    int getHeapEntryCount();

    /**
     * Size of the disk cache in gigabytes.
     *
     * @return the size of the disk cache in gigabytes
     */
    int getDiskSizeGB();

    /**
     * Number of hours after which a cache entries expires.
     *
     * @return the number of hours after which a cache entries expires
     */
    int getExpiryHours();

    /**
     * Directory were the disk cache should be stored.
     *
     * @return the directory were the disk cache should be stored
     */
    File getCacheDir();
}
