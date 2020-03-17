package com.funbridge.server.ws.tournament;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="tournamentResultPlayer")
public class WSTournamentResultPlayer {
	public int nbTotalPlayer = 0;
	public int rank = 0;
	public double result = 0;
	public int nbDealPlayed = 0;
}
