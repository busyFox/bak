package com.funbridge.server.tournament.team.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gotogames.common.tools.NumericalTools;

/**
 * Résultat d'une équipe sur la période
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TeamMemDivisionResultTeam implements Comparable<TeamMemDivisionResultTeam>{
    public String teamID;
    public int rank = -1;
    public int pointsPeriod; // points figé au debut de chaque tour
    public int trend;
    public int points; // points en temps réel en tenant compte du tour actuel
    public double resultsPeriod; // cumul resultats IMP sur la période pour tous les tournois
    public double results; // resultats IMP en temps réel en tenant compte du tour actuel
    public double averageHandicap = 0;

    public TeamMemDivisionResultTeam(){}
    public TeamMemDivisionResultTeam(String teamID){
        this.teamID = teamID;
    }

    /**
     * Update points with result on current periodTour
     * @param teamResult
     */
    public void setTeamResult(TeamMemDivisionTourResultTeam teamResult) {
        if (teamResult != null) {
            points = pointsPeriod + teamResult.points;
            results = resultsPeriod + teamResult.result;
        }
    }

    @Override
    public int compareTo(TeamMemDivisionResultTeam o) {
        // compare points, if same => compare results, if same => compare handicap
        // points of this object is bigger so ranking is smaller ! Big points => small ranking
        if (o.points > this.points) {
            return 1;
        } else if (o.points < this.points) {
            return -1;
        }
        if (o.results > this.results) {
            return 1;
        }
        if (o.results < this.results) {
            return -1;
        }
        if (o.averageHandicap > this.averageHandicap) {
            return 1;
        }
        if (o.averageHandicap < this.averageHandicap) {
            return -1;
        }
        return 0;
    }

    public double getRankPercent(int nbTeams) {
        if (rank > 0 && nbTeams > 0) {
            return 100 - NumericalTools.round((double)100*rank/nbTeams, 2);
        }
        return 0;
    }
}
