package de.rwth.imi.flare.mapping.lookup;

import de.rwth.imi.flare.api.model.TerminologyCode;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;

/**
 * Class exists to abstract attributes that are a part of the mapping json file,
 * but are not necessarily needed when the mapping is used to craft a query.
 */
public class SourceMappingEntry extends MappingEntry {
    /**
     * Key used to find the mapping, not required once attached to a specific Criterion
     */
    private TerminologyCode key;

    public TerminologyCode getKey() {
        return key;
    }
    public void setKey(TerminologyCode key) {
        this.key = key;
    }
}
