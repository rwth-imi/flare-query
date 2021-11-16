package de.rwth.imi.flare.mapping.expansion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.Query;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class QueryExpander {
    private ExpansionTreeNode expansionTree;

    public QueryExpander(InputStream expansionTreeStream) throws IOException {
        loadTree(expansionTreeStream);
    }

    public QueryExpander() throws IOException {
        InputStream expansionTreeStream = this.getClass().getClassLoader().getResourceAsStream("codex-code-tree.json");
        loadTree(expansionTreeStream);
    }

    private void loadTree(InputStream expansionTreeStream) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        expansionTree = objectMapper.readValue(expansionTreeStream, new TypeReference<>() {});
    }

    private List<Criterion> expandCriterion(Criterion criterion){
        // Find the given criterion in the tree
        ExpansionTreeNode searchRoot = expansionTree.findTermCode(criterion.getTermCode());
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
        List<Criterion> expandedCriteria = new ArrayList<>();
        Criterion.CriterionBuilder criterionBuilder = criterion.toBuilder();
        for(ExpansionTreeNode expandedLeaf : expansionTreeLeaves){
            Criterion leafCriterion = criterionBuilder.termCode(expandedLeaf.getTermCode()).build();
            expandedCriteria.add(leafCriterion);
        }

        return expandedCriteria;
    }


    public void expandQuery(Query query){
        List<CriteriaGroup> expandedExclusionCriteria = query.getExclusionCriteria();
        List<CriteriaGroup> expandedInclusionCriteria = query.getInclusionCriteria();
        expandedExclusionCriteria = expandedExclusionCriteria == null ? new ArrayList<>() : expandedExclusionCriteria;
        expandedInclusionCriteria = expandedInclusionCriteria == null ? new ArrayList<>() : expandedInclusionCriteria;
        query.setExclusionCriteria(expandCriteriaGroups(expandedExclusionCriteria));
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
}
