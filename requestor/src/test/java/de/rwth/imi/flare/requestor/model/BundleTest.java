package de.rwth.imi.flare.requestor.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BundleTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"entry\": null}", "{\"entry\": []}"})
    void fromJson_emptyEntry(String json) throws Exception {
        var bundle = mapper.readValue(json, Bundle.class);

        assertThat(bundle.entry()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"link\": null}", "{\"link\": []}"})
    void fromJson_emptyLink(String json) throws Exception {
        var bundle = mapper.readValue(json, Bundle.class);

        assertThat(bundle.link()).isEmpty();
    }

    @Test
    void fromJson_subjectReference() throws Exception {
        var bundle = mapper.readValue("""
                {"entry": [{
                   "resource": {
                     "id": "id-112925",
                     "subject": {
                       "reference": "Patient/id-113005"
                     }
                   }
                 }]
                }
                """, Bundle.class);

        assertThat(bundle.entry()).singleElement()
                .isEqualTo(new Entry(new Resource("id-112925", null, new Reference("Patient/id-113005"))));
    }

    @Test
    void fromJson_patientReference() throws Exception {
        var bundle = mapper.readValue("""
                {"entry": [{
                   "resource": {
                     "id": "id-135218",
                     "patient": {
                       "reference": "Patient/id-135225"
                     }
                   }
                 }]
                }
                """, Bundle.class);

        assertThat(bundle.entry()).singleElement()
                .isEqualTo(new Entry(new Resource("id-135218", new Reference("Patient/id-135225"), null)));
    }

    @Test
    void fromJson_nextLink() throws Exception {
        var bundle = mapper.readValue("""
                {"link": [{
                   "relation": "next",
                   "url": "url-151358"
                 }]
                }
                """, Bundle.class);

        assertThat(bundle.link()).singleElement()
                .isEqualTo(new Link("next", "url-151358"));
    }
}
