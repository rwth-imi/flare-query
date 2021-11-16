package de.rwth.imi.flare.parser.csq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import de.rwth.imi.flare.api.FlareParser;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.Query;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ParserCSQ implements FlareParser {
    @Override
    public Query parse(String input) throws JsonProcessingException {
        CSQQuery csqQuery = parseCSQQuery(input);
        return transformIntoQuery(csqQuery);
    }

    /**
     * Parses a Codex structured query input into a parser internal CSQQuery Object
     * @param input String containing a json formatted CSQ
     * @return parser internal CSQQuery Object
     */
    private CSQQuery parseCSQQuery(String input) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
        return objectMapper.readValue(input, CSQQuery.class);
    }

    /**
     * Creates a Standard FLARE Query object from a given CSQQuery Object
     * @param csqQuery given Query
     * @return Standard FLARE Query object
     */
    private Query transformIntoQuery(CSQQuery csqQuery){
        Query query = new Query();
        query.setExclusionCriteria(createCriteriaGroupsFromCriteriaLists(csqQuery.getExclusionCriteria()));
        query.setInclusionCriteria(createCriteriaGroupsFromCriteriaLists(csqQuery.getInclusionCriteria()));
        return query;
    }

    /**
     * Takes either the entire InclusionCriteria oder ExclusionCriteria list and converts it into a List of CriteriaGroups
     */
    private List<CriteriaGroup> createCriteriaGroupsFromCriteriaLists(List<List<Criterion>> criteriaLists) {
        if(criteriaLists==null){
            return new ArrayList<>();
        }
        List<CriteriaGroup> criteriaGroupList = new LinkedList<>();
        for(List<Criterion> criteriaList: criteriaLists){
            criteriaGroupList.add(new CriteriaGroup(criteriaList));
        }
        return criteriaGroupList;
    }
}
