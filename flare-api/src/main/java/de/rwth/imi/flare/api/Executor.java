package de.rwth.imi.flare.api;

import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.api.model.QueryExpanded;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The Executor asynchronously executes an entire Query and aggregates the returned data
 */
public interface Executor
{
    /**
     * Executes the query, and aggregates the returned patients into a count
     * @param mappedQuery Query with all mapping information
     * @return A CompletableFuture that when executed yields the number of patients matching the given query
     */
    CompletableFuture<Integer> calculatePatientCount(QueryExpanded mappedQuery) throws UnsupportedCriterionException;

    List<List<List<String>>> translateMappedQuery(QueryExpanded mappedQuery) throws UnsupportedCriterionException;
}
