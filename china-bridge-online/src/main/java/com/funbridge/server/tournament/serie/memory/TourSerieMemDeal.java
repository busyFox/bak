package com.funbridge.server.tournament.serie.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.generic.memory.GenericMemDeal;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.TourSerieGame;
import com.funbridge.server.tournament.serie.data.TourSerieGameLoadData;

import java.util.*;

/**
 * Created by pserent on 09/06/2014.
 * Bean to store in memory the deal result
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourSerieMemDeal extends GenericMemDeal<TourSerieMemDealPlayer> {

    @JsonIgnore
    private TourSerieMemTour memTour = null;

    public TourSerieMemDeal(){}

    public TourSerieMemDeal(TourSerieMemTour memTour, int idx){
        this.memTour = memTour;
        this.dealIndex = idx;
        this.dealID = TourSerieMgr.buildDealID(memTour.tourID, idx);
    }

    public void setMemTour(TourSerieMemTour memTour) {
        this.memTour = memTour;
    }

    public int getNbPlayer() {
        return mapPlayerResult.size();
    }

    /**
     * Return the result of this deal for a player
     * @param playerID
     * @return
     */
    @Override
    public TourSerieMemDealPlayer getResultPlayer(long playerID) {
        return mapPlayerResult.get(playerID);
    }

    /**
     * Set result not played (leave) for a player
     * @param playerID
     * @return
     */
    public TourSerieMemDealPlayer setResultPlayerNotPlayed(long playerID) {
        TourSerieMemDealPlayer resultPlayer = mapPlayerResult.get(playerID);
        if (resultPlayer != null) {
            ContextManager.getTourSerieMgr().getLogger().error("MemResultDeal - Result for player already exist ... keep it : resultPlayer={"+resultPlayer+"}");
        }
        else {
            resultPlayer = new TourSerieMemDealPlayer();
            resultPlayer.score = Constantes.GAME_SCORE_LEAVE;
            resultPlayer.playerID = playerID;
            resultPlayer.contractType = Constantes.CONTRACT_LEAVE;
            // add the result player to the map
            mapPlayerResult.put(resultPlayer.playerID, resultPlayer);
            // loop on all result and update the ranking & result
            for (TourSerieMemDealPlayer temp : mapPlayerResult.values()) {
                if (temp.playerID == playerID) {
                    // don't process the
                    continue;
                }
                // update the player position
                if (temp.score < resultPlayer.score) {
                    temp.nbPlayerBestScore++;
                } else if (temp.score == resultPlayer.score) {
                    temp.nbPlayerSameScore++;
                    resultPlayer.nbPlayerSameScore = temp.nbPlayerSameScore;
                } else {
                    resultPlayer.nbPlayerBestScore++;
                }
                // update the result value
                temp.result = Constantes.computeResultPaire(mapPlayerResult.size(), temp.nbPlayerBestScore, temp.nbPlayerSameScore);
            }
            // set result value for this new player on this deal
            resultPlayer.result = Constantes.computeResultPaire(mapPlayerResult.size(), resultPlayer.nbPlayerBestScore, resultPlayer.nbPlayerSameScore);
        }
        return resultPlayer;
    }

    /**
     * Set result for a player on this deal. Update of result for other player and
     * order the list of result for this deal (by score)
     * @param game
     * @return
     */
    public TourSerieMemDealPlayer setResultPlayerFromLoadData(TourSerieGameLoadData game) {
        if (game != null) {
            TourSerieMemDealPlayer resultPlayer = mapPlayerResult.get(game.playerID);
            if (resultPlayer != null) {
                ContextManager.getTourSerieMgr().getLogger().error("MemResultDeal - Result for player already exist ... keep it : resultPlayer={"+resultPlayer+"} - game={"+game+"}");
            }
            else {
                resultPlayer = new TourSerieMemDealPlayer();
                resultPlayer.score = game.score;
                resultPlayer.playerID = game.playerID;
                resultPlayer.contract = game.contract;
                resultPlayer.contractType = game.contractType;
                resultPlayer.declarer = game.declarer;
                resultPlayer.nbTricks = game.tricks;
                resultPlayer.gameID = game.gameID;
                resultPlayer.begins = game.getBegins();

                // add the result player to the map
                mapPlayerResult.put(resultPlayer.playerID, resultPlayer);
            }
            return resultPlayer;
        } else {
            ContextManager.getTourSerieMgr().getLogger().error("Game is null on setResultPlayer !");
        }
        return null;
    }

    public void computeResult() {
        class ScoreGroup {
            public int score;
            public int nbPlayers;
            public int nbPlayersBest;
        }

        Map<Integer, ScoreGroup> mapScore = new HashMap<>();
        // fill map of score with nb players for each score
        for (TourSerieMemDealPlayer temp : mapPlayerResult.values()) {
            ScoreGroup e = mapScore.get(temp.score);
            if (e == null) {
                e = new ScoreGroup();
                e.score = temp.score;
                mapScore.put(temp.score, e);
            }
            e.nbPlayers++;
        }
        // sort iwth at first best result
        List<ScoreGroup> listScore = new ArrayList<>(mapScore.values());
        Collections.sort(listScore, new Comparator<ScoreGroup>() {
            @Override
            public int compare(ScoreGroup o1, ScoreGroup o2) {
                return -Integer.compare(o1.score, o2.score);
            }
        });
        // set nbPlayersBestScore for each element
        int nbPlayersBestScore = 0;
        for (ScoreGroup e : listScore) {
            e.nbPlayersBest = nbPlayersBestScore;
            nbPlayersBestScore += e.nbPlayers;
        }

        for (TourSerieMemDealPlayer temp : mapPlayerResult.values()) {
            ScoreGroup e = mapScore.get(temp.score);
            if (e!=null) {
                temp.nbPlayerSameScore = e.nbPlayers;
                temp.nbPlayerBestScore = e.nbPlayersBest;
                temp.result = Constantes.computeResultPaire(mapPlayerResult.size(), temp.nbPlayerBestScore, temp.nbPlayerSameScore);
            }
        }
    }

    /**
     * Set result for a player on this deal. Update of result for other player and
     * order the list of result for this deal (by score)
     * @param game
     * @return
     */
    public TourSerieMemDealPlayer setResultPlayer(TourSerieGame game) {
        if (game != null) {
            TourSerieMemDealPlayer resultPlayer = mapPlayerResult.get(game.getPlayerID());
            if (resultPlayer != null) {
                ContextManager.getTourSerieMgr().getLogger().error("MemResultDeal - Result for player already exist ... keep it : resultPlayer={"+resultPlayer+"} - game={"+game+"}");
            }
            else {
                resultPlayer = new TourSerieMemDealPlayer();
                resultPlayer.score = game.getScore();
                resultPlayer.playerID = game.getPlayerID();
                resultPlayer.contract = game.getContract();
                resultPlayer.contractType = game.getContractType();
                resultPlayer.declarer = Character.toString(game.getDeclarer());
                resultPlayer.nbTricks = game.getTricks();
                resultPlayer.gameID = game.getIDStr();
                resultPlayer.begins = game.getBegins();

                // add the result player to the map
                mapPlayerResult.put(resultPlayer.playerID, resultPlayer);
                // loop on all result and update result on this deal
                for (TourSerieMemDealPlayer temp : mapPlayerResult.values()) {
                    if (temp.playerID == game.getPlayerID()) {
                        // don't process the player result (it's done at the end)
                        continue;
                    }
                    // update the player position
                    if (temp.score < game.getScore()) {
                        temp.nbPlayerBestScore++;
                    } else if (temp.score == game.getScore()) {
                        temp.nbPlayerSameScore++;
                        resultPlayer.nbPlayerSameScore = temp.nbPlayerSameScore;
                    } else {
                        resultPlayer.nbPlayerBestScore++;
                    }
                    // update the result value
                    temp.result = Constantes.computeResultPaire(mapPlayerResult.size(), temp.nbPlayerBestScore, temp.nbPlayerSameScore);
                }
                // set result value for this new player on this deal
                resultPlayer.result = Constantes.computeResultPaire(mapPlayerResult.size(), resultPlayer.nbPlayerBestScore, resultPlayer.nbPlayerSameScore);
            }
            return resultPlayer;
        } else {
            ContextManager.getTourSerieMgr().getLogger().error("Game is null on setResultPlayer !");
        }
        return null;
    }

    @Override
    public boolean removeResultPlayer(long playerID) {
        TourSerieMemDealPlayer resultPlayer = mapPlayerResult.remove(playerID);
        if (resultPlayer != null) {
            // loop an all result and update result
            for (TourSerieMemDealPlayer temp : mapPlayerResult.values()) {
                if (temp.score <= resultPlayer.score) {
                    if (temp.nbPlayerBestScore > 0) {
                        temp.nbPlayerBestScore--;
                    } else if (temp.score == resultPlayer.score) {
                        if (temp.nbPlayerSameScore > 0) {
                            temp.nbPlayerSameScore--;
                        }
                    }
                    // update the result value
                    temp.result = Constantes.computeResultPaire(mapPlayerResult.size(), temp.nbPlayerBestScore, temp.nbPlayerSameScore);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Return playerID list ordered by score
     * @return
     */
    public List<Long> getListPlayerOrderScore() {
        List<Long> listKeyPla = new ArrayList<>(mapPlayerResult.keySet());
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                TourSerieMemDealPlayer temp1 = mapPlayerResult.get(o1);
                TourSerieMemDealPlayer temp2 = mapPlayerResult.get(o2);
                return temp1.compareTo(temp2);
            }
        });
        return listKeyPla;
    }

    /**
     * Return the players list who stared the tournament but not played this deal
     * @return
     */
    @Override
    public List<Long> getListPlayerStartedTournamentWithNoResult() {
        List<Long> listResult = memTour.getListPlayer();
        listResult.removeAll(new ArrayList<>(mapPlayerResult.keySet()));
        return listResult;
    }

    public List<TourSerieMemDealPlayer> getListResultOrderScore() {
        List<TourSerieMemDealPlayer> listResult = new ArrayList<>(mapPlayerResult.values());
        Collections.sort(listResult, new Comparator<TourSerieMemDealPlayer>() {
            @Override
            public int compare(TourSerieMemDealPlayer o1, TourSerieMemDealPlayer o2) {
                return o1.compareTo(o2);
            }
        });
        return listResult;
    }
}
