package de.rwth.imi.flare.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Created by Lukas Szimtenings on 5/28/2021.
 */
@XmlType(name = "filterType")
@XmlEnum
public enum FilterType
{
    @XmlEnumValue("concept")
    @JsonProperty("concept")
    CONCEPT,
    @JsonProperty("quantity-comparator")
    @XmlEnumValue("quantity-comparator")
    QUANTITY_COMPARATOR,
    @JsonProperty("quantity-range")
    @XmlEnumValue("quantity-range")
    QUANTITY_RANGE
}
