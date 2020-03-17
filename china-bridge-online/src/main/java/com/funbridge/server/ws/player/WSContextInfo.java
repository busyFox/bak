package com.funbridge.server.ws.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.FBConfiguration;

public class WSContextInfo {
    public int nbPlayerConnected = 0;
    public String playerSerie;
    public int nbNewMessage = 0;
    public double frequencyEventFast = 2;
    public double frequencyEventMedium = 5;
    public double frequencyEventSlow = 10;
    public boolean enableClaim = false;
    public int nbDuelRequest = 0;
    public int nbDuelReset = 0;
    public int nbDuelInProgress = 0;
    public long serverTime = System.currentTimeMillis();
    public WSServerParameters serverParam = null;
    public int nbActivePlayers = 0;
    public int nbCountryCode = 0;
    public int nbDuelCommunity = 0;
    public String cloudinaryChatUploadPreset = FBConfiguration.getInstance().getStringValue("cloudinaryChatPreset", "chatroom");

    @JsonIgnore
    public String toString() {
        return "playerSerie=" + playerSerie + " - " +
                "nbNewMessage=" + nbNewMessage + " - " +
                "nbPlayerConnected=" + nbPlayerConnected + " - " +
                "frequencyEventFast=" + frequencyEventFast + " - " +
                "frequencyEventMedium=" + frequencyEventMedium + " - " +
                "frequencyEventSlow=" + frequencyEventSlow + " - " +
                "enableClaim=" + enableClaim + " - " +
                "nbDuelRequest=" + nbDuelRequest + " - " +
                "nbDuelReset=" + nbDuelReset + " - " +
                "nbDuelInProgress=" + nbDuelInProgress;
    }
}
