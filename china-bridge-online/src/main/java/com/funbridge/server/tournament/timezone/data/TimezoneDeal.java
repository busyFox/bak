package com.funbridge.server.tournament.timezone.data;

import com.funbridge.server.tournament.game.Deal;
import com.funbridge.server.tournament.timezone.TimezoneMgr;

/**
 * Created by pserent on 15/04/2015.
 */
public class TimezoneDeal extends Deal {
    public String getDealID(String tourID) {
        return TimezoneMgr.buildDealID(tourID, index);
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
