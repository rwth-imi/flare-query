import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.requestor.Request;
import org.junit.jupiter.api.Test;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;

public class TestFhirRequestor {
    @Test
    public void testRequest() throws URISyntaxException {
        String t = "http://localhost:8080/fhir/Condition?code=http%3A%2F%2Ffhir.de%2FCodeSystem%2Fdimdi%2Ficd-10-gm%7CJ45.9";
        Request request = new Request(new URI(t), null);
        while (request.hasNext()) {
            FlareResource res = request.next();
            System.out.println(res.getPatientId());
        }
    }
}
