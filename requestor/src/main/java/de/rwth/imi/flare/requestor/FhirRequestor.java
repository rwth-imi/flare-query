package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;

/**
 * Requestor implementation, takes a single criterion, builds a FHIR Query from
 * it, and executes it
 */
public class FhirRequestor implements de.rwth.imi.flare.api.Requestor {

    private final FhirRequestorConfig config;

    /**
     * @param requestorConfig Configuration to be used when crafting requests
     */
    public FhirRequestor(FhirRequestorConfig requestorConfig) {
        this.config = requestorConfig;
    }

    /**
     * Builds the query string specified by the criterion, then executes said
     * query string
     *
     * @param searchCriterion single criterion
     * @param includedIds
     * @return Stream that contains the results for the given criterion
     */
    @Override
    public Stream<FlareResource> execute(Criterion searchCriterion, Set<String> includedIds) {
        URI requestUrl;
        try {
            requestUrl = buildRequestUrl(searchCriterion, includedIds);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        System.out.println(requestUrl);
        String pagecount = this.config.getPageCount();
        FhirSearchRequest fhirSearchRequest = this.config.getAuthentication()
                .map((auth) -> new FhirSearchRequest(requestUrl, auth, pagecount))
                .orElseGet(() -> new FhirSearchRequest(requestUrl, pagecount));

        return createStream(fhirSearchRequest);
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
            requestUrl = buildRequestUrl(searchCriterion, null);
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


    private URI buildRequestUrl(Criterion search, Set<String> includedIds) throws URISyntaxException {
        // TODO: Find a way to properly concat URLs in Java

        String searchQuery = SearchQueryStringBuilder.constructQueryString(search);
        String searchUrl = config.getBaseURI().toString() + searchQuery;
        if (includedIds != null) {
            searchUrl = searchUrl + "?_id" + String.join(",", includedIds);
        }
        return new URI(searchUrl);
    }
}
