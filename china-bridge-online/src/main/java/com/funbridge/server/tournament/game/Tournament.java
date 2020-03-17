package com.funbridge.server.tournament.game;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.ws.tournament.WSTournament;
import org.springframework.data.annotation.Transient;

import java.util.List;

/**
 * Created by pserent on 23/04/2015.
 */
public abstract class Tournament {
    protected int nbPlayers = 0;
    protected boolean finished = false;
    protected int nbCreditsPlayDeal = 1;
    @Transient
    protected int category;

    public String toString() {
        return "ID="+getIDStr()+" - category="+getCategory()+"("+Constantes.tourCategory2Name(getCategory())+") - resultType="+getResultType()+"("+Constantes.tournamentResultType2String(getResultType())+") - nbDeals="+getNbDeals()+" - nbPlayers="+nbPlayers+" - finished="+finished;
    }

    public abstract <T extends Deal>List<T> getDeals();

    public int getNbDeals() {
        return getDeals().size();
    }

    public int getNbDealsToPlay() {
        return getNbDeals();
    }

    /**
     * Return the deal at the index
     * @param idx must be between 1 and nbDeal (included)
     * @return
     */
    public Deal getDealAtIndex(int idx) {
        if (getDeals() != null && idx >= 1 && idx <= getDeals().size()) {
            return getDeals().get(idx - 1);
        }
        return null;
    }

    /**
     * Return the deal with this ID
     * @param dealID
     * @return
     */
    public Deal getDeal(String dealID) {
        for (int i = 0; i < getNbDeals(); i++) {
            Deal d = getDeals().get(i);
            if (d.getDealID(getIDStr()).equals(dealID)) {
                return d;
            }
        }
        return null;
    }
    public abstract String getIDStr();
    public abstract int getResultType();
    public int getCategory() {
        return category;
    }
    public void setCategory(int category) {
        this.category = category;
    }

    public int getNbPlayers() {
        return nbPlayers;
    }

    public void setNbPlayers(int value) {
        this.nbPlayers = value;
    }

    public void setFinished(boolean value) {
        this.finished = value;
    }

    public int getNbCreditsPlayDeal() {
        return nbCreditsPlayDeal;
    }

    public void setNbCreditsPlayDeal(int value) {
        this.nbCreditsPlayDeal = value;
    }

    public boolean isFinished() {
        return finished;
    }

    public abstract boolean isDateValid(long dateTS);

    public abstract long getStartDate();

    public abstract long getEndDate();

    public abstract String getName();

    public abstract WSTournament toWS();

    public int getNbPlayersOnDeal(String dealID) {
        return nbPlayers;
    }

}
