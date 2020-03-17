package com.funbridge.server.tournament.serie.memory;

import com.funbridge.server.common.Constantes;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by pserent on 24/06/2014.
 * Bean to save the result on tournament by a player
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourSerieMemPeriodRankingPlayerData {
    public String tourID;
    public double result;
    public int rank;
    public long dateResult;

    public String toString() {
        return "tourID="+tourID+" - result="+result+" - rank="+rank+" - dateResult="+ Constantes.timestamp2StringDateHour(dateResult);
    }
}
