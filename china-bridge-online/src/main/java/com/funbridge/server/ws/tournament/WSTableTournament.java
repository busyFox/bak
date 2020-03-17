package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.game.WSTableGame;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSTableTournament {
	public WSTableGame table;
	public WSTournament tournament;
	public WSGameDeal currentDeal = null;
	public long gameID = -1;
    public String gameIDstr = "";
	public int conventionProfil = -1;
	public int creditAmount = -1;
	public String conventionValue = null;
	public int nbPlayedDeals;
	public boolean replayEnabled = true;
	public boolean freemium = false;
	
	@JsonIgnore
	public String toString() {
		return "table="+table+" - tournament="+tournament+" - currentDeal="+currentDeal+" - gameID="+gameIDstr+" - conventionProfil="+conventionProfil+" - creditAmount="+creditAmount+" - nbPlayedDeals="+nbPlayedDeals+" - replayEnabled="+replayEnabled+" - freemium="+freemium;
	}

    public void setGameID(long gameID) {
        this.gameID = gameID;
        this.gameIDstr = ""+gameID;
    }
}
