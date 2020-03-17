package com.funbridge.server.ws.tournament;

import javax.xml.bind.annotation.XmlRootElement;

import com.funbridge.server.tournament.data.TournamentSettings;

@XmlRootElement(name="trainingPartner")
public class WSTrainingPartner {
	public long playerID;
	public String pseudo;
	public boolean avatar = false;
	public boolean connected = false;
	public long challengeID = -1;
	public int challengeStatus = 0;
	public long creatorID = 0;
	public TournamentSettings dealSettings = null;
}
