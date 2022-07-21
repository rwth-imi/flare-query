package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.model.Criterion;

public interface HeuristicSupplier {
    public int getHeuristic(Criterion criterion);
}
