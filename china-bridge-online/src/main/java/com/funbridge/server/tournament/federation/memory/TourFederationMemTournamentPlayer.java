package com.funbridge.server.tournament.federation.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;

/**
 * Created by ldelbarre on 02/08/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourFederationMemTournamentPlayer<TFederationMemTournament extends TourFederationMemTournament> extends GenericMemTournamentPlayer{

    public String currentDealID = null;

    public TourFederationMemTournamentPlayer(){ super(); }

    public TourFederationMemTournamentPlayer(GenericMemTournament t) {
        super(t);
    }

    /**
     * Return the result on tournament for player.
     * @param useRankFinished
     * @return
     */
    public WSResultTournamentPlayer toWSResultTournamentPlayer(boolean useRankFinished) {
        WSResultTournamentPlayer resultPlayer = super.toWSResultTournamentPlayer(useRankFinished);
        if (memTour.endDate < System.currentTimeMillis()) {
            resultPlayer.setMasterPoints(-2);
            resultPlayer.setFbPoints(-2);
        }
        return resultPlayer;
    }

    @Override
    public boolean isPlayerFinish() {
        return playedDeals.size() == ((TFederationMemTournament)memTour).nbDealsToPlay;
    }
}
