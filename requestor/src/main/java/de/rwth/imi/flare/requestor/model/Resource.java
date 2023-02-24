package de.rwth.imi.flare.requestor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import de.rwth.imi.flare.api.FlareResource;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Resource(String id, Reference patient, Reference subject) {

    public FlareResource flareResource() {
        return () -> patient != null ? patient.id() : subject != null ? subject.id() : id;
    }
}
