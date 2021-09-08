package de.rwth.imi.flare.parser.csq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import de.rwth.imi.flare.api.FlareParser;
import de.rwth.imi.flare.api.model.Query;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ParserCSQ implements FlareParser {
    @Override
    public Query parse(String input) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode rootNode = objectMapper.readTree(input);

        //TODO: properly fix not having implemented the correct query format
        if(rootNode.has("query")) {
            JsonNode queryNode = rootNode.get("query");
            input = objectMapper.writeValueAsString(queryNode);
        }
        return objectMapper.readValue(input, Query.class);
    }
}
