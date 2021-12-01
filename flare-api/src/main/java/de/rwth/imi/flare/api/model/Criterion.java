package de.rwth.imi.flare.api.model;

import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by Lukas Szimtenings on 5/28/2021.
 */
@XmlType(name = "criterion")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class Criterion
{
    private List<TerminologyCode> termCode;
    private ValueFilter valueFilter;
    private MappingEntry mapping;
    private List<AttributeFilter> attributeFilters;
    private TimeRestriction timeRestriction;
}
