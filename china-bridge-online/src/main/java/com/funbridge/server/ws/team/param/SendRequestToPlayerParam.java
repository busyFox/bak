package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 12/10/2016.
 */
public class SendRequestToPlayerParam {
    public long playerID;

    public String toString() {
        return "playerID=" + playerID;
    }

    public boolean isValid() {
        return playerID > 0;
    }
}
