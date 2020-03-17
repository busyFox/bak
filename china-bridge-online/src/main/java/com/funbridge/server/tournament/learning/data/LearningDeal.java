package com.funbridge.server.tournament.learning.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.game.Deal;
import com.funbridge.server.tournament.learning.TourLearningMgr;

/**
 * Created by ldelbarre on 31/05/2018.
 */
public class LearningDeal extends Deal {
    public String bids;
    public String begins;
    public String learningCommentsDeal;
    public String getDealID(String tourID) {
        return TourLearningMgr.buildDealID(tourID, index);
    }

    public String toString() {
        return "commentsDealAuthorID="+ learningCommentsDeal +" - "+super.toString();
    }

    /**
     * Remove extra data like alerts from bids sequence
     * @param bidSequence
     * @return
     */
    private static String removeExtraFromBidSequence(String bidSequence) {
        if (bidSequence != null) {
            StringBuffer sb = new StringBuffer();
            String[] temp = bidSequence.split(Constantes.GAME_BIDCARD_SEPARATOR);
            for (String e : temp) {
                if (sb.length() > 0) {
                    sb.append(Constantes.GAME_BIDCARD_SEPARATOR);
                }
                if (e.length() > 3) {
                    sb.append(e, 0, 3);
                } else {
                    sb.append(e);
                }
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    @Override
    public String getNextBid(String currentBidSequence) {
        String currentBidSequenceFixed = removeExtraFromBidSequence(currentBidSequence);
        if (bids != null && bids.length() > 0) {
            if (currentBidSequenceFixed == null || currentBidSequenceFixed.length() == 0) {
                String temp = bids.split(Constantes.GAME_BIDCARD_SEPARATOR)[0];
                if (temp.length() == 3) {
                    return temp.substring(0,2);
                }
            }
            // currentBidSequence is the same as bids (same beginning)
            if (bids.startsWith(currentBidSequenceFixed)) {
                String temp = bids.substring(currentBidSequenceFixed.length());
                if (temp.startsWith(Constantes.GAME_BIDCARD_SEPARATOR)) {
                    temp = temp.substring(1);
                }
                if (temp.length() > 0) {
                    temp = temp.split(Constantes.GAME_BIDCARD_SEPARATOR)[0];
                    if (temp.length() == 3) {
                        return temp.substring(0,2);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getNextCard(String currentBidSequence, String currentCardSequence) {
        String currentBidSequenceFixed = removeExtraFromBidSequence(currentBidSequence);
        // begins size = 3 & current sequence empty
        if (begins != null && begins.length() == 3 && (currentCardSequence == null || currentCardSequence.length() == 0)) {
            // check bids sequence same as currentBidSequence
            if (currentBidSequenceFixed != null && bids != null && currentBidSequenceFixed.equals(bids)) {
                return begins.substring(0, 2);
            }
        }
        return null;
    }
}
