package com.funbridge.server.ws.player;

import com.funbridge.server.common.Constantes;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "serieStatus")
public class WSSerieStatus {
    public String serie = Constantes.SERIE_NOT_DEFINED; // current serie of player
    public int rank = -1; // rank in the current period
    public int nbPlayerInSerie = 0; // nb player in this serie for the current period
    public double result = 0; // result in the current period
    public List<String> lastSeries = new ArrayList<String>(); // lits of previous serie for this player
    public int trend = -1;
    public int nbTournamentPlayed = 0;
    public String historic = "";

    public String toString() {
        return "serie=" + serie + " - rank=" + rank + " - result=" + result + " - trend=" + trend + " - nbTournamentPlayed=" + nbTournamentPlayed + " - nbPlayerInSerie=" + nbPlayerInSerie;
    }
}
