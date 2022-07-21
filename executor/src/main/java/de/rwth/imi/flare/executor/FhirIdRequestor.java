package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.TerminologyCode;
import de.rwth.imi.flare.requestor.FhirRequestor;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class FhirIdRequestor {
    FhirRequestorConfig config;
    Executor futureExecutor;
    Cache cache;

    public FhirIdRequestor(FhirRequestorConfig config, Executor futureExecutor) {
        this.config = config;
        this.futureExecutor = futureExecutor;
        this.cache = new Cache();
    }

    /**
     * Get all ids fulfilling a given criterion
     */
    public CompletableFuture<Set<String>> getPatientIdsFittingCriterion(Criterion criterion, Set<String> includedIds) {
        StringBuilder sbTmp = new StringBuilder();
        TerminologyCode termCode = criterion.getTermCodes().get(0);
        sbTmp.append(termCode.getSystem()).append("|").append(termCode.getCode());
        String key = sbTmp.toString();
        cache.cleanCache();
        if (cache.isCached(key) && includedIds == null) { // Todo: when cache, when heuristics?
            return CompletableFuture.completedFuture(cache.getCachedPatientIdsFittingCriterion(key));
        } else {
            FhirRequestor requestor = new FhirRequestor(config);
            CompletableFuture<Set<String>> ret = CompletableFuture.supplyAsync(() -> requestor.execute(criterion, includedIds)
                    .map(FlareResource::getPatientId)
                    .collect(Collectors.toSet()), futureExecutor);
            if(includedIds == null) {
                ret = ret.thenApply(idSet -> cache.addCachedPatientIdsFittingTermCode(key, idSet));
            }
            return ret;
        }
    }


}
