package com.funbridge.server.ws.game;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class WSPlayDealInfo {
    public long gameID;
    public String gameIDstr;
    public int conventionProfil;
    public int creditAmount;

    @JsonIgnore
    public String toString() {
        return "gameID=" + gameIDstr + " - convention=" + conventionProfil + " - creditAmount=" + creditAmount;
    }

    public void setGameID(long gameID) {
        this.gameID = gameID;
        this.gameIDstr = "" + gameID;
    }
}
