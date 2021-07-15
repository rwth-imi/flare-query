package de.rwth.imi.flare.api;

import de.rwth.imi.flare.api.model.Query;

import java.util.concurrent.CompletableFuture;

/**
 * Created by Lukas Szimtenings on 6/4/2021.
 */
public interface FhirResourceMapper
{
    public CompletableFuture<Query> mapResources(Query query);
}
