package com.funbridge.server.ws.result;

import com.funbridge.server.ws.tournament.WSTournament;

import java.util.List;

public class WSSerieSummary {
    public String playerSerie;
    public int nbTournamentPlayed = 0;
    public int nbPlayerSerie;
    public int rank = -1;
    public double resultPlayer = 0;
    public long datePeriodStart;
    public long datePeriodEnd;
    public String projectionSerie;
    public double projectionValue;
    public WSTournament currentTournament;
    public int nbTournamentNCUp;
    public int nbTournamentUp;
    public int nbTournamentDown;
    public List<Double> resultsTour = null;
    public double thresholdResultUp = -1;
    public double thresholdResultDown = -1;
}
