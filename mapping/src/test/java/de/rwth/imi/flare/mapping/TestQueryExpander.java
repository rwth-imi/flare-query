package de.rwth.imi.flare.mapping;

import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.TerminologyCode;
import de.rwth.imi.flare.mapping.expansion.ExpansionTreeNode;
import de.rwth.imi.flare.mapping.expansion.QueryExpander;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class TestQueryExpander {
    QueryExpander queryExpander;
    List<CriteriaGroup> twoExclusionCriteria;
    CriteriaGroup twoCriteriaGroup;
    List<Criterion> twoCriteriaList;
    Criterion criterionA;
    Criterion criterionB;
    List<TerminologyCode> terminologyCodesA;
    List<TerminologyCode> terminologyCodesB;


    @BeforeEach
    void setUp(@Mock ExpansionTreeNode expansionTree) throws IOException {
        // (A ^ B) v C
        queryExpander = new QueryExpander(expansionTree);
        twoExclusionCriteria = new ArrayList<>();
        twoCriteriaGroup = new CriteriaGroup();
        twoCriteriaList = new ArrayList<>();
        criterionA = new Criterion();
        criterionB = new Criterion();
        terminologyCodesA = new ArrayList<>();
        terminologyCodesB = new ArrayList<>();


    }
    @Test
    void testExpandCriteriaGroupsExcl(@Mock ExpansionTreeNode expansionTree){
        terminologyCodesA.add(new TerminologyCode("A", "A", "A"));
        criterionA.setTermCodes(terminologyCodesA);
        twoCriteriaList.add(criterionA);
        terminologyCodesB.add(new TerminologyCode("B", "B", "B"));
        criterionB.setTermCodes(terminologyCodesA);
        twoCriteriaList.add(criterionB);
        twoCriteriaGroup.setCriteria(twoCriteriaList);
        twoExclusionCriteria.add(twoCriteriaGroup);

        ExpansionTreeNode nodeA = new ExpansionTreeNode();
        nodeA.setTermCode(new TerminologyCode("A", "A", "A"));
        ExpansionTreeNode nodeA1 = new ExpansionTreeNode();
        nodeA1.setTermCode(new TerminologyCode("A1", "A1", "A1"));
        ExpansionTreeNode nodeA2 = new ExpansionTreeNode();
        nodeA2.setTermCode(new TerminologyCode("A2", "A2", "A2"));
        nodeA.setChildren(Arrays.asList(nodeA1, nodeA2));

        ExpansionTreeNode nodeB = new ExpansionTreeNode();
        nodeB.setTermCode(new TerminologyCode("B", "B", "B"));

        Mockito.lenient().when(expansionTree.findTermCode(new TerminologyCode("A", "A", "A"))).thenReturn(nodeA);
        Mockito.lenient().when(expansionTree.findTermCode(new TerminologyCode("B", "B", "B"))).thenReturn(nodeB);


        List<List<CriteriaGroup>> expandedCriteriaGroups = queryExpander.expandCriteriaGroupsExcl(twoExclusionCriteria);
        System.out.println(expandedCriteriaGroups);
    }
}
