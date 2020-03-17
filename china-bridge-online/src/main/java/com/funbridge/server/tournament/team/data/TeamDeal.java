package com.funbridge.server.tournament.team.data;

import com.funbridge.server.tournament.game.Deal;
import com.funbridge.server.tournament.team.TourTeamMgr;

/**
 * Created by pserent on 08/11/2016.
 */
public class TeamDeal extends Deal {
    @Override
    public String getDealID(String tourID) {
        return TourTeamMgr.buildDealID(tourID, index);
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
