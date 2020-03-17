package com.funbridge.server.tournament.training.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;

import java.util.HashSet;

/**
 * Created by bplays on 21/04/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TrainingMemTournamentPlayer {
    public long playerID;
    public double result = 0;
    public int ranking = -1;
    public int rankingFinished = -1;
    public long lastPlayDate = 0;
    public long startDate = 0;
    public int nbPlayerWhoFinishedWithBetterResult = 0;
    public int currentDealIndex = -1;
    public HashSet<String> playedDeals = new HashSet<String>();

    @JsonIgnore
    private TrainingMemTournament memTournament = null;

    public TrainingMemTournamentPlayer(){}

    public TrainingMemTournamentPlayer(TrainingMemTournament memTournament){ this.memTournament = memTournament; }

    public void addPlayedDeal(String dealID){
        playedDeals.add(dealID);
    }

    public int getNbPlayedDeals(){
        return playedDeals.size();
    }

    /**
     * Check if this player has played at least one deal
     * @return
     */
    public boolean hasPlayedOneDeal() {
        return (playedDeals.size() > 0);
    }

    /**
     * Check if this player has played all deals
     * @return
     */
    public boolean hasFinished() {
        return playedDeals.size() == memTournament.getNbDeals();
    }

    /**
     * Return the result on tournament for player.
     * @param onlyFinishers
     * @return
     */
    public WSResultTournamentPlayer toWSResultTournamentPlayer(boolean onlyFinishers) {
        WSResultTournamentPlayer resultPlayer = new WSResultTournamentPlayer();
        if (lastPlayDate == 0) {
            resultPlayer.setDateLastPlay(startDate);
        } else {
            resultPlayer.setDateLastPlay(lastPlayDate);
        }
        resultPlayer.setNbDealPlayed(getNbPlayedDeals());
        resultPlayer.setPlayerID(playerID);
        int nbTotalPlayer = memTournament.getNbValidPlayers();
        if (onlyFinishers) {
            nbTotalPlayer = memTournament.getPlayersWhoFinished().size();
        }
        if (onlyFinishers) {
            // rank only if player finish tournament
            if (hasFinished()) {
                resultPlayer.setRank(rankingFinished);
            } else {
                if (memTournament.getPlayersWhoFinished().size() > 0 && FBConfiguration.getInstance().getIntValue("general.rankTournamentAmongNotFinish", 1) == 1) {
                    resultPlayer.setRank(nbPlayerWhoFinishedWithBetterResult +1);
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
        resultPlayer.setNbPlayerFinishWithBestResult(nbPlayerWhoFinishedWithBetterResult);
        resultPlayer.setRankHidden(ranking);
        return resultPlayer;
    }

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public int getRanking() {
        return ranking;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public int getRankingFinished() {
        return rankingFinished;
    }

    public void setRankingFinished(int rankingFinished) {
        this.rankingFinished = rankingFinished;
    }

    public long getLastPlayDate() {
        return lastPlayDate;
    }

    public void setLastPlayDate(long lastPlayDate) {
        this.lastPlayDate = lastPlayDate;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public void setNbPlayersWhoFinishedWithBetterResult(int nbPlayersWhoFinishedWithBetterResult) {
        this.nbPlayerWhoFinishedWithBetterResult = nbPlayersWhoFinishedWithBetterResult;
    }

    public int getCurrentDealIndex() {
        return currentDealIndex;
    }

    public void setCurrentDealIndex(int currentDealIndex) {
        this.currentDealIndex = currentDealIndex;
    }

    public TrainingMemTournament getMemTournament() {
        return memTournament;
    }

    public void setMemTournament(TrainingMemTournament memTournament) {
        this.memTournament = memTournament;
    }

    public HashSet<String> getPlayedDeals() {
        return playedDeals;
    }

    public void setPlayedDeals(HashSet<String> playedDeals) {
        this.playedDeals = playedDeals;
    }
}
