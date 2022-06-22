package de.rwth.imi.flare.api.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by Mark Hellmonds on 7/4/2022.
 */
@Data
@XmlRootElement(name = "query")
@XmlAccessorType(XmlAccessType.FIELD)
@AllArgsConstructor
@NoArgsConstructor

public class QueryExpanded {
    private List<CriteriaGroup> inclusionCriteria;
    private List<List<CriteriaGroup>> exclusionCriteria;
}
