import ca.uhn.fhir.context.FhirContext;
import de.rwth.imi.flare.api.FlareResource;
import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;
import de.rwth.imi.flare.requestor.FhirRequestor;
import de.rwth.imi.flare.requestor.FhirSearchRequest;
import de.rwth.imi.flare.requestor.SearchQueryStringBuilder;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class TestAgeRequests {
    @Test
    public void mainAgeTests() throws URISyntaxException {
        /* Suggestion for test data birthdates (relative to 2022-11-14):
        *   A: 1983-05-17
        *   B: 1990-01-01
        *   C: 1990-11-01
        *   D: 2022-10-01
        *   E: 2022-11-01
        * */

        ageSingeComparisonRequest(35.0, "a", Comparator.gt); //should find only A
        ageSingeComparisonRequest(32.0, "a", Comparator.lt); //should find D and E
        ageSingeComparisonRequest(32.53456, "a", Comparator.gt); //should find A, B and C
        ageSingeComparisonRequest(32.0 * 12.0, "mo", Comparator.gt); //should find A, B and C
        ageSingeComparisonRequest(2.0, "mo", Comparator.lt); //should find D and E
        ageSingeComparisonRequest(3.0, "wk", Comparator.lt); //should only find E

        ageRangeRequest(3.0, 10.0, "wk");//should only find D
        ageRangeRequest(32.0*12, 32.0*12+1, "mo");//should only find C
    }


    public void ageSingeComparisonRequest(Double age, String timeUnit, Comparator comparator) throws URISyntaxException{
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

        printFhirRequestResult(searchString);
    }


    public void ageRangeRequest(Double minAge, Double maxAge, String timeUnit) throws URISyntaxException {
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

        printFhirRequestResult(searchString);
    }

    private void printFhirRequestResult(String searchString)throws URISyntaxException{
        String uri = "http://localhost:8082/fhir/" + searchString;
        FhirSearchRequest fhirSearchRequest = new FhirSearchRequest(new URI(uri), "50", FhirContext.forR4());

        System.out.println("SearchString: " + searchString + " | found patients: ");
        while (fhirSearchRequest.hasNext()) {
            FlareResource res = fhirSearchRequest.next();
            System.out.println(res.getPatientId());
        }
        System.out.println("---");
    }
}
