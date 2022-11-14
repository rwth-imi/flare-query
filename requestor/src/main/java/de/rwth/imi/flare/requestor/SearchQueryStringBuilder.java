package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.AttributeSearchParameter;
import de.rwth.imi.flare.api.model.mapping.FixedCriteria;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

/**
 * Builds a FHIR search query string for a criterion, use {@link #constructQueryString(Criterion searchCriterion)}<br>
 * For each query construction creates a new Class instance
 */
public class SearchQueryStringBuilder {
    private final Criterion criterion;
    private final StringBuilder sb;

    /**
     * Constructs the FHIR query String representing the given search criterion
     * @param searchCriterion criterion to be built into a String
     * @return query String that can be appended onto a FHIR Server URI, starts with the resource (e.g. Patient?...)
     */
    public static String constructQueryString(Criterion searchCriterion){
        SearchQueryStringBuilder builder = new SearchQueryStringBuilder(searchCriterion);
        builder.constructQueryString();
        return builder.sb.toString();
    }

    /**
     * Initializes queryBuilder context
     */
    private SearchQueryStringBuilder(Criterion searchCriterion){
        this.sb = new StringBuilder();
        this.criterion = searchCriterion;
    }

    /**
     * Constructs the query string into {@link #sb}
     */
    private void constructQueryString(){
        MappingEntry mappings = this.criterion.getMapping();
        TerminologyCode termCode = this.criterion.getTermCodes().get(0);

        this.sb.append(mappings.getFhirResourceType()).append('?');

        if(mappings.getTermCodeSearchParameter() != null){
            StringBuilder sbTmp = new StringBuilder();
            this.sb.append(mappings.getTermCodeSearchParameter()).append("=");
            sbTmp.append(termCode.getSystem())
                    .append("|")
                    .append(termCode.getCode());
            this.sb.append(urlEncodeAndReset(sbTmp));
        }

        TerminologyCode currentTermCode = this.criterion.getTermCodes().get(0);
        if(currentTermCode.getCode().equals("age") && currentTermCode.getSystem().equals("mii.abide")){

            FilterType filter = this.criterion.getValueFilter().getType();
            if(filter ==  FilterType.QUANTITY_COMPARATOR){
                appendSingleAgeComparison(this.criterion.getValueFilter().getValue(), this.criterion.getValueFilter().getComparator());
            }else if(filter == FilterType.QUANTITY_RANGE) {
                appendSingleAgeComparison(this.criterion.getValueFilter().getMinValue(), Comparator.gt);
                this.sb.append("&");
                appendSingleAgeComparison(this.criterion.getValueFilter().getMaxValue(), Comparator.lt);
            }
            return;
        }

        if(this.criterion.getMapping().getFhirResourceType().equals("Consent")){
            ValueFilter valueFilter = this.criterion.getValueFilter();
            this.sb.append('$').append(concatenateTerminologyCodes(valueFilter.getSelectedConcepts()));
            return;
        }

        if(this.criterion.getValueFilter() != null){
            if( mappings.getTermCodeSearchParameter()!= null){
                this.sb.append(("&"));
            }
            appendValueFilterByType();
        }

        if(mappings.getFixedCriteria() != null){
            appendFixedCriteriaString();
        }

        if(this.criterion.getAttributeFilters() != null){
            appendAttributeSearchParameterString();
        }

        appendTimeConstraints();
    }

    private void appendSingleAgeComparison(Double age, Comparator comparator){
        this.sb.append("birthdate=");
        switch (comparator.toString()) {
            case "gt" -> this.sb.append("lt");
            case "lt" -> this.sb.append("gt");
            case "ge" -> this.sb.append("le");
            case "le" -> this.sb.append("ge");
            case "eq" -> this.sb.append("eq");
            case "ne" -> this.sb.append("ne");
        }

        LocalDate dateToCompare = this.timeValueToDate(age);
        this.sb.append(dateToCompare.toString());
    }

    private LocalDate timeValueToDate(Double age){
        int filterValue = age.intValue();
        LocalDate date = LocalDate.now();
        switch (this.criterion.getValueFilter().getUnit().getCode()) {
            case "a" -> date = date.minusYears(filterValue);
            case "mo" -> date = date.minusMonths(filterValue);
            case "wk" -> date = date.minusWeeks(filterValue);
            case "d" -> date = date.minusDays(filterValue);
            case "h" -> date = date.minusDays(filterValue / 24);
            case "min" -> date = date.minusDays((filterValue / 60) / 24);
        };
        return date;
    }

    /**
     * Appends the fixed criteria as given by the mapping
     */
    private void appendFixedCriteriaString() {
        for (FixedCriteria criterion : this.criterion.getMapping().getFixedCriteria()){
            if(criterion.getType().equals("code")){
                for (TerminologyCode valueMember : criterion.getValue()){
                    valueMember.setSystem("");
                }
            }
            String valueString = concatenateTerminologyCodes(criterion.getValue());
            this.sb.append('&').append(criterion.getSearchParameter()).append('=').append(valueString);
        }
    }

