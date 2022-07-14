package de.rwth.imi.flare.executor;

import java.util.Date;
import java.util.Set;

public class CacheEntry {
    private Set<String> resultSet;
    private Date lastUpdated;

    public CacheEntry(Set<String> resultSet, Date lastUpdated) {
        this.resultSet = resultSet;
        this.lastUpdated = lastUpdated;
    }

    public Set<String> getResultSet() {
        return resultSet;
    }

    public void setResultSet(Set<String> resultSet) {
        this.resultSet = resultSet;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
