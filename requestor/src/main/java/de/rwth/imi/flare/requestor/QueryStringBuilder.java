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

/**
 * Builds a query string for a criterion, use {@link #constructQueryString(Criterion searchCriterion)}<br>
 * For each query construction creates a new Class instance
 */
public class QueryStringBuilder {
    private final Criterion criterion;
    private final StringBuilder sb;

    /**
     * Constructs the FHIR query String representing the given search criterion
     * @param searchCriterion criterion to be built into a String
     * @return query String that can be appended onto a FHIR Server URI, starts with the resource (e.g. Patient?...)
     */
    public static String constructQueryString(Criterion searchCriterion){
        QueryStringBuilder builder = new QueryStringBuilder(searchCriterion);
        builder.constructQueryString();
        return builder.sb.toString();
    }

    /**
     * Initializes queryBuilder context
     */
    private QueryStringBuilder(Criterion searchCriterion){
        this.sb = new StringBuilder();
        this.criterion = searchCriterion;
    }

    /**
     * Constructs the query string into {@link #sb}
     */
    private void constructQueryString(){
        MappingEntry mapping = this.criterion.getMapping();
        this.sb.append(mapping.getFhirResourceType()).append('?');

        if(this.criterion.getValueFilter() != null){
            if(mapping.getTermCodeSearchParameter() != null){
                StringBuilder sbTmp = new StringBuilder();
                this.sb.append(mapping.getTermCodeSearchParameter()).append("=");
                sbTmp.append(this.criterion.getTermCode().getSystem())
                        .append("|")
                        .append(this.criterion.getTermCode().getCode());
                this.sb.append(urlEncodeAndReset(sbTmp)).append(("&"));
            }

            appendValueFilterByType();
        }

        if(mapping.getFixedCriteria() != null){
            appendFixedCriteriaString();
        }
    }

    /**
     * Appends the fixed criteria as given by the mapping
     */
    private void appendFixedCriteriaString() {
        for (FixedCriteria criterion : this.criterion.getMapping().getFixedCriteria()){
            String valueString = concatenateTerminologyCodes(criterion.getValue());
            this.sb.append('&').append(criterion.getSearchParameter()).append('=').append(valueString);
        }
    }

    /**
     * Appends the {@link MappingEntry mappings} {@link TerminologyCode termCode search parameter}
     * and appends it if existing <br>
     * Then takes the {@link ValueFilter filter} contained in the {@link Criterion} and calls the builder method
     * corresponding to the {@link FilterType} of the {@link ValueFilter filter}
     */
    private void appendValueFilterByType() {
        MappingEntry mapping = this.criterion.getMapping();
        StringBuilder sbTmp = new StringBuilder();

        if(mapping.getTermCodeSearchParameter() != null){
            TerminologyCode termCode = this.criterion.getTermCode();
            sb.append(mapping.getTermCodeSearchParameter()).append('=');
            sbTmp.append(termCode.getSystem()).append('|').append(termCode.getCode());
            sb.append(urlEncodeAndReset(sbTmp));
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

    /**
     * Called if the {@link ValueFilter} is a Concept filter, appends the concept filter
     */
    private void appendConceptFilterString() {
        ValueFilter valueFilter = this.criterion.getValueFilter();
        String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();

        sb.append(valueSearchParameter)
                .append('=')
                .append(concatenateTerminologyCodes(valueFilter.getSelectedConcepts()));
    }

    /**
     * Called if the {@link ValueFilter} is a QuantityRange filter, appends two comparator filters
     */
    private void appendQuantityRangeFilterString() {
        ValueFilter valueFilter = this.criterion.getValueFilter();
        MappingEntry mapping = this.criterion.getMapping();
        String valueSearchParameter = mapping.getValueSearchParameter();
        StringBuilder sbTmp = new StringBuilder();

        sb.append(valueSearchParameter).append("=");
        sbTmp.append("ge ").append(valueFilter.getMinValue()).append(valueFilter.getUnit());
        sb.append(urlEncodeAndReset(sbTmp)).append('&');

        sb.append(valueSearchParameter).append("=");
        sbTmp.append("le ").append(valueFilter.getMaxValue()).append(valueFilter.getUnit());
        sb.append(urlEncodeAndReset(sbTmp));
    }

    /**
     * Called if the {@link ValueFilter} is a Quantity filter, appends the comparator filter
     */
    private void appendQuantityComparatorFilterString() {
        ValueFilter valueFilter = this.criterion.getValueFilter();
        String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();
        StringBuilder sbTmp = new StringBuilder();

        this.sb.append(valueSearchParameter).append('=');
        sbTmp.append(valueFilter.getComparator()).append(valueFilter.getValue()).append(valueFilter.getUnit());
        this.sb.append(urlEncodeAndReset(sbTmp));
    }

    /**
     * Helper method, joins an array of terminology codes with commas
     * @param termCodes Codes to be joined
     * @return String looking like this: "code|system,code2|system2"...
     */
    private String concatenateTerminologyCodes(TerminologyCode[] termCodes) {
        StringBuilder sbTmp = new StringBuilder();
        List<String> encodedCriteriaValueList = new LinkedList<>();
        for(TerminologyCode value : termCodes){
            sbTmp.append(value.getCode()).append('|').append(value.getSystem());
            encodedCriteriaValueList.add(urlEncodeAndReset(sbTmp));
        }
        return String.join(",", encodedCriteriaValueList);
    }

    /**
     * Helper Method, url encodes the content of the given {@code strBuilder} and empties the builder
     * @return url encoded contents of the {@code strBuilder}
     */
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
