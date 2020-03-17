package com.funbridge.server.tournament.federation.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.federation.data.TourFederationTournament;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;

import java.util.*;

/**
 * Created by ldelbarre on 02/08/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public abstract class TourFederationMemTournament<TMemTourPlayer extends TourFederationMemTournamentPlayer,
        TMemDeal extends TourFederationMemDeal,
        TMemDealPlayer extends TourFederationMemDealPlayer> extends GenericMemTournament<TMemTourPlayer, TMemDeal, TMemDealPlayer>{

    public int nbDealsToPlay = this.getTourFederationMgr().getNbDealsToPlay();

    public TourFederationMemTournament() {}

    public TourFederationMemTournament(TourFederationTournament tour) {
        this.tourID = tour.getIDStr();
        this.name = tour.getName();
        this.endDate = tour.getEndDate();
        this.nbDealsToPlay = tour.getNbDealsToPlay();
    }

    /**
     * Insert players to
     * @param listPlayerID
     */
    public abstract void addRegisteredPlayers(List<Long> listPlayerID);

    public abstract TourFederationMgr getTourFederationMgr();

    public class NextDealElement {
        public String dealID;
        public int nbPlayers;
        public NextDealElement(String id, int nb) {
            this.dealID = id;
            this.nbPlayers = nb;
        }
    }

    public String getNextDealIDToPlay(long playerID) {
        try {
            TMemTourPlayer memTournamentPlayer = (TMemTourPlayer) getTournamentPlayer(playerID);
            // list all deals
            List<NextDealElement> listDealID = new ArrayList<>();
            for (TMemDeal e : deals) {
                // check player has not yet played this deal
                if (memTournamentPlayer == null || (memTournamentPlayer != null && !memTournamentPlayer.playedDeals.contains(e.dealID))) {
                    listDealID.add(new NextDealElement(e.dealID, e.getNbPlayers()));
                }
            }
            if (listDealID.size() > 0) {
                // shuffle list
                Collections.shuffle(listDealID, new Random(System.nanoTime()));
                // sort it to take the first with small nbPlayers value
                Collections.sort(listDealID, new Comparator<NextDealElement>() {
                    @Override
                    public int compare(NextDealElement o1, NextDealElement o2) {
                        return Integer.compare(o1.nbPlayers, o2.nbPlayers);
                    }
                });
                return listDealID.get(0).dealID;
            }
        } catch (Exception e) {
            getLog().error("Failed to get next dealID to play for playerID="+playerID, e);
        }
        return null;
    }

    public boolean isEnableToPlay() {
        return deals.length > 0;
    }

    @Override
    public int getNbDealsToPlay() {
        return nbDealsToPlay;
    }
}
