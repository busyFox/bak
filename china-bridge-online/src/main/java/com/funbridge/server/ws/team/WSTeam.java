package com.funbridge.server.ws.team;

import java.util.List;

/**
 * Created by pserent on 12/10/2016.
 */
public class WSTeam {
    public String ID;
    public String name;
    public String description;
    public String countryCode;
    public List<WSTeamPlayer> players = null;
    public int nbFriends;
    public String division;
    public int divisionNbTeam;
    public int rank;
    public int trend;
    public String trendDivision;
    public int points;
    public String divisionHistory;
}
