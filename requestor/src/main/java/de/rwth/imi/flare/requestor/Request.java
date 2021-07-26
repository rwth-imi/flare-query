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

public class Request implements Iterator<FlareResource> {
    private URI currentRequestUrl;
    private final Stack<FlareResourceImpl> remainingPageResults;
    private final HttpClient client;
    private final IParser fhirParser;
    private final int attempts = 5;

    public Request(URI requestUrl, Authenticator auth){
        this.currentRequestUrl = requestUrl;
        this.client = HttpClient.newBuilder().authenticator(auth).build();
        this.fhirParser = FhirContext.forR4().newJsonParser();
        this.remainingPageResults = new Stack<>();
        // Execute before any iteration to make sure requests with empty response set don't lead to a true hasNext
        this.ensureStackFullness();
    }

    @Override
    public boolean hasNext() {
        return this.currentRequestUrl != null || !this.remainingPageResults.isEmpty();
    }

    @Override
    public FlareResource next() {
        ensureStackFullness();
        return this.remainingPageResults.pop();
    }

    private void ensureStackFullness() {
        if(this.remainingPageResults.isEmpty()){
            if(this.currentRequestUrl != null){
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

    private void fetchNextPage() throws IOException, InterruptedException, URISyntaxException {
        HttpRequest req = HttpRequest.newBuilder().uri(currentRequestUrl)
                .GET().build();

        HttpResponse<String> response;
        int connectionsAttempted = 0;
        while(true){
            try {
                response = client.send(req, HttpResponse.BodyHandlers.ofString());
                break;
            }
            catch (InterruptedException | IOException e)
            {
                if(connectionsAttempted < this.attempts-1)
                {
                    System.err.println("Failed to retrieve a url, trying again");
                    connectionsAttempted++;
                }
                else{
                    throw e;
                }
            }
        }

        if(response.statusCode()/ 100 != 2){
            throw new IOException("Received HTTP status code indicating request failure: " + response.statusCode());
        }

        Bundle searchBundle = this.fhirParser.parseResource(Bundle.class, response.body());
        extractResourcesFromBundle(searchBundle);
        extractNextPageLink(searchBundle);
    }

    private void extractResourcesFromBundle(Bundle searchBundle) {
        List<Bundle.BundleEntryComponent> entries = searchBundle.getEntry();
        for(Bundle.BundleEntryComponent entry : entries){
            Resource resource = entry.getResource();
            this.remainingPageResults.push(new FlareResourceImpl(resource));
        }
    }

    private void extractNextPageLink(Bundle searchBundle) throws URISyntaxException {
        Bundle.BundleLinkComponent nextLink = searchBundle.getLink(IBaseBundle.LINK_NEXT);
        if(nextLink == null){
            this.currentRequestUrl = null;
        }
        else{
            this.currentRequestUrl = new URI(nextLink.getUrl());
        }
    }
}
