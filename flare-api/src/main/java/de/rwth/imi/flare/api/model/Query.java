package de.rwth.imi.flare.api.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Lukas Szimtenings on 6/2/2021.
 */
@Data
@XmlRootElement(name = "query")
@XmlAccessorType(XmlAccessType.FIELD)
@AllArgsConstructor
@NoArgsConstructor
public class Query
{
    private CriteriaGroup[] inclusionCriteria;
    private CriteriaGroup[] exclusionCriteria;
}
