package de.rwth.imi.flare.api.model;

import jakarta.xml.bind.JAXB;

import java.io.StringReader;
import java.io.StringWriter;

public class util {
    public static String serialize(Query query){
        StringWriter xml = new StringWriter();
        JAXB.marshal(query, xml);
        return xml.toString();
    }

    public static Query deserialize(String xml){
        return JAXB.unmarshal(new StringReader(xml), Query.class);
    }
}
