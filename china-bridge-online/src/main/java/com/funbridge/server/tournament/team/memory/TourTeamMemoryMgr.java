package com.funbridge.server.tournament.team.memory;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.team.TeamMgr;
import com.funbridge.server.team.cache.TeamCache;
import com.funbridge.server.team.cache.TeamCacheMgr;
import com.funbridge.server.team.data.Team;
import com.funbridge.server.team.data.TeamPlayer;
import com.funbridge.server.tournament.generic.memory.TournamentGenericMemoryMgr;
import com.funbridge.server.tournament.team.TourTeamMgr;
import com.funbridge.server.tournament.team.data.TeamGame;
import com.funbridge.server.tournament.team.data.TeamPeriod;
import com.funbridge.server.tournament.team.data.TeamTourTeamAggregatePoint;
import com.funbridge.server.tournament.team.data.TeamTournament;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.team.WSTeamPeriodResult;
import com.funbridge.server.ws.team.WSTeamPlayer;
import com.funbridge.server.ws.team.WSTeamResult;
import com.funbridge.server.ws.team.WSTeamTourResult;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.tools.JSONTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pserent on 17/11/2016.
 */
public class TourTeamMemoryMgr extends TournamentGenericMemoryMgr {
    private TeamCacheMgr teamCacheMgr = null;
    private TeamMgr teamMgr = null;

    protected Logger log = LogManager.getLogger(this.getClass());
    private JSONTools jsonTools = new JSONTools();
    private TourTeamMgr tourMgr = null;
    private LockWeakString lockTour = new LockWeakString();
    // Map to store tournament result for each division : DIVISION ==> (Map TournamentID => MEM_TOURNAMENT)
    private Map<String, ConcurrentHashMap<String, TeamMemTournament>> mapDivisionTournament = new HashMap<>();
    // Map to store division TOUR result for periodTour : DIVISION ==> MEM_DIVISION_TOUR_RESULT - Result for tour
    private Map<String, TeamMemDivisionTourResult> mapDivisionTourResult = new HashMap<>();
    // Map to store division result for period : DIVISION ==> MEM_DIVISION_RESULT_TEAM - Result for period
    private Map<String, TeamMemDivisionResult> mapDivisionResult = new HashMap<>();

    public TourTeamMemoryMgr(TourTeamMgr mgr) {
        this.tourMgr = mgr;
        this.teamCacheMgr = ContextManager.getTeamCacheMgr();
        this.teamMgr = ContextManager.getTeamMgr();
        // init maps
        for (String division : TourTeamMgr.DIVISION_TAB) {
            mapDivisionTournament.put(division, new ConcurrentHashMap<String, TeamMemTournament>());
            mapDivisionTourResult.put(division, new TeamMemDivisionTourResult(division));
            mapDivisionResult.put(division, new TeamMemDivisionResult(division, tourMgr.getConfigAveragePerformanceMinimum(division, 0)));
        }
    }

    public void destroy() {
        if (tourMgr.getConfigIntValue("backupMemoryEnable", 1) == 1) {
            backupAllTournament();
        }
        mapDivisionTournament.clear();
        mapDivisionTourResult.clear();
        mapDivisionResult.clear();
    }

    public Object getLockTour(String tourID) {
        return lockTour.getLock(tourID);
    }

