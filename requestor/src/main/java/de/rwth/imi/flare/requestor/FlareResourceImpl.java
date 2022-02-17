package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.FlareResource;
import org.hl7.fhir.r4.model.*;

/**
 * FlareResource, represents a single FHIR Resource and it's associated patient
 */
public class FlareResourceImpl implements FlareResource {
    private String patientId;
    private final Resource underlyingFhirResource;

    /**
     * Constructs a FlareResource representing a given FHIR Resource
     * @param resource to base the FlareResource upon
     */
    public FlareResourceImpl(Resource resource){
        this.underlyingFhirResource = resource;
        this.extractId();
    }

    /**
     * Extracts the patientId from the {@link #underlyingFhirResource} into {@link #patientId}
     */
    private void extractId() {
        ResourceType resourceType = this.underlyingFhirResource.getResourceType();

        switch (resourceType) {
            case Observation -> this.patientId = extractId((Observation) this.underlyingFhirResource);
            case Patient -> this.patientId = extractId((Patient) this.underlyingFhirResource);
            case Condition -> this.patientId = extractId((Condition) this.underlyingFhirResource);
            case Specimen -> this.patientId = extractId((Specimen) this.underlyingFhirResource);
            case Encounter -> this.patientId = extractId((Encounter) this.underlyingFhirResource);
            case Procedure -> this.patientId = extractId((Procedure) this.underlyingFhirResource);
            case MedicationAdministration -> this.patientId = extractId((MedicationAdministration) this.underlyingFhirResource);
            case MedicationStatement -> this.patientId = extractId((MedicationStatement) this.underlyingFhirResource);
            case Immunization -> this.patientId = extractId((Immunization) this.underlyingFhirResource);
            case Consent -> this.patientId = extractId((Consent) this.underlyingFhirResource);
            case DiagnosticReport -> this.patientId = extractId((DiagnosticReport) this.underlyingFhirResource);

        }
    }

    private String extractId(Specimen specimen) {
        return specimen.getSubject().getReference();
    }

    private String extractId(Encounter encounter) {
        return encounter.getSubject().getReference();
    }

    private String extractId(Condition condition) {
        return condition.getSubject().getReference();
    }

    private String extractId(Patient patient) {
        return patient.getId();
    }

    private String extractId(Procedure patient) {
        return patient.getSubject().getReference();
    }

    private String extractId(Observation observation) {
        return observation.getSubject().getReference();
    }

    private String extractId(MedicationAdministration medicationAdministration) {
        return medicationAdministration.getSubject().getReference();
    }

    private String extractId(MedicationStatement medicationStatement) {
        return medicationStatement.getSubject().getReference();
    }

    private String extractId(Immunization immunization) {
        return immunization.getPatient().getReference();
    }

    private String extractId(Consent consent) {
        return consent.getPatient().getReference();
    }

    private String extractId(DiagnosticReport diagnosticReport) {
        return diagnosticReport.getSubject().getReference();
    }

    @Override
    public String getPatientId() {
        return this.patientId;
    }
}
