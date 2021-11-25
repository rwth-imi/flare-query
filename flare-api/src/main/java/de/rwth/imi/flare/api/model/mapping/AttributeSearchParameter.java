package de.rwth.imi.flare.api.model.mapping;

import de.rwth.imi.flare.api.model.TerminologyCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttributeSearchParameter {
    private String attributeFhirPath;
    private TerminologyCode attributeKey;
    private String attributeType;
    private String attributeSearchParameter;
}