    private void appendTimeConstraints(){
        StringBuilder sbTemp = new StringBuilder();

        TimeRestriction timeRestriction = this.criterion.getTimeRestriction();
        String timeRestrictionParameter = this.criterion.getMapping().getTimeRestrictionParameter();
        if(timeRestrictionParameter == null || timeRestriction == null){
            return;
        }

        String beforeDate = timeRestriction.getBeforeDate();
        String afterDate = timeRestriction.getAfterDate();

        if(beforeDate != null){
            sbTemp.append("&").append(timeRestrictionParameter).append("=le").append(beforeDate);
        }
        if(afterDate != null){
            sbTemp.append("&").append(timeRestrictionParameter).append("=ge").append(afterDate);
        }
        this.sb.append(sbTemp);
    }


    private AttributeSearchParameter getSearchParameter(List<AttributeSearchParameter> attSearchParams, TerminologyCode key){

        for(int i = 0 ; i < attSearchParams.size(); i++){

            AttributeSearchParameter cur = attSearchParams.get(i);

            if(cur.getAttributeKey().getCode().equals(key.getCode()) && cur.getAttributeKey().getSystem().equals(key.getSystem())){
                return attSearchParams.get(i);
            }
        }

        return null;
    }

    /**
     * Appends the attributeFilter as given by the mapping
     */
    private void appendAttributeSearchParameterString() {

        List<AttributeSearchParameter> searchParams = this.criterion.getMapping().getAttributeSearchParameters();

        for (AttributeFilter attributeFilter : this.criterion.getAttributeFilters()){

            AttributeSearchParameter attSearchParam = this.getSearchParameter(searchParams, attributeFilter.getAttributeCode());

            if(attSearchParam.getAttributeType().equalsIgnoreCase("code")){
                for (TerminologyCode singleTermCode : attributeFilter.getSelectedConcepts()){
                    singleTermCode.setSystem("");
                }
            }

            String concepts = concatenateTerminologyCodes(attributeFilter.getSelectedConcepts());

            if(this.sb.indexOf("?") == sb.length() - 1 ) {
                this.sb.append(attSearchParam.getAttributeSearchParameter()).append('=').append(concepts);
            } else {
                this.sb.append('&').append(attSearchParam.getAttributeSearchParameter()).append('=').append(concepts);
            }

        }
    }

    /**
     * Appends the {@link MappingEntry mappings} {@link TerminologyCode termCode search parameter}
     * and appends it if existing <br>
     * Then takes the {@link ValueFilter filter} contained in the {@link Criterion} and calls the builder method
     * corresponding to the {@link FilterType} of the {@link ValueFilter filter}
     */
    private void appendValueFilterByType() {

        FilterType filter = this.criterion.getValueFilter().getType();
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
        String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();
        StringBuilder sbTmp = new StringBuilder();

        sb.append(valueSearchParameter).append("=");
        sbTmp.append("ge").append(valueFilter.getMinValue());
        appendFilterUnit(valueFilter.getUnit(), sbTmp);
        sb.append(urlEncodeAndReset(sbTmp)).append('&');

        sb.append(valueSearchParameter).append("=");
        sbTmp.append("le").append(valueFilter.getMaxValue());
        appendFilterUnit(valueFilter.getUnit(), sbTmp);
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
        sbTmp.append(valueFilter.getComparator()).append(valueFilter.getValue());
        appendFilterUnit(valueFilter.getUnit(), sbTmp);
        this.sb.append(urlEncodeAndReset(sbTmp));
    }

    private void appendFilterUnit(TerminologyCode filterUnit, StringBuilder sbTemp){
        String system = filterUnit.getSystem();

        system = system==null?"http://unitsofmeasure.org":system;
        String code = filterUnit.getCode();
        sbTemp.append("|").append(system).append("|").append(code);
    }

    /**
     * Helper method, joins an array of terminology codes with commas
     * @param termCodes Codes to be joined
     * @return String looking like this: "system|code,system2|code2"...
     */
    private String concatenateTerminologyCodes(List<TerminologyCode> termCodes) {
        List<String> encodedTerminologyList = new LinkedList<>();
        String pipe = "|";
        for(TerminologyCode value : termCodes){
            String encodedTerminologyString;

            if(this.criterion.getMapping().getValueTypeFhir() != null &&
                this.criterion.getMapping().getValueTypeFhir().equals("code")){
                encodedTerminologyString = urlEncode(value.getCode());
            } else if(value.getSystem().equals("")){
                //pipe is not needed if there is no system to be specified in the FHIR URL
                encodedTerminologyString = urlEncode(value.getCode());
            } else {
                encodedTerminologyString = urlEncode(value.getSystem() + pipe + value.getCode());
            }
            encodedTerminologyList.add(encodedTerminologyString);
        }
        return String.join(",", encodedTerminologyList);
    }

    /**
     * Helper Method, url encodes the content of the given {@code strBuilder} and empties the builder
     * @return url encoded contents of the {@code strBuilder}
     */
    private static String urlEncodeAndReset(StringBuilder strBuilder){
        return urlEncode(reset(strBuilder));
    }

    private static String reset(StringBuilder strBuilder){
        String text = strBuilder.toString();
        strBuilder.setLength(0);
        strBuilder.trimToSize();
        return text;
    }

    private static String urlEncode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }
}
