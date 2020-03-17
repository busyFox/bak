package com.funbridge.server.tournament.duel;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.duel.data.DuelGame;
import com.funbridge.server.tournament.duel.data.DuelTournamentPlayer;
import com.funbridge.server.tournament.duel.memory.DuelMemDeal;
import com.funbridge.server.tournament.duel.memory.DuelMemGame;
import com.funbridge.server.tournament.duel.memory.DuelMemTournament;
import com.funbridge.server.tournament.duel.memory.DuelMemTournamentPlayer;
import com.funbridge.server.tournament.game.GameMgr;
import com.funbridge.server.tournament.generic.memory.TournamentGenericMemoryMgr;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.tools.JSONTools;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pserent on 10/07/2015.
 */
public class DuelMemoryMgr extends TournamentGenericMemoryMgr {
    protected Logger log = LogManager.getLogger(this.getClass());
    private JSONTools jsonTools = new JSONTools();

    // Map DuelTournamentPlayer ID <==> DuelMemTournament
    private ConcurrentHashMap<String, DuelMemTournament> mapDuelMemTournament = new ConcurrentHashMap<>();
    // Map Key Players (playerID1-playerID2) <==> DuelTournament ID
    private ConcurrentHashMap<String, String> mapKeyDuelTournament = new ConcurrentHashMap<>();

    // Map TourID => DuelArgineInProgress
//    private ConcurrentHashMap<String, DuelArgineInProgress> mapDuelArgineInProgress = new ConcurrentHashMap<>();
    private DuelMgr duelMgr = null;

    public DuelMemoryMgr(DuelMgr mgr) {
        this.duelMgr = mgr;
    }

    public int countDuelTournament() {
        return mapDuelMemTournament.size();
    }

    public void loadTournamentDuelNotFinish() {
        // load all tournamentDuel not finish
        List<DuelTournamentPlayer> listTournamentPlayer = duelMgr.listTournamentPlayerNotFinish();
        int nbLoadFromFile = 0, nbLoadFromDB = 0;
        log.error("Nb DuelTournamentPlayer to load : "+listTournamentPlayer.size());
        for (DuelTournamentPlayer duelTournamentPlayer : listTournamentPlayer) {
            boolean loadFromDB = false;
//            boolean argineInProgress = false;
            DuelMemTournament duelMemTournament = null;
            if (duelMgr.getConfigIntValue("backupMemoryLoad", 1) == 1) {
                // try to load from json file
                duelMemTournament = loadDuelResultFromFile(duelTournamentPlayer);
                if (duelMemTournament != null) {
                    nbLoadFromFile++;
//                    if (duelMemTournament.isDuelWithArgine() && !duelMemTournament.isAllPlayedForPlayer(Constantes.PLAYER_ARGINE_ID)) {
//                        argineInProgress = true;
//                    }
                } else {
                    loadFromDB = true;
                }
            } else {
                loadFromDB = true;
            }
            if (loadFromDB) {
                duelMemTournament = addTournamentPlayer(duelTournamentPlayer);
                if (duelMemTournament != null) {
                    try {
                        List<DuelGame> listGame1 = duelMgr.listGameFinishedOnTournamentForPlayer(duelTournamentPlayer.getTournament().getIDStr(), duelTournamentPlayer.getPlayer1ID());
                        List<DuelGame> listGame2 = duelMgr.listGameFinishedOnTournamentForPlayer(duelTournamentPlayer.getTournament().getIDStr(), duelTournamentPlayer.getPlayer2ID());
                        for (DuelGame e : listGame1) {
                            duelMemTournament.addGamePlayer(e);
                        }
                        for (DuelGame e : listGame2) {
                            duelMemTournament.addGamePlayer(e);
                        }
                        nbLoadFromDB++;
                    } catch (Exception e) {
                        log.error("Failed to load duelMemTournament="+duelMemTournament, e);
                    }
                    if (duelMemTournament.isAllPlayed()) {
                        // player have all played => finish duel !
                        duelMgr.finishDuel(duelTournamentPlayer);
                    }
                }
            }

        }
        log.error("Nb DuelTournamentPlayer loaded : "+listTournamentPlayer.size()+" - loadFromFile="+nbLoadFromFile+" - loadFromDB="+nbLoadFromDB);
    }

