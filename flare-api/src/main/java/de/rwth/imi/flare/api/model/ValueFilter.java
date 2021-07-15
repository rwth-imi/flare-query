package de.rwth.imi.flare.api.model;

import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Lukas Szimtenings on 5/28/2021.
 */
@Data
@XmlType(name = "valueFilter")
@XmlAccessorType(XmlAccessType.FIELD)
@AllArgsConstructor
@NoArgsConstructor
public class ValueFilter
{
    @XmlElement(name = "filter")
    private FilterType filter;
    @XmlElement(name = "selectedConcepts")
    private TerminologyCode[] selectedConcepts;
    @XmlElement(name = "comparator")
    private Comparator comparator;
    @XmlElement(name = "value")
    private Double value;
    @XmlElement(name = "unit")
    private String unit;
    @XmlElement(name = "minValue")
    private Double minValue;
    @XmlElement(name = "maxValue")
    private Double maxValue;
    private MappingEntry mapping;
}
