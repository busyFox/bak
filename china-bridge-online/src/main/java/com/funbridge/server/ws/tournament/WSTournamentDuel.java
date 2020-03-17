package com.funbridge.server.ws.tournament;

import java.util.List;

import com.funbridge.server.ws.game.WSGamePlayer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSTournamentDuel {
	public long playerDuelID;
    public String playerDuelIDstr;
	public long tourID;
    public String tourIDstr;
	public WSGamePlayer player1;
	public WSGamePlayer player2;
	public List<WSDealDuel> listDeal;
	public int nbDeal = 0;

    public void setTourID(long tourID) {
        this.tourID = tourID;
        this.tourIDstr = ""+tourID;
    }

    public void setPlayerDuelID(long playerDuelID) {
        this.playerDuelID = playerDuelID;
        this.playerDuelIDstr = ""+playerDuelID;
    }
}
