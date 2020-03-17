package com.funbridge.server.ws.result;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.ws.tournament.WSTournament;

import java.util.List;

/**
 * Created by pserent on 01/07/2014.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class WSSerieSummary2 {
    public long datePeriodStart;
    public long datePeriodEnd;
    public String playerSerie;
    public int nbTournamentPlayed = 0;
    public int nbPlayerSerie;
    public int rank = -1;
    public double resultPlayer = 0;
    public int trend;
    public String trendText;
    public String trendSerie;
    public double thresholdResultUp = -1;
    public double thresholdResultDown = -1;
    public String bonusText;
    public String bonusDescriptionText;
    public List<WSRankingSeriePlayer> rankingExtract;
    public WSTournament currentTournament;
    public int playerOffset = -1;
    public String previousPlayerSerie;
}
