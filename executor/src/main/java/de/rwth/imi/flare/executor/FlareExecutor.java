package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.Executor;
import de.rwth.imi.flare.api.Requestor;
import de.rwth.imi.flare.api.UnsupportedCriterionException;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.ExpandedQuery;

import java.util.*;
import java.util.concurrent.CompletableFuture;


/**
 * Asynchronously executes a complete Query by querying the single criteria
 * and then executing a recombination of the different result sets according to the cnf
 */
public class FlareExecutor implements Executor {

    private final Requestor requestor;

    public FlareExecutor(Requestor requestor) {
        this.requestor = Objects.requireNonNull(requestor);
    }

    @Override
    public CompletableFuture<Integer> calculatePatientCount(ExpandedQuery mappedQuery) {
        return getIncludedIds(mappedQuery.getInclusionCriteria())
                .thenCombine(getExcludedIds(mappedQuery.getExclusionCriteria()),
                        (includedIds, excludedIds) -> {
                            Set<String> ret = new HashSet<>(includedIds);
                            ret.removeAll(excludedIds);
                            return ret.size();
                        });
    }

    /**
     * Separates a mappedQuery into inclusion and exclusion criterions. Recombines them after parsing into StructuredQuery format.
     *
     * @param mappedQuery
     * @return StructuredQuery
     */
    @Override
    public List<List<List<String>>> translateMappedQuery(ExpandedQuery mappedQuery) throws UnsupportedCriterionException {
        //create new FhirRequestor by provided config

        //split criterions into inculsion and exclusion
        List<CriteriaGroup> inclusionCriteria = mappedQuery.getInclusionCriteria();
        List<List<CriteriaGroup>> exclusionCriteria = mappedQuery.getExclusionCriteria();
        //translate criterions
        List<List<String>> translatedInclusionCriteria = iterateCriterion(requestor, inclusionCriteria);
        List<List<List<String>>> translatedExclusionCriteria = new ArrayList<>();
        for (List<CriteriaGroup> subCriteria : exclusionCriteria) {
            translatedExclusionCriteria.add(iterateCriterion(requestor, subCriteria));
        }
        //create new ArrayList and recombine inclusion and exclusion criterions into StructuredQuery format
        List<List<List<String>>> combinedCriteria = new ArrayList<>();
        combinedCriteria.add(translatedInclusionCriteria);
        combinedCriteria.addAll(translatedExclusionCriteria);

        return combinedCriteria;
    }

    /**
     * Iterates over a criteriaGroups (inclusion or exclusion) and returns it as a translated component of the StructuredQuery format.
     *
     * @param requestor      FhirRequestor specified earlier
     * @param criteriaGroups multiple criterions consisting of termcodes and valuefilters
     * @return String list of FHIR Search Strings containing termcodes and nested valuefilters
     */
    private List<List<String>> iterateCriterion(Requestor requestor, List<CriteriaGroup> criteriaGroups) throws UnsupportedCriterionException {
        List<List<String>> groupQueries = new ArrayList<>();
        for (CriteriaGroup group : criteriaGroups) {
            List<String> queries = new ArrayList<>();
            for (Criterion criterion : group.getCriteria()) {
                queries.add(requestor.translateCriterion(criterion));
            }
            groupQueries.add(queries);
        }
        return groupQueries;
    }

    /**
     * Build intersection of all group sets
     */
    private CompletableFuture<Set<String>> getIncludedIds(List<CriteriaGroup> inclusionCriteria) {
        if (inclusionCriteria == null) {
            return CompletableFuture.completedFuture(Set.of());
        }

        // Async fetch all ids per group
        List<CompletableFuture<Set<String>>> includedIdsByGroup = new ArrayList<>();
        for (CriteriaGroup inclusionCriterion : inclusionCriteria) {
            CompletableFuture<Set<String>> idsFittingInclusionGroup = getIdsFittingInclusionGroup(inclusionCriterion);
            includedIdsByGroup.add(idsFittingInclusionGroup);
        }

        // Wait for async exec to finish
        CompletableFuture<Void> groupExecutionFinished = CompletableFuture
                .allOf(includedIdsByGroup.toArray(new CompletableFuture[0]));

        return groupExecutionFinished.thenApply(unused -> {
            Iterator<CompletableFuture<Set<String>>> includedGroupsIterator = includedIdsByGroup.iterator();

            if (includedGroupsIterator.hasNext()) {
                Set<String> evaluableCriterion = includedGroupsIterator.next().join();

                while (includedGroupsIterator.hasNext()) {
                    evaluableCriterion.retainAll(includedGroupsIterator.next().join());
                }
                return evaluableCriterion;
            } else {
                return Set.of();
            }
        });
    }

    /**
     * Union all criteria sets for a given group
     */
    private CompletableFuture<Set<String>> getIdsFittingInclusionGroup(CriteriaGroup group) {
        List<CompletableFuture<Set<String>>> idsPerCriterion = group.getCriteria().stream().map(requestor::execute).toList();

        // Wait for all queries to finish execution
        return getUnionOfIds(idsPerCriterion);
    }

    /**
     * Build union of all group sets
     */
    private CompletableFuture<Set<String>> getExcludedIds(List<List<CriteriaGroup>> exclusionCriteria) {
        if (exclusionCriteria == null) {
            return CompletableFuture.completedFuture(Set.of());
        }
        List<CompletableFuture<Set<String>>> excludedIdsByGroups = new ArrayList<>();
        for (List<CriteriaGroup> group : exclusionCriteria) {
            CompletableFuture<Set<String>> excludedIdsByGroup = getIncludedIds(group);
            excludedIdsByGroups.add(excludedIdsByGroup);
        }
        // Wait for async exec to finish
        return getUnionOfIds(excludedIdsByGroups);
    }

    private CompletableFuture<Set<String>> getUnionOfIds(List<CompletableFuture<Set<String>>> idsByGroups) {
        CompletableFuture<Void> groupExecutionFinished = CompletableFuture
                .allOf(idsByGroups.toArray(new CompletableFuture[0]));

        return groupExecutionFinished.thenApply(unused -> {
            Set<String> ret = new HashSet<>();
            for (CompletableFuture<Set<String>> ids : idsByGroups) {
                ret.addAll(ids.join());
            }
            return ret;
        });
    }
}
