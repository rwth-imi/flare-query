package de.rwth.imi.flare.api;

import de.rwth.imi.flare.api.model.Criterion;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The Requestor's purpose is to fetch all patients described by a single criterion and parsing them into a
 * {@link FlareResource}.
 */
public interface Requestor {

    /**
     * Executes a search as defined by the given criterion and create a Stream on the results.
     *
     * @param searchCriterion defines the search
     * @return A stream of Resources matching the {@code searchCriterion}
     */
    CompletableFuture<Set<String>> execute(Criterion searchCriterion);

    String translateCriterion(Criterion searchCriterion) throws UnsupportedCriterionException;
}
