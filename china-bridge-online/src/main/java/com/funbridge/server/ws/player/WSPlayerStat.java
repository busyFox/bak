package com.funbridge.server.ws.player;

import com.funbridge.server.player.data.PlayerDuelStat;

public class WSPlayerStat {
    public int nbDealPlayed = 0;
    public int nbDuelWin = 0;
    public int nbDuelLost = 0;
    public int nbDuelDraw = 0;
    public int nbDuelPlayed = 0;
    public String serieBest = null;
    public int serieBestRank = 0;
    public long serieBestPeriodStart = 0;
    public long serieBestPeriodEnd = 0;

    public String toString() {
        return "nbDealPlayed=" + nbDealPlayed + " - nbDuelWin=" + nbDuelWin + " - nbDuelLost=" + nbDuelLost + " - nbDuelDraw=" + nbDuelDraw + " - nbDuelPlayed=" + nbDuelPlayed;
    }

    public void setDuelStat(PlayerDuelStat stat) {
        if (stat != null) {
            nbDuelPlayed = stat.nbPlayed;
            nbDuelWin = stat.nbWin;
            nbDuelLost = stat.nbLost;
            if (nbDuelPlayed > (nbDuelWin + nbDuelLost)) {
                nbDuelDraw = nbDuelPlayed - (nbDuelWin + nbDuelLost);
            }
        }
    }
}
