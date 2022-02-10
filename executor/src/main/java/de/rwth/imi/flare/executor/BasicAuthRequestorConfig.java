package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.requestor.FhirRequestorConfig;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;

public class BasicAuthRequestorConfig implements FhirRequestorConfig {
    private final URI baseURI;
    private final String user;
    private final String password;

    public BasicAuthRequestorConfig(URI baseURI, String user, String password){
        this.baseURI = baseURI;
        this.user = user;
        this.password = password;
    }

    @Override
    public Optional<Authenticator> getAuthentication() {
        return Optional.of(this.createAuth());
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
    private Authenticator createAuth() {
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        BasicAuthRequestorConfig.this.user,
                        BasicAuthRequestorConfig.this.password.toCharArray());
            }
        };
    }
}
