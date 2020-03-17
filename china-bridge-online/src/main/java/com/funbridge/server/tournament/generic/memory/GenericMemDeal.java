package com.funbridge.server.tournament.generic.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pserent on 27/01/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class GenericMemDeal<TMemDealPlayer extends GenericMemDealPlayer> {
    public int dealIndex;
    public String dealID;
    public int nbScore = 0;
    public int nbScoreTotal = 0; // including SCORE_LEAVE
    public int scoreCumul = 0;
    // RESULT FOR PLAYER : playerID => dealResult
    public Map<Long, TMemDealPlayer> mapPlayerResult = new ConcurrentHashMap<>();

    @JsonIgnore
    private GenericMemTournament memTour = null;

    public GenericMemDeal(){}

    public String toString() {
        return "DealID="+dealID+" - dealIndex="+dealIndex+" - nbScore="+nbScore+" - nbScoreTotal="+nbScoreTotal+" - nbPlayers="+getNbPlayers();
    }

    public GenericMemDeal(GenericMemTournament memTour, int idx){
        this.initData(memTour, idx);
    }

    public void initData(GenericMemTournament memTour, int idx) {
        this.memTour = memTour;
        this.dealIndex = idx;
        this.dealID = TournamentGenericMgr.buildDealID(memTour.tourID, idx);
    }

    public void setMemTour(GenericMemTournament memTour) {
        this.memTour = memTour;
    }

    public GenericMemTournament getMemTour() {
        return memTour;
    }

    /**
     * Return result for this player on deal
     * @param playerID
     * @return
     */
    public TMemDealPlayer getResultPlayer(long playerID) {
        return mapPlayerResult.get(playerID);
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
        for (Map.Entry<Long, TMemDealPlayer> e : mapPlayerResult.entrySet()) {
            GenericMemDealPlayer v = e.getValue();
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
     * Set result for a player on this deal. Update of result for other player and
     * order the list of result for this deal (by score)
     * @param game
     * @return
     */
    public GenericMemDealPlayer setResultPlayer(Game game) {
        if (game != null) {
            TMemDealPlayer resultPlayer = getResultPlayer(game.getPlayerID());
            if (resultPlayer != null) {
                memTour.getLog().error("MemResultDeal - Result for player already exist ... keep it : resultPlayer={" + resultPlayer + "} - game={" + game + "}");
            }
            else {
                if (!game.isLeaved()) {
                    scoreCumul += game.getScore();
                    nbScore++;
                }
                nbScoreTotal++;
                resultPlayer = (TMemDealPlayer)memTour.getMemoryMgr().createMemDealPlayer();
//                resultPlayer = new GenericMemDealPlayer();
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
                    for (Map.Entry<Long, TMemDealPlayer> e : mapPlayerResult.entrySet()) {
                        GenericMemDealPlayer temp = e.getValue();
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
                        temp.result = memTour.computeResultDealPlayer(this, temp);
//						}
                    }
                    // set result value for this new player on this deal
                    resultPlayer.result = memTour.computeResultDealPlayer(this, resultPlayer);
                } else {
                    resultPlayer.result = memTour.computeResultDealPlayer(this, resultPlayer);
                    for (Map.Entry<Long, TMemDealPlayer> e : mapPlayerResult.entrySet()) {
                        GenericMemDealPlayer temp = e.getValue();
                        if (temp.contractType == Constantes.CONTRACT_LEAVE) {
                            temp.nbPlayerSameScore++;
                            resultPlayer.nbPlayerSameScore = temp.nbPlayerSameScore;
                        } else {
                            resultPlayer.nbPlayerBetterScore++;
                        }
                    }
                }
                // add this result player to the list of result
                mapPlayerResult.put(resultPlayer.playerID, resultPlayer);
            }
            return resultPlayer;
        } else {
            memTour.getLog().error("MemResultDeal - Game is null !");
        }
        return null;
    }

    /**
     * Return the list of result player on this deal (order by score)
     * @return
     */
    public List<TMemDealPlayer> getResultListOrderByScore() {
        List<TMemDealPlayer> resultList = new ArrayList<>(mapPlayerResult.values());
        // need to sort list !!
        Collections.sort(resultList, new Comparator<GenericMemDealPlayer>() {
            @Override
            public int compare(GenericMemDealPlayer o1, GenericMemDealPlayer o2) {
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
     * Compute the ranking on this deal.
     */
    public void computeRanking() {
        List<TMemDealPlayer> res = getResultListOrderByScore();
        // count nb same score
        Map<Integer, Integer> mapScore = new HashMap<>();
        for (GenericMemDealPlayer e : res) {
            int currentValue = 0;
            if (mapScore.containsKey(e.score)) {
                currentValue = mapScore.get(e.score);
            }
            mapScore.put(e.score, currentValue+1);
        }

        // loop on all result on this deal
        int nbBetterScore = 0;
        int currentScore = 0;
        for (int i = 0; i < res.size(); i++) {
            GenericMemDealPlayer temp = res.get(i);
            if (i > 0) {
                if ((temp.score != currentScore) && (mapScore.get(currentScore) != null)) {
                    nbBetterScore += mapScore.get(currentScore);
                }
            }
            currentScore = temp.score;
            temp.nbPlayerBetterScore = nbBetterScore;
            temp.nbPlayerSameScore = mapScore.get(temp.score);

            // update the result value
            temp.result = memTour.computeResultDealPlayer(this, temp);
        }
    }

    /**
     * Return the players list who stared the tournament but not played this deal
     * @return
     */
    public List<Long> getListPlayerStartedTournamentWithNoResult() {
        List<Long> listResult = memTour.getListPlayer();
        listResult.removeAll(new ArrayList<>(mapPlayerResult.keySet()));
        return listResult;
    }

    public boolean removeResultPlayer(long playerID) {
        GenericMemDealPlayer resultPlayer = mapPlayerResult.remove(playerID);
        if (resultPlayer != null) {
            // loop an all result and update result
            for (GenericMemDealPlayer temp : mapPlayerResult.values()) {
                if (temp.score < resultPlayer.score) {
                    if (temp.nbPlayerBetterScore > 0) {
                        temp.nbPlayerBetterScore--;
                    } else if (temp.score == resultPlayer.score) {
                        if (temp.nbPlayerSameScore > 0) {
                            temp.nbPlayerSameScore--;
                        }
                    }
                    // update the result value
                    temp.result = Constantes.computeResultPaire(mapPlayerResult.size(), temp.nbPlayerBetterScore, temp.nbPlayerSameScore);
                }
            }
            return true;
        }
        return false;
    }
}
