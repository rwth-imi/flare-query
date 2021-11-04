package de.rwth.imi.flare.server.controller;

import de.rwth.imi.flare.server.QueryFormat;
import de.rwth.imi.flare.server.services.QueryEvaluator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    @PostMapping(path = "/translate")
    public ResponseEntity<List<List<List<String>>>> translateQuery(@RequestBody String query, @RequestHeader("Accept-Encoding") QueryFormat format) {
        try{
            List<List<List<String>>> translatedQuery = this.queryEval.translate(query, format);
            return ResponseEntity.ok().body(translatedQuery);
        }
        catch(NoSuchElementException e){
            //return ResponseEntity.badRequest().body(e.getMessage());
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().body(new ArrayList<>());
    }
}
