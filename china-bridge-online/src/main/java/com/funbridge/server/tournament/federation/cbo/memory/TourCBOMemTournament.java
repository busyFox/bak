package com.funbridge.server.tournament.federation.cbo.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.federation.memory.TourFederationMemTournament;
import com.funbridge.server.tournament.federation.cbo.data.TourCBOTournament;

import java.util.List;

/**
 * Created by ldelbarre on 21/12/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourCBOMemTournament extends TourFederationMemTournament<TourCBOMemTournamentPlayer, TourCBOMemDeal, TourCBOMemDealPlayer> {

    public TourCBOMemTournament() {
        super();
    }

    public TourCBOMemTournament(TourCBOTournament tour) {
        super(tour);
        deals = new TourCBOMemDeal[tour.getNbDeals()];
        for (int i = 0; i < deals.length; i++){
            deals[i] = new TourCBOMemDeal(this, i+1);
        }
    }

    /**
     * Insert players to
     * @param listPlayerID
     */
    @Override
    public void addRegisteredPlayers(List<Long> listPlayerID) {
        if (listPlayerID != null && listPlayerID.size() > 0) {
            long curTS = System.currentTimeMillis();
            for (Long e : listPlayerID) {
                if (!tourPlayer.containsKey(e)) {
                    TourCBOMemTournamentPlayer plaRank = new TourCBOMemTournamentPlayer(this);
                    plaRank.playerID = e;
                    plaRank.dateStart = curTS;
                    plaRank.dateLastPlay = curTS;
                    tourPlayer.put(e, plaRank);
                }
            }
        }
    }

    @Override
    public TourFederationMgr getTourFederationMgr() {
        return ContextManager.getTourCBOMgr();
    }
}
