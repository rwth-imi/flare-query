package de.rwth.imi.flare.api.model.xml;

import de.rwth.imi.flare.api.model.Criterion;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class CriteriaGroupAdapter extends XmlAdapter<CriteriaGroup, Criterion[]> {
    @Override
    public Criterion[] unmarshal(CriteriaGroup group) {
        return group.getCriteria();
    }

    @Override
    public CriteriaGroup marshal(Criterion[] criteria) {
        return new CriteriaGroup(criteria);
    }
}
