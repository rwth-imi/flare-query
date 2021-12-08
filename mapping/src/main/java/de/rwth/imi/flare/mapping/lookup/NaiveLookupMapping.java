package de.rwth.imi.flare.mapping.lookup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.rwth.imi.flare.api.FhirResourceMapper;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.api.model.TerminologyCode;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import de.rwth.imi.flare.mapping.expansion.QueryExpander;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implements a naive lookup strategy, that parses a JSON file following
 * {@see <a href="https://github.com/num-codex/codex-gecco-to-ui-profiles/blob/main/schema/term-code-mapping-schema.json">the codex mapping schema<a>}
 *
 *  Attaches the {@link de.rwth.imi.flare.api.model.mapping.MappingEntry corresponding MappingEntry} to each
 *  {@link de.rwth.imi.flare.api.model.Criterion Criterion} by using the
 *  {@link de.rwth.imi.flare.api.model.Criterion#getTermCode() Criterions termCode} as a key when looking for the mapping.
 */

public class NaiveLookupMapping implements FhirResourceMapper {
    Map<TerminologyCode, SourceMappingEntry> lookupTable;
    QueryExpander queryExpander;

    public NaiveLookupMapping(Map<TerminologyCode, SourceMappingEntry> lookupTable, QueryExpander queryExpander) throws IOException {
        this.lookupTable = lookupTable;
        this.queryExpander = queryExpander;
        //queryExpander = new QueryExpander(conceptTreeFile);
    }



    /*public NaiveLookupMapping(InputStream lookupTable) throws IOException {
        initLookupTable(lookupTable);
    }*/

    /*
    private void initLookupTable(InputStream lookupTable) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<SourceMappingEntry> sourceMappingEntries = objectMapper.readValue(lookupTable, new TypeReference<>() {});


        this.lookupTable = new HashMap<>();
        sourceMappingEntries.forEach(sourceMappingEntry -> this.lookupTable.put(sourceMappingEntry.getKey(), sourceMappingEntry));
    }
    */

    @Override
    public CompletableFuture<Query> mapResources(Query query) {
        queryExpander.expandQuery(query);
        this.mapCriterionGroup(query.getExclusionCriteria());
        this.mapCriterionGroup(query.getInclusionCriteria());
        return CompletableFuture.completedFuture(query);
    }

    private void mapCriterionGroup(List<CriteriaGroup> criterionGroup) {
        if(criterionGroup == null){
            return;
        }
        criterionGroup
                .forEach(criteriaSubGroup -> criteriaSubGroup.getCriteria()
                        .forEach(criterion -> criterion.setMapping(lookupCriterion(criterion))));
    }

    public MappingEntry lookupCriterion(Criterion criterion){
        TerminologyCode termCode = criterion.getTermCode().get(0);

        if(termCode == null){
            return null;
        }

        SourceMappingEntry mappingEntry = this.lookupTable.get(termCode);
        if(mappingEntry == null){
            throw new NoSuchElementException("No mapping entry for: " + termCode);
        }
        return mappingEntry;
    }
}
