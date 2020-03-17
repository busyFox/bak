package com.funbridge.server.ws.player;

import com.funbridge.server.tournament.data.TournamentSettings;

public class WSTrainingPartnerStatus {
    public long challengeID = -1;
    public int challengeStatus = 0; // (challenger statuse - 0=init, 1=waiting, 2=play, 3=end)
    public long creatorID = 0; //(creator ID)
    public TournamentSettings dealSettings = null;
}
