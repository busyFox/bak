package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 19/10/2016.
 */
public class UpdateTeamParam {
    public String description;

    public String toString() {
        return "description=" + description;
    }

    public boolean isValid() {
        return true;
    }
}
