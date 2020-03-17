package com.funbridge.server.team.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.team.data.Team;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bplays on 25/11/16.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TeamCache implements Serializable{
    public String ID;
    public String name;
    public String division;
    public String countryCode;
    public String description;
    public long captainID;
    public List<Long> players = new ArrayList<>();

    public String toString() {
        return "ID="+ID+" - name="+name+" - division="+division+" - countryCode="+countryCode+" - description="+description+" - captainID="+captainID;
    }

    public TeamCache(){}

    public TeamCache(String teamID){ this.ID = teamID; }

    public int getNbPlayers() {
        return players.size();
    }

    public void setTeamData(Team team){
        if(team != null){
            this.name = team.getName();
            this.division = team.getDivision();
            this.countryCode = team.getCountryCode();
            this.description = team.getDescription();
            if (team.getCaptain() != null) {
                this.captainID = team.getCaptain().getPlayerID();
            } else {
                this.captainID = 0;
            }
            this.players = team.getListPlayerID();
        }
    }
}
