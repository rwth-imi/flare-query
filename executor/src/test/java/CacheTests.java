import de.rwth.imi.flare.executor.Cache;
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
            cache.addCachedPatientIdsFittingTermCode(""+i, IntStream.of(id_base).mapToObj(el->""+(el* finalI)).collect(Collectors.toSet()));
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
        cache.addCachedPatientIdsFittingTermCode("test", Set.of(new String[]{"asd", "dsa"}));
        cache.addCachedPatientIdsFittingTermCode("test2", Set.of(new String[]{"asd2", "dsa2"}));
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
        assertEquals(cache.getCachedPatientIdsFittingCriterion("1"), IntStream.of(id_base).mapToObj(el->""+(el)).collect(Collectors.toSet()));
        assertEquals(cache.getCachedPatientIdsFittingCriterion("5"), IntStream.of(id_base).mapToObj(el->""+(el* 5)).collect(Collectors.toSet()));
        assertEquals(cache.getCachedPatientIdsFittingCriterion("77"), IntStream.of(id_base).mapToObj(el->""+(el* 77)).collect(Collectors.toSet()));
        assertEquals(cache.getCachedPatientIdsFittingCriterion("134"), IntStream.of(id_base).mapToObj(el->""+(el* 134)).collect(Collectors.toSet()));
        assertEquals(cache.getCachedPatientIdsFittingCriterion("200"), IntStream.of(id_base).mapToObj(el->""+(el* 200)).collect(Collectors.toSet()));
    }

//    @Test
//    void correctCacheClean(){
//        cache.setMaxCacheEntries(40);
//        cache.setEntryLifetimeMS(0);
//
//
//        cache.cleanCache();
//
//    }

}
