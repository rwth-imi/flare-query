package de.rwth.imi.flare.mapping.lookup;

import de.rwth.imi.flare.api.FhirResourceMapper;
import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import de.rwth.imi.flare.mapping.expansion.QueryExpander;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;


/**
 * Implements a naive lookup strategy, that parses a JSON file following
 * {@see <a href="https://github.com/num-codex/codex-gecco-to-ui-profiles/blob/main/schema/term-code-mapping-schema.json">the codex mapping schema<a>}
 *
 *  Attaches the {@link de.rwth.imi.flare.api.model.mapping.MappingEntry corresponding MappingEntry} to each
 *  {@link de.rwth.imi.flare.api.model.Criterion Criterion} by using the
 *  {@link de.rwth.imi.flare.api.model.Criterion#getTermCodes() Criterions termCode} as a key when looking for the mapping.
 */

public class NaiveLookupMapping implements FhirResourceMapper {
    Map<TerminologyCode, SourceMappingEntry> lookupTable;
    QueryExpander queryExpander;

    public NaiveLookupMapping(Map<TerminologyCode, SourceMappingEntry> lookupTable, QueryExpander queryExpander) throws IOException {
        this.lookupTable = lookupTable;
        this.queryExpander = queryExpander;
    }




    @Override
    public CompletableFuture<QueryExpanded> mapResources(Query query) {
        QueryExpanded queryExpanded = queryExpander.expandQuery(query);
        for (List<CriteriaGroup> criteriaGroups: queryExpanded.getExclusionCriteria()){
            this.mapCriterionGroup(criteriaGroups);
        }
        this.mapCriterionGroup(queryExpanded.getInclusionCriteria());
        return CompletableFuture.completedFuture(queryExpanded);
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
        TerminologyCode termCode = criterion.getTermCodes().get(0);

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
