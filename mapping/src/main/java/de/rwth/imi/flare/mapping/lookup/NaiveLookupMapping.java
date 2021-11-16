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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class NaiveLookupMapping implements FhirResourceMapper {
    Map<TerminologyCode, SourceMappingEntry> lookupTable;
    QueryExpander queryExpander;

    public NaiveLookupMapping() throws IOException {
        InputStream lookupTableStream = this.getClass().getClassLoader().getResourceAsStream("codex-term-code-mapping.json");
        initLookupTable(lookupTableStream);
        queryExpander = new QueryExpander();
    }

    public NaiveLookupMapping(InputStream lookupTable) throws IOException {
        initLookupTable(lookupTable);
    }

    private void initLookupTable(InputStream lookupTable) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<SourceMappingEntry> sourceMappingEntries = objectMapper.readValue(lookupTable, new TypeReference<>() {});

        this.lookupTable = new HashMap<>();
        sourceMappingEntries.forEach(sourceMappingEntry -> this.lookupTable.put(sourceMappingEntry.getKey(), sourceMappingEntry));
    }

    @Override
    public CompletableFuture<Query> mapResources(Query query) {
        queryExpander.expandQuery(query);
        this.mapCriterionGroup(query.getExclusionCriteria());
        this.mapCriterionGroup(query.getInclusionCriteria());
        return CompletableFuture.completedFuture(query);
    }

    private void mapCriterionGroup(List<CriteriaGroup> exclusionCriteria) {
        if(exclusionCriteria == null){
            return;
        }
        exclusionCriteria
                .forEach(criteriaSubGroup -> criteriaSubGroup.getCriteria()
                        .forEach(criterion -> criterion.setMapping(lookupCriterion(criterion))));
    }

    public MappingEntry lookupCriterion(Criterion criterion){
        TerminologyCode termCode = criterion.getTermCode();

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
