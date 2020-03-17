package com.funbridge.server.tournament.serie;

import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.game.GameMgr;
import com.funbridge.server.tournament.generic.memory.TournamentGenericMemoryMgr;
import com.funbridge.server.tournament.serie.data.*;
import com.funbridge.server.tournament.serie.memory.*;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.gotogames.common.bridge.BridgeConstantes;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pserent on 17/06/2014.
 * Object to manage tournaments in memory
 */
public class TourSerieMemoryMgr extends TournamentGenericMemoryMgr {
    protected Logger log = LogManager.getLogger(this.getClass());
    // Map SERIE -> TOUR_ID <==> MemTour
    private Map<String, ConcurrentHashMap<String, TourSerieMemTour>> mapSerieTourMem = new HashMap<>();
    private TourSerieMgr serieMgr = null;

    public TourSerieMemoryMgr(TourSerieMgr mgr) {
        this.serieMgr = mgr;
        // init map with all memtour beans
        for (String e : TourSerieMgr.SERIE_TAB) {
            mapSerieTourMem.put(e, new ConcurrentHashMap<>());
        }
    }

    @Override
    public void destroy() {
        if (serieMgr.getConfigBooleanValue("backupMemoryEnable")) {
            backupAllMemTour();
        }
        mapSerieTourMem.clear();
    }

    public void clearData() {
        for (Map<String, TourSerieMemTour> e : mapSerieTourMem.values()) {
            e.clear();
        }
    }

    @Override
    public Object getLockTour(String tourID) {
        return lockTour.getLock(tourID);
    }

    /**
     * Load tournament not finished for each serie
     * @param periodID
     */
    public void loadTournamentForPeriod(String periodID) {
        // clear all data before
        clearData();

        // loop on each serie to get tournament not finish on this period
        for (String serie : TourSerieMgr.SERIE_TAB) {
            TourSerieMemPeriodRanking serieRanking = serieMgr.getSerieRanking(serie);
            List<TourSerieTournament> listTour = serieMgr.listTournamentNotFinishedForPeriodAndSerie(periodID, serie);
            log.warn("Serie="+serie+" - period="+periodID+" - Nb tour to load="+listTour.size());
            int nbLoadFromFile = 0, nbLoadFromDB = 0;

            // retrieve list game of each tour
            for (TourSerieTournament t : listTour) {
                log.warn("Load tour="+t+" - nbLoadFromFile="+nbLoadFromFile+" - nbLoadFromDB="+nbLoadFromDB);
                boolean loadFromDB = false;
                if (serieMgr.getConfigBooleanValue("loadFromBackupEnable")) {
                    if (loadMemTourFromFile(buildBackupFilePathForTournament(serie, t.getIDStr(), false)) != null) {
                        nbLoadFromFile++;
                    } else {
                        loadFromDB = true;
                    }
                } else {
                    loadFromDB = true;
                }
                if (loadFromDB) {
                    TourSerieMemTour memTour = addTournament(t);
                    if (memTour != null) {
                        long ts = System.currentTimeMillis();
                        if (serieMgr.getConfigIntValue("loadDataOptimize", 1) == 1) {
                            List<TourSerieGameLoadData> listGame = serieMgr.listGameDataOnTournament(t.getIDStr());
                            log.warn("Tour=" + t + " - Nb game to load=" + listGame.size()+" - time to load game="+(System.currentTimeMillis() - ts));
                            for (TourSerieGameLoadData g : listGame) {
                                memTour.loadResult(g);
                            }
                            // compute result for deal
                            for (TourSerieMemDeal memDeal : memTour.deals) {
                                memDeal.computeResult();
                            }
                            memTour.computeResult();
                            memTour.computeRanking(true);
                            long ts1 = System.currentTimeMillis();
                            for (TourSerieMemTourPlayer tourPlayer : memTour.ranking.values()) {
                                if (tourPlayer.getNbDealsPlayed() == memTour.getNbDeals()) {
                                    if (serieRanking != null) {
                                        TourSerieMemPeriodRankingPlayer rankPlayer = serieRanking.getOrCreatePlayerRank(tourPlayer.playerID);
                                        rankPlayer.setTourResult(memTour.tourID, tourPlayer.result, tourPlayer.rankingFinished, tourPlayer.dateLastPlay, false);
                                    }
                                }
                            }
                            log.warn("Tour=" + t + " - time to setTourResult for players="+(System.currentTimeMillis() - ts1));

                        }
                        else {
                            List<TourSerieGame> listGame = serieMgr.listGameOnTournament(t.getIDStr());
                            log.warn("Tour=" + t + " - Nb game to load=" + listGame.size()+" - time to load game="+(System.currentTimeMillis() - ts));
                            for (TourSerieGame g : listGame) {
                                if (g.isFinished()) {
                                    try {
                                        updateResult(g, null, true);
                                    } catch (FBWSException e) {
                                        log.error("Exception to load game=" + g, e);
                                    }
                                } else {
                                    setTournamentPlayerCurrentDeal(t.getSerie(), t.getIDStr(), g.getPlayerID(), g.getDealIndex());
                                }
                            }
                        }
                        log.warn("Tour="+t+" - time to load data in memory="+(System.currentTimeMillis() - ts));

                        nbLoadFromDB++;
                    } else {
                        log.error("No memTour for tournament="+t);
                    }
                }
            }

            if (serieMgr.getConfigIntValue("loadDataOptimize", 1) == 1) {
                if(serieRanking != null) {
                    // compute result for all player
                    for (TourSerieMemPeriodRankingPlayer e : serieRanking.getAllResult()) {
                        e.computeResult();
                    }
                    // compute serie ranking

                    serieRanking.computeRanking();
                }
                long ts1 = System.currentTimeMillis();
                log.warn("Time to compute ranking serie="+(System.currentTimeMillis() - ts1));
            }
            log.warn("Serie="+serie+" - period="+periodID+" - nbLoadFromFile="+nbLoadFromFile+" - nbLoadFromDB="+nbLoadFromDB);
        }
    }

