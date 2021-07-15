package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.FilterType;
import de.rwth.imi.flare.api.model.TerminologyCode;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;

public class QueryStringBuilder {
    private final Criterion criterion;
    private final StringBuilder builder;

    public static String constructQueryString(Criterion searchCriterion){
        QueryStringBuilder builder = new QueryStringBuilder(searchCriterion);
        return builder.constructQueryString();
    }

    private QueryStringBuilder(Criterion searchCriterion){
        this.builder = new StringBuilder();
        this.criterion = searchCriterion;
    }

    private String constructQueryString(){
        MappingEntry mapping = this.criterion.getValueFilter().getMapping();
        builder.append(mapping.getFhirResourceType()).append('?');

        if(mapping.getTermCodeSearchParameter() != null){
            TerminologyCode termCode = this.criterion.getTermCode();
            builder.append(mapping.getTermCodeSearchParameter())
                    .append('=')
                    .append(termCode.getSystem()).append('|').append(termCode.getCode());
            constructConceptSearchString();
        }
        else{
            constructValueSearchString();
        }
        return builder.toString();
    }

    private void constructValueSearchString() {

    }

    private void constructConceptSearchString() {
        builder.append()
    }
}
