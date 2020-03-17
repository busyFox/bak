package com.funbridge.server.ws.player;

import java.util.ArrayList;
import java.util.List;

public class WSServerParameters {
    public int duelRequestDuration; // (durée d'une invitation pour un défi en heure)
    public int serieNbTournamentNCUp; // (nombre de tournoi pour intégrer les séries depuis NC)
    public int serieNbTournamentUp; // (nombre de tournoi pour monter de série)
    public int serieNbTournamentDown; //int (nombre de tournoi pour descendre de série)
    public boolean webSocketsEnabled = false;
    public boolean httpsWebSockets = false;
    public boolean enableStats = false;
    public String urlStats = null;
    public List<String> rpcServices = new ArrayList<>();
}
