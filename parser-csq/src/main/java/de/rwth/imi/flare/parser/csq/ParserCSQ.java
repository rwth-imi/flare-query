package de.rwth.imi.flare.parser.csq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import de.rwth.imi.flare.api.FlareParser;
import de.rwth.imi.flare.api.model.Query;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ParserCSQ implements FlareParser {
    @Override
    public Query parse(String input) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(input, Query.class);
    }
}
