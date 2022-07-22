package de.rwth.imi.flare.executor;

import de.rwth.imi.flare.api.model.Criterion;
import de.rwth.imi.flare.api.model.TerminologyCode;

public class DummyHeuristicSupplier implements HeuristicSupplier {

    @Override
    public int getHeuristic(Criterion criterion) {
        StringBuilder sbTmp = new StringBuilder();
        TerminologyCode termCode = criterion.getTermCodes().get(0);
        sbTmp.append(termCode.getSystem()).append("|").append(termCode.getCode());
        String key = sbTmp.toString();
        int pseudoHash =0;
        for (Character c : key.toCharArray()){
            pseudoHash += (int) c;
        }
        return pseudoHash;
    }
}
