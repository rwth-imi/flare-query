package de.rwth.imi.flare.api.model;

import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

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
    private List<Criterion> criteria;
}
