package de.rwth.imi.flare.api.model.xml;

import de.rwth.imi.flare.api.model.Comparator;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.util.Locale;

public class CaseInsensitiveComparatorAdapter extends XmlAdapter<String, Comparator>
{
    @Override
    public Comparator unmarshal(String v) {
        Comparator comparator;
        try{
            comparator = Comparator.valueOf(v.toUpperCase().trim());
        }
        catch (IllegalArgumentException ignore){
            comparator = Comparator.valueOf(v.toLowerCase().trim());
        }
        return comparator;
    }

    @Override
    public String marshal(Comparator v) {
        return v.name();
    }
}