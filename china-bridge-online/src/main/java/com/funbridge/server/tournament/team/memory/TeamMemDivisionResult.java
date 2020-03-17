package com.funbridge.server.tournament.team.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.team.TourTeamMgr;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Résultat des équipes sur la période pour une division
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TeamMemDivisionResult {
    public String division;
    public double thresholdUp;
    public double thresholdDown;
    public double averagePerformanceMinimum; // static value, compute at the first time and save in the configuration file ...
    // Map to store team result : TEAM_ID <==> RESULT_TEAM
    public Map<String, TeamMemDivisionResultTeam> mapResultTeam = new ConcurrentHashMap<>();

    public TeamMemDivisionResult() {}

    public TeamMemDivisionResult(String div, double averagePerformanceValue) {
        this.division = div;
        this.averagePerformanceMinimum = averagePerformanceValue;
    }

    public String toString() {
        return "division="+division+" - nb teams="+getNbTeams()+" - thresholdUp="+thresholdUp+" - thresholdDown="+thresholdDown;
    }

    public int getNbTeams() {
        return mapResultTeam.size();
    }

    public void clearData() {
        mapResultTeam.clear();
    }

    public List<TeamMemDivisionResultTeam> listDivisionResultTeam() {
        return new ArrayList<>(mapResultTeam.values());
    }

    /**
     * Load the threshold values from configuration
     */
    public void loadThresholdFromConfiguration() {
        // Threshold UP
        double tempUp = ContextManager.getTourTeamMgr().getConfigDoubleValue("division."+division+".thresholdUp", -1);
        if (tempUp == -1) {
            tempUp = ContextManager.getTourTeamMgr().getConfigDoubleValue("division.thresholdUp", 0.3334);
        }
        thresholdUp = tempUp;
        // Threshold result DOWN
        double tempDown = ContextManager.getTourSerieMgr().getConfigDoubleValue("division."+division+".thresholdDown", -1);
        if (tempDown == -1) {
            tempDown = ContextManager.getTourSerieMgr().getConfigDoubleValue("division.thresholdDown", 0.3334);
        }
        thresholdDown = tempDown;
    }

    /**
     * Load all teams playing the tour
     * @param listTeamID
     */
    public void loadTeamData(List<String> listTeamID){
        clearData();
        for(String teamID : listTeamID){
            mapResultTeam.put(teamID, new TeamMemDivisionResultTeam(teamID));
        }
    }

    /**
     * Set team points and result values for period
     * @param teamID
     * @param points
     * @param result
     */
    public void setTeamPointsAndResultPeriod(String teamID, int points, double result) {
        TeamMemDivisionResultTeam resultTeam = mapResultTeam.get(teamID);
        if (resultTeam == null) {
            resultTeam = new TeamMemDivisionResultTeam(teamID);
            mapResultTeam.put(teamID, resultTeam);
        }
        resultTeam.pointsPeriod = points;
        resultTeam.resultsPeriod = result;
    }

    /**
     * Set team average handicap value
     * @param teamID
     * @param value
     */
    public void setTeamAverageHandicap(String teamID, double value) {
        TeamMemDivisionResultTeam resultTeam = mapResultTeam.get(teamID);
        if (resultTeam == null) {
            resultTeam = new TeamMemDivisionResultTeam(teamID);
            mapResultTeam.put(teamID, resultTeam);
        }
        resultTeam.averageHandicap = value;
    }

    /**
     * On change tour, set pointsPeriod with current points value
     */
    public void updatePointsOnChangeTour() {
        for (TeamMemDivisionResultTeam resultTeam : mapResultTeam.values()) {
            resultTeam.pointsPeriod = resultTeam.points;
            resultTeam.resultsPeriod = resultTeam.results;
        }
    }

    /**
     * Set result for team on current periodTour
     * @param tourResultTeam
     * @param computeRanking
     */
    public void addTourResultTeamForCurrentPeriodTour(TeamMemDivisionTourResultTeam tourResultTeam, TeamMemDivisionTourResult tourResult, boolean computeRanking) {
        if (tourResultTeam != null) {
            TeamMemDivisionResultTeam resultTeam = mapResultTeam.get(tourResultTeam.teamID);
            if (resultTeam == null) {
                resultTeam = new TeamMemDivisionResultTeam();
                resultTeam.teamID = tourResultTeam.teamID;
                mapResultTeam.put(tourResultTeam.teamID, resultTeam);
            }
            resultTeam.setTeamResult(tourResultTeam);

            // update result for other teams
            for (TeamMemDivisionTourResultTeam e : tourResult.listTeamResults()) {
                if (!e.teamID.equals(tourResultTeam.teamID)) {
                    TeamMemDivisionResultTeam result = mapResultTeam.get(e.teamID);
                    if (result != null) {
                        result.setTeamResult(e);
                    }
                }
            }

            if (computeRanking) {
                computeRanking();
            }
        }
    }

    public int getPositionThresholdUp() {
        if (division.equals(TourTeamMgr.DIVISION_01)) {
            return 0;
        }
        return (int)(mapResultTeam.size() * thresholdUp);
    }

    public int getPositionThresholdDown() {
        if (division.equals(TourTeamMgr.DIVISION_TAB[TourTeamMgr.DIVISION_TAB.length-1])) {
            return Integer.MAX_VALUE;
        }
        return mapResultTeam.size() - (int)(mapResultTeam.size() * thresholdDown);
    }

    /**
     * Compute ranking and trend
     */
    public void computeRanking() {
        int curRank = 0;
        int nbTeamBefore = 0;
        int curPoints = 0;
//        double curResult = 0;
//        double curHandicap = 0;
        int posThresholdUp = getPositionThresholdUp();
        int posThresholdDown = getPositionThresholdDown();
        List<String> listKeyTeam = getListTeamOrderPointsResultAndPerformance();
        for (String key : listKeyTeam) {
            TeamMemDivisionResultTeam resPla = mapResultTeam.get(key);
            if (resPla != null) {
                // first element
                if (curRank == 0) {
                    resPla.rank = 1;
                    curRank = 1;
                    nbTeamBefore = 1;
                    curPoints = resPla.points;
                } else {
                    if ((resPla.points != curPoints)){
                        curPoints = resPla.points;
                        curRank = nbTeamBefore+1;
                    }
                    resPla.rank = curRank;
                    nbTeamBefore++;
                }
                if (nbTeamBefore <= posThresholdUp) {
                    resPla.trend = 1;
                }
                else if (nbTeamBefore > posThresholdDown) {
                    resPla.trend = -1;
                }
                else {
                    resPla.trend = 0;
                }
            }
        }
    }

    /**
     * Return the list of team order by points, result and performance
     * @return
     */
    public List<String> getListTeamOrderPointsResultAndPerformance() {
        List<String> listKeyPla = new ArrayList<>(mapResultTeam.keySet());
        Collections.sort(listKeyPla, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                TeamMemDivisionResultTeam elem1 = mapResultTeam.get(o1);
                TeamMemDivisionResultTeam elem2 = mapResultTeam.get(o2);
                return elem1.compareTo(elem2);
            }
        });
        return listKeyPla;
    }

    /**
     * Return a ranking in this division for period at offset with nb max element in list
     * @param offset
     * @param nbMax max value in list result or -1 to have all
     * @return
     */
    public List<TeamMemDivisionResultTeam> getRanking(int offset, int nbMax) {
        List<TeamMemDivisionResultTeam> listRanking = new ArrayList<TeamMemDivisionResultTeam>();
        // sort ranking
        List<String> listKey = getListTeamOrderPointsResultAndPerformance();
        // return a list of ranking
        if (listKey.size() > offset) {
            int nbElemValid = 0;
            for (int i=0; i < listKey.size(); i++) {
                if (nbMax >0 && listRanking.size() >= nbMax) {
                    break;
                }
                TeamMemDivisionResultTeam mtr = mapResultTeam.get(listKey.get(i));
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

    /**
     * Return a ranking for team in this division for period
     * @param teamID
     * @return
     */
    public TeamMemDivisionResultTeam getRankingForTeam(String teamID) {
        return mapResultTeam.get(teamID);
    }

    /**
     * Return the result of team
     * @param teamID
     * @return
     */
    public TeamMemDivisionResultTeam getTeamResult(String teamID) {
        return mapResultTeam.get(teamID);
    }

}
