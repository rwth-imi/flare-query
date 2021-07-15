package de.rwth.imi.flare.api.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Created by Lukas Szimtenings on 5/28/2021.
 */
@XmlType(name = "filterType")
@XmlEnum
public enum FilterType
{
    CONCEPT,
    QUANTITY_COMPARATOR,
    QUANTITY_RANGE
}
