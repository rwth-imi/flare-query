package de.rwth.imi.flare.requestor;

import java.util.*;

public class Cache {
    private Map<String, CacheEntry> cache;
    private long lastCacheClean;
    private int cleanCycleMS;
    private int entryLifetimeMS;
    private int maxCacheEntries;
    private boolean updateExpiryAtAccess;
    private boolean deleteAllEntriesOnCleanup;

    /**
     * Initializes the cache on default values.
     * <ul>
     * <li>cleanCycleMS is set to 1 day
     * <li>entryLifetimeMS is set to 7 days
     * <li>maxCacheEntries is set to 10^6
     * <li>updateExpiryAtAccess is set to true
     * <li>deleteAllEntriesOnCleanup is set to false.
     * </ul>
     *
     * @see #Cache(int cleanCycleMS, int entryLifetimeMS, int maxCacheEntries,
     * boolean updateExpiryAtAccess, boolean deleteAllEntriesOnCleanup)
     */
    public Cache() {
        this(1 * 24 * 60 * 60 * 1000, 7 * 24 * 60 * 60 * 1000,
                1000 * 1000, true, false);
    }

    /**
     * Initializes the cache.
     *
     * @param cleanCycleMS              sets the time until the {@link #cleanCache()} checks entries to minimize workload
     * @param entryLifetimeMS           the maximum time an entry is kept in memory
     * @param maxCacheEntries           the maximum entries at any given time, oldest entry is removed if new entry is added to full cache
     * @param updateExpiryAtAccess      if true, update the date of the accessed entry to keep it longer in cache
     * @param deleteAllEntriesOnCleanup if true, all entries are deleted via {@link #cleanCache()} instead of checking for their remaining lifetime
     */
    public Cache(int cleanCycleMS, int entryLifetimeMS, int maxCacheEntries, boolean updateExpiryAtAccess, boolean deleteAllEntriesOnCleanup) {
        this.cache = new HashMap<>();
        this.lastCacheClean = 0;
        this.cleanCycleMS = cleanCycleMS;
        this.entryLifetimeMS = entryLifetimeMS;
        this.maxCacheEntries = maxCacheEntries;
        this.updateExpiryAtAccess = updateExpiryAtAccess;
        this.deleteAllEntriesOnCleanup = deleteAllEntriesOnCleanup;
    }

    /**
     * Add an entry to the Cache.
     *
     * @param requestUrl the Key, consisting of the Request Url
     * @param idSet    the Value, consisting of the Ids which correspond to the key
     * @return the Value, idSet
     */
    public Set<String> addCachedPatientIdsFittingRequestUrl(String requestUrl, Set<String> idSet) {
        CacheEntry cacheEntry = new CacheEntry(idSet, new Date());
        cache.put(requestUrl, cacheEntry);
        trimCacheToMaxCacheEntries();
        return idSet;
    }

    /**
     * Remove the oldest entries until the Cache size equals maxCacheEntries recursively
     */
    public void trimCacheToMaxCacheEntries() {
        if (cache.size() > maxCacheEntries) {
            removeOldestEntry();
            trimCacheToMaxCacheEntries();
        }
    }

    /**
     * Removes the oldest entry compared to the current time
     */
    private void removeOldestEntry() {
        Date oldestDate = new Date();
        String keyToOldestDate = null;
        for (Map.Entry<String, CacheEntry> pair : cache.entrySet()) {
            if (pair.getValue().getLastUpdated().compareTo(oldestDate) < 0) {
                oldestDate = pair.getValue().getLastUpdated();
                keyToOldestDate = pair.getKey();
            }
        }
        if (keyToOldestDate != null) {
            cache.remove(keyToOldestDate);
        } else {
//            remove the first element component wise if all dates are equal to the current Date
            cache.remove((cache.keySet()).iterator().next());
        }
    }

    /**
     * Returns true if this map contains a mapping for the specified requestUrl.
     *
     * @param requestUrl key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified requestUrl
     * @see java.util.Map#containsKey(Object key)
     */
    public boolean isCached(String requestUrl) {
        return cache.containsKey(requestUrl);
    }

    /**
     * Get a set of Ids corresponding to the given requestUrl.
     *
     * @param requestUrl the given requestUrl to get a set of corresponding Ids to
     * @return a set of Ids corresponding to the given requestUrl
     */
    public Set<String> getCachedPatientIdsFittingRequestUrl(String requestUrl) {
        if(updateExpiryAtAccess && cache.get(requestUrl)!= null){
            cache.get(requestUrl).setLastUpdated(new Date());
        }
        return new HashSet<>(cache.get(requestUrl).getResultSet());
    }

