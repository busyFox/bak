package com.funbridge.server.tournament.duel.memory;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.duel.DuelMgr;
import com.funbridge.server.tournament.duel.data.DuelGame;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 09/07/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class DuelMemDeal {
    public int dealIndex;
    public String dealID;
    public DuelMemGame memGamePlayer1 = null;
    public DuelMemGame memGamePlayer2 = null;
    public long player1ID, player2ID;

    public String toString() {
        return "dealID="+dealID+" - dealIndex="+dealIndex+" - player1ID="+player1ID+" - player2ID="+player2ID+" - getNbScore="+getNbScore();
    }

    private Logger getLogger() {
        return ContextManager.getDuelMgr().getMemoryMgr().getLogger();
    }

    public int getNbScore() {
        if (memGamePlayer1 == null && memGamePlayer2 == null) {
            return 0;
        }
        if (memGamePlayer1 != null && memGamePlayer2 != null) {
            return 2;
        }
        return 1;
    }

    /**
     * Compute the average of score
     * @return
     */
    public int getScoreAverage() {
        int scoreCumul = 0;
        int nbScore = 0;
        if (memGamePlayer1 != null && memGamePlayer1.score != Constantes.GAME_SCORE_LEAVE) {
            scoreCumul += memGamePlayer1.score;
            nbScore++;
        }
        if (memGamePlayer2 != null && memGamePlayer2.score != Constantes.GAME_SCORE_LEAVE) {
            scoreCumul += memGamePlayer2.score;
            nbScore++;
        }
        if (nbScore == 0) {
            return 0;
        }
        return scoreCumul/nbScore;
    }

    /**
     * Return the other playerID
     * @param playerID
     * @return
     */
    public long getOtherPlayer(long playerID) {
        if (player1ID == playerID) {
            return player2ID;
        }
        return player1ID;
    }

    /**
     * Return the DuelMemDealPlayer for player with ID.
     * @param playerID
     * @return
     */
    public DuelMemGame getForPlayer(long playerID) {
        if (memGamePlayer1 != null && memGamePlayer1.playerID == playerID) {
            return memGamePlayer1;
        }
        if (memGamePlayer2 != null && memGamePlayer2.playerID == playerID) {
            return memGamePlayer2;
        }
        return null;
    }

    public List<DuelMemGame> listMemGame() {
        List<DuelMemGame> l = new ArrayList<>();
        if (memGamePlayer1 != null) {l.add(memGamePlayer1);}
        if (memGamePlayer2 != null) {l.add(memGamePlayer2);}
        return l;
    }

    public void computeResult() {
        // 2 players have finished this deal
        if (memGamePlayer1 != null && memGamePlayer2 != null) {
            // player1 has leaved and not player2
            if (memGamePlayer1.isLeaved() && !memGamePlayer2.isLeaved()) {
                memGamePlayer1.nbPlayersBetterScore = 1;
                memGamePlayer1.result = -15;
                memGamePlayer2.nbPlayersBetterScore = 0;
                memGamePlayer2.result = 15;
            }
            // player2 has leaved and not player1
            else if (memGamePlayer2.isLeaved() && !memGamePlayer1.isLeaved()) {
                memGamePlayer2.nbPlayersBetterScore = 1;
                memGamePlayer2.result = -15;
                memGamePlayer1.nbPlayersBetterScore = 0;
                memGamePlayer1.result = 15;
            }
            // 2 players have leaved
            else if (memGamePlayer2.isLeaved() && memGamePlayer1.isLeaved()) {
                memGamePlayer2.nbPlayersBetterScore = 0;
                memGamePlayer2.result = 0;
                memGamePlayer1.nbPlayersBetterScore = 0;
                memGamePlayer1.result = 0;
            }
            else {
                if (memGamePlayer1.score > memGamePlayer2.score) {
                    memGamePlayer1.nbPlayersBetterScore = 0;
                    memGamePlayer2.nbPlayersBetterScore = 1;
                } else if (memGamePlayer1.score < memGamePlayer2.score) {
                    memGamePlayer1.nbPlayersBetterScore = 0;
                    memGamePlayer2.nbPlayersBetterScore = 1;
                } else {
                    memGamePlayer1.nbPlayersBetterScore = 0;
                    memGamePlayer2.nbPlayersBetterScore = 0;
                }
                memGamePlayer1.result = DuelMgr.computeResultDealPlayer(getScoreAverage(), memGamePlayer1.score);
                memGamePlayer2.result = DuelMgr.computeResultDealPlayer(getScoreAverage(), memGamePlayer2.score);
            }
        }
        else {
            if (memGamePlayer1 != null) {
                memGamePlayer1.nbPlayersBetterScore = 0;
                memGamePlayer1.result = 0;
            }
            if (memGamePlayer2 != null) {
                memGamePlayer2.nbPlayersBetterScore = 0;
                memGamePlayer2.result = 0;
            }
        }
    }

    public DuelMemGame setGame(DuelGame game) {
        if (game != null) {
            DuelMemGame memDealPlayer = getForPlayer(game.getPlayerID());
            if (memDealPlayer == null) {
                memDealPlayer = new DuelMemGame();
                memDealPlayer.score = game.getScore();
                memDealPlayer.playerID = game.getPlayerID();
                memDealPlayer.contract = game.getContract();
                memDealPlayer.contractType = game.getContractType();
                memDealPlayer.declarer = Character.toString(game.getDeclarer());
                memDealPlayer.nbTricks = game.getTricks();
                memDealPlayer.gameID = game.getIDStr();
                memDealPlayer.begins = game.getBegins();

                // add this result player to the list of result
                if (game.getPlayerID() == player1ID) {
                    memGamePlayer1 = memDealPlayer;
                } else if (game.getPlayerID() == player2ID) {
                    memGamePlayer2 = memDealPlayer;
                }

                // compute result
                computeResult();
            } else {
                getLogger().error("MemDealPlayer already exist for player="+game.getPlayerID()+" - memDealPlayer="+memDealPlayer);
            }
            return memDealPlayer;
        } else {
            getLogger().error("Game is null");
        }
        return null;
    }
}
