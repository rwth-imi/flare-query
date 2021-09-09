import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.requestor.FhirSearchRequest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class TestFhirRequestor {
    @Test
    public void testRequest() throws URISyntaxException {
        String t = "http://localhost:8080/fhir/Condition?code=http%3A%2F%2Ffhir.de%2FCodeSystem%2Fdimdi%2Ficd-10-gm%7CJ45.9";
        FhirSearchRequest fhirSearchRequest = new FhirSearchRequest(new URI(t), null);
        while (fhirSearchRequest.hasNext()) {
            FlareResource res = fhirSearchRequest.next();
            System.out.println(res.getPatientId());
        }
    }
}