    /**
     * Check if cache needs to be cleaned at the current time, specified by cleanCycleMS, then delete all entries that
     * exceed their entryLifetimeMS if deleteAllEntriesOnCleanup is false or delete all entries if
     * deleteAllEntriesOnCleanup is true.
     */
    public void cleanCache() {
        long currentDateInMS = new Date().getTime();
        if (currentDateInMS - lastCacheClean > cleanCycleMS) {
            if (deleteAllEntriesOnCleanup) {
                deleteAll();
                return;
            }
            cache.values().removeIf(entry -> currentDateInMS - entry.getLastUpdated().getTime() > entryLifetimeMS);
            lastCacheClean = new Date().getTime();
            trimCacheToMaxCacheEntries();
        }
    }

    /**
     * delete the entry corresponding to the given requestUrl
     *
     * @param requestUrl the requestUrl, to which the corresponding entry is deleted
     */
    public void delete(String requestUrl) {
        if (isCached(requestUrl)) {
            cache.remove(requestUrl);
        }
    }

    /**
     * delete the entries corresponding to the requestUrls in the given List
     *
     * @param requestUrls the List of requestUrls, to which the corresponding entries are deleted
     */
    public void deleteAll(List<String> requestUrls) {
        for (String key : requestUrls) {
            if (isCached(key)) {
                cache.remove(key);
            }
        }
    }

    /**
     * delete all entries
     */
    public void deleteAll() {
        cache = new HashMap<>();
    }

    /**
     * get size of cache
     *
     * @return size of cache
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * @return cleanCycleMS, sets the time until the {@link #cleanCache()} checks entries to minimize workload
     */
    public int getCleanCycleMS() {
        return cleanCycleMS;
    }

    /**
     * @param cleanCycleMS sets the time until the {@link #cleanCache()} checks entries to minimize workload
     * @return the updated object itself
     */
    public Cache setCleanCycleMS(int cleanCycleMS) {
        this.cleanCycleMS = cleanCycleMS;
        return this;
    }

    /**
     * @return entryLifetimeMS, the maximum time an entry is kept in memory
     */
    public int getEntryLifetimeMS() {
        return entryLifetimeMS;
    }

    /**
     * @param entryLifetimeMS the maximum time an entry is kept in memory
     * @return the updated object itself
     */
    public Cache setEntryLifetimeMS(int entryLifetimeMS) {
        this.entryLifetimeMS = entryLifetimeMS;
        return this;
    }

    /**
     * @return maxCacheEntries, the maximum entries at any given time, oldest entry is removed if new entry is added to full cache
     */
    public int getMaxCacheEntries() {
        return maxCacheEntries;
    }

    /**
     * @param maxCacheEntries the maximum entries at any given time, oldest entry is removed if new entry is added to full cache
     * @return the updated object itself
     */
    public Cache setMaxCacheEntries(int maxCacheEntries) {
        this.maxCacheEntries = maxCacheEntries;
        return this;
    }

    /**
     * @return updateExpiryAtAccess, if true, update the date of the accessed entry to keep it longer in cache
     */
    public boolean isUpdateExpiryAtAccess() {
        return updateExpiryAtAccess;
    }

    /**
     * @param updateExpiryAtAccess if true, update the date of the accessed entry to keep it longer in cache
     * @return the updated object itself
     */
    public Cache setUpdateExpiryAtAccess(boolean updateExpiryAtAccess) {
        this.updateExpiryAtAccess = updateExpiryAtAccess;
        return this;
    }

    /**
     * @return deleteAllEntriesOnCleanup, if true, all entries are deleted via {@link #cleanCache()} instead of checking for their remaining lifetime
     */
    public boolean isDeleteAllEntriesOnCleanup() {
        return deleteAllEntriesOnCleanup;
    }

    /**
     * @param deleteAllEntriesOnCleanup if true, all entries are deleted via {@link #cleanCache()} instead of checking for their remaining lifetime
     * @return the updated object itself
     */
    public Cache setDeleteAllEntriesOnCleanup(boolean deleteAllEntriesOnCleanup) {
        this.deleteAllEntriesOnCleanup = deleteAllEntriesOnCleanup;
        return this;
    }
}
