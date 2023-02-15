package de.rwth.imi.flare.requestor;

import ca.uhn.fhir.context.FhirContext;

import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.jetbrains.annotations.NotNull;

/**
 * Requestor implementation, takes a single criterion, builds a FHIR Query from
 * it, and executes it
 */
@Slf4j
public class FhirRequestor implements de.rwth.imi.flare.api.Requestor, AutoCloseable{

  public static final String CACHE_ALIAS = "FlareCache";
  private final FhirRequestorConfig config;
  private final FhirContext fhirR4Context = FhirContext.forR4();
  private final Cache<String, Set> cache;
  private final Executor executor;
  private final CacheManager cacheConfigurationManager;

  /**
   * @param executor
   * @param requestorConfig Configuration to be used when crafting requests
   */
  public FhirRequestor(FhirRequestorConfig requestorConfig,
      CacheConfig cacheConfig, Executor executor) {
    this.executor = executor;
    this.config = requestorConfig;
    this.cacheConfigurationManager = createCacheManager(cacheConfig);

    cache = this.cacheConfigurationManager.getCache(CACHE_ALIAS, String.class, Set.class);
  }

  @Override
  public void close() throws Exception {
    this.cacheConfigurationManager.close();
  }

  private static CacheManager createCacheManager(CacheConfig cacheConfig) {
    CacheManager cacheConfigurationManager = CacheManagerBuilder.newCacheManagerBuilder()
            .with(CacheManagerBuilder.persistence(new File("target/", "EhCacheData")))
            .withCache(CACHE_ALIAS, CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Set.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder()
                            .heap(cacheConfig.getHeapEntryCount(), EntryUnit.ENTRIES)
                            .disk(cacheConfig.getDiskSizeGB(), MemoryUnit.GB, true)))
            .withSerializer(Set.class, ValueSetSerializer.class)
            .build(true);
    return cacheConfigurationManager;
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
    URI requestUrl;
    try {
      requestUrl = buildRequestUrl(searchCriterion);
    } catch (URISyntaxException | IncorrectQueryInputException e) {
      throw new RuntimeException(e);
    }
    String urlString = requestUrl.toString();

    Set<String> foundCacheEntry = cache.get(urlString);
    if (foundCacheEntry == null) {
      return getSetCompletableFuture(urlString, this.executor);
    } else {
      return CompletableFuture.completedFuture(foundCacheEntry);
    }
  }


  @NotNull
  private CompletableFuture<Set<String>> getSetCompletableFuture(String requestUrl, Executor executor) {
    log.debug("FHIR Search: " + requestUrl + " not cached or refreshing...");
    return CompletableFuture.supplyAsync(() -> {
      String pagecount = this.config.getPageCount();
      FhirSearchRequest fhirSearchRequest = this.config.getAuthentication()
              .map((auth) -> new FhirSearchRequest(URI.create(requestUrl), auth, pagecount, fhirR4Context))
              .orElseGet(() -> new FhirSearchRequest(URI.create(requestUrl), pagecount, fhirR4Context));
      Set<String> flareStream = createStream(fhirSearchRequest)
              .map(FlareResource::getPatientId)
              .collect(Collectors.toSet());
      log.debug("FHIR Search: " + requestUrl + " finished execution, writing to cache...");
      cache.put(requestUrl, flareStream);
      return flareStream;
    }, executor);
  }

  /**
   * Override of execute. Returns URI as String instead of FlareResource
   * stream.
   *
   * @param searchCriterion single criterion
   * @return criterion parsed to FHIR URL String
   */
  @Override
  public String translateCriterion(Criterion searchCriterion) {
    URI requestUrl;
    try {
      requestUrl = buildRequestUrl(searchCriterion);
    } catch (URISyntaxException | IncorrectQueryInputException e) {
      throw new RuntimeException(e);
    }
    return requestUrl.toString();
  }

  @NotNull
  private Stream<FlareResource> createStream(
      FhirSearchRequest fhirSearchRequest) {
    Iterable<FlareResource> streamSource = () -> fhirSearchRequest;
    return StreamSupport.stream(streamSource.spliterator(), false);
  }


  private URI buildRequestUrl(Criterion search)
      throws URISyntaxException, IncorrectQueryInputException {
    // TODO: Find a way to properly concat URLs in Java
    String searchQuery = SearchQueryStringBuilder.constructQueryString(search);
    String searchUrl = config.getBaseURI().toString() + searchQuery;
    return new URI(searchUrl);
  }



}
