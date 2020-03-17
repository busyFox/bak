package com.funbridge.server.tournament.duel.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.duel.data.DuelDeal;
import com.funbridge.server.tournament.duel.data.DuelGame;
import com.funbridge.server.tournament.duel.data.DuelTournamentPlayer;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 10/07/2015.
 * Memory object for a duel between 2 players -
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class DuelMemTournament {
    public long playerDuelID; // => PlayerDuel
    public String tourID; // => DuelTournament
    public String tourPlayerID; // => DuelTournamentPlayer
    public DuelMemDeal[] tabDeal;
    public long player1ID;
    public long player2ID;
    public DuelMemTournamentPlayer memTournamentPlayer1 = null;
    public DuelMemTournamentPlayer memTournamentPlayer2 = null;
    public long dateStart;
    public long dateFinish;
    public long dateReminder = 0;

    public DuelMemTournament(){}

    public DuelMemTournament(DuelTournamentPlayer tournamentPlayer){
        this.tourID = tournamentPlayer.getTournament().getIDStr();
        this.tourPlayerID = tournamentPlayer.getIDStr();
        this.dateFinish = ContextManager.getDuelMgr().computeDateFinish(tournamentPlayer);
        this.dateStart = tournamentPlayer.getCreationDate();
        this.playerDuelID = tournamentPlayer.getPlayerDuelID();
        this.player1ID = tournamentPlayer.getPlayer1ID();
        this.player2ID = tournamentPlayer.getPlayer2ID();
        tabDeal = new DuelMemDeal[tournamentPlayer.getTournament().getNbDeals()];
        for (int i = 0; i < tabDeal.length; i++) {
            DuelDeal d = (DuelDeal)tournamentPlayer.getTournament().getDealAtIndex(i + 1);
            tabDeal[i] = new DuelMemDeal();
            tabDeal[i].player1ID = player1ID;
            tabDeal[i].player2ID = player2ID;
            tabDeal[i].dealIndex = d.index;
            tabDeal[i].dealID = d.getDealID(tourID);
        }
        memTournamentPlayer1 = new DuelMemTournamentPlayer();
        memTournamentPlayer1.playerID = player1ID;
        memTournamentPlayer2 = new DuelMemTournamentPlayer();
        memTournamentPlayer2.playerID = player2ID;
    }

    private Logger getLogger() {
        return ContextManager.getDuelMgr().getMemoryMgr().getLogger();
    }

    public String toString() {
        return "player1ID="+player1ID+" - player2ID="+player2ID+" - playerDuelID="+playerDuelID+" - tourID="+tourID+" - dateFinish="+ Constantes.timestamp2StringDateHour(dateFinish);
    }

    /**
     * Check if player has played all deal of tournament
     * @param playerAsk
     * @return
     */
    public boolean isAllPlayedForPlayer(long playerAsk) {
        DuelMemTournamentPlayer e = getMemTournamentPlayer(playerAsk);
        if (e != null) {
            return e.getNbDealsPlayed() >= tabDeal.length;
        }
        return false;
    }

    /**
     * Check if all player have played all deal
     * @return
     */
    public boolean isAllPlayed() {
        return isAllPlayedForPlayer(player1ID) && isAllPlayedForPlayer(player2ID);
    }

    /**
     * Return the duelRanking for player
     * @param playerID
     * @return
     */
    public DuelMemTournamentPlayer getMemTournamentPlayer(long playerID) {
        if (player1ID == playerID) {
            return memTournamentPlayer1;
        }
        else if (player2ID == playerID) {
            return memTournamentPlayer2;
        }
        return null;
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
     * Return both players
     * @return
     */
    public List<DuelMemTournamentPlayer> getListMemTournamentPlayer() {
        List<DuelMemTournamentPlayer> list = new ArrayList<>();
        list.add(memTournamentPlayer1);
        list.add(memTournamentPlayer2);
        return list;
    }

    /**
     * Check date finish is not reached
     * @return true if current time > dateFinish
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > dateFinish;
    }

    /**
     * Check not yet reminder and remainting time is less than tsReminder
     * @param tsReminder
     * @return
     */
    public boolean isToReminder(long tsReminder) {
        return dateReminder == 0 && System.currentTimeMillis() < dateFinish && dateFinish - System.currentTimeMillis() < tsReminder;
    }

    /**
     * Get result for playerID. Sum of result of each deal (only positive value)
     * @param playerID
     * @return
     */
    public double getResultPlayer(long playerID) {
        double res = 0;
        for (DuelMemDeal e : tabDeal) {
            DuelMemGame memGame = e.getForPlayer(playerID);
            if (memGame != null) {
                // result on tournament is always positive => take only positive result
                if (memGame.result > 0) {
                    res += memGame.result;
                }
            }
        }
        return res;
    }

    /**
     * Compute result & ranking on tournament for all players
     */
    public void computeResultRanking() {
        memTournamentPlayer1.result = getResultPlayer(player1ID);
        memTournamentPlayer2.result = getResultPlayer(player2ID);
        if (memTournamentPlayer1.result > memTournamentPlayer2.result) {
            memTournamentPlayer1.ranking = 1;
            memTournamentPlayer2.ranking = 2;
        } else if (memTournamentPlayer1.result < memTournamentPlayer2.result) {
            memTournamentPlayer1.ranking = 2;
            memTournamentPlayer2.ranking = 1;
        } else {
            memTournamentPlayer1.ranking = 1;
            memTournamentPlayer2.ranking = 1;
        }
    }

    /**
     * Return the winner of duel. 0 if same result for the two players
     * @return
     */
    public long getWinner() {
        double resPla1 = getResultPlayer(player1ID);
        double resPla2 = getResultPlayer(player2ID);
        if (resPla1 > resPla2) {return player1ID;}
        if (resPla1 < resPla2) {return player2ID;}
        return 0;
    }

    /**
     * Build ResultTournamentPlayer for a player
     * @param playerCacheAsk
     * @return
     */
    public WSResultTournamentPlayer getWSResultTournamentPlayer(PlayerCache playerCacheAsk) {
        if (playerCacheAsk != null) {
            DuelMemTournamentPlayer memTourPlayer = getMemTournamentPlayer(playerCacheAsk.ID);
            if (memTourPlayer != null) {
                WSResultTournamentPlayer res = memTourPlayer.toWSResultTournamentPlayer();
                res.setNbTotalPlayer(2);
                res.setPlayerID(playerCacheAsk.ID);
                res.setPlayerPseudo(playerCacheAsk.getPseudo());
                res.setAvatarPresent(playerCacheAsk.avatarPresent);
                res.setCountryCode(playerCacheAsk.countryCode);
                res.setPlayerSerie(playerCacheAsk.serie);
                res.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(playerCacheAsk.ID));
                return res;
            } else {
                ContextManager.getDuelMgr().getLogger().error("No memTourPlayer on DuelMemTournament=" + this + " - for playerCacheAsk=" + playerCacheAsk);
            }
        }
        return null;
    }

    /**
     * Return the current deal for this player
     * @param playerID
     * @return
     */
    public int getCurrentDealForPlayer(long playerID) {
        DuelMemTournamentPlayer memTournamentPlayer = getMemTournamentPlayer(playerID);
        if (memTournamentPlayer != null) {
            return memTournamentPlayer.currentDealIndex;
        }
        return -1;
    }

    public DuelMemDeal getMemDeal(int dealIndex) {
        for (DuelMemDeal e : tabDeal) {
            if (e.dealIndex == dealIndex) {
                return e;
            }
        }
        return null;
    }

    public DuelMemTournamentPlayer addGamePlayer(DuelGame game) {
        if (game != null) {
            DuelMemDeal memDeal = getMemDeal(game.getDealIndex());
            if (memDeal != null) {
                // add result of player for this deal
                DuelMemGame memGame = memDeal.setGame(game);
                if (memGame != null) {
                    DuelMemTournamentPlayer memTournamentPlayer = getMemTournamentPlayer(game.getPlayerID());
                    if (memTournamentPlayer != null) {
                        memTournamentPlayer.addDealPlayed(memDeal.dealID);
                        memTournamentPlayer.dateLastPlay = game.getLastDate();

                        // update result & ranking only if method param is set
                        computeResultRanking();

                        return memTournamentPlayer;
                    } else {
                        getLogger().error("No player ranking on tourID=" + tourID + " - tourPlayerID="+tourPlayerID+" - for playerID=" + game.getPlayerID());
                    }
                } else {
                   getLogger().error("Fail to setGame - game=" + game + " - tourID=" + tourID + " - tourPlayerID="+tourPlayerID+" - player=" + game.getPlayerID());
                }
            } else {
                getLogger().error("No DuelMemDeal for game=" + game + " - tourID=" + tourID + " - tourPlayerID="+tourPlayerID+" - player=" + game.getPlayerID());
            }
        } else {
            getLogger().error("Game is null !");
        }
        return null;
    }

    public int getNbDealPlayedByPlayer1() {
        if (memTournamentPlayer1 != null) {
            return memTournamentPlayer1.getNbDealsPlayed();
        }
        return 0;
    }

    public int getNbDealPlayedByPlayer2() {
        if (memTournamentPlayer2 != null) {
            return memTournamentPlayer2.getNbDealsPlayed();
        }
        return 0;
    }

    public int getNbDealPlayed(long player) {
        if (player == player1ID) {
            return getNbDealPlayedByPlayer1();
        }
        if (player == player2ID) {
            return getNbDealPlayedByPlayer2();
        }
        return 0;
    }

    /**
     * Return the ID of player1 for player ask
     * @param playerAsk
     * @return
     */
    public long getPlayer1IDForAsk(long playerAsk) {
        if (player1ID == playerAsk) {
            return player1ID;
        }
        return player2ID;
    }

    /**
     * Return the ID of player2 for player ask
     * @param playerAsk
     * @return
     */
    public long getPlayer2IDForAsk(long playerAsk) {
        if (player2ID == playerAsk) {
            return player1ID;
        }
        return player2ID;
    }

    public boolean isDuelWithArgine() {
        return player1ID == Constantes.PLAYER_ARGINE_ID || player2ID == Constantes.PLAYER_ARGINE_ID;
    }
}
