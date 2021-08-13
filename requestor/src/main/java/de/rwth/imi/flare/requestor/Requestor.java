package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Requestor implementation, takes a single criterion, builds a FHIR Query from it, and executes it
 */
public class Requestor implements de.rwth.imi.flare.api.Requestor {
    private final URI serverBaseUrl;
    private final Authenticator auth;
    private Properties properties;

    /**
     * @param serverBaseUrl Base url of the FHIR server requests should be executed upon
     */
    public Requestor(URI serverBaseUrl) {
        loadProperties();
        this.serverBaseUrl = serverBaseUrl;
        this.auth = createAuth();
    }

    /**
     * Loads the config file
     */
    private void loadProperties() {
        this.properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("requestorConfig.properties");
        try {
            this.properties.load(inputStream);
        } catch (IOException e) {/*This isn't going to happen*/}
    }

    /**
     * Creates an Authenticator from "user" and "password" from the {@link #properties}
     * @return Authenticator containing credentials for the FHIR server
     */
    @NotNull
    private Authenticator createAuth() {
        String user = this.properties.getProperty("user");
        String password = this.properties.getProperty("password");
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        user,
                        password.toCharArray());
            }
        };
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
        Request request = new Request(requestUrl, this.auth);
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
