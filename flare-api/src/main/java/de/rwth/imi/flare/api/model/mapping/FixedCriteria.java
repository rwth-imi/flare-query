package de.rwth.imi.flare.api.model.mapping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FixedCriteria {
    private String fhirPath;
    private String status;
    private String type;
    private String code;
    private String display;
    private String system;
}
