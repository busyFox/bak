package com.funbridge.server.ws.player;

import com.funbridge.server.presence.FBSession;

/**
 * Created by pserent on 30/10/2015.
 */
public class WSPlayerConnected {
    public long playerID;
    public String pseudo;
    public boolean avatar;
    public String serie;
    public String countryCode;

    public WSPlayerConnected() {
    }

    public WSPlayerConnected(FBSession fbs) {
        this.playerID = fbs.getPlayer().getID();
        this.pseudo = fbs.getPlayer().getNickname();
        this.avatar = fbs.getPlayer().isAvatarPresent();
        this.serie = fbs.getSerie();
        this.countryCode = fbs.getPlayer().getDisplayCountryCode();
    }
}
