package de.rwth.imi.flare.requestor;

import ca.uhn.fhir.context.FhirContext;
import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Requestor implementation, takes a single criterion, builds a FHIR Query from
 * it, and executes it
 */
@Slf4j
public class FhirRequestor implements de.rwth.imi.flare.api.Requestor {

  private final FhirRequestorConfig config;
  private final FhirContext fhirR4Context = FhirContext.forR4();
  private final Executor executor;
  Cache cache;

  /**
   * @param executor
   * @param requestorConfig Configuration to be used when crafting requests
   */
  public FhirRequestor(FhirRequestorConfig requestorConfig,
      CacheConfig cacheConfig, Executor executor) {
    this.config = requestorConfig;
    this.cache = new Cache(
            cacheConfig.getCleanCycleMS(),
            cacheConfig.getEntryLifetimeMS(),
            cacheConfig.getMaxCacheEntries(),
            cacheConfig.getUpdateExpiryAtAccess(),
            cacheConfig.getDeleteAllEntriesOnCleanup()
    );
    this.executor = executor;
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
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    String urlString = requestUrl.toString();
    cache.cleanCache();

    if(cache.isCached(urlString)){
      log.debug("Get cached FHIR Search: " + urlString);
      return CompletableFuture.completedFuture(cache.getCachedPatientIdsFittingRequestUrl(urlString));
    }
    else {
      log.debug("FHIR Search: " + urlString + " not cached - executing...");
      CompletableFuture<Set<String>> ret = CompletableFuture.supplyAsync(() -> {
        String pagecount = this.config.getPageCount();
        FhirSearchRequest fhirSearchRequest = this.config.getAuthentication()
                .map((auth) -> new FhirSearchRequest(requestUrl, auth, pagecount, fhirR4Context))
                .orElseGet(() -> new FhirSearchRequest(requestUrl, pagecount, fhirR4Context));
        Set<String> flareStream = createStream(fhirSearchRequest)
                .map(FlareResource::getPatientId)
                .collect(Collectors.toSet());
        log.debug("FHIR Search: " + urlString + " finished execution, writing to cache...");
        return flareStream;
      }, executor);
      ret =  ret.thenApply(idSet -> cache.addCachedPatientIdsFittingRequestUrl(urlString, idSet));
      return ret;
      }
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


  private URI buildRequestUrl(Criterion search) throws URISyntaxException {
    // TODO: Find a way to properly concat URLs in Java
    String searchQuery = SearchQueryStringBuilder.constructQueryString(search);
    String searchUrl = config.getBaseURI().toString() + searchQuery;
    return new URI(searchUrl);
  }
}
