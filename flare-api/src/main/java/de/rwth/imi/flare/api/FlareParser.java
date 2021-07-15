package de.rwth.imi.flare.api;

import de.rwth.imi.flare.api.model.Query;
import jakarta.xml.bind.JAXBException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

/**
 * Created by Lukas Szimtenings on 6/2/2021.
 */
public interface FlareParser
{
    Query parse(String input) throws TransformerException, JAXBException, IOException;
}
