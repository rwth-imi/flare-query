import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.requestor.Requestor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FlareExecutor implements de.rwth.imi.flare.api.Executor {
    @Override
    public CompletableFuture<Integer> calculatePatientCount(Query mappedQuery) {
        CompletableFuture<Set<String>> includedIds = buildInclusionGroups(mappedQuery);
        // TODO: calculate excluded ids
        CompletableFuture<Set<String>> excludedIds = CompletableFuture.completedFuture(new HashSet<>());
        CompletableFuture<Set<String>> resultingIds = includedIds.thenCombineAsync(excludedIds, (strings, strings2) ->
        {
            strings.removeAll(strings2);
            return strings;
        });

        return resultingIds.thenApply(Set::size);
    }

    private CompletableFuture<Set<String>> buildInclusionGroups(Query query) {
        CompletableFuture<Set<String>> includedIds = new CompletableFuture<>();
        for (CriteriaGroup group : query.getInclusionCriteria()) {
            includedIds.thenCombineAsync(buildInclusionGroup(group), Set::addAll);
        }

        return includedIds;
    }

    private CompletableFuture<Set<String>> buildInclusionGroup(CriteriaGroup group) {
        CompletableFuture<Set<String>> idsIncludedInGroup = null;
        for (Criterion criterion : group.getCriteria()) {
            CompletableFuture<Set<String>> evaluableCriterion = buildCriterion(criterion);
            if (idsIncludedInGroup == null) {
                idsIncludedInGroup = evaluableCriterion;
                continue;
            }
            idsIncludedInGroup.thenCombineAsync(evaluableCriterion, Set::retainAll);
        }

        return idsIncludedInGroup;
    }

    private CompletableFuture<Set<String>> buildCriterion(Criterion criterion) {
        Requestor requestor = null;
        try {
            requestor = new Requestor(new URI("http://mock.url/replace/me"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Requestor finalRequestor = requestor;
        return CompletableFuture.supplyAsync(() -> finalRequestor.execute(criterion)
                .map(FlareResource::getPatientId)
                .collect(Collectors.toSet()));
    }
}
