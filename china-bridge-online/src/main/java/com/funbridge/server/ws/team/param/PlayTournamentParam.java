package com.funbridge.server.ws.team.param;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by pserent on 21/11/2016.
 */
public class PlayTournamentParam {
    public int conventionProfil;
    public String conventionValue = null;
    public int cardsConventionProfil = 0;
    public String cardsConventionValue = null;

    @JsonIgnore
    public boolean isValid() {
        return true;
    }

    @JsonIgnore
    public String toString() {
        return "conventionProfil=" + conventionProfil + " - conventionValue=" + conventionValue + " - cardsConventionProfil=" + cardsConventionProfil + " - cardsConventionValue=" + cardsConventionValue;
    }
}
