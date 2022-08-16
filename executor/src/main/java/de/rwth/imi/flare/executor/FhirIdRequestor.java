package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.requestor.CacheConfig;
import de.rwth.imi.flare.requestor.FhirRequestor;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FhirIdRequestor {
    FhirRequestorConfig config;
    Executor futureExecutor;
    FhirRequestor requestor;

    public FhirIdRequestor(FhirRequestorConfig config, CacheConfig cacheConfig, Executor futureExecutor) {
        this.config = config;
        this.futureExecutor = futureExecutor;
        this.requestor = new FhirRequestor(config, cacheConfig);
    }

    /**
     * Get all ids fulfilling a given criterion
     */
    public CompletableFuture<Set<String>> getPatientIdsFittingCriterion(Criterion criterion) {
        return requestor.execute(criterion);
    }


}