    /**
     * Load all necessary data for period : tournament, tournamentPlayer, division TourResult and divisionResult
     * @param period
     */
    public void loadDataForPeriod(TeamPeriod period) {
        if (period != null) {

            // clear all data before
            for (Map<String, TeamMemTournament> e : mapDivisionTournament.values()) {
                e.clear();
            }
            String periodTour = period.getPeriodTour();

            // Load tournaments
            if (periodTour != null) {
                // loop on each division to get tournament not finish on this period tour
                for (String div : TourTeamMgr.DIVISION_TAB) {
                    List<TeamTournament> listTour = tourMgr.listTournamentNotFinishedForPeriodTourAndDivision(periodTour, div);
                    log.warn("Division=" + div + " - periodTour=" + periodTour + " - Nb tour to load=" + listTour.size());
                    int nbLoadFromFile = 0, nbLoadFromDB = 0;
                    TeamMemDivisionTourResult divisionTourResult = getMemDivisionTourResult(div);
                    // retrieve list game of each tour
                    for (TeamTournament t : listTour) {
                        boolean loadFromDB = false;
                        TeamMemTournament memTour = null;
                        if (tourMgr.getConfigIntValue("loadFromBackupEnable", 1) == 1) {
                            memTour = loadMemTourFromFile(tourMgr.buildBackupFilePathForTournament(t.getIDStr(), t.getDivision(), false));
                            if (memTour != null) {
                                nbLoadFromFile++;
                            } else {
                                loadFromDB = true;
                            }
                        } else {
                            loadFromDB = true;
                        }
                        if (loadFromDB) {
                            memTour = addTournament(t);
                            if (memTour != null) {
                                List<TeamGame> listGame = tourMgr.listGameOnTournament(t.getIDStr());
                                log.warn("Tour=" + t + " - Nb game to load=" + listGame.size());
                                for (TeamGame g : listGame) {
                                    if (g.isFinished()) {
                                        try {
                                            addResult(g);
                                        } catch (FBWSException e) {
                                            log.error("Exception to load game=" + g, e);
                                        }
                                    } else {
                                        memTour.getOrCreateTournamentPlayer(g.getPlayerID(), g.getTeamID(), g.getStartDate());
                                    }
                                }
                                nbLoadFromDB++;
                            } else {
                                log.error("Failed to add tournament in memory !! - tour=" + t);
                            }
                        }
                        // set division tour result with all tournamentPlayer
                        if (memTour != null) {
                            if (divisionTourResult != null) {
                                for (TeamMemTournamentPlayer memTournamentPlayer : memTour.getTourPlayers()) {
                                    divisionTourResult.addPlayerTournamentResult(memTournamentPlayer, false);
                                }
                                divisionTourResult.computeRanking();
                            } else {
                                log.error("No divisionTouResult found for division=" + div);
                            }
                        }
                    }
                    log.warn("Division=" + div + " - periodTour=" + periodTour + " - nbLoadFromFile=" + nbLoadFromFile + " - nbLoadFromDB=" + nbLoadFromDB);
                }
            } else {
                log.warn("Current tour is null => No load tournament for period="+period);
            }

            // load division result
            for (String div : TourTeamMgr.DIVISION_TAB) {
                TeamMemDivisionResult divisionResult = getMemDivisionResult(div);
                TeamMemDivisionTourResult divisionTourResult = getMemDivisionTourResult(div);
                if (divisionResult != null && divisionTourResult != null) {
                    divisionResult.loadThresholdFromConfiguration();
                    // load result for previous tour of period
                    int nbTeamsUpdatePointsPeriod = setTeamPointsAndResultPeriodForDivision(div);
                    log.warn("Set points and result period for teams in divisionResult "+div+" - nbTeams="+nbTeamsUpdatePointsPeriod);

                    // add result for current period
                    for (TeamMemDivisionTourResultTeam e : divisionTourResult.listTeamResults()) {
                        divisionResult.addTourResultTeamForCurrentPeriodTour(e, divisionTourResult, false);
                    }

                    // set handicap for teams
                    int nbTeamsUpdated = 0;
                    List<Team> listTeam = teamMgr.getTeamsForDivision(div);
                    for (Team t : listTeam) {
                        TeamMemDivisionResultTeam resultTeam = divisionResult.getTeamResult(t.getIDStr());
                        if (resultTeam != null) {
                            resultTeam.averageHandicap = t.getAverageHandicap();
                            nbTeamsUpdated++;
                        }
                    }
                    log.warn("Set handicap for tems in divisionResult "+div+" - nbTeamsUpdated="+nbTeamsUpdated+" - total nb teams in DB for this division="+listTeam.size());

                    // compute ranking
                    divisionResult.computeRanking();

                    // set value of nb teams for each tournament
                    List<TeamMemTournament> listMemTournament = listMemTournamentForDivision(div);
                    for (TeamMemTournament e : listMemTournament) {
                        e.nbTeams = divisionResult.getNbTeams();
                    }
                } else {
                    log.error("No divisionResult found for division="+div);
                }
            }
        } else {
            log.error("Parameters not valid - period="+period);
        }
    }

