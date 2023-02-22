package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.Requestor;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.ExpandedQuery;
import de.rwth.imi.flare.api.model.TerminologyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FlareExecutorTest {

    private TerminologyCode terminologyCodeIncl;
    private TerminologyCode terminologyCodeA;
    private TerminologyCode terminologyCodeA1;
    private TerminologyCode terminologyCodeA2;
    private TerminologyCode terminologyCodeB;
    private TerminologyCode terminologyCodeC;

    Criterion criterionA;
    Criterion criterionA1;
    Criterion criterionA2;
    Criterion criterionB;
    Criterion criterionC;
    Criterion inclCriterion;

    @Mock
    private Requestor requestor;

    private FlareExecutor flareExecutor;

    @BeforeEach
    void setUp() {
        flareExecutor = new FlareExecutor(requestor);
        terminologyCodeIncl = new TerminologyCode("Incl", "Incl", "Incl");
        terminologyCodeA = new TerminologyCode("A", "A", "A");
        terminologyCodeA1 = new TerminologyCode("A1", "A1", "A1");
        terminologyCodeA2 = new TerminologyCode("A2", "A2", "A2");
        terminologyCodeB = new TerminologyCode("B", "B", "B");
        terminologyCodeC = new TerminologyCode("C", "C", "C");
    }

    @Test
    void calculatePatientCountWorkingAtAll() throws Exception {
        // logic is :
        // Inclusion / ( ( (A v A1 v A2) ^ B) v C)
        var ids = Map.of("Inclusion", Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                "A", Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                "A1", Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                "A2", Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                "B", Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                "C", Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));

        ExpandedQuery query = createExpandedQuery();
        mockGetIds(ids);

        CompletableFuture<Integer> number = flareExecutor.calculatePatientCount(query);

        assertEquals(0, number.get());
    }

    @Test
    void calculatePatientCountOuterOrWorking() throws Exception {
        // logic is :
        // Inclusion / ( ( (A v A1 v A2) ^ B) v C)
        var ids = Map.of("Inclusion", Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                "A", Set.of("0", "1", "2", "3"),
                "A1", Set.of("0", "1", "2", "3"),
                "A2", Set.of("0", "1", "2", "3"),
                "B", Set.of("0", "1", "2", "3"),
                "C", Set.of("4", "5", "6", "7", "8", "9"));

        ExpandedQuery query = createExpandedQuery();
        mockGetIds(ids);

        CompletableFuture<Integer> number = flareExecutor.calculatePatientCount(query);

        assertEquals(0, number.get());
    }

    @Test
    void calculatePatientCountAndWorking() throws Exception {
        // logic is :
        // Inclusion / ( ( (A v A1 v A2) ^ B) v C)
        var ids = Map.<String, Set<String>>of("Inclusion", Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                "A", Set.of("0", "1", "2", "3"),
                "A1", Set.of("0", "1", "2", "3"),
                "A2", Set.of("0", "1", "2", "3"),
                "B", Set.of("0", "1"),
                "C", Set.of());

        ExpandedQuery query = createExpandedQuery();
        mockGetIds(ids);

        CompletableFuture<Integer> number = flareExecutor.calculatePatientCount(query);

        assertEquals(8, number.get());
    }

    @Test
    void calculatePatientCountInnerOrWorking() throws Exception {
        // logic is :
        // Inclusion / ( ( (A v A1 v A2) ^ B) v C)
        var ids = Map.<String, Set<String>>of("Inclusion", Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"),
                "A", Set.of("0", "1"),
                "A1", Set.of("2", "3"),
                "A2", Set.of("0", "2", "4", "5"),
                "B", Set.of("0", "1", "2", "3", "4", "5"),
                "C", Set.of());

        ExpandedQuery query = createExpandedQuery();
        mockGetIds(ids);

        CompletableFuture<Integer> number = flareExecutor.calculatePatientCount(query);

        assertEquals(4, number.get());
    }

    private void mockGetIds(Map<String, Set<String>> ids) {
        if (ids.size() != 6) {
            throw new IllegalArgumentException("The number of Id-lists need to be 6, for A, A1, A2, B and C.");
        }
        when(requestor.execute(inclCriterion)).thenReturn(CompletableFuture.completedFuture(ids.get("Inclusion")));
        when(requestor.execute(criterionA)).thenReturn(CompletableFuture.completedFuture(ids.get("A")));
        when(requestor.execute(criterionA1)).thenReturn(CompletableFuture.completedFuture(ids.get("A1")));
        when(requestor.execute(criterionA2)).thenReturn(CompletableFuture.completedFuture(ids.get("A2")));
        when(requestor.execute(criterionB)).thenReturn(CompletableFuture.completedFuture(ids.get("B")));
        when(requestor.execute(criterionC)).thenReturn(CompletableFuture.completedFuture(ids.get("C")));
    }

    private ExpandedQuery createExpandedQuery() {
        ExpandedQuery expandedQuery = new ExpandedQuery();
        expandedQuery.setInclusionCriteria(createInclusionCriteriaGroups());
        expandedQuery.setExclusionCriteria(createExclusionCriteriaGroups());
        return expandedQuery;
    }

    private List<CriteriaGroup> createInclusionCriteriaGroups() {
        inclCriterion = new Criterion();
        inclCriterion.setTermCodes(List.of(terminologyCodeIncl));

        CriteriaGroup inclusionCriteriaGroup = new CriteriaGroup();
        inclusionCriteriaGroup.setCriteria(List.of(inclCriterion));

        return List.of(inclusionCriteriaGroup);
    }

    private List<List<CriteriaGroup>> createExclusionCriteriaGroups() {
        criterionA = new Criterion();
        criterionA1 = new Criterion();
        criterionA2 = new Criterion();
        criterionB = new Criterion();
        criterionC = new Criterion();

        criterionA.setTermCodes(List.of(terminologyCodeA));
        criterionA1.setTermCodes(List.of(terminologyCodeA1));
        criterionA2.setTermCodes(List.of(terminologyCodeA2));
        criterionB.setTermCodes(List.of(terminologyCodeB));
        criterionC.setTermCodes(List.of(terminologyCodeC));

        CriteriaGroup criteriaGroupA = new CriteriaGroup();
        CriteriaGroup criteriaGroupB = new CriteriaGroup();
        CriteriaGroup criteriaGroupC = new CriteriaGroup();

        criteriaGroupA.setCriteria(List.of(criterionA, criterionA1, criterionA2));
        criteriaGroupB.setCriteria(List.of(criterionB));
        criteriaGroupC.setCriteria(List.of(criterionC));

        return List.of(List.of(criteriaGroupA, criteriaGroupB), List.of(criteriaGroupC));
    }
}
