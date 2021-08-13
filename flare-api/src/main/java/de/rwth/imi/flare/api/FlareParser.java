package de.rwth.imi.flare.api;

import de.rwth.imi.flare.api.model.Query;
import java.io.IOException;

/**
 * Parses all information contained in a formatted query into the internal {@link Query} representation
 */
public interface FlareParser
{
    Query parse(String input) throws IOException;
}
