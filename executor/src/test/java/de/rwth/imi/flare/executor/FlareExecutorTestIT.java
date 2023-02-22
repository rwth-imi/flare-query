package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.AttributeSearchParameter;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import de.rwth.imi.flare.requestor.CacheConfig;
import de.rwth.imi.flare.requestor.FhirRequestor;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;
import de.rwth.imi.flare.requestor.FlareThreadPoolConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@Testcontainers
public class FlareExecutorTestIT {

    public static final int MALES = 100;
    public static final int FEMALES = 10;

    private FhirRequestor requestor;
    private FlareExecutor executor;
    private String baseFhirUri;

    @Container
    private final FixedHostPortGenericContainer<?> fhirContainer = new FixedHostPortGenericContainer<>("samply/blaze:0.19")
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withFixedExposedPort(8080, 8080)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200))
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withEnv("LOG_LEVEL", "debug")
            .withStartupAttempts(FEMALES);

    @BeforeEach
    void setUp() throws Exception {
        baseFhirUri = "http://localhost:%d/fhir".formatted(fhirContainer.getMappedPort(8080));
        FhirRequestorConfig config = new AuthlessRequestorConfig(URI.create(baseFhirUri + "/"), "50",
                new FlareThreadPoolConfig(4, 16, 10));
        final var tempDir = Files.createTempDirectory("cache");
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
                return tempDir.toFile();
            }
        };
        requestor = new FhirRequestor(config, cacheConfig, Executors.newFixedThreadPool(16));
        executor = new FlareExecutor(requestor);
    }

    @AfterEach
    void tearDown() {
        requestor.close();
    }

    @Test
    public void calculatePatientCount() throws Exception {
        uploadTestData();

        assertEquals(MALES, executor.calculatePatientCount(buildGenderQuery("male")).get());
        assertEquals(FEMALES, executor.calculatePatientCount(buildGenderQuery("female")).get());
    }


    private void uploadTestData() throws Exception {
        for (int i = 0; i < MALES; i++) {
            postPatient(generatePatient("male"));
        }

        for (int i = 0; i < FEMALES; i++) {
            postPatient(generatePatient("female"));
        }
    }

    private void postPatient(String patient) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseFhirUri))
                .POST(HttpRequest.BodyPublishers.ofString(patient))
                .header("Content-Type", "application/fhir+json")
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String generatePatient(String gender) throws IOException {
        String patient = slurp("single-patient-template.json");
        String patId = UUID.randomUUID().toString();
        patient = patient.replace("pat-generated-id", patId);
        patient = patient.replace("con-generated-id", patId);
        patient = patient.replace("obs-generated-id", patId);
        patient = patient.replace("enc-generated-id", patId);
        patient = patient.replace("<gender>", gender);
        patient = patient.replace("<birthdate>", "1990-01-01");
        return patient;
    }

    private ExpandedQuery buildGenderQuery(String gender) {

        Criterion criterion1 = new Criterion();
        List<TerminologyCode> femaleTerminology = List.of(new TerminologyCode("gender", "mii.abide", "Geschlecht"));
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
        ExpandedQuery parsedQuery = new ExpandedQuery();
        parsedQuery.setInclusionCriteria(expectedResult.getInclusionCriteria());

        return parsedQuery;
    }

    private static String slurp(String name) throws IOException {
        try (InputStream in = FlareExecutorTestIT.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException(format("Can't find `%s` in classpath.", name));
            } else {
                return new String(in.readAllBytes(), UTF_8);
            }
        } catch (IOException e) {
            throw new IOException(format("error while reading the file `%s` from classpath", name));
        }
    }
}
