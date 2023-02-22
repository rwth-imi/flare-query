package de.rwth.imi.flare.requestor;

import java.util.Set;

public record CacheValue(Set<String> patientIds) {

    public CacheValue {
        patientIds = Set.copyOf(patientIds);
    }

    public CacheValue() {
        this(Set.of());
    }
}
