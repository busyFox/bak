package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 25/08/2016.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSTournamentFederation {
    public String tourID;
    public long dateStart;
    public long dateEnd;
    public int nbPlayersRegistered = 0;
    public int nbFriendsRegistered = 0;
    public boolean isPlayerRegistered = false;
    public int status = 0; // 0 : no register, 1 : register enable, 2 : in progress, 3 : played, 4 : annulé
    public List<String> listPlayedDeals = new ArrayList<>();
    public int resultType;
    public boolean free = false;
    public boolean endowed = false;
    public boolean special = false;
    public String subName;
    public int nbDeals = 0;
}
