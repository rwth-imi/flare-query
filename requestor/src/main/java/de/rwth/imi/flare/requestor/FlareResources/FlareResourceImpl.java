package de.rwth.imi.flare.requestor.FlareResources;

import de.rwth.imi.flare.api.FlareResource;
import org.hl7.fhir.r4.model.*;

public class FlareResourceImpl implements FlareResource {
    private String patientId;
    private final Resource underlyingFhirResource;

    public FlareResourceImpl(Resource resource){
        this.underlyingFhirResource = resource;
        this.extractId();
    }

    private void extractId() {
        ResourceType resourceType = this.underlyingFhirResource.getResourceType();
        switch(resourceType){
            case Observation:
                this.patientId = extractId((Observation)this.underlyingFhirResource);
                break;
            case Patient:
                this.patientId = extractId((Patient)this.underlyingFhirResource);
                break;
            case Condition:
                this.patientId = extractId((Condition)this.underlyingFhirResource);
                break;
            case Specimen:
                this.patientId = extractId((Specimen)this.underlyingFhirResource);
                break;
            case Encounter:
                this.patientId = extractId((Encounter)this.underlyingFhirResource);
                break;
        }
    }

    private String extractId(Specimen specimen) {
        return specimen.getSubject().getId();
    }

    private String extractId(Encounter encounter) {
        return encounter.getSubject().getId();
    }

    private String extractId(Condition condition) {
        return condition.getSubject().getId();
    }

    private String extractId(Patient patient) {
        return patient.getId();
    }

    private String extractId(Observation observation) {
        return observation.getSubject().getId();
    }

    @Override
    public String getPatientId() {
        return this.patientId;
    }
}
