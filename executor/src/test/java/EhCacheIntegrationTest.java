import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.AttributeSearchParameter;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import de.rwth.imi.flare.executor.AuthlessRequestorConfig;
import de.rwth.imi.flare.executor.FlareExecutor;
import de.rwth.imi.flare.requestor.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;


@Slf4j
@Testcontainers
public class EhCacheIntegrationTest {

    FlareExecutor executor;
    FhirRequestorConfig config;
    private final int malesToGenerate = 10;
    private final int femalesToGenerate = 0;
    private String baseFhirUri;
    private int idCounter = 0;
    private String singlePatientTemplate;
    ///*
    @Container
    private final FixedHostPortGenericContainer<?> fhirContainer = new FixedHostPortGenericContainer<>("samply/blaze:0.18")
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withFixedExposedPort(8080, 8080)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200))
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withEnv("LOG_LEVEL", "debug")
            .withStartupAttempts(5);//*/

    @Test
    public void mainCacheIntegrationTest() throws Exception {
        baseFhirUri = "http://localhost:" + fhirContainer.getMappedPort(8080) + "/fhir" ;
        //baseFhirUri = "http://localhost:8082/fhir"; //overwrite baseFhirUri like this if you want to use your own fhir server
        singlePatientTemplate = loadSinglePatientTemplate();
        // System.out.println("mapped port: " + fhirContainer.getMappedPort(8080));
        createExecutor();

        try{
            uploadTestData();

        }catch(IOException e){
            e.printStackTrace();
        }

        QueryExpanded query = buildGenderQuery("female");


        CompletableFuture<Integer> compFuture1 = executor.calculatePatientCount(query);
        int patientCount1 = compFuture1.get();
        //Path cachePath = Path.of("target/jcs-cache-db/default.data");
        //System.out.println("cache size: " + Files.size(cachePath));
        assertEquals(femalesToGenerate, patientCount1);



        long startTime2 = System.nanoTime();
        CompletableFuture<Integer> compFuture2 = executor.calculatePatientCount(query);
        int patientCount2 = compFuture2.get();
        float duration2 = (System.nanoTime() - startTime2) / 1000000.f;
        assertEquals(femalesToGenerate, patientCount2);



        //QueryExpanded query2 = buildGenderQuery("male");

        //TODO upload lots of patients to test how long it takes to retrieve lots of patient ids from disk
    }


    private void uploadTestData() throws IOException, InterruptedException {

        for(int i = 0; i < malesToGenerate; i++){
            String newPatient = generateSinglePatient("male");
            postPatient(newPatient);
        }

        for(int i = 0; i < femalesToGenerate; i++){
            String newPatient = generateSinglePatient("female");
            postPatient(newPatient);
        }
    }

    private void postPatient(String patient) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        patient = patient.replace("\n", "").replace("\r", "");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseFhirUri))
                .POST(HttpRequest.BodyPublishers.ofString(patient))
                .header("Content-Type", "application/fhir+json")
                .build();

        client.send(request,
                HttpResponse.BodyHandlers.ofString());
    }

    private String loadSinglePatientTemplate() throws IOException {
        String baseFilePath = new File("").getAbsolutePath();
        Path filePath = Path.of(baseFilePath + "/src/test/resources/single-patient-template.json");
        return Files.readString(filePath);
    }

    private String generateSinglePatient(String gender) {
        String newPatient = singlePatientTemplate;
        //String patId = String.valueOf(idCounter);
        String patId = UUID.randomUUID().toString();
        newPatient = newPatient.replace("pat-generated-id", patId);
        newPatient = newPatient.replace("con-generated-id", patId);
        newPatient = newPatient.replace("obs-generated-id", patId);
        newPatient = newPatient.replace("enc-generated-id", patId);
        newPatient = newPatient.replace("<gender>", gender);
        newPatient = newPatient.replace("<birthdate>", "1990-01-01");
        idCounter++;
        return newPatient;
    }

    public void createExecutor() throws URISyntaxException {
        config = new AuthlessRequestorConfig(new URI(baseFhirUri + "/"), "50", new FlareThreadPoolConfig(4,16,10));
        CacheConfig cacheConfig = new CacheConfig() {
            @Override
            public int getHeapEntryCount() {
                return 2;
            }
            @Override
            public int getDiskSizeGB() {
                return 2;
            }

            @Override
            public int getExpiryHours() {
                return 24;
            }

            @Override
            public File getCacheDir() {
                return new File( "target", "EhCacheData");
            }


        };
        executor = new FlareExecutor(new FhirRequestor(config,cacheConfig, Executors.newFixedThreadPool(16)));
    }

    private QueryExpanded buildGenderQuery(String gender) {

        Criterion criterion1 = new Criterion();
        List<TerminologyCode> femaleTerminology = Arrays.asList(new TerminologyCode("gender", "mii.abide", "Geschlecht"));
        criterion1.setTermCodes(femaleTerminology);

        MappingEntry mapping = new MappingEntry();
        AttributeSearchParameter attributeSearchParameter = new AttributeSearchParameter();
        attributeSearchParameter.setAttributeKey(new TerminologyCode("gender", "mii.abide", "Geschlecht"));
        attributeSearchParameter.setAttributeFhirPath("gender");
        attributeSearchParameter.setAttributeType("code");
        attributeSearchParameter.setAttributeSearchParameter("gender");
        mapping.setAttributeSearchParameters(List.of(attributeSearchParameter));
        mapping.setFhirResourceType("Patient");
        criterion1.setMapping(mapping);

        AttributeFilter femaleAttributeFilter = new AttributeFilter();
        femaleAttributeFilter.setType(FilterType.CONCEPT);
        TerminologyCode attributeConcept = new TerminologyCode(gender, "http://hl7.org/fhir/administrative-gender", "someDisplay");
        femaleAttributeFilter.setSelectedConcepts(List.of(attributeConcept));
        TerminologyCode femaleAttributeCode = new TerminologyCode("gender", "mii.abide", "Geschlecht");
        femaleAttributeFilter.setAttributeCode(femaleAttributeCode);
        List<AttributeFilter> attributeFilters = List.of(femaleAttributeFilter);
        criterion1.setAttributeFilters(attributeFilters);

        CriteriaGroup criteriaGroup1 = new CriteriaGroup(List.of(criterion1));
        Query expectedResult = new Query();
        expectedResult.setInclusionCriteria(List.of(criteriaGroup1));
        QueryExpanded parsedQuery = new QueryExpanded();
        parsedQuery.setInclusionCriteria(expectedResult.getInclusionCriteria());

        return parsedQuery;
    }

}
