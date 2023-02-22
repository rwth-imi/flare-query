package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.UnsupportedCriterionException;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.QueryExpanded;
import de.rwth.imi.flare.requestor.FhirRequestor;

import java.util.*;
import java.util.concurrent.*;


/**
 * Asynchronously executes a complete Query by querying the single criteria
 * and then executing a recombination of the different result sets according to the cnf
 */
public class FlareExecutor implements de.rwth.imi.flare.api.Executor {
    private final FhirRequestor requestor;
    private final FhirIdRequestor fhirIdRequestor;

    public FlareExecutor(FhirRequestor requestor) {
        this.requestor = requestor;
        this.fhirIdRequestor = new FhirIdRequestor(requestor);

    }

    @Override
    public CompletableFuture<Integer> calculatePatientCount(QueryExpanded mappedQuery)
            throws UnsupportedCriterionException {
        CompletableFuture<Set<String>> includedIds = getIncludedIds(mappedQuery.getInclusionCriteria());
        CompletableFuture<Set<String>> excludedIds = getExcludedIds(mappedQuery.getExclusionCriteria());
        CompletableFuture<Set<String>> resultingIds = includedIds.thenCombine(excludedIds, (strings, strings2) ->
        {
            if (strings2 != null) {
                strings.removeAll(strings2);
            }
            return strings;
        });

        return resultingIds.thenApply(Set::size);
    }

    /**
     * Separetes a mappedQuery into inclusion and exclusion criterions. Recombines them after parsing into StructuredQuery format.
     *
     * @param mappedQuery
     * @return StructuredQuery
     */
    @Override
    public List<List<List<String>>> translateMappedQuery(QueryExpanded mappedQuery)
            throws UnsupportedCriterionException {
        //create new FhirRequestor by provided config

        //split criterions into inculsion and exclusion
        List<CriteriaGroup> inclusionCriteria = mappedQuery.getInclusionCriteria();
        List<List<CriteriaGroup>> exclusionCriteria = mappedQuery.getExclusionCriteria();
        //translate criterions
        List<List<String>> translatedInclusionCriteria = iterateCriterion(
            requestor, inclusionCriteria);
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
     * Iterates over a criterion (inclusion or exclusion) and returns it as a translated component of the StructuredQuery format.
     *
     * @param translator FhirRequestor specified earlier
     * @param criterion  multiple criterions consisting of termcodes and valuefilters
     * @return String list of FHIR Search Strings containing termcodes and nested valuefilters
     */
    private List<List<String>> iterateCriterion(FhirRequestor translator, List<CriteriaGroup> criterion) throws UnsupportedCriterionException {
        //return array
        List<List<String>> termCodeList = new ArrayList<>();
        //loop TermCodes
        for (CriteriaGroup criteriaGrp : criterion) {
            //temporal list of ValueFilters
            List<String> valueFilterList = new ArrayList<>();
            for (Criterion criterion1 : criteriaGrp.getCriteria()) {
                valueFilterList.add(translator.translateCriterion(criterion1));
            }
            //add ValueFilter List to termCodeList
            //creates new ArrayList to copy value
            termCodeList.add(valueFilterList);
        }
        return termCodeList;
    }

    /**
     * Build intersection of all group sets
     */
    private CompletableFuture<Set<String>> getIncludedIds(List<CriteriaGroup> inclusionCriteria)
            throws UnsupportedCriterionException {
        if (inclusionCriteria == null) {
            return CompletableFuture.completedFuture(new HashSet<>());
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
            try {
                Set<String> evaluableCriterion = null;
                if (includedGroupsIterator.hasNext()) {
                    evaluableCriterion = includedGroupsIterator.next().get();
                }
                while (includedGroupsIterator.hasNext()) {
                    evaluableCriterion.retainAll(includedGroupsIterator.next().get());
                }
                return evaluableCriterion;
            } catch (InterruptedException | ExecutionException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Union all criteria sets for a given group
     */
    private CompletableFuture<Set<String>> getIdsFittingInclusionGroup(CriteriaGroup group)
            throws UnsupportedCriterionException {
        final List<CompletableFuture<Set<String>>> idsPerCriterion = new ArrayList<>();
        for (Criterion criterion : group.getCriteria()) {
            CompletableFuture<Set<String>> patientIdsFittingCriterion = fhirIdRequestor.getPatientIdsFittingCriterion(criterion);
            idsPerCriterion.add(patientIdsFittingCriterion);
        }

        // Wait for all queries to finish execution
        return getUnionOfIds(idsPerCriterion);
    }

    /**
     * Build union of all group sets
     */
    private CompletableFuture<Set<String>> getExcludedIds(List<List<CriteriaGroup>> exclusionCriteria)
            throws UnsupportedCriterionException {
        if (exclusionCriteria == null) {
            return CompletableFuture.completedFuture(new HashSet<>());
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
            Iterator<CompletableFuture<Set<String>>> includedGroupsIterator = idsByGroups.iterator();
            try {
                Set<String> ret = new HashSet<String>();
                while (includedGroupsIterator.hasNext()) {
                    ret.addAll(includedGroupsIterator.next().get());
                }
                return ret;
            } catch (ExecutionException | InterruptedException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Intersect all criteria sets for a given group
     */
    private CompletableFuture<Set<String>> getIdsFittingExclusionGroup(List<CriteriaGroup> groups)
            throws UnsupportedCriterionException {
        final List<CompletableFuture<Set<String>>> idsPerCriterion = new ArrayList<>();
        for (CriteriaGroup group : groups) {
            for (Criterion criterion : group.getCriteria()) {
                CompletableFuture<Set<String>> evaluableCriterion = fhirIdRequestor.getPatientIdsFittingCriterion(criterion);
                idsPerCriterion.add(evaluableCriterion);
            }
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
}
