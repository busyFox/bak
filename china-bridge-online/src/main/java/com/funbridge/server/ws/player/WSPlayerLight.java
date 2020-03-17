package com.funbridge.server.ws.player;

/**
 * Created by pserent on 09/07/2015.
 */
public class WSPlayerLight {
    public long playerID = 0;
    public String pseudo = "";
    public boolean avatar = false;
    public boolean connected = false;
    public String countryCode;
    public int relationMask; // mask of relation between the player who asks and this player
    public WSTrainingPartnerStatus trainingPartnerStatus = null;

}
