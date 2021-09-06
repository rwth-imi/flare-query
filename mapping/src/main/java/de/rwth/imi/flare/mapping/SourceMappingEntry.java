package de.rwth.imi.flare.mapping;

import de.rwth.imi.flare.api.model.TerminologyCode;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;

public class SourceMappingEntry extends MappingEntry {
    private TerminologyCode key;

    public TerminologyCode getKey() {
        return key;
    }
    public void setKey(TerminologyCode key) {
        this.key = key;
    }
}
