package de.rwth.imi.flare.cli;

import de.rwth.imi.flare.api.Executor;
import de.rwth.imi.flare.api.FhirResourceMapper;
import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.mapping.lookup.NaiveLookupMapping;
import de.rwth.imi.flare.parser.i2b2.ParserI2B2;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import de.rwth.imi.flare.api.FlareParser;
import de.rwth.imi.flare.parser.csq.ParserCSQ;
import de.rwth.imi.flare.executor.FlareExecutor;

import javax.xml.transform.TransformerConfigurationException;

//TODO: Change to properties.ini
@Command(name = "flare-cli", mixinStandardHelpOptions = true,
        description = "Executes a given query")
class CLI implements Callable<Integer> {

    @Parameters(index = "0", description = "The request to be executed by FLARE")
    private File requestFile;

    @Parameters(index = "1", description = "FHIR-server base uri")
    private String fhirBaseUri;

    @Option(names = {"-u", "--username"}, description = "FHIR-server username\n if either username or password are missing no authentication will be done")
    private String userName = null;

    @Option(names = {"-p", "--password"}, description = "FHIR-server password\n if either username or password are missing no authentication will be done")
    private String password = null;

    @Option(names = {"-f", "query-format"}, description = "Format of the query to be executed")
    private QueryFormat algorithm = QueryFormat.I2B2;

    private Executor executor;

    private final FhirResourceMapper mapping;

    private FlareParser parser;

    public CLI() throws IOException {
        createExecutor();
        mapping = new NaiveLookupMapping();
    }

    private void createExecutor() {
        Authenticator auth = createAuthenticator();
        executor = new FlareExecutor(new FhirRequestorConfig() {
            @Override
            public Authenticator getAuthentication() {
                return auth;
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

    @Nullable
    private Authenticator createAuthenticator() {
        Authenticator auth = null;
        // TODO: Log warning if only one parameter is provided
        if(this.userName != null && this.password != null){
            auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            userName,
                            password.toCharArray());
                }
            };
        }
        return auth;
    }

    private FlareParser getParser() throws TransformerConfigurationException {
        FlareParser parser = null;
        switch (this.algorithm) {
            case CSQ -> parser = new ParserCSQ();
            case I2B2 -> parser = new ParserI2B2();
        }
        return parser;
    }

    @Override
    public Integer call() {
        try {

            Query parsedQuery = parseQuery();
            Query mappedQuery = mapQuery(parsedQuery);
            int queryResult = executeQuery(mappedQuery);
            System.out.println(queryResult);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            return -2;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return -3;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -4;
        }
        return 0;
    }

    private int executeQuery(Query mappedQuery) throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> integerCompletableFuture = this.executor.calculatePatientCount(mappedQuery);
        return integerCompletableFuture.get();
    }


    private Query mapQuery(Query parsedQuery) {
        try {
            parsedQuery = mapping.mapResources(parsedQuery).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return parsedQuery;
    }

    private Query parseQuery() throws IOException, TransformerConfigurationException {
        String query = Files.readString(requestFile.toPath());
        FlareParser parser = getParser();
        return parser.parse(query);
    }

    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new CLI()).execute(args);
        System.exit(exitCode);
    }
}