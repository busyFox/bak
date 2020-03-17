package com.funbridge.server.tournament.serie.data;

import com.funbridge.server.common.ContextManager;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 08/07/2014.
 * Serie information & data for player
 */
@Document(collection="serie_player")
public class TourSeriePlayer {
    @Id
    private ObjectId ID;
    @Indexed(unique = true)
    private long playerID;
    @Indexed
    private String serie;
    @Indexed
    private String lastPeriodPlayed;
    private int nbTournamentPlayed;

    private String bestSerie = null;
    private int bestRank = -1;
    private String bestPeriod = null;
    private String countryCode = "";

    public String toString() {
        return "playerID="+ playerID+" - serie="+serie+" - lastPeriodPlayed="+lastPeriodPlayed+" - nbTournamentPlayed="+nbTournamentPlayed+" - bestSerie="+bestSerie+" - bestRank="+bestRank+" - bestPeriod="+bestPeriod;
    }

    public ObjectId getID() {
        return ID;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getLastPeriodPlayed() {
        return lastPeriodPlayed;
    }

    public void setLastPeriodPlayed(String lastPeriodPlayed) {
        this.lastPeriodPlayed = lastPeriodPlayed;
    }

    public int getNbTournamentPlayed() {
        return nbTournamentPlayed;
    }

    public void setNbTournamentPlayed(int nbTournamentPlayed) {
        this.nbTournamentPlayed = nbTournamentPlayed;
    }

    public void incrementNbTournamentPlayed(int value) {
        this.nbTournamentPlayed += value;
    }

    public boolean isReserve() {
        return ContextManager.getTourSerieMgr().isPlayerReserve(serie, lastPeriodPlayed, false);
    }

    public String getBestSerie() {
        return bestSerie;
    }

    public void setBestSerie(String bestSerie) {
        this.bestSerie = bestSerie;
    }

    public int getBestRank() {
        return bestRank;
    }

    public void setBestRank(int bestRank) {
        this.bestRank = bestRank;
    }

    public boolean isBestResult() {
        return bestSerie != null;
    }

    public String getBestPeriod() {
        return bestPeriod;
    }

    public void setBestPeriod(String bestPeriod) {
        this.bestPeriod = bestPeriod;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
