package com.funbridge.server.ws.player;

import com.funbridge.server.player.data.PlayerDuelStat;
import com.funbridge.server.ws.tournament.WSDuelHistory;

public class WSPlayer {
    public long playerID = 0;
    public String pseudo = "";
    public boolean avatar = false;
    public boolean connected = false;
    public int nbDealPlayed = 0;
    public int nbFriendsAndFollowers = 0;
    public int nbFriends = 0;
    public int nbFollowers = 0;
    public int relationMask; // mask of relation between the player who asks and this player
    public int nbMessageNotRead; // nb message not read send by this player to the one who asks
    public String requestMessage; // message in the friend request done by this player
    public long dateLastMessage; // the timestamp of the last message exchanged between the 2 players
    public WSProfile profile;
    public WSDuelHistory duelHistory = null;
    public WSTrainingPartnerStatus trainingPartnerStatus = null;
    public WSSerieStatus serieStatus = null;
    public int nbDuelPlayed = 0;
    public int nbDuelWin = 0;
    public int nbDuelLost = 0;
    public int nbDuelDraw = 0;
    public String serieBest = null;
    public int serieBestRank = 0;
    public long serieBestPeriodStart = 0;
    public long serieBestPeriodEnd = 0;
    public int conventionProfil = 0;
    public String conventionValue = "";
    public int cardsConventionProfil = 0;
    public String cardsConventionValue = "";
    public int relationMaskOther; // mask of relation between this player and the one who asks
    public boolean messageOnlyFriend = false;
    public boolean duelOnlyFriend = false;
    public boolean duelsNotPublic = false;
    public int licences = 0; //
    public double averagePerformanceIMP = 0;
    public double averagePerformanceMP = 0;
    public int averagePerformanceRank = 0;
    public int averagePerformanceNbPlayers;
    public String teamName = null;
    public String teamID = null;
    public String teamDivision = null;
    public int teamRank = 0;
    public int teamNbTeams = 0;
    public int funbridgePoints = 0;
    public int funbridgePointsRank = 0;
    public int funbridgePointsNbPlayers = 0;

    public WSPlayer() {
    }

    public WSPlayer(long plaID) {
        this.playerID = plaID;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof WSPlayer) {
            return (this.playerID == ((WSPlayer) obj).playerID);
        }
        return super.equals(obj);
    }

    public void setDuelStat(PlayerDuelStat stat) {
        if (stat != null) {
            nbDuelPlayed = stat.nbPlayed;
            nbDuelWin = stat.nbWin;
            nbDuelLost = stat.nbLost;
            if (nbDuelPlayed > (nbDuelWin + nbDuelLost)) {
                nbDuelDraw = nbDuelPlayed - (nbDuelWin + nbDuelLost);
            }
        }
    }
}
