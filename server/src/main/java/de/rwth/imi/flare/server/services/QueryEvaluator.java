package de.rwth.imi.flare.server.services;

import de.rwth.imi.flare.api.Executor;
import de.rwth.imi.flare.api.FhirResourceMapper;
import de.rwth.imi.flare.api.FlareParser;

import de.rwth.imi.flare.server.QueryFormat;
import org.springframework.stereotype.Service;
import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.parser.csq.ParserCSQ;
import de.rwth.imi.flare.parser.i2b2.ParserI2B2;

import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Evaluates query by parsing, mapping and executing the provided query string
 */
@Service
public class QueryEvaluator {

    private final Executor executor;
    private final FhirResourceMapper mapper;

    /**
     * Constructor to load bean objects for execution and mapping
     * @param executor query executer
     * @param mapper query mapper
     */
    public QueryEvaluator(Executor executor, FhirResourceMapper mapper){
        this.executor = executor;
        this.mapper = mapper;
    }

    /**
     * Evaluate query and retrieve population
     * @param query query string from post request body
     * @param format parser foramt requeested
     * @return population count
     * @throws TransformerConfigurationException
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public CompletableFuture<Integer> evaluate(String query, String format) throws TransformerConfigurationException, IOException, ExecutionException, InterruptedException {
        Query parsedQuery = parseQuery(query, format);
        Query mappedQuery = mapQuery(parsedQuery);
        return executeQuery(mappedQuery);
    }

    /**
     * parses, mappes and translates a posted query to the StructuredQuery format.
     * @param query posted query from post request
     * @param format
     * @return StructuredQuery
     * @throws TransformerConfigurationException
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public List<List<List<String>>> translate(String query, String format) throws TransformerConfigurationException, IOException, ExecutionException, InterruptedException {
        Query parsedQuery = parseQuery(query, format);
        Query mappedQuery = mapQuery(parsedQuery);
        return translateQuery(mappedQuery);
    }


    private Query parseQuery(String query, String format) throws IOException, TransformerConfigurationException {
        FlareParser parser = getParser(format);
        return parser.parse(query);
    }
    private FlareParser getParser(String format) throws TransformerConfigurationException {
        FlareParser parser = null;
        switch (format) {
            case "application/sq+json" -> parser = new ParserCSQ();
            case "text/i2b2" -> parser = new ParserI2B2();
        }
        return parser;
    }

    private Query mapQuery(Query parsedQuery) {
        Query mappedQuery = null;
        try {
            mappedQuery = this.mapper.mapResources(parsedQuery).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return mappedQuery;
    }

    private CompletableFuture<Integer> executeQuery(Query mappedQuery) throws ExecutionException, InterruptedException {
        return this.executor.calculatePatientCount(mappedQuery);
    }

    /**
     * executes translations of the provided mappedQuery
     * @param mappedQuery
     * @return
     */
    private List<List<List<String>>> translateQuery(Query mappedQuery) {
        return this.executor.translateMappedQuery(mappedQuery);
    }
}
