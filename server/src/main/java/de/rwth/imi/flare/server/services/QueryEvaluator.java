package de.rwth.imi.flare.server.services;

import de.rwth.imi.flare.api.Executor;
import de.rwth.imi.flare.api.FhirResourceMapper;
import de.rwth.imi.flare.server.QueryFormat;
import org.springframework.stereotype.Service;

@Service
public class QueryEvaluator {

    private final Executor executor;
    private final FhirResourceMapper mapper;

    public QueryEvaluator(Executor executor, FhirResourceMapper mapper){

        this.executor = executor;
        this.mapper = mapper;
    }

    public int evaluate(String query, QueryFormat format){
        return 0;
    }
}
