package de.rwth.imi.flare.api.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Created by Lukas Szimtenings on 5/28/2021.
 */
@XmlType(name = "ComparatorType")
@XmlEnum()
public enum Comparator
{
    gt,
    ge,
    lt,
    le,
    eq,
    ne
}
