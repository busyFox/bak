package com.funbridge.server.ws.player;

/**
 * Created by pserent on 14/04/2016.
 */
public class WSPlayerDuel {
    public long playerID;
    public String pseudo;
    public boolean avatar;
    public String countryCode;
    public boolean connected = false;
    public int nbDuelWin;
    public int nbDuelDraw;
    public int nbDuelLost;
}
