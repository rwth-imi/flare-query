package de.rwth.imi.flare.api.model.mapping;

import de.rwth.imi.flare.api.model.TerminologyCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FixedCriteria {
    private String fhirPath;
    private String status;
    private String type;
    private List<TerminologyCode> value;
    private String searchParameter;
}
