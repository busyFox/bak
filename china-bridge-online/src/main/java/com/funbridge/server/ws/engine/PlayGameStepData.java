package com.funbridge.server.ws.engine;

import com.funbridge.server.tournament.game.GameMgr;

/**
 * Created by pserent on 13/04/2016.
 */
public class PlayGameStepData {
    public String asyncID;
    public String sessionID;
    public int step;
    public long timestamp;
    public String param;
    public GameMgr gameMgr;
    public int requestType;
    public boolean playArgine = false;

    public static String buildAsyncID(String dataID, int step) {
        return dataID + "-step-" + step;
    }

    public static String buildAsyncID(String dataID) {
        return dataID;
    }

    public static String extractDataID(String ID) {
        if (ID != null) {
            if (ID.indexOf("-step-") >= 0) {
                return ID.substring(0, ID.indexOf("-step-"));
            }
        }
        return ID;
    }

    public String toString() {
        return "asyncID=" + asyncID + " - dataID=" + getDataID() + " - gameMgr=" + gameMgr + " - sessionID=" + sessionID + " - step=" + step + " - timestamp=" + timestamp + " - param=" + param + " - requestType=" + requestType + " - playArgine=" + playArgine;
    }

    public String getDataID() {
        return extractDataID(asyncID);
    }
}