    /**
     * Backup all DuelMemTournament
     * @param clearMap flag to clear map data
     * @return nb DuelMemTournament backup with success (-1 if config parameter is disable)
     */
    public int backupAllDuel(boolean clearMap) {
        int nbBackup = -1;
        boolean doBackup = duelMgr.getConfigIntValue("backupMemorySave", 1) == 1;
        if (doBackup) {
            nbBackup = 0;
            for (DuelMemTournament e : mapDuelMemTournament.values()) {
                if (backupMemTournamentToFile(e) != null) {
                    nbBackup++;
                }
            }
        }
        if (clearMap) {
            mapDuelMemTournament.clear();
            mapKeyDuelTournament.clear();
        }
        return nbBackup;
    }

    /**
     * Save DuelMemTournament fo file json
     * @param duelMemTournament
     * @return the file data written or null if failed
     */
    public String backupMemTournamentToFile(DuelMemTournament duelMemTournament) {
        if (duelMemTournament != null) {
            String pathBackup = buildBackupFilePathForTournamentDuel(duelMemTournament.tourPlayerID, true);
            if (pathBackup != null) {
                try {
                    jsonTools.transform2File(pathBackup, duelMemTournament, true);
                    log.debug("Backup with success DuelMemTournament - duelMemTournament= "+duelMemTournament);
                    return pathBackup;
                } catch (Exception e) {
                    log.error("Failed to backup DuelMemTournament - mtr="+duelMemTournament, e);
                }
            } else {
                log.error("No path to backup duelMemTournament="+duelMemTournament);
            }
        } else {
            log.error("Param DuelMemTournament is null !");
        }
        return null;
    }

    public String backupMemTournamentToFile(String duelMemTournamentID) {
        return backupMemTournamentToFile(getDuelMemTournament(duelMemTournamentID));
    }

    /**
     * Return the file path used to backup a tournamentDuel (from memory)
     * @param duelTourID
     * @param mkdir if true, create dir if not existing
     * @return
     */
    public String buildBackupFilePathForTournamentDuel(String duelTourID, boolean mkdir) {
        String path = duelMgr.getStringResolvEnvVariableValue("backupMemoryPath", null);
        if (path != null) {
            if (mkdir) {
                try {
                    File f = new File(path);
                    if (!f.exists()) {
                        f.mkdirs();
                    }
                } catch (Exception e) {}
            }
            path = FilenameUtils.concat(path, "" + duelTourID + ".json");
        } else {
            log.error("No backupMemoryPath define in configuration");
        }
        return path;
    }

    /**
     * Transform json data to MemDuelResult and add it in memory
     * @param duelTournamentPlayer
     * @return
     */
    public DuelMemTournament loadDuelResultFromFile(DuelTournamentPlayer duelTournamentPlayer) {
        if (duelTournamentPlayer != null) {
            String filePath = buildBackupFilePathForTournamentDuel(duelTournamentPlayer.getIDStr(), false);
            try {
                DuelMemTournament duelMemTournament = jsonTools.mapDataFromFile(filePath, DuelMemTournament.class);
                if (duelMemTournament != null) {
                    duelMemTournament.dateFinish = duelMgr.computeDateFinish(duelTournamentPlayer);
                    for (DuelMemDeal duelMemDeal : duelMemTournament.tabDeal) {
                        duelMemDeal.computeResult();
                    }
                    // check tournament not yet existing in map
                    if (getDuelMemTournament(duelMemTournament.tourPlayerID) == null) {
                        mapDuelMemTournament.put(duelMemTournament.tourPlayerID, duelMemTournament);
                        mapKeyDuelTournament.put(buildKey(duelMemTournament.player1ID, duelMemTournament.player2ID), duelMemTournament.tourPlayerID);
                        duelMemTournament.computeResultRanking();
                        log.debug("Load with success MemDuelResult = "+duelMemTournament);
                    } else {
                        log.error("TournamentDuel already present in memory ... duelMemTournament="+duelMemTournament);
                    }
                    return duelMemTournament;
                } else {
                    log.error("MemDuelResult is null !! loaded from filePath="+filePath);
                }
            } catch (Exception e) {
                log.error("Failed to load MemDuelResult from filePath="+filePath+" - error="+e.getMessage());
            }
        } else {
            log.error("Param duelTournamentPlayer is null");
        }
        return null;
    }

    public Logger getLogger() {
        return log;
    }

