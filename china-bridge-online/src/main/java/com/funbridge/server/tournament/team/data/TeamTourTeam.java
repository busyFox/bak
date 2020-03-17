package com.funbridge.server.tournament.team.data;

import com.funbridge.server.tournament.team.TourTeamMgr;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by bplays on 17/11/16.
 */
@Document(collection="team_tour_team")
public class TeamTourTeam {
    @Id
    private ObjectId ID;
    private String teamID;
    private String division;
    private String periodID;
    private int tourIndex;
    private double result;
    private int points;
    private int rank;
    private Date creationDateISO;

    public String toString(){
        return "ID="+getIDStr()+" - teamID="+teamID+" - periodID="+periodID+" - tourIndex="+tourIndex+" - points="+points+" - rank="+rank+" - result="+result;
    }

    public String getIDStr() {
        return ID.toString();
    }

    public String getTourID(String periodID) {
        return periodID+ TourTeamMgr.PERIOD_ID_INDICE_SEPARATOR+String.format("%02d", tourIndex);
    }

    public int getTourIndex() {
        return tourIndex;
    }

    public void setTourIndex(int tourIndex) {
        this.tourIndex = tourIndex;
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

    public String getTeamID() {
        return teamID;
    }

    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getPeriodID() {
        return periodID;
    }

    public void setPeriodID(String periodID) {
        this.periodID = periodID;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }
}
