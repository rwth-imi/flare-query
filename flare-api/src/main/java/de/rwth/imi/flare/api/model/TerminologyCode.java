package de.rwth.imi.flare.api.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Created by Lukas Szimtenings on 5/28/2021.
 */
@Data
@XmlType(name = "terminologyCode")
@XmlAccessorType(XmlAccessType.FIELD)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TerminologyCode
{
    private String code;
    private String system;
    @EqualsAndHashCode.Exclude
    private String display;
}
