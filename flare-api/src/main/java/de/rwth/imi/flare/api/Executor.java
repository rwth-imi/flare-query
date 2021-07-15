package de.rwth.imi.flare.api;

import de.rwth.imi.flare.api.model.Query;

import java.util.concurrent.CompletableFuture;

/**
 * Created by Lukas Szimtenings on 6/2/2021.
 */
public interface Executor
{
    public CompletableFuture<Integer> calculatePatientCount(Query mappedQuery);
}
