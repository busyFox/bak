package com.funbridge.server.tournament.team.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.generic.memory.GenericMemDeal;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.tournament.team.data.TeamGame;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object to store player result on this deal
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TeamMemDeal extends GenericMemDeal<TeamMemDealPlayer> {
    public int dealIndex;
    public String dealID;
    public int nbScore = 0;
    public int nbScoreTotal = 0; // including SCORE_LEAVE
    public int scoreCumul = 0;

    // RESULT FOR PLAYER : playerID => dealResult
    public Map<Long, TeamMemDealPlayer> mapPlayerResult = new ConcurrentHashMap<>();
    @JsonIgnore
    private TeamMemTournament memTour = null;

    public TeamMemDeal(){}

    public String toString() {
        return "DealID="+dealID+" - dealIndex="+dealIndex+" - nbScore="+nbScore+" - nbScoreTotal="+nbScoreTotal+" - nbPlayers="+getNbPlayers();
    }

    public TeamMemDeal(TeamMemTournament memTour, int idx){
        this.memTour = memTour;
        this.dealIndex = idx;
        this.dealID = TourTeamMgr.buildDealID(memTour.tourID, idx);
    }

    public void setMemTour(TeamMemTournament memTour) {
        this.memTour = memTour;
    }

    public TeamMemTournament getMemTour() {
        return memTour;
    }

    public int getNbPlayers() {
        return mapPlayerResult.size();
    }

    public int getScoreAverage() {
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
        for (Map.Entry<Long, TeamMemDealPlayer> e : mapPlayerResult.entrySet()) {
            TeamMemDealPlayer v = e.getValue();
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
        List<TeamMemDealPlayer> res = getResultListOrderByScore();
        // count nb same score
        Map<Integer, Integer> mapScore = new HashMap<Integer, Integer>();
        for (TeamMemDealPlayer e : res) {
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
            TeamMemDealPlayer temp = res.get(i);
            if (i > 0) {
                if ((temp.score != currentScore) && (mapScore.get(currentScore) != null)) {
                    nbBetterScore += mapScore.get(currentScore);
                }
            }
            currentScore = temp.score;
            temp.nbPlayerBetterScore = nbBetterScore;
            temp.nbPlayerSameScore = mapScore.get(temp.score);

            // update the result value
            temp.result = Constantes.computeResultIMP(getScoreAverage(), temp.score);
        }
    }

    /**
     * Return the list of result player on this deal (order by score)
     * @return
     */
    public List<TeamMemDealPlayer> getResultListOrderByScore() {
        List<TeamMemDealPlayer> resultList = new ArrayList<>(mapPlayerResult.values());
        // need to sort list !!
        Collections.sort(resultList, new Comparator<TeamMemDealPlayer>() {
            @Override
            public int compare(TeamMemDealPlayer o1, TeamMemDealPlayer o2) {
                if (o2.score < o1.score) {
                    return -1;
                } else if (o2.score == o1.score) {
                    //return 0;
                    if (o2.contract.length() > 0 && o1.contract.length() > 0) {
                        return o2.contract.substring(0, 1).compareTo(o1.contract.substring(0, 1));
                    }
                    return 0;
                }
                return 1;
            }
        });
        return resultList;
    }

    /**
     * Return result for this player on deal
     * @param playerID
     * @return
     */
    public TeamMemDealPlayer getResultPlayer(long playerID) {
        return mapPlayerResult.get(playerID);
    }

    /**
     * Set result for a player on this deal. Update of result for other player and
     * order the list of result for this deal (by score)
     * @param game
     * @return
     */
    public TeamMemDealPlayer setResultPlayer(TeamGame game) {
        if (game != null) {
            TeamMemDealPlayer resultPlayer = mapPlayerResult.get(game.getPlayerID());
            if (resultPlayer != null) {
                ContextManager.getTourTeamMgr().getLogger().error("MemResultDeal - Result for player already exist ... keep it : resultPlayer={"+resultPlayer+"} - game={"+game+"}");
            }
            else {
                if (!game.isLeaved()) {
                    scoreCumul += game.getScore();
                    nbScore++;
                }
                nbScoreTotal++;
                resultPlayer = new TeamMemDealPlayer();
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
                    for (Map.Entry<Long, TeamMemDealPlayer> e : mapPlayerResult.entrySet()) {
                        TeamMemDealPlayer temp = e.getValue();
                        // update the player position
                        if (temp.score < game.getScore()) {
                            temp.nbPlayerBetterScore++;
                        } else if (temp.score == game.getScore()) {
                            temp.nbPlayerSameScore++;
                            resultPlayer.nbPlayerSameScore = temp.nbPlayerSameScore;
                        } else {
                            resultPlayer.nbPlayerBetterScore++;
                        }
                        // update the result value
                        temp.result = Constantes.computeResultIMP(getScoreAverage(), temp.score);
                    }
                    // set result value for this new player on this deal
                    resultPlayer.result = Constantes.computeResultIMP(getScoreAverage(), resultPlayer.score);
                } else {
                    resultPlayer.result = Constantes.computeResultIMP(getScoreAverage(), resultPlayer.score);
                    for (Map.Entry<Long, TeamMemDealPlayer> e : mapPlayerResult.entrySet()) {
                        TeamMemDealPlayer temp = e.getValue();
                        if (temp.contractType == Constantes.CONTRACT_LEAVE) {
                            temp.nbPlayerSameScore++;
                            resultPlayer.nbPlayerSameScore = temp.nbPlayerSameScore;
                        } else {
                            resultPlayer.nbPlayerBetterScore++;
                        }
                    }
                }
                // add the result player to the map
                mapPlayerResult.put(resultPlayer.playerID, resultPlayer);
            }
            return resultPlayer;
        } else {
            ContextManager.getTourSerieMgr().getLogger().error("Game is null on setResultPlayer !");
        }
        return null;
    }

    /**
     * Set result not played (leave) for a player
     * @param playerID
     * @return
     */
    public TeamMemDealPlayer setResultPlayerNotPlayed(long playerID) {
        TeamMemDealPlayer resultPlayer = mapPlayerResult.get(playerID);
        if (resultPlayer != null) {
            ContextManager.getTourSerieMgr().getLogger().error("MemResultDeal - Result for player already exist ... keep it : resultPlayer={"+resultPlayer+"}");
        }
        else {
            resultPlayer = new TeamMemDealPlayer();
            resultPlayer.score = Constantes.GAME_SCORE_LEAVE;
            resultPlayer.playerID = playerID;
            resultPlayer.contractType = Constantes.CONTRACT_LEAVE;
            // add the result player to the map
            mapPlayerResult.put(resultPlayer.playerID, resultPlayer);
            // loop on all result and update the ranking & result
            for (TeamMemDealPlayer temp : mapPlayerResult.values()) {
                if (temp.playerID == playerID) {
                    // don't process the
                    continue;
                }
                // update the player position
                if (temp.score < resultPlayer.score) {
                    temp.nbPlayerBetterScore++;
                } else if (temp.score == resultPlayer.score) {
                    temp.nbPlayerSameScore++;
                    resultPlayer.nbPlayerSameScore = temp.nbPlayerSameScore;
                } else {
                    resultPlayer.nbPlayerBetterScore++;
                }
                // update the result value
                temp.result = Constantes.computeResultIMP(getScoreAverage(), temp.score);
            }
            // set result value for this new player on this deal
            resultPlayer.result = Constantes.computeResultIMP(getScoreAverage(), resultPlayer.score);
        }
        return resultPlayer;
    }

    /**
     * Return the players list who started the tournament but not played this deal
     * @return
     */
    public List<Long> getListPlayerStartedTournamentWithNoResult() {
        List<Long> listPlayer = memTour.getListPlayer();
        listPlayer.removeAll(new ArrayList<>(mapPlayerResult.keySet()));
        return listPlayer;
    }
}