    /**
     *
     * @param tour
     * @param playerID
     * @throws FBWSException
     */
    public void resetCurrentDealForPlayerOnTournament(final TourSerieTournament tour, final long playerID) {
        final String tournamentID = tour.getIDStr();
        final List<TourSerieGame> listGameForPlayer = serieMgr.listGameOnTournamentForPlayer(tournamentID, playerID);
        if(!listGameForPlayer.isEmpty()){
            final TourSerieGame lastGame = listGameForPlayer.get(listGameForPlayer.size()-1);
            this.setTournamentPlayerCurrentDeal(tour.getSerie(), tournamentID, playerID, lastGame.getDealIndex());
        }
    }

    /**
     * Return the total number tournament in memory
     * @return
     */
    public int getTotalNbTour() {
        int nbTour = 0;
        for (Map<String, TourSerieMemTour> e : mapSerieTourMem.values()) {
            nbTour += e.size();
        }
        return nbTour;
    }

    /**
     * return the number of tournament in memory for a serie
     * @param serie
     * @return
     */
    public int getNbTourInSerie(String serie) {
        Map mapTourMem = mapSerieTourMem.get(serie);
        if (mapTourMem != null) {
            return mapTourMem.size();
        }
        return 0;
    }

    public Map<String, TourSerieMemTour> getMapTourForSerie(String serie) {
        return mapSerieTourMem.get(serie);
    }

    /**
     * Add Tournament in memory
     * @param tour
     */
    public TourSerieMemTour addTournament(TourSerieTournament tour) {
        Map<String, TourSerieMemTour> mapMemTourSerie = getMapTourForSerie(tour.getSerie());
        if (mapMemTourSerie != null) {
            TourSerieMemTour memTour = mapMemTourSerie.get(tour.getIDStr());
            if (memTour == null) {
                memTour = new TourSerieMemTour(tour);
                mapMemTourSerie.put(tour.getIDStr(), memTour);
            }
            return memTour;
        }
        return null;
    }

