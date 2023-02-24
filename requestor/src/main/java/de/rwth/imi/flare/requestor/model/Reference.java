package de.rwth.imi.flare.requestor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Reference(String reference) {

    public Reference {
        Objects.requireNonNull(reference);
    }

    public String id() {
        return reference.substring(reference.indexOf('/') + 1);
    }
}
