package com.funbridge.server.tournament.federation.cbo.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.tournament.federation.memory.TourFederationMemTournamentPlayer;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;

/**
 * Created by ldelbarre on 21/12/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourCBOMemTournamentPlayer extends TourFederationMemTournamentPlayer<TourCBOMemTournament> {

    public TourCBOMemTournamentPlayer(){ super(); }

    public TourCBOMemTournamentPlayer(GenericMemTournament t) {
        super(t);
    }

}
