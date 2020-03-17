package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

/**
 * Created by pserent on 26/08/2016.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSFederationSummary {
    public String licence;
    public int credit;
    public String firstname = null;
    public String lastname = null;
    public String club = null;
    public List<WSTournamentFederation> tournaments;
    public String federationURL;
    public WSTournament lastTournament;
    public String toString() {
        return "licence="+licence+" - credit="+credit+" - nextTournaments="+(tournaments!=null?tournaments.size():null);
    }
}
