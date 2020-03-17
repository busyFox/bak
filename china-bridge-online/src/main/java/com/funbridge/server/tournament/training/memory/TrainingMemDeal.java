package com.funbridge.server.tournament.training.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.training.data.TrainingGame;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bplays on 21/04/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TrainingMemDeal {
    public int index;
    public String dealID;
    public int nbScore = 0;
    public int nbScoreTotal = 0; // including SCORE_LEAVE
    public int scoreCumul = 0;
    public Map<Long, TrainingMemDealPlayer> ranking = new ConcurrentHashMap<>();

    @JsonIgnore
    private TrainingMemTournament memTournament = null;

    public TrainingMemDeal(){}

    public TrainingMemDeal(TrainingMemTournament memTournament, int index){
        this.memTournament = memTournament;
        this.index = index;
        this.dealID = ContextManager.getTrainingMgr().buildDealID(memTournament.getTournamentID(), index);
    }

    public String toString() {
        return "ID="+dealID+" - index="+index+" - nbScore="+nbScore+" - nbScoreTotal="+nbScoreTotal+" - scoreAverage="+getAverageScore();
    }

    public int getAverageScore() {
        if (nbScore == 0) {
            return 0;
        }
        return scoreCumul/nbScore;
    }

    /**
     * Compute nbScore & scoreCumul (only for score != GAME_SCORE_LEAVE)
     */
    public void computeScoreData() {
        int tempCumul = 0, tempNbScore = 0, tempNbScoreTotal = 0;
        for (Map.Entry<Long, TrainingMemDealPlayer> e : ranking.entrySet()) {
            TrainingMemDealPlayer v = e.getValue();
            if (v.score != Constantes.GAME_SCORE_LEAVE) {
                tempNbScore++;
                tempCumul += v.score;
            }
            tempNbScoreTotal++;
        }
        nbScore = tempNbScore;
        scoreCumul = tempCumul;
        nbScoreTotal = tempNbScoreTotal;
    }

    /**
     * Compute the ranking on this deal.
     */
    public void computeRanking() {
        List<TrainingMemDealPlayer> res = getResultsOrderedByScore();
        // count nb same score
        Map<Integer, Integer> mapScore = new HashMap<Integer, Integer>();
        for (TrainingMemDealPlayer e : res) {
            int currentValue = 0;
            if (mapScore.containsKey(e.score)) {
                currentValue = mapScore.get(e.score);
            }
            mapScore.put(e.score, new Integer(currentValue+1));
        }

        // loop on all result on this deal
        int nbBetterScore = 0;
        int currentScore = 0;
        for (int i = 0; i < res.size(); i++) {
            TrainingMemDealPlayer temp = res.get(i);
            if (i > 0) {
                if ((temp.score != currentScore) && (mapScore.get(currentScore) != null)) {
                    nbBetterScore += mapScore.get(currentScore);
                }
            }
            currentScore = temp.score;
            temp.nbPlayersBetterScore = nbBetterScore;
            temp.nbPlayersSameScore = mapScore.get(temp.score);

            // update the result value
            temp.result = memTournament.computeResultDealPlayer(this, temp);
        }
    }

    public int getNbPlayers() {
        return ranking.size();
    }

    public TrainingMemDealPlayer getResultForPlayer(long playerId){
       return ranking.get(playerId);
    }

    public TrainingMemDealPlayer setResultForPlayer(TrainingGame game){
        if (game != null) {
            TrainingMemDealPlayer resultPlayer = getResultForPlayer(game.getPlayerID());
            if (resultPlayer != null) {
                memTournament.getLog().error("Result for player already exist ... keep it : resultPlayer={" + resultPlayer + "} - game={" + game + "}");
            }
            else {
                if (!game.isLeaved()) {
                    scoreCumul += game.getScore();
                    nbScore++;
                }
                nbScoreTotal++;
                resultPlayer = new TrainingMemDealPlayer();
                resultPlayer.score = game.getScore();
                resultPlayer.playerID = game.getPlayerID();
                resultPlayer.contract = game.getContract();
                resultPlayer.contractType = game.getContractType();
                resultPlayer.declarer = Character.toString(game.getDeclarer());
                resultPlayer.nbTricks = game.getTricks();
                resultPlayer.gameID = game.getIDStr();
                resultPlayer.begins = game.getBegins();

                if (!game.isLeaved()) {
                    // loop on all result on this deal
                    for (Map.Entry<Long, TrainingMemDealPlayer> e : ranking.entrySet()) {
                        TrainingMemDealPlayer temp = e.getValue();
                        // update the player position
                        if (temp.score < game.getScore()) {
                            temp.nbPlayersBetterScore = temp.nbPlayersBetterScore + 1;
                        } else if (temp.score == game.getScore()) {
                            temp.nbPlayersSameScore = temp.nbPlayersSameScore + 1;
                            resultPlayer.nbPlayersSameScore = temp.nbPlayersSameScore;
                        } else {
                            resultPlayer.nbPlayersBetterScore = resultPlayer.nbPlayersBetterScore + 1;
                        }
                        // update the result value
                        temp.result = memTournament.computeResultDealPlayer(this, temp);
                    }
                    // set result value for this new player on this deal
                    resultPlayer.result = memTournament.computeResultDealPlayer(this, resultPlayer);
                } else {
                    resultPlayer.result = memTournament.computeResultDealPlayer(this, resultPlayer);
                    for (Map.Entry<Long, TrainingMemDealPlayer> e : ranking.entrySet()) {
                        TrainingMemDealPlayer temp = e.getValue();
                        if (temp.contractType == Constantes.CONTRACT_LEAVE) {
                            temp.nbPlayersSameScore = temp.nbPlayersSameScore + 1;
                            resultPlayer.nbPlayersSameScore = temp.nbPlayersSameScore;
                        } else {
                            resultPlayer.nbPlayersBetterScore = resultPlayer.nbPlayersBetterScore + 1;
                        }
                    }
                }
                // add this result player to the list of result
                ranking.put(resultPlayer.playerID, resultPlayer);
            }
            return resultPlayer;
        } else {
            memTournament.getLog().error("Game is null !");
        }
        return null;
    }

    public TrainingMemDealPlayer setNotPlayedResultForPlayer(long playerID){
        TrainingMemDealPlayer resultPlayer = ranking.get(playerID);
        if (resultPlayer != null) {
            ContextManager.getTrainingMgr().getLogger().error("Result for player already exist ... keep it : resultPlayer={"+resultPlayer+"}");
        }
        else {
            resultPlayer = new TrainingMemDealPlayer();
            resultPlayer.score = Constantes.GAME_SCORE_LEAVE;
            resultPlayer.playerID = playerID;
            resultPlayer.contractType = Constantes.CONTRACT_LEAVE;
            // add the result player to the map
            ranking.put(resultPlayer.playerID, resultPlayer);
            // loop on all result and update the ranking & result
            for (TrainingMemDealPlayer temp : ranking.values()) {
                if (temp.playerID == playerID) {
                    // don't process the
                    continue;
                }
                // update the player position
                if (temp.score < resultPlayer.score) {
                    temp.nbPlayersBetterScore = temp.nbPlayersBetterScore + 1;
                } else if (temp.score == resultPlayer.score) {
                    temp.nbPlayersSameScore = temp.nbPlayersSameScore + 1;
                    resultPlayer.nbPlayersSameScore = temp.nbPlayersSameScore;
                } else {
                    resultPlayer.nbPlayersBetterScore = resultPlayer.nbPlayersBetterScore + 1;
                }
                // update the result value
                temp.result = Constantes.computeResultPaire(ranking.size(), temp.nbPlayersBetterScore, temp.nbPlayersSameScore);
            }
            // set result value for this new player on this deal
            resultPlayer.result = Constantes.computeResultPaire(ranking.size(), resultPlayer.nbPlayersBetterScore, resultPlayer.nbPlayersSameScore);
        }
        return resultPlayer;
    }

    public List<TrainingMemDealPlayer> getResultsOrderedByScore() {
        List<TrainingMemDealPlayer> listResult = new ArrayList<>(ranking.values());
        Collections.sort(listResult, new Comparator<TrainingMemDealPlayer>() {
            @Override
            public int compare(TrainingMemDealPlayer o1, TrainingMemDealPlayer o2) {
                return o1.compareTo(o2);
            }
        });
        return listResult;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getDealID() {
        return dealID;
    }

    public void setDealID(String dealID) {
        this.dealID = dealID;
    }

    public Map<Long, TrainingMemDealPlayer> getRanking() {
        return ranking;
    }

    public void setRanking(Map<Long, TrainingMemDealPlayer> ranking) {
        this.ranking = ranking;
    }

    public TrainingMemTournament getMemTournament() {
        return memTournament;
    }

    public void setMemTournament(TrainingMemTournament memTournament) {
        this.memTournament = memTournament;
    }

    public boolean removeResultPlayer(long playerID) {
        TrainingMemDealPlayer resultPlayer = ranking.remove(playerID);
        if (resultPlayer != null) {
            // loop an all result and update result
            for (TrainingMemDealPlayer temp : ranking.values()) {
                if (temp.score < resultPlayer.score) {
                    if (temp.nbPlayersBetterScore > 0) {
                        temp.nbPlayersBetterScore = temp.nbPlayersBetterScore - 1;
                    } else if (temp.score == resultPlayer.score) {
                        if (temp.nbPlayersSameScore > 0) {
                            temp.nbPlayersSameScore = temp.nbPlayersSameScore - 1;
                        }
                    }
                    // update the result value
                    temp.result = Constantes.computeResultPaire(ranking.size(), temp.nbPlayersBetterScore, temp.nbPlayersSameScore);
                }
            }
            return true;
        }
        return false;
    }
}
