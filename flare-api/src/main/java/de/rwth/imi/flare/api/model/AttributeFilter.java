package de.rwth.imi.flare.api.model;

import de.rwth.imi.flare.api.model.xml.CaseInsensitiveComparatorAdapter;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@XmlType(name = "attributeFilter")
@XmlAccessorType(XmlAccessType.FIELD)
@AllArgsConstructor
@NoArgsConstructor
public class AttributeFilter {
    private FilterType type;
    private TerminologyCode attributeCode;
    private List<TerminologyCode> selectedConcepts;
    @XmlJavaTypeAdapter(CaseInsensitiveComparatorAdapter.class)
    private Comparator comparator;
    private Double value;
    private TerminologyCode unit;
    private Double minValue;
    private Double maxValue;
}
