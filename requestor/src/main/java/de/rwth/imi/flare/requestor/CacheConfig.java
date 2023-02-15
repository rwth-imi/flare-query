package de.rwth.imi.flare.requestor;

import java.io.File;

public interface CacheConfig {
    int getHeapEntryCount();
    int getDiskSizeGB();
    File getCacheDir();

}
