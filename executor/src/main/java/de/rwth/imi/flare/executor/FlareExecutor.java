package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.requestor.FhirRequestor;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


/**
 * Asynchronously executes a complete Query by querying the single criteria
 * and then executing a recombination of the different result sets according to the cnf
 */
public class FlareExecutor implements de.rwth.imi.flare.api.Executor {
    private FhirRequestorConfig config;
    private Executor futureExecutor;

    public void setConfig(FhirRequestorConfig config){
        this.config = config;
    }

    public void setFutureExecutor(Executor futureExecutor){
        this.futureExecutor = futureExecutor;
    }

    public FlareExecutor(FhirRequestorConfig config){
        this.config = config;
        this.futureExecutor = new ThreadPoolExecutor(4, 16, 10,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @Override
    public CompletableFuture<Integer> calculatePatientCount(Query mappedQuery) {
        CompletableFuture<Set<String>> includedIds = getIncludedIds(mappedQuery);
        CompletableFuture<Set<String>> excludedIds = getExcludedIds(mappedQuery);
        CompletableFuture<Set<String>> resultingIds = includedIds.thenCombineAsync(excludedIds, (strings, strings2) ->
        {
            if(strings2 != null){
                strings.removeAll(strings2);
            }
            return strings;
        }, this.futureExecutor);

        return resultingIds.thenApply(Set::size);
    }

    /**
     * Separetes a mappedQuery into inclusion and exclusion criterions. Recombines them after parsing into StructuredQuery format.
     * @param mappedQuery
     * @return StructuredQuery
     */
    @Override
    public List<List<List<String>>> translateMappedQuery(Query mappedQuery) {
        //create new FhirRequestor by provided config
        FhirRequestor translator = new FhirRequestor(config);
        //split criterions into inculsion and exclusion
        Criterion[][] inclusionCriteria = mappedQuery.getInclusionCriteria();
        Criterion[][] exclusionCriteria = mappedQuery.getExclusionCriteria();
        //translate criterions
        List<List<String>> translatedInclusionCriteria = iterateCriterion(translator, inclusionCriteria);
        List<List<String>> translatedExclusionCriteria = iterateCriterion(translator, exclusionCriteria);
        //create new ArrayList and recombine inclusion and exclusion criterions into StructuredQuery format
        List<List<List<String>>> combinedCriteria = new ArrayList<>();
        combinedCriteria.add(translatedInclusionCriteria);
        combinedCriteria.add(translatedExclusionCriteria);

        return combinedCriteria;
    }

    /**
     * Iterates over a criterion (inclusion or exclusion) and returns it as a translated component of the StructuredQuery format.
     * @param translator FhirRequestor specified earlier
     * @param criterion multiple criterions consisting of termcodes and valuefilters
     * @return String list of FHIR Search Strings containing termcodes and nested valuefilters
     */
    private List<List<String>> iterateCriterion(FhirRequestor translator, Criterion[][] criterion){
        //length of termCodes and valueFilter arrays in criterion for iteration
        int numTermCodes = criterion.length;
        int numValueFilter;
        //return array
        List<List<String>> termCodeList = new ArrayList<>();
        //loop TermCodes
        for (int i=0; i<numTermCodes; i++){
            //get number of ValueFilters for current criterion
            numValueFilter = criterion[i].length;
            //temporal list of ValueFilters
            List<String> valueFilterList = new ArrayList<>();
            //loop ValueFilters
            for (int j=0; j < numValueFilter; j++){
                //translate current ValueFilter and add to list
                valueFilterList.add(translator.translateCriterion(criterion[i][j]));
            }
            //add ValueFilter List to termCodeList
            //creates new ArrayList to copy value
            termCodeList.add(new ArrayList<>(valueFilterList));
        }
        return termCodeList;
    }

    /**
     * Build intersection of all group sets
     */
    private CompletableFuture<Set<String>> getIncludedIds(Query query) {
        if(query.getInclusionCriteria() == null){
            return CompletableFuture.completedFuture(new HashSet<>());
        }
        // Async fetch all ids per group
        List<CompletableFuture<Set<String>>> includedIdsByGroup =
                Arrays.stream(query.getInclusionCriteria()).map(this::getIdsFittingInclusionGroup).toList();

        // Wait for async exec to finish
        CompletableFuture<Void> groupExecutionFinished = CompletableFuture
                .allOf(includedIdsByGroup.toArray(new CompletableFuture[0]));

        return groupExecutionFinished.thenApply(unused -> {
            Iterator<CompletableFuture<Set<String>>> includedGroupsIterator = includedIdsByGroup.iterator();
            try {
                Set<String> evaluableCriterion = null;
                if(includedGroupsIterator.hasNext()){
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
    private CompletableFuture<Set<String>> getIdsFittingInclusionGroup(Criterion[] group) {
        final List<CompletableFuture<Set<String>>> idsPerCriterion = Arrays.stream(group)
                .map(this::getPatientIdsFittingCriterion).toList();

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
    private CompletableFuture<Set<String>> getExcludedIds(Query query) {
        if(query.getExclusionCriteria() == null){
            return CompletableFuture.completedFuture(new HashSet<>());
        }

        // Execute all group queries and wait for execution to finish
        List<CompletableFuture<Set<String>>> excludedIdsByGroup =
                Arrays.stream(query.getExclusionCriteria()).map(this::getIdsFittingExclusionGroup).toList();
        CompletableFuture<Void> allPatientIdsReceived = CompletableFuture
                .allOf(excludedIdsByGroup.toArray(new CompletableFuture[0]));

        // Build union of all groups
        return allPatientIdsReceived.thenApply(unused -> {
            Iterator<CompletableFuture<Set<String>>> groupIdsIterator = excludedIdsByGroup.iterator();
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
    private CompletableFuture<Set<String>> getIdsFittingExclusionGroup(Criterion[] group) {
        final List<CompletableFuture<Set<String>>> idsPerCriterion = new ArrayList<>();
        for (Criterion criterion : group) {
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
        FhirRequestor requestor = new FhirRequestor(config);
        return CompletableFuture.supplyAsync(() -> requestor.execute(criterion)
                .map(FlareResource::getPatientId)
                .collect(Collectors.toSet()), this.futureExecutor);
    }
}
