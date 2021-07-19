package de.rwth.imi.flare.api.model;

import jakarta.xml.bind.JAXB;

import java.io.StringWriter;

public class util {
    public static String serialize(Query query){
        StringWriter xml = new StringWriter();
        JAXB.marshal(query, xml);
        return xml.toString();
    }

    public static Query deserialize(String xml){
        return JAXB.unmarshal(xml, Query.class);
    }
}
