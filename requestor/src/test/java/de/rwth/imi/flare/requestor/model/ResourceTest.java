package de.rwth.imi.flare.requestor.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceTest {

    public static final String ID = "id-130803";
    public static final String PATIENT_REF_ID = "id-133158";
    public static final String PATIENT_REF = "Patient/" + PATIENT_REF_ID;
    public static final String SUBJECT_REF_ID = "id-133313";
    public static final String SUBJECT_REF = "Subject/" + SUBJECT_REF_ID;

    @Test
    void patientId_fromId() {
        var resource = new Resource(ID, null, null).flareResource();

        String patientId = resource.getPatientId();

        assertThat(patientId).isEqualTo(ID);
    }

    @Test
    void patientId_fromPatientRef() {
        var resource = new Resource(null, new Reference(PATIENT_REF), null).flareResource();

        String patientId = resource.getPatientId();

        assertThat(patientId).isEqualTo(PATIENT_REF_ID);
    }

    @Test
    void patientId_fromSubjectRef() {
        var resource = new Resource(null, null, new Reference(SUBJECT_REF)).flareResource();

        String patientId = resource.getPatientId();

        assertThat(patientId).isEqualTo(SUBJECT_REF_ID);
    }
}
