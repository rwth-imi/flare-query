package de.rwth.imi.flare.parser.csq;

import de.rwth.imi.flare.api.model.Criterion;
import lombok.Data;

import java.util.List;

/**
 * Internal Query Object used in preliminary parsing of the Codex Structured Query
 */
@Data
public class CSQQuery {
    private String display;
    private String version;
    private List<List<Criterion>> InclusionCriteria;
    private List<List<Criterion>> ExclusionCriteria;
}
