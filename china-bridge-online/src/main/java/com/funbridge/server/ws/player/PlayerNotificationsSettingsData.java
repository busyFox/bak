package com.funbridge.server.ws.player;

/**
 * Created by bplays on 02/06/15.
 */
public class PlayerNotificationsSettingsData {
    public boolean newsletter;
    public boolean endOfPeriodReport;
    public boolean commercialEmails;
    public boolean pushFunBridgeNotifs;
    public boolean pushFriendRequests;
    public String pushMessages;
    public String pushDuelRequests;
    //    public boolean pushTeams;
    public boolean notifyResultsToFriends;
    public boolean receiveDuelsFromFriendsOnly;
    public boolean receiveChatFromFriendsOnly;
    //    public boolean pushFavoritePrivateTournaments;
//    public boolean pushTournamentPlayerNotFinished;
    public boolean pushTournament = true;
}
