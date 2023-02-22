
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.QueryExpanded;
import de.rwth.imi.flare.api.model.TerminologyCode;
import de.rwth.imi.flare.executor.AuthlessRequestorConfig;
import de.rwth.imi.flare.executor.FhirIdRequestor;
import de.rwth.imi.flare.executor.FlareExecutor;
import de.rwth.imi.flare.requestor.CacheConfig;
import de.rwth.imi.flare.requestor.FhirRequestor;
import de.rwth.imi.flare.requestor.FlareThreadPoolConfig;
import java.util.concurrent.Executors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Disabled("TODO: fix tests")
@ExtendWith(MockitoExtension.class)
public class ExecutorTests {

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
    FhirIdRequestor fhirIdRequestor;
    FlareExecutor flareExecutor;
    QueryExpanded queryExpanded;

    @BeforeEach
    void setUp() throws URISyntaxException {
        terminologyCodeIncl = new TerminologyCode("Incl", "Incl", "Incl");
        terminologyCodeA = new TerminologyCode("A", "A", "A");
        terminologyCodeA1 = new TerminologyCode("A1", "A1", "A1");
        terminologyCodeA2 = new TerminologyCode("A2", "A2", "A2");
        terminologyCodeB = new TerminologyCode("B", "B", "B");
        terminologyCodeC = new TerminologyCode("C", "C", "C");

        AuthlessRequestorConfig config = new AuthlessRequestorConfig(
            new URI("http://localhost:8080/fhir/"),
            "50", new FlareThreadPoolConfig(4, 16, 10));
        CacheConfig cacheConfig = new CacheConfig() {
            @Override
            public int getCacheSizeInMb() {
                return 100;
            }

            @Override
            public int getEntryRefreshTimeHours() {
                return 1;
            }
        };
        flareExecutor = new FlareExecutor(new FhirRequestor(config, cacheConfig, Executors.newFixedThreadPool(16)));
        queryExpanded = getQueryExpanded();
    }

