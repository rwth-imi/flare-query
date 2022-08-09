import de.rwth.imi.flare.requestor.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class CacheTests {
    Cache cache;
    int[] id_base = {1,2,3,4,5,6,7,8,9};

    @BeforeEach
    void setUp(TestInfo testinfo) throws InterruptedException {
        cache = new Cache(1 * 24 * 60 * 60 * 1000, 7 * 24 * 60 * 60 * 1000,
                1000 * 1000, true, false);
        for (int i=1; i<=200; i++){
            int finalI = i;
            cache.addCachedPatientIdsFittingRequestUrl(""+i, IntStream.of(id_base).mapToObj(el->""+(el* finalI)).collect(Collectors.toSet()));
            // ensure different dates
            Thread.sleep(1);
        }
    }

    @Test
    void correctlyIsCached(){
        assertTrue(cache.isCached("1"));
        assertTrue(cache.isCached("7"));
        assertTrue(cache.isCached("12"));
        assertTrue(cache.isCached("42"));
        assertTrue(cache.isCached("123"));
        assertTrue(cache.isCached("177"));
    }

    @Test
    void addCorrectElementAndRemoveOldest(){
        cache.setMaxCacheEntries(201);
        assertTrue(cache.isCached("1"));
        assertFalse(cache.isCached("test"));
        assertFalse(cache.isCached("test2"));
        cache.addCachedPatientIdsFittingRequestUrl("test", Set.of(new String[]{"asd", "dsa"}));
        cache.addCachedPatientIdsFittingRequestUrl("test2", Set.of(new String[]{"asd2", "dsa2"}));
        assertFalse(cache.isCached("1"));
        assertTrue(cache.isCached("test"));
        assertTrue(cache.isCached("test2"));

    }
    @Test
    void correctlyTrimmed(){
        cache.setMaxCacheEntries(40);
        cache.trimCacheToMaxCacheEntries();

        assertFalse(cache.isCached("1"));
        assertFalse(cache.isCached("7"));
        assertFalse(cache.isCached("12"));
        assertFalse(cache.isCached("42"));
        assertFalse(cache.isCached("123"));
        assertTrue(cache.isCached("177"));
        assertTrue(cache.isCached("180"));
        assertTrue(cache.isCached("189"));
        assertTrue(cache.isCached("194"));
    }

    @Test
    void getCorrectValues(){
        assertEquals(cache.getCachedPatientIdsFittingRequestUrl("1"), IntStream.of(id_base).mapToObj(el->""+(el)).collect(Collectors.toSet()));
        assertEquals(cache.getCachedPatientIdsFittingRequestUrl("5"), IntStream.of(id_base).mapToObj(el->""+(el* 5)).collect(Collectors.toSet()));
        assertEquals(cache.getCachedPatientIdsFittingRequestUrl("77"), IntStream.of(id_base).mapToObj(el->""+(el* 77)).collect(Collectors.toSet()));
        assertEquals(cache.getCachedPatientIdsFittingRequestUrl("134"), IntStream.of(id_base).mapToObj(el->""+(el* 134)).collect(Collectors.toSet()));
        assertEquals(cache.getCachedPatientIdsFittingRequestUrl("200"), IntStream.of(id_base).mapToObj(el->""+(el* 200)).collect(Collectors.toSet()));
    }

    @Test
    void correctSizeReduction(){
        assertEquals(cache.getCacheSize(), 200);
        cache.setMaxCacheEntries(40);
        cache.trimCacheToMaxCacheEntries();
        assertEquals(cache.getCacheSize(), 40);
    }

    @Test
    void correctCacheCleanSizeReduction(){
        assertEquals(cache.getCacheSize(), 200);
        cache.setMaxCacheEntries(40);
        cache.cleanCache();
        assertEquals(cache.getCacheSize(), 40);
    }
    @Test
    void correctCacheCleanCycleWaitTime() throws InterruptedException {
        assertEquals(cache.getCacheSize(), 200);
        cache.setCleanCycleMS(1000);
        cache.setMaxCacheEntries(100);
        cache.cleanCache();
        assertEquals(cache.getCacheSize(), 100);
        cache.setMaxCacheEntries(50);
        cache.cleanCache();
        assertEquals(cache.getCacheSize(), 100);
        Thread.sleep(1000);
        cache.cleanCache();
        assertEquals(cache.getCacheSize(), 50);
    }

    @Test
    void correctCacheCleanLifetimeHandling() throws InterruptedException {
        Thread.sleep(1);
        cache.setCleanCycleMS(0);
        assertEquals(cache.getCacheSize(), 200);
        cache.setEntryLifetimeMS(10000);
        cache.cleanCache();
        Thread.sleep(1);
        assertEquals(cache.getCacheSize(), 200);
        cache.setEntryLifetimeMS(0);
        cache.cleanCache();
        assertEquals(cache.getCacheSize(), 0);
    }

    @Test
    void correctCacheCleanDeleteAll(){
        assertEquals(cache.getCacheSize(), 200);
        cache.setDeleteAllEntriesOnCleanup(true);
        cache.cleanCache();
        assertEquals(cache.getCacheSize(), 0);
    }

    @Test
    void singleDeleteCorrectly(){
        assertEquals(cache.getCacheSize(), 200);
        assertTrue(cache.isCached("1"));
        cache.delete("1");
        assertEquals(cache.getCacheSize(), 199);
        assertFalse(cache.isCached("1"));
    }

    @Test
    void multipleDeleteCorrectly(){
        assertEquals(cache.getCacheSize(), 200);
        assertTrue(cache.isCached("1"));
        List<String> toDelete = new ArrayList<String>();
        toDelete.add("1");
        toDelete.add("4");
        toDelete.add("78");
        toDelete.add("187");
        cache.deleteAll(toDelete);
        assertEquals(cache.getCacheSize(), 196);
        assertFalse(cache.isCached("1"));
        assertFalse(cache.isCached("4"));
        assertFalse(cache.isCached("78"));
        assertFalse(cache.isCached("187"));
    }

    @Test
    void AllDeleteCorrectly(){
        assertEquals(cache.getCacheSize(), 200);
        assertTrue(cache.isCached("1"));
        cache.deleteAll();
        assertEquals(cache.getCacheSize(), 0);
        assertFalse(cache.isCached("1"));
    }

}
