package de.rwth.imi.flare.server.configuration;

import de.rwth.imi.flare.api.Executor;
import de.rwth.imi.flare.api.FhirResourceMapper;
import de.rwth.imi.flare.executor.FlareExecutor;
import de.rwth.imi.flare.mapping.NaiveLookupMapping;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

@Configuration
public class FlareAlgorithmConfiguration {
    @Bean
    public FhirResourceMapper mapper() throws IOException {
        return new NaiveLookupMapping();
    }

    @Bean
    public Executor executor(Optional<Authenticator> auth, @Value("flare.fhir.server") String fhirBaseUri){
        Authenticator authenticator = auth.orElse(null);
        return new FlareExecutor(new FhirRequestorConfig() {
            @Override
            public Authenticator getAuthentication() {
                return authenticator;
            }

            @Override
            public URI getBaseURI() {
                URI uri = null;
                try {
                    uri = new URI(fhirBaseUri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                return uri;
            }
        });
    }

    @Bean
    public Optional<Authenticator> createAuthenticator(
            @Value("flare.fhir.user") String userName,
            @Value("flare.fhir.password") String password) {
        if(userName != "" && password != ""){
            Authenticator auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            userName,
                            password.toCharArray());
                }
            };
            return Optional.of(auth);
        }
        return Optional.empty();
    }
}
