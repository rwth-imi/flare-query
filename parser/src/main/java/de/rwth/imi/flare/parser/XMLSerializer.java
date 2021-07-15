package de.rwth.imi.flare.parser;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

import de.rwth.imi.flare.api.model.*;

/**
 * Created by Lukas Szimtenings on 6/18/2021.
 */
public class XMLSerializer
{
    private final JAXBContext jc;
    private final Unmarshaller unmarshaller;
    private final Marshaller marshaller;
    
    public XMLSerializer() throws JAXBException
    {
        this.jc = JAXBContext.newInstance(Comparator.class, CriteriaGroup.class, Criterion.class,
                FilterType.class, Query.class, TerminologyCode.class, ValueFilter.class);
        this.unmarshaller = this.jc.createUnmarshaller();
        this.marshaller = this.jc.createMarshaller();
        this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    }
    
    /**
     * Deserializes a Query object from it's xml representation
     *
     * @param xml XML-representation of a Query object
     * @return Query-Object
     * @throws JAXBException when something goes wrong during unmarshalling
     * @see StringReader , Unmarshaller.unmarshall()
     */
    Query xmlStringToQuery(String xml) throws JAXBException {
        return (Query) unmarshaller.unmarshal(new StringReader(xml));
    }
    
    public String QueryToXmlString(Query query) throws JAXBException
    {
        StringWriter strWriter = new StringWriter();
        marshaller.marshal(query, strWriter);
        return strWriter.toString();
    }
}
