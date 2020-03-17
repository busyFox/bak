package com.funbridge.server.tournament.team.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;


/**
 * Created by bplays on 17/11/16.
 */
@Document(collection="team_period_team")
public class TeamPeriodTeam {
    @Id
    private ObjectId ID;
    private String teamID;
    private String periodID;
    private String division;
    private double result;
    private int points;
    private int rank;
    private int evolution;
    private int nbTeams;
    private Date creationDateISO;

    public String toString(){
        return "ID="+getIDStr()+" - teamID="+teamID+" - periodID="+periodID+" - division="+division+" - points="+points+" - rank="+rank;
    }

    public String getIDStr() {
        return ID.toString();
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public String getTeamID() {
        return teamID;
    }

    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }

    public String getPeriodID() {
        return periodID;
    }

    public void setPeriodID(String periodID) {
        this.periodID = periodID;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
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

    public int getNbTeams() {
        return nbTeams;
    }

    public void setNbTeams(int nbTeams) {
        this.nbTeams = nbTeams;
    }
}
