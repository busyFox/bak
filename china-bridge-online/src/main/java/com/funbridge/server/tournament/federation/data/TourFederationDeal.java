package com.funbridge.server.tournament.federation.data;

import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.game.Deal;

/**
 * Created by ldelbarre on 02/08/2017.
 */
public class TourFederationDeal extends Deal {
    private int nbPlayers = 0;

    public int getNbPlayers() {
        return nbPlayers;
    }

    public void setNbPlayers(int nbPlayers) {
        this.nbPlayers = nbPlayers;
    }

    @Override
    public String getDealID(String tourID) {
        return TourFederationMgr.buildDealID(tourID, index);
    }

    @Override
    public String getNextBid(String currentBidSequence) {
        return null;
    }

    @Override
    public String getNextCard(String currentBidSequence, String currentCardSequence) {
        return null;
    }
}
