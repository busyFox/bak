package com.funbridge.server.tournament.duel.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.player.data.PlayerDuelStat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Document(collection="duel_stat")
public class DuelStat {
    @Id
    public long playerID;
    public long dateLastUpdate = 0;
    public DuelStatResult total = new DuelStatResult();
    public DuelStatResult totalArgine = new DuelStatResult();
    public String countryCode = "";
    /* Map for period -> result */
    public Map<String, DuelStatResult> resultPeriod = new HashMap<>();
    public Map<String, DuelStatResult> resultArginePeriod = new HashMap<>();

    public DuelStatScoring scoring = new DuelStatScoring();

    public DuelStat() {}

    public DuelStat(long playerID) {
        this.playerID = playerID;
    }

    public String toString() {
        return "playerID="+playerID+" - dateLastUpdate="+ Constantes.timestamp2StringDateHour(dateLastUpdate)+" - countryCode="+countryCode+" - total=["+total+"]";
    }

    public void update(long winner, boolean duelWithArgine, String periodID, String previousPeriodID) {
        dateLastUpdate = System.currentTimeMillis();

        // get period result
        DuelStatResult rp = resultPeriod.get(periodID);
        if (rp == null) {
            rp = new DuelStatResult();
            resultPeriod.put(periodID, rp);
        }

        // update total & period result
        rp.nbPlayed++;
        total.nbPlayed++;

        if (winner == playerID) {
            total.nbWin++;
            rp.nbWin++;
        } else {
            if (winner != 0) {
                total.nbLost++;
                rp.nbLost++;
            }
        }

        if (duelWithArgine && playerID != Constantes.PLAYER_ARGINE_ID) {
            DuelStatResult rpArgine = resultArginePeriod.get(periodID);
            if (rpArgine == null) {
                rpArgine = new DuelStatResult();
                resultArginePeriod.put(periodID, rpArgine);
            }
            rpArgine.nbPlayed++;
            totalArgine.nbPlayed++;
            if (winner == playerID) {
                totalArgine.nbWin++;
                rpArgine.nbWin++;
            } else {
                if (winner != 0) {
                    totalArgine.nbLost++;
                    rpArgine.nbLost++;
                }
            }
        }

        // remove data from old periods
        Iterator<String> itResult = resultPeriod.keySet().iterator();
        while (itResult.hasNext()) {
            String key = itResult.next();
            if (!key.equals(periodID) && !key.equals(previousPeriodID)) {
                itResult.remove();
            }
        }
        Iterator<String> itResultArgine = resultArginePeriod.keySet().iterator();
        while (itResultArgine.hasNext()) {
            String key = itResultArgine.next();
            if (!key.equals(periodID) && !key.equals(previousPeriodID)) {
                itResultArgine.remove();
            }
        }
    }

    public void setData(PlayerDuelStat playerDuelStat, DuelArgineStat duelArgineStat) {
        if (playerDuelStat != null) {
            dateLastUpdate = playerDuelStat.dateLastDuel;
            total.nbPlayed = playerDuelStat.nbPlayed;
            total.nbLost = playerDuelStat.nbLost;
            total.nbWin = playerDuelStat.nbWin;
        }
        if (duelArgineStat != null) {
            totalArgine.nbPlayed = duelArgineStat.nbPlayed;
            totalArgine.nbWin = duelArgineStat.nbWon;
            totalArgine.nbLost = duelArgineStat.nbLost;
            if (dateLastUpdate < duelArgineStat.dateLastDuel) {
                dateLastUpdate = duelArgineStat.dateLastDuel;
            }
        }
    }

    public boolean synchronizeData(PlayerDuelStat playerDuelStat, DuelArgineStat duelArgineStat) {
        boolean needUpdate = false;
        if (playerDuelStat != null) {
            if (total.nbLost != playerDuelStat.nbLost) {
                total.nbLost = playerDuelStat.nbLost;
                needUpdate = true;
            }
            if (total.nbWin != playerDuelStat.nbWin) {
                total.nbWin = playerDuelStat.nbWin;
                needUpdate = true;
            }
            if (total.nbPlayed != playerDuelStat.nbPlayed) {
                total.nbPlayed = playerDuelStat.nbPlayed;
                needUpdate = true;
            }
            if (needUpdate) {
                dateLastUpdate = playerDuelStat.dateLastDuel;
            }
        }
        if (duelArgineStat != null) {
            if (totalArgine.nbLost != duelArgineStat.nbLost) {
                totalArgine.nbLost = duelArgineStat.nbLost;
                needUpdate = true;
            }
            if (totalArgine.nbWin != duelArgineStat.nbWon) {
                totalArgine.nbWin = duelArgineStat.nbWon;
                needUpdate = true;
            }
            if (totalArgine.nbPlayed != duelArgineStat.nbPlayed) {
                totalArgine.nbPlayed = duelArgineStat.nbPlayed;
                needUpdate = true;
            }
            if (needUpdate && dateLastUpdate < duelArgineStat.dateLastDuel) {
                dateLastUpdate = playerDuelStat.dateLastDuel;
            }
        }
        return needUpdate;
    }
}
