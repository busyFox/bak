package com.funbridge.server.tournament.team.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object to store team result on periodTour
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TeamMemDivisionTourResult {
    public String division;

    // Map to store team result : TEAM_ID <==> RESULT_TEAM
    public Map<String, TeamMemDivisionTourResultTeam> mapResultTeam = new ConcurrentHashMap<>();
    public int nbTeam = 0;

    public TeamMemDivisionTourResult(){}
    public TeamMemDivisionTourResult(String div){
        this.division = div;
    }
    public String toString() {
        return "division="+division+" - nbTeam="+mapResultTeam.size();
    }

    public void clearData() {
        mapResultTeam.clear();
        nbTeam = 0;
    }

    public List<String> listTeams() {
        return new ArrayList<>(mapResultTeam.keySet());
    }

    public List<TeamMemDivisionTourResultTeam> listTeamResults() {
        return new ArrayList<>(mapResultTeam.values());
    }

    public int getNbTeam() {
        return mapResultTeam.size();
    }

    /**
     * Load all teams playing the tour
     * @param listTeamID
     */
    public void loadTeamData(List<String> listTeamID){
        clearData();
        for(String teamID : listTeamID){
            mapResultTeam.put(teamID, new TeamMemDivisionTourResultTeam(teamID));
        }
    }

    /**
     * Add player result on tournament for the team
     * @param memTourPlayer
     * @param computeRanking
     * @return
     */
    public TeamMemDivisionTourResultTeam addPlayerTournamentResult(TeamMemTournamentPlayer memTourPlayer, boolean computeRanking) {
        if (memTourPlayer != null) {
            TeamMemDivisionTourResultTeam teamResult = getOrCreateTeamResult(memTourPlayer.teamID);
            if (teamResult != null) {
                // update team result on periodTour
                teamResult.setTourResultForPlayer(memTourPlayer);

                // compute result for other teams
                for (TeamMemDivisionTourResultTeam tourResultTeam : listTeamResults()) {
                    if (!tourResultTeam.teamID.equals(memTourPlayer.teamID)) {
                        tourResultTeam.computeResult();
                    }
                }

                // compute ranking
                if (computeRanking) {
                    computeRanking();
                }
                return teamResult;
            }
        }
        return null;
    }

    /**
     * Return the result of team
     * @param teamID
     * @return
     */
    public TeamMemDivisionTourResultTeam getTeamResult(String teamID) {
        return mapResultTeam.get(teamID);
    }

    /**
     * Return the result of team. If not yet existing, create a new one for team with default value (rank=-1 ...).
     * @param teamID
     * @return
     */
    public TeamMemDivisionTourResultTeam getOrCreateTeamResult(String teamID) {
        TeamMemDivisionTourResultTeam teamResult = mapResultTeam.get(teamID);
        if (teamResult == null) {
            teamResult = new TeamMemDivisionTourResultTeam();
            teamResult.teamID = teamID;
            mapResultTeam.put(teamID, teamResult);
        }
        return teamResult;
    }

    public void computeResult() {
        for (TeamMemDivisionTourResultTeam e : mapResultTeam.values()) {
            e.computeResult();
        }
    }

    /**
     * Compute the ranking for all team
     */
    public void computeRanking() {
        int curRank = 0;
        int nbPlayerBefore = 0;
        double curResult = 0;
        List<String> listKeyTeam = getListTeamOrderResult();
        for (String key : listKeyTeam) {
            TeamMemDivisionTourResultTeam resPla = mapResultTeam.get(key);
            if (resPla != null) {
                // first element
                if (curRank == 0) {
                    resPla.rank = 1;
                    curRank = 1;
                    nbPlayerBefore = 1;
                    curResult = resPla.result;
                } else {
                    if (resPla.result != curResult) {
                        curResult = resPla.result;
                        curRank = nbPlayerBefore+1;
                    }
                    resPla.rank = curRank;
                    nbPlayerBefore++;
                }
            }
        }
    }

    /**
     * Return the list of team order by result
     * @return
     */
    public List<String> getListTeamOrderResult() {
        List<String> listKeyPla = new ArrayList<>(mapResultTeam.keySet());
        Collections.sort(listKeyPla, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                TeamMemDivisionTourResultTeam elem1 = mapResultTeam.get(o1);
                TeamMemDivisionTourResultTeam elem2 = mapResultTeam.get(o2);
                return elem1.compareTo(elem2);
            }
        });
        return listKeyPla;
    }

    /**
     * Return a ranking in this division for current periodTour at offset with nb max element in list
     * @param offset
     * @param nbMax max value in list result or -1 to have all
     * @return
     */
    public List<TeamMemDivisionTourResultTeam> getRanking(int offset, int nbMax) {
        List<TeamMemDivisionTourResultTeam> listRanking = new ArrayList<TeamMemDivisionTourResultTeam>();
        // sort ranking
        List<String> listKey = getListTeamOrderResult();
        // return a list of ranking
        if (listKey.size() > offset) {
            int nbElemValid = 0;
            for (int i=0; i < listKey.size(); i++) {
                if (nbMax >0 && listRanking.size() >= nbMax) {
                    break;
                }
                TeamMemDivisionTourResultTeam mtr = mapResultTeam.get(listKey.get(i));
                if (mtr != null) {
                    nbElemValid++;
                    if (nbElemValid > offset) {
                        listRanking.add(mtr);
                    }
                }
            }
        }
        return listRanking;
    }
}
