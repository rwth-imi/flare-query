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
    private FilterType filter;
    private TerminologyCode[] selectedConcepts;
    private Comparator comparator;
    private Double value;
    private TerminologyCode unit;
    private Double minValue;
    private Double maxValue;
}
