package com.gotogames.bridge.engineserver.ws.servlet;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gotogames.common.tools.NumericalTools;

/**
 * Created by pserent on 26/10/2016.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class EngineWSCommandStatsValues {
    public long start;
    public long elapsed;
    public long count;
    public long compute;

    /**
     * compute time
     * @return
     */
    public double computeComputeTimeRequest() {
        if (count > 0) {
            return NumericalTools.round(compute/count, 2);
        }
        return 0;
    }

    /**
     * compute nb request computed by hour
     * @return
     */
    public long computeNbRequestByHour() {

        return 0;
    }
}
