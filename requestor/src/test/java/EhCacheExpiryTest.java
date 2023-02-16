
import de.rwth.imi.flare.requestor.ValueSetSerializer;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.internal.statistics.DefaultStatisticsService;
import org.ehcache.core.spi.service.StatisticsService;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class EhCacheExpiryTest {

    int idCounter = 0;

    @Test
    void mainEhExpiryTest() throws InterruptedException {
        StatisticsService statService = new DefaultStatisticsService();

        PersistentCacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(new File("target/", "ExpiryTestData")))
                .withCache("mixedCache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Set.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                .heap(2000, EntryUnit.ENTRIES)
                                .disk(2, MemoryUnit.GB, true))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(10))))
                .withSerializer(Set.class, ValueSetSerializer.class)
                .using(statService)
                .build(true);

        Cache<String, Set> diskCache = cacheManager.getCache("mixedCache", String.class, Set.class);

        int keyCount = 1;
        int valueCount = 3;

        Set<String> dummyValues =  generateValueSet(valueCount);
        String[] keys = generateKeys( keyCount);

        diskCache.put(keys[0], dummyValues);

        Set<String> foundValuesBeforeExpiry = diskCache.get(keys[0]);
        System.out.println("fond values before expiry: " + foundValuesBeforeExpiry);

        Thread.sleep(20 * 1000);

        Set<String> foundValuesAfterExpiry = diskCache.get(keys[0]);
        System.out.println("fond values after expiry: " + foundValuesAfterExpiry);

        cacheManager.close();
    }


    String[] generateKeys(int keyCount){
        String[] keyArray = new String[keyCount];
        for(int i = 0; i < keyCount; i++){
            String currentKey = generatePseudoUUID();
            keyArray[i] = currentKey;
        }
        return keyArray;
    }


    Set<String> generateValueSet(int valueCount){
        Set<String> newSet = new HashSet<>();

        for(int i = 0; i < valueCount; i++){
            String dummyId = generatePseudoUUID();
            newSet.add(dummyId);
        }
        return newSet;
    }

    String generatePseudoUUID(){

        String uuid = Integer.toString(idCounter);

        int amountOfZeros = 36 - uuid.length();
        char[] pseudoUUID = new char[36];
        for(int i = 0; i < uuid.length(); i++){
            pseudoUUID[i] = uuid.charAt(i);
        }

        Random rand = new Random(idCounter);
        for(int i = uuid.length(); i < 36; i++){

            pseudoUUID[i] = String.valueOf(rand.nextInt( 10)).charAt(0);
        }
        idCounter++;

        return new String(pseudoUUID);
    }
}
