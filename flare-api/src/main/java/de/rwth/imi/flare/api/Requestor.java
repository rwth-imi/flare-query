package de.rwth.imi.flare.api;

import de.rwth.imi.flare.api.model.Criterion;

import java.util.stream.Stream;

/**
 * Created by Lukas Szimtenings on 6/4/2021.
 */
public interface Requestor
{
    public Stream<FlareResource> execute(Criterion search);
}
