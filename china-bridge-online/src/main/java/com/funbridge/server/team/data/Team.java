package com.funbridge.server.team.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.data.PlayerHandicap;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.tournament.team.memory.TeamMemDivisionTourResultTeam;
import com.gotogames.common.tools.NumericalTools;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by pserent on 12/10/2016.
 */
@Document(collection = "team")
public class Team {
    @Id
    private ObjectId ID;
    private String name;
    private long dateCreation;
    private String description;
    private String countryCode;
    private List<TeamPlayer> players = new ArrayList<>();
    private int nbPlayers = 0; //utilisé pour effectuer des requêtes mongodb plus facilement
    private String chatroomID;
    private String division;
    private String lastPeriodWithCompleteTeam; // Dernière période pour laquelle l'équipe a bien été composée de 4 joueurs

    public String toString() {
        return "ID="+getIDStr()+" - name="+name+" - dateCreation="+ Constantes.timestamp2StringDateHour(dateCreation)+" - countryCode="+countryCode+" - players size="+players.size()+" - captain="+getCaptain();
    }

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    public boolean isCaptain(long playerID){
        TeamPlayer p = getPlayer(playerID);
        if(p == null) return false;
        else return p.isCaptain();
    }

    public TeamPlayer getCaptain(){
        for (TeamPlayer e : players) {
            if (e.isCaptain()) {
                return e;
            }
        }
        return null;
    }

    public boolean isFull(){
        return getNbPlayers() == ContextManager.getTeamMgr().getTeamSize();
    }

    public double getAveragePerformanceIMP() {
        double averagePerformance = 0.0;
        if (players.size() > 0) {
            for (TeamPlayer tp : players) {
                double averagePerformanceForPlayer = PlayerHandicap.getAveragePerformanceIMP(tp.getHandicap());
                averagePerformance += NumericalTools.round(averagePerformanceForPlayer, 4);
            }
            return averagePerformance / players.size();
        }
        return 0;
    }

    public double getAveragePerformanceMP() {
        double averagePerformance = 0.0;
        if (players.size() > 0) {
            for (TeamPlayer tp : players) {
                double averagePerformanceForPlayer = PlayerHandicap.getAveragePerformanceMP(tp.getHandicap());
                averagePerformance += NumericalTools.round(averagePerformanceForPlayer, 4);
            }
            return averagePerformance / players.size();
        }
        return 0;
    }

    public double getAverageHandicap() {
        double averageHandicap = 0.0;
        if (players.size() > 0) {
            for (TeamPlayer tp : players) {
                averageHandicap += tp.getHandicap();
            }
            return averageHandicap / players.size();
        }
        return 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(long dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public List<TeamPlayer> getPlayers() {
        return players;
    }

    public String getChatroomID() {
        return chatroomID;
    }

    public void setChatroomID(String chatroomID) {
        this.chatroomID = chatroomID;
    }

    public TeamPlayer getPlayer(long playerID) {
        for (TeamPlayer e : players) {
            if (e.getPlayerID() == playerID) {
                return e;
            }
        }
        return null;
    }

    public boolean isPlayerOfTeam(long playerID) {
        return getPlayer(playerID) != null;
    }

    public List<Long> getListPlayerID() {
        List<Long> list = new ArrayList<>();
        for (TeamPlayer e : players) {
            list.add(e.getPlayerID());
        }
        return list;
    }

    public List<Long> getListPlayerID(boolean substitute) {
        List<Long> list = new ArrayList<>();
        for (TeamPlayer e : players) {
            if (e.isSubstitute() == substitute) {
                list.add(e.getPlayerID());
            }
        }
        return list;
    }

    /**
     * Add player in the team. Set captain status if first player. Set substitue if nb players < nb lead players
     * @param playerID
     * @param playerHandicap
     * @return
     */
    public boolean addPlayer(long playerID, double playerHandicap) {
        if (getPlayer(playerID) == null && players.size() < ContextManager.getTeamMgr().getTeamSize()) {
            TeamPlayer teamPlayer = new TeamPlayer();
            teamPlayer.setPlayerID(playerID);
            teamPlayer.setCaptain(players.size() == 0);
            teamPlayer.setSubstitute(players.size()>= ContextManager.getTeamMgr().getNbLeadPlayers());
            teamPlayer.setDateJoinedTeam(System.currentTimeMillis());
            teamPlayer.setHandicap(playerHandicap);
            if(this.ID != null){
                // Set group if player isn't substitute and team is currently playing a tour
                if(!teamPlayer.isSubstitute()){
                    TeamMemDivisionTourResultTeam resultOnCurrentTour = ContextManager.getTourTeamMgr().getMemoryMgr().getTeamMemDivisionTourResultForTeam(this.getIDStr(), null);
                    // Team is currently playing a tour, the newcomer needs a group
                    if(resultOnCurrentTour != null){
                        // Let's find an empty group
                        for(String group : TourTeamMgr.GROUP_TAB){
                            boolean playerAlreadyInGroup = false;
                            for(TeamPlayer tp : players){
                                if(!tp.isSubstitute()){
                                    if(tp.getGroup() != null && tp.getGroup().equalsIgnoreCase(group)){
                                        playerAlreadyInGroup = true;
                                        break;
                                    }
                                }
                            }
                            if(!playerAlreadyInGroup){
                                teamPlayer.setGroup(group);
                                break;
                            }
                        }
                    }
                }
            }

            players.add(teamPlayer);
            nbPlayers = players.size();
            return true;
        }
        return false;
    }

    /**
     * Remove player. If player is lead => set another player lead
     * @param playerID
     * @return
     */
    public boolean removePlayer(long playerID) {
        boolean isPlayerRemove = false;
        boolean isPlayerLead = false;
        String group = null;
        Iterator<TeamPlayer> it = players.iterator();
        while (it.hasNext()) {
            TeamPlayer e = it.next();
            // no remove if player is captain !
            if (e.getPlayerID() == playerID && !e.isCaptain()) {
                it.remove();
                isPlayerRemove = true;
                isPlayerLead = !e.isSubstitute();
                group = e.getGroup();
                break;
            }
        }
        if (isPlayerRemove) {
            nbPlayers = players.size();
            if (isPlayerLead) {
                // first player substitute (older) => lead
                for (TeamPlayer e : players) {
                    if (e.isSubstitute()) {
                        e.setSubstitute(false);
                        e.setGroup(group);
                        break; // only one player to modify !
                    }
                }
            }
        }
        return isPlayerRemove;
    }

    public int getNbPlayers() {
        return nbPlayers;
    }

    public void setNbPlayers(int nbPlayers) {
        this.nbPlayers = nbPlayers;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getLastPeriodWithCompleteTeam() {
        return lastPeriodWithCompleteTeam;
    }

    public void setLastPeriodWithCompleteTeam(String lastPeriodWithCompleteTeam) {
        this.lastPeriodWithCompleteTeam = lastPeriodWithCompleteTeam;
    }
}
