package com.funbridge.server.ws.support;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSSupTournamentPlay {
	public long tourID;
    public String tourIDstr;
	public String name;
	public boolean finished;
	public String dateStart;
	public String dateEnd;
	public int category;
	public int nbPlayer;
	public int nbDeal;
	public String playerDateStart;
	public String playerDateLast;
	public int playerRank;
	public double playerResult;
	public double opponentResult;
	public boolean playerFinished;
    public int resultType;
	
	public WSSupTournamentPlay() {}
	
	public void setTourID(long tourID) {
        this.tourID = tourID;
        this.tourIDstr = ""+tourID;
    }
}
