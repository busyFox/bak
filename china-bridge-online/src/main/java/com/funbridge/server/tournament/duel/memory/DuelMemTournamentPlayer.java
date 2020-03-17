package com.funbridge.server.tournament.duel.memory;

import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.HashSet;

/**
 * Created by pserent on 10/07/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class DuelMemTournamentPlayer {
    public long playerID;
    public double result = 0;
    public int ranking = -1;
    public long dateLastPlay = 0;
    public HashSet<String> dealsPlayed = new HashSet<String>();
    public int currentDealIndex = -1;

    public String toString() {
        return "playerID="+playerID+" - result="+result+" - currentDealIndex="+currentDealIndex+" - ranking="+ranking+" - getNbDealsPlayed="+getNbDealsPlayed();
    }

    public int getNbDealsPlayed() {
        return dealsPlayed.size();
    }

    public void addDealPlayed(String dealID) {
        dealsPlayed.add(dealID);
    }

    /**
     * Return the result on tournament for player.
     * @return
     */
    public WSResultTournamentPlayer toWSResultTournamentPlayer() {
        WSResultTournamentPlayer resultPlayer = new WSResultTournamentPlayer();
        resultPlayer.setDateLastPlay(dateLastPlay);
        resultPlayer.setNbDealPlayed(getNbDealsPlayed());
        resultPlayer.setPlayerID(playerID);
        resultPlayer.setRank(ranking);
        resultPlayer.setResult(result);
        return resultPlayer;
    }
}