    private String buildKey(long player1, long player2) {
        if (player1 < player2) {
            return player1+"-"+player2;
        } else {
            return player2+"-"+player1;
        }
    }

    /**
     * Return list of DuelMemTournament managed in memory
     * @return
     */
    public List<DuelMemTournament> listDuelMemTournament() {
        return new ArrayList<DuelMemTournament>(mapDuelMemTournament.values());
    }

    /**
     * Get DuelMemTournament for for these 2 players
     * @param player1
     * @param player2
     * @return
     */
    public DuelMemTournament getDuelMemTournamentForPlayers(long player1, long player2) {
        String duelTourID = mapKeyDuelTournament.get(buildKey(player1, player2));
        if (duelTourID != null) {
            return mapDuelMemTournament.get(duelTourID);
        }
        return null;
    }

    /**
     * Get DuelMemTournament associated for this ID
     * @param duelTournamentPlayerID
     * @return
     */
    public DuelMemTournament getDuelMemTournament(String duelTournamentPlayerID) {
        return mapDuelMemTournament.get(duelTournamentPlayerID);
    }

    /**
     * Add duelTournamentPlayer in memory. Check if not yet existing.
     * @param tournamentPlayer
     * @return
     */
    public DuelMemTournament addTournamentPlayer(DuelTournamentPlayer tournamentPlayer) {
        if (tournamentPlayer != null) {
            DuelMemTournament dmt = mapDuelMemTournament.get(tournamentPlayer.getIDStr());
            if (dmt == null) {
                dmt = new DuelMemTournament(tournamentPlayer);
                mapDuelMemTournament.put(tournamentPlayer.getIDStr(), dmt);
                mapKeyDuelTournament.put(buildKey(tournamentPlayer.getPlayer1ID(), tournamentPlayer.getPlayer2ID()), tournamentPlayer.getIDStr());
            }
            return dmt;
        }
        return null;
    }

    /**
     * List duel to send reminder : remainingTime > tsRemainingLow & remainingTime < tsRemainingHigh
     * @param tsRemainingLow
     * @param tsRemainingHigh
     * @return
     */
    public List<DuelMemTournament> listDuelToReminder(long currentTime, long tsRemainingLow, long tsRemainingHigh) {
        List<DuelMemTournament> listResult = new ArrayList<DuelMemTournament>();
        for (Map.Entry<String, DuelMemTournament> e : mapDuelMemTournament.entrySet()) {
            DuelMemTournament dmt = e.getValue();
            // not all played and not expired and not yet reminder
            if (!dmt.isAllPlayed() && !dmt.isExpired() && dmt.dateReminder == 0) {
                long remainingTime = dmt.dateFinish - currentTime;
                if (remainingTime > 0 && remainingTime > tsRemainingLow &&  remainingTime < tsRemainingHigh) {
                    listResult.add(dmt);
                }
            }
        }
        return listResult;
    }

    /**
     * Check game exist for this player on tournament and deal
     * @param tournamentPlayer
     * @param memDeal
     * @param playerID
     * @return
     */
    private DuelGame checkGameOnFinish(DuelTournamentPlayer tournamentPlayer, DuelMemDeal memDeal, long playerID) {
        if (memDeal != null && tournamentPlayer != null) {
            DuelGame game = duelMgr.getGameOnTournamentAndDealForPlayer(tournamentPlayer.getTournament().getIDStr(), memDeal.dealIndex, playerID);
            if (game == null) {
                game = duelMgr.createGame(tournamentPlayer, playerID, memDeal.dealIndex,
                        ContextManager.getArgineEngineMgr().getDefaultProfile(), "",
                        ContextManager.getArgineEngineMgr().getDefaultProfileCards(), "", 0);
            }
            DuelMemGame resPla = memDeal.getForPlayer(playerID);
            if (resPla == null) {
                // game not finished => claim or leave it
                if (!game.isFinished()) {
                    if (game.getBidContract() != null) {
                        game.claimForPlayer(BridgeConstantes.POSITION_SOUTH, 0);
                        game.setFinished(true);
                        // compute score
                        GameMgr.computeScore(game);
                    } else {
                        // not played => score = -32000; rank = -1; result = 25%/-6IMP
                        game.setLeaveValue();
                    }
                    game.setLastDate(System.currentTimeMillis());
                }
                memDeal.setGame(game);
            }
            return game;
        }
        log.error("Param resDeal is null");
        return null;
    }

