import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import de.rwth.imi.flare.requestor.SearchQueryStringBuilder;
import org.junit.jupiter.api.Test;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestAgeStringBuilder {
    @Test
    public void mainAgeTests() throws URISyntaxException {

        assertEquals("Patient?birthdate=lt" + getUpdatedDate("1987-11-16"), ageSingeComparisonRequest(35.0, "a", Comparator.gt));
        assertEquals("Patient?birthdate=gt" + getUpdatedDate("1990-11-16"), ageSingeComparisonRequest(32.0, "a", Comparator.lt));
        assertEquals("Patient?birthdate=gt" + getUpdatedDate("1990-11-16"), ageSingeComparisonRequest(32.634,  "a", Comparator.lt));
        assertEquals("Patient?birthdate=gt" + getUpdatedDate("1990-11-16"), ageSingeComparisonRequest(32.0 * 12.0, "mo", Comparator.lt));
        assertEquals("Patient?birthdate=lt" + getUpdatedDate("2022-11-02"), ageSingeComparisonRequest(2.0,  "wk", Comparator.gt));

        assertEquals("Patient?birthdate=gt" + getUpdatedDate("1989-11-17")
                          + "&birthdate=lt" + getUpdatedDate("1990-11-16"), ageSingeComparisonRequest(32.0,  "a", Comparator.eq));

        assertEquals("Patient?birthdate=lt" + getUpdatedDate("2021-11-16") + "&birthdate=gt" + getUpdatedDate("2019-11-16")
                , ageRangeRequest(1.0, 3.0, "a" ));

        ageSingeComparisonRequest(32.0,  "a", Comparator.ne); // should produce a "comparator not implemented" exception
        ageSingeComparisonRequest(2.0, "d", Comparator.gt); // should produce a unit exception
    }


    public String ageSingeComparisonRequest(Double age, String timeUnit, Comparator comparator) throws URISyntaxException{
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
        TerminologyCode termCode = new TerminologyCode("age", "mii.abide", "SomeDisplay");
        ageTestCrit.setTermCodes(List.of(termCode));
        String searchString = SearchQueryStringBuilder.constructQueryString(ageTestCrit);
        return searchString;
    }


    public String ageRangeRequest(Double minAge, Double maxAge, String timeUnit) throws URISyntaxException {
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
        TerminologyCode termCode = new TerminologyCode("age", "mii.abide", "SomeDisplay");
        ageTestCrit.setTermCodes(List.of(termCode));
        String searchString = SearchQueryStringBuilder.constructQueryString(ageTestCrit);
        return searchString;
    }

    private String getUpdatedDate(String inputDateStrng){
        LocalDate originDate = LocalDate.parse("2022-11-16");
        Period timeSinceOrigin = Period.between(originDate, LocalDate.now());

        LocalDate inputDate = LocalDate.parse(inputDateStrng);
        inputDate = inputDate.plus(timeSinceOrigin);

        return inputDate.toString();
    }
}
