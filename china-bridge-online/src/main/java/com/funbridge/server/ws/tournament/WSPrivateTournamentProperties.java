package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr;

/**
 * Created by pserent on 23/01/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSPrivateTournamentProperties {
    public String propertiesID;
    public String name;
    public int nbDeals;
    public int rankingType; // 1 => PAIRE , 2 => IMP
    public int duration; // nb minutes
    public String recurrence; //"once" => une fois, "daily" => quotidien, "weekly" => hebdomadaire
    public long startDate;
    public String accessRule; //"public" or "password"
    public String password;
    public String description;
    public int nbPlayersFavorite = 0;
    public int nbFriendsFavorite = 0;

    public String toString() {
        return "propertiesID="+propertiesID+" - name="+name+" - nbDeals="+nbDeals+" - startDate="+Constantes.timestamp2StringDateHour(startDate)+" - rankingType="+rankingType+" - duration="+duration+" - recurrence="+recurrence+" - accessRole="+accessRule+" - password="+password;
    }

    public boolean isValid() {
        if (propertiesID != null && propertiesID.length() == 0) {
            return false;
        }
        if (name == null || name.length() == 0) {
            return false;
        }
        if (nbDeals < 3 || nbDeals > 40) {
            return false;
        }
        if (rankingType != Constantes.TOURNAMENT_RESULT_IMP && rankingType != Constantes.TOURNAMENT_RESULT_PAIRE) {
            return false;
        }
        if (duration <= 0) {
            return false;
        }
        if (!recurrence.equals(PrivateTournamentMgr.RECURRENCE_DAILY) && !recurrence.equals(PrivateTournamentMgr.RECURRENCE_ONCE) && !recurrence.equals(PrivateTournamentMgr.RECURRENCE_WEEKLY)) {
            return false;
        }
        if (!accessRule.equals(PrivateTournamentMgr.ACCESS_RULE_PASSWORD) && !accessRule.equals(PrivateTournamentMgr.ACCESS_RULE_PUBLIC)) {
            return false;
        }
        if (accessRule.equals(PrivateTournamentMgr.ACCESS_RULE_PASSWORD) && (password == null || password.length() == 0)) {
            return false;
        }
        return startDate != 0;
    }
}
