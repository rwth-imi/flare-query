package de.rwth.imi.flare.cli;

import de.rwth.imi.flare.api.Executor;
import de.rwth.imi.flare.api.FhirResourceMapper;
import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.mapping.NaiveLookupMapping;
import de.rwth.imi.flare.parser.i2b2.ParserI2B2;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;
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

    @Option(names = {"-u", "--username"}, description = "FHIR-server username")
    private String userName = null;

    @Option(names = {"-p", "--password"}, description = "FHIR-server password")
    private String password = null;

    @Option(names = {"-f", "query-format"}, description = "Format of the query to be executed")
    private Parser algorithm = Parser.I2B2;

    private final Executor executor;

    private final FhirResourceMapper mapping;

    private FlareParser parser;

    public CLI() throws IOException {
        Authenticator auth = null;
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

        Authenticator finalAuth = auth;
        executor = new FlareExecutor(new FhirRequestorConfig() {
            @Override
            public Authenticator getAuthentication() {
                return finalAuth;
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
        mapping = new NaiveLookupMapping();
    }

    @Override
    public Integer call() throws Exception {
        Query parsedQuery = parseQuery();
        Query mappedQuery = mapQuery(parsedQuery);
        int queryResult = executeQuery(mappedQuery);
        System.out.println(queryResult);
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

    private FlareParser getParser() throws TransformerConfigurationException {
        FlareParser parser = null;
        switch (this.algorithm) {
            case CSQ -> parser = new ParserCSQ();
            case I2B2 -> parser = new ParserI2B2();
        }
        return parser;
    }

    enum Parser {
        I2B2,
        CSQ
    }

    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new CLI()).execute(args);
        System.exit(exitCode);
    }
}