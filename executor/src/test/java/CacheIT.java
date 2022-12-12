import ca.uhn.fhir.context.FhirContext;
import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.AttributeSearchParameter;
import de.rwth.imi.flare.api.model.mapping.FixedCriteria;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import de.rwth.imi.flare.executor.AuthlessRequestorConfig;
import de.rwth.imi.flare.executor.FlareExecutor;
import de.rwth.imi.flare.requestor.*;
import org.hl7.fhir.r4.model.IntegerType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.annotation.JsonAppend;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;


@Testcontainers
public class CacheIT {

    FlareExecutor executor;
    FhirRequestorConfig config;
    private final int malesToGenerate =15;
    private final int femalesToGenerate = 10;
    private String baseFhirUri;
    private String baseFlareUri;
    private int idCounter = 0;
    private String singlePatientTemplate;

    @Container
    private final GenericContainer<?> fhirContainer = new GenericContainer<>(DockerImageName.parse("ghcr.io/medizininformatik-initiative/blaze:0.17"))
            .withExposedPorts(8080)
            .withStartupAttempts(5);

    @Test
    public void mainCacheIntegrationTest() throws URISyntaxException, ExecutionException, InterruptedException, IOException {
        baseFhirUri = "http://localhost:" + fhirContainer.getMappedPort(8080) + "/fhir" ;
        singlePatientTemplate = loadSinglePatientTemplate();
        createExecutor();

        try{
            uploadTestData();
        }catch(IOException e){
            e.printStackTrace();
        }

        QueryExpanded query = buildGenderQuery("female");

        long startTime1 = System.nanoTime();
        CompletableFuture<Integer> compFuture1 = executor.calculatePatientCount(query);
        int patientCount1 = compFuture1.get();
        float duration1 = (System.nanoTime() - startTime1) / 1000000.f;
        assertEquals(femalesToGenerate, patientCount1);

        long startTime2 = System.nanoTime();
        CompletableFuture<Integer> compFuture2 = executor.calculatePatientCount(query);
        int patientCount2 = compFuture2.get();
        float duration2 = (System.nanoTime() - startTime2) / 1000000.f;
        assertEquals(femalesToGenerate, patientCount2);

        System.out.println("time not cached: "  + duration1 + "ms");
        System.out.println("time cached: " + duration2 + "ms");


        //TODO upload lots of patients to test how long it takes to retreive lots of patient ids from disk
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
        Path filePath = Path.of(baseFilePath + "/src/test/java/single-patient-template.json");
        return Files.readString(filePath);
    }

    private String generateSinglePatient(String gender) {
        String newPatient = singlePatientTemplate;
        newPatient = newPatient.replace("pat-generated-id", String.valueOf(idCounter));
        newPatient = newPatient.replace("con-generated-id", String.valueOf(idCounter));
        newPatient = newPatient.replace("obs-generated-id", String.valueOf(idCounter));
        newPatient = newPatient.replace("enc-generated-id", String.valueOf(idCounter));
        newPatient = newPatient.replace("<gender>", gender);
        newPatient = newPatient.replace("<birthdate>", "1990-01-01");
        idCounter++;
        return newPatient;
    }

    public void createExecutor() throws URISyntaxException {
        config = new AuthlessRequestorConfig(new URI(baseFhirUri + "/"), "50", new FlareThreadPoolConfig(4,16,10));
        executor = new FlareExecutor(new FhirRequestor(config, Executors.newFixedThreadPool(16)));
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
