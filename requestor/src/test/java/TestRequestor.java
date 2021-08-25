import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.requestor.Request;
import org.junit.jupiter.api.Test;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;

public class TestRequestor {
    @Test
    public void testRequest() throws URISyntaxException {
        String t = "https://localhost:9443/fhir-server/api/v4/Observation?code=I_COVAS_COV_M030_LAB_PARA_Q040&value-quantity=lt40";
        Request request = new Request(new URI(t), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        "fhiruser",
                        "change-password".toCharArray());
            }
        });
        while (request.hasNext()) {
            FlareResource res = request.next();
            System.out.println(res.getPatientId());

        }
    }
}
