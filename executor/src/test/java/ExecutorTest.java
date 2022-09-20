import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import de.rwth.imi.flare.executor.AuthlessRequestorConfig;
import de.rwth.imi.flare.executor.FlareExecutor;
import de.rwth.imi.flare.requestor.CacheConfig;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;
import de.rwth.imi.flare.requestor.FlareThreadPoolConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ExecutorTest
{
    FhirRequestorConfig config;
    FlareExecutor executor;

    public ExecutorTest() throws URISyntaxException {
        config = new AuthlessRequestorConfig(new URI("http://localhost:8080/fhir/"), "50", false, new FlareThreadPoolConfig(4,16,10));
        executor = new FlareExecutor(config, new CacheConfig() {
            @Override
            public int getCleanCycleMS() {
                return 1 * 24 * 60 * 60 * 1000;
            }

            @Override
            public int getEntryLifetimeMS() {
                return 7 * 24 * 60 * 60 * 1000;
            }

            @Override
            public int getMaxCacheEntries() {
                return 8000;
            }

            @Override
            public boolean getUpdateExpiryAtAccess() {
                return false;
            }

            @Override
            public boolean getDeleteAllEntriesOnCleanup() {
                return false;
            }
        });
    }

    @Test
    public void testExecutor() throws ExecutionException, InterruptedException {
        QueryExpanded query = buildQuery();
        CompletableFuture<Integer> calc = executor.calculatePatientCount(query);
        System.out.printf("found %d values", calc.get());
    }

    private QueryExpanded buildQuery() {
        List<TerminologyCode> female_terminology = Arrays.asList(new TerminologyCode("76689-9", "http://loinc.org", "Sex assigned at birth"));
        ValueFilter female_filter = new ValueFilter(FilterType.CONCEPT, List.of(new TerminologyCode("female", "http://hl7.org/fhir/administrative-gender", "Female")), null, null , null, null, null);
        MappingEntry mapping = new MappingEntry(null, "Observation","code", "value-concept", new ArrayList<>(), "", new ArrayList<>());
        Criterion criterion1 = new Criterion(female_terminology, female_filter, mapping, null, null);
        CriteriaGroup criteriaGroup1 = new CriteriaGroup(List.of(criterion1));

        Query expectedResult = new Query();
        expectedResult.setInclusionCriteria(List.of(criteriaGroup1));
        expectedResult.setExclusionCriteria(new ArrayList<>());

        QueryExpanded parsedQuery = new QueryExpanded();
        // TODO: specific init needed?
        parsedQuery.setInclusionCriteria(expectedResult.getInclusionCriteria());
        List<List<CriteriaGroup>> exclusionCriteria = new LinkedList<>();
        for(CriteriaGroup criteriaGroup: expectedResult.getExclusionCriteria()){
            List<CriteriaGroup> subCriteria = new LinkedList<>();
            for(Criterion criterion: criteriaGroup.getCriteria()){
                CriteriaGroup newGroup = new CriteriaGroup();
                List<Criterion> criteria = new LinkedList<>();
                criteria.add(criterion);
                newGroup.setCriteria(criteria);
                subCriteria.add(newGroup);
            }
            exclusionCriteria.add(subCriteria);
        }
        parsedQuery.setExclusionCriteria(exclusionCriteria);

        return parsedQuery;
    }
}
