package de.rwth.imi.flare.parser.i2b2;

import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.api.model.util;
import de.rwth.imi.flare.api.FlareParser;

import java.io.IOException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Created by Lukas Szimtenings on 6/4/2021.
 * Parses i2b2 Queries into the internal data format
 */
public class ParserI2B2 implements FlareParser
{
    private final ModelTransformer transformer;

    public ParserI2B2() throws TransformerConfigurationException
    {
        this.transformer = new ModelTransformer();
    }

    /**
     * Parses an i2b2 query definition into a {@link Query}
     * @param input i2b2 Query definition as String
     * @return Unmapped {@link Query} corresponding to the {@code input}
     * @throws IOException thrown if Input could not be transformed correctly
     */
    @Override
    public Query parse(String input) throws IOException
    {
        String transformedXml;
        try
        {
            transformedXml = transformer.transform(input);
            return util.deserialize(transformedXml);

        } catch (TransformerException e)
        {
            throw new IOException(e);
        }
    }
}