    /**
     * Add memTour in memory
     * @param memTour
     * @return true if the tournament is not yet existing is memory
     */
    private boolean addMemTour(TourSerieMemTour memTour) {
        if (memTour != null) {
            Map<String, TourSerieMemTour> mapMemTourSerie = getMapTourForSerie(memTour.serie);
            if (mapMemTourSerie != null && !mapMemTourSerie.containsKey(memTour.tourID)) {
                mapMemTourSerie.put(memTour.tourID, memTour);
                return true;
            }
        }
        return false;
    }

    /**
     * Return a tournament in progress for player :
     * @param serie
     * @param playerID
     * @return ID of tournament in progress for player or -1 if not found.
     */
    public TourSerieMemTour getTournamentInProgressForPlayer(String serie, long playerID) {
        ConcurrentHashMap<String, TourSerieMemTour> mapTourMem = mapSerieTourMem.get(serie);
        if (mapTourMem != null) {
            for (TourSerieMemTour mtr : mapTourMem.values()) {
                synchronized (getLockTour(mtr.tourID)) {
                    TourSerieMemTourPlayer rankPla = mtr.getRankingPlayer(playerID);
                    // player begin to play a tournament and not finish it.
                    if (rankPla != null && rankPla.getNbDealsPlayed() < mtr.getNbDeals()) {
                        return mtr;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return the memTour associated to this tourID in the serie
     * @param serie
     * @param tourID
     * @return
     */
    public TourSerieMemTour getMemTour(String serie, String tourID) {
        ConcurrentHashMap<String, TourSerieMemTour> mapTourMem = mapSerieTourMem.get(serie);
        if (mapTourMem != null) {
            return mapTourMem.get(tourID);
        }
        return null;
    }

    /**
     * Return a tournament not yet played by this player. It is the tournament with the least player.
     * @param serie
     * @param playerID
     * @param listTourToIgnore tournament to ignore
     * @return
     */
    public TourSerieMemTour getTournamentNotPlayedForPlayer(String serie, long playerID, List<String> listTourToIgnore) {
        ConcurrentHashMap<String, TourSerieMemTour> mapTourMem = mapSerieTourMem.get(serie);
        if (mapTourMem != null) {
            String tourIDSel = "";
            int nbPlayerSel = 0;
            for (TourSerieMemTour mtr : mapTourMem.values()) {
                if (listTourToIgnore != null && !listTourToIgnore.isEmpty() && listTourToIgnore.contains(mtr.tourID)) {
                    continue;
                }
                if (mtr.getRankingPlayer(playerID) == null) {
                    // first case
                    if (tourIDSel.length() == 0) {
                        nbPlayerSel = mtr.getNbPlayers();
                        tourIDSel = mtr.tourID;
                    }
                    // compare with the previous value read
                    else {
                        if (mtr.getNbPlayers() < nbPlayerSel) {
                            tourIDSel = mtr.tourID;
                            nbPlayerSel = mtr.getNbPlayers();
                        }
                    }
                    // Nb player = 0 on tour sel => stop loop on other tournament (no best result ban be found !)
                    if (tourIDSel.length() > 0 && nbPlayerSel == 0) {
                        break;
                    }
                }
            }

            if (tourIDSel.length() > 0) {
                return mapTourMem.get(tourIDSel);
            }
        }
        return null;
    }

    public void setTournamentPlayerCurrentDeal(String serie, String tourID, long playerID, int dealIndex) {
        synchronized (getLockTour(tourID)) {
            TourSerieMemTour memTour = getMemTour(serie, tourID);
            TourSerieMemTourPlayer mtp = memTour.getOrCreateRankingPlayer(playerID);
            mtp.currentDealIndex = dealIndex;
        }
    }

    public boolean removeResult(String tourID, int dealIndex, long playerID) {
        TourSerieTournament tour = serieMgr.getTournament(tourID);
        if (tour != null) {
            synchronized (getLockTour(tourID)) {
                TourSerieMemTour memTour = getMemTour(tour.getSerie(), tour.getIDStr());
                if (memTour != null) {
                    if (memTour.removeResult(dealIndex, playerID)) {
                        TourSerieMemPeriodRanking serieRanking = serieMgr.getSerieRanking(memTour.serie);
                        if (serieRanking != null) {
                            serieRanking.removePlayerTournamentResult(memTour, playerID);
                        }
                        return true;
                    }
                } else {
                    log.error("No memTour found in memory for tour="+tour);
                }
            }
        } else {
            log.error("No tournament found with id="+tourID);
        }
        return false;
    }

    /**
     * Update the tournament in memory with this game.
     * @param game
     * @param playerCache use to check if data for SeriePlayer must be updated : lastPeriodPlayed, nb players in serie ... and update serieLastPeriodPlayed if necessary
     * @param computeResultRanking
     * @throws FBWSException
     */
    public void updateResult(TourSerieGame game, PlayerCache playerCache, boolean computeResultRanking) throws FBWSException {
        if (game != null) {
            synchronized (getLockTour(game.getTournament().getIDStr())) {
                TourSerieMemTour memTour = getMemTour(game.getTournament().getSerie(), game.getTournament().getIDStr());
                if (memTour != null) {
                    TourSerieMemTourPlayer mtrPla = memTour.addResult(game, computeResultRanking);
                    if (mtrPla == null) {
                        log.error("Failed to add result for player - game="+game);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                    mtrPla.currentDealIndex = -1;
                    // all deals played => update period ranking
                    if (mtrPla.getNbDealsPlayed() == game.getTournament().getNbDeals()) {
                        // update period ranking to set player result
                        TourSerieMemPeriodRanking serieRanking = serieMgr.getSerieRanking(memTour.serie);
                        if (serieRanking != null) {
                            serieRanking.addPlayerTournamentResult(memTour, mtrPla);
                        }

                        if (playerCache != null) {
                            boolean updateNbPlayerSerie = false;
                            // check if player is out of the reserve to update nb players in serie
                            if (!playerCache.serie.equals(TourSerieMgr.SERIE_NC) && serieMgr.isPlayerReserve(playerCache.serie, playerCache.serieLastPeriodPlayed, false)) {
                                // player is in reserve but has just finished this tournament => player go back in serie
                                updateNbPlayerSerie = true;
                            }

                            // update lastPeriodPlayed in cache object (if necessary !)
                            playerCache.serieLastPeriodPlayed = serieMgr.getCurrentPeriod().getPeriodID();

                            // update nb tournament played for player and set if necessary the lastPeriodPlayed
                            serieMgr.updateSeriePlayerTournamentPlayed(mtrPla.playerID, 1);

                            if (updateNbPlayerSerie && serieRanking != null) {
                                serieRanking.updateNbPlayerSerie(serieMgr.countPlayerInSerieExcludeReserve(playerCache.serie, true));
                            }
                        }
                    }
                }
                else {
                    log.error("No memTour found for tourID="+game.getTournament().getIDStr()+" - serie="+game.getTournament().getSerie()+" - game="+game);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }

    /**
     * Finish the tournament with this tourID
     * @param serie
     * @param tourID
     * @return
     */
    public boolean finishTournament(String serie, String tourID) {
        return finishTournament(getMemTour(serie, tourID));
    }

    /**
     * Finish the tournament associated to this memory data
     * @param memTour
     */
    public boolean finishTournament(TourSerieMemTour memTour) {
        long tsBegin = System.currentTimeMillis();
        long ts = 0;
        if (memTour != null) {
            TourSerieTournament tour = serieMgr.getTournament(memTour.tourID);
            if (tour != null && !tour.isFinished()) {
                boolean includeTournamentNotFinish = serieMgr.getConfigBooleanValue("includeTournamentNotFinish");
                // list with all player on tournament
                Set<Long> listPlayer = memTour.ranking.keySet();
                // loop on each deals to complete games for each player
                TourSerieMemPeriodRanking serieRanking = serieMgr.getSerieRanking(tour.getSerie());
                try {
                    List<TourSerieGame> listGameToUpdate = new ArrayList<>();
                    List<TourSerieGameUpdateBulk> listGameUpdateBulk = new ArrayList<>();
                    List<TourSerieGame> listGameToInsert = new ArrayList<>();
                    List<TourSerieGame> listGameNotFinished = new ArrayList<>();
                    //******************************************
                    // loop on all deals to update rank & result
                    for (TourSerieMemDeal memDeal : memTour.deals) {
                        listGameToUpdate.clear();
                        listGameToInsert.clear();
                        listGameNotFinished.clear();
                        // add in memory all game not finished (set leave or claim)
                        ts = System.currentTimeMillis();
                        listGameNotFinished = serieMgr.listGameOnTournamentForDealNotFinished(memTour.tourID, memDeal.dealIndex);
                        log.warn("TourID:"+memTour.tourID+" - Time to listGameOnTournamentForDealNotFinished="+(System.currentTimeMillis() - ts)+" - nbGame="+listGameNotFinished.size());
                        if (listGameNotFinished != null) {
                            for (TourSerieGame game : listGameNotFinished) {
                                game.setFinished(true);
                                if (game.getBidContract() != null) {
                                    game.claimForPlayer(BridgeConstantes.POSITION_SOUTH, 0);
                                    GameMgr.computeScore(game);
                                } else {
                                    game.setLeaveValue();
                                    game.setLastDate(tour.getTsDateEnd());
                                }
                                TourSerieMemTourPlayer mtrPla = memTour.addResult(game, false);
                                // all deals played => update period ranking
                                if (includeTournamentNotFinish && mtrPla.getNbDealsPlayed() == tour.getNbDeals()) {
                                    // update period ranking to set player result
                                    if (serieRanking != null) {
                                        serieRanking.addPlayerTournamentResult(memTour, mtrPla);
                                    }
                                }
                            }
                        }
                        // update list game not finished in DB
                        if (!listGameNotFinished.isEmpty()) {
                            ts = System.currentTimeMillis();
                            serieMgr.updateListGameDB(listGameNotFinished);
                            log.warn("TourID:"+memTour.tourID+" - Time to updateListGameDB (GameNotFinished)="+(System.currentTimeMillis() - ts)+" - nbGame="+listGameNotFinished.size());
                        }

                        // insert game not existing for players started the tournament (players not playing some deals)
                        List<Long> listPlaNoResult = memDeal.getListPlayerStartedTournamentWithNoResult();
                        for (long pla : listPlaNoResult) {
                            TourSerieMemTourPlayer mtrPla = memTour.addResultGameNotPlayed(memDeal, pla);
                            // all deals played => update period ranking
                            if (includeTournamentNotFinish && mtrPla.getNbDealsPlayed() == tour.getNbDeals() && serieRanking != null) {
                                // update period ranking to set player result
                                serieRanking.addPlayerTournamentResult(memTour, mtrPla);
                            }
                        }

                        // loop all player to update game in DB
                        listGameToUpdate.clear();
                        listGameUpdateBulk.clear();
                        listGameToInsert.clear();
                        ts = System.currentTimeMillis();
                        for (long pla : listPlayer) {
                            TourSerieMemDealPlayer resPla = memDeal.getResultPlayer(pla);
                            if (resPla != null) {
                                if (serieMgr.getConfigIntValue("useBulk", 1) == 1) {
                                    if (resPla.gameID != null) {
                                        TourSerieGameUpdateBulk guf = new TourSerieGameUpdateBulk();
                                        guf.gameID = resPla.gameID;
                                        guf.rank = resPla.nbPlayerBestScore + 1;
                                        guf.result = resPla.result;
                                        listGameUpdateBulk.add(guf);
                                    } else {
                                        TourSerieGame game = new TourSerieGame(pla, tour, memDeal.dealIndex);
                                        // game is leaved
                                        game.setLeaveValue();
                                        game.setStartDate(tour.getTsDateEnd() - 1);
                                        game.setLastDate(tour.getTsDateEnd());
                                        listGameToInsert.add(game);
                                        game.setRank(resPla.nbPlayerBestScore + 1);
                                        game.setResult(resPla.result);
                                    }
                                } else {
                                    TourSerieGame game = null;
                                    if (resPla.gameID != null) {
                                        game = serieMgr.getGame(resPla.gameID);
                                    } else {
                                        game = serieMgr.getGameOnTournamentAndDealForPlayer(memTour.tourID, memDeal.dealIndex, pla);
                                    }
                                    if (game != null) {
                                        listGameToUpdate.add(game);
                                    } else {
                                        game = new TourSerieGame(pla, tour, memDeal.dealIndex);
                                        // game is leaved
                                        game.setLeaveValue();
                                        game.setStartDate(tour.getTsDateEnd() - 1);
                                        game.setLastDate(tour.getTsDateEnd());
                                        listGameToInsert.add(game);
                                    }
                                    game.setRank(resPla.nbPlayerBestScore + 1);
                                    game.setResult(resPla.result);
                                }
                            } else {
                                log.error("Failed to find result on deal for player=" + pla);
                                // todo add game with leave value
                            }
                        }
                        log.warn("TourID:"+memTour.tourID+" - Time to update games in memory="+(System.currentTimeMillis() - ts)+" - nbPlayer="+listPlayer.size()+" - listGameToInsert="+listGameToInsert.size()+" - listGameUpdateBulk="+listGameUpdateBulk.size()+" - listGameToUpdate="+listGameToUpdate.size());

                        // persist game in DB
                        if (!listGameToUpdate.isEmpty()) {
                            ts = System.currentTimeMillis();
                            serieMgr.updateListGameDB(listGameToUpdate);
                            log.warn("TourID:"+memTour.tourID+" - Time to updateListGameDB="+(System.currentTimeMillis() - ts)+" - nbGame="+listGameToUpdate.size());
                        }
                        if (!listGameUpdateBulk.isEmpty()) {
                            ts = System.currentTimeMillis();
                            int nbGameUpdated = serieMgr.updateListGameBulk(listGameUpdateBulk);
                            log.warn("TourID:"+memTour.tourID+" - Time to updateListGameBulk="+(System.currentTimeMillis() - ts)+" - nbGame="+listGameUpdateBulk.size()+" - nbGameUpdated="+nbGameUpdated);
                        }
                        ts = System.currentTimeMillis();
                        serieMgr.insertListGameDB(listGameToInsert);
                        log.warn("TourID:"+memTour.tourID+" - Time to insertListGameDB="+(System.currentTimeMillis() - ts)+" - nbGame="+listGameToInsert.size());
                    }
                    //******************************************
                    // compute tournament result & ranking
                    ts = System.currentTimeMillis();
                    memTour.computeResult();
                    memTour.computeRanking(false);
                    log.warn("TourID:"+memTour.tourID+" - Time to computeResult&ranking="+(System.currentTimeMillis() - ts));
                    //******************************************
                    // insert tournament result to DB
                    List<TourSerieTournamentPlayer> listTourPlay = new ArrayList<>();
                    for (TourSerieMemTourPlayer mtp : memTour.getListRanking()) {
                        TourSerieTournamentPlayer tp = new TourSerieTournamentPlayer();
                        tp.setPlayerID(mtp.playerID);
                        tp.setTournament(tour);
                        tp.setPeriodID(tour.getPeriod());
                        tp.setRank(mtp.ranking);
                        tp.setResult(mtp.result);
                        tp.setLastDate(mtp.dateLastPlay);
                        tp.setStartDate(mtp.dateStartPlay);
                        tp.setCreationDateISO(new Date());
                        listTourPlay.add(tp);
                    }
                    ts = System.currentTimeMillis();
                    serieMgr.insertListTourPlayDB(listTourPlay);
                    log.warn("TourID:"+memTour.tourID+" - Time to insertListTourPlayDB&ranking="+(System.currentTimeMillis() - ts)+" - nb TourPlay="+listTourPlay.size());

                    //******************************************
                    // set tournament finished & update tournament
                    tour.setFinished(true);
                    tour.setNbPlayers(listPlayer.size());
                    serieMgr.updateTournamentDB(tour);

                    // remove tournament from memory
                    removeTournament(tour.getSerie(), tour.getIDStr());
                    log.debug("Finish tournament="+tour+" - TS="+(System.currentTimeMillis() - tsBegin));
                    return true;
                } catch (Exception e) {
                    log.error("Exception to finish tournament="+tour, e);
                }
            } else {
                log.error("Tournament not found or already finished !");
            }
        } else {
            log.error("Parameter not valid - memTour="+memTour);
        }
        return false;
    }

    /**
     * Remove tournament from map
     * @param serie
     * @param tourID
     */
    private void removeTournament(String serie, String tourID) {
        Map<String, TourSerieMemTour> mapTour = mapSerieTourMem.get(serie);
        if (mapTour != null) {
            mapTour.remove(tourID);
        }
    }

    /**
     * Finish all tournament for a serie. Check tournament period to be the same.
     * @param serie
     * @param periodID
     */
    public int finishAllTournamentForSerie(String serie, String periodID) {
        Map<String, TourSerieMemTour> mapTour = mapSerieTourMem.get(serie);
        int nbTourToFinish = mapTour.size();
        int nbTourFinishOK = 0;
        for (TourSerieMemTour memTour : mapTour.values()) {
            if (memTour.periodID.equals(periodID) && finishTournament(memTour)) {
                nbTourFinishOK++;
            }
            log.warn("Serie="+serie+" - nbTourToFinish="+nbTourToFinish+" - nbTourFinishOK="+nbTourFinishOK);
        }
        if (nbTourFinishOK != nbTourToFinish) {
            log.error("Failed to finish some tournament ... Nb Tour finished OK="+nbTourFinishOK+" - nb tour to finish="+nbTourToFinish);
        }
        return nbTourFinishOK;
    }

    /**
     * Return the file path used to backup a tournament
     * @param serie
     * @param tourID
     * @param mkdir if true, create dir if not existing
     * @return
     */
    public String buildBackupFilePathForTournament(String serie, String tourID, boolean mkdir) {
        String path = serieMgr.getStringResolvEnvVariableValue("backupMemoryPath", null);
        if (path != null) {
            path = FilenameUtils.concat(path, serie);
            if (mkdir) {
                try {
                    File f = new File(path);
                    if (!f.exists()) {
                        f.mkdirs();
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
            path = FilenameUtils.concat(path, tourID + ".json");
        }
        return path;
    }

    /**
     * Save memTour fo file json
     * @param memTour
     * @return
     */
    public boolean backupMemTourToFile(TourSerieMemTour memTour) {
        if (memTour != null) {
            String pathBackup = buildBackupFilePathForTournament(memTour.serie, memTour.tourID, true);
            if (pathBackup != null) {
                try {
                    jsonTools.transform2File(pathBackup, memTour, true);
                    return true;
                } catch (Exception e) {
                    log.error("Failed to backup TourSerieMemTour - memTour="+memTour, e);
                }
            } else {
                log.error("No path to backup memTour="+memTour);
            }
        } else {
            log.error("Param TourSerieMemTour is null !");
        }
        return false;
    }

    public int backupAllMemTour() {
        int nbMemTourBackup = 0;
        long ts = System.currentTimeMillis();
        log.error("Backup AllMemTour - Start");
        for (String serie : TourSerieMgr.SERIE_TAB) {
            int tempNb = 0;
            long tsTemp = System.currentTimeMillis();
            Map<String, TourSerieMemTour> mapTour = mapSerieTourMem.get(serie);
            if (mapTour != null) {
                for (TourSerieMemTour memTour : mapTour.values()) {
                    if (backupMemTourToFile(memTour)) {
                        tempNb++;
                    }
                }
            }
            log.error("Backup mem tour for serie="+serie+" - nbTour="+tempNb+" - ts="+(System.currentTimeMillis() - tsTemp));
        }
        log.error("Backup AllMemTour - End - nbMemTourBackup="+nbMemTourBackup+" - ts="+(System.currentTimeMillis() - ts));
        return nbMemTourBackup;
    }

    /**
     * Transform json data to MemTouResult and add it in memory
     * @param filePath
     * @return
     */
    @Override
    public TourSerieMemTour loadMemTourFromFile(String filePath) {
        if (filePath != null) {
            try {
                File f = new File(filePath);
                if (f.exists()) {
                    TourSerieMemTour memTour = jsonTools.mapDataFromFile(filePath, TourSerieMemTour.class);
                    if (memTour != null) {
                        // check tournament not yet existing in map
                        if (addMemTour(memTour)) {
                            memTour.initAfterLoadJSON();
                            return memTour;
                        } else {
                            log.error("Tournament already present in memory ... memTour=" + memTour);
                        }
                    } else {
                        log.error("TourSerieMemTour is null !! loaded from filePath=" + filePath);
                    }
                } else {
                    log.error("File not existing for filepath="+filePath);
                }
            } catch (Exception e) {
                log.error("Failed to load TourSerieMemTour from filePath="+filePath, e);
            }
        } else {
            log.error("Param filePath is null");
        }
        return null;
    }

    /**
     * Clear data on memTour ranking and reload all game from DB.
     * @param memTour
     */
    public void cleanAndReloadDataForMemTour(TourSerieMemTour memTour) {
        if (memTour != null) {
            synchronized (getLockTour(memTour.tourID)) {
                memTour.ranking.clear();
                memTour.listPlayerFinishAll.clear();
                memTour.listPlayerForRanking.clear();

                TourSerieMemPeriodRanking serieRanking = serieMgr.getSerieRanking(memTour.serie);
                List<TourSerieGame> listGame = serieMgr.listGameOnTournament(memTour.tourID);
                for (TourSerieGame tg : listGame) {
                    if (tg.isFinished()) {
                        TourSerieMemTourPlayer e = memTour.addResult(tg, true);
                        if (e.getNbDealsPlayed() == 4) {
                            serieRanking.addPlayerTournamentResult(memTour, e);
                        }
                    } else {
                        memTour.getOrCreateRankingPlayer(tg.getPlayerID());
                    }
                }
                memTour.computeResult();
                memTour.computeRanking(true);
            }
        }
    }

    /**
     * Reload the deals for the player on the tournament from mongodb to the memory
     * @param playerID the touch id of the player
     * @param memTour the tournament in memory
     * @throws FBWSException if there is an issue during the mongodb query
     */
    public void reloadDealsForPlayerOnTournament(final long playerID, final TourSerieMemTour memTour) throws FBWSException {
        if (memTour != null) {
            final String tournamentID = memTour.tourID;
            synchronized (getLockTour(tournamentID)) {
                final List<TourSerieGame> games = serieMgr.listGameOnTournamentForPlayer(tournamentID, playerID);
                for(final TourSerieGame g : games) {
                    memTour.addResult(g, true);
                }
            }
        }
    }
}
