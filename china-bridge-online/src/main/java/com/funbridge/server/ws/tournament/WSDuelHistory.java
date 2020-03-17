package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.game.WSGamePlayer;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSDuelHistory {
	public long playerDuelID;
    public String playerDuelIDstr;
	public WSGamePlayer player1;
	public WSGamePlayer player2;
	public int status = 0;
	public long creationDate = 0;
	public long expirationDate = 0;
	public int resetRequest = 0;
	public long tourID = 0;
    public String tourIDstr;
	public int nbWinPlayer1 = 0;
	public int nbWinPlayer2 = 0;
	public int nbDraw = 0;
    public long dateLastDuel = 0;
    @JsonIgnore
    public int orderList = 0;
    @JsonIgnore
    public boolean arginePlayAll = false;
	
	public boolean arePlayersValid() {
		return (player1 != null && player2 != null);
	}
	
	public String toString() {
		return "duelID="+playerDuelID+" - player1="+player1+" - player2="+player2+" - status="+status+" - resetRequest="+resetRequest+" - nbWinPlayer1="+nbWinPlayer1+" - nbWinPlayer2="+nbWinPlayer2+" - nbDraw="+nbDraw;
	}

    public void setTourID(long tourID) {
        this.tourID = tourID;
        this.tourIDstr = ""+tourID;
    }

    public void setPlayerDuelID(long playerDuelID) {
        this.playerDuelID = playerDuelID;
        this.playerDuelIDstr = ""+playerDuelID;
    }
}
