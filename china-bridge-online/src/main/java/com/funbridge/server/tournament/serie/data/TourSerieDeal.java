package com.funbridge.server.tournament.serie.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.tournament.game.Deal;
import com.funbridge.server.tournament.serie.TourSerieMgr;
/**
 * Created by pserent on 28/05/2014.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourSerieDeal extends Deal {


    public String getDealID(String tourID) {
        return TourSerieMgr.buildDealID(tourID, index);
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
