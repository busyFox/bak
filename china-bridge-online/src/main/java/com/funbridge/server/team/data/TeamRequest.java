package com.funbridge.server.team.data;

import com.funbridge.server.common.Constantes;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by bplays on 14/10/16.
 */
@Document(collection = "team_request")
public class TeamRequest {
    @Id
    private ObjectId ID;
    private String author;
    private long playerID;
    private String teamID;
    private long date;
    private Date creationDateISO;

    public String toString(){
        return "ID="+getIDStr()+" - author="+ author +" - teamID="+teamID+" - playerID="+playerID+" - date="+ Constantes.timestamp2StringDateHour(date);
    }

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public String getTeamID() {
        return teamID;
    }

    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }
}
