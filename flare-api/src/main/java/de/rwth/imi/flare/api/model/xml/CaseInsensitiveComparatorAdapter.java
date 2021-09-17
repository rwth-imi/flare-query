package de.rwth.imi.flare.api.model.xml;

import de.rwth.imi.flare.api.model.Comparator;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class CaseInsensitiveComparatorAdapter extends XmlAdapter<String, Comparator>
{
    @Override
    public Comparator unmarshal(String v) {
        return Comparator.valueOf(v.toUpperCase().trim());
    }

    @Override
    public String marshal(Comparator v) {
        return v.name();
    }
}