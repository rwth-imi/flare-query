package de.rwth.imi.flare.server.controller;

import de.rwth.imi.flare.api.UnsupportedCriterionException;
import de.rwth.imi.flare.server.services.QueryEvaluator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
@CrossOrigin
@RequestMapping(value = "query")
public class FlareController {

    private final QueryEvaluator evaluator;

    public FlareController(QueryEvaluator evaluator) {
        this.evaluator = Objects.requireNonNull(evaluator);
    }

    /**
     * Enables post mapping of structured queries to the specified endpoint "/executeQuery"
     *
     * @param query  Query from body
     * @param format Encoding, either I2B2 or CSQ
     */
    @PostMapping(path = "/execute")
    public ResponseEntity<String> executeQuery(@RequestBody String query, @RequestHeader("Content-Type") String format)
            throws TransformerConfigurationException, IOException {
        var queryResponse = evaluator.evaluate(query, format);
        // TODO: don't block on async computations. just use thenApply to carry out the async computation to a upper level
        return ResponseEntity.ok().body(String.valueOf(queryResponse.join()));
    }

    @PostMapping(path = "/translate")
    public ResponseEntity<List<List<List<String>>>> translateQuery(@RequestBody String query,
                                                                   @RequestHeader("Content-Type") String format)
            throws TransformerConfigurationException, IOException {
        // TODO: don't block on async computations. just use thenApply to carry out the async computation to a upper level
        List<List<List<String>>> translatedQuery = evaluator.translate(query, format).join();
        return ResponseEntity.ok().body(translatedQuery);
    }
}
