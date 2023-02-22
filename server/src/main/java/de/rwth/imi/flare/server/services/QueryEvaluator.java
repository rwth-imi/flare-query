package de.rwth.imi.flare.server.services;

import de.rwth.imi.flare.api.Executor;
import de.rwth.imi.flare.api.FhirResourceMapper;
import de.rwth.imi.flare.api.FlareParser;
import de.rwth.imi.flare.api.UnsupportedCriterionException;
import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.parser.csq.ParserCSQ;
import de.rwth.imi.flare.parser.i2b2.ParserI2B2;
import org.springframework.stereotype.Service;

import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Evaluates query by parsing, mapping and executing the provided query string
 */
@Service
public class QueryEvaluator {

    private final Executor executor;
    private final FhirResourceMapper mapper;

    /**
     * Constructor to load bean objects for execution and mapping
     *
     * @param executor query executer
     * @param mapper   query mapper
     */
    public QueryEvaluator(Executor executor, FhirResourceMapper mapper) {
        this.executor = executor;
        this.mapper = mapper;
    }

    /**
     * Evaluates {@code query} returning the population count.
     *
     * @param query  query string from post request body
     * @param format the format of {@code query}
     * @return population count
     * @throws TransformerConfigurationException
     * @throws IOException
     */
    public CompletableFuture<Integer> evaluate(String query, String format) throws TransformerConfigurationException, IOException {
        Query parsedQuery = parseQuery(query, format);
        return mapper.mapResources(parsedQuery).thenCompose(executor::calculatePatientCount);
    }

    /**
     * Parses, map's and translates a posted query to the some nested form of FHIR search query strings.
     *
     * @param query  posted query from post request
     * @param format the format of {@code query}
     * @return some nested form of FHIR search query strings
     * @throws TransformerConfigurationException
     * @throws IOException                       when a problem during parsing occurs
     */
    public CompletableFuture<List<List<List<String>>>> translate(String query, String format)
            throws TransformerConfigurationException, IOException {
        Query parsedQuery = parseQuery(query, format);
        return mapper.mapResources(parsedQuery)
                .thenCompose(mappedQuery -> {
                    try {
                        return CompletableFuture.completedFuture(executor.translateMappedQuery(mappedQuery));
                    } catch (UnsupportedCriterionException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                });
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
}
