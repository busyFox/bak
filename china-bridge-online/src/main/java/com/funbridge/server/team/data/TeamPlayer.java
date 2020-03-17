package com.funbridge.server.team.data;

/**
 * Created by pserent on 12/10/2016.
 */
public class TeamPlayer {
    private long playerID;
    private long dateJoinedTeam;
    private boolean substitute = false;
    private boolean captain = false;
    private double handicap;
    private String group;

    public String toString() {
        return "playerID="+playerID+" - captain="+captain+" - substitute="+substitute+" - group="+group+" - handicap="+handicap;
    }

    public boolean isLead(){
        return !substitute;
    }

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public long getDateJoinedTeam() {
        return dateJoinedTeam;
    }

    public void setDateJoinedTeam(long dateJoinedTeam) {
        this.dateJoinedTeam = dateJoinedTeam;
    }

    public boolean isSubstitute() {
        return substitute;
    }

    public void setSubstitute(boolean substitute) {
        this.substitute = substitute;
    }

    public boolean isCaptain() {
        return captain;
    }

    public void setCaptain(boolean captain) {
        this.captain = captain;
    }

    public double getHandicap() {
        return handicap;
    }

    public void setHandicap(double handicap) {
        this.handicap = handicap;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
