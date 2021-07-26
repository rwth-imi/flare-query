package de.rwth.imi.flare.requestor;

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
        switch (resourceType) {
            case Observation -> this.patientId = extractId((Observation) this.underlyingFhirResource);
            case Patient -> this.patientId = extractId((Patient) this.underlyingFhirResource);
            case Condition -> this.patientId = extractId((Condition) this.underlyingFhirResource);
            case Specimen -> this.patientId = extractId((Specimen) this.underlyingFhirResource);
            case Encounter -> this.patientId = extractId((Encounter) this.underlyingFhirResource);
        }
    }

    private String extractId(Specimen specimen) {
        return specimen.getSubject().getIdentifier().getValue();
    }

    private String extractId(Encounter encounter) {
        return encounter.getSubject().getIdentifier().getValue();
    }

    private String extractId(Condition condition) {
        return condition.getSubject().getIdentifier().getValue();
    }

    private String extractId(Patient patient) {
        return patient.getIdentifierFirstRep().getValue();
    }

    private String extractId(Observation observation) {
        return observation.getSubject().getIdentifier().getValue();
    }

    @Override
    public String getPatientId() {
        return this.patientId;
    }
}
