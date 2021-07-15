package de.rwth.imi.flare.parser;

import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.api.FlareParser;
import jakarta.xml.bind.JAXBException;

import java.io.IOException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Created by Lukas Szimtenings on 6/4/2021.
 */
public class I2b2Parser implements FlareParser
{
    private final ModelTransformer transformer;
    private final XMLSerializer xmlSerializer;
    
    public I2b2Parser() throws TransformerConfigurationException, JAXBException
    {
        this.transformer = new ModelTransformer();
        this.xmlSerializer = new XMLSerializer();
    }
    
    @Override
    public Query parse(String input) throws IOException
    {
        String transformedXml;
        try
        {
            transformedXml = transformer.transform(input);
            System.out.println(transformedXml);
            return this.xmlSerializer.xmlStringToQuery(transformedXml);

        } catch (TransformerException | JAXBException e)
        {
            throw new IOException(e);
        }
    }
}
