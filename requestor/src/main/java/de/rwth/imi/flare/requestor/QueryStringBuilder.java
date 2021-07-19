package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.TerminologyCode;
import de.rwth.imi.flare.api.model.FilterType;
import de.rwth.imi.flare.api.model.ValueFilter;
import de.rwth.imi.flare.api.model.mapping.FixedCriteria;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class QueryStringBuilder {
    private final Criterion criterion;
    private final StringBuilder sb;

    public static String constructQueryString(Criterion searchCriterion){
        QueryStringBuilder builder = new QueryStringBuilder(searchCriterion);
        return builder.constructQueryString();
    }

    private QueryStringBuilder(Criterion searchCriterion){
        this.sb = new StringBuilder();
        this.criterion = searchCriterion;
    }

    private String constructQueryString(){
        MappingEntry mapping = this.criterion.getMapping();
        this.sb.append(mapping.getFhirResourceType()).append('?');

        if(this.criterion.getValueFilter() != null){
            appendValueFilterByType();
        }
        else{
            appendConceptFilterString();
        }

        if(mapping.getFixedCriteria() != null){
            appendFixedCriteriaString();
        }

        return sb.toString();
    }

    private void appendFixedCriteriaString() {
        for (FixedCriteria criterion : this.criterion.getMapping().getFixedCriteria()){
            List<String> encodedCriteriaValueList = new LinkedList<>();

            // Join all values into one string
            StringBuilder sbTemp = new StringBuilder();
            for(TerminologyCode value : criterion.getValue()){
                sbTemp.append(value.getCode()).append('|').append(value.getSystem());
                encodedCriteriaValueList.add(urlEncodeAndReset(sbTemp));
            }
            String valueString = String.join(",", encodedCriteriaValueList);

            this.sb.append('&').append(criterion.getSearchParameter()).append('=').append(valueString);
        }
    }

    private void appendConceptFilterString() {
        MappingEntry mapping = this.criterion.getMapping();
        StringBuilder sbTemp = new StringBuilder();
        // TODO: Tree expansion if necessary
        TerminologyCode termCode = this.criterion.getTermCode();
        this.sb.append(mapping.getTermCodeSearchParameter()).append('=');
        sbTemp.append(termCode.getSystem()).append('|').append(termCode.getCode());
        this.sb.append(urlEncodeAndReset(sbTemp));
    }

    private void appendValueFilterByType() {
        MappingEntry mapping = this.criterion.getMapping();
        StringBuilder sbTemp = new StringBuilder();

        if(mapping.getTermCodeSearchParameter() != null){
            TerminologyCode termCode = this.criterion.getTermCode();
            sb.append(mapping.getTermCodeSearchParameter()).append('=');
            sbTemp.append(termCode.getSystem()).append('|').append(termCode.getCode());
            sb.append(urlEncodeAndReset(sbTemp));
        }
        FilterType filter = this.criterion.getValueFilter().getFilter();
        if (filter == FilterType.QUANTITY_COMPARATOR){
            appendQuantityComparatorFilterString();
        }
        else if (filter == FilterType.QUANTITY_RANGE){
            appendQuantityRangeFilterString();
        }
        //TODO: handle FilterType.CONCEPT
    }

    private void appendQuantityRangeFilterString() {
        ValueFilter valueFilter = this.criterion.getValueFilter();
        MappingEntry mapping = this.criterion.getMapping();
        String valueSearchParameter = mapping.getValueSearchParameter();
        StringBuilder sbTemp = new StringBuilder();

        sb.append(valueSearchParameter).append("=");
        sbTemp.append("ge ").append(valueFilter.getMinValue()).append(valueFilter.getUnit());
        sb.append(urlEncodeAndReset(sbTemp)).append('&');

        sb.append(valueSearchParameter).append("=");
        sbTemp.append("le ").append(valueFilter.getMaxValue()).append(valueFilter.getUnit());
        sb.append(urlEncodeAndReset(sbTemp));
    }

    private void appendQuantityComparatorFilterString() {
        ValueFilter valueFilter = this.criterion.getValueFilter();
        String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();
        StringBuilder sbTemp = new StringBuilder();

        this.sb.append(valueSearchParameter).append('=');
        sbTemp.append(valueFilter.getComparator()).append(valueFilter.getValue()).append(valueFilter.getUnit());
        this.sb.append(urlEncodeAndReset(sbTemp));
    }

    private static String urlEncodeAndReset(StringBuilder strBuilder){
        String encoded = URLEncoder.encode(strBuilder.toString(), StandardCharsets.UTF_8);
        // Reset StringBuilder
        strBuilder.setLength(0);
        strBuilder.trimToSize();
        return encoded;
    }
}
