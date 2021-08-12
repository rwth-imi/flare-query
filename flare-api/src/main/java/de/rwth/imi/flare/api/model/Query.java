package de.rwth.imi.flare.api.model;

import de.rwth.imi.flare.api.model.xml.CriteriaGroupAdapter;
import jakarta.xml.bind.annotation.*;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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
    @XmlJavaTypeAdapter(CriteriaGroupAdapter.class)
    private Criterion[][] inclusionCriteria;
    @XmlJavaTypeAdapter(CriteriaGroupAdapter.class)
    private Criterion[][] exclusionCriteria;
}
