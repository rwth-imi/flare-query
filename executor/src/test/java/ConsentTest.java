import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import de.rwth.imi.flare.executor.AuthlessRequestorConfig;
import de.rwth.imi.flare.executor.FlareExecutor;
import de.rwth.imi.flare.requestor.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;


@Testcontainers
public class ConsentTest {

    FlareExecutor executor;
    FhirRequestorConfig config;

    private String baseFhirUri;
    private int idCounter = 0;

    @Container
    private final GenericContainer<?> fhirContainer = new GenericContainer<>(DockerImageName.parse("ghcr.io/medizininformatik-initiative/blaze:0.17"))
            .withExposedPorts(8080)
            .withStartupAttempts(5);

    @Test
    public void mainConsentTest() throws URISyntaxException, ExecutionException, InterruptedException, IOException {
        baseFhirUri = "http://localhost:" + fhirContainer.getMappedPort(8080) + "/fhir" ;
        //baseFhirUri = "http://localhost:8082/fhir"; //overwrite baseFhirUri like this if you want to use your own fhir server
        createExecutor();
        postFhirBundle();

        Criterion consentCriterion = createConsentCriterion();
        Criterion observationCriterion = createObservationCriterion();

        /*
        Testdata:
        (each variant with 2 different ids)
        -A: 2 pats with cons and obs in range
        -B: 2 pats with cons and obs not in range
        -C: 2 pats with cons without obs
        -D: 2 pats with obs without cons

         */

        assertEquals(executeQuery(buildConsentQuery(List.of(consentCriterion), new LinkedList<>())), 6); //only cons in inclusion  (should find A, B, C)
        assertEquals(executeQuery(buildConsentQuery( new LinkedList<>(), List.of(consentCriterion))), 0); //only cons in exclusion (should find none)

        assertEquals(executeQuery(buildConsentQuery(List.of(consentCriterion, observationCriterion), new LinkedList<>())), 2); //cons in inclusion and obs in inclusion (should find A)
        assertEquals(executeQuery(buildConsentQuery(List.of(consentCriterion), List.of(observationCriterion))), 2); //cons in inclusion and obs in exclusion (should find C)

        assertEquals(executeQuery(buildConsentQuery(List.of(observationCriterion), List.of(consentCriterion))), 2); //cons in exclusion and obs in inclusion (should find D)
        assertEquals(executeQuery(buildConsentQuery( new LinkedList<>(), List.of(consentCriterion, observationCriterion))), 0); //cons in exclusion and obs in exclusion (should find none)
    }

    private int executeQuery(QueryExpanded query){
        try{
            CompletableFuture<Integer> patientCountCompFuture = executor.calculatePatientCount(query);
            int patientCount = patientCountCompFuture.get();
            System.out.println("Patient Count: " + patientCount);
            return patientCount;
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }



    public void createExecutor() throws URISyntaxException {
        config = new AuthlessRequestorConfig(new URI(baseFhirUri + "/"), "50", new FlareThreadPoolConfig(4,16,10));
        executor = new FlareExecutor(new FhirRequestor(config, Executors.newFixedThreadPool(16)));
    }

    private void postFhirBundle() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        List<String> fhirBundleList = loadFhirBundle();
        for(String fhirBundle : fhirBundleList){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseFhirUri))
                    .POST(HttpRequest.BodyPublishers.ofString(fhirBundle))
                    .header("Content-Type", "application/fhir+json")
                    .build();

            client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        }

    }

    private List<String> loadFhirBundle() throws IOException {
        String baseFilePath = new File("").getAbsolutePath();
        Path filePath = Path.of(baseFilePath + "/src/test/java/fhir-bundle-with-consent");
        return Files.readAllLines(filePath);
    }


    private Criterion createObservationCriterion(){
        Criterion criterion = new Criterion();
        List<TerminologyCode> observationTermCode = Arrays.asList(new TerminologyCode("55782-7", "http://loinc.org", "IgE"));
        criterion.setTermCodes(observationTermCode);

        ValueFilter obsValueFilter = new ValueFilter();
        obsValueFilter.setType(FilterType.QUANTITY_COMPARATOR);
        TerminologyCode obsUnit = new TerminologyCode();
        obsUnit.setCode("g/dL");
        obsValueFilter.setUnit(obsUnit);
        obsValueFilter.setValue(0.0);
        obsValueFilter.setComparator(Comparator.gt);
        //obsValueFilter.setSelectedConcepts(new LinkedList<>());
        criterion.setValueFilter(obsValueFilter);

        MappingEntry obsMapping = new MappingEntry();
        obsMapping.setFhirResourceType("Observation");
        obsMapping.setTermCodeSearchParameter("code");
        obsMapping.setValueSearchParameter("value-quantity");
        criterion.setMapping(obsMapping);

        return criterion;
    }
    private Criterion createConsentCriterion(){
        Criterion criterion = new Criterion();
        List<TerminologyCode> consentTermCode = Arrays.asList(new TerminologyCode("2.16.840.1.113883.3.1937.777.24.5.1.1", "urn:oid:2.16.840.1.113883.3.1937.777.24.5.1", "someDisplay"));
        criterion.setTermCodes(consentTermCode);

        MappingEntry mapping = new MappingEntry();
        mapping.setFhirResourceType("Consent");
        mapping.setTermCodeSearchParameter("mii-provision-provision-code-type");
        mapping.setValueSearchParameter("mii-provision-provision-code-type");
        criterion.setMapping(mapping);

        ValueFilter consValueFilter = new ValueFilter();
        TerminologyCode selectedConcept = new TerminologyCode("permit", "", "permit");
        consValueFilter.setType(FilterType.CONCEPT);
        consValueFilter.setSelectedConcepts(List.of(selectedConcept));
        criterion.setValueFilter(consValueFilter);

        return criterion;
    }
    private QueryExpanded buildConsentQuery(List<Criterion> inclusionCriteria, List<Criterion> exclusionCriteria) {
        QueryExpanded parsedQuery = new QueryExpanded();

        Query newQuery = new Query();
        if(inclusionCriteria.size() > 0){
            CriteriaGroup criteriaGroup1 = new CriteriaGroup(inclusionCriteria);
            newQuery.setInclusionCriteria(List.of(criteriaGroup1));

            parsedQuery.setInclusionCriteria(newQuery.getInclusionCriteria());

        }

        if(exclusionCriteria.size() > 0){
            CriteriaGroup criteriaGroup2 = new CriteriaGroup(exclusionCriteria);
            newQuery.setExclusionCriteria(List.of(criteriaGroup2));

            parsedQuery.setExclusionCriteria(List.of(newQuery.getExclusionCriteria()));
        }

        return parsedQuery;
    }

}
