package de.rwth.imi.flare.mapping.expansion;


import com.google.common.collect.Sets;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.api.model.TerminologyCode;

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


    public void expandQuery(Query query){
        List<CriteriaGroup> expandedExclusionCriteria = query.getExclusionCriteria();
        List<CriteriaGroup> expandedInclusionCriteria = query.getInclusionCriteria();
        expandedExclusionCriteria = expandedExclusionCriteria == null ? new ArrayList<>() : expandedExclusionCriteria;
        expandedInclusionCriteria = expandedInclusionCriteria == null ? new ArrayList<>() : expandedInclusionCriteria;
        query.setExclusionCriteria(expandCriteriaGroupsExcl(expandedExclusionCriteria));
        query.setInclusionCriteria(expandCriteriaGroups(expandedInclusionCriteria));
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

    public List<CriteriaGroup> expandCriteriaGroupsExcl(List<CriteriaGroup> criteriaGroups){
        List<CriteriaGroup> expandedCriteriaGroups = new ArrayList<>(criteriaGroups.size());

        LinkedList<Set<Criterion>> tmpList  = new LinkedList<>();
        for(CriteriaGroup subgroup: criteriaGroups){
            tmpList.addAll(expandCriteriaGroupExcl(subgroup));
        }

        Set<List<Criterion>> productList = Sets.cartesianProduct(tmpList);

        for (List<Criterion> criterionList : productList) {

            if(criterionList.size() > 0) {
                expandedCriteriaGroups.add(new CriteriaGroup(criterionList));
            }
        }

        return expandedCriteriaGroups;
    }

    private LinkedList<Set<Criterion>> expandCriteriaGroupExcl(CriteriaGroup originalCriteriaGroup) {
        LinkedList<Set<Criterion>> tmpList = new LinkedList<>();
        for(Criterion criterion : originalCriteriaGroup.getCriteria()) {
            Set<Criterion> expandedCriterion = new HashSet<>(expandCriterion(criterion));
            tmpList.add(expandedCriterion);
        }

        return tmpList;

    }
}
