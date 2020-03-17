package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 12/10/2016.
 */
public class SendRequestToTeamParam {
    public String teamID;

    public String toString() {
        return "teamID=" + teamID;
    }

    public boolean isValid() {
        return teamID != null && !teamID.isEmpty();
    }
}
