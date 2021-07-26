package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Requestor implements de.rwth.imi.flare.api.Requestor {
    private final URI serverBaseUrl;

    public Requestor(URI serverBaseUrl){
        this.serverBaseUrl = serverBaseUrl;
    }

    @Override
    public Stream<FlareResource> execute(Criterion search) {
        URI requestUrl;
        try {
            requestUrl = buildRequestUrl(search);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Request request = new Request(requestUrl);
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
        String searchUrl = serverBaseUrl.toString() + searchQuery;
        return new URI(searchUrl);
    }
}
