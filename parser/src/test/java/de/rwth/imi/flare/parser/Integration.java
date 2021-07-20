package de.rwth.imi.flare.parser;

import de.rwth.imi.flare.api.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.xml.transform.TransformerConfigurationException;
import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class Integration {
    private Helpers helpers;

    @BeforeAll
    public void setUp() {
        this.helpers = new Helpers();
    }

    @Test
    public void testParser() throws IOException, TransformerConfigurationException {
        I2b2Parser parser = new I2b2Parser();

        String request = helpers.readResourceIntoString("i2b2_request.xml");
        Query parsed = parser.parse(request);

        TerminologyCode male_terminology = new TerminologyCode("\\\\i2b2_DEMO\\i2b2\\Demographics\\Gender\\Male\\", "i2b2_sim", "Male");
        Criterion criterion1 = new Criterion(male_terminology,
                null, null);//new ValueFilter(FilterType.CONCEPT, new TerminologyCode[]{male_terminology}, null, null, null, null, null, null));
        CriteriaGroup criteriaGroup1 = new CriteriaGroup(new Criterion[]{criterion1});

        criterion1 = new Criterion(
                new TerminologyCode("\\\\i2b2_DIAG\\i2b2\\Measurements\\Lymphozyten\\", "i2b2_sim", "Lymphozyten"),
                new ValueFilter(FilterType.QUANTITY_COMPARATOR, null, Comparator.LT, 40.0, "%", null, null), null);
        Criterion criterion2 = new Criterion(
                new TerminologyCode("\\\\i2b2_DIAG\\i2b2\\Measurements\\Lymphozyten_absolut\\", "i2b2_sim", "Lymphozyten - absolut"),
                new ValueFilter(FilterType.QUANTITY_COMPARATOR, null, Comparator.LT, 3.0, "/nl", null, null), null);
        CriteriaGroup criteriaGroup2 = new CriteriaGroup(new Criterion[]{criterion1, criterion2});


        criterion1 = new Criterion(new TerminologyCode("\\\\i2b2_DIAG\\i2b2\\Measurements\\Bilirubin\\", "i2b2_sim", "Bilirubin (gesamt)"),
                new ValueFilter(FilterType.QUANTITY_COMPARATOR, null, Comparator.LT, 8.0, "mg/dl", null, null), null);
        criterion2 = new Criterion(new TerminologyCode("\\\\i2b2_DIAG\\i2b2\\Measurements\\Bilirubin_direkt\\", "i2b2_sim", "Bilirubin (direkt)"),
                new ValueFilter(FilterType.QUANTITY_COMPARATOR, null, Comparator.LT, 6.0, "mg/dl", null, null), null);
        CriteriaGroup criteriaGroup3 = new CriteriaGroup(new Criterion[]{criterion1, criterion2});

        Query expectedResult = new Query();
        expectedResult.setInclusionCriteria(new CriteriaGroup[]{criteriaGroup1, criteriaGroup2, criteriaGroup3});
        Assertions.assertEquals(expectedResult, parsed);
    }
}
