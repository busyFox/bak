package com.funbridge.server.player.cache;

import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.PlayerHandicap;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.TourSeriePlayer;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.tools.JSONTools;
import org.apache.commons.io.FilenameUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * Created by pserent on 08/07/2014.
 * Manage player data in cache
 */
@Component(value="playerCacheMgr")
@Scope(value="singleton")
public class PlayerCacheMgr extends FunbridgeMgr {
    public static int NB_DATA = 100000;
    private PlayerCacheMap mapPlayer = new PlayerCacheMap();
    private LockWeakString lockPlayer = new LockWeakString();
    private JSONTools jsonTools = new JSONTools();
    @Resource(name = "playerMgr")
    private PlayerMgr playerMgr = null;
    @Resource(name = "tourSerieMgr")
    private TourSerieMgr serieMgr = null;

    @Override
    @PostConstruct
    public void init() {
        log.debug("Init");
    }

    @Override
    @PreDestroy
    public void destroy() {
        backupMapPlayer();
        log.debug("Destroy");
    }

    @Override
    public void startUp() {
        log.debug("Startup");
        loadMapPlayer();
    }

    /**
     * Find object using value
     * @param playerID
     * @return
     */
    private PlayerCache findDataByValue(long playerID) {
        for (PlayerCache pc : mapPlayer.map.values()) {
            if (pc.ID == playerID) {
                return pc;
            }
        }
        return null;
    }

    /**
     * Find object using key
     * @param playerID
     * @return
     */
    private PlayerCache findDataByKey(long playerID) {
        return mapPlayer.map.get(playerID);
    }

