
import de.rwth.imi.flare.requestor.CacheValue;
import de.rwth.imi.flare.requestor.CacheValueSerializer;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNull;

public class EhCacheExpiryTest {

    public static final String KEY = "key-144049";
    public static final CacheValue VALUE = new CacheValue();

    private CacheManager cacheManager;
    private Cache<String, CacheValue> cache;

    @BeforeEach
    void setUp() {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("mixedCache", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, CacheValue.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(10, EntryUnit.ENTRIES))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(1))))
                .withSerializer(CacheValue.class, CacheValueSerializer.class)
                .build(true);

        cache = cacheManager.getCache("mixedCache", String.class, CacheValue.class);
    }

    @AfterEach
    void tearDown() {
        cacheManager.close();
    }

    @Test
    void mainEhExpiryTest() throws InterruptedException {
        cache.put(KEY, VALUE);

        Thread.sleep(2000);

        assertNull(cache.get(KEY), "value not found after expiry");
    }
}
