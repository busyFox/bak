package com.funbridge.server.tournament.serie.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 09/07/2014.
 */
@Document(collection="serie_period_result")
public class TourSeriePeriodResult {
    @Id
    private ObjectId ID;
    @Indexed
    private long playerID;
    @Indexed
    private String serie;

    @Indexed
    private String periodID;

    private int nbTournamentPlayed;
    private double result;
    private int rank;
    private int evolution;
    private int nbPlayer;
    private String countryCode;
    private int rankMain = 0;

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

    public String getPeriodID() {
        return periodID;
    }

    public void setPeriodID(String value) {
        this.periodID = value;
    }

    public int getNbTournamentPlayed() {
        return nbTournamentPlayed;
    }

    public void setNbTournamentPlayed(int nbTournamentPlayed) {
        this.nbTournamentPlayed = nbTournamentPlayed;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getEvolution() {
        return evolution;
    }

    public void setEvolution(int evolution) {
        this.evolution = evolution;
    }

    public int getNbPlayer() {
        return nbPlayer;
    }

    public void setNbPlayer(int nbPlayer) {
        this.nbPlayer = nbPlayer;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public int getRankMain() {
        return rankMain;
    }

    public void setRankMain(int rankMain) {
        this.rankMain = rankMain;
    }
}
