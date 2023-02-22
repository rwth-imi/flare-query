package de.rwth.imi.flare.mapping.expansion;


import de.rwth.imi.flare.api.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class QueryExpander {
    private ExpansionTreeNode expansionTree;


    public QueryExpander(ExpansionTreeNode expansionTree) throws IOException {
        this.expansionTree = expansionTree;
    }

    private List<Criterion> expandTermCodes(Criterion criterion){

        List<TerminologyCode> termCodes = criterion.getTermCodes();
        List<Criterion> expandedCriteria = new ArrayList<>();

        for(TerminologyCode singleTermCode: termCodes){
            // Find the given criterion in the tree
            ExpansionTreeNode searchRoot = expansionTree.findTermCode(singleTermCode);
            if(searchRoot == null)
            {
                return List.of(criterion);
            }

            // Consumer collecting all leaves of the expansion tree
            List<ExpansionTreeNode> expansionTreeLeaves = new ArrayList<>();
            Consumer<ExpansionTreeNode> leafCollectorConsumer = expansionTreeLeaves::add;

            // Collect all leaves
            searchRoot.bfs(leafCollectorConsumer);

            // Create a criterion for each leaf

            Criterion.CriterionBuilder criterionBuilder = criterion.toBuilder();
            for(ExpansionTreeNode expandedLeaf : expansionTreeLeaves){

                TerminologyCode newTermCode = expandedLeaf.getTermCode();
                Criterion leafCriterion = criterionBuilder.termCodes(Arrays.asList(newTermCode)).build();
                expandedCriteria.add(leafCriterion);
            }
        }
        return expandedCriteria;
    }


    public ExpandedQuery expandQuery(Query query){
        ExpandedQuery expandedQuery = new ExpandedQuery();
        List<CriteriaGroup> exclusionCriteria = query.getExclusionCriteria();
        List<CriteriaGroup> inclusionCriteria = query.getInclusionCriteria();
        exclusionCriteria = exclusionCriteria == null ? new ArrayList<>() : exclusionCriteria;
        inclusionCriteria = inclusionCriteria == null ? new ArrayList<>() : inclusionCriteria;
        expandedQuery.setExclusionCriteria(expandCriteriaGroupsExcl(exclusionCriteria));
        expandedQuery.setInclusionCriteria(expandCriteriaGroups(inclusionCriteria));
        return expandedQuery;
    }

    public List<CriteriaGroup> expandCriteriaGroups(List<CriteriaGroup> criteriaGroups){
        List<CriteriaGroup> expandedCriteriaGroups = new ArrayList<>(criteriaGroups.size());
        for(CriteriaGroup subgroup: criteriaGroups){
            CriteriaGroup expandedCriteria = expandCriteriaGroup(subgroup);
            expandedCriteriaGroups.add(expandedCriteria);
        }
        return expandedCriteriaGroups;
    }


    private List<Criterion> getCritsForAtt(Criterion attFilterInputCrit, int attFilterIndex) {

        List<AttributeFilter> inputAttFilterList = new LinkedList<>();
        inputAttFilterList.addAll(attFilterInputCrit.getAttributeFilters());
        AttributeFilter attFilterInput = attFilterInputCrit.getAttributeFilters()
            .get(attFilterIndex);
        inputAttFilterList.remove(attFilterIndex);

        if (attFilterInput.getSelectedConcepts().isEmpty()) {
            return List.of(attFilterInputCrit);
        } else {
            return attFilterInputCrit.getAttributeFilters().get(attFilterIndex)
                .getSelectedConcepts().stream().map(
                    concept -> {
                        Criterion crit = new Criterion();
                        AttributeFilter attFilter = new AttributeFilter();
                        attFilter.setSelectedConcepts(List.of(concept));
                        attFilter.setType(attFilterInput.getType());
                        attFilter.setAttributeCode(attFilterInput.getAttributeCode());
                        List<AttributeFilter>critAttFilterList = new LinkedList<>();
                        critAttFilterList.addAll(inputAttFilterList);
                        critAttFilterList.add(attFilter);
                        crit.setAttributeFilters(critAttFilterList);
                        crit.setMapping(attFilterInputCrit.getMapping());
                        crit.setTermCodes(attFilterInputCrit.getTermCodes());
                        crit.setTimeRestriction(
                            attFilterInputCrit.getTimeRestriction());
                        return crit;
                    }
                ).toList();

        }
    }

    private Stream<Criterion> expandSelectedConcepts(Criterion criterion){

        List<Criterion> expandedCriterion = new LinkedList<>();
        expandedCriterion.add(criterion);

        if (criterion.getValueFilter() != null &&  ! criterion.getValueFilter().getSelectedConcepts().isEmpty()){
            expandedCriterion = expandedCriterion.stream().flatMap(
                valFilterInputCrit ->  valFilterInputCrit.getValueFilter().getSelectedConcepts().stream().map(
                    valFilterConcept -> {
                        Criterion crit = new Criterion();
                        ValueFilter valueFilter = new ValueFilter();
                        valueFilter.setSelectedConcepts(List.of(valFilterConcept));
                        valueFilter.setType(FilterType.CONCEPT);
                        crit.setValueFilter(valueFilter);
                        crit.setMapping(valFilterInputCrit.getMapping());
                        crit.setTermCodes(valFilterInputCrit.getTermCodes());
                        crit.setTimeRestriction(valFilterInputCrit.getTimeRestriction());
                        crit.setAttributeFilters(valFilterInputCrit.getAttributeFilters());
                        return crit;
                    }
                )
            ).toList();
        }

        if(criterion.getAttributeFilters() == null){
            return expandedCriterion.stream();
        }

        List<Criterion> finalCritList = new LinkedList<>();

        expandedCriterion.stream().forEach(
            valCriterion -> {
                List<Criterion> expandedCrit = new LinkedList<>();
                expandedCrit.add(valCriterion);
                int attFilterIndex = 0;

                while(! expandedCrit.isEmpty()){

                    Criterion firstCrit = expandedCrit.get(0);
                    expandedCrit.remove(0);

                    List<Criterion> tempCrits = getCritsForAtt(firstCrit, attFilterIndex);

                    if(tempCrits.size() > 1){
                        expandedCrit.addAll(tempCrits);

                        if(attFilterIndex < tempCrits.get(0).getAttributeFilters().size() - 1){
                            attFilterIndex++;
                        }
                    } else {
                        finalCritList.addAll(tempCrits);
                    }
                }
            }
        );


        return finalCritList.stream();

    }

    private CriteriaGroup expandCriteriaGroup(CriteriaGroup originalCriteriaGroup) {
        LinkedList<Criterion> expandedCriteria = new LinkedList<>();
        CriteriaGroup expandedCriteriaGroup = new CriteriaGroup(expandedCriteria);
        for(Criterion criterion : originalCriteriaGroup.getCriteria()){
            Stream<Criterion> expandedCriterion = expandTermCodes(criterion).stream();
            expandedCriteria.addAll(expandedCriterion.flatMap(this::expandSelectedConcepts).toList());
        }
        return expandedCriteriaGroup;
    }

    public List<List<CriteriaGroup>> expandCriteriaGroupsExcl(List<CriteriaGroup> criteriaGroups){
        List<List<CriteriaGroup>> expandedCriteriaGroups = new ArrayList<>(criteriaGroups.size());
        for(CriteriaGroup subgroup: criteriaGroups){
            List<CriteriaGroup> excl = expandCriteriaGroupExcl(subgroup);
            expandedCriteriaGroups.add(excl);
        }
        return expandedCriteriaGroups;
    }

    private List<CriteriaGroup> expandCriteriaGroupExcl(CriteriaGroup originalCriteriaGroup) {
        LinkedList<CriteriaGroup> tmpList = new LinkedList<>();
        for(Criterion criterion : originalCriteriaGroup.getCriteria()) {
            Stream<Criterion> expandedCriterions = expandTermCodes(criterion).stream();
            CriteriaGroup expandedCriterion = new CriteriaGroup(expandedCriterions.flatMap(this::expandSelectedConcepts).toList());
            tmpList.add(expandedCriterion);
        }

        return tmpList;

    }
}
