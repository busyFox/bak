package com.funbridge.server.ws.result;

import com.funbridge.server.ws.tournament.WSTournament;

import java.util.List;

public class WSTrainingSummary {
    public WSResultStatCategory stat;
    public List<WSTournament> listLastTournament;
    public double cumulResult;
    public int nbPlayedDeal = -1;
}
