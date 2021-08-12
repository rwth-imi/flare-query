package de.rwth.imi.flare.parser;

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
 */
public class ModelTransformer
{
    private final Transformer transformer;
    
    public ModelTransformer() throws TransformerConfigurationException
    {
        StreamSource styleSource = this.readResourceIntoStream("i2b2_to_internal.xslt");
        TransformerFactory factory = TransformerFactory.newInstance();
        this.transformer = factory.newTransformer(styleSource);
    }
    
    public String transform(String xml) throws TransformerException
    {
        StreamResult outputTarget = new StreamResult(new StringWriter());
        this.transformer.transform(new StreamSource(new StringReader(xml)), outputTarget);
        return outputTarget.getWriter().toString();
    }
    
    public StreamSource readResourceIntoStream(String resourcePath){
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        Objects.requireNonNull(resourceAsStream, "Resource cannot be found");
        return new StreamSource(resourceAsStream);
    }
}
