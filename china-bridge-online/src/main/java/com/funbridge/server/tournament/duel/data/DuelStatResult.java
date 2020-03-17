package com.funbridge.server.tournament.duel.data;

public class DuelStatResult {
    public int nbPlayed = 0;
    public int nbWin = 0;
    public int nbLost = 0;

    public String toString() {
        return "nbPlayed="+nbPlayed+" - nbWin="+nbWin+" - nbLost="+nbLost;
    }
}
