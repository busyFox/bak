package com.funbridge.server.tournament.duel.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.game.TournamentPlayer;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by pserent on 08/07/2015.
 */
@Document(collection="duel_tournament_player")
public class DuelTournamentPlayer extends TournamentPlayer {
    @Id
    private ObjectId ID;

    @DBRef
    private DuelTournament tournament;

    private long playerDuelID;
    private long player1ID;
    private long player2ID;
    private double resultPlayer1 = 0;
    private double resultPlayer2 = 0;
    private long creationDate = 0;
    private long finishDate = 0;
    private String origin = "";


    public String toString() {
        return "ID="+getIDStr()+" - playerDuelID="+playerDuelID+" - player1ID="+player1ID+" - player2ID="+player2ID+" - dateCreation="+Constantes.timestamp2StringDateHour(creationDate)+" - finishDate="+Constantes.timestamp2StringDateHour(finishDate)+" - tournament={"+tournament+"}";
    }

    public ObjectId getID() {
        return ID;
    }

    public String getIDStr() {
        if (ID != null) {return ID.toString();}
        return null;
    }

    public DuelTournament getTournament() {
        return tournament;
    }

    public void setTournament(DuelTournament tournament) {
        this.tournament = tournament;
    }

    public long getPlayerDuelID() {
        return playerDuelID;
    }

    public void setPlayerDuelID(long playerDuelID) {
        this.playerDuelID = playerDuelID;
    }

    public long getPlayer1ID() {
        return player1ID;
    }

    public void setPlayer1ID(long player1ID) {
        this.player1ID = player1ID;
    }

    public long getPlayer2ID() {
        return player2ID;
    }

    public void setPlayer2ID(long player2ID) {
        this.player2ID = player2ID;
    }

    public double getResultPlayer1() {
        return resultPlayer1;
    }

    public void setResultPlayer1(double resultPlayer1) {
        this.resultPlayer1 = resultPlayer1;
    }

    public double getResultPlayer2() {
        return resultPlayer2;
    }

    public void setResultPlayer2(double resultPlayer2) {
        this.resultPlayer2 = resultPlayer2;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(long finishDate) {
        this.finishDate = finishDate;
    }

    public double getResultForPlayer1(long playerAsk) {
        if (isPlayer1(playerAsk)) {
            return resultPlayer1;
        }
        return resultPlayer2;
    }

    public double getResultForPlayer2(long playerAsk) {
        if (isPlayer1(playerAsk)) {
            return resultPlayer2;
        }
        return resultPlayer1;
    }

    public double getResultForPlayer(long playerAsk) {
        if (isPlayer1(playerAsk)) {
            return resultPlayer1;
        }
        if (isPlayer2(playerAsk)) {
            return resultPlayer2;
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

    /**
     * Check if player with playerID is player1
     * @param playerID
     * @return
     */
    public boolean isPlayer1(long playerID) {
        return (player1ID == playerID);
    }

    /**
     * Check if player with playerID is player2
     * @param playerID
     * @return
     */
    public boolean isPlayer2(long playerID) {
        return (player2ID == playerID);
    }

    /**
     * Return the result of winner player
     * @return
     */
    public double getResultWinner() {
        if (resultPlayer1 > resultPlayer2) {
            return resultPlayer1;
        }
        return resultPlayer2;
    }

    /**
     * Return the result of loser player
     * @return
     */
    public double getResultLoser() {
        if (resultPlayer1 < resultPlayer2) {
            return resultPlayer1;
        }
        return resultPlayer2;
    }

    public boolean isFinished() {
        return finishDate != 0;
    }

    /**
     * Compute the future date of finsh duel
     * @return
     */
    public long computeDateFinish() {
        if (finishDate == 0) {
            return creationDate+ ContextManager.getDuelMgr().getDuelDuration();
        }
        return finishDate;
    }

    /**
     * Check if the duel has expired : current date > date finish (compute)
     * @return
     */
    public boolean isExpired() {
        return computeDateFinish() < System.currentTimeMillis();
    }

    /**
     * Return the winner playerID. 0 if draw.
     * @return
     */
    public long getWinner() {
        if (isFinished()) {
            if (resultPlayer1 > resultPlayer2) {
                return player1ID;
            }
            if (resultPlayer1 < resultPlayer2) {
                return player2ID;
            }
            return 0;
        }
        return 0;
    }

    public int getRankForPlayer(long playerID) {
        if (getWinner() == playerID) {
            return 1;
        }
        return 2;
    }

    /**
     * Return the partner of the player for this duel
     * @param playerID
     * @return
     */
    public long getPartner(long playerID) {
        if (isPlayer1(playerID)) {
            return player2ID;
        }
        return player1ID;
    }

    /**
     * Return true if playerID is player1 or player2, else false
     * @param playerID
     * @return
     */
    public boolean isPlayerDuel(long playerID) {
        return player1ID == playerID || player2ID == playerID;
    }

    public WSResultTournamentPlayer toWSResultTournamentPlayer(PlayerCache pc) {
        WSResultTournamentPlayer resPla = new WSResultTournamentPlayer();
        resPla.setDateLastPlay(finishDate);
        resPla.setNbDealPlayed(tournament.getNbDeals());
        if (pc != null) {
            resPla.setPlayerID(pc.ID);
            resPla.setPlayerPseudo(pc.getPseudo());
            resPla.setAvatarPresent(pc.avatarPresent);
            resPla.setPlayerSerie(pc.serie);
            resPla.setCountryCode(pc.countryCode);
            resPla.setRank(getRankForPlayer(pc.ID));
            resPla.setResult(getResultForPlayer(pc.ID));
            resPla.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(pc.ID));
        }
        resPla.setNbTotalPlayer(2);
        return resPla;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public boolean isDuelWithArgine() {
        return player1ID == Constantes.PLAYER_ARGINE_ID || player2ID == Constantes.PLAYER_ARGINE_ID;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}
