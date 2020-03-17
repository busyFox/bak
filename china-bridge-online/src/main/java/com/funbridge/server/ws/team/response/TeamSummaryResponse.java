package com.funbridge.server.ws.team.response;

import com.funbridge.server.ws.team.WSTeam;
import com.funbridge.server.ws.team.WSTeamPeriod;
import com.funbridge.server.ws.team.WSTeamTour;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 12/10/2016.
 */
public class TeamSummaryResponse {
    public WSTeam team;
    public int nbRequests;
    public int nbMessages;
    public String chatroomID;
    public WSTeamPeriod period;
    public WSTeamTour tour;
    public long dateNextTour;
    public long dateNextPeriod;
    public boolean championshipStarted;
    public int nbTourPerPeriod;
    public int nbMaxPlayersPerTeam;
    public int nbLeadPlayersPerTeam;
    public List<String> listAvailablePeriodID = new ArrayList<>();
    public int nbTeams;
}
