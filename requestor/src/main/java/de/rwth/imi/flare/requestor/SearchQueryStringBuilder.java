package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.UnsupportedCriterionException;
import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.AttributeSearchParameter;
import de.rwth.imi.flare.api.model.mapping.FixedCriteria;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Builds a FHIR search query string for a criterion, use {@link #constructQueryString(Criterion searchCriterion)}.
 * <p>
 * For each query construction creates a new Class instance.
 */
public class SearchQueryStringBuilder {

    /**
     * This clock should return the system time in production and can be used with a fixed time in tests.
     */
    private final Clock clock;

    public SearchQueryStringBuilder(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    /**
     * Constructs the FHIR query String representing the given search criterion.
     *
     * @param searchCriterion criterion to be built into a String
     * @return the query string that can be appended onto a FHIR Server URI, starts with the resource (e.g. Patient?...)
     * @throws UnsupportedCriterionException in case the criterion is not supported and so the search query string can't
     *                                       be constructed
     */
    public String constructQueryString(Criterion searchCriterion) throws UnsupportedCriterionException {
        Builder builder = new Builder(searchCriterion);
        builder.constructQueryString();
        return builder.sb.toString();
    }

    private class Builder {

        private final Criterion criterion;
        private final StringBuilder sb;
        private final LinkedList<String> queryParams;

        /**
         * Initializes queryBuilder context
         */
        private Builder(Criterion searchCriterion) {
            this.sb = new StringBuilder();
            this.criterion = searchCriterion;
            this.queryParams = new LinkedList<>();
        }

        /**
         * Constructs the query string into {@link #sb}
         */
        private void constructQueryString() throws UnsupportedCriterionException {
            MappingEntry mappings = this.criterion.getMapping();
            TerminologyCode termCode = this.criterion.getTermCodes().get(0);


            if (mappings.getTermCodeSearchParameter() != null) {
                StringBuilder sbTmp = new StringBuilder();
                String queryParam = mappings.getTermCodeSearchParameter() + "=";
                sbTmp.append(termCode.getSystem())
                        .append("|")
                        .append(termCode.getCode());
                this.queryParams.add(queryParam + urlEncodeAndReset(sbTmp));
            }

            TerminologyCode currentTermCode = this.criterion.getTermCodes().get(0);
            if (currentTermCode.getCode().equals("424144002") && currentTermCode.getSystem().equals("http://snomed.info/sct")) {
                FilterType filter = this.criterion.getValueFilter().getType();
                if (filter == FilterType.QUANTITY_COMPARATOR) {
                    if (checkAppendEqOrNeComparison()) {
                        constructFinalString(mappings);
                        return;
                    }

                    appendSingleAgeComparison(this.criterion.getValueFilter().getValue(), this.criterion.getValueFilter().getComparator());
                } else if (filter == FilterType.QUANTITY_RANGE) {
                    appendSingleAgeComparison(this.criterion.getValueFilter().getMinValue(), Comparator.gt);
                    appendSingleAgeComparison(this.criterion.getValueFilter().getMaxValue(), Comparator.lt);
                }
                constructFinalString(mappings);
                return;
            }

            if(this.criterion.getValueFilter() != null) {
                appendValueFilterByType();
            }

            if (mappings.getFixedCriteria() != null) {
                appendFixedCriteriaString();
            }

            if (this.criterion.getAttributeFilters() != null) {
                appendAttributeSearchParameterString();
            }

            appendTimeConstraints();

            constructFinalString(mappings);

        }

        private void constructFinalString(MappingEntry mappings){
            this.sb.append(mappings.getFhirResourceType());
            if(this.queryParams.size() > 0 ){
                this.sb.append('?').append(this.queryParams.removeFirst());
            }

            this.queryParams.stream().forEach((queryParam) -> this.sb.append("&").append(queryParam));

        }

        private boolean checkAppendEqOrNeComparison() throws UnsupportedCriterionException {
            String comparator = this.criterion.getValueFilter().getComparator().toString();
            if (comparator.equals("eq")) {
                Double age = this.criterion.getValueFilter().getValue();
                LocalDate minDate = timeValueToDate(age + 1);
                LocalDate maxDate = timeValueToDate(age);
                minDate = minDate.plusDays(1);

                this.queryParams.add("birthdate=gt" + minDate);
                this.queryParams.add("birthdate=lt" + maxDate.toString());
                return true;

            } else if (comparator.equals("ne")) {
                throw new UnsupportedCriterionException("comparator 'ne' is not implemented");
            }
            return false;
        }

        private void appendSingleAgeComparison(Double age, Comparator comparator) throws UnsupportedCriterionException {
            StringBuilder queryParam = new StringBuilder();
            queryParam.append("birthdate=");
            switch (comparator.toString()) {
                case "gt" -> queryParam.append("lt");
                case "lt" -> queryParam.append("gt");
                case "ge" -> queryParam.append("le");
                case "le" -> queryParam.append("ge");
            }

            LocalDate dateToCompare = this.timeValueToDate(age);
            this.queryParams.add(queryParam + dateToCompare.toString());
        }

        private LocalDate timeValueToDate(Double timeValue) throws UnsupportedCriterionException {
            int filterValue = timeValue.intValue();
            LocalDate date = LocalDate.now(clock);
            // TODO: bad style to pass the age as argument but take the unit from the current criterion
            switch (this.criterion.getValueFilter().getUnit().getCode()) {
                case "a" -> date = date.minusYears(filterValue);
                case "mo" -> date = date.minusMonths(filterValue);
                case "wk" -> date = date.minusWeeks(filterValue);
                case "d", "h", "min" ->
                        throw new UnsupportedCriterionException("d, h, and min as unit of time not implemented");
            }
            return date;
        }

        /**
         * Appends the fixed criteria as given by the mapping
         */
        private void appendFixedCriteriaString() {

            for (FixedCriteria criterion : this.criterion.getMapping().getFixedCriteria()) {
                if (criterion.getType().equals("code")) {
                    for (TerminologyCode valueMember : criterion.getValue()) {
                        valueMember.setSystem("");
                    }
                }
                String valueString = concatenateTerminologyCodes(criterion.getValue());
                this.queryParams.add(criterion.getSearchParameter() + "=" + valueString);

            }
        }

        private void appendTimeConstraints() {
            StringBuilder sbTemp = new StringBuilder();

            TimeRestriction timeRestriction = this.criterion.getTimeRestriction();
            String timeRestrictionParameter = this.criterion.getMapping().getTimeRestrictionParameter();
            if (timeRestrictionParameter == null || timeRestriction == null) {
                return;
            }

            String beforeDate = timeRestriction.getBeforeDate();
            String afterDate = timeRestriction.getAfterDate();

            if (beforeDate != null) {
                sbTemp.append("&").append(timeRestrictionParameter).append("=le").append(beforeDate);
            }
            if (afterDate != null) {
                sbTemp.append("&").append(timeRestrictionParameter).append("=ge").append(afterDate);
            }
            this.queryParams.add(sbTemp.toString());
        }


        private AttributeSearchParameter getSearchParameter(List<AttributeSearchParameter> attSearchParams, TerminologyCode key) {

            for (int i = 0; i < attSearchParams.size(); i++) {

                AttributeSearchParameter cur = attSearchParams.get(i);

                if (cur.getAttributeKey().getCode().equals(key.getCode()) && cur.getAttributeKey().getSystem().equals(key.getSystem())) {
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

            for (AttributeFilter attributeFilter : this.criterion.getAttributeFilters()) {

                AttributeSearchParameter attSearchParam = this.getSearchParameter(searchParams, attributeFilter.getAttributeCode());

                if (attSearchParam.getAttributeType().equalsIgnoreCase("code")) {
                    for (TerminologyCode singleTermCode : attributeFilter.getSelectedConcepts()) {
                        singleTermCode.setSystem("");
                    }
                }

                String concepts = concatenateTerminologyCodes(attributeFilter.getSelectedConcepts());
                this.queryParams.add(attSearchParam.getAttributeSearchParameter() + "=" +concepts);
            }
        }

        private void appendValueFilterByType() {

            FilterType filter = this.criterion.getValueFilter().getType();
            if (filter == FilterType.QUANTITY_COMPARATOR) {
                appendQuantityComparatorFilterString();
            } else if (filter == FilterType.QUANTITY_RANGE) {
                appendQuantityRangeFilterString();
            } else if (filter == FilterType.CONCEPT) {
                appendConceptFilterString();
            }
        }

        /**
         * Called if the {@link ValueFilter} is a Concept filter, appends the concept filter
         */
        private void appendConceptFilterString() {
            ValueFilter valueFilter = this.criterion.getValueFilter();
            String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();

            this.queryParams.add(valueSearchParameter + "=" +
                concatenateTerminologyCodes(valueFilter.getSelectedConcepts()));
        }

        /**
         * Called if the {@link ValueFilter} is a QuantityRange filter, appends two comparator filters
         */
        private void appendQuantityRangeFilterString() {
            ValueFilter valueFilter = this.criterion.getValueFilter();
            String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();
            StringBuilder sbTmp = new StringBuilder();

            String queryParamAgeGe = valueSearchParameter +"=";
            sbTmp.append("ge").append(valueFilter.getMinValue());
            appendFilterUnit(valueFilter.getUnit(), sbTmp);
            this.queryParams.add(queryParamAgeGe + urlEncodeAndReset(sbTmp));

            String queryParamAgeLe = valueSearchParameter +"=";
            sbTmp.append("le").append(valueFilter.getMaxValue());
            appendFilterUnit(valueFilter.getUnit(), sbTmp);
            this.queryParams.add( queryParamAgeLe + urlEncodeAndReset(sbTmp));
        }

        /**
         * Called if the {@link ValueFilter} is a Quantity filter, appends the comparator filter
         */
        private void appendQuantityComparatorFilterString() {
            ValueFilter valueFilter = this.criterion.getValueFilter();
            String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();
            StringBuilder sbTmp = new StringBuilder();
            String queryParam = valueSearchParameter + "=";
            sbTmp.append(valueFilter.getComparator()).append(valueFilter.getValue());
            appendFilterUnit(valueFilter.getUnit(), sbTmp);
            this.queryParams.add(queryParam + urlEncodeAndReset(sbTmp));
        }

        private void appendFilterUnit(TerminologyCode filterUnit, StringBuilder sbTemp) {
            String system = filterUnit.getSystem();

            system = system == null ? "http://unitsofmeasure.org" : system;
            String code = filterUnit.getCode();
            sbTemp.append("|").append(system).append("|").append(code);
        }

        /**
         * Helper method, joins an array of terminology codes with commas
         *
         * @param termCodes Codes to be joined
         * @return String looking like this: "system|code,system2|code2"...
         */
        private String concatenateTerminologyCodes(List<TerminologyCode> termCodes) {
            List<String> encodedTerminologyList = new LinkedList<>();
            String pipe = "|";
            for (TerminologyCode value : termCodes) {
                String encodedTerminologyString;

                if (this.criterion.getMapping().getValueTypeFhir() != null &&
                        this.criterion.getMapping().getValueTypeFhir().equals("code")) {
                    encodedTerminologyString = urlEncode(value.getCode());
                } else if (value.getSystem().equals("")) {
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
         *
         * @return url encoded contents of the {@code strBuilder}
         */
        private static String urlEncodeAndReset(StringBuilder strBuilder) {
            return urlEncode(reset(strBuilder));
        }

        private static String reset(StringBuilder strBuilder) {
            String text = strBuilder.toString();
            strBuilder.setLength(0);
            strBuilder.trimToSize();
            return text;
        }

        private static String urlEncode(String str) {
            return URLEncoder.encode(str, StandardCharsets.UTF_8);
        }
    }
}
