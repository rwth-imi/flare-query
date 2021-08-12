package de.rwth.imi.flare.api.model.xml;

import de.rwth.imi.flare.api.model.Criterion;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Lukas Szimtenings on 6/25/2021.
 */
@Data
@XmlType(name = "criteriaGroup")
@XmlAccessorType(XmlAccessType.FIELD)
@AllArgsConstructor
@NoArgsConstructor
public class CriteriaGroup
{
    @XmlElement(name = "criterion")
    private Criterion[] criteria;
}
