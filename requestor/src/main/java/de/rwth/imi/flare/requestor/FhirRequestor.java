package de.rwth.imi.flare.requestor;

import ca.uhn.fhir.context.FhirContext;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Weigher;
import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.UnsupportedCriterionException;
import de.rwth.imi.flare.api.model.Criterion;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.jetbrains.annotations.NotNull;

/**
 * Requestor implementation, takes a single criterion, builds a FHIR Query from
 * it, and executes it
 */
@Slf4j
public class FhirRequestor implements de.rwth.imi.flare.api.Requestor {

  private static final StringSetWeigher WEIGHER = new StringSetWeigher();
  private static final long N_BYTES_IN_MB = 1024*1024;

  private final FhirRequestorConfig config;
  private final FhirContext fhirR4Context = FhirContext.forR4();
  private final AsyncLoadingCache<String, Set<String>> cache;
  private final SearchQueryStringBuilder searchQueryStringBuilder;

  /**
   * @param executor
   * @param requestorConfig Configuration to be used when crafting requests
   */
  public FhirRequestor(FhirRequestorConfig requestorConfig,
      CacheConfig cacheConfig, Executor executor) {
    this.config = requestorConfig;
    this.cache = Caffeine.newBuilder()
        .maximumWeight(cacheConfig.getCacheSizeInMb() * N_BYTES_IN_MB)
        .weigher(WEIGHER)
        .refreshAfterWrite(cacheConfig.getEntryRefreshTimeHours(), TimeUnit.HOURS)
        .executor(executor)
        .evictionListener((String key, Set<String> idSet, RemovalCause cause) ->
            log.debug("Key " + key + " was evicted, cause: " + cause))
        .buildAsync(this::getSetCompletableFuture);
    searchQueryStringBuilder = new SearchQueryStringBuilder(Clock.systemDefaultZone());
  }


  /**
   * Builds the query string specified by the criterion, then executes said
   * query string
   *
   * @param searchCriterion single criterion
   * @return Stream that contains the results for the given criterion
   */
  @Override
  public CompletableFuture<Set<String>> execute(Criterion searchCriterion) throws UnsupportedCriterionException {
    URI requestUrl;
    try {
      requestUrl = buildRequestUrl(searchCriterion);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    String urlString = requestUrl.toString();
    return cache.get(urlString);

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
  public String translateCriterion(Criterion searchCriterion) throws UnsupportedCriterionException {
    URI requestUrl;
    try {
      requestUrl = buildRequestUrl(searchCriterion);
    } catch (URISyntaxException e) {
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


  private URI buildRequestUrl(Criterion search) throws URISyntaxException, UnsupportedCriterionException {
    // TODO: Find a way to properly concat URLs in Java
    String searchQuery = searchQueryStringBuilder.constructQueryString(search);
    String searchUrl = config.getBaseURI().toString() + searchQuery;
    return new URI(searchUrl);
  }

  private static class StringSetWeigher implements
      Weigher<String, Set<String>> {

    @Override
    public @NonNegative int weigh(String key, Set<String> idSet) {

      return calcStringMemUsage(key) + (idSet.isEmpty() ? 88:
          calSetItemMemUsage(calcStringMemUsage(idSet.iterator().next())) * idSet.size());

    }

    private int calSetItemMemUsage(int elemMemUsage){
        // 44 = HashMapNode, 16 = table array allocation
        return 44 + 16 + elemMemUsage;
    }

    private int calcStringMemUsage(String s){
      //30 = StringHeader, 24 = ByteArrayHeader
      return 30 + 24 + s.length();

    }
  }
}
