package de.rwth.imi.flare.api;

import de.rwth.imi.flare.api.model.Query;
import de.rwth.imi.flare.api.model.mapping.MappingEntry;

import java.util.concurrent.CompletableFuture;

/**
 * A FhirResourceMapper fills in information that is not included in a given query format and thus can not be
 * extrapolated when parsing a query, but is vital for execution and has to be fetched from another source.
 */
public interface FhirResourceMapper
{
    /**
     * Takes a parsed {@link Query} and fills in the missing information by consulting a mapping file/server.<br>
     * Should fill in the {@link de.rwth.imi.flare.api.model.Criterion#setMapping(MappingEntry) mapping} for each
     * {@link de.rwth.imi.flare.api.model.Criterion Criterion}
     *
     * @param query Freshly parsed {@link Query} still missing information required to actually execute it
     * @return {@link Query} that is ready to be executed by the {@link Executor}
     */
    CompletableFuture<Query> mapResources(Query query);
}
