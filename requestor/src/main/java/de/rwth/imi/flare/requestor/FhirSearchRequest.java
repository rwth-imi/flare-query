package de.rwth.imi.flare.requestor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.rwth.imi.flare.api.FlareResource;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

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
public class FhirSearchRequest implements Iterator<FlareResource> {
    private URI nextPageUri;
    //Stack of results returned by last request
    private final Deque<FlareResourceImpl> remainingPageResults;
    private final HttpClient client;
    // Parses only JSON FHIR responses
    private final IParser fhirParser;

    public FhirSearchRequest(URI fhirRequestUrl, Authenticator auth){
        this.nextPageUri = fhirRequestUrl;
        if(auth != null)
            this.client = HttpClient.newBuilder().authenticator(auth).build();
        else
            this.client = HttpClient.newBuilder().build();
        this.fhirParser = FhirContext.forR4().newJsonParser();
        this.remainingPageResults = new LinkedBlockingDeque<>();
        // Execute before any iteration to make sure requests with empty response set don't lead to a true hasNext
        this.ensureStackFullness();
    }

    @Override
    public boolean hasNext() {
        return this.nextPageUri != null || !this.remainingPageResults.isEmpty();
    }

    @Override
    public FlareResource next() {
        ensureStackFullness();
        return this.remainingPageResults.pop();
    }

    /**
     * Fetches next page if stack isn't full, and turns checked exceptions that should not be thrown into unchecked ones
     */
    private void ensureStackFullness() throws NoSuchElementException {
        if(this.remainingPageResults.isEmpty()){
            if(this.nextPageUri != null){
                try {
                    fetchNextPage();
                }
                // If these Exceptions get thrown, execution can not continue.
                catch (InterruptedException | URISyntaxException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
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
     */
    private void fetchNextPage() throws IOException, InterruptedException, URISyntaxException {
        System.out.println("Executing next URI: " + nextPageUri.toString());
        HttpRequest req = HttpRequest.newBuilder().uri(nextPageUri).GET().build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()/ 100 != 2){
            throw new IOException("Received HTTP status code indicating request failure: " + response.statusCode());
        }

        Bundle searchBundle = this.fhirParser.parseResource(Bundle.class, response.body());
        extractResourcesFromBundle(searchBundle);
        extractNextPageLink(searchBundle);
    }

    /**
     * Extracts the resources contained in a given bundle into the {@link #remainingPageResults} stack
     * @param searchBundle Response to a search request currently being processed
     */
    private void extractResourcesFromBundle(Bundle searchBundle) {
        List<Bundle.BundleEntryComponent> entries = searchBundle.getEntry();
        for(Bundle.BundleEntryComponent entry : entries){
            Resource resource = entry.getResource();
            this.remainingPageResults.push(new FlareResourceImpl(resource));
        }
    }

    /**
     * Extracts the Link to the next page of search results from a bundle into {@link #nextPageUri}
     * @param searchBundle Response to the search request currently being processed
     * @throws URISyntaxException If the URI contained in the bundle is not valid
     */
    private void extractNextPageLink(Bundle searchBundle) throws URISyntaxException {
        Bundle.BundleLinkComponent nextLink = searchBundle.getLink(IBaseBundle.LINK_NEXT);
        if(nextLink == null){
            this.nextPageUri = null;
        }
        else{
            this.nextPageUri = new URI(nextLink.getUrl());
        }
    }
}
