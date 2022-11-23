
import ca.uhn.fhir.context.FhirContext;
import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Comparator;
import de.rwth.imi.flare.requestor.FhirSearchRequest;
import de.rwth.imi.flare.requestor.IncorrectQueryInputException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class FlareFhirAgeIT {

    @Container
    private final GenericContainer<?> fhirContainer = new GenericContainer<>(DockerImageName.parse("ghcr.io/medizininformatik-initiative/blaze:0.17"))
            .withExposedPorts(8080)
            .withStartupAttempts(5);


    @Test
    public void mainIntegrationTest() throws URISyntaxException, InterruptedException, IncorrectQueryInputException {
        String baseUri = "http://localhost:" + fhirContainer.getMappedPort(8080) + "/fhir" ;

        try{
            uploadTestData(baseUri);
        }catch(IOException e){
            e.printStackTrace();
        }

        /* test data birth dates:
         *   id 1: 1983-05-17
         *   id 2: 1990-01-01
         *   id 3: 1990-11-01
         *   id 4: 2022-10-01
         *   id 5: 2022-11-01
         * */
        TestAgeStringBuilder sb = new TestAgeStringBuilder();

        assertEquals(1, getNumberResultPatIds (baseUri + "/" + sb.ageSingleComparisonRequest(getPatientAge("1983-05-17", "a"), "a", Comparator.gt)));
        assertEquals(2, getNumberResultPatIds (baseUri + "/" + sb.ageSingleComparisonRequest(getPatientAge("1990-01-01", "a"), "a", Comparator.lt)));
        assertEquals(3, getNumberResultPatIds (baseUri + "/" + sb.ageSingleComparisonRequest(getPatientAge("1990-11-01", "mo"), "mo", Comparator.gt)));
        assertEquals(5, getNumberResultPatIds (baseUri + "/" + sb.ageSingleComparisonRequest(getPatientAge("2022-10-01", "mp"), "mo", Comparator.gt)));
        assertEquals(1, getNumberResultPatIds (baseUri + "/" + sb.ageSingleComparisonRequest(getPatientAge("2022-11-01", "wk"), "wk", Comparator.lt)));

        assertEquals(2, getNumberResultPatIds (baseUri + "/" + sb.ageRangeRequest(getPatientAge("2022-10-01", "mo"), getPatientAge("1990-01-01", "mo"), "mo")));
    }

        private double getPatientAge(String birthdateString, String unit){
            LocalDate birthdate = LocalDate.parse(birthdateString);
            double patientAge = 0;
            switch(unit){
                case "a" -> patientAge = ChronoUnit.YEARS.between(birthdate, LocalDate.now());
                case "mo" -> patientAge = ChronoUnit.MONTHS.between(birthdate, LocalDate.now());
                case "wk" -> patientAge = ChronoUnit.WEEKS.between(birthdate, LocalDate.now());
            }
            return patientAge;
    }

    private void uploadTestData(String baseUri) throws IOException, InterruptedException {
        String baseFilePath = new File("").getAbsolutePath();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(baseFilePath + "/src/test/java/patient-age-testdata.ndjson"));
        String nextLine = bufferedReader.readLine();
        HttpClient client = HttpClient.newHttpClient();
        while(nextLine != null){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUri))
                    .POST(HttpRequest.BodyPublishers.ofString(nextLine))
                    .header("Content-Type", "application/fhir+json")
                    .build();

            client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            nextLine = bufferedReader.readLine();
        }
        bufferedReader.close();
    }

    private int getNumberResultPatIds (String uri) throws URISyntaxException {
        FhirSearchRequest fhirSearchRequest = new FhirSearchRequest(new URI(uri), "50", FhirContext.forR4());

        System.out.println("URI: " + uri + " \nfound patients: ");
        int patientCount = 0;
        while (fhirSearchRequest.hasNext()) {
            FlareResource res = fhirSearchRequest.next();
            System.out.println(res.getPatientId());
            patientCount++;
        }
        System.out.println("---");
        return patientCount;
    }
}
