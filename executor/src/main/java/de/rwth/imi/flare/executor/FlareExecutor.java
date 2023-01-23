package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.FlareIdDateWrap;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.QueryExpanded;
import de.rwth.imi.flare.requestor.FhirRequestor;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Asynchronously executes a complete Query by querying the single criteria
 * and then executing a recombination of the different result sets according to the cnf
 */
public class FlareExecutor implements de.rwth.imi.flare.api.Executor {
    private final FhirRequestor requestor;
    private final FhirIdRequestor fhirIdRequestor;
    private List<CompletableFuture<Set<FlareIdDateWrap>>> consentResourceIds = new LinkedList<>();

    public FlareExecutor(FhirRequestor requestor) {
        this.requestor = requestor;
        this.fhirIdRequestor = new FhirIdRequestor(requestor);

    }

    @Override
    public CompletableFuture<Integer> calculatePatientCount(QueryExpanded mappedQuery) {
        CompletableFuture<Set<FlareIdDateWrap>> includedIdDateWraps = getIncludedIds(mappedQuery.getInclusionCriteria());
        CompletableFuture<Set<FlareIdDateWrap>> excludedIdDateWraps = getExcludedIds(mappedQuery.getExclusionCriteria());


        CompletableFuture<Set<String>> resultingIds = includedIdDateWraps.thenCombine(excludedIdDateWraps, (idDateWrapSet0, idDateWrapSet1) ->
        {
            Set<String> includedIds = idDateWrapSet0.stream().map(flareIdDateWrap -> flareIdDateWrap.patId).collect(Collectors.toSet());
            Set<String> excludedIds = idDateWrapSet1.stream().map(flareIdDateWrap -> flareIdDateWrap.patId).collect(Collectors.toSet());
            if (includedIds != null) {
                includedIds.removeAll(excludedIds);
            }
            return includedIds;
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
    public List<List<List<String>>> translateMappedQuery(QueryExpanded mappedQuery) {
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
    private List<List<String>> iterateCriterion(FhirRequestor translator, List<CriteriaGroup> criterion) {
        //return array
        List<List<String>> termCodeList = new ArrayList<>();
        //loop TermCodes
        for (CriteriaGroup criteriaGrp : criterion) {
            //temporal list of ValueFilters
            List<String> valueFilterList = criteriaGrp.getCriteria().stream().map(translator::translateCriterion).collect(Collectors.toList());
            //add ValueFilter List to termCodeList
            //creates new ArrayList to copy value
            termCodeList.add(new ArrayList<>(valueFilterList));
        }
        return termCodeList;
    }

    /**
     * Build intersection of all group sets
     */
    private CompletableFuture<Set<FlareIdDateWrap>> getIncludedIds(List<CriteriaGroup> inclusionCriteria) {
        this.consentResourceIds = new LinkedList<>(); //needs to be reset because this method is used for exlucion after it has been used for inclusion
        if (inclusionCriteria == null) {
            return CompletableFuture.completedFuture(new HashSet<>());
        }

        List<CompletableFuture<Set<FlareIdDateWrap>>> includedIdsByGroup = new LinkedList<>();
        Iterator<CriteriaGroup> includedResourcedIterator = inclusionCriteria.iterator();
        while(includedResourcedIterator.hasNext()){
            addIdsAndConsentFittingInclusionGroup(includedResourcedIterator.next(), includedIdsByGroup);
        }

        // Wait for async exec to finish
        CompletableFuture<Void> groupExecutionFinished = CompletableFuture
                .allOf(includedIdsByGroup.toArray(new CompletableFuture[0]));
        CompletableFuture<Void> consentRequestExecution = CompletableFuture
                .allOf(this.consentResourceIds.toArray(new CompletableFuture[0]));



        Set<FlareIdDateWrap> allConsentIds;
        try{
            allConsentIds = this.getUnionOfIds(this.consentResourceIds).get();
        }catch (ExecutionException | InterruptedException e) {
            throw new CompletionException(e);
        }

        if(includedIdsByGroup.size() == 0){
            return consentRequestExecution.thenApply(unused -> allConsentIds);
        }
        return groupExecutionFinished.thenApply(unused -> {
            Set<FlareIdDateWrap> intersectionOfIds = this.getSimpleIntersectionOfCriterionGroups(includedIdsByGroup);
            if(intersectionOfIds == null){
                return new HashSet<FlareIdDateWrap>();
            }
            if(allConsentIds.size() > 0){
                return this.getCriterionIdsFittingToConsent(allConsentIds, intersectionOfIds);
            }
            return intersectionOfIds;
        });
    }

    Set<FlareIdDateWrap> getSimpleIntersectionOfCriterionGroups(List<CompletableFuture<Set<FlareIdDateWrap>>> includedIdsByGroup){
        Iterator<CompletableFuture<Set<FlareIdDateWrap>>> includedGroupsIterator = includedIdsByGroup.iterator();
        try {
            Set<FlareIdDateWrap> evaluableCriterion = null;
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
    }
    Set<FlareIdDateWrap> getCriterionIdsFittingToConsent(Set<FlareIdDateWrap>consentIds, Set<FlareIdDateWrap>includedIdsByGroup){
        FlareIdDateWrap[] includedConsentIds = consentIds.toArray(new FlareIdDateWrap[consentIds.size()]);
        FlareIdDateWrap[] includedResourceIds = includedIdsByGroup.toArray(new FlareIdDateWrap[includedIdsByGroup.size()]);

        Set<FlareIdDateWrap> idsInConsentRange = new HashSet<FlareIdDateWrap>();
        for(int i = 0; i < includedResourceIds.length; i++){
            FlareIdDateWrap currentResource = includedResourceIds[i];
            for(int j = 0; j < includedConsentIds.length; j++){
                FlareIdDateWrap currentConsentId = includedConsentIds[j];
                if(currentConsentId.patId.equals(currentResource.patId)){
                    if(criterionIsInConsentRange(currentConsentId, currentResource)){
                        idsInConsentRange.add(currentResource);
                    }
                    break;//assumption: a resource can only belong to one consent
                }
            }
        }
    return idsInConsentRange;
    }

    private boolean criterionIsInConsentRange(FlareIdDateWrap consentWrap, FlareIdDateWrap criterionWrap){
        if(criterionWrap.startDate == null || criterionWrap.endDate == null){
            return false;
        }
        boolean startDateInRange = criterionWrap.startDate.after(consentWrap.startDate) && criterionWrap.startDate.before(consentWrap.endDate);
        boolean endDateInRange = criterionWrap.endDate.before(consentWrap.endDate) && criterionWrap.endDate.after(consentWrap.startDate);
        return  startDateInRange || endDateInRange;
    }

    private List<Criterion>  getAndRemoveConsent(List<Criterion> criteriaOfGroup){
        List<Criterion> consentCriteria = new LinkedList<Criterion>();
        int i = 0;
        while(i < criteriaOfGroup.size()){
            Criterion criterion = criteriaOfGroup.get(i);
            if(criterion.getMapping().getFhirResourceType().toString().equals("Consent") ){
                criteriaOfGroup.remove(i);
                consentCriteria.add(criterion);
            }else{
                i++;
            }
        }
        return consentCriteria;
    }

    /**
     * Union all criteria sets for a given group
     */
    private CompletableFuture<Set<FlareIdDateWrap>> getIdsFittingInclusionGroup(CriteriaGroup group) {
        List<Criterion> criterionList = new LinkedList<Criterion>(group.getCriteria());
        List<Criterion> consentCriterionList = getAndRemoveConsent(criterionList);

        CompletableFuture<Set<FlareIdDateWrap>> consentIdsOfThisGroup = getUnionOfIds(consentCriterionList.stream()
                .map(fhirIdRequestor::getPatientIdsFittingCriterion).toList());
        try{
            if(consentIdsOfThisGroup.get().size() > 0){
                this.consentResourceIds.add(consentIdsOfThisGroup);
            }
        }catch(Exception e){
            e.printStackTrace();
        }


        final List<CompletableFuture<Set<FlareIdDateWrap>>> idsPerCriterion = criterionList.stream()
                .map(fhirIdRequestor::getPatientIdsFittingCriterion).toList();

        // Wait for all queries to finish execution
        return getUnionOfIds(idsPerCriterion);
    }

    private void addIdsAndConsentFittingInclusionGroup(CriteriaGroup group, List<CompletableFuture<Set<FlareIdDateWrap>>> includedIdsByGroup) {
        List<Criterion> otherResourceCriterionList = new LinkedList<Criterion>(group.getCriteria());
        List<Criterion> consentCriterionList = getAndRemoveConsent(otherResourceCriterionList);

        CompletableFuture<Set<FlareIdDateWrap>> consentIdsOfThisGroup = getUnionOfIds(consentCriterionList.stream()
                .map(fhirIdRequestor::getPatientIdsFittingCriterion).toList());
        try{
            if(consentIdsOfThisGroup.get().size() > 0){
                this.consentResourceIds.add(consentIdsOfThisGroup);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        if(otherResourceCriterionList.size() > 0){
            final List<CompletableFuture<Set<FlareIdDateWrap>>> idsPerCriterion = otherResourceCriterionList.stream()
                    .map(fhirIdRequestor::getPatientIdsFittingCriterion).toList();
            includedIdsByGroup.add(getUnionOfIds(idsPerCriterion));
        }
    }

    /**
     * Build union of all group sets
     */
    private CompletableFuture<Set<FlareIdDateWrap>> getExcludedIds(List<List<CriteriaGroup>> exclusionCriteria) {
        if (exclusionCriteria == null) {
            return CompletableFuture.completedFuture(new HashSet<>());
        }
        List<CompletableFuture<Set<FlareIdDateWrap>>> excludedIdsByGroups = new ArrayList<>();
        for (List<CriteriaGroup> group : exclusionCriteria) {
            CompletableFuture<Set<FlareIdDateWrap>> excludedIdsByGroup = getIncludedIds(group);
            excludedIdsByGroups.add(excludedIdsByGroup);
        }
        // Wait for async exec to finish
        return getUnionOfIds(excludedIdsByGroups);
    }

    private CompletableFuture<Set<FlareIdDateWrap>> getUnionOfIds(List<CompletableFuture<Set<FlareIdDateWrap>>> idsByGroups) {
        CompletableFuture<Void> groupExecutionFinished = CompletableFuture
                .allOf(idsByGroups.toArray(new CompletableFuture[0]));

        return groupExecutionFinished.thenApply(unused -> {
            Iterator<CompletableFuture<Set<FlareIdDateWrap>>> includedGroupsIterator = idsByGroups.iterator();
            try {
                Set<FlareIdDateWrap> ret = new HashSet<FlareIdDateWrap>();
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
    //TODO i think this function might be deleted as it is not used and was not used before
    private CompletableFuture<Set<String>> getIdsFittingExclusionGroup(List<CriteriaGroup> groups) {
        final List<CompletableFuture<Set<FlareIdDateWrap>>> idsPerCriterion = new ArrayList<>();
        for (CriteriaGroup group : groups) {
            for (Criterion criterion : group.getCriteria()) {
                CompletableFuture<Set<FlareIdDateWrap>> evaluableCriterion = fhirIdRequestor.getPatientIdsFittingCriterion(criterion);
                idsPerCriterion.add(evaluableCriterion);
            }
        }
        // Wait for all queries to finish execution
        CompletableFuture<Void> allPatientIdsReceived = CompletableFuture.allOf(idsPerCriterion.toArray(new CompletableFuture[0]));

        // Return intersection of found ids
        //TODO this is a place holder so i dont have to adapt the old code that is not used anyway
        return allPatientIdsReceived.thenApply(unused -> {
            HashSet<String> testSet = new HashSet<String>();
            return testSet;});
        /*
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
        });*/
    }
}
