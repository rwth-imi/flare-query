import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.requestor.Requestor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class FlareExecutor implements de.rwth.imi.flare.api.Executor {
    @Override
    public CompletableFuture<Integer> calculatePatientCount(Query mappedQuery) {
        CompletableFuture<Set<String>> includedIds = getIncludedIds(mappedQuery);
        CompletableFuture<Set<String>> excludedIds = getExcludedIds(mappedQuery);
        CompletableFuture<Set<String>> resultingIds = includedIds.thenCombineAsync(excludedIds, (strings, strings2) ->
        {
            strings.removeAll(strings2);
            return strings;
        });

        return resultingIds.thenApply(Set::size);
    }

    private CompletableFuture<Set<String>> getExcludedIds(Query query) {
        List<CompletableFuture<Set<String>>> excludedIdsByGroup =
                Arrays.stream(query.getInclusionCriteria()).map(this::getIdsFittingExclusionGroup).toList();
        CompletableFuture<Void> groupExecutionFinished = CompletableFuture
                .allOf(excludedIdsByGroup.toArray(new CompletableFuture[0]));

        return groupExecutionFinished.thenApply(unused -> {
            Iterator<CompletableFuture<Set<String>>> excludedGroupsIterator = excludedIdsByGroup.iterator();
            try {
                Set<String> evaluableCriterion;
                evaluableCriterion = excludedGroupsIterator.next().get();
                while (excludedGroupsIterator.hasNext()) {
                    evaluableCriterion.retainAll(excludedGroupsIterator.next().get());
                }
                return evaluableCriterion;
            } catch (InterruptedException | ExecutionException e) {
                throw new CompletionException(e);
            }
        });
    }

    private CompletableFuture<Set<String>> getIdsFittingExclusionGroup(CriteriaGroup group) {
        final List<CompletableFuture<Set<String>>> idsPerCriterion = Arrays.stream(group.getCriteria())
                .map(this::getPatientIdsFittingCriterion).collect(Collectors.toList());

        // Wait for all queries to finish execution
        CompletableFuture<Void> allPatientIdsReceived = CompletableFuture.allOf(idsPerCriterion.toArray(new CompletableFuture[0]));

        //Return union of found ids
        return allPatientIdsReceived.thenApply(unused -> {
            Iterator<CompletableFuture<Set<String>>> iterator = idsPerCriterion.iterator();
            try {
                Set<String> ret = iterator.next().get();
                while (iterator.hasNext()) {
                    ret.addAll(iterator.next().get());
                }
                return ret;
            } catch (ExecutionException | InterruptedException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Build union of all group sets
     */
    private CompletableFuture<Set<String>> getIncludedIds(Query query) {
        // Execute all group queries and wait for execution to finish
        List<CompletableFuture<Set<String>>> includedIdsByGroup =
                Arrays.stream(query.getInclusionCriteria()).map(this::getIdsFittingInclusionGroup).toList();
        CompletableFuture<Void> allPatientIdsReceived = CompletableFuture
                .allOf(includedIdsByGroup.toArray(new CompletableFuture[0]));

        // Build union of all groups
        return allPatientIdsReceived.thenApply(unused -> {
            Iterator<CompletableFuture<Set<String>>> groupIdsIterator = includedIdsByGroup.iterator();
            Set<String> ret = new HashSet<>();
            while (groupIdsIterator.hasNext()) {
                try {
                    ret.addAll(groupIdsIterator.next().get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new CompletionException(e);
                }
            }
            return ret;
        });
    }

    /**
     * Intersect all criteria sets for a given group
     */
    private CompletableFuture<Set<String>> getIdsFittingInclusionGroup(CriteriaGroup group) {
        final List<CompletableFuture<Set<String>>> idsPerCriterion = new ArrayList<>();
        for (Criterion criterion : group.getCriteria()) {
            CompletableFuture<Set<String>> evaluableCriterion = getPatientIdsFittingCriterion(criterion);
            idsPerCriterion.add(evaluableCriterion);
        }
        // Wait for all queries to finish execution
        CompletableFuture<Void> allPatientIdsReceived = CompletableFuture.allOf(idsPerCriterion.toArray(new CompletableFuture[0]));

        // Return intersection of found ids
        return allPatientIdsReceived.thenApply(unused -> {
            Iterator<CompletableFuture<Set<String>>> iterator = idsPerCriterion.iterator();
            try {
                Set<String> ret = iterator.next().get();
                while (iterator.hasNext()) {
                    ret.retainAll(iterator.next().get());
                }
                return ret;
            } catch (ExecutionException | InterruptedException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Get all ids fulfilling a given criterion
     */
    public CompletableFuture<Set<String>> getPatientIdsFittingCriterion(Criterion criterion) {
        Requestor requestor;
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
