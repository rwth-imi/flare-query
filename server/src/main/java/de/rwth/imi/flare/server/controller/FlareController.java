package de.rwth.imi.flare.server.controller;

import de.rwth.imi.flare.server.QueryFormat;
import de.rwth.imi.flare.server.services.QueryEvaluator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class FlareController {

    private final QueryEvaluator queryEval;

    public FlareController(QueryEvaluator queryEval){
        this.queryEval = queryEval;
    }

    @PostMapping(path = "/executeQuery")
    public ResponseEntity<String> executeQuery(@RequestBody String query, @RequestHeader("Accept-Encoding") QueryFormat format){
        int queryResponse = this.queryEval.evaluate(query, format);
        return ResponseEntity.ok().body(String.valueOf(queryResponse));
    }
}
