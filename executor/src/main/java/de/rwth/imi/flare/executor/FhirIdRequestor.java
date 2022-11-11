package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.requestor.CacheConfig;
import de.rwth.imi.flare.requestor.FhirRequestor;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FhirIdRequestor {
    FhirRequestor requestor;

    public FhirIdRequestor(FhirRequestor requestor) {
        this.requestor = requestor;
    }

    /**
     * Get all ids fulfilling a given criterion
     */
    public CompletableFuture<Set<String>> getPatientIdsFittingCriterion(Criterion criterion) {
        return requestor.execute(criterion);
    }


}
