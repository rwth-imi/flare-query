
import de.rwth.imi.flare.requestor.ValueSetSerializer;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.internal.statistics.DefaultStatisticsService;
import org.ehcache.core.spi.service.StatisticsService;
import org.ehcache.core.statistics.CacheStatistics;
import org.ehcache.core.statistics.TierStatistics;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class EHCacheTest {

    int idCounter = 0;

    @Test
    void mainEhTest(){
        String myCacheAlias = "myCache";
        StatisticsService statService = new DefaultStatisticsService();

        PersistentCacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(new File("target/", "TestCacheData")))
                .withCache("mixedCache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Set.class,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                                .heap(2000, EntryUnit.ENTRIES)
                                .disk(2, MemoryUnit.GB, true)))
                .withSerializer(Set.class, ValueSetSerializer.class)
                .using(statService)
                .build(true);

        Cache<String, Set> diskCache = cacheManager.getCache("mixedCache", String.class, Set.class);



        //
        int keyCount = 200;
        int valueCount = 1000;

        Set<String> dummyValues =  generateValueSet(valueCount);

        long startPutTime = System.nanoTime();
        int foundCount = 0;
        String[] keys = generateKeys( keyCount);
        for(int i = 0; i < keyCount; i++){
            //diskCache.put(keys[i], dummyValues);
        }
        float putDurationMS = (System.nanoTime() - startPutTime) / 1000000.f;
        System.out.println("It took " + putDurationMS + " ms to put " + keyCount + "*" + valueCount + " ids into the cache");
        System.out.println("Duration per value: " + (putDurationMS / (keyCount * valueCount)));
        try{
            Thread.sleep(1000);
        }catch (Exception e){e.printStackTrace();}

        for(int i = 0; i < 1; i++){ //reading all values once so values are put into memory, to test memory read time
        for(int j = 0; j < keyCount; j++){
                Set<String> foundItem = diskCache.get(keys[j]);
            }
        }
        long startReadTime = System.nanoTime();
        for(int i = 0; i < keyCount; i++){
            Set<String> foundItem = diskCache.get(keys[i]);
            if(foundItem != null){
                foundCount++;
            }

        }
        System.out.println("---");
        float readTimeMS = (System.nanoTime() - startReadTime) / 1000000.f;
        System.out.println("It took " + readTimeMS + " ms to read " + keyCount + "*" + valueCount + " ids from the cache");
        System.out.println("Duration per value: " + (readTimeMS / (keyCount * valueCount)));
        System.out.println("---");

        System.out.println("found cache entries " + foundCount);
        int sizeInMb = ((10 + 46 * valueCount) * foundCount) / (1000 * 1000);
        System.out.println("raw cache date in MB: " + sizeInMb);

        CacheStatistics cacheStats = statService.getCacheStatistics("mixedCache");
        Map<String, TierStatistics> tierStatistics = cacheStats.getTierStatistics();
        TierStatistics diskStats = tierStatistics.get("Disk");
        if(diskStats != null){
            long occupiedMB = diskStats.getOccupiedByteSize() / (1000 * 1000);
            long allocatedMB = diskStats.getAllocatedByteSize() / (1000 * 1000);
            System.out.println("currently occupied MB on disk : " + occupiedMB);
            System.out.println("allocated MB on disk: " +allocatedMB );
        }

        TierStatistics onHeapStats = tierStatistics.get("OnHeap");
        if(onHeapStats != null){
            long occupiedMB = onHeapStats.getOccupiedByteSize();
            long allocatedMB = onHeapStats.getAllocatedByteSize();
            System.out.println("currently occupied bytes on heap : " + occupiedMB);
            System.out.println("allocated bytes on heap: " +allocatedMB );
        }
        //cacheManager.close();//TODO ---------------- this is only temporarily commented
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
        //String pseudoUUID = uuid;
        char[] pseudoUUID = new char[36];
        for(int i = 0; i < uuid.length(); i++){
            pseudoUUID[i] = uuid.charAt(i);
        }

        Random rand = new Random(idCounter);
        for(int i = uuid.length(); i < 36; i++){

            // System.out.println();

            pseudoUUID[i] = String.valueOf(rand.nextInt( 10)).charAt(0);//'0';
        }
        idCounter++;

        return new String(pseudoUUID);
    }
}
