package com.funbridge.server.ws.support;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournament;
import com.funbridge.server.tournament.privatetournament.data.PrivateTournamentProperties;

/**
 * Created by pserent on 13/03/2017.
 */
public class WSSupPrivateTournament {
    public String tourID;
    public String propertiesID;
    public String name;
    public long ownerID;
    public boolean finished;
    public String dateStart;
    public String dateEnd;
    public int nbPlayers;
    public int nbPlayersFavorite;
    public int nbDeals;
    public int resultType;
    public String countryCode;
    public String recurrence;
    public String accessRule;
    public String password;
    public String description;
    public String chatroomID;

    public WSSupPrivateTournament(){}

    public WSSupPrivateTournament(PrivateTournament t, PrivateTournamentProperties p) {
        this.tourID = t.getIDStr();
        this.name = t.getName();
        this.propertiesID = p.getIDStr();
        this.ownerID = t.getOwnerID();
        this.finished = t.isFinished();
        this.dateStart = Constantes.timestamp2StringDateHour(t.getStartDate());
        this.dateEnd = Constantes.timestamp2StringDateHour(t.getEndDate());
        this.nbPlayers = t.getNbPlayers();
        this.nbPlayersFavorite = p.getPlayersFavorite().size();
        this.nbDeals = t.getNbDeals();
        this.resultType = t.getResultType();
        this.countryCode = t.getCountryCode();
        this.recurrence = p.getRecurrence();
        this.accessRule = p.getAccessRule();
        this.password = p.getPassword();
        this.description = p.getDescription();
        this.chatroomID = t.getChatroomID();
    }
}
