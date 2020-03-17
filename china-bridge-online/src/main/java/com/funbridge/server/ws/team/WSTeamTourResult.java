package com.funbridge.server.ws.team;

/**
 * Created by pserent on 15/11/2016.
 */
public class WSTeamTourResult implements Comparable<WSTeamTourResult> {
    public WSTeamPlayer player;
    public double result;
    public int rank;
    public int count;
    public int points;
    public int nbPlayedDeals;
    public int nbDeals;

    @Override
    public int compareTo(WSTeamTourResult o) {
        if (this.player == null || this.player.group == null || this.player.group.isEmpty()) return -1;
        if (o.player == null || o.player.group == null || o.player.group.isEmpty()) return 1;
        return this.player.group.compareToIgnoreCase(o.player.group);
    }
}
