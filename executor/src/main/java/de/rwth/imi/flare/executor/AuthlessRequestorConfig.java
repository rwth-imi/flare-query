package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.requestor.FhirRequestorConfig;

import java.net.Authenticator;
import java.net.URI;

public class AuthlessRequestorConfig implements FhirRequestorConfig{
    private final URI baseURI;

    public AuthlessRequestorConfig(URI baseUri){
        this.baseURI = baseUri;
    }

    @Override
    public Authenticator getAuthentication() {
        return null;
    }

    @Override
    public URI getBaseURI() {
        return this.baseURI;
    }
}
