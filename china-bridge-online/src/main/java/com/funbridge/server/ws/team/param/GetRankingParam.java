package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 10/11/2016.
 */
public class GetRankingParam {
    public String division;
    public String periodID;
    public int tourID;
    public String group;
    public int offset;
    public int nbMaxResult;

    public String toString() {
        return "division=" + division + " - periodID=" + periodID + " - tourID=" + tourID + " - offset=" + offset + " - nbMaxResult=" + nbMaxResult;
    }

    public boolean isValid() {
        if (division == null || division.isEmpty()) return false; // division obligatoire
        if (periodID == null || periodID.isEmpty()) return false; // periodID obligatoire
        return tourID >= 0; // tourIndex facultatif mais pas inférieur à 1 si fourni
    }

}
