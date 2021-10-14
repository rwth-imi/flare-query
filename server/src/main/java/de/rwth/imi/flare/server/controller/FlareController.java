package de.rwth.imi.flare.server.controller;

import de.rwth.imi.flare.server.QueryFormat;
import de.rwth.imi.flare.server.services.QueryEvaluator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Controller
public class FlareController {

    private final QueryEvaluator queryEval;

    public FlareController(QueryEvaluator queryEval){
        this.queryEval = queryEval;
    }

    /**
     * Enables post mapping of structured queries to the specified endpoint "/executeQuery"
     * @param query Query from body
     * @param format Encoding, either I2B2 or CSQ
     * @return
     * @throws TransformerConfigurationException
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @PostMapping(path = "/executeQuery")
    //return "New Endpoint";
    public ResponseEntity<String> executeQuery(@RequestBody String query, @RequestHeader("Accept-Encoding") QueryFormat format) throws TransformerConfigurationException, IOException, ExecutionException, InterruptedException {
        int queryResponse = this.queryEval.evaluate(query, format);
        return ResponseEntity.ok().body(String.valueOf(queryResponse));
    }
}
