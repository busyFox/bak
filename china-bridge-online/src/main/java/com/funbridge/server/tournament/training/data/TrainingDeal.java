package com.funbridge.server.tournament.training.data;

import com.funbridge.server.tournament.game.Deal;
import com.funbridge.server.tournament.training.TrainingMgr;

/**
 * Created by pserent on 09/04/2015.
 */
public class TrainingDeal extends Deal {

    @Override
    public String getDealID(String tourID) {
        return TrainingMgr.buildDealID(tourID, index);
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
