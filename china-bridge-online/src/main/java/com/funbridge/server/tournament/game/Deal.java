package com.funbridge.server.tournament.game;

/**
 * Created by pserent on 23/04/2015.
 */
public abstract class Deal {
    public int index;
    public char dealer;
    public char vulnerability;
    public String cards;
    public String paramGenerator = null;
    public String engineParInfo = null;

    public abstract String getDealID(String tourID);

    public String toString() {
        return "index="+index+" - dealer="+getStrDealer()+" - vulnerability="+getStrVulnerability()+" - cards="+cards+" - paramGenerator="+paramGenerator;
    }

    public String getStrDealer() {
        return Character.toString(dealer);
    }

    public String getStrVulnerability() {
        return Character.toString(vulnerability);
    }

    public String getString() {
        return getStrDealer()+getStrVulnerability()+cards;
    }

    public int getIndex() {
        return index;
    }

    public char getDealer() {
        return dealer;
    }

    public char getVulnerability() {
        return vulnerability;
    }

    public String getCards() {
        return cards;
    }

    public String getEngineParInfo() {
        return engineParInfo;
    }

    public void setEngineParInfo(String value) {
        engineParInfo = value;
    }

    public abstract String getNextBid(String currentBidSequence);

    public abstract String getNextCard(String currentBidSequence, String currentCardSequence);
}
