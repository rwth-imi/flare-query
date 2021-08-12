package de.rwth.imi.flare.parser.csq;

import de.rwth.imi.flare.api.FlareParser;
import de.rwth.imi.flare.api.model.Query;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class Integration {
    Helpers helper = new Helpers();

    @Test()
    public void test() throws IOException {
        FlareParser parser = new ParserCSQ();
        String testQuery = helper.readResourceIntoString("csq_example.json");
        Query actual = parser.parse(testQuery);
        System.out.println(actual);
    }
}
