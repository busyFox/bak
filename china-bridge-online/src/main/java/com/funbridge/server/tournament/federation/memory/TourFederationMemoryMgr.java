package com.funbridge.server.tournament.federation.memory;

import com.funbridge.server.tournament.federation.data.TourFederationTournament;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.generic.memory.TournamentGenericMemoryMgr;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by bplays on 10/01/17.
 */
public abstract class TourFederationMemoryMgr<TMemTour extends TourFederationMemTournament,
        TMemTourPlayer extends TourFederationMemTournamentPlayer,
        TMemDeal extends TourFederationMemDeal,
        TMemDealPlayer extends TourFederationMemDealPlayer> extends TournamentGenericMemoryMgr {

    public TourFederationMemoryMgr(TournamentGenericMgr mgr,
                            Class<TMemTour> classMemTournament,
                            Class<TMemTourPlayer> classMemTournamentPlayer,
                            Class<TMemDeal> classMemDeal,
                            Class<TMemDealPlayer> classMemDealPlayer) {
        super(mgr, classMemTournament, classMemTournamentPlayer, classMemDeal, classMemDealPlayer);
    }

    /**
     * Add Tournament in memory
     * @param tour
     */
    public TMemTour addTournament(Tournament tour) {
        super.addTournament(tour);
        TMemTour memTour = (TMemTour)mapMemTour.get(tour.getIDStr());
        if(memTour != null){
            memTour.addRegisteredPlayers(new ArrayList<>(((TourFederationTournament)tour).getMapRegistration().keySet()));
        }
        return memTour;
    }

    /**
     * Update the tournament in memory with this game.
     * @param game
     * @throws FBWSException
     */
    public void updateResult(Game game) throws FBWSException {
        if (game != null) {
            synchronized (tournamentGenericMgr.getLockOnTournament(game.getTournament().getIDStr())) {
                GenericMemTournament memTour = getTournament(game.getTournament().getIDStr());
                if (memTour != null) {
                    TMemTourPlayer tourPlayer = (TMemTourPlayer) memTour.addResult(game, true, false);
                    if (tourPlayer == null) {
                        log.error("Failed to add result for player - game="+game);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                    tourPlayer.currentDealIndex = -1;
                    tourPlayer.currentDealID = null;
                }
                else {
                    log.error("No memTour found game="+game);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }

    /**
     * List tournament in progress for player (tournament date valid and nb deals played < total )
     * @param playerID
     * @return
     */
    public List<TMemTour> listMemTournamentInProgressForPlayer(long playerID) {
        List<TMemTour> listMemTour = new ArrayList<>();
        for (TMemTour memTour : (Collection<TMemTour>) mapMemTour.values()) {
            if (memTour.endDate > System.currentTimeMillis()) {
                TourFederationMemTournamentPlayer e = (TourFederationMemTournamentPlayer) memTour.getTournamentPlayer(playerID);
                if (e!=null && memTour.isEnableToPlay() && e.getNbPlayedDeals() < memTour.nbDealsToPlay) {
                    listMemTour.add(memTour);
                }
            }
        }
        return listMemTour;
    }
}
