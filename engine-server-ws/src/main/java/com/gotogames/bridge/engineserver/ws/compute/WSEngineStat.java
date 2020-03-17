package com.gotogames.bridge.engineserver.ws.compute;

import com.gotogames.bridge.engineserver.common.Constantes;

/**
 * Created by pserent on 13/06/2016.
 */
public class WSEngineStat {
    public long totalRequest = 0; // total request
    public long averageNbRequestHour = 0; // nb request computed / hour
    public double averageTimeRequest = 0; // average time in ms to compute a request
    public long date = 0;

    public String toString() {
        return "date="+ Constantes.timestamp2StringDateHour(date)+" - totalRequest="+totalRequest+" - averageNbRequestHour="+averageNbRequestHour+" - averageTimeRequest="+averageTimeRequest;
    }
}
