package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.requestor.FhirRequestorConfig;

import java.net.Authenticator;
import java.net.URI;
import java.util.Optional;

public class AuthlessRequestorConfig implements FhirRequestorConfig{
    private final URI baseURI;

    public AuthlessRequestorConfig(URI baseUri){
        this.baseURI = baseUri;
    }

    @Override
    public Optional<Authenticator> getAuthentication() {
        return Optional.empty();
    }

    @Override
    public URI getBaseURI() {
        return this.baseURI;
    }
}
