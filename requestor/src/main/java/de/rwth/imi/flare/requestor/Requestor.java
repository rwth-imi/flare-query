package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Requestor implementation, takes a single criterion, builds a FHIR Query from it, and executes it
 */
public class Requestor implements de.rwth.imi.flare.api.Requestor {
    private final RequestorConfig config;

    /**
     * @param requestorConfig Configuration to be used when crafting requests
     */
    public Requestor(RequestorConfig requestorConfig) {
        this.config = requestorConfig;
    }

    /**
     * Builds the query string specified by the criterion, then executes said query string
     * @param searchCriterion single criterion
     * @return Stream that contains the results for the given criterion
     */
    @Override
    public Stream<FlareResource> execute(Criterion searchCriterion) {
        URI requestUrl;
        try {
            requestUrl = buildRequestUrl(searchCriterion);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Request request = new Request(requestUrl, this.config.getAuthentication());
        return createStream(request);
    }

    @NotNull
    private Stream<FlareResource> createStream(Request request) {
        Iterable<FlareResource> streamSource = () -> request;
        return StreamSupport.stream(streamSource.spliterator(), false);
    }

    private URI buildRequestUrl(Criterion search) throws URISyntaxException {
        // TODO: Find a way to properly concat URLs in Java
        String searchQuery = QueryStringBuilder.constructQueryString(search);
        String searchUrl = config.getBaseURI().toString() + searchQuery;
        return new URI(searchUrl);
    }
}
