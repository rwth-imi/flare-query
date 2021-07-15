package de.rwth.imi.flare.api.model.mapping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MappingEntry {
    private String fhirResourceType;
    private String termCodeSearchParameter;
    private String valueSearchParameter;
    private FixedCriteria[] fixedCriteria;
}
