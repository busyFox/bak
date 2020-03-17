package com.funbridge.server.tournament.serie.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by bplays on 27/04/16.
 */
@Document(collection="serie_top_challenge_tournament_player")
public class SerieTopChallengeTournamentPlayer {
    @Id
    private ObjectId ID;

    private long playerID;

    @DBRef
    private TourSerieTournament tournament;

    private String periodID;
    private long startDate;
    private long lastDate;
    private HashSet<String> playedDeals = new HashSet<>();
    private int currentDealIndex = -1;
    private boolean finished = false;
    private int rank = 0;
    private double result = 0;
    private Date creationDateISO = null;

    public String getIDStr() { return ID != null ? ID.toString() : null; }

    public int getNbPlayedDeals() {
        return playedDeals.size();
    }

    public void addPlayedDeals(String dealID) { playedDeals.add(dealID); }

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public String getPeriodID() {
        return periodID;
    }

    public void setPeriodID(String periodID) {
        this.periodID = periodID;
    }

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

    public Set<String> getPlayedDeals(){ return playedDeals; }

    public int getCurrentDealIndex() {
        return currentDealIndex;
    }

    public void setCurrentDealIndex(int currentDealIndex) {
        this.currentDealIndex = currentDealIndex;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
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

    public TourSerieTournament getTournament() {
        return tournament;
    }

    public void setTournament(TourSerieTournament tournament) {
        this.tournament = tournament;
    }

    public WSResultTournamentPlayer toWSResultTournamentPlayer(PlayerCache pc, Tournament t, long playerAsk) {
        WSResultTournamentPlayer resPla = new WSResultTournamentPlayer();
        resPla.setDateLastPlay(lastDate);
        resPla.setNbDealPlayed(getNbPlayedDeals());
        if (pc != null) {
            resPla.setPlayerID(pc.ID);
            resPla.setPlayerPseudo(pc.getPseudo());
            resPla.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(pc.ID));
            if (pc.ID == playerAsk) {
                resPla.setAvatarPresent(pc.avatarPresent);
            } else {
                resPla.setAvatarPresent(pc.avatarPublic);
            }
            resPla.setCountryCode(pc.countryCode);
            resPla.setPlayerSerie(pc.serie);
        }
        resPla.setRank(rank);
        resPla.setResult(result);
        resPla.setNbTotalPlayer(t.getNbPlayers()+1);
        return resPla;
    }
}
