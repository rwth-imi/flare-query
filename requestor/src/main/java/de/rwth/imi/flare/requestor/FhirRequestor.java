package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.Requestor;
import de.rwth.imi.flare.api.UnsupportedCriterionException;
import de.rwth.imi.flare.api.model.Criterion;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Requestor implementation, takes a single criterion, builds a FHIR Query from
 * it, and executes it
 */
@Slf4j
public class FhirRequestor implements Requestor, AutoCloseable {

    public static final String CACHE_ALIAS = "FlareCache";

    private final FhirRequestorConfig config;
    private final Cache<String, CacheValue> cache;
    private final Executor executor;
    private final CacheManager cacheConfigurationManager;
    private final SearchQueryStringBuilder searchQueryStringBuilder;

    /**
     * @param requestorConfig configuration to be used when crafting requests
     * @param cacheConfig     the cache config to use
     * @param executor        the executor to use for FHIR REST API requests
     */
    public FhirRequestor(FhirRequestorConfig requestorConfig, CacheConfig cacheConfig, Executor executor) {
        this.config = Objects.requireNonNull(requestorConfig);
        this.cacheConfigurationManager = createCacheManager(Objects.requireNonNull(cacheConfig));
        cache = this.cacheConfigurationManager.getCache(CACHE_ALIAS, String.class, CacheValue.class);
        this.executor = Objects.requireNonNull(executor);
        searchQueryStringBuilder = new SearchQueryStringBuilder(Clock.systemDefaultZone());
    }

    private static CacheManager createCacheManager(CacheConfig cacheConfig) {
        return CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(cacheConfig.getCacheDir()))
                .withCache(CACHE_ALIAS, CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, CacheValue.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(cacheConfig.getHeapEntryCount(), EntryUnit.ENTRIES)
                                        .disk(cacheConfig.getDiskSizeGB(), MemoryUnit.GB, true))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofHours(cacheConfig.getExpiryHours()))))
                .withSerializer(CacheValue.class, CacheValueSerializer.class)
                .build(true);
    }

    /**
     * Builds the query string specified by the criterion, then executes said
     * query string
     *
     * @param searchCriterion single criterion
     * @return Stream that contains the results for the given criterion
     */
    @Override
    public CompletableFuture<Set<String>> execute(Criterion searchCriterion) {
        String key;
        try {
            key = translateCriterion(searchCriterion);
        } catch (UnsupportedCriterionException e) {
            return CompletableFuture.failedFuture(e);
        }
        CacheValue value = cache.get(key);
        if (value == null) {
            return executeFhirQuery(key, executor);
        } else {
            return CompletableFuture.completedFuture(value.patientIds());
        }
    }

    private CompletableFuture<Set<String>> executeFhirQuery(String requestUrl, Executor executor) {
        log.debug("FHIR Search: " + requestUrl + " not cached or refreshing...");

        return CompletableFuture.supplyAsync(() -> {
            FhirSearchRequest fhirSearchRequest = createFhirSearchRequest(URI.create(requestUrl));
            Set<String> patientIds = createStream(fhirSearchRequest)
                    .map(FlareResource::getPatientId)
                    .collect(Collectors.toSet());

            log.debug("FHIR Search: " + requestUrl + " finished execution, writing to cache...");
            cache.put(requestUrl, new CacheValue(patientIds));
            return patientIds;

        }, executor);
    }

    private FhirSearchRequest createFhirSearchRequest(URI requestUrl) {
        String pageCount = config.getPageCount();
        return config.getAuthentication()
                .map((auth) -> new FhirSearchRequest(requestUrl, auth, pageCount))
                .orElseGet(() -> new FhirSearchRequest(requestUrl, pageCount));
    }

    /**
     * Override of execute. Returns URI as String instead of FlareResource
     * stream.
     *
     * @param searchCriterion single criterion
     * @return criterion parsed to FHIR URL String
     */
    @Override
    public String translateCriterion(Criterion searchCriterion) throws UnsupportedCriterionException {
        String searchQuery = searchQueryStringBuilder.constructQueryString(searchCriterion);
        return config.getBaseURI().resolve(searchQuery).toString();
    }

    private Stream<FlareResource> createStream(FhirSearchRequest fhirSearchRequest) {
        Iterable<FlareResource> streamSource = () -> fhirSearchRequest;
        return StreamSupport.stream(streamSource.spliterator(), false);
    }

    @Override
    public void close() {
        this.cacheConfigurationManager.close();
    }
}
