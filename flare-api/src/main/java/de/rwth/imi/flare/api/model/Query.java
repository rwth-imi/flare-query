package de.rwth.imi.flare.api.model;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by Lukas Szimtenings on 6/2/2021.
 */
@Data
@XmlRootElement(name = "query")
@XmlAccessorType(XmlAccessType.FIELD)
@AllArgsConstructor
@NoArgsConstructor
public class Query {

    private List<CriteriaGroup> inclusionCriteria;
    private List<CriteriaGroup> exclusionCriteria;
}
