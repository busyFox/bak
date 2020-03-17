package com.funbridge.server.ws.team.param;

/**
 * Created by bplays on 30/11/16.
 */
public class GetTourResultsParam {
    public String teamID;
    public int tourID;

    public String toString() {
        return "teamID=" + teamID + " - tourID=" + tourID;
    }

    public boolean isValid() {
        if (teamID == null || teamID.isEmpty()) return false;
        return tourID > 0;
    }
}
