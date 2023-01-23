package de.rwth.imi.flare.api;

import java.time.LocalDateTime;

/**
 * Created by Lukas Szimtenings on 6/2/2021.
 */
public interface FlareResource
{
    String getPatientId();
    
    // TODO: implement later on
    // public LocalDateTime getRelevantDate();

    // TODO: Determine proper return type
    // public Object getRelevantValue();

    public FlareIdDateWrap getIdDateWrap();

}
