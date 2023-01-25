package de.rwth.imi.flare.requestor;

import ca.uhn.fhir.context.FhirContext;
import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jcs3.access.exception.CacheException;
import org.jetbrains.annotations.NotNull;
import org.apache.commons.jcs3.JCS;
import org.apache.commons.jcs3.access.CacheAccess;

/**
 * Requestor implementation, takes a single criterion, builds a FHIR Query from
 * it, and executes it
 */
@Slf4j
public class FhirRequestor implements de.rwth.imi.flare.api.Requestor {

  private CacheAccess<String, HashMap<String,Set<String>>> cache = null;
  private final int cacheRefreshTimeInDays = 7;
  private HashMap<String, CompletableFuture<Set<String>>> currentlyRequestingQueries = new HashMap<String, CompletableFuture<Set<String>>>();
  private final FhirRequestorConfig config;
  private final FhirContext fhirR4Context = FhirContext.forR4();
  private final Executor executor;

  /**
   * @param executor
   * @param requestorConfig Configuration to be used when crafting requests
   */
  public FhirRequestor(FhirRequestorConfig requestorConfig, Executor executor) {
    this.executor = executor;
    this.config = requestorConfig;
    try{
      this.cache = JCS.getInstance("default");
    }catch(CacheException e){
      log.debug("Problem initializing cache: {}", e.getMessage() );
    }

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
    CompletableFuture<Set<String>> ongoingRequest = currentlyRequestingQueries.get(urlString);
    if (ongoingRequest != null) {
      log.debug("Same request ongoing. Not starting new request");
      return ongoingRequest;
    }

    HashMap<String, Set<String>> cacheEntry = cache.get(urlString); //this won't work if the base url changed in the meantime (e.g. different port because of testcontainers)
    if(cacheEntry != null){
      if(mustRefreshEntry(cacheEntry)) {
        log.debug("Url " + urlString + " cached, but too long ago. Requesting again...");
        return getSetFlareStream(urlString,this.executor);
      }
      return  CompletableFuture.completedFuture(cacheEntry.get("ids"));
    }else{
      return getSetFlareStream(urlString, this.executor);
    }

  }


  @NotNull
  private CompletableFuture<Set<String>> getSetFlareStream(String requestUrl, Executor executor) {
    log.debug("FHIR Search: " + requestUrl + " not cached or refreshing...");
    CompletableFuture<Set<String>> compFuture =  CompletableFuture.supplyAsync(() -> {
      String pagecount = this.config.getPageCount();
      FhirSearchRequest fhirSearchRequest = this.config.getAuthentication()
              .map((auth) -> new FhirSearchRequest(URI.create(requestUrl), auth, pagecount, fhirR4Context))
              .orElseGet(() -> new FhirSearchRequest(URI.create(requestUrl), pagecount, fhirR4Context));
      Set<String> flareStream = createStream(fhirSearchRequest)
              .map(FlareResource::getPatientId)
              .collect(Collectors.toSet());
      log.debug("FHIR Search: " + requestUrl + " finished execution, writing to cache...");

      putIdsInCache(requestUrl, flareStream);
      return flareStream;
    }, executor);

    this.currentlyRequestingQueries.put(requestUrl, compFuture);
    log.debug("Noted url " + requestUrl + " as ongoing request");
    compFuture.thenApply(s -> {
      this.currentlyRequestingQueries.remove(requestUrl);
      log.debug("removed url " + requestUrl + " from ongoing requests");
      return s;});

    return compFuture;
  }

  private void putIdsInCache(String requestUrl, Set<String> flareStream){
    HashMap<String, Set<String>> hashMap = new HashMap<String, Set<String>>();
    hashMap.put("ids", flareStream);
    hashMap.put("lastRefreshTime", new HashSet<String>(List.of(LocalDateTime.now().toString())));
    cache.put(requestUrl, hashMap);
  }

  private boolean mustRefreshEntry(HashMap<String, Set<String>> cacheEntry){
    LocalDateTime lastRequestTime = LocalDateTime.parse(cacheEntry.get("lastRefreshTime").toArray()[0].toString());
    long timeSinceLastRequest = Duration.between(lastRequestTime, LocalDateTime.now()).toDays();
    if(timeSinceLastRequest > cacheRefreshTimeInDays){
        return true;
    }
    return false;
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
