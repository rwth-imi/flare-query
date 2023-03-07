package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.UnsupportedCriterionException;
import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.AttributeSearchParameter;
import de.rwth.imi.flare.api.model.mapping.FixedCriteria;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;

import javax.ws.rs.core.UriBuilder;
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
        return builder.ub.toString();
    }

    private class Builder {

        private final Criterion criterion;
        private final UriBuilder ub;

        private Builder(Criterion searchCriterion) {
            this.ub = UriBuilder.fromUri("");
            this.criterion = searchCriterion;
        }

        private void constructQueryString() throws UnsupportedCriterionException {
            MappingEntry mappings = this.criterion.getMapping();
            String termCodeSearchParam = mappings.getTermCodeSearchParameter();
            TerminologyCode termCode = this.criterion.getTermCodes().get(0);
            ValueFilter valueFilter = this.criterion.getValueFilter();
            List<FixedCriteria> fixedCriteria = mappings.getFixedCriteria();
            List<AttributeFilter> attributeFilters = this.criterion.getAttributeFilters();

            this.ub.path(mappings.getFhirResourceType());

            if (termCode.getCode().equals("424144002") && termCode.getSystem().equals("http://snomed.info/sct")) {
                appendAge();
                return;
            }

            if (termCodeSearchParam != null) {
                appendTermCode(mappings, termCode);
            }

            if(valueFilter != null) {
                appendValueFilter();
            }

            if (fixedCriteria != null) {
                appendFixedCriteria();
            }

            if (attributeFilters != null) {
                appendAttributeFilter();
            }

            appendTimeConstraints();
        }

        private void appendTermCode(MappingEntry mappings,
            TerminologyCode termCode) {
            StringBuilder sbTmp = new StringBuilder();
            sbTmp.append(termCode.getSystem())
                    .append("|")
                    .append(termCode.getCode());
            this.ub.queryParam(mappings.getTermCodeSearchParameter(), sbTmp);
        }

        private void appendAge() throws UnsupportedCriterionException {
            FilterType filter = this.criterion.getValueFilter().getType();
            if (filter == FilterType.QUANTITY_COMPARATOR) {
                if (checkAppendEqOrNeComparison()) {
                    return;
                }

                appendSingleAgeComparison(this.criterion.getValueFilter().getValue(), this.criterion.getValueFilter().getComparator());
            } else if (filter == FilterType.QUANTITY_RANGE) {
                appendSingleAgeComparison(this.criterion.getValueFilter().getMinValue(), Comparator.gt);
                appendSingleAgeComparison(this.criterion.getValueFilter().getMaxValue(), Comparator.lt);
            }
            return;
        }

        private boolean checkAppendEqOrNeComparison() throws UnsupportedCriterionException {
            String comparator = this.criterion.getValueFilter().getComparator().toString();
            if (comparator.equals("eq")) {
                Double age = this.criterion.getValueFilter().getValue();
                LocalDate minDate = timeValueToDate(age + 1);
                LocalDate maxDate = timeValueToDate(age);
                minDate = minDate.plusDays(1);

                this.ub.queryParam("birthdate", "gt" + minDate);
                this.ub.queryParam("birthdate", "lt" + maxDate);
                return true;

            } else if (comparator.equals("ne")) {
                throw new UnsupportedCriterionException("comparator 'ne' is not implemented");
            }
            return false;
        }

        private void appendSingleAgeComparison(Double age, Comparator comparator) throws UnsupportedCriterionException {
            String newComparator = "";
            switch (comparator.toString()) {
                case "gt" -> newComparator = "lt";
                case "lt" -> newComparator = "gt";
                case "ge" -> newComparator = "le";
                case "le" -> newComparator = "ge";
            }

            LocalDate dateToCompare = this.timeValueToDate(age);
            this.ub.queryParam("birthdate", newComparator + dateToCompare);
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

        private void appendFixedCriteria() {

            for (FixedCriteria criterion : this.criterion.getMapping().getFixedCriteria()) {
                if (criterion.getType().equals("code")) {
                    for (TerminologyCode valueMember : criterion.getValue()) {
                        valueMember.setSystem("");
                    }
                }
                String valueString = concatenateTerminologyCodes(criterion.getValue());
                this.ub.queryParam(criterion.getSearchParameter(), valueString);
            }
        }

        private void appendTimeConstraints() {

            TimeRestriction timeRestriction = this.criterion.getTimeRestriction();
            String timeRestrictionParameter = this.criterion.getMapping().getTimeRestrictionParameter();
            if (timeRestrictionParameter == null || timeRestriction == null) {
                return;
            }

            String beforeDate = timeRestriction.getBeforeDate();
            String afterDate = timeRestriction.getAfterDate();

            if (beforeDate != null) {
                this.ub.queryParam(timeRestrictionParameter, "le" + beforeDate);
            }
            if (afterDate != null) {
                this.ub.queryParam(timeRestrictionParameter, "ge" + afterDate);
            }
        }

        private AttributeSearchParameter getSearchParameter(List<AttributeSearchParameter> attSearchParams, TerminologyCode key) {

            for (AttributeSearchParameter cur : attSearchParams) {

                if (cur.getAttributeKey().getCode().equals(key.getCode()) && cur.getAttributeKey().getSystem().equals(key.getSystem())) {
                    return cur;
                }
            }

            return null;
        }

        private void appendAttributeFilter() {

            List<AttributeSearchParameter> searchParams = this.criterion.getMapping().getAttributeSearchParameters();

            for (AttributeFilter attributeFilter : this.criterion.getAttributeFilters()) {

                AttributeSearchParameter attSearchParam = this.getSearchParameter(searchParams, attributeFilter.getAttributeCode());

                if (attSearchParam.getAttributeType().equalsIgnoreCase("code")) {
                    for (TerminologyCode singleTermCode : attributeFilter.getSelectedConcepts()) {
                        singleTermCode.setSystem("");
                    }
                }

                String concepts = concatenateTerminologyCodes(attributeFilter.getSelectedConcepts());
                this.ub.queryParam(attSearchParam.getAttributeSearchParameter(), concepts);
            }
        }

        private void appendValueFilter() {

            FilterType filter = this.criterion.getValueFilter().getType();
            if (filter == FilterType.QUANTITY_COMPARATOR) {
                appendQuantityComparatorFilterString();
            } else if (filter == FilterType.QUANTITY_RANGE) {
                appendQuantityRangeFilterString();
            } else if (filter == FilterType.CONCEPT) {
                appendConceptFilterString();
            }
        }

        private void appendConceptFilterString() {
            ValueFilter valueFilter = this.criterion.getValueFilter();
            String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();
            this.ub.queryParam(valueSearchParameter, concatenateTerminologyCodes(valueFilter.getSelectedConcepts()));
        }

        private void appendQuantityRangeFilterString() {
            ValueFilter valueFilter = this.criterion.getValueFilter();
            String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();

            StringBuilder sbGe = new StringBuilder();
            sbGe.append("ge").append(valueFilter.getMinValue());
            appendFilterUnit(valueFilter.getUnit(), sbGe);
            this.ub.queryParam(valueSearchParameter, sbGe.toString());

            StringBuilder sbLe = new StringBuilder();
            sbLe.append("le").append(valueFilter.getMaxValue());
            appendFilterUnit(valueFilter.getUnit(), sbLe);
            this.ub.queryParam(valueSearchParameter, sbLe.toString());
        }

        private void appendQuantityComparatorFilterString() {
            ValueFilter valueFilter = this.criterion.getValueFilter();
            String valueSearchParameter = this.criterion.getMapping().getValueSearchParameter();
            StringBuilder sbTmp = new StringBuilder();
            sbTmp.append(valueFilter.getComparator()).append(valueFilter.getValue());
            appendFilterUnit(valueFilter.getUnit(), sbTmp);
            this.ub.queryParam(valueSearchParameter, sbTmp.toString());
        }

        private void appendFilterUnit(TerminologyCode filterUnit, StringBuilder sbTemp) {
            String system = filterUnit.getSystem();

            system = system == null ? "http://unitsofmeasure.org" : system;
            String code = filterUnit.getCode();
            sbTemp.append("|").append(system).append("|").append(code);
        }

        private String concatenateTerminologyCodes(List<TerminologyCode> termCodes) {
            List<String> encodedTerminologyList = new LinkedList<>();
            String pipe = "|";
            for (TerminologyCode value : termCodes) {
                String encodedTerminologyString;

                if (this.criterion.getMapping().getValueTypeFhir() != null &&
                        this.criterion.getMapping().getValueTypeFhir().equals("code")) {
                    encodedTerminologyString = value.getCode();
                } else if (value.getSystem().equals("")) {
                    //pipe is not needed if there is no system to be specified in the FHIR URL
                    encodedTerminologyString = value.getCode();
                } else {
                    encodedTerminologyString = value.getSystem() + pipe + value.getCode();
                }
                encodedTerminologyList.add(encodedTerminologyString);
            }
            return String.join(",", encodedTerminologyList);
        }

    }
}
