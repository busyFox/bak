package com.funbridge.server.tournament.serie.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.tournament.data.TournamentGamePlayer;
import com.funbridge.server.ws.game.WSTableGame;
import com.gotogames.common.bridge.BridgeConstantes;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by pserent on 16/06/2014.
 * Table to play tournament serie
 */
public class TourSerieTable {
    private long ID;
    private TourSerieTournament tournament;
    private Player creator;
    private char currentPlayerPosition;
    private TourSerieGame currentGame = null;
    private TournamentGamePlayer[] tableGameplayers = new TournamentGamePlayer[4];
    private HashSet<String> dealsPlayed = new HashSet<String>();

    public TourSerieTable(Player player, TourSerieTournament tour) {
        this.ID = System.currentTimeMillis();
        this.creator = player;
        this.tournament = tour;
        addGamePlayer(new TournamentGamePlayer(false, BridgeConstantes.POSITION_EAST, -1,  "", "", ""));
        addGamePlayer(new TournamentGamePlayer(false, BridgeConstantes.POSITION_NORTH, -1,  "", "", ""));
        addGamePlayer(new TournamentGamePlayer(false, BridgeConstantes.POSITION_WEST, -1,  "", "", ""));
        addGamePlayer(new TournamentGamePlayer(true, BridgeConstantes.POSITION_SOUTH, player.getID(), player.getNickname(), player.getDisplayLang(), player.getDisplayCountryCode()));
    }

    public String toString() {
        return "ID="+ID+" - creator="+creator+" - currentGame="+currentGame;
    }

    public long getID() {
        return ID;
    }

    public void setID(long iD) {
        ID = iD;
    }

    public TourSerieTournament getTournament() {
        return tournament;
    }

    public void setTournament(TourSerieTournament tournament) {
        this.tournament = tournament;
    }

    public Player getCreator() {
        return creator;
    }

    public void setCreator(Player creator) {
        this.creator = creator;
    }

    public char getCurrentPlayerPosition() {
        return currentPlayerPosition;
    }

    public void setCurrentPlayerPosition(char currentPlayerPosition) {
        this.currentPlayerPosition = currentPlayerPosition;
    }

    public TourSerieDeal getCurrentDeal() {
        return null;
    }

    public TourSerieGame getCurrentGame() {
        return currentGame;
    }

    public void setCurrentGame(TourSerieGame currentGame) {
        this.currentGame = currentGame;
    }

    public int getNbDealPlayed() {
        return dealsPlayed.size();
    }

    public void setDealsPlayed(Set<String> deals) {
        this.dealsPlayed.clear();
        this.dealsPlayed.addAll(deals);
    }

    public void addDealPlayed(String dealID) {
        dealsPlayed.add(dealID);
    }

    private void addGamePlayer(TournamentGamePlayer gamePlayer) {
        if (gamePlayer != null) {
            int tablePosition = Constantes.getTablePosition(gamePlayer.getPosition());
            if (tablePosition >= 0 && tablePosition < tableGameplayers.length) {
                tableGameplayers[tablePosition] = gamePlayer;
            }
            gamePlayer.setStatus(Constantes.PLAYER_STATUS_PRESENT);
        }
    }

    public boolean isAllPlayerPresent() {
        for (final TournamentGamePlayer player : tableGameplayers) {
            if (player == null) {
                return false;
            }
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
    }

    public TournamentGamePlayer getCurrentGamePlayer() {
        int idx = Constantes.getTablePosition(currentPlayerPosition);
        if (idx >= 0 && idx < tableGameplayers.length) {
            return tableGameplayers[idx];
        }
        return null;
    }

    public char getPlayerPosition(long playerID) {
        for (int i = 0; i < tableGameplayers.length; i++) {
            if (tableGameplayers[i].isHuman() && tableGameplayers[i].getPlayerID() == playerID) {
                return Constantes.getPositionForTableIndex(i);
            }
        }
        return BridgeConstantes.POSITION_NOT_VALID;
    }

    public boolean isPlayerHumanAtPosition(char pos) {
        int idx = Constantes.getTablePosition(pos);
        if ((idx >= 0) && (idx < tableGameplayers.length) && (tableGameplayers[idx] == null)) {
            return tableGameplayers[idx].isHuman();
        }
        return false;
    }

    public boolean isPlayerOnTable(long playerID) {
        for (final TournamentGamePlayer player : tableGameplayers) {
            if (player.isHuman() && player.getPlayerID() == playerID) {
                return true;
            }
        }
        return false;
    }

    private TournamentGamePlayer getGamePlayerForID(long playerID) {
        for (final TournamentGamePlayer player : tableGameplayers) {
            if (player.isHuman() && player.getPlayerID() == playerID) {
                return player;
            }
        }
        return null;
    }

    public TournamentGamePlayer getGamePlayerAtPosition(char pos) {
        int idx = Constantes.getTablePosition(pos);
        if ((idx >= 0) && (idx < tableGameplayers.length)) {
            return tableGameplayers[idx];
        }
        return null;
    }

    public TournamentGamePlayer[] getPlayers() {
        return tableGameplayers;
    }

    public WSTableGame toWSTableGame() {
        WSTableGame wsTable = new WSTableGame();
        wsTable.tableID = ID;
        if (tableGameplayers[Constantes.TABLE_POSITION_SOUTH] != null) {
            wsTable.playerSouth = tableGameplayers[Constantes.TABLE_POSITION_SOUTH].toWSGamePlayer();
        }
        if (tableGameplayers[Constantes.TABLE_POSITION_WEST] != null) {
            wsTable.playerWest = tableGameplayers[Constantes.TABLE_POSITION_WEST].toWSGamePlayer();
        }
        if (tableGameplayers[Constantes.TABLE_POSITION_NORTH] != null) {
            wsTable.playerNorth = tableGameplayers[Constantes.TABLE_POSITION_NORTH].toWSGamePlayer();
        }
        if (tableGameplayers[Constantes.TABLE_POSITION_EAST] != null) {
            wsTable.playerEast = tableGameplayers[Constantes.TABLE_POSITION_EAST].toWSGamePlayer();
        }
        wsTable.leaderID = creator.getID();
        return wsTable;
    }

    public boolean isCurrentDealFinished() {
        if (currentGame != null) {
            return currentGame.isFinished();
        }
        return false;
    }

    /**
     * Check if all deal of tournament has been played
     * @return
     */
    public boolean isAllTournamentDealPlayed() {
        return (getNbDealPlayed() >= tournament.getNbDeals());
    }

    public String playersToString() {
        String str = "";
        for (TournamentGamePlayer tgp : tableGameplayers) {
            if (tgp != null) {
                if (str.length() > 0) str += " | ";
                str += tgp.toString();
            }
        }
        return str;
    }

    public int getDealIndexInProgress() {
        if (currentGame != null && !currentGame.isFinished()) {
            return currentGame.getDealIndex();
        }
        return -1;
    }

    /**
     * Set the value of player status for this player on this table.
     * @param playerID
     * @param playerStatus
     */
    public void setPlayerStatus(long playerID, int playerStatus) {
        TournamentGamePlayer gamePlayer = getGamePlayerForID(playerID);
        if (gamePlayer != null) {
            gamePlayer.setStatus(playerStatus);
        }
    }

    public boolean isReplay() {
        if (currentGame != null) {
            return currentGame.isReplay();
        }
        return false;
    }
}
