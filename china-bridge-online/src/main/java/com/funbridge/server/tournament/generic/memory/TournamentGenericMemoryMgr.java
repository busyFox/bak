package com.funbridge.server.tournament.generic.memory;

import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.tools.JSONTools;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pserent on 27/01/2017.
 * ?????
 */
public class TournamentGenericMemoryMgr<TMemTour extends GenericMemTournament,
        TMemTourPlayer extends GenericMemTournamentPlayer,
        TMemDeal extends GenericMemDeal,
        TMemDealPlayer extends GenericMemDealPlayer> {
    protected Logger log = LogManager.getLogger(this.getClass());
    protected TournamentGenericMgr tournamentGenericMgr;
    protected JSONTools jsonTools = new JSONTools();
    // Map TOUR_ID <==> MEM_TOUR
    protected ConcurrentHashMap<String, TMemTour> mapMemTour = new ConcurrentHashMap<>();
    protected LockWeakString lockTour = new LockWeakString();
    protected Class<TMemTour> classEntityMemTournament;
    protected Class<TMemTourPlayer> classEntityMemTournamentPlayer;
    protected Class<TMemDeal> classEntityMemDeal;
    protected Class<TMemDealPlayer> classEntityMemDealPlayer;

    public TournamentGenericMemoryMgr(){}

    public TournamentGenericMemoryMgr(TournamentGenericMgr mgr,
                                      Class<TMemTour> classMemTournament,
                                      Class<TMemTourPlayer> classMemTournamentPlayer,
                                      Class<TMemDeal> classMemDeal,
                                      Class<TMemDealPlayer> classMemDealPlayer) {
        tournamentGenericMgr = mgr;
        classEntityMemTournament = classMemTournament;
        classEntityMemTournamentPlayer = classMemTournamentPlayer;
        classEntityMemDeal = classMemDeal;
        classEntityMemDealPlayer = classMemDealPlayer;
    }

    public Class<TMemTour> getClassEntityMemTournament() {
        return classEntityMemTournament;
    }

    public Class<TMemTourPlayer> getClassEntityMemTournamentPlayer() {
        return classEntityMemTournamentPlayer;
    }

    public Class<TMemDeal> getClassEntityMemDeal() {
        return classEntityMemDeal;
    }

    public TMemDeal createMemDeal(TMemTour memTour, int dealIndex) {
        try {
            TMemDeal d = classEntityMemDeal.newInstance();
            d.initData(memTour, dealIndex);
            return d;
        } catch (Exception e) {
            log.error("Failed to create instance of MemDeal - memTour="+memTour+" - dealIndex="+dealIndex, e);
        }
        return null;
    }

    public TMemTourPlayer createMemTournamentPlayer(TMemTour memTour) {
        try {
            TMemTourPlayer t = classEntityMemTournamentPlayer.newInstance();
            t.initData(memTour);
            return t;
        } catch (Exception e) {
            log.error("Failed to create instance of MemTournamentPlayer - memTour="+memTour, e);
        }
        return null;
    }

    public TMemDealPlayer createMemDealPlayer() {
        try {
            return classEntityMemDealPlayer.newInstance();
        } catch (Exception e) {
            log.error("Failed to create instance of MemDealPlayer", e);
        }
        return null;
    }

    public Class<TMemDealPlayer> getClassEntityMemDealPlayer() {
        return classEntityMemDealPlayer;
    }

    public void destroy() {
        mapMemTour.clear();
    }

    public List<GenericMemTournament> listTournament() {
        return new ArrayList<>(mapMemTour.values());
    }

    protected TMemTour loadMemTournamentFromJSONFile(String filePath) throws IOException {
        return jsonTools.mapDataFromFile(filePath, classEntityMemTournament);
    }


    public int getSize() {
        return mapMemTour.size();
    }

    /**
     * Transform json data to MemTouResult and add it in memory
     * ?json?????MemTouResult?????????
     * @param filePath
     * @return
     */
    public TMemTour loadMemTourFromFile(String filePath) {
        if (filePath != null) {
            try {
                TMemTour memTour = loadMemTournamentFromJSONFile(filePath);
                if (memTour != null) {
                    // check tournament not yet existing in map
                    if (addMemTour(memTour)) {
                        memTour.initAfterLoad();
                        if (tournamentGenericMgr.getConfigIntValue("backupMemoryDeleteAfterSuccessLoad", 1) == 1
                                && !FileUtils.deleteQuietly(new File(filePath))) {
                            log.warn("Failed to delete quietly file=" + filePath);
                        }
                        return memTour;
                    } else {
                        log.error("Tournament already present in memory ... memTour=" + memTour);
                    }
                } else {
                    log.error("Failed to load memTour from filePath=" + filePath);
                }
            } catch (Exception e) {
                log.error("Failed to load memTour from filePath="+filePath, e);
            }
        } else {
            log.error("Param filePath is null");
        }
        return null;
    }

    /**
     * Backup all MemTourResult
     * @return nb MemTourResult backup with success
     */
    public int backupAllTour() {
        int nbBackup = 0;
        for (GenericMemTournament memTour : mapMemTour.values()) {
            if (backupMemTourResultToFile(memTour)) {
                nbBackup++;
            }
        }
        return nbBackup;
    }

    /**
     * Save memTourResult fo file json
     * @param memTour
     * @return
     */
    public boolean backupMemTourResultToFile(GenericMemTournament memTour) {
        if (memTour != null) {
            String pathBackup = tournamentGenericMgr.buildBackupFilePathForTournament(memTour.tourID, true);
            if (pathBackup != null) {
                try {
                    jsonTools.transform2File(pathBackup, memTour, true);
                    log.debug("Backup with success TimezoneMemTournament = "+memTour);
                    return true;
                } catch (Exception e) {
                    log.error("Failed to backup TimezoneMemTournament="+memTour, e);
                }
            } else {
                log.error("No path to backup TimezoneMemTournament="+memTour);
            }
        } else {
            log.error("Param memTour is null !");
        }
        return false;
    }

    /**
     * Add memTour in memory
     * @param memTour
     * @return true if the tournament is not yet existing is memory
     */
    protected boolean addMemTour(TMemTour memTour) {
        if (memTour != null) {
            memTour.setMemoryMgr(this);
            if (!mapMemTour.containsKey(memTour.tourID)) {
                mapMemTour.put(memTour.tourID, memTour);
                return true;
            }
        }
        return false;
    }

    /**
     * Add Tournament in memory
     * @param tour
     */
    public GenericMemTournament addTournament(Tournament tour) {
        TMemTour memTour = mapMemTour.get(tour.getIDStr());
        if (memTour == null) {
            try {
                memTour = classEntityMemTournament.newInstance();
                memTour.initData(tour, this);
                mapMemTour.put(tour.getIDStr(), memTour);
            } catch (Exception e) {
                log.error("Failed to create new instance of MemTournament for tour="+tour, e);
            }
        }
        return memTour;
    }

    /**
     * Add a player on a tournament
     * @param tour
     * @param playerID
     * @param dateStart
     */
    public void addTournamentPlayer(Tournament tour, long playerID, long dateStart) {
        if (tour != null) {
            synchronized (getLockTour(tour.getIDStr())) {
                GenericMemTournament memTour = addTournament(tour);
                if (memTour != null) {
                    memTour.addPlayer(playerID, dateStart);
                }
            }
        }
    }

    /**
     * Remove the tournament mem object associated to this ID
     * @param tourID
     * @return true if object is present and remove - false if not present
     */
    public boolean removeTournament(String tourID) {
        return mapMemTour.remove(tourID) != null;
    }

    public Object getLockTour(String tourID) {
        return lockTour.getLock(tourID);
    }

    public GenericMemTournament getTournament(String tourID) {
        return mapMemTour.get(tourID);
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
                    GenericMemTournamentPlayer tourPlayer = memTour.addResult(game, true, false);
                    if (tourPlayer == null) {
                        log.error("Failed to add result for player - game="+game);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                    tourPlayer.currentDealIndex = -1;
                }
                else {
                    log.error("No memTour found game="+game);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }

    /**
     * Return the number of player on this tournament. The tournament is not finished.
     * @param tourID
     * @param listPlaFilter
     * @param onlyFinisher
     * @return
     */
    public int getNbPlayerOnTournament(String tourID, List<Long> listPlaFilter, boolean onlyFinisher) {
        int nbPlayer = 0;
        TMemTour mtr = mapMemTour.get(tourID);
        if (mtr != null) {
            if (listPlaFilter != null && !listPlaFilter.isEmpty()) {
                for (Object e : mtr.tourPlayer.values()) {
                    TMemTourPlayer mtp = (TMemTourPlayer)e;
                    if (!listPlaFilter.contains(mtp.playerID)) {
                        continue;
                    }
                    if (onlyFinisher) {
                        if (mtp.isPlayerFinish()) {
                            nbPlayer++;
                        }
                    } else {
                        if (mtp.hasPlayedOneDeal()) {
                            nbPlayer++;
                        }
                    }
                }
            } else {
                if (onlyFinisher) {
                    nbPlayer = mtr.getNbPlayerFinishAll();
                } else {
                    nbPlayer = mtr.getNbPlayer();
                }
            }
        }
        return nbPlayer;
    }

    /**
     * List tournament played by this player
     * @param playerID
     * @param onlyFinish
     * @return
     */
    public List<GenericMemTournamentPlayer> listMemTournamentForPlayer(long playerID, boolean onlyFinish) {
        List<GenericMemTournamentPlayer> listMemTourPlayer = new ArrayList<>();
        for (GenericMemTournament e : mapMemTour.values()) {
            GenericMemTournamentPlayer resPla = e.getTournamentPlayer(playerID);
            if (resPla != null) {
                if (onlyFinish && !resPla.isPlayerFinish()) {
                    continue;
                }
                listMemTourPlayer.add(resPla);
            }
        }
        return listMemTourPlayer;
    }

    /**
     * List tournament played by this player. Last date play must be > dateRef. List is ordered by date last play DESC
     * @param playerID
     * @param onlyFinish
     * @param resultType if O all tournament are used
     * @param dateRef
     * @return
     */
    public List<GenericMemTournamentPlayer> listMemTournamentForPlayer(long playerID, boolean onlyFinish, int resultType, long dateRef) {
        List<GenericMemTournamentPlayer> listMemTourPlayer = new ArrayList<>();
        for (GenericMemTournament e : mapMemTour.values()) {
            GenericMemTournamentPlayer resPla = e.getTournamentPlayer(playerID);
            if (resPla != null && resPla.dateLastPlay > dateRef) {
                if (onlyFinish && !resPla.isPlayerFinish()) {
                    continue;
                }
                if (resultType == 0 || resPla.memTour.resultType == resultType) {
                    listMemTourPlayer.add(resPla);
                }
            }
        }
        // sort on date last play DESC
        listMemTourPlayer.sort(Comparator.comparingLong(GenericMemTournamentPlayer::getDateLastPlay).reversed());

        return listMemTourPlayer;
    }

    /**
     * List tournament played by player
     * @param playerID
     * @return
     */
    public List<GenericMemTournamentPlayer> listMemTournamentWithPlayer(long playerID, int offset, int nbMax) {
        List<GenericMemTournamentPlayer> listMemTourPlayer = new ArrayList<>();
        for (GenericMemTournament e : mapMemTour.values()) {
            GenericMemTournamentPlayer resPla = e.getTournamentPlayer(playerID);
            if (resPla != null) {
                listMemTourPlayer.add(resPla);
            }
        }
        // sort on date last play DESC
        Collections.sort(listMemTourPlayer, new Comparator<GenericMemTournamentPlayer>() {
            @Override
            public int compare(GenericMemTournamentPlayer o1, GenericMemTournamentPlayer o2) {
                if (o1.dateLastPlay > o2.dateLastPlay) {
                    return -1;
                } else if (o1.dateLastPlay < o2.dateLastPlay) {
                    return 1;
                }
                return 0;
            }
        });
        if (offset > listMemTourPlayer.size()) {
            listMemTourPlayer.clear();
        } else {
            if (offset > 0) {
                listMemTourPlayer = listMemTourPlayer.subList(offset, listMemTourPlayer.size()>nbMax?nbMax:listMemTourPlayer.size());
            }
        }
        return listMemTourPlayer;
    }

    public void deleteDataForPlayerList(List<Long> listPlaID) {
        for (GenericMemTournament memTour : mapMemTour.values()) {
            synchronized (tournamentGenericMgr.getLockOnTournament(memTour.tourID)) {
                memTour.removeResultForPlayer(listPlaID);
            }
        }
    }

    /**
     * List tournament in progress for player (tournament date valid and nb deals played < total deals
     * @param playerID
     * @return
     */
    public List<GenericMemTournament> listMemTournamentInProgressForPlayer(long playerID) {
        List<GenericMemTournament> listMemTour = new ArrayList<>();
        for (GenericMemTournament memTour : mapMemTour.values()) {
            if (memTour.endDate > System.currentTimeMillis()) {
                GenericMemTournamentPlayer e = memTour.getTournamentPlayer(playerID);
                if (e!=null && e.getNbPlayedDeals() < memTour.getNbDeal()) {
                    listMemTour.add(memTour);
                }
            }
        }
        return listMemTour;
    }

    /**
     * Find in memory tournaments to play for player. This player has not yet started a deal on this tournament. Actually use for TRAINING
     * @param playerID
     * @param resultType
     * @param nbMax
     * @return a list of TrainingMemTournament
     */
    public List<GenericMemTournament> findMemTournamentsToPlay(long playerID, int resultType, int nbMax, int nbPlayersMin){
        ArrayList<GenericMemTournament> memTournaments = new ArrayList<>(mapMemTour.values());
        List<GenericMemTournament> memTournamentsToPlay = new ArrayList<>();
        if (nbMax > 0) {
            for (GenericMemTournament memTournament : memTournaments) {
                if (memTournament.resultType == resultType && memTournament.isOpen() && !memTournament.isFull() && memTournament.getNbPlayer() >= nbPlayersMin
                        && memTournament.getTournamentPlayer(playerID) == null) {
                    // player has not played this tournament
                    memTournamentsToPlay.add(memTournament);
                    if (memTournamentsToPlay.size() >= nbMax) {
                        break;
                    }
                }
            }
        }
        return memTournamentsToPlay;
    }
}
