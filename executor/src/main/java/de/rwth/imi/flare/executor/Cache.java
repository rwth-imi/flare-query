package de.rwth.imi.flare.executor;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Cache {
    Map<String, Set<String>> cache;
    Map<String, Date> datesForCache;
    Date lastCacheClean;
    int cleanCycleMS;
    int maxCacheEntries;

    public Cache() {
        this.cache = new HashMap<>();
        this.datesForCache = new HashMap<>();
        this.lastCacheClean = new Date();
        // 7 days in ms
        this.cleanCycleMS = 7 * 24 * 60 * 60 * 1000;
        this.maxCacheEntries = 1000 * 1000;
    }

    public Set<String> addCachedPatientIdsFittingTermCode(String termCode, Set<String> idSet) {
        trimCacheToMaxCacheEntries();
        cache.put(termCode, idSet);
        datesForCache.put(termCode, new Date());
        return idSet;
    }

    public void trimCacheToMaxCacheEntries() {
        if (cache.size() > maxCacheEntries + 1) {
            removeOldestEntry();
            trimCacheToMaxCacheEntries();
        } else if (cache.size() == maxCacheEntries + 1) {
            removeOldestEntry();
        }
    }

    private void removeOldestEntry() {
        Date oldestDate = new Date();
        String keyToOldestDate = null;
        for (Map.Entry<String, Date> pair : datesForCache.entrySet()) {
            if (pair.getValue().compareTo(oldestDate) < 0) {
                oldestDate = pair.getValue();
                keyToOldestDate = pair.getKey();
            }
        }
        if (keyToOldestDate != null) {
            cache.remove(keyToOldestDate);
            datesForCache.remove(keyToOldestDate);
        } else {
//            remove the first element component wise if all dates are equal
            cache.remove((cache.keySet()).iterator().next());
            datesForCache.remove(datesForCache.keySet().iterator().next());
        }
    }

    public boolean isCached(String criterion) {
        return cache.containsKey(criterion);
    }

    public CompletableFuture<Set<String>> getCachedPatientIdsFittingCriterion(String criterion) {
        return CompletableFuture.completedFuture(new HashSet<>(cache.get(criterion)));
    }

    public void cleanCache() {
        long currentDateInMS = new Date().getTime();
        if (currentDateInMS - lastCacheClean.getTime() > cleanCycleMS) {
            for (Map.Entry<String, Date> pair : datesForCache.entrySet()) {
                if (currentDateInMS - pair.getValue().getTime() > cleanCycleMS) {
                    cache.remove(pair.getKey());
                    datesForCache.remove(pair.getKey());
                }
            }
            lastCacheClean = new Date();
        }
    }

    public void delete(String key) {
        if (isCached(key)) {
            cache.remove(key);
            datesForCache.remove(key);
        }
    }
}
