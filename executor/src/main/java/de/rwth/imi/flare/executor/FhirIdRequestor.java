package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.requestor.FhirRequestor;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class FhirIdRequestor {
    FhirRequestorConfig config;
    Executor futureExecutor;
    Map<Criterion, Set<String>> cache;

    public FhirIdRequestor(FhirRequestorConfig config, Executor futureExecutor){
        this.config = config;
        this.futureExecutor = futureExecutor;
        this.cache = new HashMap<>();
    }

    /**
     * Get all ids fulfilling a given criterion
     */
    public CompletableFuture<Set<String>> getPatientIdsFittingCriterion(Criterion criterion) {
        if(isCached(criterion)){
            return getCachedPatientIdsFittingCriterion(criterion);
        }else{
            FhirRequestor requestor = new FhirRequestor(config);
            CompletableFuture<Set<String>> ret = CompletableFuture.supplyAsync(() -> requestor.execute(criterion)
                    .map(FlareResource::getPatientId)
                    .collect(Collectors.toSet()), futureExecutor);
            return ret.thenApply(idSet -> addCachedPatientIdsFittingCriterion(criterion, idSet));
        }
    }

    private Set<String> addCachedPatientIdsFittingCriterion(Criterion criterion, Set<String> idSet) {
        cache.put(criterion, idSet);
        return idSet;
    }

    private boolean isCached(Criterion criterion) {
        return cache.containsKey(criterion);
    }

    private CompletableFuture<Set<String>> getCachedPatientIdsFittingCriterion(Criterion criterion) {
        return CompletableFuture.completedFuture(cache.get(criterion));
    }
}
