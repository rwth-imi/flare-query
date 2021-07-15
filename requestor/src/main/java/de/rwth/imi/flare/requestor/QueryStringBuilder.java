package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.TerminologyCode;
import de.rwth.imi.flare.api.model.FilterType;
import de.rwth.imi.flare.api.model.ValueFilter;
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
        this.builder.append(mapping.getFhirResourceType()).append('?');
        if(this.criterion.getValueFilter() != null){
            appendValueFilterByType();
        }
        // Search for concepts
        else{
            appendConceptFilterString();
        }

        if(mapping.getFixedCriteria() != null){

        }

        return builder.toString();
    }

    private void appendConceptFilterString() {
        MappingEntry mapping = this.criterion.getValueFilter().getMapping();
        // TODO: Tree expansion if necessary
        TerminologyCode termCode = this.criterion.getTermCode();
        this.builder.append(mapping.getTermCodeSearchParameter())
                .append('=')
                .append(termCode.getCode()).append('|').append(termCode.getSystem());
    }

    private void appendValueFilterByType() {
        MappingEntry mapping = this.criterion.getValueFilter().getMapping();

        if(mapping.getTermCodeSearchParameter() != null){
            TerminologyCode termCode = this.criterion.getTermCode();
            builder.append(mapping.getTermCodeSearchParameter())
                    .append('=')
                    .append(termCode.getSystem()).append('|').append(termCode.getCode());
        }
        FilterType filter = this.criterion.getValueFilter().getFilter();
        if (filter == FilterType.QUANTITY_COMPARATOR){
            appendQuantityComparatorFilterString();
        }
        else if (filter == FilterType.QUANTITY_RANGE){
            appendQuantityRangeFilterString();
        }
        //TODO: Implement concept filter
    }

    private void appendQuantityRangeFilterString() {
        ValueFilter valueFilter = this.criterion.getValueFilter();
        MappingEntry mapping = valueFilter.getMapping();
        String valueSearchParameter = mapping.getValueSearchParameter();
        builder.append(valueSearchParameter).append("=ge").append(valueFilter.getMinValue()).append(valueFilter.getUnit());
        builder.append('&');
        builder.append(valueSearchParameter).append("=le").append(valueFilter.getMaxValue()).append(valueFilter.getUnit());
    }

    private void appendQuantityComparatorFilterString() {
        ValueFilter valueFilter = this.criterion.getValueFilter();
        String valueSearchParameter = this.criterion.getValueFilter().getMapping().getValueSearchParameter();
        this.builder.append(valueSearchParameter).append('=')
                .append(valueFilter.getComparator()).append(valueFilter.getValue()).append(valueFilter.getUnit());
    }
}
