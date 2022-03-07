package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.requestor.FhirRequestorConfig;

import java.net.Authenticator;
import java.net.URI;
import java.util.Optional;

public class AuthlessRequestorConfig implements FhirRequestorConfig{
    private final URI baseURI;
    private final String pagecount;

    public AuthlessRequestorConfig(URI baseUri, String pagecount){
        this.baseURI = baseUri;
        this.pagecount = pagecount;

    }

    @Override
    public Optional<Authenticator> getAuthentication() {
        return Optional.empty();
    }

    @Override
    public URI getBaseURI() {
        return this.baseURI;
    }

    @Override
    public String getPageCount() {
        return this.pagecount;
    }
}
