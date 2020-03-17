package com.funbridge.server.ws.team;

/**
 * Created by pserent on 29/11/2016.
 */
public class WSTeamPeriodResult implements Comparable<WSTeamPeriodResult> {
    public WSTeamPlayer player;
    public double result;
    public int rank = -1;
    public int count;
    public int points;
    public int nbPlayedTours;

    @Override
    public int compareTo(WSTeamPeriodResult o) {
        if (o.points > this.points) {
            return 1;
        } else if (o.points == this.points) {
            return 0;
        }
        return -1;
    }
}
