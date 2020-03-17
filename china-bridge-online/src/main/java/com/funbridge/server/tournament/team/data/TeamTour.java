package com.funbridge.server.tournament.team.data;

import com.funbridge.server.common.Constantes;

/**
 * Created by pserent on 08/11/2016.
 */
public class TeamTour {
    public int index;
    public long dateStart;
    public long dateEnd;
    public boolean finished = false;

    public String toString() {
        return "index="+index+" - finished="+finished+" - dateStart="+ Constantes.timestamp2StringDateHour(dateStart)+" - dateEnd="+ Constantes.timestamp2StringDateHour(dateEnd);
    }

    public boolean isValidForTS(long ts) {
        return (ts > dateStart) && (ts < dateEnd);
    }
}
