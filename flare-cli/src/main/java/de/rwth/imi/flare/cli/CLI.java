package de.rwth.imi.flare.cli;

import de.rwth.imi.flare.api.Executor;
import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.executor.BasicAuthRequestorConfig;
import de.rwth.imi.flare.parser.i2b2.ParserI2B2;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
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

@Command(name = "flare-cli", mixinStandardHelpOptions = true,
        description = "Executes a given query")
class CLI implements Callable<Integer> {

    @Parameters(index = "0", description = "The request to be executed by FLARE")
    private File requestFile;

    @Parameters(index = "1", description = "Mapping file")
    private File mappingFile;

    @Parameters(index = "2", description = "FHIR-server base uri")
    private String fhirBaseUri;

    @Parameters(index = "3", description = "FHIR-server username")
    private String userName;

    @Parameters(index = "4", description = "FHIR-server password")
    private String password;

    @Option(names = {"-f", "query-format"}, description = "Format of the query to be executed")
    private Parser algorithm = Parser.I2B2;

    @Override
    public Integer call() throws Exception {
        Query parsedQuery = parseQuery();
        Query mappedQuery = mapQuery(parsedQuery);
        int queryResult = executeQuery(mappedQuery);
        System.out.println(queryResult);
        return 0;
    }

    private int executeQuery(Query mappedQuery) throws URISyntaxException, ExecutionException, InterruptedException {
        Executor executor = new FlareExecutor(new BasicAuthRequestorConfig(new URI(this.fhirBaseUri), this.userName, this.password));
        CompletableFuture<Integer> integerCompletableFuture = executor.calculatePatientCount(mappedQuery);
        return integerCompletableFuture.get();
    }

    /**
     * TODO: Implement Query mapping
     */
    private Query mapQuery(Query parsedQuery) {
        return parsedQuery;
    }

    private Query parseQuery() throws IOException, TransformerConfigurationException {
        String query = Files.readString(requestFile.toPath());
        FlareParser parser = getParser();
        return parser.parse(query);
    }

    private FlareParser getParser() throws TransformerConfigurationException {
        switch (this.algorithm) {
            case CSQ -> new ParserCSQ();
            case I2B2 -> new ParserI2B2();
        }
        return null;
    }

    enum Parser {
        I2B2,
        CSQ
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CLI()).execute(args);
        System.exit(exitCode);
    }
}