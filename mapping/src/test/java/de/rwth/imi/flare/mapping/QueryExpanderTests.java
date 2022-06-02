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
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class QueryExpanderTests {
    QueryExpander queryExpander;
    TerminologyCode terminologyCodeA;
    TerminologyCode terminologyCodeA1;
    TerminologyCode terminologyCodeA2;
    TerminologyCode terminologyCodeB;
    TerminologyCode terminologyCodeC;
    @Mock
    ExpansionTreeNode expansionTree;


    @BeforeEach
    void setUp() throws IOException {
        queryExpander = new QueryExpander(expansionTree);

        terminologyCodeA = new TerminologyCode("A", "A", "A");
        terminologyCodeA1 = new TerminologyCode("A1", "A1", "A1");
        terminologyCodeA2 = new TerminologyCode("A2", "A2", "A2");
        terminologyCodeB = new TerminologyCode("B", "B", "B");
        terminologyCodeC = new TerminologyCode("C", "C", "C");

        mockFindTermCode();
    }

    @Test
    void exclusionCriteriaExpansionFollowsLogic() {
        // (A ^ B) v C
        List<CriteriaGroup> inputExclusionCriteria = getInputExclusionCriteria();

        // ((A v A1 v A2) ^ B) v C
        List<List<CriteriaGroup>> expectedExpandedCriteriaGroups = getExpectedExpandedCriteriaGroups();

        List<List<CriteriaGroup>> expandedCriteriaGroups = queryExpander.expandCriteriaGroupsExcl(inputExclusionCriteria);
        assertTrue(compareExpandedCriteriaGroups(expandedCriteriaGroups, expectedExpandedCriteriaGroups));
    }

    /**
     * compare if the nested lists have the same Dimension and the same underlying value (TerminologyCode)
     * @param expandedCriteriaGroups the nested lists to be tested
     * @param expectedExpandedCriteriaGroups the expected format and values of the nested lists
     * @return true if the nested lists are equal, else false
     */
    private boolean compareExpandedCriteriaGroups(List<List<CriteriaGroup>> expandedCriteriaGroups, List<List<CriteriaGroup>> expectedExpandedCriteriaGroups) {
        if(expandedCriteriaGroups.size() != expectedExpandedCriteriaGroups.size()){
            return false;
        }
        for(int i = 0; i < expectedExpandedCriteriaGroups.size(); i++) {
            if(expandedCriteriaGroups.get(i).size() != expectedExpandedCriteriaGroups.get(i).size()){
                return false;
            }
            for (int j = 0; j< expectedExpandedCriteriaGroups.get(i).size(); j++){
                if(expandedCriteriaGroups.get(i).get(j).getCriteria().size()!=expectedExpandedCriteriaGroups.get(i).get(j).getCriteria().size()){
                    return false;
                }
                for (int k = 0; k<expectedExpandedCriteriaGroups.get(i).get(j).getCriteria().size(); k++){
                    Criterion testCriterion = expandedCriteriaGroups.get(i).get(j).getCriteria().get(k);
                    Criterion expectedCriterion = expectedExpandedCriteriaGroups.get(i).get(j).getCriteria().get(k);
                    if(testCriterion.getTermCodes().size() != expectedCriterion.getTermCodes().size()){
                        return false;
                    }
                    if(!testCriterion.getTermCodes().get(0).equals(expectedCriterion.getTermCodes().get(0))){
                        return false;
                    }
                }
            }

        }
        return true;
    }

    private List<CriteriaGroup> getInputExclusionCriteria() {
        //TODO exchange with parser using resources
        List<CriteriaGroup> exclusionCriteria = new ArrayList<>();
        CriteriaGroup twoCriteriaGroup = new CriteriaGroup();
        CriteriaGroup oneCriteriaGroup = new CriteriaGroup();
        List<Criterion> twoCriteriaList = new ArrayList<>();
        List<Criterion> oneCriteriaList = new ArrayList<>();
        Criterion criterionA = new Criterion();
        Criterion criterionB = new Criterion();
        Criterion criterionC = new Criterion();
        List<TerminologyCode> terminologyCodesA = new ArrayList<>();
        List<TerminologyCode> terminologyCodesB = new ArrayList<>();
        List<TerminologyCode> terminologyCodesC = new ArrayList<>();


        terminologyCodesA.add(terminologyCodeA);
        terminologyCodesB.add(terminologyCodeB);
        terminologyCodesC.add(terminologyCodeC);

        criterionA.setTermCodes(terminologyCodesA);
        criterionB.setTermCodes(terminologyCodesB);
        criterionC.setTermCodes(terminologyCodesC);

        twoCriteriaList.add(criterionA);
        twoCriteriaList.add(criterionB);
        oneCriteriaList.add(criterionC);

        twoCriteriaGroup.setCriteria(twoCriteriaList);
        oneCriteriaGroup.setCriteria(oneCriteriaList);

        exclusionCriteria.add(twoCriteriaGroup);
        exclusionCriteria.add(oneCriteriaGroup);

        return exclusionCriteria;
    }

    private List<List<CriteriaGroup>> getExpectedExpandedCriteriaGroups() {
        List<List<CriteriaGroup>> expectedExpandedCriteriaGroups = new ArrayList<>();
        List<CriteriaGroup> twoCriteriaGroups = new ArrayList<>();
        List<CriteriaGroup> oneCriteriaGroups = new ArrayList<>();
        CriteriaGroup criteriaGroupA = new CriteriaGroup();
        CriteriaGroup criteriaGroupB = new CriteriaGroup();
        CriteriaGroup criteriaGroupC = new CriteriaGroup();
        List<Criterion> criteriaListA = new ArrayList<>();
        List<Criterion> criteriaListB = new ArrayList<>();
        List<Criterion> criteriaListC = new ArrayList<>();
        Criterion criterionA = new Criterion();
        Criterion criterionA1 = new Criterion();
        Criterion criterionA2 = new Criterion();
        Criterion criterionB = new Criterion();
        Criterion criterionC = new Criterion();
        List<TerminologyCode> terminologyCodesA = new ArrayList<>();
        List<TerminologyCode> terminologyCodesA1 = new ArrayList<>();
        List<TerminologyCode> terminologyCodesA2 = new ArrayList<>();
        List<TerminologyCode> terminologyCodesB = new ArrayList<>();
        List<TerminologyCode> terminologyCodesC = new ArrayList<>();


        terminologyCodesA.add(terminologyCodeA);
        terminologyCodesA1.add(terminologyCodeA1);
        terminologyCodesA2.add(terminologyCodeA2);
        terminologyCodesB.add(terminologyCodeB);
        terminologyCodesC.add(terminologyCodeC);

        criterionA.setTermCodes(terminologyCodesA);
        criterionA1.setTermCodes(terminologyCodesA1);
        criterionA2.setTermCodes(terminologyCodesA2);
        criterionB.setTermCodes(terminologyCodesB);
        criterionC.setTermCodes(terminologyCodesC);

        criteriaListA.add(criterionA);
        criteriaListA.add(criterionA1);
        criteriaListA.add(criterionA2);
        criteriaListB.add(criterionB);
        criteriaListC.add(criterionC);

        criteriaGroupA.setCriteria(criteriaListA);
        criteriaGroupB.setCriteria(criteriaListB);
        criteriaGroupC.setCriteria(criteriaListC);

        twoCriteriaGroups.add(criteriaGroupA);
        twoCriteriaGroups.add(criteriaGroupB);
        oneCriteriaGroups.add(criteriaGroupC);

        expectedExpandedCriteriaGroups.add(twoCriteriaGroups);
        expectedExpandedCriteriaGroups.add(oneCriteriaGroups);



        return expectedExpandedCriteriaGroups;
    }

    private void mockFindTermCode() {
        ExpansionTreeNode nodeA = new ExpansionTreeNode();
        nodeA.setTermCode(terminologyCodeA);
        ExpansionTreeNode nodeA1 = new ExpansionTreeNode();
        nodeA1.setTermCode(terminologyCodeA1);
        ExpansionTreeNode nodeA2 = new ExpansionTreeNode();
        nodeA2.setTermCode(terminologyCodeA2);
        nodeA.setChildren(Arrays.asList(nodeA1, nodeA2));

        ExpansionTreeNode nodeB = new ExpansionTreeNode();
        nodeB.setTermCode(terminologyCodeB);

        ExpansionTreeNode nodeC = new ExpansionTreeNode();
        nodeC.setTermCode(terminologyCodeC);

        when(expansionTree.findTermCode(terminologyCodeA)).thenReturn(nodeA);
        when(expansionTree.findTermCode(terminologyCodeB)).thenReturn(nodeB);
        when(expansionTree.findTermCode(terminologyCodeC)).thenReturn(nodeC);
    }
}
