package com.funbridge.server.tournament.game;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;

import java.util.Date;

/**
 * Created by pserent on 03/02/2017.
 */
public abstract class TournamentPlayer {
    protected long playerID;

    protected long startDate;
    protected long lastDate;
    protected int rank = 0;
    protected double result = 0;
    protected Date creationDateISO = null;

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public abstract Tournament getTournament();

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getLastDate() {
        return lastDate;
    }

    public void setLastDate(long lastDate) {
        this.lastDate = lastDate;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public WSResultTournamentPlayer toWSResultTournamentPlayer(Player p, long playerAsk) {
        WSResultTournamentPlayer resPla = new WSResultTournamentPlayer();
        resPla.setDateLastPlay(lastDate);
        resPla.setNbDealPlayed(getTournament().getNbDeals());
        if (p != null) {
            resPla.setPlayerID(p.getID());
            resPla.setPlayerPseudo(p.getNickname());
            resPla.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(p.getID()));
            if (p.getID() == playerAsk) {
                resPla.setAvatarPresent(p.isAvatarPresent());
            }
            resPla.setCountryCode(p.getDisplayCountryCode());
        }
        resPla.setRank(rank);
        resPla.setResult(result);
        resPla.setNbTotalPlayer(getTournament().getNbPlayers());
        return resPla;
    }

    public WSResultTournamentPlayer toWSResultTournamentPlayer(PlayerCache pc, long playerAsk) {
        WSResultTournamentPlayer resPla = new WSResultTournamentPlayer();
        resPla.setDateLastPlay(lastDate);
        resPla.setNbDealPlayed(getTournament().getNbDeals());
        if (pc != null) {
            resPla.setPlayerID(pc.ID);
            resPla.setPlayerPseudo(pc.getPseudo());
            resPla.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(pc.ID));
            if (pc.ID == playerAsk) {
                resPla.setAvatarPresent(pc.avatarPresent);
            } else {
                resPla.setAvatarPresent(pc.avatarPublic);
            }
            resPla.setPlayerSerie(pc.serie);
            resPla.setCountryCode(pc.countryCode);
        }
        resPla.setRank(rank);
        resPla.setResult(result);
        resPla.setNbTotalPlayer(getTournament().getNbPlayers());
        return resPla;
    }


}