    /**
     * Finish the duel identify by the tournamentID. After this operation, the duelResult is removed from the memory.
     * @param tournamentPlayer
     * @return the DuelMemTournament after finished duel.
     */
    public DuelMemTournament finishDuel(DuelTournamentPlayer tournamentPlayer) {
        if (tournamentPlayer != null) {
            DuelMemTournament memTournament = mapDuelMemTournament.get(tournamentPlayer.getIDStr());
            if (memTournament != null) {
                for (DuelMemDeal memDeal : memTournament.tabDeal) {
                    // check game exist for the two players
                    // player1
                    DuelGame game1 = checkGameOnFinish(tournamentPlayer,memDeal, memTournament.player1ID);
                    // player2
                    DuelGame game2 = checkGameOnFinish(tournamentPlayer,memDeal, memTournament.player2ID);

                    // update game data
                    DuelMemGame memGame1 = memDeal.getForPlayer(memTournament.player1ID);
                    if (memGame1 != null) {
                        game1.setResult(memGame1.result);
                        game1.setRank(memGame1.getRank());
                    }
                    DuelMemGame memGame2 = memDeal.getForPlayer(memTournament.player2ID);
                    if (memGame1 != null) {
                        game2.setResult(memGame2.result);
                        game2.setRank(memGame2.getRank());
                    }
                    duelMgr.updateGameDB(game1);
                    duelMgr.updateGameDB(game2);
                }
                // compute result & ranking on duel
                memTournament.computeResultRanking();
                // remove duel from memory
                removeMemTournament(memTournament.tourPlayerID);
            }
            return memTournament;
        }
        return null;
    }

    public void removeMemTournament(String tournamentPlayerID) {
        DuelMemTournament e = mapDuelMemTournament.remove(tournamentPlayerID);
        if (e != null) {
            mapKeyDuelTournament.remove(buildKey(e.player1ID, e.player2ID));
        }
    }

    /**
     * Build ResultTournamentPlayer for a player on a tournamentDuel
     * @param duelTournamentPlayerID
     * @param playerCacheAsk
     * @return
     */
    public WSResultTournamentPlayer getResultTournamentPlayer(String duelTournamentPlayerID, PlayerCache playerCacheAsk) {
        if (playerCacheAsk != null) {
            DuelMemTournament duelMemTournament = mapDuelMemTournament.get(duelTournamentPlayerID);
            if (duelMemTournament != null) {
                DuelMemTournamentPlayer memTourPlayer = duelMemTournament.getMemTournamentPlayer(playerCacheAsk.ID);
                if (memTourPlayer != null) {
                    WSResultTournamentPlayer res = memTourPlayer.toWSResultTournamentPlayer();
                    res.setNbTotalPlayer(2);
                    res.setPlayerID(playerCacheAsk.ID);
                    res.setPlayerPseudo(playerCacheAsk.getPseudo());
                    res.setAvatarPresent(playerCacheAsk.avatarPresent);
                    res.setCountryCode(playerCacheAsk.countryCode);
                    res.setPlayerSerie(playerCacheAsk.serie);
                    res.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(playerCacheAsk.ID));
                    return res;
                } else {
                    log.error("No memTourPlayer on duelTournamentPlayerID="+duelTournamentPlayerID+" - for playerCacheAsk="+playerCacheAsk);
                }
            } else {
                log.error("No memoryDuelResult for duelTournamentPlayerID="+duelTournamentPlayerID);
            }
        } else {
            log.error("Parameter null duelTournamentPlayerID=" + duelTournamentPlayerID + " - playerCacheAsk=" + playerCacheAsk);
        }
        return null;
    }

    public DuelMemTournament addGamePlayer(DuelGame game) {
        if (game != null) {
            DuelTournamentPlayer duelTournamentPlayer = duelMgr.getTournamentPlayer(game.getTournament().getIDStr(), game.getPlayerID());
            if (duelTournamentPlayer != null) {
                DuelMemTournament duelMemTournament = getDuelMemTournament(duelTournamentPlayer.getIDStr());
                if (duelMemTournament != null) {
                    duelMemTournament.addGamePlayer(game);
                    return duelMemTournament;
                } else {
                    log.error("Failed to get DuelMemTournament for duelTournamentPlayer="+duelTournamentPlayer);
                }
            } else {
                log.error("Failed to get DuelTournamentPlayer for game="+game);
            }
        }
        return null;
    }
}
