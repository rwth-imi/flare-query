package de.rwth.imi.flare.server.controller;

import de.rwth.imi.flare.server.QueryFormat;
import de.rwth.imi.flare.server.services.QueryEvaluator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping(value = "query")
public class FlareController {

    private final QueryEvaluator queryEval;

    public FlareController(QueryEvaluator queryEval){
        this.queryEval = queryEval;
    }

    /**
     * Enables post mapping of structured queries to the specified endpoint "/executeQuery"
     * @param query Query from body
     * @param format Encoding, either I2B2 or CSQ
     */
    @PostMapping(path = "/execute")
    public ResponseEntity<String> executeQuery(@RequestBody String query, @RequestHeader("Accept-Encoding") QueryFormat format) throws TransformerConfigurationException, IOException, ExecutionException, InterruptedException {
        try {
            int queryResponse = this.queryEval.evaluate(query, format);
            return ResponseEntity.ok().body(String.valueOf(queryResponse));
        }
        catch (NoSuchElementException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
