package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.Constantes;

/**
 * Created by pserent on 14/03/2016.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSTournamentInProgress {
    public int category;
    public String tourID;
    public long endDate;

    public String toString() {
        return "category="+category+" - tourID="+tourID+" - endDate="+ Constantes.timestamp2StringDateHour(endDate);
    }
}
