package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSDealDuel {
    public String dealIDstr;
	public double result;
	public String gameIDPlayer1str;
	public String declarerPlayer1;
	public String contractPlayer1;
	public int nbTricksPlayer1;
	public int scorePlayer1;
    public boolean playedPlayer1 = false;
	public String gameIDPlayer2str;
	public String declarerPlayer2;
	public String contractPlayer2;
	public int nbTricksPlayer2;
	public int scorePlayer2;
    public boolean playedPlayer2 = false;
    public String leadPlayer1;
    public String leadPlayer2;
}