    /**
     * Compute in byte the size of mapPlayer
     * @return size in bytes
     */
    public long computeByteSizeMapPlayer() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(mapPlayer);
            baos.close();
            return baos.size();
        } catch (Exception e) {
            log.error("Failed to compute map player byte size", e);
        }
        return -1;
    }

    private void testLoadData() {
        mapPlayer.map.clear();
        for (long i = 0;i < NB_DATA;i++) {
            PlayerCache pc = new PlayerCache();
            pc.countryCode = "FR";
            pc.pseudo = "playerPseudo1234567890-"+String.format("%05d", i);
            pc.serie = "SNC";
            pc.ID = i;
            mapPlayer.map.put(i, pc);
        }
    }

    public static void main(String[] args) {
        PlayerCacheMgr mgr = new PlayerCacheMgr();
        long ts = System.currentTimeMillis();
        mgr.testLoadData();
        System.out.println("Time to load data="+(System.currentTimeMillis() - ts));
        System.out.println("Nb player="+mgr.mapPlayer.size());
        System.out.println("Data size=" + mgr.computeByteSizeMapPlayer());

        Random random = new Random(System.nanoTime());
        long computeTS_key = 0;
        long computeTS_value = 0;

        for (int i = 0; i < 1000; i++) {
            long randomID = random.nextInt(NB_DATA);
            ts = System.currentTimeMillis();
            mgr.findDataByKey(randomID);
            computeTS_key += (System.currentTimeMillis() - ts);
            ts = System.currentTimeMillis();
            mgr.findDataByValue(randomID);
            computeTS_value += (System.currentTimeMillis() - ts);
        }
        System.out.println("Average time for key="+(computeTS_key/100));
        System.out.println("Average time for value="+(computeTS_value/100));

        ts = System.currentTimeMillis();
        System.out.println("Backup = "+mgr.backupMapPlayer()+" - ts="+(System.currentTimeMillis() - ts));
        ts = System.currentTimeMillis();
        System.out.println("Load = "+mgr.loadMapPlayer()+" - ts="+(System.currentTimeMillis() - ts));
        System.out.println("Nb player="+mgr.mapPlayer.size());
        System.out.println("Data size=" + mgr.computeByteSizeMapPlayer());
    }

    /**
     * return the path to store/load map player
     * @return
     */
    private String getbackupPathMapPlayer() {
        String pathBackup = FBConfiguration.getInstance().getStringResolvEnvVariableValue("playerCache.backupMemoryPath", null);
        if (pathBackup != null) {
            try {
                File f = new File(pathBackup);
                if (!f.exists()) {
                    f.mkdirs();
                }
                pathBackup = FilenameUtils.concat(pathBackup, "mapPlayer.json");
            } catch (Exception e) {
                log.error("Failed to create dir - pathBackup="+pathBackup, e);
                pathBackup = null;
            }
        } else {
            log.error("No path define in configuration playerCache.backupMemoryPath");
        }
        return pathBackup;
    }

    /**
     * Backup map player to JSON file
     * @return
     */
    public boolean backupMapPlayer() {
        if (FBConfiguration.getInstance().getIntValue("playerCache.backupMemoryEnable", 1) == 1){
            String pathBackup = getbackupPathMapPlayer();
            if (pathBackup != null) {
                try {
                    long ts = System.currentTimeMillis();
                    jsonTools.transform2File(pathBackup, mapPlayer, true);
                    log.error("Backup with success mapPlayer size=" + mapPlayer.size() + " - pathBackup=" + pathBackup + " - ts=" + (System.currentTimeMillis() - ts));
                    return true;
                } catch (Exception e) {
                    log.error("Failed to backup mapPlayer", e);
                }
            } else {
                log.error("Path backup is null");
            }
        } else {
            log.error("Backup player cache not enable");
        }
        return false;
    }

    /**
     * Load map player from JSON file
     * @return
     */
    public boolean loadMapPlayer() {
        if (FBConfiguration.getInstance().getIntValue("playerCache.loadFromBackupEnable", 1) == 1) {
            String pathBackup = getbackupPathMapPlayer();
            if (pathBackup != null) {
                try {
                    long ts = System.currentTimeMillis();
                    mapPlayer = jsonTools.mapDataFromFile(pathBackup, PlayerCacheMap.class);
                    log.info("Load with success mapPlayer size=" + mapPlayer.size() + " - pathBackup=" + pathBackup + " - ts=" + (System.currentTimeMillis() - ts));
                    return true;
                } catch (Exception e) {
                    log.error("Failed to load mapPlayer from pathBackup="+pathBackup, e);
                }
            } else {
                log.error("Path backup is null");
            }
        } else {
            log.error("Load backup player cache not enable");
        }
        return false;
    }

    /**
     * Get PlayerCache object and update data. If player not yet existing in map, load it.
     * @param playerID
     * @return
     */
    public PlayerCache getAndLoadPlayerCache(long playerID) {
        synchronized (lockPlayer.getLock(""+playerID)) {
            PlayerCache pc = mapPlayer.map.get(playerID);
            if (pc == null) {
                pc = new PlayerCache(playerID);
                mapPlayer.map.put(playerID, pc);
            }
            Player p = playerMgr.getPlayer(playerID);
            PlayerHandicap ph = playerMgr.getPlayerHandicap(playerID);
            if (p != null) {
                pc.setPlayerData(p, ph);
            }
            TourSeriePlayer tsp = serieMgr.getOrCreatePlayerSerie(playerID);
            if (tsp != null) {
                pc.setPlayerSerieData(tsp);
                if (p.getDisplayCountryCode() != null && (tsp.getCountryCode() == null || !tsp.getCountryCode().equals(p.getDisplayCountryCode()))) {
                    // update countryCode in TourSeriePlayer
                    serieMgr.updatePlayerCountryCode(p);
                }
            }
            return pc;
        }
    }

    /**
     * Get PlayerCache object. If player not yet existing in map, load it. No update data if already present.
     * @param playerID
     * @return
     */
    public PlayerCache getOrLoadPlayerCache(long playerID) {
        synchronized (lockPlayer.getLock(""+playerID)) {
            PlayerCache pc = mapPlayer.map.get(playerID);
            if (pc == null) {
                pc = new PlayerCache(playerID);
                Player p = playerMgr.getPlayer(playerID);
                PlayerHandicap ph = playerMgr.getPlayerHandicap(playerID);
                if (p != null) {
                    pc.setPlayerData(p, ph);
                }
                TourSeriePlayer tsp = serieMgr.getTourSeriePlayer(playerID);
                if (tsp != null) {
                    pc.setPlayerSerieData(tsp);
                }
                mapPlayer.map.put(playerID, pc);
            }
            return pc;
        }
    }

    /**
     * Simply get playerCache object for this playerID
     * @param playerID
     * @return
     */
    public PlayerCache getPlayerCache(long playerID) {
        PlayerCache pc = mapPlayer.map.get(playerID);
        if (pc == null) {
            pc = getOrLoadPlayerCache(playerID);
        }

        return pc;
    }

    /**
     * Update the PlayerCache object. All data. If not yet existing in map, add it
     * @param playerID
     * @return
     */
    public PlayerCache updatePlayerAllData(long playerID) {
        synchronized (lockPlayer.getLock(""+playerID)) {
            PlayerCache pc = mapPlayer.map.get(playerID);
            if (pc == null) {
                pc = new PlayerCache(playerID);
                mapPlayer.map.put(playerID, pc);
            }
            Player p = playerMgr.getPlayer(playerID);
            PlayerHandicap ph = playerMgr.getPlayerHandicap(playerID);
            if (p != null) {
                pc.setPlayerData(p, ph);
            }
            TourSeriePlayer tsp = serieMgr.getOrCreatePlayerSerie(playerID);
            if (tsp != null) {
                pc.setPlayerSerieData(tsp);
            }
            return pc;
        }
    }

    /**
     * Update the PlayerCache object. All data. If not yet existing in map, add it
     * @param player object used to set value in cache
     * @return
     */
    public PlayerCache updatePlayerAllData(Player player) {
        if (player != null) {
            synchronized (lockPlayer.getLock(""+player.getID())) {
                PlayerCache pc = mapPlayer.map.get(player.getID());
                if (pc == null) {
                    pc = new PlayerCache(player.getID());
                    mapPlayer.map.put(player.getID(), pc);
                }
                PlayerHandicap ph = playerMgr.getPlayerHandicap(player.getID());
                pc.setPlayerData(player, ph);
                TourSeriePlayer tsp = serieMgr.getOrCreatePlayerSerie(player.getID());
                if (tsp != null) {
                    pc.setPlayerSerieData(tsp);
                }
                return pc;
            }
        }
        return null;
    }

    /**
     * Update for all player all data.
     * Warning : could be long ... reload all data from DB
     */
    public void updateAllPlayerAllData() {
        Iterator<PlayerCache> it = mapPlayer.map.values().iterator();
        while (it.hasNext()) {
            PlayerCache pc = it.next();
            Player p = playerMgr.getPlayer(pc.ID);
            if (p != null) {
                PlayerHandicap ph = playerMgr.getPlayerHandicap(p.getID());
                if (p != null) {
                    pc.setPlayerData(p, ph);
                }
                TourSeriePlayer tsp = serieMgr.getOrCreatePlayerSerie(pc.ID);
                if (tsp != null) {
                    pc.setPlayerSerieData(tsp);
                }
            }
            else {
                it.remove();
            }
        }
    }

    /**
     * Update for all player data serie.
     * Warning : could be long ... reload serie data from DB
     */
    public void updateAllPlayerSerieData() {
        for (PlayerCache pc : mapPlayer.map.values()) {
            TourSeriePlayer tsp = serieMgr.getOrCreatePlayerSerie(pc.ID);
            if (tsp != null) {
                pc.setPlayerSerieData(tsp);
            }
        }
    }

    /**
     * Update the serie for list of players
     * @param listPlaID
     * @param newSerie
     * @return
     */
    public int updatePlayerSerieForList(List<Long> listPlaID, String newSerie) {
        int nbPlayerUpdated = 0;
        for (Long l : listPlaID) {
            PlayerCache pc = mapPlayer.map.get(l);
            if (pc != null) {
                pc.serie = newSerie;
                nbPlayerUpdated++;
            }
        }
        return nbPlayerUpdated;
    }

    /**
     * Remove the playerCache object for this playerID
     * @param playerID
     * @return
     */
    public PlayerCache removePlayerCache(long playerID) {
        synchronized (lockPlayer.getLock(""+playerID)) {
            PlayerCache pc = mapPlayer.map.remove(playerID);
            return pc;
        }
    }

    /**
     * Return cache size (number of elements ...)
     * @return
     */
    public int getCacheSize() {
        return mapPlayer.size();
    }

    /**
     * Return collection values from cache
     * @return
     */
    public Collection<PlayerCache> getCollectionCacheValue() {
        return mapPlayer.map.values();
    }

    /**
     * Return playerCache with pseudo
     * @param pseudo
     * @return
     */
    public PlayerCache getPlayerWithPseudo(String pseudo) {
        for (PlayerCache pc : mapPlayer.map.values()) {
            if (pc.pseudo != null && pc.pseudo.equals(pseudo)) {
                return pc;
            }
        }
        return null;
    }
}
