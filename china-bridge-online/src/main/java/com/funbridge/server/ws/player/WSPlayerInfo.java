package com.funbridge.server.ws.player;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;

public class WSPlayerInfo {
    public long playerID = 0;
    public String pseudo = "";
    public String cert = "" ;
    public int type = 0 ;  //0: phone ; 1: oppenId
    public String mail = "";
    public int creditAmount = 0;
    public boolean receiveNewsletter = false;
    public int subscriptionMask = 0;
    public WSPlayerProfile profile = null;
    public long accountExpirationDate = 0;
    public String settings = null;
    public int pushMask = 0;
    public String serie;
    public boolean avatar = false;
    public boolean pseudoChange = false;
    public boolean mailValid = false;
    public int notificationFriendMask = 0;
    public int friendMask = 0;
    public WSPlayerStat stat = null;
    public int storePromo = 0;
    public long subscriptionTimeLeft = 0;
    public boolean freemium = false;
    public boolean replayEnabled = true;
    public int credentialsMask = 0;

    @JsonIgnore
    public String toString() {
        return "playerID=" + playerID + " - pseudo=" + pseudo  + " - cert=" + cert + " - type=" + type + " - mail=" + mail + " - creditAmount=" + creditAmount + " - accountExpirationDate=" + Constantes.timestamp2StringDateHour(accountExpirationDate) + " - storePromo=" + storePromo + " - freemium=" + freemium + " - replayEnabled=" + replayEnabled;
    }
}
