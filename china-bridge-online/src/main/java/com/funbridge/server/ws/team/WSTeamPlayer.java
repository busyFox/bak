package com.funbridge.server.ws.team;

/**
 * Created by pserent on 12/10/2016.
 */
public class WSTeamPlayer {
    public long playerID;
    public String pseudo;
    public boolean avatar;
    public String countryCode;
    public boolean connected = false;
    public boolean captain = false;
    public boolean substitute = false;
    public double averagePerformance;
    public String serie;
    public String group;
    public boolean requestPending = false;
}
