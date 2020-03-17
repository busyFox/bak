package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 19/10/2016.
 */
public class ChangeCaptainParam {
    public long playerID = -1;

    public String toString() {
        return "playerID=" + playerID;
    }

    public boolean isValid() {
        return playerID != -1;
    }
}
