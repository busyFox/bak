package com.funbridge.server.tournament.duel.data;

import com.funbridge.server.tournament.duel.DuelMgr;
import com.funbridge.server.tournament.game.Deal;

/**
 * Created by pserent on 07/07/2015.
 */
public class DuelDeal extends Deal {
    public String getDealID(String tourID) {
        return DuelMgr.buildDealID(tourID, index);
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
