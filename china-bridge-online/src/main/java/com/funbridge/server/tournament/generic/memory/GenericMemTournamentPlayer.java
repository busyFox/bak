package com.funbridge.server.tournament.generic.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 27/01/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class GenericMemTournamentPlayer {
    public long playerID;
    public double result = 0;
    public int ranking = -1;
    public int rankingFinished = -1;
    public long dateLastPlay = 0;
    public long dateStart = 0;
    public int nbPlayerFinishWithBestResult = 0;
    public int currentDealIndex = -1;
    @JsonIgnore
    public GenericMemTournament memTour = null;
    public List<String> playedDeals = new ArrayList<>();

    public GenericMemTournamentPlayer(){}

    public GenericMemTournamentPlayer(GenericMemTournament t) {
        this.memTour = t;
    }

    public void initData(GenericMemTournament t) {
        this.memTour = t;
    }

    public String toString() {
        return "playerID="+playerID+" - result="+result+" - ranking="+ranking+" - nbDealPlayed="+ getNbPlayedDeals();
    }

    public int getNbPlayedDeals() {
        return playedDeals.size();
    }

    public void addPlayedDeal(String dealID) {
        if(!playedDeals.contains(dealID)){
            playedDeals.add(dealID);
        }
    }

    /**
     * Check if this player has played at least one deal
     * @return
     */
    public boolean hasPlayedOneDeal() {
        return !playedDeals.isEmpty();
    }

    /**
     * Check if this player has played all deals
     * @return
     */
    public boolean isPlayerFinish() {
        return playedDeals.size() == memTour.getNbDeal();
    }

    /**
     * Return the result on tournament for player.
     * @param useRankFinished
     * @return
     */
    public WSResultTournamentPlayer toWSResultTournamentPlayer(boolean useRankFinished) {
        WSResultTournamentPlayer resultPlayer = new WSResultTournamentPlayer();
        if (dateLastPlay == 0) {
            resultPlayer.setDateLastPlay(dateStart);
        } else {
            resultPlayer.setDateLastPlay(dateLastPlay);
        }
        resultPlayer.setNbDealPlayed(getNbPlayedDeals());
        resultPlayer.setPlayerID(playerID);
        int nbTotalPlayer = memTour.getNbPlayer();
        if (useRankFinished) {
            nbTotalPlayer = memTour.getNbPlayerFinishAll();
        }
        if (useRankFinished) {
            // rank only if player finish tournament
            if (isPlayerFinish()) {
                resultPlayer.setRank(rankingFinished);
            } else {
                if (memTour.nbPlayerFinishAll > 0) {
                    resultPlayer.setRank(nbPlayerFinishWithBestResult+1);
                    nbTotalPlayer++;
                } else {
                    resultPlayer.setRank(-1);
                }
            }
        } else {
            resultPlayer.setRank(ranking);
        }
        resultPlayer.setNbTotalPlayer(nbTotalPlayer);
        resultPlayer.setResult(result);
        resultPlayer.setNbPlayerFinishWithBestResult(nbPlayerFinishWithBestResult);
        resultPlayer.setRankHidden(ranking);
        return resultPlayer;
    }

    public long getDateLastPlay() {
        return dateLastPlay;
    }
}
