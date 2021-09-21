import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.xml.CriteriaGroup;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created by Lukas Szimtenings on 6/25/2021.
 */
public class Test
{
    public static int counter = 0;
    
    public static void main(String[] args) throws JAXBException
    {
        Query query = new Query();
        query.setInclusionCriteria(new Criterion[][]{createCriteriaConjunction(), createCriteriaConjunction()});
        query.setExclusionCriteria(new Criterion[][]{createCriteriaConjunction(), createCriteriaConjunction()});
        
        XMLSerialiser serialiser = new XMLSerialiser();
        System.out.println(serialiser.QueryToXmlString(query));
    }
    
    public static int getAndInc(){
        return counter++;
    }
    
    public static Criterion[] createCriteriaConjunction(){
        CriteriaGroup conj = new CriteriaGroup();
        conj.setCriteria(createCriteriaArray());
        return createCriteriaArray();
    }

    private static Criterion[] createCriteriaArray() {
        return new Criterion[]{createCriterion(), createCriterion(), createCriterion()};
    }

    public static Criterion createCriterion(){
        Criterion criterion = new Criterion();
        criterion.setTermCode(createTermCode());
        criterion.setValueFilter(createFilter());
        return criterion;
    }
    
    public static TerminologyCode createTermCode(){
        TerminologyCode termCode = new TerminologyCode();
        termCode.setCode("Code"+getAndInc());
        termCode.setSystem("System"+getAndInc());
        return termCode;
    }
    
    public static ValueFilter createFilter(){
        ValueFilter filter = new ValueFilter();
        filter.setType(FilterType.QUANTITY_COMPARATOR);
        filter.setComparator(Comparator.eq);
        filter.setValue((double)getAndInc());
        return filter;
    }
    
    public static class XMLSerialiser
    {
        private final JAXBContext jc;
        private final Unmarshaller unmarshaller;
        private final Marshaller marshaller;
        
        public XMLSerialiser() throws JAXBException
        {
            this.jc = JAXBContext.newInstance(Comparator.class, CriteriaGroup.class, Criterion.class,
                    FilterType.class, Query.class, TerminologyCode.class, ValueFilter.class);
            this.unmarshaller = this.jc.createUnmarshaller();
            this.marshaller = this.jc.createMarshaller();
            this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        }
        
        /**
         * Deserializes a Query object from it's xml representation
         *
         * @param xml XML-representation of a Query object
         * @return Query-Object
         * @throws JAXBException when something goes wrong during unmarshalling
         * @see StringReader , Unmarshaller.unmarshall()
         */
        Query xmlStringToQuery(String xml) throws JAXBException {
            return (Query) unmarshaller.unmarshal(new StringReader(xml));
        }
    
        public String QueryToXmlString(Query query) throws JAXBException
        {
            StringWriter strWriter = new StringWriter();
            marshaller.marshal(query, strWriter);
            return strWriter.toString();
        }
    }
    
}
