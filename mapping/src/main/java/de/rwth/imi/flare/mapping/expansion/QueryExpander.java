package de.rwth.imi.flare.mapping.expansion;


import de.rwth.imi.flare.api.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class QueryExpander {
    private ExpansionTreeNode expansionTree;


    public QueryExpander(ExpansionTreeNode expansionTree) throws IOException {
        this.expansionTree = expansionTree;
    }

    private List<Criterion> expandCriterion(Criterion criterion){

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


    public QueryExpanded expandQuery(Query query){
        QueryExpanded queryExpanded = new QueryExpanded();
        List<CriteriaGroup> exclusionCriteria = query.getExclusionCriteria();
        List<CriteriaGroup> inclusionCriteria = query.getInclusionCriteria();
        exclusionCriteria = exclusionCriteria == null ? new ArrayList<>() : exclusionCriteria;
        inclusionCriteria = inclusionCriteria == null ? new ArrayList<>() : inclusionCriteria;
        queryExpanded.setExclusionCriteria(expandCriteriaGroupsExcl(exclusionCriteria));
        queryExpanded.setInclusionCriteria(expandCriteriaGroups(inclusionCriteria));
        return queryExpanded;
    }

    public List<CriteriaGroup> expandCriteriaGroups(List<CriteriaGroup> criteriaGroups){
        List<CriteriaGroup> expandedCriteriaGroups = new ArrayList<>(criteriaGroups.size());
        for(CriteriaGroup subgroup: criteriaGroups){
            CriteriaGroup expandedCriteria = expandCriteriaGroup(subgroup);
            expandedCriteriaGroups.add(expandedCriteria);
        }
        return expandedCriteriaGroups;
    }

    private CriteriaGroup expandCriteriaGroup(CriteriaGroup originalCriteriaGroup) {
        LinkedList<Criterion> expandedCriteria = new LinkedList<>();
        CriteriaGroup expandedCriteriaGroup = new CriteriaGroup(expandedCriteria);
        for(Criterion criterion : originalCriteriaGroup.getCriteria()){
            List<Criterion> expandedCriterion = expandCriterion(criterion);
            expandedCriteria.addAll(expandedCriterion);
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
            CriteriaGroup expandedCriterion = new CriteriaGroup(expandCriterion(criterion));
            tmpList.add(expandedCriterion);
        }

        return tmpList;

    }
}
