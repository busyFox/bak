package com.funbridge.server.ws.player;

import java.util.List;

public class WSPlayerProfile {
    public String firstName;
    public String lastName;
    public int sex;
    public String countryCode;
    public String town;
    public String description;
    public long birthdate;
    public boolean avatarPresent = false;
    public long creationDate = 0;
    public int nbPlayedDeals = 0;
    public String serie;
    public int conventionProfile = -1;
    public List<WSPlayerPeriodResult> historicPeriod = null;
    public int nbFriendsAndFollowers = 0;
    public int linkMask = 0;
    public int nbDuelWin = 0;
    public int nbDuelLost = 0;
    public int nbDuelDraw = 0;

    public String toString() {
        return "firstName=" + firstName + " - lastName=" + lastName + " - avatarPresent=" + avatarPresent;
    }
}
