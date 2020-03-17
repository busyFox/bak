package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by pserent on 03/03/2016.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSTournamentBadges {
    public int series;
    public int timezone;
    public int training;
    public int serieTopChallenge;
    public int serieEasyChallenge;
    public int CBO;
    public int teams;
    public int privateTournament;

    public String toString() {
        return "series="+series+" - timezone="+timezone+" - training="+training+" - serieTopChallenge="+serieTopChallenge+" - serieEasyChallenge="+serieEasyChallenge+" - teams="+teams+" - CBO="+ CBO +" - privateTournament="+privateTournament;
    }
}
