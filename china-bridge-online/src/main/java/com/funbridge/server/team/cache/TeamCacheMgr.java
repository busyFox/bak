package com.funbridge.server.team.cache;

import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.team.TeamMgr;
import com.funbridge.server.team.data.Team;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by bplays on 25/11/16.
 */
@Component(value="teamCacheMgr")
@Scope(value="singleton")
public class TeamCacheMgr extends FunbridgeMgr {

    private TeamCacheMap mapTeam = new TeamCacheMap();
    private JSONTools jsonTools = new JSONTools();
    @Resource(name="teamMgr")
    private TeamMgr teamMgr = null;

    @Override
    @PostConstruct
    public void init() {
        log.debug("init");
    }

    @Override
    @PreDestroy
    public void destroy() {
        log.debug("destroy");
        backupMapTeam();
        removeAllTeamCache();
    }

    @Override
    public void startUp() {
        log.debug("startup");
        loadMapTeam();
    }

    /**
     * Backup map team to JSON file
     * @return
     */
    public boolean backupMapTeam() {
        if (FBConfiguration.getInstance().getIntValue("teamCache.backupMemoryEnable", 1) == 1){
            String pathBackup = getbackupPathMapTeam();
            if (pathBackup != null) {
                try {
                    long ts = System.currentTimeMillis();
                    jsonTools.transform2File(pathBackup, mapTeam, true);
                    log.error("Backup with success mapTeam size=" + mapTeam.size() + " - pathBackup=" + pathBackup + " - ts=" + (System.currentTimeMillis() - ts));
                    return true;
                } catch (Exception e) {
                    log.error("Failed to backup mapTeam", e);
                }
            } else {
                log.error("Path backup is null");
            }
        } else {
            log.error("Backup team cache not enable");
        }
        return false;
    }

    public void removeAllTeamCache() {
        mapTeam.map.clear();
    }

    /**
     * return the path to store/load map player
     * @return
     */
    private String getbackupPathMapTeam() {
        String pathBackup = FBConfiguration.getInstance().getStringResolvEnvVariableValue("teamCache.backupMemoryPath", null);
        if (pathBackup != null) {
            try {
                File f = new File(pathBackup);
                if (!f.exists()) {
                    f.mkdirs();
                }
                pathBackup = FilenameUtils.concat(pathBackup, "mapTeam.json");
            } catch (Exception e) {
                log.error("Failed to create dir - pathBackup="+pathBackup, e);
                pathBackup = null;
            }
        } else {
            log.error("No path define in configuration teamCache.backupMemoryPath");
        }
        return pathBackup;
    }

    /**
     * Load map team from JSON file
     * @return
     */
    public boolean loadMapTeam() {
        if (FBConfiguration.getInstance().getIntValue("teamCache.loadFromBackupEnable", 1) == 1) {
            String pathBackup = getbackupPathMapTeam();
            if (pathBackup != null) {
                try {
                    long ts = System.currentTimeMillis();
                    mapTeam = jsonTools.mapDataFromFile(pathBackup, TeamCacheMap.class);
                    log.error("Load with success mapTeam size=" + mapTeam.size() + " - pathBackup=" + pathBackup + " - ts=" + (System.currentTimeMillis() - ts));
                    return true;
                } catch (Exception e) {
                    log.error("Failed to load mapTeam from pathBackup="+pathBackup, e);
                }
            } else {
                log.error("Path backup is null");
            }
        } else {
            log.error("Load backup team cache not enable");
        }
        return false;
    }

    /**
     * Get TeamCache object. If team not yet existing in map, load it. No update data if already present.
     * @param teamID
     * @return
     */
    public TeamCache getOrLoadTeamCache(String teamID) {
        TeamCache teamCache = mapTeam.map.get(teamID);
        if (teamCache == null) {
            teamCache = new TeamCache(teamID);
            Team t = teamMgr.findTeamByID(teamID);
            if (t != null) {
                teamCache.setTeamData(t);
            }
            mapTeam.map.put(teamID, teamCache);
        }
        return teamCache;
    }

    /**
     * Update a TeamCache object. If not existing in map, add it
     * @param team
     * @return
     */
    public TeamCache updateTeamData(Team team){
        if(team != null){
            TeamCache teamCache = mapTeam.map.get(team.getIDStr());
            if(teamCache == null){
                teamCache = new TeamCache(team.getIDStr());
                mapTeam.map.put(team.getIDStr(), teamCache);
            }
            teamCache.setTeamData(team);
            return teamCache;
        }
        return null;
    }

    /**
     * Update teamCache data for these teams
     * @param teams
     * @return
     */
    public int updateTeamsData(List<Team> teams) {
        int nbUpdate = 0;
        for (Team t : teams) {
            if (updateTeamData(t) != null) {
                nbUpdate++;
            }
        }
        return nbUpdate;
    }

    /**
     * Update a TeamCache object. If not existing in map, add it
     * @param teamID
     * @return
     */
    public TeamCache updateTeamData(String teamID){
        Team team = teamMgr.findTeamByID(teamID);
        return updateTeamData(team);
    }

    /**
     * Update all the teams in the map. WARNING : COULD BE LONG !
     */
    public void updateAllTeamData(){
        // iterator to remove cache data if not found team in DB
        for(Iterator<Map.Entry<String, TeamCache>> it = mapTeam.map.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, TeamCache> entry = it.next();
            Team team = teamMgr.findTeamByID(entry.getKey());
            if(team != null){
                entry.getValue().setTeamData(team);
            } else {
                it.remove();
            }
        }
    }

    /**
     * Update division for teams with id in listTeamID
     * @param listTeamID
     * @param division
     */
    public void updateTeamsDivision(List<String> listTeamID, String division) {
        for (String e : listTeamID) {
            TeamCache teamCache = mapTeam.map.get(e);
            if (teamCache != null) {
                teamCache.division = division;
            }
        }
    }

    /**
     * Remove the TeamCache object for this teamID
     * @param teamID
     * @return
     */
    public TeamCache removeTeamCache(String teamID) {
        TeamCache pc = mapTeam.map.remove(teamID);
        return pc;
    }

    /**
     * Find TeamCache by name
     * @param name
     * @return
     */
    public TeamCache findTeamCacheByName(String name) {
        for (TeamCache tc : mapTeam.map.values()) {
            if (tc.name != null && tc.name.equals(name)) {
                return tc;
            }
        }
        return null;
    }

    /**
     * Find TeamCache by ID
     * @param ID
     * @return
     */
    public TeamCache findTeamCacheByID(String ID) {
        return mapTeam.map.get(ID);
    }

    /**
     * Return cache size (number of elements)
     * @return
     */
    public int getCacheSize() {
        return mapTeam.size();
    }

    /**
     * Count teams with nbPlayers >= threshold
     * @param threshold
     * @return
     */
    public int getNbTeamsWithNbPlayersGreater(int threshold) {
        int nbTeamCompleted = 0;
        for (TeamCache tc : mapTeam.map.values()) {
            if (tc.getNbPlayers() >= threshold) {
                nbTeamCompleted++;
            }
        }
        return nbTeamCompleted;
    }

    /**
     * Return collection values from cache
     * @return
     */
    public Collection<TeamCache> getCollectionCacheValue() {
        return mapTeam.map.values();
    }

    /**
     * Compute in byte the size of mapTeam
     * @return size in bytes
     */
    public long computeByteSizeMapTeam() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(mapTeam);
            baos.close();
            return baos.size();
        } catch (Exception e) {
            log.error("Failed to compute mapTeam byte size", e);
        }
        return -1;
    }

}
