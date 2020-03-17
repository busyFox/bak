package com.funbridge.server.ws.result;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.player.cache.PlayerCache;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSMainRankingPlayer {
    public long playerID = 0;
    public int rank;
    public String playerPseudo = "";
    public String countryCode;
    public String lang;
    public boolean avatarPresent;
    public boolean connected = false;
    public double value;
    public double pfValue;
    public int nbTournmanents;

    public WSMainRankingPlayer() {}

    public WSMainRankingPlayer(long playerID) {
        this.playerID = playerID;
    }

    public WSMainRankingPlayer(PlayerCache pc, boolean connected, long playerAsk) {
        if (pc != null) {
            this.playerID = pc.ID;
            this.playerPseudo = pc.getPseudo();
            this.countryCode = pc.countryCode;
            this.lang = pc.lang;
            if (playerAsk == playerID) {
                this.avatarPresent = pc.avatarPresent;
            } else {
                this.avatarPresent = pc.avatarPublic;
            }
        }
        this.connected = connected;
    }

    public String toString() {
        return "playerID="+playerID+" - pseudo="+playerPseudo+" - country="+countryCode+" - rank="+rank+" - value="+value;
    }

    public String getStringValue() {
        return ""+value;
    }

    public long getPlayerID() {
        return playerID;
    }

    public int getRank() {
        return rank;
    }

    public String getPlayerPseudo() {
        return playerPseudo;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public boolean isAvatarPresent() {
        return avatarPresent;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setPlayerPseudo(String playerPseudo) {
        this.playerPseudo = playerPseudo;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void setAvatarPresent(boolean avatarPresent) {
        this.avatarPresent = avatarPresent;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
