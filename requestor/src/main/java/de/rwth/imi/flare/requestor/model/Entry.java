package de.rwth.imi.flare.requestor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Entry(Resource resource) {
}
