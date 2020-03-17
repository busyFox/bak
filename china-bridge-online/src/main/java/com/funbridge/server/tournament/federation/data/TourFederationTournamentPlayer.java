package com.funbridge.server.tournament.federation.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.game.TournamentPlayer;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ldelbarre on 02/08/2017.
 */
public class TourFederationTournamentPlayer extends TournamentPlayer{
    @Id
    private ObjectId ID;

    @DBRef
    private TourFederationTournament tournament;

    private double points = 0;
    private int funbridgePoints = 0;
    private List<String> playedDeals = new ArrayList<>();

    public WSResultTournamentPlayer toWSResultTournamentPlayer(PlayerCache pc, long playerAsk) {
        WSResultTournamentPlayer resPla = super.toWSResultTournamentPlayer(pc, playerAsk);
        resPla.setMasterPoints(points);
        resPla.setFbPoints(funbridgePoints);
        resPla.setNbDealPlayed(playedDeals.size());
        return resPla;
    }

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    public ObjectId getID() {
        return ID;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public TourFederationTournament getTournament() {
        return tournament;
    }

    public void setTournament(TourFederationTournament tournament) {
        this.tournament = tournament;
    }

    public List<String> getPlayedDeals() {
        return playedDeals;
    }

    public void setPlayedDeals(List<String> playedDeals) {
        this.playedDeals = playedDeals;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = points;
    }

    public int getFunbridgePoints() {
        return funbridgePoints;
    }

    public void setFunbridgePoints(int funbridgePoints) {
        this.funbridgePoints = funbridgePoints;
    }

    @JsonIgnore
    public String[] getArrayPlayDeals() {
        return playedDeals.toArray(new String[playedDeals.size()]);
    }

    public String getStringPoints() {
        return String.format("%.0f", points);
    }
}
