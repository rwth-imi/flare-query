package de.rwth.imi.flare.requestor;


import java.net.Authenticator;
import java.net.URI;
import java.util.Optional;

public interface FhirRequestorConfig {

    /**
     *
     * TODO: Figure out a way to set Header based authentication e.g. a consumer that modifies each request prior to exec
     * @return Authenticator that allows the Requestor to search for Resources on the server
     */
    Optional<Authenticator> getAuthentication();

    /**
     * @return Bearer token that allows the Requestor to search for Resources on the server
     */
    Optional<String> getToken();

    /**
     *
     * @return URI pointing to the Base address of the FHIR Server, such that concatenating /Patient/1
     * would return the Patient with ID 1
     */
    URI getBaseURI();


    /**
     *
     * @return String setting the number of resources per page
     *
     */
    String getPageCount();


    /**
     *
     * @return FlareThradPoolConfig, setting the configuration of the thread pool
     * used by Flare
     */
     FlareThreadPoolConfig getThreadPoolConfig();
}
