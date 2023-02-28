package de.rwth.imi.flare.requestor;

import de.rwth.imi.flare.api.UnsupportedCriterionException;
import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchQueryStringBuilderTest {

    private SearchQueryStringBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new SearchQueryStringBuilder(Clock.fixed(Instant.parse("2023-02-20T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    public void constructQueryString_age() throws Exception {
        assertEquals("Patient?birthdate=lt1988-02-20", ageSingleComparisonRequest(35.0, "a", Comparator.gt));
        assertEquals("Patient?birthdate=gt1991-02-20", ageSingleComparisonRequest(32.0, "a", Comparator.lt));
        assertEquals("Patient?birthdate=gt1991-02-20", ageSingleComparisonRequest(32.634, "a", Comparator.lt));
        assertEquals("Patient?birthdate=gt1991-02-20", ageSingleComparisonRequest(32.0 * 12.0, "mo", Comparator.lt));
        assertEquals("Patient?birthdate=lt2023-02-06", ageSingleComparisonRequest(2.0, "wk", Comparator.gt));

        assertEquals("Patient?birthdate=gt1990-02-21&birthdate=lt1991-02-20", ageSingleComparisonRequest(32.0, "a", Comparator.eq));
        assertEquals("Patient?birthdate=lt2022-02-20&birthdate=gt2020-02-20", ageRangeRequest(1.0, 3.0, "a"));

        assertThrows(UnsupportedCriterionException.class, () -> ageSingleComparisonRequest(32.0, "a", Comparator.ne));
        assertThrows(UnsupportedCriterionException.class, () -> ageSingleComparisonRequest(2.0, "d", Comparator.gt));
    }

    public String ageSingleComparisonRequest(Double age, String timeUnit, Comparator comparator)
            throws UnsupportedCriterionException {
        Criterion ageTestCrit = new Criterion();
        ValueFilter valueFilter = new ValueFilter();
        valueFilter.setValue(age);
        valueFilter.setComparator(comparator);
        valueFilter.setType(FilterType.QUANTITY_COMPARATOR);
        TerminologyCode unit = new TerminologyCode();
        unit.setCode(timeUnit);
        valueFilter.setUnit(unit);
        ageTestCrit.setValueFilter(valueFilter);
        MappingEntry mapping = new MappingEntry();
        mapping.setFhirResourceType("Patient");
        ageTestCrit.setMapping(mapping);
        TerminologyCode termCode = new TerminologyCode("424144002", "http://snomed.info/sct", "SomeDisplay");
        ageTestCrit.setTermCodes(List.of(termCode));
        return builder.constructQueryString(ageTestCrit);
    }

    public String ageRangeRequest(Double minAge, Double maxAge, String timeUnit) throws UnsupportedCriterionException {
        Criterion ageTestCrit = new Criterion();
        ValueFilter valueFilter = new ValueFilter();
        valueFilter.setMinValue(minAge);
        valueFilter.setMaxValue(maxAge);
        valueFilter.setType(FilterType.QUANTITY_RANGE);
        TerminologyCode unit = new TerminologyCode();
        unit.setCode(timeUnit);
        valueFilter.setUnit(unit);
        ageTestCrit.setValueFilter(valueFilter);
        MappingEntry mapping = new MappingEntry();
        mapping.setFhirResourceType("Patient");
        ageTestCrit.setMapping(mapping);
        TerminologyCode termCode = new TerminologyCode("424144002", "http://snomed.info/sct", "SomeDisplay");
        ageTestCrit.setTermCodes(List.of(termCode));
        return builder.constructQueryString(ageTestCrit);
    }
}
