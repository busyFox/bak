package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.ws.player.WSPlayerLight;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 20/01/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSPrivateTournament {
    public String tournamentID = null;
    public WSPrivateTournamentProperties properties;
    public WSPlayerLight owner;
    public int nbPlayers;
    public int nbFriends;
    public long startDate;
    public long endDate;
    public boolean favorite;
    public List<String> listPlayedDeals = new ArrayList<>();
    public boolean playerStarted = false;
    public int nbUnreadMessages;
    public boolean accessGranted = false;
}
