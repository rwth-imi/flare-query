package de.rwth.imi.flare.parser;

import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.api.model.util;
import de.rwth.imi.flare.api.FlareParser;

import java.io.IOException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Created by Lukas Szimtenings on 6/4/2021.
 */
public class I2b2Parser implements FlareParser
{
    private final ModelTransformer transformer;

    public I2b2Parser() throws TransformerConfigurationException
    {
        this.transformer = new ModelTransformer();
    }
    
    @Override
    public Query parse(String input) throws IOException
    {
        String transformedXml;
        try
        {
            transformedXml = transformer.transform(input);
            System.out.println(transformedXml);
            return util.deserialize(transformedXml);

        } catch (TransformerException e)
        {
            throw new IOException(e);
        }
    }
}
