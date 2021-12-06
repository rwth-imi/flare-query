import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.api.model.CriteriaGroup;
import jakarta.xml.bind.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Lukas Szimtenings on 6/25/2021.
 */
public class Test
{
    public static int counter = 0;
    
    public static void main(String[] args) throws JAXBException
    {
        Query query = new Query();
        //query.setInclusionCriteria(new ArrayList<>(List.of(createCriteriaArray(), createCriteriaArray())));
        //query.setExclusionCriteria(new ArrayList<>(List.of(createCriteriaArray(), createCriteriaArray())));
        
        XMLSerialiser serialiser = new XMLSerialiser();
        System.out.println(serialiser.QueryToXmlString(query));

/*        CriteriaGroup criteriaGroup = new CriteriaGroup(query.getInclusionCriteria().get(0));
        StringWriter strWriter = new StringWriter();
        serialiser.marshaller.marshal(criteriaGroup, strWriter);
        System.out.println(strWriter);


        CriteriaGroup criteriaGroup2 = new CriteriaGroup(query.getInclusionCriteria().get(1));
        ArrayList<CriteriaGroup> criteriaGroups = new ArrayList<>();
        criteriaGroups.add(criteriaGroup);
        criteriaGroups.add(criteriaGroup2);
        strWriter = new StringWriter();
        serialiser.marshaller.marshal(new InclusionCriteria(criteriaGroups), strWriter);
       //System.out.println(strWriter);

        I2B2Query i2b2Query = new I2B2Query();
        i2b2Query.setInclusionCriteria(criteriaGroups);
        i2b2Query.setExclusionCriteria(criteriaGroups);
        strWriter = new StringWriter();
        serialiser.marshaller.marshal(i2b2Query, strWriter);
        //System.out.println(strWriter);

        /*
        strWriter = new StringWriter();
        serialiser.marshaller.marshal(new ExclusionCriteria(criteriaGroups), strWriter);
        System.out.println(strWriter);
        */
    }
    
    public static int getAndInc(){
        return counter++;
    }

    private static ArrayList<Criterion> createCriteriaArray() {
        return new ArrayList<>(List.of(createCriterion(), createCriterion(), createCriterion()));
    }

    public static Criterion createCriterion(){
        Criterion criterion = new Criterion();
        criterion.setTermCode(createTermCode());
        criterion.setValueFilter(createFilter());
        return criterion;
    }
    
    public static List<TerminologyCode> createTermCode(){
        List<TerminologyCode> termCode = Arrays.asList(new TerminologyCode());
        termCode.get(0).setCode("Code"+getAndInc());
        termCode.get(0).setSystem("System"+getAndInc());
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
