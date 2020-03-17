package com.funbridge.server.tournament.privatetournament.data;

import com.funbridge.server.tournament.game.Deal;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr;

/**
 * Created by pserent on 23/01/2017.
 */
public class PrivateDeal extends Deal {

    private String chatroomID;

    public String getDealID(String tourID) {
        return PrivateTournamentMgr.buildDealID(tourID, index);
    }

    @Override
    public String getNextBid(String currentBidSequence) {
        return null;
    }

    @Override
    public String getNextCard(String currentBidSequence, String currentCardSequence) {
        return null;
    }

    public String getChatroomID() {
        return chatroomID;
    }

    public void setChatroomID(String chatroomID) {
        this.chatroomID = chatroomID;
    }
}
