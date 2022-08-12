package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.requestor.FhirRequestorConfig;
import de.rwth.imi.flare.requestor.FlareThreadPoolConfig;
import org.jetbrains.annotations.NotNull;

import java.net.Authenticator;
import java.net.URI;
import java.util.Optional;

public class TokenAuthRequestorConfig implements FhirRequestorConfig {
    private final URI baseURI;
    private final String token;

    public TokenAuthRequestorConfig(URI baseURI, String token){
        this.baseURI = baseURI;
        this.token = token;
    }

    @Override
    public Optional<Authenticator> getAuthentication() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getToken() {
        return Optional.ofNullable(token);
    }

    @Override
    public URI getBaseURI() {
        return this.baseURI;
    }

    /**
     * Creates a basic Authenticator
     * @return Authenticator containing credentials for the FHIR server
     */
    @NotNull
    public String getPageCount() {
        return "500";
    }

    /**
     * Creates a basic Authenticator
     * @return Authenticator containing credentials for the FHIR server
     */
    @NotNull
    public FlareThreadPoolConfig getThreadPoolConfig() {
        return new FlareThreadPoolConfig(4,16,10);
    }
}
