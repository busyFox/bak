package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 12/10/2016.
 */
public class ListTeamsParam {
    public String search;
    public String countryCode;
    public boolean searchMode = true; // search mode or list mode ...
    public int offset;
    public int nbMax;

    public String toString() {
        return "search=" + search + " - searchMode=" + searchMode + " - countryCode=" + countryCode + " - offset=" + offset + " - nbMax=" + nbMax;
    }

    public boolean isValid() {
        return true;
    }
}
