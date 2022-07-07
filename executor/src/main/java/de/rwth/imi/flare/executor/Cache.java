package de.rwth.imi.flare.executor;

import java.util.*;

public class Cache {
    private Map<String, Set<String>> cache;
    private Map<String, Date> datesForCache;
    private Date lastCacheClean;
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
        this.datesForCache = new HashMap<>();
        this.lastCacheClean = new Date();
        this.cleanCycleMS = cleanCycleMS;
        this.entryLifetimeMS = entryLifetimeMS;
        this.maxCacheEntries = maxCacheEntries;
        this.updateExpiryAtAccess = updateExpiryAtAccess;
        this.deleteAllEntriesOnCleanup = deleteAllEntriesOnCleanup;
    }

    /**
     * Add an entry to the Cache.
     *
     * @param termCode the Key, consisting of the Termcode
     * @param idSet    the Value, consisting of the Ids which correspond to the key
     * @return the Value, idSet
     */
    public Set<String> addCachedPatientIdsFittingTermCode(String termCode, Set<String> idSet) {
        cache.put(termCode, idSet);
        datesForCache.put(termCode, new Date());
        trimCacheToMaxCacheEntries();
        return idSet;
    }

    /**
     * Remove the oldest entries until the Cache size equals maxCacheEntries-1
     */
    public void trimCacheToMaxCacheEntries() {
        if (cache.size() > maxCacheEntries) {
            removeOldestEntry();
            trimCacheToMaxCacheEntries();
        } else if (cache.size() == maxCacheEntries) {
            removeOldestEntry();
        }
    }

    /**
     * Removes the oldest entry compared to the current time
     */
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

    /**
     * Returns true if this map contains a mapping for the specified termCode.
     *
     * @param termCode key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified termCode
     * @see java.util.Map#containsKey(Object key)
     */
    public boolean isCached(String termCode) {
        return cache.containsKey(termCode);
    }

    /**
     * Get a set of Ids corresponding to the given termCode.
     *
     * @param termCode the given termCode to get a set of corresponding Ids to
     * @return a set of Ids corresponding to the given termCode
     */
    public Set<String> getCachedPatientIdsFittingCriterion(String termCode) {
        return new HashSet<>(cache.get(termCode));
    }

    /**
     * Check if cache needs to be cleaned at the current time, specified by cleanCycleMS, then delete all entries that
     * exceed their entryLifetimeMS if deleteAllEntriesOnCleanup is false or delete all entries if
     * deleteAllEntriesOnCleanup is true.
     */
    public void cleanCache() {
        long currentDateInMS = new Date().getTime();
        if (currentDateInMS - lastCacheClean.getTime() > cleanCycleMS) {
            if (deleteAllEntriesOnCleanup) {
                delete_all();
                return;
            }
//            List<String> entriesToDelete = new ArrayList<>(); TODO: check if on the fly delete works
            for (Map.Entry<String, Date> pair : datesForCache.entrySet()) {
                if (currentDateInMS - pair.getValue().getTime() > entryLifetimeMS) {
                    delete(pair.getKey());
                }
            }
            lastCacheClean = new Date();
        }
        trimCacheToMaxCacheEntries();
    }

    /**
     * delete the entry corresponding to the given termCode
     *
     * @param termCode the termCode, to which the corresponding entry is deleted
     */
    public void delete(String termCode) {
        if (isCached(termCode)) {
            cache.remove(termCode);
            datesForCache.remove(termCode);
        }
    }

    /**
     * delete the entries corresponding to the termCodes in the given List
     *
     * @param termCodes the List of termCodes, to which the corresponding entries are deleted
     */
    public void delete_all(List<String> termCodes) {
        for (String key : termCodes) {
            if (isCached(key)) {
                cache.remove(key);
                datesForCache.remove(key);
            }
        }
    }

    /**
     * delete all entries
     */
    public void delete_all() {
        cache = new HashMap<>();
        datesForCache = new HashMap<>();
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
