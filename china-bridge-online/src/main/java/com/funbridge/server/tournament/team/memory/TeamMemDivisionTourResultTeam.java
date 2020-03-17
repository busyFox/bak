package com.funbridge.server.tournament.team.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Data of team result for periodTour
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TeamMemDivisionTourResultTeam implements Comparable<TeamMemDivisionTourResultTeam>{
    public String teamID;
    public double result;
    public int points; // cumul des points des (4) joueurs sur le tour
    public int rank = -1;
    @JsonIgnore
    public List<TeamMemTournamentPlayer> listData = new ArrayList<>(); // résultat des 4 joueurs sur les tournois. 1 tournoi / joueur.

    public TeamMemDivisionTourResultTeam(){}
    public TeamMemDivisionTourResultTeam(String teamID){
        this.teamID = teamID;
    }


    public String toString() {
        return "teamID="+teamID+" - result="+result+" - rank="+rank+" - points="+points;
    }

    public int getNbTournamentPlayed() {
        return listData.size();
    }

    public TeamMemTournamentPlayer getResultForPlayer(long playerID) {
        for (TeamMemTournamentPlayer e : listData) {
            if (e.playerID == playerID) {
                return e;
            }
        }
        return null;
    }

    public void setTourResultForPlayer(TeamMemTournamentPlayer memTournamentPlayer) {
        if (memTournamentPlayer != null) {
            if (getResultForPlayer(memTournamentPlayer.playerID) == null) {
                listData.add(memTournamentPlayer);
            }
        }
        computeResult();
    }

    /**
     * Compute the result of player on this serie.
     * @return
     */
    public void computeResult() {
        double valRes = 0;
        int valPoints = 0;
        for (TeamMemTournamentPlayer e : listData) {
            valRes += e.result;
            valPoints += e.points;
        }
        this.result = valRes;
        this.points = valPoints;
    }

    @Override
    public int compareTo(TeamMemDivisionTourResultTeam o) {
        if(o.points > this.points){
            return 1;
        } else if(o.points == this.points){
            return 0;
        }
        return -1;
    }
}
