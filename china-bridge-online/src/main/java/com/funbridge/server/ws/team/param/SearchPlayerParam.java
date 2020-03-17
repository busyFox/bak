package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 19/10/2016.
 */
public class SearchPlayerParam {
    public String search;
    public boolean friend;
    public String countryCode;
    public int offset;
    public int nbMax;

    public String toString() {
        return "search=" + search + " - friend=" + friend + " - countryCode=" + countryCode + " - offset=" + offset + " - nbMax=" + nbMax;
    }

    public boolean isValid() {
        if (friend && (search != null && !search.isEmpty())) return false; // No search pattern when friend filter is on
        return !friend || (countryCode == null || countryCode.isEmpty()); // No countryCode when friend filter is on
    }
}
