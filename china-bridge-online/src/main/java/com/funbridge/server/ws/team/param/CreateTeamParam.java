package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 12/10/2016.
 */
public class CreateTeamParam {
    public String name;
    public String description;
    public String countryCode;

    public String toString() {
        return "name=" + name + " - description=" + description + " - countryCode=" + countryCode;
    }

    public boolean isValid() {
        return name != null && !name.isEmpty();
    }
}
