package com.funbridge.server.ws.team.param;

/**
 * Created by bplays on 30/11/16.
 */
public class GetPeriodResultsParam {
    public String teamID;
    public String periodID;

    public String toString() {
        return "teamID=" + teamID + " - periodID=" + periodID;
    }

    public boolean isValid() {
        return teamID != null && !teamID.isEmpty();
    }
}
