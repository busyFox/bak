package com.funbridge.server.ws.game;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class WSTableGame {
    public long tableID;
    public WSGamePlayer playerSouth = null;
    public WSGamePlayer playerWest = null;
    public WSGamePlayer playerNorth = null;
    public WSGamePlayer playerEast = null;
    public long leaderID = -1;

    @JsonIgnore
    public String toString() {
        return "tableID=" + tableID + " - leaderID=" + leaderID + " - south=" + playerSouth + " - west=" + playerWest + " - north=" + playerNorth + " - east=" + playerEast;
    }
}
