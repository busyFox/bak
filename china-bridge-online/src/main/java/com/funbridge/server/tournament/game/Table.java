package com.funbridge.server.tournament.game;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.tournament.data.TournamentGamePlayer;
import com.funbridge.server.ws.game.WSTableGame;
import com.gotogames.common.bridge.BridgeConstantes;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by pserent on 23/04/2015.
 */
public class Table {
    private long ID;
    private Player creator;
    private Game game = null;
    private Tournament tournament = null;
    private TournamentGamePlayer[] tableGameplayers = new TournamentGamePlayer[4];
    private HashSet<String> playedDeals = new HashSet<String>();
    private boolean replay = false;

    public Table(Player player, Tournament tour) {
        this.ID = System.currentTimeMillis();
        this.creator = player;
        this.tournament = tour;
        addGamePlayer(new TournamentGamePlayer(false, BridgeConstantes.POSITION_EAST, -1,  "", "", ""));
        addGamePlayer(new TournamentGamePlayer(false, BridgeConstantes.POSITION_NORTH, -1,  "", "", ""));
        addGamePlayer(new TournamentGamePlayer(false, BridgeConstantes.POSITION_WEST, -1,  "", "", ""));
        addGamePlayer(new TournamentGamePlayer(true, BridgeConstantes.POSITION_SOUTH, player.getID(), player.getNickname(), player.getDisplayLang(), player.getDisplayCountryCode()));
    }

    public long getID() {
        return ID;
    }

    public String toString() {
        return "ID="+ID+" - tournament={"+tournament+"} - game={"+game+"}";
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game g) {
        game = g;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void addGamePlayer(TournamentGamePlayer gamePlayer) {
        if (gamePlayer != null) {
            int tablePosition = Constantes.getTablePosition(gamePlayer.getPosition());
            if (tablePosition >= 0 && tablePosition < tableGameplayers.length) {
                tableGameplayers[tablePosition] = gamePlayer;
            }
            gamePlayer.setStatus(Constantes.PLAYER_STATUS_PRESENT);
        }
    }

    public void setPlayedDeals(Collection<String> deals) {
        this.playedDeals.clear();
        this.playedDeals.addAll(deals);
    }

    public void addPlayedDeal(String dealID) {
        playedDeals.add(dealID);
    }

    public int getNbPlayedDeals() {
        return playedDeals.size();
    }

    public boolean isReplay() {
        return replay;
    }

    public void setReplay(boolean replay) {
        this.replay = replay;
    }

    /**
     * Check if all deal of tournament has been played
     * @return
     */
    public boolean isAllTournamentDealPlayed() {
        return getNbPlayedDeals() >= tournament.getNbDeals();
    }

    public boolean isAllPlayerPresent() {
        for (int i = 0; i < tableGameplayers.length; i++) {
            if (tableGameplayers[i] == null) {
                return false;
            }
            if (!tableGameplayers[i].isReady()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return player associated to this position
     * @param pos
     * @return
     */
    public TournamentGamePlayer getGamePlayerAtPosition(char pos) {
        int idx = Constantes.getTablePosition(pos);
        if ((idx >= 0) && (idx < tableGameplayers.length)) {
            return tableGameplayers[idx];
        }
        return null;
    }

    /**
     * Return position for player
     * @param playerID
     * @return
     */
    public char getPlayerPosition(long playerID) {
        for (int i = 0; i < tableGameplayers.length; i++) {
            if (tableGameplayers[i].isHuman() && tableGameplayers[i].getPlayerID() == playerID) {
                return Constantes.getPositionForTableIndex(i);
            }
        }
        return BridgeConstantes.POSITION_NOT_VALID;
    }

    /**
     * The player at the position is Human or Engine ?
     * @param pos
     * @return
     */
    public boolean isPlayerHumanAtPosition(char pos) {
        int idx = Constantes.getTablePosition(pos);
        if ((idx >= 0) && (idx < tableGameplayers.length) && (tableGameplayers[idx] == null)) {
            return tableGameplayers[idx].isHuman();
        }
        return false;
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
}
