package de.rwth.imi.flare.requestor;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.requestor.model.Bundle;
import de.rwth.imi.flare.requestor.model.Entry;
import de.rwth.imi.flare.requestor.model.Link;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Authenticator;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Iterates over the paged results of a given FHIR search request
 */
@Slf4j
public class FhirSearchRequest implements Iterator<FlareResource> {

    private URI nextPageUri;
    //Stack of results returned by last request
    private final Deque<FlareResource> remainingPageResults;
    private final HttpClient client;
    private final String pagecount;
    private final ObjectMapper mapper = new ObjectMapper();

    public FhirSearchRequest(URI fhirRequestUrl, Authenticator auth, String pagecount) {
        this(fhirRequestUrl, HttpClient.newBuilder().authenticator(auth).build(), pagecount);
    }

    public FhirSearchRequest(URI fhirRequestUrl, String pagecount) {
        this(fhirRequestUrl, HttpClient.newBuilder().build(), pagecount);
    }

    private FhirSearchRequest(URI fhirRequestUrl, HttpClient client, String pagecount) {
        this.nextPageUri = fhirRequestUrl;
        this.client = client;
        this.pagecount = pagecount;
        this.remainingPageResults = new LinkedBlockingDeque<>();
        // Execute before any iteration to make sure requests with empty response set don't lead to a true hasNext
        this.ensureStackFullness(true);
    }

    @Override
    public boolean hasNext() {
        return this.nextPageUri != null || !this.remainingPageResults.isEmpty();
    }

    @Override
    public FlareResource next() {
        ensureStackFullness(false);
        return this.remainingPageResults.pop();
    }

    /**
     * Fetches next page if stack isn't full, and turns checked exceptions that should not be thrown into unchecked ones
     * @param sendPostRequest Determines whether the request is sent via POST or GET
     */
    private void ensureStackFullness(boolean sendPostRequest) throws NoSuchElementException {
        if(this.remainingPageResults.isEmpty()){
            if(this.nextPageUri != null){
                try {
                    fetchNextPage(sendPostRequest);
                }
                // If these Exceptions get thrown, execution can not continue.
                catch (InterruptedException | URISyntaxException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    log.error("Error Connecting to FHIR Server with URL " + nextPageUri);
                    throw new UncheckedIOException(e);
                }
            }
            else{
                throw new NoSuchElementException();
            }
        }
    }

    /**
     * Fetches the next page of search results and updates {@link #remainingPageResults} and {@link #nextPageUri}
     * @param sendPostRequest Determines whether the request is sent via POST or GET
     */
    private void fetchNextPage(boolean sendPostRequest) throws IOException, InterruptedException, URISyntaxException {
        HttpRequest req = sendPostRequest ? buildPostRequest() : HttpRequest.newBuilder().uri(nextPageUri).GET().build();
        executeRequestAndProcessResponse(req);
    }

    /**
     * creates an initial POST Request for a FHIR Search.
     * This is done to bypass the 2.083 character limit for a URL in a GET request.
     * @return post request
     */
    private HttpRequest buildPostRequest(){
        String uri = this.nextPageUri.getScheme() + "://" + this.nextPageUri.getAuthority() + this.nextPageUri.getPath() + "/_search";
        String query = this.nextPageUri.getQuery();

        query = query + "&_elements=" + queryElements();

        if (!this.pagecount.isEmpty()){
            query = query + "&_count=" + this.pagecount;
        }

        return HttpRequest.newBuilder(URI.create(uri))
                .header("Prefer", "handling=strict")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(query))
                .build();
    }

    /*
     * The elements the FHIR server should return in resources. For patients the id is sufficient and for all other
     * resource types, we need the subject reference.
     */
    private String queryElements() {
        return nextPageUri.getPath().endsWith("Patient")
                ? "id"
                : nextPageUri.getPath().endsWith("Immunization")
                || nextPageUri.getPath().endsWith("Consent")
                ? "patient"
                : "subject";
    }

    /**
     * Executes a given HttpRequest.
     * Parses both the nextPageLink and the resources contained in the response bundle
     *
     * @param req request to be executed
     * @throws IOException Thrown when a non 2xx http code is received
     * @throws InterruptedException Thrown by the HttpClient
     * @throws URISyntaxException Thrown when the FHIR Server returns a malformed URI
     */
    private void executeRequestAndProcessResponse(HttpRequest req) throws IOException, InterruptedException, URISyntaxException {
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()/ 100 != 2){
            throw new IOException("Received HTTP status code indicating request failure: " + response.statusCode());
        }

        Bundle searchBundle = mapper.readValue(response.body(), Bundle.class);
        extractResourcesFromBundle(searchBundle);
        extractNextPageLink(searchBundle);
    }

    /**
     * Extracts the resources contained in a given bundle into the {@link #remainingPageResults} stack
     * @param searchBundle Response to a search request currently being processed
     */
    private void extractResourcesFromBundle(Bundle searchBundle) {
        List<Entry> entries = searchBundle.entry();
        for(Entry entry : entries){
            this.remainingPageResults.push(entry.resource().flareResource());
        }
    }

    /**
     * Extracts the Link to the next page of search results from a bundle into {@link #nextPageUri}
     * @param searchBundle Response to the search request currently being processed
     * @throws URISyntaxException If the URI contained in the bundle is not valid
     */
    private void extractNextPageLink(Bundle searchBundle) throws URISyntaxException {
        String nextLink = searchBundle.linkWithRel("next").map(Link::url).orElse(null);
        if (nextLink == null) {
            this.nextPageUri = null;
        } else {
            this.nextPageUri = new URI(nextLink);
        }
    }
}
