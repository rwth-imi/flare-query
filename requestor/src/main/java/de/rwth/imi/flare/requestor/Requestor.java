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

public class Requestor implements de.rwth.imi.flare.api.Requestor {
    private final URI serverBaseUrl;
    private final Authenticator auth;
    private Properties properties;

    public Requestor(URI serverBaseUrl) {
        properties = loadProperties();
        this.serverBaseUrl = serverBaseUrl;
        this.auth = createAuth();
    }

    private Properties loadProperties() {
        this.properties = new Properties();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.properties");
        try {
            this.properties.load(inputStream);
        } catch (IOException e) {/*This isn't going to happen*/}
        return properties;
    }

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

    @Override
    public Stream<FlareResource> execute(Criterion search) {
        URI requestUrl;
        try {
            requestUrl = buildRequestUrl(search);
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
