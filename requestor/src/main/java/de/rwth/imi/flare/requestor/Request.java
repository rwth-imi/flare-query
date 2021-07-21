package de.rwth.imi.flare.requestor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.requestor.FlareResources.FlareResourceImpl;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;

public class Request implements Iterator<FlareResource> {
    private URI currentRequestUrl;
    private Stack<FlareResourceImpl> remainingPageResults;
    private final HttpClient client;
    private IParser fhirParser;

    public Request(URI requestUrl){
        this.currentRequestUrl = requestUrl;
        this.client = HttpClient.newBuilder().build();
        this.fhirParser = FhirContext.forR4().newJsonParser();
        this.remainingPageResults = new Stack<>();
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
                catch (IOException | InterruptedException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
            else{
                throw new NoSuchElementException();
            }
        }
    }

    private void fetchNextPage() throws IOException, InterruptedException, URISyntaxException {
        HttpRequest req = HttpRequest.newBuilder().uri(currentRequestUrl).GET().build();
        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
        if(response.statusCode()/ 200 != 2){
            // TODO error handling
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
