package de.rwth.imi.flare.requestor;
import lombok.Getter;

public class FlareThreadPoolConfig {

    @Getter int corePoolSize;
    @Getter int maxPoolSize;
    @Getter int keepAliveTimeSeconds;


    public FlareThreadPoolConfig(int corePoolSize, int maxPoolSize, int keepAliveTimeSeconds){
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
    }


}
