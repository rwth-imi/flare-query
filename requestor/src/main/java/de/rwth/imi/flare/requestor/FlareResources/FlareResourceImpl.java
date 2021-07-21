package de.rwth.imi.flare.requestor.FlareResources;

import de.rwth.imi.flare.api.FlareResource;
import org.hl7.fhir.r4.model.Resource;

public class FlareResourceImpl implements FlareResource {
    private String patientId;
    private Resource underlyingFhirResource;

    public FlareResourceImpl(Resource resource){
        this.underlyingFhirResource = resource;
        this.extractId();
    }

    private void extractId() {
        // TODO implement
    }

    @Override
    public String getPatientId() {
        return this.patientId;
    }
}
