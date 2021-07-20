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
            if(mapping.getTermCodeSearchParameter() != null){
                StringBuilder sbTemp = new StringBuilder();
                this.sb.append(mapping.getTermCodeSearchParameter()).append("=");
                sbTemp.append(this.criterion.getTermCode().getSystem())
                        .append("|")
                        .append(this.criterion.getTermCode().getCode());
                this.sb.append(urlEncodeAndReset(sbTemp)).append(("&"));
            }

            appendValueFilterByType();
        }

        if(mapping.getFixedCriteria() != null){
            appendFixedCriteriaString();
        }

        return sb.toString();
    }

    private void appendFixedCriteriaString() {
        for (FixedCriteria criterion : this.criterion.getMapping().getFixedCriteria()){
            String valueString = concatenateTerminologyCodes(criterion.getValue());
            this.sb.append('&').append(criterion.getSearchParameter()).append('=').append(valueString);
        }
    }

    private String concatenateTerminologyCodes(TerminologyCode[] termCodes) {
        StringBuilder sbTemp = new StringBuilder();
        List<String> encodedCriteriaValueList = new LinkedList<>();
        for(TerminologyCode value : termCodes){
            sbTemp.append(value.getCode()).append('|').append(value.getSystem());
            encodedCriteriaValueList.add(urlEncodeAndReset(sbTemp));
        }
        return String.join(",", encodedCriteriaValueList);
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
        else if (filter == FilterType.CONCEPT){
            appendConceptFilterString();
        }
    }

    private void appendConceptFilterString() {
        ValueFilter valueFilter = this.criterion.getValueFilter();
        String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();

        sb.append(valueSearchParameter)
                .append('=')
                .append(concatenateTerminologyCodes(valueFilter.getSelectedConcepts()));
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
        String encoded = urlEncode(strBuilder.toString());
        // Reset StringBuilder
        strBuilder.setLength(0);
        strBuilder.trimToSize();
        return encoded;
    }

    private static String urlEncode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }
}
