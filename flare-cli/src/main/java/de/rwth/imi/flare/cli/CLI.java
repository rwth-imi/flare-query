package de.rwth.imi.flare.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.rwth.imi.flare.api.Executor;
import de.rwth.imi.flare.api.FhirResourceMapper;
import de.rwth.imi.flare.api.UnsupportedCriterionException;
import de.rwth.imi.flare.api.model.*;
import de.rwth.imi.flare.mapping.expansion.ExpansionTreeNode;
import de.rwth.imi.flare.mapping.expansion.QueryExpander;
import de.rwth.imi.flare.mapping.lookup.NaiveLookupMapping;
import de.rwth.imi.flare.mapping.lookup.SourceMappingEntry;
import de.rwth.imi.flare.parser.i2b2.ParserI2B2;
import de.rwth.imi.flare.requestor.CacheConfig;
import de.rwth.imi.flare.requestor.FhirRequestor;
import de.rwth.imi.flare.requestor.FhirRequestorConfig;
import de.rwth.imi.flare.requestor.FlareThreadPoolConfig;

import java.util.*;

import java.util.concurrent.*;

import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import de.rwth.imi.flare.api.FlareParser;
import de.rwth.imi.flare.parser.csq.ParserCSQ;
import de.rwth.imi.flare.executor.FlareExecutor;

import javax.xml.transform.TransformerConfigurationException;

//TODO: Change to properties.ini
@Command(name = "flare-cli", mixinStandardHelpOptions = true,
        description = "Executes a given query")
class CLI implements Callable<Integer> {

    @Parameters(index = "0", description = "The request to be executed by FLARE")
    private File requestFile;

    @Parameters(index = "1", description = "FHIR-server base uri")
    private String fhirBaseUri;

    @Option(names = {"-u", "--username"}, description = "FHIR-server username\n if either username or password are missing no authentication will be done")
    private String userName = null;

    @Option(names = {"-p", "--password"}, description = "FHIR-server password\n if either username or password are missing no authentication will be done")
    private String password = null;

    @Option(names = {"-f", "query-format"}, description = "Format of the query to be executed")
    private QueryFormat algorithm = QueryFormat.I2B2;

    @Option(names = {"-c", "pagecount"}, description = "Number of resources per page as result from fhir server")
    private String pagecount = "";

    private Executor executor;

    private final FhirResourceMapper mapping;

    private FlareParser parser;

    public CLI(FhirResourceMapper mapper) {
        createExecutor();
        mapping = mapper;
    }

    public static Map<TerminologyCode, SourceMappingEntry> loadMappingFile()
        throws IOException {

        var lookupTable = new HashMap<TerminologyCode, SourceMappingEntry>();
        ObjectMapper objectMapper = new ObjectMapper().configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<SourceMappingEntry> sourceMappingEntries = objectMapper.readValue(new File("ontology/codex-term-code-mapping.json"), new TypeReference<>() {});
        sourceMappingEntries.forEach(sourceMappingEntry -> lookupTable.put(sourceMappingEntry.getKey(), sourceMappingEntry));
        return lookupTable;
    }

    public static ExpansionTreeNode loadExpansionTree()
        throws IOException {

        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(new File("ontology/codex-code-tree.json"), new TypeReference<>() {});
    }

    public static QueryExpander expander() throws IOException {
        return new QueryExpander(loadExpansionTree());
    }


    public static FhirResourceMapper mapper() throws IOException {
        return new NaiveLookupMapping(loadMappingFile(), expander());
    }

    private void createExecutor() {
        Optional<Authenticator> auth = Optional.ofNullable(createAuthenticator());

        FhirRequestorConfig config = new FhirRequestorConfig() {
            @Override
            public Optional<Authenticator> getAuthentication() {
                return auth;
            }

            @Override
            public URI getBaseURI() {
                URI uri = null;
                try {
                    uri = new URI(fhirBaseUri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                return uri;
            }

            @Override
            public String getPageCount() {
                return pagecount;
            }

            @Override
            public FlareThreadPoolConfig getThreadPoolConfig() {
                return new FlareThreadPoolConfig(4, 16, 10);
            }

        };

        CacheConfig cacheConfig = new CacheConfig() {
            @Override
            public int getHeapEntryCount() {
                return 2;
            }
            @Override
            public int getDiskSizeGB() {
                return 2;
            }
            @Override
            public File getCacheDir() {
                return new File( "target", "EhCacheData");
            }
            @Override
            public int getExpiryHours() {
                return 24;
            }

        };

        // TODO: thread pool should be configurable
        executor = new FlareExecutor(new FhirRequestor(config, cacheConfig, Executors.newFixedThreadPool(16)));
    }

    @Nullable
    private Authenticator createAuthenticator() {
        Authenticator auth = null;
        // TODO: Log warning if only one parameter is provided
        if(this.userName != null && this.password != null){
            auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(
                            userName,
                            password.toCharArray());
                }
            };
        }
        return auth;
    }

    private FlareParser getParser() throws TransformerConfigurationException {
        FlareParser parser = null;
        switch (this.algorithm) {
            case CSQ -> parser = new ParserCSQ();
            case I2B2 -> parser = new ParserI2B2();
        }
        return parser;
    }

    @Override
    public Integer call() {
        try {
            Query parsedQuery = parseQuery();
            ExpandedQuery mappedQuery = mapQuery(parsedQuery);
            int queryResult = executor.calculatePatientCount(mappedQuery).get();
            System.out.println(queryResult);
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            return -2;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return -3;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return -4;
        }
    }

    private ExpandedQuery mapQuery(Query query) {
        ExpandedQuery parsedQuery = new ExpandedQuery();
        // TODO: specific init needed?
        parsedQuery.setInclusionCriteria(query.getInclusionCriteria());
        List<List<CriteriaGroup>> exclusionCriteria = new LinkedList<>();
        for(CriteriaGroup criteriaGroup: query.getExclusionCriteria()){
            List<CriteriaGroup> subCriteria = new LinkedList<>();
            for(Criterion criterion: criteriaGroup.getCriteria()){
                CriteriaGroup newGroup = new CriteriaGroup();
                List<Criterion> criteria = new LinkedList<>();
                criteria.add(criterion);
                newGroup.setCriteria(criteria);
                subCriteria.add(newGroup);
            }
            exclusionCriteria.add(subCriteria);
        }
        parsedQuery.setExclusionCriteria(exclusionCriteria);
        try {
            parsedQuery = mapping.mapResources(query).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return parsedQuery;
    }

    private Query parseQuery() throws IOException, TransformerConfigurationException {
        String query = Files.readString(requestFile.toPath());
        FlareParser parser = getParser();
        return parser.parse(query);
    }

    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new CLI(mapper())).execute(args);
        System.exit(exitCode);
    }
}
