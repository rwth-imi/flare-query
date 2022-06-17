package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.requestor.FhirRequestor;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class FhirIdRequestor {
    FhirRequestorConfig config;
    Executor futureExecutor;

    public FhirIdRequestor(FhirRequestorConfig config, Executor futureExecutor){
        this.config = config;
        this.futureExecutor = futureExecutor;
    }

    /**
     * Get all ids fulfilling a given criterion
     */
    public CompletableFuture<Set<String>> getPatientIdsFittingCriterion(Criterion criterion) {
        // TODO bessere wart/testbarkeit, Interface zum mocking auslagern oder Lambda Function-Interface
        FhirRequestor requestor = new FhirRequestor(config);
        return CompletableFuture.supplyAsync(() -> requestor.execute(criterion)
                .map(FlareResource::getPatientId)
                .collect(Collectors.toSet()), futureExecutor);
    }
}
