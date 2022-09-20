package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.requestor.FhirRequestorConfig;

import de.rwth.imi.flare.requestor.FlareThreadPoolConfig;
import java.net.Authenticator;
import java.net.URI;
import java.util.Optional;

public class AuthlessRequestorConfig implements FhirRequestorConfig{
    private final URI baseURI;
    private final String pagecount;
    private final boolean postpaging;
    private final FlareThreadPoolConfig threadPoolConfig;

    public AuthlessRequestorConfig(URI baseUri, String pagecount, boolean postpaging, FlareThreadPoolConfig threadPoolConfig){
        this.baseURI = baseUri;
        this.pagecount = pagecount;
        this.postpaging = postpaging;
        this.threadPoolConfig = threadPoolConfig;

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

    @Override
    public boolean getPostPaging() {
        return this.postpaging;
    }

    @Override
    public FlareThreadPoolConfig getThreadPoolConfig() {
        return this.threadPoolConfig;
    }
}
