package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 10/11/2016.
 */
public class GetTeamParam {
    public String teamID;

    public String toString() {
        return "teamID=" + teamID;
    }

    public boolean isValid() {
        return teamID != null && teamID.length() != 0;
    }
}