    @Test
    void calculatePatientCountWorkingAtAll() throws ExecutionException, InterruptedException {
        Map<String, List<String>> ids = new HashMap<>();
        // logic is :
        // Inclusion / ( ( (A v A1 v A2) ^ B) v C)
        ids.put("Inclusion", Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        ids.put("A", Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        ids.put("A1", Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        ids.put("A2", Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        ids.put("B", Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        ids.put("C", Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));

        mockGetIds(ids);

        CompletableFuture<Integer> number = flareExecutor.calculatePatientCount(queryExpanded);

        assertEquals(0, number.get());
    }

    @Test
    void calculatePatientCountOuterOrWorking() throws ExecutionException, InterruptedException {
        Map<String, List<String>> ids = new HashMap<>();
        // logic is :
        // Inclusion / ( ( (A v A1 v A2) ^ B) v C)
        ids.put("Inclusion", Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        ids.put("A", Arrays.asList("0", "1", "2", "3"));
        ids.put("A1", Arrays.asList("0", "1", "2", "3"));
        ids.put("A2", Arrays.asList("0", "1", "2", "3"));
        ids.put("B", Arrays.asList("0", "1", "2", "3"));
        ids.put("C", Arrays.asList("4", "5", "6", "7", "8", "9"));

        mockGetIds(ids);

        CompletableFuture<Integer> number = flareExecutor.calculatePatientCount(queryExpanded);

        assertEquals(0, number.get());
    }

    @Test
    void calculatePatientCountAndWorking() throws ExecutionException, InterruptedException {
        Map<String, List<String>> ids = new HashMap<>();
        // logic is :
        // Inclusion / ( ( (A v A1 v A2) ^ B) v C)
        ids.put("Inclusion", Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        ids.put("A", Arrays.asList("0", "1", "2", "3"));
        ids.put("A1", Arrays.asList("0", "1", "2", "3"));
        ids.put("A2", Arrays.asList("0", "1", "2", "3"));
        ids.put("B", Arrays.asList("0", "1"));
        ids.put("C", new ArrayList<>());

        mockGetIds(ids);

        CompletableFuture<Integer> number = flareExecutor.calculatePatientCount(queryExpanded);

        assertEquals(8, number.get());
    }

    @Test
    void calculatePatientCountInnerOrWorking() throws ExecutionException, InterruptedException {
        Map<String, List<String>> ids = new HashMap<>();
        // logic is :
        // Inclusion / ( ( (A v A1 v A2) ^ B) v C)
        ids.put("Inclusion", Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
        ids.put("A", Arrays.asList("0", "1"));
        ids.put("A1", Arrays.asList("2", "3"));
        ids.put("A2", Arrays.asList("0", "2", "4", "5"));
        ids.put("B", Arrays.asList("0", "1", "2", "3", "4", "5"));
        ids.put("C", new ArrayList<>());

        mockGetIds(ids);

        CompletableFuture<Integer> number = flareExecutor.calculatePatientCount(queryExpanded);

        assertEquals(4, number.get());
    }

    private void mockGetIds(Map<String, List<String>> ids) {
        if (ids.size() != 6) {
            throw new IllegalArgumentException("The number of Id-lists need to be 6, for A, A1, A2, B and C.");
        }
        CompletableFuture<Set<String>> includedIds = CompletableFuture.supplyAsync(() -> new HashSet<>(ids.get("Inclusion")));
        when(fhirIdRequestor.getPatientIdsFittingCriterion(inclCriterion)).thenReturn(includedIds);
        CompletableFuture<Set<String>> excludedIdsA = CompletableFuture.supplyAsync(() -> new HashSet<>(ids.get("A")));
        when(fhirIdRequestor.getPatientIdsFittingCriterion(criterionA)).thenReturn(excludedIdsA);
        CompletableFuture<Set<String>> excludedIdsA1 = CompletableFuture.supplyAsync(() -> new HashSet<>(ids.get("A1")));
        when(fhirIdRequestor.getPatientIdsFittingCriterion(criterionA1)).thenReturn(excludedIdsA1);
        CompletableFuture<Set<String>> excludedIdsA2 = CompletableFuture.supplyAsync(() -> new HashSet<>(ids.get("A2")));
        when(fhirIdRequestor.getPatientIdsFittingCriterion(criterionA2)).thenReturn(excludedIdsA2);
        CompletableFuture<Set<String>> excludedIdsB = CompletableFuture.supplyAsync(() -> new HashSet<>(ids.get("B")));
        when(fhirIdRequestor.getPatientIdsFittingCriterion(criterionB)).thenReturn(excludedIdsB);
        CompletableFuture<Set<String>> excludedIdsC = CompletableFuture.supplyAsync(() -> new HashSet<>(ids.get("C")));
        when(fhirIdRequestor.getPatientIdsFittingCriterion(criterionC)).thenReturn(excludedIdsC);
    }

    @NotNull
    private QueryExpanded getQueryExpanded() {
        QueryExpanded queryExpanded = new QueryExpanded();
        queryExpanded.setInclusionCriteria(getInclusionCriteriaGroups());
        queryExpanded.setExclusionCriteria(getExclusionCriteriaGroups());
        return queryExpanded;
    }

    @NotNull
    private List<CriteriaGroup> getInclusionCriteriaGroups() {
        List<CriteriaGroup> inclusionCriteria = new ArrayList<>();
        CriteriaGroup inclusionCriteriaGroup = new CriteriaGroup();
        List<Criterion> inclusionCriteriaList = new ArrayList<>();
        inclCriterion = new Criterion();
        List<TerminologyCode> codes = new ArrayList<>();
        codes.add(terminologyCodeIncl);
        inclCriterion.setTermCodes(codes);
        inclusionCriteriaList.add(inclCriterion);
        inclusionCriteriaGroup.setCriteria(inclusionCriteriaList);
        inclusionCriteria.add(inclusionCriteriaGroup);
        return inclusionCriteria;
    }

    @NotNull
    private List<List<CriteriaGroup>> getExclusionCriteriaGroups() {
        List<List<CriteriaGroup>> expectedExpandedCriteriaGroups = new ArrayList<>();
        List<CriteriaGroup> twoCriteriaGroups = new ArrayList<>();
        List<CriteriaGroup> oneCriteriaGroups = new ArrayList<>();
        CriteriaGroup criteriaGroupA = new CriteriaGroup();
        CriteriaGroup criteriaGroupB = new CriteriaGroup();
        CriteriaGroup criteriaGroupC = new CriteriaGroup();
        List<Criterion> criteriaListA = new ArrayList<>();
        List<Criterion> criteriaListB = new ArrayList<>();
        List<Criterion> criteriaListC = new ArrayList<>();
        criterionA = new Criterion();
        criterionA1 = new Criterion();
        criterionA2 = new Criterion();
        criterionB = new Criterion();
        criterionC = new Criterion();
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
}
