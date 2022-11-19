
import ca.uhn.fhir.context.FhirContext;
import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.Comparator;
import de.rwth.imi.flare.requestor.FhirSearchRequest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Testcontainers
public class FlareFhirAgeIT {

    @Container
    private final GenericContainer<?> fhirContainer = new GenericContainer<>(DockerImageName.parse("ghcr.io/medizininformatik-initiative/blaze:0.17"))
            .withExposedPorts(8080)
            .withStartupAttempts(5);


    @Test
    public void mainIntegrationTest() throws URISyntaxException, InterruptedException  {
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
        fhirRequest(baseUri + "/" + sb.ageSingeComparisonRequest(35.0, "a", Comparator.gt));
        fhirRequest(baseUri + "/" + sb.ageSingeComparisonRequest(32.0, "a", Comparator.lt));
        fhirRequest(baseUri + "/" + sb.ageSingeComparisonRequest(32.53456, "a", Comparator.gt));
        fhirRequest(baseUri + "/" + sb.ageSingeComparisonRequest(32.0 * 12.0, "mo", Comparator.gt));
        fhirRequest(baseUri + "/" + sb.ageSingeComparisonRequest(2.0, "mo", Comparator.lt));

        fhirRequest(baseUri + "/" + sb.ageRangeRequest(32.0*12, 32.0*12+1, "mo"));
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

    private void fhirRequest(String uri) throws URISyntaxException {
        FhirSearchRequest fhirSearchRequest = new FhirSearchRequest(new URI(uri), "50", FhirContext.forR4());

        System.out.println("URI: " + uri + " \nfound patients: ");
        while (fhirSearchRequest.hasNext()) {
            FlareResource res = fhirSearchRequest.next();
            System.out.println(res.getPatientId());
        }
        System.out.println("---");
    }
}
