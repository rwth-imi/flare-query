package de.rwth.imi.flare.api.model.mapping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MappingEntry {
    private String fhirResourceType;
    private String termCodeSearchParameter;
    private String valueSearchParameter;
    private List<FixedCriteria> fixedCriteria;
    private String timeRestrictionParameter;
    private List<AttributeSearchParameter> attributeSearchParameters;
}
