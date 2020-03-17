package com.funbridge.server.tournament.federation.cbo.memory;

import com.funbridge.server.tournament.federation.memory.TourFederationMemoryMgr;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;

/**
 * Created by ldelbarre on 21/12/2017.
 */
public class TourCBOMemoryMgr extends TourFederationMemoryMgr<TourCBOMemTournament, TourCBOMemTournamentPlayer, TourCBOMemDeal, TourCBOMemDealPlayer> {

    public TourCBOMemoryMgr(TournamentGenericMgr mgr, Class<TourCBOMemTournament> classMemTournament, Class<TourCBOMemTournamentPlayer> classMemTournamentPlayer, Class<TourCBOMemDeal> classMemDeal, Class<TourCBOMemDealPlayer> classMemDealPlayer) {
        super(mgr, classMemTournament, classMemTournamentPlayer, classMemDeal, classMemDealPlayer);
    }
}