import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.executor.BasicAuthRequestorConfig;
import de.rwth.imi.flare.executor.FlareExecutor;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ExecutorTest
{
    BasicAuthRequestorConfig config;
    FlareExecutor executor;

    public ExecutorTest() throws URISyntaxException {
        config = new BasicAuthRequestorConfig(
                new URI("https://localhost:9443/fhir-server/api/v4/"),
                "fhiruser",
                "change-password");
        executor = new FlareExecutor(config);
    }

    @Test
    public void testExecutor() throws ExecutionException, InterruptedException {
        Query query = buildQuery();
        CompletableFuture<Integer> calc = executor.calculatePatientCount(query);
        calc.get();
    }

    private Query buildQuery() {
        TerminologyCode male_terminology = new TerminologyCode("\\\\i2b2_DEMO\\i2b2\\Demographics\\Gender\\Male\\", "i2b2_sim", "Male");
        Criterion criterion1 = new Criterion(male_terminology,
                null, null);//new ValueFilter(FilterType.CONCEPT, new TerminologyCode[]{male_terminology}, null, null, null, null, null, null));
        Criterion[] criteriaGroup1 = new Criterion[]{criterion1};

        criterion1 = new Criterion(
                new TerminologyCode("\\\\i2b2_DIAG\\i2b2\\Measurements\\Lymphozyten\\", "i2b2_sim", "Lymphozyten"),
                new ValueFilter(FilterType.QUANTITY_COMPARATOR, null, Comparator.LT, 40.0, createUnit("%"), null, null), null);
        Criterion criterion2 = new Criterion(
                new TerminologyCode("\\\\i2b2_DIAG\\i2b2\\Measurements\\Lymphozyten_absolut\\", "i2b2_sim", "Lymphozyten - absolut"),
                new ValueFilter(FilterType.QUANTITY_COMPARATOR, null, Comparator.LT, 3.0, createUnit("/nl"), null, null), null);
        Criterion[] criteriaGroup2 = new Criterion[]{criterion1, criterion2};


        criterion1 = new Criterion(new TerminologyCode("\\\\i2b2_DIAG\\i2b2\\Measurements\\Bilirubin\\", "i2b2_sim", "Bilirubin (gesamt)"),
                new ValueFilter(FilterType.QUANTITY_COMPARATOR, null, Comparator.LT, 8.0, createUnit("mg/dl"), null, null), null);
        criterion2 = new Criterion(new TerminologyCode("\\\\i2b2_DIAG\\i2b2\\Measurements\\Bilirubin_direkt\\", "i2b2_sim", "Bilirubin (direkt)"),
                new ValueFilter(FilterType.QUANTITY_COMPARATOR, null, Comparator.LT, 6.0, createUnit("mg/dl"), null, null), null);
        Criterion[] criteriaGroup3 = new Criterion[]{criterion1, criterion2};

        Query expectedResult = new Query();
        expectedResult.setInclusionCriteria(new Criterion[][]{criteriaGroup1, criteriaGroup2, criteriaGroup3});
        return expectedResult;
    }

    public TerminologyCode createUnit(String name){
        return new TerminologyCode(name, null, name);
    }
}