    /**
     * Update points period for memory data : sum of points for finished tours in current period.
     * @param division
     * @return
     */
    public int setTeamPointsAndResultPeriodForDivision(String division) {
        int nbTeams = 0;
        TeamMemDivisionResult divisionResult = getMemDivisionResult(division);
        if (division != null && tourMgr.getCurrentPeriod() != null) {
            List<TeamTourTeamAggregatePoint> listTeamPoints = tourMgr.listTeamPointsForDivisionAndPeriod(division, tourMgr.getCurrentPeriod().getID());
            for (TeamTourTeamAggregatePoint e : listTeamPoints) {
                divisionResult.setTeamPointsAndResultPeriod(e.teamID, e.nbPoints, e.cumulResult);
                nbTeams++;
            }
        } else {
            log.error("Null parameter : division="+division+" - currentPeriod="+tourMgr.getCurrentPeriod());
        }
        return nbTeams;
    }

    /**
     * Transform json data to MemTouResult and add it in memory
     * @param filePath
     * @return
     */
    public TeamMemTournament loadMemTourFromFile(String filePath) {
        if (filePath != null) {
            try {
                TeamMemTournament memTour = jsonTools.mapDataFromFile(filePath, TeamMemTournament.class);
                if (memTour != null) {
                    // check tournament not yet existing in map
                    if (addMemTour(memTour)) {
                        memTour.initAfterLoad();
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
     * Backup all tournament mem object
     * @return nb MemTourResult backup with success
     */
    public int backupAllTournament() {
        int nbMemTourBackup = 0;
        long ts = System.currentTimeMillis();
        log.error("Backup All Tour - Start");
        for (String div : TourTeamMgr.DIVISION_TAB) {
            int tempNb = 0;
            long tsTemp = System.currentTimeMillis();
            Map<String, TeamMemTournament> mapTour = mapDivisionTournament.get(div);
            if (mapTour != null) {
                for (TeamMemTournament memTour : mapTour.values()) {
                    if (backupMemTourToFile(memTour)) {
                        tempNb++;
                    }
                }
            }
            log.error("Backup mem tour for division="+div+" - nbTour="+tempNb+" - ts="+(System.currentTimeMillis() - tsTemp));
            nbMemTourBackup += tempNb;
        }
        log.error("Backup AllTour - End - nbMemTourBackup="+nbMemTourBackup+" - ts="+(System.currentTimeMillis() - ts));
        return nbMemTourBackup;
    }


    /**
     * Save MemTournament to JSON
     * @param memTour
     * @return
     */
    public boolean backupMemTourToFile(TeamMemTournament memTour) {
        if (memTour != null) {
            String pathBackup = tourMgr.buildBackupFilePathForTournament(memTour.tourID, memTour.division, true);
            if (pathBackup != null) {
                try {
                    jsonTools.transform2File(pathBackup, memTour, true);
                    log.debug("Backup with success TeamMemTournament="+memTour);
                    return true;
                } catch (Exception e) {
                    log.error("Failed to backup TeamMemTournament="+memTour, e);
                }
            } else {
                log.error("No path to backup TeamMemTournament="+memTour);
            }
        } else {
            log.error("Param memTour is null !");
        }
        return false;
    }

    public Map<String, TeamMemTournament> getMapTourForDivision(String division) {
        return mapDivisionTournament.get(division);
    }

    /**
     * Add memTour in memory
     * @param memTour
     * @return true if the tournament is not yet existing is memory
     */
    private boolean addMemTour(TeamMemTournament memTour) {
        if (memTour != null) {
            Map<String, TeamMemTournament> mapMemTour = getMapTourForDivision(memTour.division);
            if (mapMemTour != null) {
                if (!mapMemTour.containsKey(memTour.tourID)) {
                    mapMemTour.put(memTour.tourID, memTour);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add Tournament in memory
     * @param tour
     */
    public TeamMemTournament addTournament(TeamTournament tour) {
        Map<String, TeamMemTournament> mapMemTour = getMapTourForDivision(tour.getDivision());
        if (mapMemTour != null) {
            TeamMemTournament memTour = mapMemTour.get(tour.getIDStr());
            if (memTour == null) {
                memTour = new TeamMemTournament(tour);
                mapMemTour.put(tour.getIDStr(), memTour);
            } else {
                log.warn("Mem tournament already existed for tour="+tour);
            }
            return memTour;
        } else {
            log.error("Failed to add tournament="+tour);
        }
        return null;
    }

    /**
     * Return the memTournament associated to this tournamentID in the division
     * @param division
     * @param tournamentID
     * @return
     */
    public TeamMemTournament getMemTournamentDivisionAndTourID(String division, String tournamentID) {
        ConcurrentHashMap<String, TeamMemTournament> mapTourMem = mapDivisionTournament.get(division);
        if (mapTourMem != null) {
            return mapTourMem.get(tournamentID);
        }
        return null;
    }

    /**
     * Return the memTournament associated to this division and group
     * @param division
     * @param group
     * @return
     */
    public TeamMemTournament getMemTournamentForDivisionAndGroup(String division, String group) {
        ConcurrentHashMap<String, TeamMemTournament> mapTourMem = mapDivisionTournament.get(division);
        if (mapTourMem != null) {
            for (TeamMemTournament memTour : mapTourMem.values()) {
                if (memTour.group.equals(group)) {
                    return memTour;
                }
            }
        }
        return null;
    }

    /**
     * Return the memTournament associated to this division and playerID
     * @param division
     * @param playerID
     * @return
     */
    public TeamMemTournament getMemTournamentForDivisionAndPlayer(String division, long playerID) {
        ConcurrentHashMap<String, TeamMemTournament> mapTourMem = mapDivisionTournament.get(division);
        if (mapTourMem != null) {
            for (TeamMemTournament memTour : mapTourMem.values()) {
                if (memTour.getTournamentPlayer(playerID) != null) {
                    return memTour;
                }
            }
        }
        return null;
    }

    public TeamMemTournamentPlayer getMemTournamentPlayer(long playerID) {
        for(String division : mapDivisionTournament.keySet()) {
            List<TeamMemTournament> memTournaments = listMemTournamentForDivision(division);
            for (TeamMemTournament e : memTournaments) {
                TeamMemTournamentPlayer teamMemTournamentPlayer = e.getTournamentPlayer(playerID);
                if (teamMemTournamentPlayer != null) {
                    return teamMemTournamentPlayer;
                }
            }
        }
        return null;
    }


    /**
     * Find a memTournament by its ID
     * @param tournamentID
     * @return
     */
    public TeamMemTournament getMemTournament(String tournamentID){
        TeamMemTournament memTournament = null;
        for(String division : mapDivisionTournament.keySet()){
            memTournament = getMemTournamentDivisionAndTourID(division, tournamentID);
            if(memTournament != null) break;
        }
        return memTournament;
    }

    public List<TeamMemTournament> listMemTournamentForDivision(String division) {
        ConcurrentHashMap<String, TeamMemTournament> mapTourMem = mapDivisionTournament.get(division);
        if (mapTourMem != null) {
            return new ArrayList<>(mapTourMem.values());
        }
        return null;
    }

    /**
     * Return a list of all results for a team on current tour. The lead players results + the global team result
     * @param teamID
     * @return
     */
    public List<WSTeamTourResult> getListWSTeamTourResultForTeam(String teamID){
        List<WSTeamTourResult> listWSTeamTourResult = new ArrayList<>();
        // Get team players results for the tour
        List<TeamMemTournamentPlayer> playersResults = getListMemTournamentPlayerForTeam(teamID);
        for(TeamMemTournamentPlayer playerResult : playersResults){
            listWSTeamTourResult.add(toWSTeamTourResult(playerResult));
        }
        // Add the team lead players who didn't start the tournament yet
        if(playersResults.size() < teamMgr.getNbLeadPlayers()){
            Team team = teamMgr.findTeamByID(teamID);
            if(team != null){
                for(TeamPlayer tp : team.getPlayers()){
                    if(tp.isSubstitute()) continue; // Only lead players
                    boolean playerStartedTournament = false;
                    boolean groupAlreadyPlayed = false;
                    for(TeamMemTournamentPlayer playerResult : playersResults){
                        // If team player is found in the results, go on to next player
                        if(playerResult.playerID == tp.getPlayerID()){
                            playerStartedTournament = true;
                            break;
                        }
                        // If there's already a result for the group of the player (he arrived after someone already played in the group) then go on to next player
                        if(playerResult.memTour.group.equalsIgnoreCase(tp.getGroup())){
                            groupAlreadyPlayed = true;
                            break;
                        }
                    }
                    // If team player hasn't started the tournament, add him to the list of WSResult
                    if(!playerStartedTournament && !groupAlreadyPlayed){
                        WSTeamTourResult wsTeamTourResult = new WSTeamTourResult();
                        wsTeamTourResult.player = teamMgr.toWSTeamPlayer(tp.getPlayerID(), tp.isCaptain(), false, tp.getGroup());
                        wsTeamTourResult.nbDeals = tourMgr.getConfigIntValue("tourNbDeal", 15);
                        listWSTeamTourResult.add(wsTeamTourResult);
                    }
                }
            }
        }
        // Order by group
        Collections.sort(listWSTeamTourResult, new Comparator<WSTeamTourResult>() {
            @Override
            public int compare(WSTeamTourResult o1, WSTeamTourResult o2) {
                return o1.compareTo(o2);
            }
        });
        // Add the global team result on the tour (player = null in WSTeamTourResult)
        listWSTeamTourResult.add(0, toWSTeamTourResult(getTeamMemDivisionTourResultForTeam(teamID, null)));
        return listWSTeamTourResult;
    }

    /**
     * Get a list of TeamMemTournamentPlayer for a team on current tour
     * @param teamID
     * @return
     */
    public List<TeamMemTournamentPlayer> getListMemTournamentPlayerForTeam(String teamID){
        List<TeamMemTournamentPlayer> listMemTournamentPlayer = new ArrayList<>();
        TeamCache teamCache = teamCacheMgr.getOrLoadTeamCache(teamID);
        if(teamCache != null){
            if (teamCache.division != null && !teamCache.division.equals(TourTeamMgr.DIVISION_NO)) {
                // Get Tour results for the team's division
                TeamMemDivisionTourResult divisionTourResult = mapDivisionTourResult.get(teamCache.division);
                if (divisionTourResult != null) {
                    // Get Team results
                    TeamMemDivisionTourResultTeam teamResult = divisionTourResult.getTeamResult(teamID);
                    if (teamResult != null) {
                        return teamResult.listData;
                    } else {
                        log.error("no results found for team=" + teamID + " on current tour");
                    }
                } else {
                    log.error("no results found for division=" + teamCache.division);
                }
            }
        } else {
            log.error("no team found for teamID="+teamID);
        }
        return listMemTournamentPlayer;
    }

    public TeamMemDivisionTourResultTeam getTeamMemDivisionTourResultForTeam(String teamID, String division){
        TeamCache teamCache = teamCacheMgr.getOrLoadTeamCache(teamID);
        if(teamCache != null) {
            if(division == null){
                division = teamCache.division;
            }
            if (!division.equals(TourTeamMgr.DIVISION_NO)) {
                // Get Tour results for the team's division
                TeamMemDivisionTourResult divisionTourResult = mapDivisionTourResult.get(division);
                if (divisionTourResult != null) {
                    // Get Team results
                    return divisionTourResult.getTeamResult(teamID);
                } else {
                    log.error("no results found for division=" + division);
                }
            }
        } else {
            log.error("no team found for teamID="+teamID);
        }
        return null;
    }

    public TeamMemDivisionResultTeam getTeamMemDivisionResultForTeam(String teamID, String division){
        TeamCache teamCache = teamCacheMgr.getOrLoadTeamCache(teamID);
        if(teamCache != null){
            if(division == null){
                division = teamCache.division;
            }
            // Get Period results for the team's division
            TeamMemDivisionResult divisionResult = mapDivisionResult.get(division);
            if(divisionResult != null){
                // Get Team results
                return divisionResult.getTeamResult(teamID);
            } else {
                log.error("no results found for division="+division);
            }
        } else {
            log.error("no team found for teamID="+teamID);
        }
        return null;
    }

    public WSTeamPeriodResult toWSTeamPeriodResult(TeamMemDivisionResultTeam result){
        WSTeamPeriodResult wsTeamPeriodResult = new WSTeamPeriodResult();
        if(result != null){
            wsTeamPeriodResult.player = null;
            wsTeamPeriodResult.result = result.results;
            wsTeamPeriodResult.rank = result.rank;
            wsTeamPeriodResult.points = result.points;
            TeamCache teamCache = teamCacheMgr.getOrLoadTeamCache(result.teamID);
            if(teamCache != null) {
                wsTeamPeriodResult.count = mapDivisionResult.get(teamCache.division).mapResultTeam.size();
            } else {
                log.error("no team found for teamID="+result.teamID);
            }
        }
        return wsTeamPeriodResult;
    }

    public WSTeamTourResult toWSTeamTourResult(TeamMemDivisionTourResultTeam result){
        WSTeamTourResult wsTeamTourResult = new WSTeamTourResult();
        if(result != null){
            wsTeamTourResult.player = null;
            wsTeamTourResult.result = result.result;
            wsTeamTourResult.rank = result.rank;
            wsTeamTourResult.points = result.points;
            TeamCache teamCache = teamCacheMgr.getOrLoadTeamCache(result.teamID);
            if(teamCache != null) {
                wsTeamTourResult.count = mapDivisionTourResult.get(teamCache.division).mapResultTeam.size();
            } else {
                log.error("no team found for teamID="+result.teamID);
            }
        }
        return wsTeamTourResult;
    }

    public WSTeamTourResult toWSTeamTourResult(TeamMemTournamentPlayer result){
        if(result != null){
            // Get Team
            Team team = teamMgr.findTeamByID(result.teamID);
            if(team != null){
                // Build WSTeamPlayer
                WSTeamPlayer wsTeamPlayer = teamMgr.toWSTeamPlayer(result.playerID, team.isCaptain(result.playerID), false, result.memTour.group);
                // Build WSTeamTourResult
                WSTeamTourResult wsTeamTourResult = new WSTeamTourResult();
                wsTeamTourResult.player = wsTeamPlayer;
                wsTeamTourResult.result = result.result;
                wsTeamTourResult.rank = result.ranking;
                wsTeamTourResult.points = result.points;
                wsTeamTourResult.count = result.memTour.getNbPlayersForRanking();
                wsTeamTourResult.nbPlayedDeals = result.getNbPlayedDeals();
                wsTeamTourResult.nbDeals = result.memTour.getNbDeals();
                return wsTeamTourResult;
            } else {
                log.error("team not found for teamID="+result.teamID);
            }
        }
        return null;
    }


    /**
     * Resultats des équipes sur un tour pour une division
     * @param division
     * @return
     */
    public TeamMemDivisionTourResult getMemDivisionTourResult(String division) {
        return mapDivisionTourResult.get(division);
    }

    /**
     * Resultats des équipe sur la période pour une division
     * @param division
     * @return
     */
    public TeamMemDivisionResult getMemDivisionResult(String division) {
        return mapDivisionResult.get(division);
    }

    public List<WSTeamResult> getRankingForDivision(String division, int offset, int nbMax){
        List<WSTeamResult> wsRanking = new ArrayList<>();
        TeamMemDivisionResult divisionResult = getMemDivisionResult(division);
        if(divisionResult != null){
            List<TeamMemDivisionResultTeam> ranking = divisionResult.getRanking(offset, nbMax);
            for(TeamMemDivisionResultTeam result : ranking){
                wsRanking.add(toWSTeamResult(result));
            }
        } else {
            log.error("No result found for division="+division);
        }
        return wsRanking;
    }

    public WSTeamResult getRankingForTeamInDivision(String teamID, String division){
        TeamMemDivisionResult divisionResult = getMemDivisionResult(division);
        if(divisionResult != null){
            TeamMemDivisionResultTeam ranking = divisionResult.getRankingForTeam(teamID);
            return toWSTeamResult(ranking);
        } else {
            log.warn("No result found for team="+teamID+" in division="+division);
        }
        return null;
    }

    public List<WSTeamResult> getTourRankingForDivision(String division, int offset, int nbMax){
        List<WSTeamResult> wsRanking = new ArrayList<>();
        TeamMemDivisionTourResult divisionTourResult = getMemDivisionTourResult(division);
        if(divisionTourResult != null){
            List<TeamMemDivisionTourResultTeam> ranking = divisionTourResult.getRanking(offset, nbMax);
            for(TeamMemDivisionTourResultTeam result : ranking){
                wsRanking.add(toWSTeamResult(result));
            }
        } else {
            log.error("No result found for current tour and division="+division);
        }
        return wsRanking;
    }

    /**
     * Return the number of player on this tournament. The tournament is not finished.
     * @param tourID
     * @param listPlaFilter
     * @param onlyFinisher
     * @return
     */
    public int getNbPlayersOnTournament(String tourID, List<Long> listPlaFilter, boolean onlyFinisher) {
        int nbPlayer = 0;
        TeamMemTournament memTournament = getMemTournament(tourID);
        if (memTournament != null) {
            if (listPlaFilter != null && listPlaFilter.size() > 0) {
                for (TeamMemTournamentPlayer e : memTournament.mapTourPlayer.values()) {
                    if (!listPlaFilter.contains(e.playerID)) {
                        continue;
                    }
                    if (onlyFinisher) {
                        if (e.isPlayerFinish()) {
                            nbPlayer++;
                        }
                    } else {
                        if (e.hasPlayedOneDeal()) {
                            nbPlayer++;
                        }
                    }
                }
            } else {
                if (onlyFinisher) {
                    nbPlayer = memTournament.getNbPlayersWhoFinished();
                } else {
                    nbPlayer = memTournament.getNbPlayersForRanking();
                }
            }
        }
        return nbPlayer;
    }

    public WSTeamResult toWSTeamResult(TeamMemDivisionResultTeam teamResult){
        if(teamResult != null){
            TeamCache teamCache = teamCacheMgr.getOrLoadTeamCache(teamResult.teamID);
            if(teamCache != null){
                WSTeamResult result = new WSTeamResult();
                result.teamID = teamResult.teamID;
                result.name = teamCache.name;
                result.countryCode = teamCache.countryCode;
                result.rank = teamResult.rank;
                result.points = teamResult.points;
                result.trend = teamResult.trend;
                return result;
            }
        }
        return null;
    }

    public WSTeamResult toWSTeamResult(TeamMemDivisionTourResultTeam teamResult){
        if(teamResult != null){
            TeamCache teamCache = teamCacheMgr.getOrLoadTeamCache(teamResult.teamID);
            if(teamCache != null){
                WSTeamResult result = new WSTeamResult();
                result.teamID = teamResult.teamID;
                result.name = teamCache.name;
                result.countryCode = teamCache.countryCode;
                result.rank = teamResult.rank;
                result.points = teamResult.points;
                return result;
            }
        }
        return null;
    }

    /**
     * Add game result for the tournament in memory
     * @param game
     * @throws FBWSException
     */
    public void addResult(TeamGame game) throws FBWSException {
        if (game != null) {
            synchronized (getLockTour(game.getTournament().getIDStr())) {
                TeamMemTournament memTournament = getMemTournamentDivisionAndTourID(game.getTournament().getDivision(), game.getTournament().getIDStr());
                if (memTournament != null) {
                    // update tournament player result
                    TeamMemTournamentPlayer mtrPla = memTournament.addResult(game, true);
                    if (mtrPla == null) {
                        log.error("Failed to add result for player - game="+game);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                    mtrPla.currentDealIndex = -1;

                    // update team result ... on tour and on period after each deals played
                    TeamMemDivisionTourResult divisionTourResult = getMemDivisionTourResult(memTournament.division);
                    if (divisionTourResult != null) {
                        // update result for team on tour
                        TeamMemDivisionTourResultTeam divisionTourResultTeam = divisionTourResult.addPlayerTournamentResult(mtrPla, true);
                        if (divisionTourResultTeam != null) {
                            // update result for team on period
                            TeamMemDivisionResult divisionResult = getMemDivisionResult(memTournament.division);
                            divisionResult.addTourResultTeamForCurrentPeriodTour(divisionTourResultTeam, divisionTourResult, true);
                        }
                    } else {
                        log.error("No division result found for division="+memTournament.division);
                    }
                }
                else {
                    log.error("No memTournament found for tourID="+game.getTournament().getIDStr()+" - division="+game.getTournament().getDivision()+" - game="+game);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            }
        }
    }

    /**
     * Return tournamentPlayer for player (search over all division)
     * @param playerID
     * @return
     */
    public TeamMemTournamentPlayer getTournamentPlayer(long playerID) {
        for (String division : TourTeamMgr.DIVISION_TAB) {
            List<TeamMemTournament> listTour = listMemTournamentForDivision(division);
            for (TeamMemTournament e : listTour) {
                TeamMemTournamentPlayer tp = e.getTournamentPlayer(playerID);
                if (tp != null) {
                    return tp;
                }
            }
        }
        return null;
    }

    /**
     * Remove tournament from map
     * @param division
     * @param tourID
     */
    public void removeTournament(String division, String tourID) {
        Map<String, TeamMemTournament> mapTour = mapDivisionTournament.get(division);
        if (mapTour != null) {
            mapTour.remove(tourID);
        }
    }

    /**
     * Get tournament in progress for playerID
     * @param playerID
     * @return
     */
    public TeamMemTournament getTournamentInProgressForPlayer(long playerID) {
        Team team = teamMgr.getTeamForPlayer(playerID);
        if (team != null) {
            if (team.getDivision() != null && !team.getDivision().equals(TourTeamMgr.DIVISION_NO)) {
                TeamPlayer teamPlayer = team.getPlayer(playerID);
                if (teamPlayer != null && teamPlayer.getGroup() != null) {
                    // 1 tournament by period tour for each player group
                    TeamMemTournament memTournament = getMemTournamentForDivisionAndGroup(team.getDivision(), teamPlayer.getGroup());
                    if (memTournament != null) {
                        TeamMemTournamentPlayer tourPla = memTournament.getTournamentPlayer(playerID);
                        // If player has already finished the tournament
                        if (tourPla != null && tourPla.isPlayerFinish()) {
                            return null;
                        }
                        // If another player of the team already played this tournament
                        List<TeamMemTournamentPlayer> listCurrentResultsForTeam = getListMemTournamentPlayerForTeam(team.getIDStr());
                        for(TeamMemTournamentPlayer result : listCurrentResultsForTeam){
                            if(result.memTour != null && result.memTour.group != null){
                                if(result.memTour.group.equalsIgnoreCase(teamPlayer.getGroup()) && result.playerID != teamPlayer.getPlayerID()){
                                    return null;
                                }
                            }
                        }
                        return memTournament;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Load average performance minimum from configuration file
     * @return nb division updated
     */
    public int loadAveragePerformanceMinimumForAllDivisions() {
        int result = 0;
        for (String div : TourTeamMgr.DIVISION_TAB) {
            TeamMemDivisionResult memDivisionResult = getMemDivisionResult(div);
            if (memDivisionResult != null) {
                double defaultValue = 123.45;
                double value = tourMgr.getConfigAveragePerformanceMinimum(div, defaultValue);
                if (value != defaultValue) {
                    memDivisionResult.averagePerformanceMinimum = value;
                    result++;
                }
            }
        }
        return result;
    }

    /**
     * Return the total number of teams in divisions
     * @return
     */
    public int getNbTeamsInAllDivisions() {
        int value = 0;
        for (String division : TourTeamMgr.DIVISION_TAB) {
            value += getNbTeamsInDivision(division);
        }
        return value;
    }

    /**
     * Return nb teams in division
     * @param division
     * @return
     */
    public int getNbTeamsInDivision(String division) {
        TeamMemDivisionResult divisionResult = getMemDivisionResult(division);
        if (divisionResult != null) {
            return divisionResult.getNbTeams();
        }
        return 0;
    }

    /**
     * Reload average handicap value in memory for each team in this division
     * @param division
     * @return nb teams updated
     */
    public int reloadTeamAverageHandicapForDivision(String division) {
        int nbTeamReloaded = 0;
        if (tourMgr.getCurrentPeriod() != null) {
            TeamMemDivisionResult divisionResult = getMemDivisionResult(division);
            if (divisionResult != null) {
                List<Team> listTeams = teamMgr.getActiveTeamsForDivision(division, tourMgr.getCurrentPeriod().getID());
                log.warn("Division result nb teams="+divisionResult.getNbTeams()+" - list teams active size="+listTeams.size());
                for (Team t : listTeams) {
                    TeamMemDivisionResultTeam resultTeam = divisionResult.getTeamResult(t.getIDStr());
                    if (resultTeam != null) {
                        resultTeam.averageHandicap = t.getAverageHandicap();
                        nbTeamReloaded++;
                    }
                }
            }
        }
        return nbTeamReloaded;
    }
}
