package com.funbridge.server.tournament.federation.data;

import org.springframework.data.annotation.Id;

/**
 * Created by bplays on 10/01/17.
 */
public class PlayerFederation {

    @Id
    public long playerId;
    public String licence;
    public int nbBoughtCredits;
    public int nbBoughtCreditsPlayed;
    public int nbBonusCredits;
    public int nbBonusCreditsPlayed;
    public long playerTouchId;
    public double pointsEarned = 0;
    public int tournamentsPlayed = 0;
    public String firstname = null;
    public String lastname = null;
    public String club = null;

    public int getNbCreditsLeft(){
        return this.getNbBonusCreditsLeft() + this.getNbBoughtCreditsLeft();
    }

    public int getNbBonusCreditsLeft(){
        return nbBonusCredits - nbBonusCreditsPlayed;
    }

    public int getNbBoughtCreditsLeft(){
        return nbBoughtCredits - nbBoughtCreditsPlayed;
    }


    public String toString() {
        return "playerId="+playerId+" - playerTouchId="+playerTouchId+" - licence="+licence+" - pointsEarned="+pointsEarned+" - tournamentsPlayed="+tournamentsPlayed;
    }
}
