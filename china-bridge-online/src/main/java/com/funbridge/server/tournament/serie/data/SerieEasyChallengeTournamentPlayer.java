package com.funbridge.server.tournament.serie.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.tournament.game.TournamentPlayer;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Document(collection="serie_easy_challenge_tournament_player")
public class SerieEasyChallengeTournamentPlayer extends TournamentPlayer {

    @Id
    private ObjectId id;

    @DBRef
    private TourSerieTournament tournament;

    private String periodID;
    private Set<String> playedDeals = new HashSet<>();
    private int currentDealIndex = -1;
    private boolean finished = false;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @Override
    public TourSerieTournament getTournament() {
        return tournament;
    }

    public void setTournament(TourSerieTournament tournament) {
        this.tournament = tournament;
    }

    public String getPeriodID() {
        return periodID;
    }

    public void setPeriodID(String periodID) {
        this.periodID = periodID;
    }

    public Set<String> getPlayedDeals() {
        return playedDeals;
    }

    public void setPlayedDeals(Set<String> playedDeals) {
        this.playedDeals = playedDeals;
    }

    public int getCurrentDealIndex() {
        return currentDealIndex;
    }

    public void setCurrentDealIndex(int currentDealIndex) {
        this.currentDealIndex = currentDealIndex;
    }

    public String getIDStr(){ return this.id != null ? this.id.toString() : null; }

    public int getNbPlayedDeals() {
        return playedDeals.size();
    }

    public void addPlayedDeals(final String dealID) { playedDeals.add(dealID); }

    /**
     * Transform the object into a WS result.
     * @param pc the player cache.
     * @param t the tournament.
     * @param playerAsk the required player.
     * @return an object used in the result of the WS.
     */
    public WSResultTournamentPlayer toWSResultTournamentPlayer(final PlayerCache pc, final Tournament t, final long playerAsk) {
        final WSResultTournamentPlayer resPla = new WSResultTournamentPlayer();
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

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
