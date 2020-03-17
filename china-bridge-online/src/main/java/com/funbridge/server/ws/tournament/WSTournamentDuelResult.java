package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSTournamentDuelResult {
	public long tourID;
    public String tourIDstr;
	public long dateFinish = 0;
	public double resultPlayer1 = 0;
	public double resultPlayer2 = 0;
	
	public String toString() {
		return "tourID="+tourIDstr+" - dateFinish="+dateFinish+" - resultPlayer1="+resultPlayer1+" - resultPlayer2="+resultPlayer2;
	}
}
