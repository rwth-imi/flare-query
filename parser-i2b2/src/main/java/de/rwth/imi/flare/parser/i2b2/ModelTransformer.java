package de.rwth.imi.flare.parser.i2b2;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Objects;

/**
 * Created by Lukas Szimtenings on 6/18/2021.
 * Uses a XSL transformation to transform a standard i2b2 query definition into an XML representation of a
 * {@link de.rwth.imi.flare.api.model.Query Query}
 */
public class ModelTransformer
{
    private final Transformer transformer;

    /**
     * Sets up a transformer based on the <a href="file:../../resources/i2b2_to_internal.xslt>xslt</a>
     * @throws TransformerConfigurationException if the xslt is broken
     */
    public ModelTransformer() throws TransformerConfigurationException
    {
        StreamSource styleSource = this.readResourceIntoStream("i2b2_to_internal.xslt");
        TransformerFactory factory = TransformerFactory.newInstance();
        // Disable Access to local files to avoid file disclosures or SSRF
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        this.transformer = factory.newTransformer(styleSource);
    }

    /**
     * transforms an i2b2 query definition into an XML representation of a
     * {@link de.rwth.imi.flare.api.model.Query Query}
     * @param xml i2b2 query definition
     * @return XML representation of {@link de.rwth.imi.flare.api.model.Query Query}
     * @throws TransformerException if the given xml is not valid
     */
    public String transform(String xml) throws TransformerException
    {
        StreamResult outputTarget = new StreamResult(new StringWriter());
        this.transformer.transform(new StreamSource(new StringReader(xml)), outputTarget);
        return outputTarget.getWriter().toString();
    }

    /**
     * Helper method, reads file from resources and creates a StreamSource from it
     */
    public StreamSource readResourceIntoStream(String resourcePath){
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        Objects.requireNonNull(resourceAsStream, "Resource cannot be found");
        return new StreamSource(resourceAsStream);
    }
}
