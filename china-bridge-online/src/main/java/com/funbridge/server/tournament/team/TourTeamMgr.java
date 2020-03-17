package com.funbridge.server.tournament.team;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.PlayerHandicap;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.team.TeamMgr;
import com.funbridge.server.team.cache.TeamCache;
import com.funbridge.server.team.cache.TeamCacheMgr;
import com.funbridge.server.team.data.Team;
import com.funbridge.server.team.data.TeamPlayer;
import com.funbridge.server.tournament.DealGenerator;
import com.funbridge.server.tournament.IDealGeneratorCallback;
import com.funbridge.server.tournament.game.*;
import com.funbridge.server.tournament.generic.GameGroupMapping;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.team.data.*;
import com.funbridge.server.tournament.team.memory.*;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.game.WSGameView;
import com.funbridge.server.ws.game.WSTableGame;
import com.funbridge.server.ws.result.*;
import com.funbridge.server.ws.team.*;
import com.funbridge.server.ws.tournament.WSTableTournament;
import com.funbridge.server.ws.tournament.WSTournament;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.lock.LockWeakString;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by pserent on 08/11/2016.
 */
@Component(value="tourTeamMgr")
public class TourTeamMgr extends TournamentGenericMgr implements IDealGeneratorCallback, ITournamentMgr {
    @Resource(name="mongoTeamTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name = "presenceMgr")
    private PresenceMgr presenceMgr = null;
    @Resource(name="teamMgr")
    private TeamMgr teamMgr = null;
    @Resource(name="teamCacheMgr")
    private TeamCacheMgr teamCacheMgr = null;
    @Resource(name="playerMgr")
    private PlayerMgr playerMgr = null;
    @Resource(name="playerCacheMgr")
    private PlayerCacheMgr playerCacheMgr = null;

    private TourTeamMemoryMgr memoryMgr = null;
    private GameMgr gameMgr;
    private final String configName = "TEAM";
    public static final String PERIOD_ID_INDICE_SEPARATOR = "_";
    public static final String TOUR_ID_INDICE_SEPARATOR = "-";

    public static final String DIVISION_01 = "D01", DIVISION_02 = "D02", DIVISION_03 = "D03", DIVISION_04 = "D04", DIVISION_05 = "D05", DIVISION_06 ="D06", DIVISION_NO = "DNO";
    public static String[] DIVISION_TAB = new String[]{};
    public static final String GROUP_A = "A", GROUP_B = "B", GROUP_C = "C", GROUP_D = "D";
    public static String[] GROUP_TAB = new String[]{};

    private static SimpleDateFormat dateFormatPeriod = new SimpleDateFormat("yyyyMM");
    private static SimpleDateFormat dateFormatPeriodDev = new SimpleDateFormat("yyyyMMdd");
    private TeamPeriod currentPeriod;
    private long dateNextTour;
    private boolean tourChangeProcessing = false;
    private boolean periodChangeProcessing = false;
    private boolean enable = true;
    private boolean runProcessInProgress = false;
    private Scheduler scheduler;
    private DealGenerator dealGenerator = null;
    private LockWeakString lockCreateGame = new LockWeakString();
    private ExecutorService threadPoolPushMessage = null;

    @Override
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    /**
     * Build the deal ID using tourID and deal index
     * @param tourID
     * @param index
     * @return
     */
    public static String buildDealID(String tourID, int index) {
        return tourID+"-"+(index<10?"0":"")+index;
    }

    public static String buildPeriodTour(String periodID, int tour) {
        return periodID+"-"+tour;
    }

    /**
     * Extract tourID string from dealID
     * @param dealID
     * @return
     */
    public static String extractTourIDFromDealID(String dealID) {
        if (dealID != null && dealID.indexOf("-") >= 0) {
            return dealID.substring(0, dealID.indexOf("-"));
        }
        return null;
    }

    /**
     * Extract dealIndex from dealID
     * @param dealID
     * @return
     */
    public static int extractDealIndexFromDealID(String dealID) {
        if (dealID != null && dealID.indexOf("-") >= 0) {
            try {
                return Integer.parseInt(dealID.substring(dealID.indexOf("-")+1));
            } catch (Exception e) {
                ContextManager.getTourTeamMgr().getLogger().error("Failed to retrieve dealIndex from dealID=" + dealID, e);
            }
        }
        return -1;
    }

    /**
     * Extract TourID from periodTour
     * @param periodTour
     * @return
     */
    public static int extractTourIDFromPeriodTour(String periodTour) {
        if (periodTour != null && periodTour.indexOf("-") >= 0) {
            try {
                return Integer.parseInt(periodTour.substring(periodTour.indexOf("-")+1));
            } catch (Exception e) {
                ContextManager.getTourTeamMgr().getLogger().error("Failed to retrieve TourID from periodTour=" + periodTour, e);
            }
        }
        return -1;
    }

    /**
     * Extract PeriodID string from periodTour
     * @param periodTour
     * @return
     */
    public static String extractPeriodIDFromPeriodTour(String periodTour) {
        if (periodTour != null && periodTour.indexOf("-") >= 0) {
            return periodTour.substring(0, periodTour.indexOf("-"));
        }
        return null;
    }

    /**
     * Retrieve the timestamp of begin or end period for a periodID
     * @param periodID
     * @param begin
     * @return
     */
    public static long transformPeriodID2TS(String periodID, boolean begin, boolean devMode) {
        long tsValue = 0;
        if (periodID != null) {
            try {
                String temp = periodID;
                if (temp.contains(PERIOD_ID_INDICE_SEPARATOR)) {
                    temp = temp.substring(0, temp.indexOf(PERIOD_ID_INDICE_SEPARATOR));
                }
                Calendar cal = Calendar.getInstance();
                if (devMode) {
                    cal.setTimeInMillis(dateFormatPeriodDev.parse(temp).getTime());
                } else {
                    cal.setTimeInMillis(dateFormatPeriod.parse(temp).getTime());
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                }
                cal.set(Calendar.MILLISECOND, 0);
                if (begin) {
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                } else {
                    if (!devMode) {
                        cal.add(Calendar.MONTH, 1);
                        cal.add(Calendar.DAY_OF_MONTH, -1);
                    }
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                }
                return cal.getTimeInMillis();
            } catch (Exception e) {}
        }
        return tsValue;
    }

    @Override
    @PostConstruct
    public void init() {
        log.info("Init");
        threadPoolPushMessage = Executors.newFixedThreadPool(2);
        int nbDivision = getNbDivision();
        switch (nbDivision) {
            case 2: {
                DIVISION_TAB = new String[]{DIVISION_01, DIVISION_02};
                break;
            }
            case 3: {
                DIVISION_TAB = new String[]{DIVISION_01, DIVISION_02, DIVISION_03};
                break;
            }
            case 4: {
                DIVISION_TAB = new String[]{DIVISION_01, DIVISION_02, DIVISION_03, DIVISION_04};
                break;
            }
            case 5: {
                DIVISION_TAB = new String[]{DIVISION_01, DIVISION_02, DIVISION_03, DIVISION_04, DIVISION_05};
                break;
            }
            default: {
                DIVISION_TAB = new String[]{DIVISION_01, DIVISION_02, DIVISION_03, DIVISION_04, DIVISION_05, DIVISION_06};
                break;
            }
        }
        int nbGroup = teamMgr.getNbLeadPlayers();
        switch (nbGroup) {
            case 2: {
                GROUP_TAB = new String[]{GROUP_A, GROUP_B};
                break;
            }
            case 3: {
                GROUP_TAB = new String[]{GROUP_A, GROUP_B, GROUP_C};
                break;
            }
            default: {
                GROUP_TAB = new String[]{GROUP_A, GROUP_B, GROUP_C, GROUP_D};
                break;
            }
        }
        // generator of deal
        dealGenerator = new DealGenerator(this, getStringResolvEnvVariableValue("generatorParamFile", null));
    }

    @Override
    @PreDestroy
    public void destroy() {
        log.info("Init");
        if (gameMgr != null) {
            gameMgr.destroy();
        }
        if (memoryMgr != null) {
            memoryMgr.destroy();
        }
        // shutdown thread pool
        threadPoolPushMessage.shutdown();
        try {
            if (threadPoolPushMessage.awaitTermination(30, TimeUnit.SECONDS)) {
                threadPoolPushMessage.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPoolPushMessage.shutdownNow();
        }
    }

    /**
     * Read string value for parameter in name (tournament."+configName+".paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public String getConfigStringValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringValue("tournament." + configName + "." + paramName, defaultValue);
    }

    public String getStringResolvEnvVariableValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringResolvEnvVariableValue("tournament." + configName + "." + paramName, defaultValue);
    }

    /**
     * Read int value for parameter in name (tournament."+configName+".paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public int getConfigIntValue(String paramName, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("tournament." + configName + "." + paramName, defaultValue);
    }

    public boolean isDevMode() {
        return FBConfiguration.getInstance().getIntValue("general.devMode", 0) == 1;
    }

    public GameMgr getGameMgr() {
        return gameMgr;
    }

    @Override
    public Class<TeamGame> getGameEntity() {
        return TeamGame.class;
    }

    @Override
    public String getGameCollectionName() {
        return "team_game";
    }

    @Override
    public Class<TeamTournament> getTournamentEntity() {
        return TeamTournament.class;
    }

    @Override
    public Class<TeamTournamentPlayer> getTournamentPlayerEntity() {
        return TeamTournamentPlayer.class;
    }

    @Override
    public boolean finishTournament(Tournament tour) {
        return false; // Default implementation (not used)
    }

    @Override
    public TourTeamMemoryMgr getMemoryMgr() {
        return memoryMgr;
    }

    @Override
    public void checkTournamentMode(boolean isReplay, long playerId) throws FBWSException {
        if(!isReplay) checkPeriodTourValid();
    }

    /**
     * Read double value for parameter in name (tournament."+configName+".paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public double getConfigDoubleValue(String paramName, double defaultValue) {
        return FBConfiguration.getInstance().getDoubleValue("tournament." + configName + "." + paramName, defaultValue);
    }

    public double getConfigAveragePerformanceMinimum(String division, double defaultValue) {
        return getConfigDoubleValue("division."+division+".averagePerformanceMinimum", defaultValue);
    }

    /**
     * Return the file path used to backup a tournament
     * @param tourID
     * @param division
     * @param mkdir if true, create dir if not existing
     * @return
     */
    public String buildBackupFilePathForTournament(String tourID, String division, boolean mkdir) {
        String path = getStringResolvEnvVariableValue("backupMemoryPath", null);
        if (path != null) {
            path = FilenameUtils.concat(path, division);
            if (mkdir) {
                try {
                    File f = new File(path);
                    if (!f.exists()) {
                        f.mkdirs();
                    }
                } catch (Exception e) {
                    log.error("Exception to mkdir for path="+path, e);
                }
            }
            path = FilenameUtils.concat(path, tourID + ".json");
        } else {
            log.error("No path defined in configuration !");
        }
        return path;
    }

    public boolean isChampionshipStarted() {
        return getConfigIntValue("championshipStarted", 1) == 1;
    }

    public int getNbDivision() {
        return getConfigIntValue("nbDivision", 6);
    }

    @Override
    public void startUp() {
        log.info("Init");
        gameMgr = new GameMgr(this);
        memoryMgr = new TourTeamMemoryMgr(this);

        if (isChampionshipStarted()) {
            // init current period
            if (currentPeriod == null) {
                // get last period
                List<TeamPeriod> listPeriod = listPeriod(0, 1);
                if (listPeriod != null && listPeriod.size() > 0) {
                    currentPeriod = listPeriod.get(0);
                }

                // no period found current period
                initCurrentPeriod();

                // Load teams for current period and tour
                for (String division : DIVISION_TAB) {
                    List<Team> teamsForDivision = getTeamsCompleteForDivision(division);
                    List<String> listTeamID = new ArrayList<>();
                    for (Team team : teamsForDivision) {
                        listTeamID.add(team.getIDStr());
                    }
                    memoryMgr.getMemDivisionResult(division).loadTeamData(listTeamID);
                    memoryMgr.getMemDivisionTourResult(division).loadTeamData(listTeamID);
                }

                // load all data in memory
                memoryMgr.loadDataForPeriod(currentPeriod);
                // create tournament if necessary
                int nbTournamentCreated = createTournamentsForPeriodAndTour();
                // upgrade handicap and group for all teams (only if tournaments are created !)
                if (nbTournamentCreated > 0) {
                    upgradeHandicapAndGroupForAllTeams();
                }

                // change current tour if date end has expired
                while (currentPeriod.getCurrentTour() != null && currentPeriod.getCurrentTour().dateEnd <= System.currentTimeMillis()) {
                    processChangeTour();
                }
                if (currentPeriod.isAllTourFinished() && currentPeriod.getDateEnd() <= System.currentTimeMillis()) {
                    processChangePeriod();
                }
            } else {
                log.error("Current period is not null or config param resetEnable is not set on - currentPeriod=" + currentPeriod);
            }
        }

        try {
            // task thread for period management
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
            // Enable task - triger every 5 mn
            JobDetail jobProcess = JobBuilder.newJob(TourTeamProcessTask.class).withIdentity("processTask", "TourTeam").build();
            CronTrigger triggerProcess = TriggerBuilder.newTrigger().withIdentity("triggerProcessTask", "TourTeam").withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?")).build();
            Date dateNextJobProcess = scheduler.scheduleJob(jobProcess, triggerProcess);
            log.warn("Sheduled for job=" + jobProcess.getKey() + " run at="+dateNextJobProcess+" - cron expression=" + triggerProcess.getCronExpression() + " - next fire=" + triggerProcess.getNextFireTime());
        } catch (Exception e) {
            log.error("Exception to start change period task", e);
        }

    }

    /**
     * Return next TS for enable JOB
     * @return
     */
    public long getDateNextJobProcess() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerProcessTask", "TourTeam"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    /**
     * Create a periodID associated to this timestamp : format yyyyMM
     * @param ts
     * @return
     */
    public String buildPeriodID(long ts) {
        Calendar calCurrent = Calendar.getInstance();
        calCurrent.setTimeInMillis(ts);
        String periodID = dateFormatPeriod.format(calCurrent.getTime());

        if (isDevMode()) {
            // PERIOD 1 DAY
            periodID += String.format("%02d", calCurrent.get(Calendar.DAY_OF_MONTH));
        }
        return periodID;
    }

    /**
     * Return the date of beginning for the next period
     * @return
     */
    public long getDateNextPeriod() {
        if (!isChampionshipStarted()) {
            try {
                return Constantes.stringDateHour2Timestamp(getConfigStringValue("championshipDateStart", "01/02/2017 - 10:00:00"));
            } catch (ParseException e) {
                log.error("Failed to parse date", e);
            }
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            if (isDevMode()) {
                // period 1 day
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                return calendar.getTimeInMillis();
            } else {
                // period 1 month
                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                return calendar.getTimeInMillis();
            }
        }
        return 0;
    }

    /**
     * Return the date of beginning for the next period
     * @return
     */
    public long getDateEndNextPeriod() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 0);
        if (isDevMode()) {
            // period 1 day
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            return calendar.getTimeInMillis();
        } else {
            // period 1 month
            calendar.add(Calendar.MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            return calendar.getTimeInMillis();
        }
    }

    /**
     * Check if period is valid
     * @throws FBWSException
     */
    public void checkPeriodValid() throws FBWSException {
        checkTeamTournamentsEnable();
        if (currentPeriod == null) {
            log.error("Current period is null");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!currentPeriod.isPeriodValidForTS(System.currentTimeMillis())) {
            log.warn("Current period is not valid ... changing period is coming ...");
            throw new FBWSException(FBExceptionType.TOURNAMENT_TEAM_CHANGE_PROCESSING);
        }
    }

    /**
     * Check if period and current tour is valid
     * @throws FBWSException
     */
    public void checkPeriodTourValid() throws FBWSException {
        checkPeriodValid();
        if (currentPeriod.getCurrentTour() == null || !currentPeriod.getCurrentTour().isValidForTS(System.currentTimeMillis())) {
            log.warn("Current tour on period is not valid ... currentPeriod="+currentPeriod);
            throw new FBWSException(FBExceptionType.TOURNAMENT_TEAM_CHANGE_PROCESSING);
        }
    }

    public void checkTeamTournamentsEnable() throws FBWSException {
        if (!isEnable()) {
            throw new FBWSException(FBExceptionType.TOURNAMENT_TEAM_CLOSED);
        }
        if (isTourChangeProcessing() || isPeriodChangeProcessing()) {
            throw new FBWSException(FBExceptionType.TOURNAMENT_TEAM_CHANGE_PROCESSING);
        }
    }

    public boolean isEnable() {
        if (getConfigIntValue("enable", 1) != 1) {
            return false;
        }
        return enable;
    }

    public void setEnable(boolean value) {
        enable = value;
    }

    public boolean isTourChangeProcessing() {
        return tourChangeProcessing;
    }

    public void setTourChangeProcessing(boolean tourChangeProcessing) {
        this.tourChangeProcessing = tourChangeProcessing;
    }

    public boolean isPeriodChangeProcessing() {
        return periodChangeProcessing;
    }

    public void setPeriodChangeProcessing(boolean periodChangeProcessing) {
        this.periodChangeProcessing = periodChangeProcessing;
    }

    public TeamPeriod getCurrentPeriod() {
        return currentPeriod;
    }

    /**
     * get the existing period for current time or create a new one
     * @param
     * @return
     */
    public synchronized TeamPeriod createNewPeriod(String periodID) {
        try {
            TeamPeriod period = new TeamPeriod(periodID, isDevMode());
            // create period tours
            period.createTours(getConfigIntValue("nbTours", 5), getConfigIntValue("durationTourNbHours", 5*24), isDevMode());
            mongoTemplate.insert(period);
            if (log.isDebugEnabled()) {
                log.debug("Create and insert period="+period);
            }
            return period;
        } catch (Exception e) {
            log.error("Failed to create new period with ID="+periodID, e);
        }
        return null;
    }

    /**
     * Init the current period if this one is null
     */
    public void initCurrentPeriod() {
        if (currentPeriod == null) {
            log.warn("No current period => create a new one");
            String periodID = buildPeriodID(System.currentTimeMillis());
            // create new period for current timestamp
            currentPeriod = getPeriodForID(periodID);
            if (currentPeriod == null) {
                currentPeriod = createNewPeriod(periodID);
                if (currentPeriod == null) {
                    log.error("Failed to create period with ID=" + periodID);
                }
            }
        }
        log.warn("Current period=" + currentPeriod);
        // initialize current tour for this period
        currentPeriod.updateCurrentTour();
    }

    /**
     * Reload current period from DB
     */
    public void reloadCurrentPeriod() {
        String periodID = buildPeriodID(System.currentTimeMillis());
        log.warn("Before reload - currentPeriod="+currentPeriod);
        currentPeriod = getPeriodForID(periodID);
        if (currentPeriod != null) {
            currentPeriod.updateCurrentTour();
        }
        log.warn("After reload - currentPeriod="+currentPeriod);
    }

    public void resetCurrentPeriod() {
        log.warn("Before reset - currentPeriod="+currentPeriod);
        currentPeriod = null;
    }

    /**
     * Call to init first period. Championship must not be started !
     */
    public void initChampionship() {
        if (!isChampionshipStarted()) {
            resetCurrentPeriod();
            // init current period
            initCurrentPeriod();

            // integrate teams from DNO in divisions for complete teams
            List<Team> teamsIntegrated = processIntegrateTeamsInDivisions();
            log.warn("*** Nb teams integrated in divisions = " + teamsIntegrated != null ? teamsIntegrated.size() : null);
            String strListIntegrated = "";
            for (Team t : teamsIntegrated) {
                if (strListIntegrated.length() > 0) {
                    strListIntegrated += ", ";
                }
                strListIntegrated += t.getName() + " (" + t.getIDStr() + ")";
            }
            log.warn("*** Teams integrated=[" + strListIntegrated + "]");

            // Update field lastPeriodWithCompleteTeam for each team complete
            updateLastPeriodWithCompleteTeamForAllDivision();
            log.warn("*** Update teams lastPeriodWithCompleteTeam in divisions");

            // Load teams for current period
            log.warn("*** Load teams for current period in memory");
            for (String division : DIVISION_TAB) {
                List<Team> teamsForDivision = getTeamsCompleteForDivision(division);
                List<String> listTeamID = new ArrayList<>();
                for (Team team : teamsForDivision) {
                    listTeamID.add(team.getIDStr());
                }
                // load teams in divisionResult
                TeamMemDivisionResult divisionResult = memoryMgr.getMemDivisionResult(division);
                if (divisionResult != null) {
                    // remove data
                    divisionResult.clearData();
                    // load threshold
                    divisionResult.loadThresholdFromConfiguration();
                    // now add teams for division and init the handicap value
                    for (Team t : teamsForDivision) {
                        divisionResult.setTeamAverageHandicap(t.getIDStr(), t.getAverageHandicap());
                    }
                    divisionResult.computeRanking();
                }
                // load teams in divisionTourResult
                TeamMemDivisionTourResult tourResult = memoryMgr.getMemDivisionTourResult(division);
                if (tourResult != null) {
                    tourResult.loadTeamData(listTeamID);
                }

                log.warn("Nb teams loaded in memory division result for division=" + division + " nbTeams=" + listTeamID.size());

                // add notif championship started
                if (listTeamID.size() > 0) {
                    int nbPlayersNotif = teamMgr.createAndSetNotifGroupTeamStartChampionshipForDivision(listTeamID, currentPeriod.getDateEnd(), division);
                    log.warn("Notif for division "+division+" - nbPlayersNotif="+nbPlayersNotif+" - nbTeams="+listTeamID.size());
                }
            }

            // Create tournament for period
            int nbTournamentCreated = createTournamentsForPeriodAndTour();
            log.warn("*** Nb tournament created for period = " + nbTournamentCreated);
        }
    }
    /**
     * Is it time to change Tour or Period ?
     */
    public void runProcessForPeriod() {
        if (!runProcessInProgress) {
            runProcessInProgress = true;
            try {
                if (isChampionshipStarted()) {
                    try {
                        if (currentPeriod == null) {
                            initCurrentPeriod();
                        }
                        long currentTime = System.currentTimeMillis();
                        // time to finish current tour of period
                        if (currentPeriod.getCurrentTour() != null && currentPeriod.getCurrentTour().dateEnd < currentTime) {
                            processChangeTour();
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("No change tour for currentPeriod=" + currentPeriod);
                            }
                        }

                        // time to change period
                        if (currentPeriod.isAllTourFinished() && currentPeriod.getDateEnd() < System.currentTimeMillis()) {
                            processChangePeriod();
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("No change period for currentPeriod=" + currentPeriod);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Exception to run process for period=" + currentPeriod, e);
                        ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to run process for period="+currentPeriod, e.getMessage(), null);
                    }
                } else {
                    log.warn("Championship is not started !");
                }
            } catch (Exception e) {
                log.error("Failed to runProcessForPeriod", e);
                ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to runProcessForPeriod", e.getMessage(), null);
            }
            runProcessInProgress = false;
        } else {
            log.error("Run process already in progress !!");
            ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for runProcessForPeriod", null, null);
        }
    }

    /**
     * Do the job to finish current period (check current period date) or create new period for the next
     * @return
     */
    public boolean processChangePeriod() {
        boolean result = false;
        if (periodChangeProcessing) {
            log.error("Change on period already in process ...");
        } else {
            if (isChampionshipStarted()) {
                // block access
                periodChangeProcessing = true;
                if ((currentPeriod != null) && (isDevMode() || currentPeriod.getDateEnd() < System.currentTimeMillis())) {
                    try {
                        // TODO close and finish all tour not yet finished !

                        // wait 5 seconds
                        Thread.sleep(5000);

                        TourTeamThreadPushMessage threadPushMessage = new TourTeamThreadPushMessage("team_start_period");

                        //********************************************
                        // START ETAPES DU PROCESS A EXECUTER DANS CET ORDRE

                        // 1 - Process up and down in divisions
                        log.warn("*** Process UP & DOWN in divisions");
                        for (String division : DIVISION_TAB) {
                            TeamMemDivisionResult divisionResult = memoryMgr.getMemDivisionResult(division);
                            divisionResult.computeRanking();

                            List<TeamPeriodTeam> listPeriodTeam = new ArrayList<>();
                            // list of teams to go up
                            List<String> listTeamIDUp = new ArrayList<>();
                            // list of teams to go down
                            List<String> listTeamIDDown = new ArrayList<>();
                            // list of teams to maintain
                            List<String> listTeamIDMaintain = new ArrayList<>();

                            // incompleted teams
                            List<Team> teamsBecomeIncomplete = teamMgr.listIncompleteTeamsForDivision(division, currentPeriod.getID());
                            List<String> listTeamIDBecomeIncomplete = new ArrayList<>();
                            String strIncompleteTeams = "";
                            for (Team e : teamsBecomeIncomplete) {
                                listTeamIDBecomeIncomplete.add(e.getIDStr());
                                if (strIncompleteTeams.length() > 0) {
                                    strIncompleteTeams += ", ";
                                }
                                strIncompleteTeams += e.getName() + " (" + e.getIDStr() + ")";
                            }
                            log.warn("For division "+division+" nb incomplete teams="+teamsBecomeIncomplete.size()+" - teams set in DNO : "+strIncompleteTeams);

                            // loop on teams to insert it on list up or list down
                            List<TeamMemDivisionResultTeam> listResultTeam = divisionResult.listDivisionResultTeam();
                            for (TeamMemDivisionResultTeam e : listResultTeam) {
                                if (!listTeamIDBecomeIncomplete.contains(e.teamID)) {
                                    if (e.trend > 0) {
                                        listTeamIDUp.add(e.teamID);
                                    } else if (e.trend < 0) {
                                        listTeamIDDown.add(e.teamID);
                                    } else {
                                        listTeamIDMaintain.add(e.teamID);
                                    }
                                }

                                TeamPeriodTeam periodTeam = new TeamPeriodTeam();
                                periodTeam.setTeamID(e.teamID);
                                periodTeam.setDivision(division);
                                periodTeam.setEvolution(e.trend);
                                periodTeam.setPeriodID(currentPeriod.getID());
                                periodTeam.setPoints(e.points);
                                periodTeam.setRank(e.rank);
                                periodTeam.setNbTeams(divisionResult.getNbTeams());
                                periodTeam.setResult(e.resultsPeriod);
                                listPeriodTeam.add(periodTeam);
                            }
                            mongoTemplate.insertAll(listPeriodTeam);
                            log.warn("Nb results for division=" + division + " on period=" + currentPeriod.getID() + " - nb teams=" + listPeriodTeam.size());

                            if (listTeamIDDown.size() > 0) {
                                String nextDivision = computeDivisionEvolution(division, -1);
                                teamMgr.setTeamsDivision(listTeamIDDown, nextDivision);
                                log.warn("Nb teams DOWN from division=" + division + " to division=" + nextDivision + " - nbTeamsUpdated=" + listTeamIDDown.size() + " (listTeamIDDown size=" + listTeamIDDown.size() + ")");

                                teamCacheMgr.updateTeamsDivision(listTeamIDDown, nextDivision);
                                // send notif
                                int nbPlayerNotif = teamMgr.createAndSetNotifGroupPeriodEnd(nextDivision, division, currentPeriod.getID(), -1, getDateEndNextPeriod(), listTeamIDDown);
                                log.warn("Set notif group DOWN for nbPlayerNotif=" + nbPlayerNotif);
                            }
                            if (listTeamIDUp.size() > 0) {
                                String nextDivision = computeDivisionEvolution(division, 1);
                                teamMgr.setTeamsDivision(listTeamIDUp, nextDivision);
                                log.warn("Nb teams UP from division=" + division + " to division=" + nextDivision + " - nbTeamsUpdated=" + listTeamIDUp.size() + " (listTeamIDUp size=" + listTeamIDUp.size() + ")");

                                teamCacheMgr.updateTeamsDivision(listTeamIDUp, nextDivision);
                                // send notif
                                int nbPlayerNotif = teamMgr.createAndSetNotifGroupPeriodEnd(nextDivision, division, currentPeriod.getID(), 1, getDateEndNextPeriod(), listTeamIDUp);
                                log.warn("Set notif group UP for nbPlayerNotif=" + nbPlayerNotif);
                            }
                            if (listTeamIDMaintain.size() > 0) {
                                // send notif
                                int nbPlayerNotif = teamMgr.createAndSetNotifGroupPeriodEnd(division, division, currentPeriod.getID(), 0, getDateEndNextPeriod(), listTeamIDMaintain);
                                log.warn("Set notif group MAINTAIN for nbPlayerNotif=" + nbPlayerNotif);
                            }
                            if (listTeamIDBecomeIncomplete.size() > 0) {
                                teamMgr.setTeamsDivision(listTeamIDBecomeIncomplete, DIVISION_NO);
                                log.warn("Nb teams INCOMPLETE set to DNO from division=" + division + " - nbTeamsUpdated=" + listTeamIDBecomeIncomplete.size() + " (listTeamIDIncompleted size=" + listTeamIDBecomeIncomplete.size() + ")");
                                teamCacheMgr.updateTeamsDivision(listTeamIDBecomeIncomplete, DIVISION_NO);
                                // send notif
                                int nbPlayerNotif = teamMgr.createAndSetNotifGroupTeamBecomeIncompleteEndPeriod(listTeamIDBecomeIncomplete, getDateEndNextPeriod());
                                log.warn("Set notif group exit division for INCOMPLETE teams for nbPlayerNotif=" + nbPlayerNotif);
                            }
                        }

                        // 2 - set period finish & update in DB
                        currentPeriod.setFinished(true);
                        mongoTemplate.save(currentPeriod);
                        log.warn("*** Update period finished for currentPeriod = " + currentPeriod);

                        // 3 -init the new period
                        String periodID = buildPeriodID(System.currentTimeMillis());
                        TeamPeriod existingPeriod = getPeriodForID(periodID);
                        if (existingPeriod == null) {
                            currentPeriod = createNewPeriod(periodID);
                        } else {
                            //  if devMode, add indice to value _XXX
                            if (isDevMode()) {
                                if (existingPeriod.getID().contains(PERIOD_ID_INDICE_SEPARATOR)) {
                                    int indice = Integer.parseInt(existingPeriod.getID().substring(existingPeriod.getID().indexOf(PERIOD_ID_INDICE_SEPARATOR) + 1));
                                    periodID += PERIOD_ID_INDICE_SEPARATOR + String.format("%03d", indice + 1);
                                } else {
                                    periodID += PERIOD_ID_INDICE_SEPARATOR + "001";
                                }
                                currentPeriod = createNewPeriod(periodID);
                            } else {
                                throw new Exception("A period already existing with this ID=" + periodID);
                            }
                        }
                        if (currentPeriod == null) {
                            throw new Exception("Failed to create new period with ID=" + periodID);
                        }
                        currentPeriod.updateCurrentTour();
                        log.warn("*** Create new current period = " + currentPeriod);

                        // 4 - integrate new team from DNO in divisions for complete teams
                        List<Team> teamsIntegrated = processIntegrateTeamsInDivisions();
                        log.warn("*** Nb teams integrated in divisions = " + teamsIntegrated != null ? teamsIntegrated.size() : null);
                        String strListIntegrated = "";
                        for (Team t : teamsIntegrated) {
                            if (strListIntegrated.length() > 0) {
                                strListIntegrated += ", ";
                            }
                            strListIntegrated += t.getName() + " (" + t.getIDStr() + ")";
                        }
                        log.warn("*** Teams integrated=[" + strListIntegrated + "]");

                        // 5 - Update field lastPeriodWithCompleteTeam for each team complete
                        updateLastPeriodWithCompleteTeamForAllDivision();
                        log.warn("*** Update teams lastPeriodWithCompleteTeam in divisions");

                        // 6 - Load teams for current period
                        log.warn("*** Load teams for current period in memory");
                        for (String division : DIVISION_TAB) {
                            List<Team> teamsForDivision = getTeamsCompleteForDivision(division);
                            List<String> listTeamID = new ArrayList<>();
                            for (Team team : teamsForDivision) {
                                listTeamID.add(team.getIDStr());
                                threadPushMessage.addPlayers(team.getListPlayerID());
                                teamCacheMgr.updateTeamData(team);
                            }
                            // load teams in divisionResult
                            TeamMemDivisionResult divisionResult = memoryMgr.getMemDivisionResult(division);
                            if (divisionResult != null) {
                                // remove data
                                divisionResult.clearData();
                                // now add teams for division and init the handicap value
                                for (Team t : teamsForDivision) {
                                    divisionResult.setTeamAverageHandicap(t.getIDStr(), t.getAverageHandicap());
                                }

                                // compute ranking for period
                                divisionResult.computeRanking();
                            }
                            // load teams in divisionTourResult
                            TeamMemDivisionTourResult tourResult = memoryMgr.getMemDivisionTourResult(division);
                            if (tourResult != null) {
                                tourResult.loadTeamData(listTeamID);
                            }
                            int nbPlayerNotif = teamMgr.createAndSetNotifGroupStartPeriod(listTeamID, currentPeriod.getDateEnd());
                            log.warn("Nb teams loaded in memory division result for division=" + division + " nbTeams=" + listTeamID.size()+" - nbPlayerNotif="+nbPlayerNotif);
                        }

                        // 7 - create tournament for period
                        int nbTournamentCreated = createTournamentsForPeriodAndTour();
                        log.warn("*** Nb tournament created for period = " + nbTournamentCreated);

                        // FIN
                        // ****************************

                        // set all incomplete teams to divison NO
                        List<Team> incompleteTeamsWithDivision = teamMgr.listIncompleteTeamsWithDivision(null);
                        String strListIncompleteTeamsWithDivision = "";
                        for (Team e : incompleteTeamsWithDivision) {
                            e.setDivision(DIVISION_NO);
                            if (strListIncompleteTeamsWithDivision.length() > 0) {
                                strListIncompleteTeamsWithDivision += ", ";
                            }
                            strListIncompleteTeamsWithDivision += e.getName() + " (" + e.getIDStr() + ")";
                            teamCacheMgr.updateTeamData(e);
                            mongoTemplate.save(e);
                        }
                        log.warn("Nb incomplete teams with division : "+incompleteTeamsWithDivision.size()+" - teams : "+strListIncompleteTeamsWithDivision);

                        // Delete incomplete teams for many period
                        int nbPeriodBeforeDeleteTeams = getConfigIntValue("nbPeriodBeforeDeleteTeams", 3);
                        TeamPeriod periodForDelete = getPreviousPeriod(nbPeriodBeforeDeleteTeams);
                        log.warn("*** Period used to remove incomplete or inactive teams = " + periodForDelete);
                        if (periodForDelete != null) {
                            int nbTeamsDeleted = teamMgr.deleteInactiveIncompleteTeams(periodForDelete.getID());
                            log.warn("Nb teams inactive or incomplete deleted = " + nbTeamsDeleted);
                        }

                        // notif for incomplete teams
                        List<Team> incompleteTeams = teamMgr.listIncompleteTeamsForDivision(DIVISION_NO, null);
                        List<String> listIncompleteTeamID = new ArrayList<>();
                        for (Team e : incompleteTeams) {
                            if (e.getNbPlayers() > 0) {
                                listIncompleteTeamID.add(e.getIDStr());
                            }
                        }
                        int nbPlayerNotifTeamIncompleteWarning = teamMgr.createAndSetNotifGroupTeamIncompleteWarning(listIncompleteTeamID, currentPeriod.getDateEnd());
                        log.warn("Nb teams incomplete="+listIncompleteTeamID.size()+" - set notif group for nbPlayerNotif="+nbPlayerNotifTeamIncompleteWarning);

                        // start thread to push message for team
                        threadPoolPushMessage.execute(threadPushMessage);

                        result = true;
                    } catch (Exception e) {
                        log.error("Exception to change period ... ", e);
                    }
                } else {
                    log.error("Current period is nul or it is always valid for current time : currentPeriod=" + currentPeriod);
                }
                // open access to team tournaments
                periodChangeProcessing = false;
            }
        }
        return result;
    }

    /**
     * Change current tour : finish all tournaments and set next tour.
     */
    public void processChangeTour() {
        if (tourChangeProcessing) {
            log.error("Change on tour already in process ...");
        } else {
            if (isChampionshipStarted()) {
                // block access
                tourChangeProcessing = true;
                if (currentPeriod != null) {
                    if (currentPeriod.getCurrentTour() != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Start change tour for currentPeriod=" + currentPeriod);
                        }
                        try {
                            boolean previousTourFinishedIsLast = false;
                            TourTeamThreadPushMessage threadPushMessage = null;
                            //**********************************
                            // Finish tournaments
                            if (!currentPeriod.getCurrentTour().finished) {
                                log.warn("Wait 10s ...");
                                Thread.sleep(10 * 1000);

                                // save games from session
                                List<TeamGame> listGameInSession = ContextManager.getPresenceMgr().getListGameInSessionsForCategory(Constantes.TOURNAMENT_CATEGORY_TEAM, true);
                                log.warn("Nb games from session to update : " + listGameInSession.size());
                                updateListGameDB(listGameInSession);

                                // finish tournaments for this tour
                                finishTournamentForPeriodAndTour();
                                // set tour finish
                                currentPeriod.getCurrentTour().finished = true;
                                mongoTemplate.save(currentPeriod);
                                log.warn("After finish tournament and set tour finished - currentPeriod=" + currentPeriod);
                                previousTourFinishedIsLast = currentPeriod.getCurrentTour().index == currentPeriod.getNbTours();
                            } else {
                                log.error("Current tour is already finished ! - currentPeriod=" + currentPeriod);
                            }
                            //**********************************
                            // change update current tour
                            int tourIndexBeforeUpdate = currentPeriod.getCurrentTourIndex();
                            currentPeriod.updateCurrentTour();
                            int tourIndexAfterUpdate = currentPeriod.getCurrentTourIndex();

                            // Send notif end tour
                            createAndSetNotifEndTour(tourIndexBeforeUpdate);

                            // Load new tour
                            boolean tourHasChanged = tourIndexAfterUpdate != -1 && tourIndexAfterUpdate != tourIndexBeforeUpdate;
                            log.warn("After update tour tourHasChanged=" + tourHasChanged + " - currentPeriod=" + currentPeriod+" - previousTourFinishedIsLast="+previousTourFinishedIsLast);
                            if (currentPeriod.getCurrentTour() != null && tourHasChanged) {
                                List<String> listTeamTrendLimitUp = null;
                                List<String> listTeamTrendLimitDown = null;
                                if (currentPeriod.getCurrentTourIndex() == currentPeriod.getNbTours()) {
                                    listTeamTrendLimitUp = new ArrayList<>();
                                    listTeamTrendLimitDown = new ArrayList<>();
                                }
                                if (currentPeriod.getCurrentTourIndex() > 1) {
                                    threadPushMessage = new TourTeamThreadPushMessage("team_start_period_tour");
                                }
                                // load teams for current periodTour
                                for (String division : DIVISION_TAB) {
                                    List<Team> teamsForDivision = getTeamsCompleteForDivision(division);
                                    List<String> listTeamID = new ArrayList<>();
                                    TeamMemDivisionResult divisionResult = memoryMgr.getMemDivisionResult(division);
                                    for (Team team : teamsForDivision) {
                                        listTeamID.add(team.getIDStr());
                                        if (threadPushMessage != null) {
                                            // players not substitute
                                            threadPushMessage.addPlayers(team.getListPlayerID(false));
                                        }
                                        if (divisionResult != null && listTeamTrendLimitDown != null && listTeamTrendLimitUp != null && divisionResult.getNbTeams() > 0) {
                                            TeamMemDivisionResultTeam resultTeam = divisionResult.getTeamResult(team.getIDStr());
                                            if (resultTeam != null) {
                                                double rankPercent = resultTeam.getRankPercent(divisionResult.getNbTeams());
                                                if (!division.equals(DIVISION_06) && rankPercent >= 20 && rankPercent <= 40) {
                                                    listTeamTrendLimitDown.add(team.getIDStr());
                                                }
                                                else if (!division.equals(DIVISION_01) && rankPercent >= 60 && rankPercent <= 80) {
                                                    listTeamTrendLimitUp.add(team.getIDStr());
                                                }
                                            }
                                        }
                                    }
                                    memoryMgr.getMemDivisionTourResult(division).loadTeamData(listTeamID);
                                    log.warn("For division=" + division + " nb teams loaded in memory division tour result=" + listTeamID.size());
                                }
                                if (listTeamTrendLimitDown != null && listTeamTrendLimitDown.size() > 0) {
                                    int nbPlayerNotif = teamMgr.createAndSetNotifGroupTeamTrendLimit(listTeamTrendLimitDown, currentPeriod.getCurrentTour().dateEnd, -1);
                                    log.warn("Set notif group trend limit down for nbPlayerNotif=" + nbPlayerNotif);
                                }
                                if (listTeamTrendLimitUp != null && listTeamTrendLimitUp.size() > 0) {
                                    int nbPlayerNotif = teamMgr.createAndSetNotifGroupTeamTrendLimit(listTeamTrendLimitUp, currentPeriod.getCurrentTour().dateEnd, 1);
                                    log.warn("Set notif group trend limit up for nbPlayerNotif=" + nbPlayerNotif);
                                }
                                //**********************************
                                // create tournaments for current periodTour
                                createTournamentsForPeriodAndTour();
                                //**********************************
                                // upgrade handicap and group for all teams
                                upgradeHandicapAndGroupForAllTeams();
                            } else if (log.isDebugEnabled()) {
                                log.debug("All tour finished for current period, no need to create tournaments");
                            }

                            if (previousTourFinishedIsLast) {
                                List<Team> incompleteTeams = teamMgr.listIncompleteTeamsWithDivision(currentPeriod.getID());
                                if (incompleteTeams.size() > 0) {
                                    List<String> listTeamID = new ArrayList<>();
                                    for (Team team : incompleteTeams) {
                                        listTeamID.add(team.getIDStr());
                                    }
                                    int nbPlayerNotif = teamMgr.createAndSetNotifGroupTeamIncompleteEndLastTour(listTeamID, currentPeriod.getDateEnd());
                                    log.warn("Set notif group Incomplete Team for nbPlayerNotif=" + nbPlayerNotif+" - nb team="+listTeamID.size());
                                } else {
                                    log.warn("None incomplete teams");
                                }
                            }

                            if (threadPushMessage != null) {
                                // start thread to push message for team
                                threadPoolPushMessage.execute(threadPushMessage);
                            }
                        } catch (Exception e) {
                            log.error("Exception to perform change tour on currentPeriod=" + currentPeriod, e);
                        }

                    } else {
                        log.error("Current tour in null on currentPeriod=" + currentPeriod);
                    }
                } else {
                    log.error("Current period is null !");
                }
                // open access to team tournaments
                tourChangeProcessing = false;
            }
        }
    }

    /**
     * Create a notif on end tour
     * @param tourIndexBeforeUpdate
     * @return
     */
    public void createAndSetNotifEndTour(int tourIndexBeforeUpdate) {
        // load teams for current periodTour
        for (String division : DIVISION_TAB) {
            int nbPlayerNotif = 0;
            TeamMemDivisionTourResult divisionTourResult = memoryMgr.getMemDivisionTourResult(division);
            if (divisionTourResult != null) {
                divisionTourResult.computeRanking();
                TeamMemDivisionResult divisionResult = memoryMgr.getMemDivisionResult(division);
                if (divisionResult != null) {
                    divisionResult.computeRanking();
                    for (TeamMemDivisionTourResultTeam tourResultTeam : divisionTourResult.listTeamResults()) {
                        if (tourResultTeam != null) {
                            TeamCache team = teamCacheMgr.getOrLoadTeamCache(tourResultTeam.teamID);
                            if (team != null) {

                                String templateName = "";

                                // Global parameters
                                Map<String, String> templateParameters = new HashMap<>();
                                templateParameters.put("TOUR_INDEX", String.valueOf(tourIndexBeforeUpdate));
                                if (team != null) {
                                    templateParameters.put("TEAM_NAME", team.name);
                                } else {
                                    templateParameters.put("TEAM_NAME", "");
                                }
                                templateParameters.put("TOUR_RANK_TEAM", MessageNotifMgr.VARIABLE_NUMBER_START + tourResultTeam.rank + MessageNotifMgr.VARIABLE_END);
                                templateParameters.put("TOUR_NB_TEAM", MessageNotifMgr.VARIABLE_NUMBER_START + divisionTourResult.getNbTeam() + MessageNotifMgr.VARIABLE_END);
                                templateParameters.put("TOUR_RESULT_TEAM", (int) tourResultTeam.result + " IMP");

                                // First tour
                                if (tourIndexBeforeUpdate == 1) {
                                    templateName = "teamTourFinishFirstTour";

                                    templateParameters.put("NEXT_TOUR_INDEX", String.valueOf(currentPeriod.getCurrentTour().index));
                                }
                                // Not first tour and before last tour
                                else if (tourIndexBeforeUpdate < currentPeriod.getNbTours()) {
                                    templateName = "teamTourFinishOtherTour";

                                    templateParameters.put("NEXT_TOUR_INDEX", String.valueOf(currentPeriod.getCurrentTour().index));

                                    // Result team for period
                                    TeamMemDivisionResultTeam resultTeam = divisionResult.getTeamResult(tourResultTeam.teamID);
                                    fillTourTeamFinishPeriodParam(templateParameters, divisionResult, resultTeam);
                                }
                                // Last tour
                                else {
                                    templateName = "teamTourFinishLastTour";

                                    // Result team for period
                                    TeamMemDivisionResultTeam resultTeam = divisionResult.getTeamResult(tourResultTeam.teamID);
                                    fillTourTeamFinishPeriodParam(templateParameters, divisionResult, resultTeam);

                                    // Podium for period
                                    List<TeamMemDivisionResultTeam> divisionResultPodium = divisionResult.getRanking(0, 3);
                                    for (int i = 0; i < 3; i++) {
                                        TeamCache podiumTeam = teamCacheMgr.getOrLoadTeamCache(divisionResultPodium.get(i).teamID);
                                        if (podiumTeam != null) {
                                            templateParameters.put("TEAM" + i, podiumTeam.name);
                                            templateParameters.put("COUNTRY_TEAM" + i, podiumTeam.countryCode);
                                        }
                                    }
                                }

                                for (Long playerID : team.players) {
                                    String playerTemplateName = templateName;
                                    Map<String, String> playerTemplateParameters = new HashMap<>(templateParameters);
                                    TeamMemTournamentPlayer tournamentPlayer = tourResultTeam.getResultForPlayer(playerID);
                                    if (tournamentPlayer != null) {
                                        playerTemplateName += "WithPlayer";

                                        playerTemplateParameters.put("TOUR_RANK_PLAYER", MessageNotifMgr.VARIABLE_NUMBER_START + tournamentPlayer.ranking + MessageNotifMgr.VARIABLE_END);
                                        playerTemplateParameters.put("TOUR_NB_PLAYER", MessageNotifMgr.VARIABLE_NUMBER_START + tournamentPlayer.memTour.getNbPlayers() + MessageNotifMgr.VARIABLE_END);
                                        playerTemplateParameters.put("TOUR_RESULT_PLAYER", (int) tournamentPlayer.result + " IMP");
                                        playerTemplateParameters.put("GROUP", tournamentPlayer.memTour.group);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            log.warn("For division="+division+" send notif end tour for nbPlayerNotif="+nbPlayerNotif);
        }
    }

    private void fillTourTeamFinishPeriodParam(Map<String, String> templateParameters, TeamMemDivisionResult divisionResult, TeamMemDivisionResultTeam resultTeam) {
        templateParameters.put("PERIOD_RANK_TEAM", String.valueOf(resultTeam.rank));
        templateParameters.put("PERIOD_NB_TEAM", String.valueOf(divisionResult.getNbTeams()));
        String division = "0";
        if (StringUtils.isNotBlank(divisionResult.division)) {
            division = divisionResult.division.substring(divisionResult.division.length()-1);
        }
        templateParameters.put("DIVISION", division);
    }

    /**
     * Simulate teams integration and return map with list team for each division
     * @return
     */
    public Map<String, List<Team>> simulationIntegrationTeamsInDivision() {
        Map<String, List<Team>> mapDivisionTeams = new HashMap<>();
        List<Team> listTeamsDNO = teamMgr.listTeamReadyToIntegrateDivisions();
        Collections.sort(listTeamsDNO, new Comparator<Team>() {
            @Override
            public int compare(Team o1, Team o2) {
                // sort with at first the biggest average handicap
                return -Double.compare(o1.getAverageHandicap(), o2.getAverageHandicap());
            }
        });
        log.warn("Nb teams to integrate in division : "+listTeamsDNO.size());
        if (listTeamsDNO.size() > 0) {
            // first integration => same number of teams in each division
            if (memoryMgr.getNbTeamsInAllDivisions() == 0) {
                log.warn("No teams in division => first integration !");
                int nbTeamByDivision = (listTeamsDNO.size() / DIVISION_TAB.length);
                if (listTeamsDNO.size() % DIVISION_TAB.length != 0) {
                    nbTeamByDivision++;
                }
                // start with the lowest division
                String currentDivision = DIVISION_TAB[DIVISION_TAB.length - 1];
                int nbTeamForCurrentDivision = 0;
                log.warn("Start with currentDivision=" + currentDivision);
                List<Team> listTeamDivision = new ArrayList<>();
                mapDivisionTeams.put(currentDivision, listTeamDivision);
                for (Team t : listTeamsDNO) {
                    listTeamDivision.add(t);
                    if (nbTeamForCurrentDivision == 0) {
                        log.warn("first team in division team="+t+" - average performance="+t.getAverageHandicap());
                    }
                    nbTeamForCurrentDivision++;
                    if (nbTeamForCurrentDivision >= nbTeamByDivision) {
                        // change division
                        log.warn("Change division - nb team added in " + currentDivision + "=" + nbTeamForCurrentDivision);
                        if (currentDivision.equals(DIVISION_01)) {
                            log.warn("Current division is D01 => no division change !!");
                        } else {
                            currentDivision = computeDivisionEvolution(currentDivision, 1);
                            nbTeamForCurrentDivision = 0;
                            log.warn("Change division - now currentDivision=" + currentDivision);
                            listTeamDivision = new ArrayList<>();
                            mapDivisionTeams.put(currentDivision, listTeamDivision);
                        }
                    }
                }
            }
            // else => integration according performance average
            else {
                int nbTeamByDivision = (listTeamsDNO.size() / DIVISION_TAB.length);
                if (listTeamsDNO.size() % DIVISION_TAB.length != 0) {
                    nbTeamByDivision++;
                }
                log.warn("Nb team by division = "+nbTeamByDivision);
                // start with the lowest division
                String currentDivision = DIVISION_TAB[DIVISION_TAB.length - 1];
                int nbTeamAdded = 0;
                log.warn("Start with currentDivision=" + currentDivision);
                List<Team> listTeamDivision = new ArrayList<>();
                mapDivisionTeams.put(currentDivision, listTeamDivision);
                for (Team t : listTeamsDNO) {
                    listTeamDivision.add(t);
                    nbTeamAdded++;
                    if (nbTeamAdded >= nbTeamByDivision) {
                        // change division
                        log.warn("Change division - nb team added in " + currentDivision + "=" + nbTeamAdded);
                        if (currentDivision.equals(DIVISION_01)) {
                            log.warn("Current division is D01 => no division change !!");
                        } else {
                            currentDivision = computeDivisionEvolution(currentDivision, 1);
                            nbTeamAdded = 0;
                            log.warn("Change division - now currentDivision=" + currentDivision);
                            listTeamDivision = new ArrayList<>();
                            mapDivisionTeams.put(currentDivision, listTeamDivision);
                        }
                    }
                }
            }
        }
        return mapDivisionTeams;
    }

    /**
     * List teams ready to integrate division and set in divisions according to average performance. Upgrade handicap and group for team.
     * @return teams integrated
     */
    public List<Team> processIntegrateTeamsInDivisions() {
        List<Team> listTeamsDNO = null;
        if (currentPeriod != null) {
            // get teams ready
            listTeamsDNO = teamMgr.listTeamReadyToIntegrateDivisions();
            // compute handicap and group
            for (Team t : listTeamsDNO) {
                upgradeHandicapAndGroupForTeam(t, false);
            }
            // sort by handicap ASC (at first teams with biggest handicap)
            Collections.sort(listTeamsDNO, new Comparator<Team>() {
                @Override
                public int compare(Team o1, Team o2) {
                    // sort with at first the biggest average handicap
                    return -Double.compare(o1.getAverageHandicap(), o2.getAverageHandicap());
                }
            });
            log.warn("Nb teams to integrate in division : "+listTeamsDNO.size());

            if (listTeamsDNO.size() > 0) {
                // first integration => same number of teams in each division
                if (memoryMgr.getNbTeamsInAllDivisions() == 0) {
                    log.warn("No teams in division => first integration !");
                    int nbTeamByDivision = (listTeamsDNO.size() / DIVISION_TAB.length);
                    if (listTeamsDNO.size() % DIVISION_TAB.length != 0) {
                        nbTeamByDivision++;
                    }
                    // start with the lowest division
                    String currentDivision = DIVISION_TAB[DIVISION_TAB.length - 1];
                    int nbTeamForCurrentDivision = 0;
                    log.warn("Start with currentDivision=" + currentDivision);
                    for (Team t : listTeamsDNO) {
                        t.setDivision(currentDivision);
//                        upgradeHandicapAndGroupForTeam(t, false);
                        mongoTemplate.save(t);
                        if (nbTeamForCurrentDivision == 0) {
                            log.warn("first team in division team="+t+" - average performance="+t.getAverageHandicap());
                        }
                        nbTeamForCurrentDivision++;
                        if (nbTeamForCurrentDivision >= nbTeamByDivision) {
                            if (currentDivision.equals(DIVISION_01)) {
                                log.warn("Current division is D01 => no division change !!");
                            } else {
                                // change division
                                log.warn("Change division - nb team added in " + currentDivision + "=" + nbTeamForCurrentDivision);
                                currentDivision = computeDivisionEvolution(currentDivision, 1);
                                nbTeamForCurrentDivision = 0;
                                log.warn("Change division - now currentDivision=" + currentDivision);
                            }
                        }
                    }
                }
                // else => integration according performance average
                else {
                    int nbTeamByDivision = (listTeamsDNO.size() / DIVISION_TAB.length);
                    if (listTeamsDNO.size() % DIVISION_TAB.length != 0) {
                        nbTeamByDivision++;
                    }
                    log.warn("Nb team by division = "+nbTeamByDivision);
                    // start with the lowest division
                    String currentDivision = DIVISION_TAB[DIVISION_TAB.length - 1];
                    int nbTeamAdded = 0;
                    log.warn("Start with currentDivision=" + currentDivision);
                    for (Team t : listTeamsDNO) {
                        t.setDivision(currentDivision);
                        mongoTemplate.save(t);
                        nbTeamAdded++;
                        if (nbTeamAdded >= nbTeamByDivision) {
                            if (currentDivision.equals(DIVISION_01)) {
                                log.warn("Current division is D01 => no division change !!");
                            } else {
                                // change division
                                log.warn("Change division - nb team added in " + currentDivision + "=" + nbTeamAdded);
                                currentDivision = computeDivisionEvolution(currentDivision, 1);
                                nbTeamAdded = 0;
                                log.warn("Change division - now currentDivision=" + currentDivision);
                            }
                        }
                    }
                }

                // update cache
                teamCacheMgr.updateTeamsData(listTeamsDNO);
            } else {
                log.warn("No teams to integrate");
            }
        }
        return listTeamsDNO;
    }

    /**
     * Return the existing period for this periodID or null if not found
     * @param periodID
     * @return
     */
    public TeamPeriod getPeriodForID(String periodID) {
        try {
            Query q = Query.query(Criteria.where("_id").regex("^" + periodID, "m"));
            q.with(new Sort(Sort.Direction.DESC, "_id"));
            return mongoTemplate.findOne(q, TeamPeriod.class);
        } catch (Exception e) {
            log.error("Failed to get period for periodID="+periodID, e);
        }
        return null;
    }

    /**
     * Return list of period from database. List is ordered by ID desc
     * @return
     */
    public List<TeamPeriod> listPeriod(int offset, int nbMax) {
        try {
            Query q = new Query();
            if (nbMax > 0) {
                q.limit(nbMax);
            }
            if (offset > 0) {
                q.skip(offset);
            }
            q.with(new Sort(Sort.Direction.DESC, "_id"));
            return mongoTemplate.find(q, TeamPeriod.class);
        } catch (Exception e) {
            log.error("Exception to list period from database - offset="+offset+" - nbMax="+nbMax, e);
        }
        return null;
    }

    /**
     * Return the period before the current period
     * @param nbPeriodBefore
     * @return
     */
    public TeamPeriod getPreviousPeriod(int nbPeriodBefore) {
        try {
            Query q = new Query();
            q.with(new Sort(Sort.Direction.DESC, "_id"));
            q.limit(nbPeriodBefore + 1);
            List<TeamPeriod> listPeriod = mongoTemplate.find(q, TeamPeriod.class);
            if (listPeriod != null && listPeriod.size() == (nbPeriodBefore+1)) {
                return listPeriod.get(nbPeriodBefore);
            }
        } catch (Exception e) {
            log.error("Failed to get previous period - nbPeriodBefore="+nbPeriodBefore, e);
        }
        return null;
    }

    /**
     * Finish all tournament for current periodTour
     */
    public void finishTournamentForPeriodAndTour() {
        if (currentPeriod != null && currentPeriod.getCurrentTour() != null) {
            String currentPeriodTour = currentPeriod.getPeriodTour();
            if (currentPeriodTour != null) {
                for (String div : DIVISION_TAB) {
                    // finish all tournaments
                    List<TeamMemTournament> listMemTournament = memoryMgr.listMemTournamentForDivision(div);
                    log.warn("Division "+div+" - Start finish tournaments : nbTour="+listMemTournament.size());
                    for (TeamMemTournament e : listMemTournament) {
                        if (!finishTournament(e)) {
                            log.error("Failed to finish tournament="+e);
                        }
                    }
                    log.warn("Division "+div+" - End finish tournaments : nbTour="+listMemTournament.size());

                    TeamMemDivisionTourResult divisionTourResult = memoryMgr.getMemDivisionTourResult(div);
                    if (divisionTourResult != null) {
                        // insert DB object for each team on this tour
                        divisionTourResult.computeRanking();
                        List<TeamTourTeam> listTourTeam = new ArrayList<>();
                        for (TeamMemDivisionTourResultTeam e : divisionTourResult.listTeamResults()) {
                            TeamTourTeam tourTeam = new TeamTourTeam();
                            tourTeam.setTeamID(e.teamID);
                            tourTeam.setDivision(div);
                            tourTeam.setPeriodID(currentPeriod.getID());
                            tourTeam.setTourIndex(currentPeriod.getCurrentTour().index);
                            tourTeam.setResult(e.result);
                            tourTeam.setPoints(e.points);
                            tourTeam.setRank(e.rank);
                            tourTeam.setCreationDateISO(new Date());
                            listTourTeam.add(tourTeam);
                        }
                        if (listTourTeam.size() > 0) {
                            mongoTemplate.insertAll(listTourTeam);
                        }
                        log.warn("Division "+div+" - End insert all TeamTourTeam : nb elements="+listTourTeam.size());
                        // Update points value for each team in DivisionResult
                        TeamMemDivisionResult divisionResult = memoryMgr.getMemDivisionResult(div);
                        if (divisionResult != null) {
                            divisionResult.updatePointsOnChangeTour();
                        }
                    }
                }
            } else {
                log.error("Current periodTour is null ! - currentPeriod="+currentPeriod);
            }
        } else {
            log.error("Current period is null - currentPeriod="+currentPeriod);
        }
    }

    /**
     * Finish the tournament associated to this memory data
     * @param memTour
     */
    public boolean finishTournament(TeamMemTournament memTour) {
        long tsBegin = System.currentTimeMillis();
        if (memTour != null) {
            TeamTournament tour = getTournament(memTour.tourID);
            if (tour != null && !tour.isFinished()) {
                // list with all player on tournament
                Set<Long> listPlayer = memTour.mapTourPlayer.keySet();
                // loop on each deals to complete games for each player

                try {
                    List<TeamGame> listGameToUpdate = new ArrayList<>();
                    List<TeamGame> listGameToInsert = new ArrayList<>();
                    List<TeamGame> listGameNotFinished = new ArrayList<>();
                    //******************************************
                    // loop on all deals to update rank & result
                    for (TeamMemDeal memDeal : memTour.deals) {
                        listGameToUpdate.clear();
                        listGameToInsert.clear();
                        listGameNotFinished.clear();
                        // add in memory all game not finished (set leave or claim)
                        listGameNotFinished = listGameOnTournamentForDealNotFinished(memTour.tourID, memDeal.dealIndex);
                        if (listGameNotFinished != null) {
                            for (TeamGame game : listGameNotFinished) {
                                game.setFinished(true);
                                if (game.getBidContract() != null) {
                                    game.claimForPlayer(BridgeConstantes.POSITION_SOUTH, 0);
                                    GameMgr.computeScore(game);
                                } else {
                                    game.setLeaveValue();
                                    game.setLastDate(tour.getEndDate());
                                }
                                memTour.addResult(game, false);
                            }
                        }
                        // update list game not finished in DB
                        updateListGameDB(listGameNotFinished);

                        // insert game not existing for players started the tournament (players not playing some deals)
                        List<Long> listPlaNoResult = memDeal.getListPlayerStartedTournamentWithNoResult();
                        for (long pla : listPlaNoResult) {
                            memTour.addResultGameNotPlayed(memDeal, pla);
                        }

                        // loop all player to update game in DB
                        listGameToUpdate.clear();
                        listGameToInsert.clear();
                        for (long pla : listPlayer) {
                            TeamMemDealPlayer resPla = memDeal.getResultPlayer(pla);
                            if (resPla != null) {
                                TeamGame game = getGameOnTournamentAndDealForPlayer(memTour.tourID, memDeal.dealIndex, pla);
                                if (game != null) {
                                    listGameToUpdate.add(game);
                                } else {
                                    TeamMemTournamentPlayer memTourPla = memTour.getTournamentPlayer(pla);
                                    if (memTourPla != null) {
                                        game = new TeamGame(pla, tour, memDeal.dealIndex, memTourPla.teamID);
                                        // game is leaved
                                        game.setLeaveValue();
                                        game.setStartDate(tour.getEndDate() - 1);
                                        game.setLastDate(tour.getEndDate());
                                        listGameToInsert.add(game);
                                    } else {
                                        log.error("No memTourPlayer found pla="+pla);
                                    }
                                }
                                game.setRank(resPla.nbPlayerBetterScore + 1);
                                game.setResult(resPla.result);
                            } else {
                                log.error("Failed to find result on deal for player=" + pla);
                            }
                        }

                        // persist game in DB
                        updateListGameDB(listGameToUpdate);
                        insertListGameDB(listGameToInsert);
                    }
                    //******************************************
                    // compute tournament result & ranking
                    memTour.computeResult();
                    memTour.computeRanking(false);

                    //******************************************
                    // update division result for current tourPeriod & cureent period
                    TeamMemDivisionTourResult tourResult = getMemoryMgr().getMemDivisionTourResult(memTour.division);
                    if (tourResult != null) {
                        for (TeamMemTournamentPlayer mtp : memTour.getTourPlayers()) {
                            tourResult.addPlayerTournamentResult(mtp, false);
                        }
                        tourResult.computeRanking();
                        TeamMemDivisionResult divisionResult = getMemoryMgr().getMemDivisionResult(memTour.division);
                        if (divisionResult != null) {
                            for (TeamMemDivisionTourResultTeam tourResultTeam : tourResult.listTeamResults()) {
                                divisionResult.addTourResultTeamForCurrentPeriodTour(tourResultTeam, tourResult, false);
                            }
                            divisionResult.computeRanking();
                        } else {
                            log.error("No divisionResult found for division="+memTour.division);
                        }
                    } else {
                        log.error("No tourResult found for division="+memTour.division);
                    }

                    //******************************************
                    // insert tournament result to DB
                    List<TeamTournamentPlayer> listTourPlay = new ArrayList<>();
                    for (TeamMemTournamentPlayer mtp : memTour.getTourPlayers()) {
                        TeamTournamentPlayer tp = new TeamTournamentPlayer();
                        tp.setPlayerID(mtp.playerID);
                        tp.setTournament(tour);
                        tp.setPeriodTour(tour.getPeriodTour());
                        tp.setTeamID(mtp.teamID);
                        tp.setRank(mtp.ranking);
                        tp.setResult(mtp.result);
                        tp.setLastDate(mtp.dateLastPlay);
                        tp.setStartDate(mtp.dateStart);
                        tp.setCreationDateISO(new Date());
                        tp.setPoints(mtp.points);
                        listTourPlay.add(tp);
                    }
                    insertListTourPlayDB(listTourPlay);

                    //******************************************
                    // set tournament finished & update tournament
                    tour.setFinished(true);
                    tour.setNbPlayers(listPlayer.size());
                    updateTournamentDB(tour);

                    // remove tournament from memory
                    memoryMgr.removeTournament(tour.getDivision(), tour.getIDStr());
                    log.warn("Finish tournament="+tour+" - nb player="+listPlayer.size()+" - TS="+(System.currentTimeMillis() - tsBegin));
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
     * Create tournaments for current periodTour
     * @return nb tournament created
     */
    public int createTournamentsForPeriodAndTour() {
        int nbTournamentCreated = 0;
        if (currentPeriod != null) {
            TeamTour currentTour = currentPeriod.getCurrentTour();
            if (currentTour != null /*&& currentTour.isValidForTS(System.currentTimeMillis())*/) {
                int nbDeal = getConfigIntValue("tourNbDeal", 15);
                int nbCreditPlayDeal = getConfigIntValue("nbCreditPlayDeal", 1);
                String periodTour = buildPeriodTour(currentPeriod.getID(), currentTour.index);
                for (String div : DIVISION_TAB) {
                    int nbTeamForDivision = 0;
                    TeamMemDivisionResult divisionResult = memoryMgr.getMemDivisionResult(div);
                    if (divisionResult != null) {
                        nbTeamForDivision = divisionResult.getNbTeams();
                    }

                    for (String group : GROUP_TAB) {
                        // check no tournament exiting for this periodTour, division and group
                        int nbExisting = (int)mongoTemplate.count(new Query().addCriteria(new Criteria().andOperator(
                                Criteria.where("periodTour").is(periodTour),
                                Criteria.where("division").is(div),
                                Criteria.where("group").is(group))), TeamTournament.class);
                        if (nbExisting > 0) {
                            log.warn("Tournament already existing for periodTour="+periodTour+" - division="+div+" - group="+group+" - nbExisting="+nbExisting);
                            continue;
                        }
                        DealGenerator.DealGenerate[] tabDeal = dealGenerator.generateDeal(nbDeal, 0);
                        if (tabDeal.length == nbDeal) {
                            TeamTournament tour = new TeamTournament();
                            tour.setStartDate(currentTour.dateStart);
                            tour.setEndDate(currentTour.dateEnd);
                            tour.setPeriodTour(periodTour);
                            tour.setDivision(div);
                            tour.setGroup(group); // As many groups as lead players in a team (each lead player plays in one group)
                            tour.setCreationDateISO(new Date());
                            tour.setNbCreditsPlayDeal(nbCreditPlayDeal);
                            tour.setDealGenerate(tabDeal);
                            // persist date
                            mongoTemplate.insert(tour);
                            // add tour in memory
                            TeamMemTournament memTournament = memoryMgr.addTournament(tour);
                            if (memTournament != null) {
                                memTournament.nbTeams = nbTeamForDivision;
                            }
                            nbTournamentCreated++;
                        } else {
                            log.error("Failed to generate deals for tournament - division="+div+" - group="+group+" - tabDeal length="+tabDeal.length);
                        }
                    }
                }
                log.warn("Nb tournament created="+nbTournamentCreated);
            } else {
                log.error("Current tour is not valid ! - currentTour="+currentTour);
            }
        } else {
            log.error("Current period is null");
        }
        return nbTournamentCreated;
    }

    /**
     * Return WSTeamPeriod object for this period
     * @param period
     * @return
     */
    public WSTeamPeriod toWSTeamPeriod(TeamPeriod period) {
        if (period != null) {
            WSTeamPeriod wsPeriod = new WSTeamPeriod();
            wsPeriod.periodID = period.getID();
            wsPeriod.dateStart = period.getDateStart();
            wsPeriod.dateEnd = period.getDateEnd();
            wsPeriod.nbPlayedTours = period.getNbPlayedTours();
            wsPeriod.currentTourID = period.getCurrentTourIndex();
            return wsPeriod;
        }
        return null;
    }

    /**
     * Return WSTeamTour object for the current tour of the period
     * @param period
     * @param team
     * @param playerAsk
     * @return
     */
    public WSTeamTour toWSTeamTour(TeamPeriod period, Team team, PlayerCache playerAsk) {
        if (period != null && team != null && playerAsk != null) {
            TeamTour curTour = period.getCurrentTour();
            if (curTour != null && curTour.isValidForTS(System.currentTimeMillis())) {
                WSTeamTour wsTour = new WSTeamTour();
                wsTour.tourID = curTour.index;
                wsTour.dateStart = curTour.dateStart;
                wsTour.dateEnd = curTour.dateEnd;

                if (team.getDivision() != null && !team.getDivision().equals(DIVISION_NO)) {
                    // add tournament in progress for player
                    TeamPlayer teamPlayer = team.getPlayer(playerAsk.ID);
                    if (teamPlayer != null) {
                        TeamMemTournament memTournament = memoryMgr.getMemTournamentForDivisionAndPlayer(team.getDivision(), playerAsk.ID);
                        if (memTournament != null) {
                            wsTour.tournament = toWSTournament(memTournament, playerAsk);
                        }
                    }
                }
                return wsTour;
            }
        }
        return null;
    }

    @Override
    public boolean distributionExists(String cards) {
        // check if a tournament with a deal containing this distribution exists
        return mongoTemplate.findOne(Query.query(Criteria.where("listDeal.cards").is(cards)), TeamTournament.class) != null;
    }

    public List<TeamTournament> listTournamentNotFinishedForPeriodTourAndDivision(String periodTour, String division) {
        Query q = new Query(new Criteria().andOperator(
                Criteria.where("periodTour").is(periodTour),
                Criteria.where("division").is(division),
                Criteria.where("finished").is(false)));
        return mongoTemplate.find(q, TeamTournament.class);
    }

    /**
     * List game on a tournament
     * @param tourID
     * @return
     */
    public List<TeamGame> listGameOnTournament(String tourID){
        Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)));
        return mongoTemplate.find(q, TeamGame.class);
    }

    @Override
    public String getTournamentCategoryName() {
        return Constantes.TOURNAMENT_CATEGORY_NAME_TEAM;
    }

    @Override
    public TeamTournament getTournament(String tourID) {
        return mongoTemplate.findById(new ObjectId(tourID), TeamTournament.class);
    }

    @Override
    public TeamGame getGame(String gameID) {
        return mongoTemplate.findById(new ObjectId(gameID), TeamGame.class);
    }

    @Override
    public String _buildDealID(String tourID, int index) {
        return buildDealID(tourID, index);
    }

    @Override
    public String _extractTourIDFromDealID(String dealID) {
        return extractTourIDFromDealID(dealID);
    }

    @Override
    public int _extractDealIndexFromDealID(String dealID) {
        return extractDealIndexFromDealID(dealID);
    }

    @Override
    public boolean isBidAnalyzeEnable() {
        return getConfigIntValue("engineAnalyzeBid", 1) == 1;
    }

    @Override
    public boolean isDealParEnable() {
        return getConfigIntValue("enginePar", 1) == 1;
    }

    @Override
    public void updateTournamentDB(Tournament tour) throws FBWSException {
        if (tour != null && tour instanceof TeamTournament) {
            TeamTournament tourTeam = (TeamTournament)tour;
            try {
                mongoTemplate.save(tourTeam);
            } catch (Exception e) {
                log.error("Exception to save tour=" + tour, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.error("Tour not valid ! tour="+tour);
        }
    }

    @Override
    public void updateGameFinished(FBSession session) throws FBWSException {
        if (session == null) {
            log.error("Session parameter is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (session.getCurrentGameTable() == null || session.getCurrentGameTable().getGame() == null || !(session.getCurrentGameTable().getGame() instanceof TeamGame)) {
            log.error("Table or game in session not valid ! - table="+session.getCurrentGameTable());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TeamGame game = (TeamGame)session.getCurrentGameTable().getGame();
        if (!game.isReplay()) {
            if (game.isFinished()) {
                // update data game and table in DB
                updateGameDB(game);

                // update tournament play in memory
                memoryMgr.addResult(game);

                // remove game from session
                session.removeGame();
                // remove player from set
                gameMgr.removePlayerRunning(session.getPlayer().getID());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Nothing to do ... game not finished - game=" + game);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Nothing to do ... replay game - game=" + game);
            }
        }
    }

    @Override
    public void checkGame(Game game) throws FBWSException {
        if (game != null) {
            if (game.getTournament().isFinished()) {
                throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
            }
            if (!game.getTournament().isDateValid(System.currentTimeMillis())) {
                throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
            }
        } else {
            log.error("Game is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Save the game to DB. if already existing an update is done, else an insert
     * @param game
     * @throws FBWSException
     */
    public void updateGameDB(TeamGame game) throws FBWSException {
        if (game != null && !game.isReplay()) {
            // try to find bug of not valid game ...
            if (getConfigIntValue("findBugGameNotValid", 0) == 1) {
                if (game.isFinished() && game.getContractType() == 0 && game.getBids().length() == 0) {
                    log.error("Game not valid !!! - game=" + game);
                    Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                    if (getConfigIntValue("blockBugGameNotValid", 0) == 1) {
                        log.error("Game not save ! game="+game);
                        return;
                    }
                }
            }
            try {
                mongoTemplate.save(game);
            } catch (Exception e) {
                log.error("Exception to save game="+game, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Update games to DB
     * @param listGame
     * @throws FBWSException
     */
    public <T extends Game> void updateListGameDB(List<T> listGame) throws FBWSException {
        if (listGame != null && listGame.size() > 0) {
            try {
                long ts = System.currentTimeMillis();
                for (T g : listGame) {
                    // try to find bug of not valid game ...
                    if (getConfigIntValue("findBugGameNotValid", 0) == 1) {
                        if (g.isFinished() && g.getContractType() == 0 && g.getBids().length() == 0) {
                            log.error("Game not valid !!! - game=" + g);
                            Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                            log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                            if (getConfigIntValue("blockBugGameNotValid", 0) == 1) {
                                log.error("Game not save ! game="+g);
                                continue;
                            }
                        }
                    }
                    mongoTemplate.save(g);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Time to updateListGameDB : size="+listGame.size()+" - ts="+(System.currentTimeMillis() - ts));
                }
            } catch (Exception e) {
                log.error("Exception to save listGame", e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Insert games to DB
     * @param listGame
     * @throws FBWSException
     */
    public <T extends Game> void insertListGameDB(List<T> listGame) throws FBWSException {
        if (listGame != null && listGame.size() > 0) {
            try {
                mongoTemplate.insertAll(listGame);
            } catch (Exception e) {
                log.error("Exception to save listGame", e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Insert tourPlay to DB
     * @param listTourPlay
     * @throws FBWSException
     */
    public void insertListTourPlayDB(List<TeamTournamentPlayer> listTourPlay) throws FBWSException {
        if (listTourPlay != null && listTourPlay.size() > 0) {
            try {
                mongoTemplate.insertAll(listTourPlay);
            } catch (Exception e) {
                log.error("Exception to save listTourPlay", e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Select tournament for player : use tournament in progress or select a new one
     * @param session
     * @param conventionProfil
     * @param conventionValue
     * @param cardsConventionProfil
     * @param cardsConventionValue
     * @return
     * @throws FBWSException
     */
    public WSTableTournament playTournament(FBSession session, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) throws FBWSException {
        if (session == null) {
            log.error("Session is null");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        Player player = session.getPlayer();
        Team team = teamMgr.getTeamForPlayer(player.getID());
        if (team == null) {
            log.error("No team found for player="+player);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NO_TEAM);
        }
        if (team.getDivision() == null || team.getDivision().length() == 0 || team.getDivision().equals(DIVISION_NO) ||
                team.getLastPeriodWithCompleteTeam() == null || !team.getLastPeriodWithCompleteTeam().equals(currentPeriod.getID())) {
            log.error("Team is not authorized to play ! division not valid or lastPeriodWithCompleteTeam!=currentPeriod - team="+team);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TeamPlayer teamPlayer = team.getPlayer(player.getID());
        if (teamPlayer == null) {
            log.error("No teamPlayer found in team=["+team+"] for player=["+player+"]");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (teamPlayer.isSubstitute()) {
            log.error("Player is subsitute ! team="+team+" - teamPlayer="+teamPlayer);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_SUBSTITUTE);
        }
        if (teamPlayer.getGroup() == null) {
            log.error("Player group is null ! team="+team+" - teamPlayer="+teamPlayer);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        String division = team.getDivision();
        String playerGroup = teamPlayer.getGroup();
        Table table = null;
        TeamTournament tour = null;
        TeamMemTournament memTournament = null;

        //**********************
        // use current table in session ?
        if (session.getCurrentGameTable() != null &&
                !session.getCurrentGameTable().isReplay() &&
                session.getCurrentGameTable().getTournament() != null &&
                session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TEAM &&
                ((TeamTournament)session.getCurrentGameTable().getTournament()).getPeriodTour().equals(getCurrentPeriod().getPeriodTour()) &&
                !session.getCurrentGameTable().isAllTournamentDealPlayed()) {
            // continue to play the current tournament
            table = session.getCurrentGameTable();
            tour = (TeamTournament) table.getTournament();
            memTournament = memoryMgr.getMemTournamentDivisionAndTourID(division, tour.getIDStr());
            if (memTournament == null) {
                log.error("Failed to retrieve MemTour for tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        //********************
        // need to get tournament in progress or one not yet played for this player
        else {
            // 1 tournament by period tour for each player group
            memTournament = memoryMgr.getMemTournamentForDivisionAndGroup(division, playerGroup);
            if (memTournament == null) {
                log.error("Failed to get tournament for player="+player+" - division="+division+" - group="+playerGroup);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            TeamMemTournamentPlayer tourPla = memTournament.getTournamentPlayer(player.getID());
            if (tourPla != null) {
                if (tourPla.isPlayerFinish()) {
                    // only 1 tournament by period tour for player group
                    throw new FBWSException(FBExceptionType.TOURNAMENT_TEAM_ALL_TOUR_PLAYED);
                }
            } else {
                // first play for this player on tournament => check not play by this team
                TeamMemTournamentPlayer tp = memTournament.getTournamentPlayerForTeam(team.getIDStr());
                if (tp != null) {
                    log.error("Another player has already played this tournament for this team - tp="+tp+" - team="+team+" - playerID="+player.getID());
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

            }
            tour = getTournament(memTournament.tourID);
            if (tour == null) {
                log.error("Failed to load tournament with ID="+memTournament.tourID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }

        //*******************
        // build table
        if (table == null) {
            table = new Table(player, tour);
            // set deals played
            TeamMemTournamentPlayer memRanking = memTournament.getTournamentPlayer(player.getID());
            if (memRanking != null) {
                table.setPlayedDeals(memRanking.playedDeals);
            }
            // retrieve game if exist
            TeamGame game = getGameNotFinishedOnTournamentForPlayer(tour.getIDStr(), player.getID());
            if (game != null) {
                table.setGame(game);
            }
            session.setCurrentGameTable(table);
        }

        //*******************
        // retrieve game
        TeamGame game = null;
        if (table.getGame() != null && !table.getGame().isFinished() && table.getGame().getTournament().getIDStr().equals(tour.getIDStr())) {
            game = (TeamGame)table.getGame();
        } else {
            // need to retrieve an existing game
            int lastIndexPlayed = 0;
            List<TeamGame> listGame = listGameOnTournamentForPlayer(tour.getIDStr(), player.getID());
            if (listGame != null) {
                for (TeamGame g : listGame) {
                    // BUG with game finished, contractType=0 (PA) and bids = ""
                    if (g.isInitFailed() && g.isFinished() && g.getContractType() == 0 && g.getBids().length() == 0) {
                        if (getConfigIntValue("fixBugGameNotValidOnPlayTournament", 0) == 1) {
                            log.error("BugGameNotValid - init failed => Reset game data g="+g);
                            g.resetData();
                            g.setConventionSelection(conventionProfil, conventionValue);
                            g.setDeviceID(session.getDeviceID());
                        }
                    }

                    if (!g.isFinished()) {
                        game = g;
                        break;
                    }
                    lastIndexPlayed = g.getDealIndex();
                }
            }
            // need to create a new game
            if (game == null) {
                // check player credit
                playerMgr.checkPlayerCredit(player, tour.getNbCreditsPlayDeal());
                // need to create a new game
                if (lastIndexPlayed >= tour.getNbDeals()) {
                    log.error("lastIndexPlayed to big to play another deal ! player="+session.getPlayer()+" - lastIndexPlayed="+lastIndexPlayed+" - tour="+tour);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                // create game
                game = createGame(player.getID(), tour, team.getIDStr(), lastIndexPlayed+1, conventionProfil, conventionValue, cardsConventionProfil, cardsConventionValue, session.getDeviceID());

                if (game != null) {
                    ContextManager.getPlayerMgr().updatePlayerCreditDeal(player, tour.getNbCreditsPlayDeal(), 1);
                } else {
                    log.error("Failed to create game - player="+player.getID()+" - tour="+tour);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_TEAM, 1);
            }
            table.setGame(game);
        }

        // check if a thread is not running for this game
        GameThread robotThread = gameMgr.getThreadPlayRunning(game.getIDStr());
        if (robotThread != null) {
            if (log.isDebugEnabled()) {
                log.debug("A thread is currently running for gameID=" + game.getID() + " - stop it !");
            }
            robotThread.interruptRun();
        }

        // add player on tournament memory
        synchronized (memoryMgr.getLockTour(tour.getIDStr())) {
            TeamMemTournamentPlayer mtp = memTournament.getOrCreateTournamentPlayer(player.getID(), team.getIDStr(), System.currentTimeMillis());
            mtp.currentDealIndex = game.getDealIndex();
        }

        //*********************
        // Build data to return
        WSTableTournament tableTour = new WSTableTournament();
        tableTour.tournament = toWSTournament(tour, session.getPlayerCache());
        tableTour.tournament.currentDealIndex = game.getDealIndex();
        tableTour.currentDeal = new WSGameDeal();
        tableTour.currentDeal.setDealData(game.getDeal(), tour.getIDStr());
        tableTour.currentDeal.setGameData(game);
        tableTour.table = table.toWSTableGame();
        tableTour.gameIDstr = game.getIDStr();
        tableTour.conventionProfil = game.getConventionProfile();
        tableTour.tournament.remainingTime = tour.getEndDate() - System.currentTimeMillis();
        tableTour.creditAmount = session.getPlayer().getCreditAmount();
        tableTour.nbPlayedDeals = session.getPlayer().getNbPlayedDeals();
        tableTour.replayEnabled = playerMgr.isReplayEnabledForPlayer(session);
        tableTour.freemium = session.isFreemium();
        return tableTour;
    }

    /**
     * Return the game not finished for a player on tournament.
     * @param tourID
     * @param playerID
     * @return
     */
    protected TeamGame getGameNotFinishedOnTournamentForPlayer(String tourID, long playerID) {
        List<TeamGame> listGame = listGameOnTournamentForPlayer(tourID, playerID);
        if (listGame != null) {
            for (TeamGame g : listGame) {
                if (!g.isFinished()) {
                    return g;
                }
            }
        }
        return null;
    }

    /**
     * Return if existing game on tournament and deal for a player
     * @param tourID
     * @param dealIndex
     * @param playerID
     * @return
     * @throws FBWSException
     */
    public TeamGame getGameOnTournamentAndDealForPlayer(String tourID, int dealIndex, long playerID) {
        try {
            Query q = new Query();
            Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cPlayerID));
            return mongoTemplate.findOne(q, TeamGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament deal and playerID", e);
        }
        return null;
    }

    /**
     * List game for a player on a tournament (order by dealIndex asc)
     * @param tourID
     * @param playerID
     * @return
     */
    public List<TeamGame> listGameOnTournamentForPlayer(String tourID, long playerID){
        try {
            Query q = Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("tournament.$id").is(new ObjectId(tourID))));
            q.with(new Sort(Sort.Direction.ASC, "dealIndex"));
            return mongoTemplate.find(q, TeamGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament="+tourID+" and playerID="+playerID, e);
        }
        return null;
    }

    /**
     * List game on a tournament and deal not finished
     * @param tourID
     * @param dealIndex
     * @return
     */
    public List<TeamGame> listGameOnTournamentForDealNotFinished(String tourID, int dealIndex) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            Criteria cNotFinished = Criteria.where("finished").is(false);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cNotFinished));
            return mongoTemplate.find(q, TeamGame.class);
        } catch (Exception e) {
            log.error("Failed to list game not finished for tournament="+tourID+" and dealIndex="+dealIndex, e);
        }
        return null;
    }

    public List<TeamGame> listGameFinishedOnTournamentAndDeal(String tourID, int dealIndex, int offset, int nbMax) {
        Query q = new Query();
        Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
        Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
        Criteria cNotFinished = Criteria.where("finished").is(true);
        q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cNotFinished));
        q.with(new Sort(Sort.Direction.DESC, "score"));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return mongoTemplate.find(q, TeamGame.class);
    }

    /**
     * Create a game for player on tournament and dealIndex.
     * @param playerID
     * @param tour
     * @param dealIndex
     * @param conventionProfil
     * @param conventionData
     * @param cardsConventionProfil
     * @param cardsConventionValue
     * @param deviceID
     * @return
     * @throws FBWSException if an existing game existed for player, tournament and dealIndex or if an exception occurs
     */
    private TeamGame createGame(long playerID, TeamTournament tour, String teamID, int dealIndex, int conventionProfil, String conventionData, int cardsConventionProfil, String cardsConventionValue, long deviceID) throws FBWSException {
        synchronized (lockCreateGame.getLock(tour.getIDStr()+"-"+playerID)) {
            if (getGameOnTournamentAndDealForPlayer(tour.getIDStr(), dealIndex, playerID) != null) {
                log.error("A game already exist on tour="+tour+" - dealIndex="+dealIndex+" - playerID="+playerID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            try {
                TeamGame game = new TeamGame(playerID, tour, dealIndex, teamID);
                game.setStartDate(Calendar.getInstance().getTimeInMillis());
                game.setLastDate(Calendar.getInstance().getTimeInMillis());
                game.setConventionSelection(conventionProfil, conventionData);
                game.setCardsConventionSelection(cardsConventionProfil, cardsConventionValue);
                game.setDeviceID(deviceID);
                // try to find bug of not valid game ...
                if (getConfigIntValue("findBugGameNotValid", 0) == 1) {
                    if (game.isFinished() && game.getContractType() == 0 && game.getBids().length() == 0) {
                        log.error("Game not valid !!! - game=" + game);
                        Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                        log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                        if (getConfigIntValue("blockBugGameNotValid", 0) == 1) {
                            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                        }
                    }
                }
                mongoTemplate.insert(game);
                return game;
            } catch (Exception e) {
                log.error("Exception to create game player="+playerID+" - tour="+tour+" - dealIndex="+dealIndex, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public WSTournament toWSTournament(TeamMemTournament memTournament, PlayerCache playerCache) {
        if (memTournament != null) {
            TeamTournament tournament = getTournament(memTournament.tourID);
            if (tournament != null) {
                WSTournament wst = tournament.toWS();
                wst.nbTotalPlayer = memTournament.getNbPlayers();
                wst.resultPlayer = memTournament.getWSResultPlayer(playerCache, true);
                if (wst.resultPlayer != null) {
                    wst.nbTotalPlayer = memTournament.getNbPlayersWhoFinished();
                    wst.playerOffset = wst.resultPlayer.getRank();
                }
                wst.currentDealIndex = memTournament.getCurrentDealForPlayer(playerCache.ID);
                wst.remainingTime = tournament.getEndDate() - System.currentTimeMillis();
                return wst;
            }
        }
        return null;
    }

    /**
     * Transform tournament for webservice object
     * @param tour
     * @param playerCache
     * @return
     */
    public WSTournament toWSTournament(TeamTournament tour, PlayerCache playerCache) {
        if (tour != null) {
            WSTournament wst = tour.toWS();
            TeamMemTournament memTournament = memoryMgr.getMemTournamentDivisionAndTourID(tour.getDivision(), tour.getIDStr());
            if (memTournament != null) {
                wst.nbTotalPlayer = memTournament.getNbPlayers();
                wst.resultPlayer = memTournament.getWSResultPlayer(playerCache, true);
                if (wst.resultPlayer != null) {
                    wst.nbTotalPlayer = memTournament.getNbPlayersWhoFinished();
                }
                wst.currentDealIndex = memTournament.getCurrentDealForPlayer(playerCache.ID);
                wst.remainingTime = tour.getEndDate() - System.currentTimeMillis();
            } else {
                wst.nbTotalPlayer = tour.getNbPlayers();
                TeamTournamentPlayer tourPlayer = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
                if (tourPlayer != null) {
                    wst.resultPlayer = tourPlayer.toWSResultTournamentPlayer(playerCache, playerCache.ID);
                }
            }
            if (wst.resultPlayer != null) {
                wst.playerOffset = wst.resultPlayer.getRank();
            }
            return wst;
        }
        return null;
    }

    /**
     * Return tournamentPlayer object for a player on a tournament
     * @param tournamentID
     * @param playerID
     * @return
     */
    public TeamTournamentPlayer getTournamentPlayer(String tournamentID, long playerID) {
        return mongoTemplate.findOne(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tournamentID)).andOperator(Criteria.where("playerID").is(playerID))), TeamTournamentPlayer.class);
    }

    /**
     * Return a list of TeamTournamentPlayer for a specific team and tour
     * @param teamID
     * @param periodTour (format : periodID-tourIndex)
     * @return
     */
    public List<TeamTournamentPlayer> getListTournamentPlayerForTeamAndPeriodTour(String teamID, String periodTour){
        return mongoTemplate.find(Query.query(Criteria.where("teamID").is(teamID).andOperator(Criteria.where("periodTour").is(periodTour))), TeamTournamentPlayer.class);
    }

    public TeamTourTeam getTeamTourTeam(String teamID, String periodID, int tourIndex){
        Query q = new Query();
        q.addCriteria(Criteria.where("teamID").is(teamID));
        q.addCriteria(Criteria.where("periodID").is(periodID));
        q.addCriteria(Criteria.where("tourIndex").is(tourIndex));
        return mongoTemplate.findOne(q, TeamTourTeam.class);
    }

    /**
     * Retrieve tournament wich include this deal
     * @param dealID
     * @return
     */
    public TeamTournament getTournamentWithDeal(String dealID) {
        String tourID = extractTourIDFromDealID(dealID);
        if (tourID != null) {
            return getTournament(tourID);
        }
        return null;
    }

    /**
     * Return the gameView object for this game with game data and table. The player must have played the deal of the game.
     * @param gameID
     * @param player
     * @return
     * @throws FBWSException
     */
    public WSGameView viewGame(String gameID, Player player) throws FBWSException {
        // check gameID
        TeamGame game = getGame(gameID);
        if (game == null) {
            log.error("GameID not found - gameID="+gameID+" - player="+player);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // check player has played this deal
        if ((game.getPlayerID() != player.getID()) && getGameOnTournamentAndDealForPlayer(game.getTournament().getIDStr(), game.getDealIndex(), player.getID()) == null) {
            log.error("Deal not played by player - gameID="+gameID+" - playerID="+player.getID()+" - tour="+game.getTournament()+" - deal index="+game.getDealIndex());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (game.getPlayerID() == player.getID() && !game.isFinished()) {
            log.error("Deal not finished by player - gameID="+gameID+" - playerID="+player.getID());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        WSTableGame table = new WSTableGame();
        table.tableID = -1;
        table.playerSouth = WSGamePlayer.createGamePlayerHuman(game.getPlayerID()==player.getID()?player:playerMgr.getPlayer(game.getPlayerID()), Constantes.PLAYER_STATUS_PRESENT, player.getID());
        table.playerWest = WSGamePlayer.createGamePlayerRobot();
        table.playerNorth = WSGamePlayer.createGamePlayerRobot();
        table.playerEast = WSGamePlayer.createGamePlayerRobot();

        WSGameDeal gameDeal = new WSGameDeal();
        gameDeal.setDealData(game.getDeal(), game.getTournament().getIDStr());
        gameDeal.setGameData(game);

        WSGameView gameView = new WSGameView();
        gameView.game = gameDeal;
        gameView.table = table;
        gameView.tournament = toWSTournament(game.getTournament(), playerCacheMgr.getPlayerCache(player.getID()));

        return gameView;
    }

    /**
     * Return a gameView for the deal, score and contract. If player has played this deal with this contract and score, return the game of this player else return the game of unknown player
     * @param dealID
     * @param score
     * @param contractString
     * @param player
     * @return
     * @throws FBWSException
     */
    public WSGameView viewGameForDealScoreAndContract(String dealID, int score, String contractString, String lead, Player player) throws FBWSException {
        TeamTournament tour = getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        TeamDeal deal = (TeamDeal) tour.getDeal(dealID);
        if (deal == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // check player played the deal of this game
        TeamGame game = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, player.getID());
        if (game == null) {
            log.error("PLAYER DOESN'T PLAY THIS DEAL : dealID="+dealID+" - playerID="+player.getID());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        // check deal
        boolean bLoadGameForUnknown = true;
        if (game != null) {
            if (game.getContractWS().equals(contractString) && game.getScore() == score) {
                if (lead != null && lead.length() > 0) {
                    if (game.getBegins().equals(lead)) {
                        bLoadGameForUnknown = false;
                    }
                } else {
                    bLoadGameForUnknown = false;
                }
            }
        }
        // no game found for this contract and score for the player => so find from any player
        if (bLoadGameForUnknown) {
            String contract = Constantes.contractStringToContract(contractString);
            int contractType = Constantes.contractStringToType(contractString);
            if (contractType == Constantes.CONTRACT_TYPE_PASS) {
                contract = "";
            }
            List<Criteria> listCriteria = new ArrayList<>();
            listCriteria.add(Criteria.where("tournament.$id").is(new ObjectId(tour.getIDStr())));
            listCriteria.add(Criteria.where("dealIndex").is(deal.index));
            listCriteria.add(Criteria.where("finished").is(true));
            listCriteria.add(Criteria.where("score").is(score));
            listCriteria.add(Criteria.where("contract").is(contract));
            listCriteria.add(Criteria.where("contractType").is(contractType));
            if (lead != null && lead.length() > 0) {
                listCriteria.add(Criteria.where("cards").regex("^"+lead));
            }
            Query q = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
            q.limit(1);
            try {
                game = mongoTemplate.findOne(q, TeamGame.class);
            } catch (Exception e) {
                log.error("Error to retrieve data for deal="+dealID+" score="+score+" and contract="+contract,e);
            }
        }

        if (game == null) {
            log.error("NO GAME FOUND FOR THIS DEAL, SCORE AND CONTRACT - dealID="+dealID+" - score="+score+" - contract="+contractString);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        WSTableGame table = new WSTableGame();
        table.tableID = -1;
        table.playerSouth = WSGamePlayer.createGamePlayerHuman(game.getPlayerID()==player.getID()?player:playerMgr.getPlayer(game.getPlayerID()), Constantes.PLAYER_STATUS_PRESENT, player.getID());
        table.playerWest = WSGamePlayer.createGamePlayerRobot();
        table.playerNorth = WSGamePlayer.createGamePlayerRobot();
        table.playerEast = WSGamePlayer.createGamePlayerRobot();

        WSGameDeal gameDeal = new WSGameDeal();
        gameDeal.setDealData(game.getDeal(), game.getTournament().getIDStr());
        gameDeal.setGameData(game);

        WSGameView gameView = new WSGameView();
        gameView.game = gameDeal;
        gameView.table = table;
        gameView.tournament = toWSTournament(game.getTournament(), playerCacheMgr.getPlayerCache(player.getID()));

        return gameView;
    }

    public List<WSTeamResult> getTeamRanking(FBSession session, String division, String periodID, int tourIndex, int offset, int nbMaxResult) throws FBWSException {
        if (division == null || periodID == null || tourIndex < 0) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        boolean isCurrentPeriod = false;
        boolean isCurrentTour = false;
        if (currentPeriod != null) {
            isCurrentPeriod = currentPeriod.getID().equalsIgnoreCase(periodID);
            if (currentPeriod.getCurrentTour() != null) {
                isCurrentTour = isCurrentPeriod && getCurrentPeriod().getCurrentTour().index == tourIndex;
            }
        }
        // Results for a period
        if(tourIndex == 0){
            if(isCurrentPeriod){
                return memoryMgr.getRankingForDivision(division, offset, nbMaxResult);
            } else {
                return getRankingForDivisionAndPeriod(division, periodID, nbMaxResult, offset);
            }
        // Results for a tour
        } else {
            if(isCurrentTour){
                return memoryMgr.getTourRankingForDivision(division, offset, nbMaxResult);
            } else{
                return getRankingForDivisionPeriodAndTour(division, periodID, tourIndex, nbMaxResult, offset);
            }
        }
    }

    public WSTeamResult getTeamCurrentPeriodRankingForTeam(String teamID, String division) throws FBWSException {
        if (division == null || teamID == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        return memoryMgr.getRankingForTeamInDivision(teamID, division);
    }

    public WSTeamResult getTeamResult(String teamID, String division, String periodID, int tourIndex) throws FBWSException {
        if (division == null || periodID == null || tourIndex < 0) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        boolean isCurrentPeriod = false;
        boolean isCurrentTour = false;
        if (currentPeriod != null) {
            isCurrentPeriod = currentPeriod.getID().equalsIgnoreCase(periodID);
            if (currentPeriod.getCurrentTour() != null) {
                isCurrentTour = isCurrentPeriod && getCurrentPeriod().getCurrentTour().index == tourIndex;
            }
        }
        // Results for a period
        if(tourIndex == 0){
            if(isCurrentPeriod){
                return memoryMgr.toWSTeamResult(memoryMgr.getTeamMemDivisionResultForTeam(teamID, division));
            } else {
                return getTeamResultForDivisionAndPeriod(teamID, division, periodID);
            }
            // Results for a tour
        } else {
            if(isCurrentTour){
                return memoryMgr.toWSTeamResult(memoryMgr.getTeamMemDivisionTourResultForTeam(teamID, division));
            } else{
                return getTeamResultForDivisionPeriodAndTour(teamID, division, periodID, tourIndex);
            }
        }
    }

    public int getTeamRankingSize(String division, String periodID, int tourIndex) throws FBWSException {
        if (division == null || periodID == null || tourIndex < 0) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        boolean isCurrentPeriod = false;
        boolean isCurrentTour = false;
        if (currentPeriod != null) {
            isCurrentPeriod = currentPeriod.getID().equalsIgnoreCase(periodID);
            if (currentPeriod.getCurrentTour() != null) {
                isCurrentTour = isCurrentPeriod && getCurrentPeriod().getCurrentTour().index == tourIndex;
            }
        }
        // Results for a period
        if(tourIndex == 0){
            if(isCurrentPeriod){
                return memoryMgr.getMemDivisionResult(division).mapResultTeam.size();
            } else {
                return countNbTeamsForDivisionAndPeriod(division, periodID);
            }
            // Results for a tour
        } else {
            if(isCurrentTour){
                return memoryMgr.getMemDivisionTourResult(division).mapResultTeam.size();
            } else{
                return countNbTeamsForDivisionPeriodAndTour(division, periodID, tourIndex);
            }
        }
    }

    public int getTeamCurrentPeriodRankingSize(String division) throws FBWSException {
        if (currentPeriod != null) {
            return getTeamRankingSize(division, currentPeriod.getID(), 0);
        }
        return 0;
    }

    public int countNbTeamsForDivisionAndPeriod(String division, String periodID){
        Query q = new Query();
        q.addCriteria(Criteria.where("division").is(division));
        q.addCriteria(Criteria.where("periodID").is(periodID));
        return (int) mongoTemplate.count(q, TeamPeriodTeam.class);
    }

    public int countNbTeamsForDivisionPeriodAndTour(String division, String periodID, int tourIndex){
        Query q = new Query();
        q.addCriteria(Criteria.where("division").is(division));
        q.addCriteria(Criteria.where("periodID").is(periodID));
        q.addCriteria(Criteria.where("tourIndex").is(tourIndex));
        return (int) mongoTemplate.count(q, TeamTourTeam.class);
    }

    public List<WSResultTournamentPlayer> getPlayerRanking(FBSession session, String division, String periodID, int tourIndex, String group, int offset, int nbMaxResult) throws FBWSException {
        if (division == null || periodID == null || tourIndex < 0 || group == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        boolean isCurrentPeriod = false;
        boolean isCurrentTour = false;
        if (currentPeriod != null) {
            isCurrentPeriod = currentPeriod.getID().equalsIgnoreCase(periodID);
            if (currentPeriod.getCurrentTour() != null) {
                isCurrentTour = isCurrentPeriod && getCurrentPeriod().getCurrentTour().index == tourIndex;
            }
        }
        if(isCurrentTour){
            // Results for current tour (data comes from memory)
            TeamMemTournament memTournament = memoryMgr.getMemTournamentForDivisionAndGroup(division, group);
            List<TeamMemTournamentPlayer> ranking = memTournament.getRanking(offset, nbMaxResult, null, false);
            List<WSResultTournamentPlayer> wsRanking = new ArrayList<>();
            for(TeamMemTournamentPlayer result : ranking){
                wsRanking.add(result.toWSResultTournamentPlayer(false));
            }
            return wsRanking;
        } else {
            // Results for a previous tour (data comes from DB)
            TeamTournament tournament = getTournamentForDivisionPeriodTourAndGroup(division, periodID, tourIndex, group);
            return getListWSResultTournamentPlayer(tournament, offset, nbMaxResult, null, session.getPlayer().getID(), false, null);
        }
    }

    public TeamTournament getTournamentForDivisionPeriodTourAndGroup(String division, String periodID, int tourIndex, String group){
        Query q = new Query();
        q.addCriteria(Criteria.where("periodTour").is(buildPeriodTour(periodID, tourIndex)));
        q.addCriteria(Criteria.where("division").is(division));
        q.addCriteria(Criteria.where("group").is(group));
        return mongoTemplate.findOne(q, TeamTournament.class);
    }

    public List<WSTeamResult> getRankingForDivisionPeriodAndTour(String division, String periodID, int tourIndex, int nbMaxResult, int offset){
        List<WSTeamResult> ranking = new ArrayList<>();
        Query q = new Query();
        q.addCriteria(Criteria.where("division").is(division));
        q.addCriteria(Criteria.where("periodID").is(periodID));
        q.addCriteria(Criteria.where("tourIndex").is(tourIndex));
        q.limit(nbMaxResult).skip(offset);
        q.with(new Sort(Sort.Direction.ASC, "rank"));
        List<TeamTourTeam> teamResults = mongoTemplate.find(q, TeamTourTeam.class);
        for(TeamTourTeam result : teamResults){
            ranking.add(toWSTeamResult(result));
        }
        return ranking;
    }

    public List<WSTeamResult> getRankingForDivisionAndPeriod(String division, String periodID, int nbMaxResult, int offset){
        List<WSTeamResult> ranking = new ArrayList<>();
        Query q = new Query();
        q.addCriteria(Criteria.where("division").is(division));
        q.addCriteria(Criteria.where("periodID").is(periodID));
        q.limit(nbMaxResult).skip(offset);
        q.with(new Sort(Sort.Direction.ASC, "rank"));
        List<TeamPeriodTeam> teamResults = mongoTemplate.find(q, TeamPeriodTeam.class);
        for(TeamPeriodTeam result : teamResults){
            ranking.add(toWSTeamResult(result));
        }
        return ranking;
    }

    public WSTeamResult getTeamResultForDivisionAndPeriod(String teamID, String division, String periodID){
        Query q = new Query();
        q.addCriteria(Criteria.where("teamID").is(teamID));
        q.addCriteria(Criteria.where("division").is(division));
        q.addCriteria(Criteria.where("periodID").is(periodID));
        return toWSTeamResult(mongoTemplate.findOne(q, TeamPeriodTeam.class));
    }

    public WSTeamPeriodResult getTeamResultForPeriod(String teamID, String periodID){
        Query q = new Query();
        q.addCriteria(Criteria.where("teamID").is(teamID));
        q.addCriteria(Criteria.where("periodID").is(periodID));
        return toWSTeamPeriodResult(mongoTemplate.findOne(q, TeamPeriodTeam.class));
    }

    public WSTeamResult getTeamResultForDivisionPeriodAndTour(String teamID, String division, String periodID, int tourIndex){
        Query q = new Query();
        q.addCriteria(Criteria.where("teamID").is(teamID));
        q.addCriteria(Criteria.where("periodID").is(periodID));
        q.addCriteria(Criteria.where("division").is(division));
        q.addCriteria(Criteria.where("tourIndex").is(tourIndex));
        return toWSTeamResult(mongoTemplate.findOne(q, TeamTourTeam.class));
    }

    /**
     * List teams for division with field lastPeriodWithCompleteTeam = currentPeriod
     * @param division
     * @return
     */
    public List<Team> getTeamsCompleteForDivision(String division){
        Query q = new Query();
        q.addCriteria(Criteria.where("division").is(division));
        q.addCriteria(Criteria.where("lastPeriodWithCompleteTeam").is(currentPeriod.getID()));
        return mongoTemplate.find(q, Team.class);
    }

    /**
     * Set field lastPeriodWithCompleteTeam with the value of currentPeroid for all teams with complete teams.
     */
    public void updateLastPeriodWithCompleteTeamForAllDivision() {
        for(String division : DIVISION_TAB){
            teamMgr.updateLastPeriodWithCompleteTeamForDivision(division, GROUP_TAB.length, currentPeriod.getID());
            log.warn("Update field lastPeriodWithCompleteTeam for division="+division);
        }
    }

    public List<TeamPeriodTeam> listPeriodTeam(String division, String periodID) {
        Query q = new Query(Criteria.where("division").is(division).andOperator(Criteria.where("periodID").is(periodID)));
        q.with(new Sort(Sort.Direction.ASC, "rank"));
        return mongoTemplate.find(q, TeamPeriodTeam.class);
    }

    public WSTeamResult toWSTeamResult(TeamTourTeam teamResult){
        if(teamResult != null){
            TeamCache teamCache = teamCacheMgr.getOrLoadTeamCache(teamResult.getTeamID());
            if(teamCache != null){
                WSTeamResult result = new WSTeamResult();
                result.teamID = teamResult.getTeamID();
                result.name = teamCache.name;
                result.countryCode = teamCache.countryCode;
                result.rank = teamResult.getRank();
                result.points = teamResult.getPoints();
                return result;
            }
        }
        return null;
    }

    public WSTeamResult toWSTeamResult(TeamPeriodTeam teamResult){
        if(teamResult != null){
            TeamCache teamCache = teamCacheMgr.getOrLoadTeamCache(teamResult.getTeamID());
            if(teamCache != null){
                WSTeamResult result = new WSTeamResult();
                result.teamID = teamResult.getTeamID();
                result.name = teamCache.name;
                result.countryCode = teamCache.countryCode;
                result.rank = teamResult.getRank();
                result.points = teamResult.getPoints();
                result.trend = teamResult.getEvolution();
                return result;
            }
        }
        return null;
    }

    /**
     * Builds WS object for result player
     * @param tourID
     * @param playerCache
     * @param useRankingFinished
     * @return
     */
    public WSResultTournamentPlayer getWSResultTournamentPlayer(String tourID, PlayerCache playerCache, boolean useRankingFinished) {
        TeamMemTournament memTournament = memoryMgr.getMemTournament(tourID);
        if (memTournament != null) {
            return memTournament.getWSResultPlayer(playerCache, useRankingFinished);
        }
        TeamTournamentPlayer tourPlayer = getTournamentPlayer(tourID, playerCache.ID);
        if (tourPlayer != null) {
            return tourPlayer.toWSResultTournamentPlayer(playerCache, playerCache.ID);
        }
        return null;
    }

    /**
     * Retun list of result player for tournament
     * @param tour
     * @param offset
     * @param nbMaxResult
     * @param listFollower
     * @param playerAsk
     * @param useRankingFinished
     * @param resultPlayerAsk
     * @return
     */
    public List<WSResultTournamentPlayer> getListWSResultTournamentPlayer(TeamTournament tour, int offset, int nbMaxResult, List<Long> listFollower, long playerAsk, boolean useRankingFinished, WSResultTournamentPlayer resultPlayerAsk) {
        List<WSResultTournamentPlayer> listResult = new ArrayList<>();
        int idxPlayer = -1;
        TeamMemTournament memTour = memoryMgr.getMemTournament(tour.getIDStr());
        if (memTour != null) {
            List<TeamMemTournamentPlayer> listRanking = memTour.getRanking(offset, nbMaxResult, listFollower, useRankingFinished);
            boolean includePlayerAskResult = false;
            if (resultPlayerAsk != null && resultPlayerAsk.getNbDealPlayed() < memTour.getNbDeals() && useRankingFinished) {
                // case of ranking with only players finished and player asked has not finished => include the player result in the list and increment nb player
                includePlayerAskResult = true;
            }
            boolean resultPlayerAskAdded = false;
            for (TeamMemTournamentPlayer e : listRanking) {
                // only player with at least one deal finished
                if (e.getNbPlayedDeals() > 0) {
                    WSResultTournamentPlayer resultPlayer = e.toWSResultTournamentPlayer(useRankingFinished);
                    // process for include player ask (tournament not finished and ranking with only finished results)
                    if (includePlayerAskResult && resultPlayer.getResult() <= resultPlayerAsk.getResult() && !resultPlayerAskAdded) {
                        idxPlayer = listResult.size();
                        listResult.add(resultPlayerAsk);
                        resultPlayerAskAdded = true;
                    }
                    PlayerCache pc = playerCacheMgr.getPlayerCache(e.playerID);
                    resultPlayer.setPlayerPseudo(pc.getPseudo());
                    resultPlayer.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(pc.ID));
                    if (pc.ID == playerAsk) {
                        resultPlayer.setAvatarPresent(pc.avatarPresent);
                    } else {
                        resultPlayer.setAvatarPresent(pc.avatarPublic);
                    }
                    resultPlayer.setCountryCode(pc.countryCode);
                    resultPlayer.setPlayerSerie(pc.serie);
                    if (resultPlayer.getPlayerID() == playerAsk) {
                        idxPlayer = listResult.size();
                    }
                    listResult.add(resultPlayer);
                }
            }
            // player result must be added but not yet done
            if (includePlayerAskResult && !resultPlayerAskAdded && listResult.size() < nbMaxResult) {
                if (listResult.size() > 0) {
                    WSResultTournamentPlayer e = listResult.get(listResult.size()-1);
                    if (e.getResult()> resultPlayerAsk.getResult()) {
                        idxPlayer = listResult.size();
                        listResult.add(resultPlayerAsk);
                    }
                }
            }
        } else {
            // get data from DB
            Query q= new Query();
            Criteria cTour = Criteria.where("tournament.$id").is(new ObjectId(tour.getIDStr()));
            if (listFollower != null && listFollower.size() > 0) {
                cTour.andOperator(Criteria.where("playerID").in(listFollower));
            }
            q.addCriteria(cTour);
            q.limit(nbMaxResult).skip(offset);
            q.with(new Sort(Sort.Direction.ASC, "rank"));
            List<TeamTournamentPlayer> listTP = mongoTemplate.find(q, TeamTournamentPlayer.class);
            if (listTP != null && listTP.size() > 0) {
                for (TeamTournamentPlayer tp : listTP) {
                    WSResultTournamentPlayer rp = tp.toWSResultTournamentPlayer(playerCacheMgr.getPlayerCache(tp.getPlayerID()), playerAsk);
                    if (rp.getPlayerID() == playerAsk) {
                        idxPlayer = listResult.size();
                    }
                    listResult.add(rp);
                }
            }
        }
        if (resultPlayerAsk != null && listResult.size() > 0 && idxPlayer != -1) {
            for (int i = 0; i < listResult.size(); i++) {
                // it's player index or player index is best => stop
                if (i >= idxPlayer) {
                    break;
                }
                // same result and playerID different => swap it !
                if (listResult.get(i).getResult() == resultPlayerAsk.getResult() && listResult.get(i).getPlayerID() != playerAsk) {
                    Collections.swap(listResult, idxPlayer, i);
                    break;
                }
            }
        }
        return listResult;
    }

    /**
     * Return a list of all results for a team on a given tour of the current period. The 4 lead players results + the global team result
     * @param teamID
     * @param tourID
     * @return
     */
    public List<WSTeamTourResult> getTourResultsForTeam(String teamID, int tourID){
        if(getCurrentPeriod() == null || getCurrentPeriod().getCurrentTour() == null || tourID != getCurrentPeriod().getCurrentTourIndex()){
            // Find results in DB
            return getListWSTeamTourResultForTeam(teamID, tourID);
        } else {
            // Find results in memory
            return memoryMgr.getListWSTeamTourResultForTeam(teamID);
        }
    }

    /**
     * Return a list of all results for a team on a given period. All results of the players who played for the team during the period + the global team result
     * @param teamID
     * @return
     */
    public List<WSTeamPeriodResult> getPeriodResultsForTeam(String teamID, String periodID){
        List<WSTeamPeriodResult> teamPeriodResults = new ArrayList<>();
        // If a periodID is given and is different from current period, get the results from DB
        if(periodID != null && !periodID.isEmpty() && !periodID.equalsIgnoreCase(getCurrentPeriod().getID())){
            // First we get the results of all players who played for the team on finished tours (data comes from DB)
            teamPeriodResults = getListWSTeamPeriodResultForTeamFromDB(teamID, periodID);
            // Order the list by points DESC
            Collections.sort(teamPeriodResults, new Comparator<WSTeamPeriodResult>() {
                @Override
                public int compare(WSTeamPeriodResult o1, WSTeamPeriodResult o2) {
                    return o1.compareTo(o2);
                }
            });
            // Add the global team result on the period (player = null in WSTeamTourResult)
            WSTeamPeriodResult teamResult = getTeamResultForPeriod(teamID, periodID);
            if(teamResult != null) teamPeriodResults.add(0, teamResult);
        }
        // If no periodID is given or it's the current period, get the results from DB AND memory
        else {
            // First we get the results of all players who played for the team on finished tours (data comes from DB)
            teamPeriodResults = getListWSTeamPeriodResultForTeamFromDB(teamID, getCurrentPeriod().getID());
            // Then we get the results on current tour (data comes from memory)
            if(getCurrentPeriod() != null && getCurrentPeriod().getCurrentTour() != null){
                List<WSTeamTourResult> playerResultsOnCurrentTour = memoryMgr.getListWSTeamTourResultForTeam(teamID);
                // Loop on the current tour results to add them to previous tours results
                for(WSTeamTourResult tourResult : playerResultsOnCurrentTour){
                    if(tourResult.player != null){
                        boolean playerAlreadyPlayedInPreviousTours = false;
                        // Let's find this player in the previous tours result
                        for(WSTeamPeriodResult periodResult : teamPeriodResults){
                            if(periodResult.player.playerID == tourResult.player.playerID){
                                // Player found ! Let's update his result
                                playerAlreadyPlayedInPreviousTours = true;
                                periodResult.result += tourResult.result;
                                periodResult.points += tourResult.points;
                                periodResult.nbPlayedTours++;
                                break;
                            }
                        }
                        if(!playerAlreadyPlayedInPreviousTours){
                            // Player not found in previous results. Let's add his result
                            WSTeamPeriodResult newResult = new WSTeamPeriodResult();
                            newResult.player = tourResult.player;
                            newResult.points = tourResult.points;
                            newResult.result = tourResult.result;
                            newResult.nbPlayedTours = tourResult.nbPlayedDeals > 0 ? 1 : 0;
                            teamPeriodResults.add(newResult);
                        }
                    }
                }
                // If some players didn't start the current tour yet, add them to the results
                if(playerResultsOnCurrentTour.size() < teamMgr.getNbLeadPlayers()){
                    Team team = teamMgr.findTeamByID(teamID);
                    if(team != null){
                        for(TeamPlayer tp : team.getPlayers()){
                            if(!tp.isSubstitute()){
                                boolean playerStartedTournament = false;
                                for(WSTeamTourResult playerResult : playerResultsOnCurrentTour){
                                    // If team player is found in the current tour results, go on to next player
                                    if(playerResult.player.playerID == tp.getPlayerID()){
                                        playerStartedTournament = true;
                                        break;
                                    }
                                }
                                // If team player hasn't started the current tour, check if he played during the period
                                if(!playerStartedTournament){
                                    boolean playerPlayedDuringPeriod = false;
                                    for(WSTeamPeriodResult teamResult : teamPeriodResults){
                                        if(teamResult.player.playerID == tp.getPlayerID()){
                                            playerPlayedDuringPeriod = true;
                                            break;
                                        }
                                    }
                                    // If player hasn't played in the current period nor in the current tour, add him to the results
                                    if(!playerPlayedDuringPeriod){
                                        WSTeamPeriodResult wsTeamPeriodResult = new WSTeamPeriodResult();
                                        wsTeamPeriodResult.player = teamMgr.toWSTeamPlayer(tp.getPlayerID(), tp.isCaptain(), false, tp.getGroup());
                                        teamPeriodResults.add(wsTeamPeriodResult);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Order the list by points DESC
            Collections.sort(teamPeriodResults, new Comparator<WSTeamPeriodResult>() {
                @Override
                public int compare(WSTeamPeriodResult o1, WSTeamPeriodResult o2) {
                    return o1.compareTo(o2);
                }
            });
            // Add the global team result on the period (player = null in WSTeamTourResult)
            TeamMemDivisionResultTeam teamResult = memoryMgr.getTeamMemDivisionResultForTeam(teamID, null);
            if(teamResult != null) teamPeriodResults.add(0, memoryMgr.toWSTeamPeriodResult(teamResult));
        }
        if(teamPeriodResults == null) teamPeriodResults = new ArrayList<>();
        return teamPeriodResults;
    }

    private List<WSTeamTourResult> getListWSTeamTourResultForTeam(String teamID, int tourID){
        List<WSTeamTourResult> listWSTeamTourResult = new ArrayList<>();
        if (getCurrentPeriod() != null) {
            // Get team players results for the tour
            List<TeamTournamentPlayer> playerResults = getListTournamentPlayerForTeamAndPeriodTour(teamID, buildPeriodTour(getCurrentPeriod().getID(), tourID));
            for (TeamTournamentPlayer playerResult : playerResults) {
                listWSTeamTourResult.add(toWSTeamTourResult(playerResult));
            }
            // Order by group
            Collections.sort(listWSTeamTourResult, new Comparator<WSTeamTourResult>() {
                @Override
                public int compare(WSTeamTourResult o1, WSTeamTourResult o2) {
                    return o1.compareTo(o2);
                }
            });
            // Add the global team result on the tour
            TeamTourTeam teamResult = getTeamTourTeam(teamID, getCurrentPeriod().getID(), tourID);
            if (teamResult != null) {
                int nbTeamsInTour = countNbTeamsInTour(teamResult.getDivision(), getCurrentPeriod().getID(), tourID);
                listWSTeamTourResult.add(0, toWSTeamTourResult(teamResult, nbTeamsInTour));
            }
        }
        return listWSTeamTourResult;
    }

    /**
     * Return a list of WSTeamPeriodResult for a team on a given period FROM DATABASE so only for finished tours. Returns results of all players who played for the team during finished tours.
     * @param teamID
     * @param periodID
     * @return
     */
    private List<WSTeamPeriodResult> getListWSTeamPeriodResultForTeamFromDB(String teamID, String periodID){
        List<WSTeamPeriodResult> listWSTeamPeriodResult = new ArrayList<>();
        TypedAggregation<TeamTournamentPlayer> aggGame = Aggregation.newAggregation(
                TeamTournamentPlayer.class,
                Aggregation.match(Criteria.where("teamID").is(teamID).andOperator(Criteria.where("periodTour").regex(periodID+".+"))),
                Aggregation.group("playerID").first("playerID").as("playerID").count().as("nbPlayedTours").sum("points").as("points").sum("result").as("result")
        );
        AggregationResults<TeamTournamentPlayerAggregate> results = mongoTemplate.aggregate(aggGame, TeamTournamentPlayerAggregate.class);
        if (results != null) {
            for (TeamTournamentPlayerAggregate e : results.getMappedResults()) {
                listWSTeamPeriodResult.add(toWSTeamPeriodResult(e, teamID));
            }
        }
        return listWSTeamPeriodResult;
    }

    public WSTeamPeriodResult toWSTeamPeriodResult(TeamTournamentPlayerAggregate playerResult, String teamID){
        if(playerResult != null){
            // Get Team
            Team team = teamMgr.findTeamByID(teamID);
            if(team != null){
                // Build WSTeamPlayer
                WSTeamPlayer wsTeamPlayer = teamMgr.toWSTeamPlayer(playerResult.playerID, team.isCaptain(playerResult.playerID), false, null);
                // Build WSTeamPeriodResult
                WSTeamPeriodResult wsTeamPeriodResult = new WSTeamPeriodResult();
                wsTeamPeriodResult.player = wsTeamPlayer;
                wsTeamPeriodResult.nbPlayedTours = playerResult.nbPlayedTours;
                wsTeamPeriodResult.points = playerResult.points;
                wsTeamPeriodResult.result = playerResult.result;
                return wsTeamPeriodResult;
            } else {
                log.error("team not found for teamID="+teamID);
            }
        }
        return null;
    }

    public WSTeamPeriodResult toWSTeamPeriodResult(TeamPeriodTeam teamResult){
        if(teamResult != null){
            WSTeamPeriodResult wsTeamPeriodResult = new WSTeamPeriodResult();
            wsTeamPeriodResult.player = null;
            wsTeamPeriodResult.result = teamResult.getResult();
            wsTeamPeriodResult.rank = teamResult.getRank();
            wsTeamPeriodResult.count = teamResult.getNbTeams();
            wsTeamPeriodResult.points = teamResult.getPoints();
            wsTeamPeriodResult.nbPlayedTours = -1;
            return wsTeamPeriodResult;
        }
        return null;
    }

    private WSTeamTourResult toWSTeamTourResult(TeamTournamentPlayer result){
        if(result != null){
            // Get Team
            Team team = teamMgr.findTeamByID(result.getTeamID());
            if(team != null){
                // Build WSTeamPlayer
                WSTeamPlayer wsTeamPlayer = teamMgr.toWSTeamPlayer(result.getPlayerID(), team.isCaptain(result.getPlayerID()), false, result.getTournament().getGroup());
                // Build WSTeamTourResult
                WSTeamTourResult wsTeamTourResult = new WSTeamTourResult();
                wsTeamTourResult.player = wsTeamPlayer;
                wsTeamTourResult.result = result.getResult();
                wsTeamTourResult.rank = result.getRank();
                wsTeamTourResult.points = result.getPoints();
                wsTeamTourResult.count = result.getTournament().getNbPlayers();
                wsTeamTourResult.nbPlayedDeals = result.getTournament().getNbDeals(); // It's a TeamTournamentPlayer stored in DB so all deals have been played
                wsTeamTourResult.nbDeals = result.getTournament().getNbDeals();
                return wsTeamTourResult;
            } else {
                log.error("team not found for teamID="+result.getTeamID());
            }
        }
        return null;
    }

    private WSTeamTourResult toWSTeamTourResult(TeamTourTeam result, int nbTeamsInTour){
        WSTeamTourResult wsTeamTourResult = new WSTeamTourResult();
        if(result != null){
            wsTeamTourResult.player = null;
            wsTeamTourResult.result = result.getResult();
            wsTeamTourResult.rank = result.getRank();
            wsTeamTourResult.points = result.getPoints();
            wsTeamTourResult.count = nbTeamsInTour;
        }
        return wsTeamTourResult;
    }

    /**
     * Return the number of player on tournament
     * @param tour
     * @param listFollower count only among these player
     * @param onlyFinisher
     * @return
     */
    public int getNbPlayersOnTournament(TeamTournament tour, List<Long> listFollower, boolean onlyFinisher) {
        if (tour.isFinished()) {
            if (listFollower != null && listFollower.size() > 0) {
                return (int) mongoTemplate.count(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tour.getIDStr())).andOperator(Criteria.where("playerID").in(listFollower))), TeamTournamentPlayer.class);
            } else {
                return tour.getNbPlayers();
            }
        } else {
            return memoryMgr.getNbPlayersOnTournament(tour.getIDStr(), listFollower, onlyFinisher);
        }
    }



    private int countNbTeamsInTour(String division, String periodID, int tourIndex){
        return (int) mongoTemplate.count(Query.query(Criteria.where("division").is(division).andOperator(Criteria.where("periodID").is(periodID).andOperator(Criteria.where("tourIndex").is(tourIndex)))), TeamTourTeam.class);
    }


    /**
     * Return list of result deal on tournament for player
     * @param tour
     * @param playerID
     * @return
     */
    public List<WSResultDeal> resultListDealForTournamentForPlayer(TeamTournament tour, long playerID) throws FBWSException {
        if (tour == null) {
            log.error("Parameter not valid");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        List<WSResultDeal> listResultDeal = new ArrayList<WSResultDeal>();
        TeamMemTournament mtr = memoryMgr.getMemTournamentDivisionAndTourID(tour.getDivision(), tour.getIDStr());
        for (TeamDeal deal : tour.deals) {
            String dealID = deal.getDealID(tour.getIDStr());
            WSResultDeal resultPlayer = new WSResultDeal();
            resultPlayer.setDealIDstr(dealID);
            resultPlayer.setDealIndex(deal.index);
            resultPlayer.setResultType(Constantes.TOURNAMENT_RESULT_IMP);
            // Tour in memory
            if (mtr != null) {
                TeamMemDeal mrd = mtr.getDealWithID(dealID);
                if (mrd != null) {
                    TeamMemDealPlayer memResPla = mrd.getResultPlayer(playerID);
                    if (memResPla != null) {
                        resultPlayer.setPlayed(true);
                        resultPlayer.setContract(memResPla.getContractWS());
                        resultPlayer.setDeclarer(memResPla.declarer);
                        resultPlayer.setNbTricks(memResPla.nbTricks);
                        resultPlayer.setScore(memResPla.score);
                        resultPlayer.setRank(memResPla.nbPlayerBetterScore+1);
                        resultPlayer.setResult(memResPla.result);
                        resultPlayer.setNbTotalPlayer(mrd.getNbPlayers());
                        resultPlayer.setLead(memResPla.begins);
                    } else {
                        resultPlayer.setPlayed(false);
                        resultPlayer.setRank(-1);
                    }
                }
            }
            // Tour not in memory => search in DB
            else {
                resultPlayer.setNbTotalPlayer(tour.getNbPlayers());
                TeamGame game = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerID);
                if (game != null) {
                    resultPlayer.setPlayed(true);
                    resultPlayer.setContract(game.getContractWS());
                    resultPlayer.setDeclarer(Character.toString(game.getDeclarer()));
                    resultPlayer.setNbTricks(game.getTricks());
                    resultPlayer.setScore(game.getScore());
                    resultPlayer.setRank(game.getRank());
                    resultPlayer.setResult(game.getResult());
                    resultPlayer.setLead(game.getBegins());
                } else {
                    resultPlayer.setPlayed(false);
                    resultPlayer.setRank(-1);
                }
            }
            listResultDeal.add(resultPlayer);
        }
        return listResultDeal;
    }

    /**
     * Return summary at end replay game
     * @param session
     * @param dealID
     * @return
     * @throws FBWSException
     */
    public WSResultReplayDealSummary resultReplaySummary(FBSession session, String dealID) throws FBWSException {
        if (session == null) {
            log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TeamGame replayGame = (TeamGame) session.getCurrentGameTable().getGame();
        if (replayGame == null) {
            log.error("No replay game in session !");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (!replayGame.isReplay()) {
            log.error("Game in session is not a replay game! - game="+replayGame);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        if (!replayGame.getDealID().equals(dealID)) {
            log.error("Replay game in session is for dealID="+replayGame.getDealID()+" - and not for dealID="+dealID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        String tourID = replayGame.getTournament().getIDStr();
        int dealIndex = replayGame.getDealIndex();
        long playerID = session.getPlayer().getID();
        TeamGame originalGame = getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        if (originalGame == null) {
            log.error("Original game not found ! dealID="+dealID+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }

        int nbTotalPlayer = -1;
        double resultOriginal = 0;
        int rankOriginal = 0;
        TeamMemTournament memTournament = memoryMgr.getMemTournament(tourID);
        if (memTournament != null) {
            TeamMemDeal memDeal = memTournament.getDealWithID(dealID);
            if (memDeal == null) {
                log.error("No memDeal found for dealID="+dealID+" - on memTour="+memTournament);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            TeamMemDealPlayer memDealPlayer = memDeal.getResultPlayer(playerID);
            if (memDealPlayer == null) {
                log.error("No memDealPlayer found for player="+playerID+" on memTour="+memTournament+" - memDeal="+memDeal+" - replayGame="+replayGame);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            nbTotalPlayer = memDeal.getNbPlayers();
            resultOriginal = memDealPlayer.result;
            rankOriginal = memDealPlayer.nbPlayerBetterScore+1;
        } else {
            nbTotalPlayer = replayGame.getTournament().getNbPlayers();
            resultOriginal = originalGame.getResult();
            rankOriginal = originalGame.getRank();
        }

        // result original
        WSResultDeal resultPlayer = new WSResultDeal();
        resultPlayer.setDealIDstr(dealID);
        resultPlayer.setDealIndex(dealIndex);
        resultPlayer.setResultType(originalGame.getTournament().getResultType());
        resultPlayer.setNbTotalPlayer(nbTotalPlayer);
        resultPlayer.setContract(originalGame.getContractWS());
        resultPlayer.setDeclarer(Character.toString(originalGame.getDeclarer()));
        resultPlayer.setNbTricks(originalGame.getTricks());
        resultPlayer.setScore(originalGame.getScore());
        resultPlayer.setRank(rankOriginal);
        resultPlayer.setResult(resultOriginal);
        resultPlayer.setLead(originalGame.getBegins());

        // result replay
        WSResultDeal resultReplay = new WSResultDeal();
        resultReplay.setDealIDstr(dealID);
        resultReplay.setDealIndex(dealIndex);
        resultReplay.setResultType(originalGame.getTournament().getResultType());
        resultReplay.setNbTotalPlayer(nbTotalPlayer+1); // + 1 to count replay game !
        resultReplay.setContract(replayGame.getContractWS());
        resultReplay.setDeclarer(Character.toString(replayGame.getDeclarer()));
        resultReplay.setNbTricks(replayGame.getTricks());
        resultReplay.setScore(replayGame.getScore());
        int nbPlayerWithBestScoreReplay = countGamesWithBetterScore(tourID, dealIndex, replayGame.getScore());
        resultReplay.setRank(nbPlayerWithBestScoreReplay + 1);
        resultReplay.setNbPlayerSameGame(countGamesWithSameScoreAndContract(tourID, dealIndex, replayGame.getScore(), replayGame.getContract(), replayGame.getContractType()));
        double replayResult = -1;
        ComputeScoreAverageResult resultScoreAverage = null;
        if (replayGame.getTournament().getResultType() == Constantes.TOURNAMENT_RESULT_PAIRE) {
            int nbPlayerSameScore = countGamesWithSameScore(tourID, dealIndex, replayGame.getScore());
            replayResult = Constantes.computeResultPaire(nbTotalPlayer, nbPlayerWithBestScoreReplay, nbPlayerSameScore);
        } else if (replayGame.getTournament().getResultType() == Constantes.TOURNAMENT_RESULT_IMP) {
            resultScoreAverage = computeScoreAverage(tourID, dealIndex);
            if (resultScoreAverage != null) {
                replayResult = Constantes.computeResultIMP(resultScoreAverage.computeAverageWithAddScore(replayGame.getScore()), replayGame.getScore());
            }
        }
        resultReplay.setResult(replayResult);
        resultReplay.setLead(replayGame.getBegins());

        // result for most played
        WSResultDeal mostPlayedResult = buildWSMostPlayedResult(tourID, dealIndex, nbTotalPlayer, replayGame.getTournament().getResultType());
        if (mostPlayedResult == null) {
            log.error("No result for most played game for tourID="+tourID+" - dealIndex="+dealIndex);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        mostPlayedResult.setDealIDstr(dealID);
        mostPlayedResult.setDealIndex(replayGame.getDealIndex());
        mostPlayedResult.setResultType(originalGame.getTournament().getResultType());
        mostPlayedResult.setNbTotalPlayer(nbTotalPlayer);
        if (!replayGame.getTournament().isFinished()) {
            int nbPlayerWithBestScoreMP = countGamesWithBetterScore(tourID, dealIndex, mostPlayedResult.getScore());
            mostPlayedResult.setRank(nbPlayerWithBestScoreMP + 1);
            double resultMP = -1;
            if (replayGame.getTournament().getResultType() == Constantes.TOURNAMENT_RESULT_PAIRE) {
                int nbPlayerSameScore = countGamesWithSameScore(tourID, dealIndex, mostPlayedResult.getScore());
                resultMP = Constantes.computeResultPaire(nbTotalPlayer, nbPlayerWithBestScoreMP, nbPlayerSameScore);
            } else if (replayGame.getTournament().getResultType() == Constantes.TOURNAMENT_RESULT_IMP) {
                if (resultScoreAverage != null) {
                    resultMP = Constantes.computeResultIMP(resultScoreAverage.scoreAverage, mostPlayedResult.getScore());
                }
            }
            mostPlayedResult.setResult(resultMP);
        }

        WSResultReplayDealSummary replayDealSummary = new WSResultReplayDealSummary();
        replayDealSummary.setResultPlayer(resultPlayer);
        replayDealSummary.setResultMostPlayed(mostPlayedResult);
        replayDealSummary.setResultReplay(resultReplay);
        replayDealSummary.getAttributes().add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, ""+(nbTotalPlayer+1)));
        replayDealSummary.getAttributes().add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_RESULT_TYPE, ""+originalGame.getTournament().getResultType()));
        return replayDealSummary;
    }

    /**
     * Build the resultDeal for the game most played
     * @param tourID
     * @param dealIndex
     * @return
     */
    private WSResultDeal buildWSMostPlayedResult(String tourID, int dealIndex, int nbPlayer, int resultType) {
        WSResultDeal result = new WSResultDeal();
        try {
            Criteria criteria = new Criteria().andOperator(Criteria.where("tournament.$id").is(new ObjectId(tourID)), Criteria.where("dealIndex").is(dealIndex), Criteria.where("finished").is(true));
            TypedAggregation<TeamGame> aggGame = Aggregation.newAggregation(
                    TeamGame.class,
                    Aggregation.match(criteria),
                    Aggregation.project("score", "contract","contractType","declarer","tricks","result","rank").andExpression("substr($cards,0,3)").as("lead"),
                    Aggregation.group("score", "contract", "contractType").addToSet("declarer").as("declarer").addToSet("tricks").as("tricks").addToSet("result").as("result").addToSet("rank").as("rank").count().as("nbPlayer"),
                    Aggregation.sort(Sort.Direction.DESC, "nbPlayer"),
                    Aggregation.limit(1)
            );
            AggregationResults<GameGroupMapping> results = mongoTemplate.aggregate(aggGame, GameGroupMapping.class);
            if (results != null && results.getMappedResults().size() > 0) {
                GameGroupMapping e = results.getMappedResults().get(0);
                if (!e.isLeaved()) {
                    result.setContract(e.getContractWS());
                    result.setDeclarer(Character.toString(e.declarer));
                    result.setNbTricks(e.tricks);
                }
                result.setNbPlayerSameGame(e.nbPlayer);
                result.setResult(e.result);
                result.setRank(e.rank);
                result.setScore(e.score);
                result.setLead(e.lead);
                result.setDealIndex(dealIndex);
                result.setNbTotalPlayer(nbPlayer);
                result.setResultType(resultType);
            }
            else {
                log.error("Result is null or empty for aggregate operation ! - tourID=" + tourID + " - dealIndex=" + dealIndex);
            }
        } catch (Exception e) {
            log.error("Failed to get most game played on tourID="+tourID+" - dealIndex="+dealIndex, e);
        }
        return result;
    }

    /**
     * Return results on deal for a player. Deal must be played by this player. Same contract are grouped
     * @param dealID
     * @param playerCache
     * @param useLead
     * @return
     * @throws FBWSException
     */
    public WSResultDealTournament getWSResultDealTournamentGroupped(String dealID, PlayerCache playerCache, boolean useLead) throws FBWSException {
        TeamTournament tour = getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        TeamDeal deal = (TeamDeal)tour.getDeal(dealID);
        if (deal == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // check deal played by this player
        TeamGame gamePlayer = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
        if (gamePlayer == null) {
            log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
            throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
        }
        WSResultDealTournament resultDealTour = new WSResultDealTournament();
        //**************************************
        // TOURNAMENT Finished => data are in DB
        if (tour.isFinished()) {
            List<GameGroupMapping> listResultMapping = this.getResults(tour.getIDStr(), deal.index, useLead);
            if (listResultMapping == null) {
                log.error("Result is null for aggregate operation !");
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            int idxPlayer = 0;
            boolean idxPlayerFound = false;
            int idxSameRankBefore = -1;
            for (GameGroupMapping e : listResultMapping) {
                WSResultDeal result = new WSResultDeal();
                if (!e.isLeaved()) {
                    result.setContract(e.getContractWS());
                    result.setDeclarer(Character.toString(e.declarer));
                    result.setNbTricks(e.tricks);
                }
                result.setDealIDstr(dealID);
                result.setNbPlayerSameGame(e.nbPlayer);
                result.setResult(e.result);
                result.setRank(e.rank);
                result.setScore(e.score);
                result.setDealIndex(deal.index);
                result.setNbTotalPlayer(tour.getNbPlayers());
                result.setResultType(Constantes.TOURNAMENT_RESULT_IMP);
                result.setLead(e.lead);
                resultDealTour.listResultDeal.add(result);
                if (e.score == gamePlayer.getScore() && e.score == Constantes.GAME_SCORE_LEAVE) {
                    idxPlayerFound = true;
                } else if (e.score == gamePlayer.getScore() && e.getContractWS() != null &&
                        gamePlayer.getContractWS() != null && e.getContractWS().equals(gamePlayer.getContractWS())) {
                    if (useLead) {
                        if (e.lead == null && gamePlayer.getBegins() == null) {
                            idxPlayerFound = true;
                        }
                        else if (e.lead != null && gamePlayer.getBegins() != null && e.lead.equals(gamePlayer.getBegins())) {
                            idxPlayerFound = true;
                        }
                    } else {
                        idxPlayerFound = true;
                    }
                }
                // same rank with player and player position (with contract) not yet found
                if (e.rank == gamePlayer.getRank() && idxSameRankBefore == -1 && !idxPlayerFound) {
                    idxSameRankBefore = idxPlayer;
                }
                if (!idxPlayerFound) {
                    idxPlayer++;
                }
            }
            if (!idxPlayerFound) {
                idxPlayer = -1;
            }

            // line with same rank before
            if (getConfigIntValue("swapRankPlayer", 1) == 1 &&
                    idxPlayer >= 0 && idxSameRankBefore >= 0 &&
                    idxPlayer < resultDealTour.listResultDeal.size() && idxSameRankBefore < resultDealTour.listResultDeal.size() &&
                    idxPlayer > idxSameRankBefore) {
                Collections.swap(resultDealTour.listResultDeal, idxPlayer, idxSameRankBefore);
                idxPlayer = idxSameRankBefore;
            }
            // set tour data
            resultDealTour.tournament = tour.toWS();
            resultDealTour.tournament.remainingTime = 0;

            TeamTournamentPlayer tp = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
            if (tp == null) {
                log.error("No tournament play found for player="+playerCache.ID+" - tournament="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            resultDealTour.tournament.resultPlayer = tp.toWSResultTournamentPlayer(playerCache, playerCache.ID);
            if (resultDealTour.tournament.resultPlayer != null) {
                resultDealTour.tournament.playerOffset = resultDealTour.tournament.resultPlayer.getRank();
            }
            // set attributes
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_PLAYER_CONTRACT, gamePlayer.getContractWS()));
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_INDEX_PLAYER, ""+idxPlayer));
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, ""+tour.getNbPlayers()));
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_DEAL_INDEX, ""+deal.index));
        }
        //**************************************
        // TOURNAMENT NOT FINISHED => FIND RESULT IN MEMORY
        else {
            TeamMemTournament memTournament = memoryMgr.getMemTournamentDivisionAndTourID(tour.getDivision(), tour.getIDStr());
            if (memTournament == null) {
                log.error("No memTournament found for tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            TeamMemDeal memDeal = memTournament.getDealWithID(dealID);
            if (memDeal == null) {
                log.error("No memDeal found for tour="+tour+" - dealID="+dealID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            int rankPlayer = -1;
            List<TeamMemDealPlayer> listResultMem = memDeal.getResultListOrderByScore();
            int nbTotalPlayer = listResultMem.size();
            // Map with nb game with same score and contract
            HashMap<String, Integer> mapRankNbPlayer = new HashMap<>(); /* Map to store the number of player for each score */
            for (TeamMemDealPlayer result : listResultMem) {
                int temp = 0;
                String key = result.score + "-" + result.getContractWS() + "-" + result.declarer;
                if (useLead && result.begins != null) {
                    key += "-" + result.begins;
                }
                if (mapRankNbPlayer.get(key) != null){
                    temp = mapRankNbPlayer.get(key);
                }
                mapRankNbPlayer.put(key, temp + 1);
                if (result.playerID == playerCache.ID) {
                    rankPlayer = result.nbPlayerBetterScore+1;
                }
            }
            List<WSResultDeal> listResult = new ArrayList<>();
            List<String> listScoreContract = new ArrayList<>();
            int idxPlayer = -1;
            int idxSameRankBefore = -1;
            String playerContract = "";
            for (TeamMemDealPlayer resPla : listResultMem) {
                if (resPla.score == Constantes.CONTRACT_LEAVE) {
                    continue;
                }
                String key = resPla.score + "-" + resPla.getContractWS() + "-" + resPla.declarer;
                if (useLead && resPla.begins != null) {
                    key += "-" + resPla.begins;
                }
                if (!listScoreContract.contains(key)) {
                    listScoreContract.add(key);
                    WSResultDeal resDeal = new WSResultDeal();
                    resDeal.setContract(resPla.getContractWS());
                    resDeal.setDealIDstr(memDeal.dealID);
                    resDeal.setDealIndex(memDeal.dealIndex);
                    resDeal.setDeclarer(resPla.declarer);
                    resDeal.setRank(resPla.nbPlayerBetterScore+1);
                    resDeal.setNbTotalPlayer(nbTotalPlayer);
                    resDeal.setLead(resPla.begins);
                    if (mapRankNbPlayer.get(key) != null) {
                        resDeal.setNbPlayerSameGame(mapRankNbPlayer.get(key));
                    }
                    resDeal.setNbTricks(resPla.nbTricks);
                    resDeal.setResultType(Constantes.TOURNAMENT_RESULT_IMP);
                    resDeal.setScore(resPla.score);
                    resDeal.setResult(resPla.result);
                    listResult.add(resDeal);
                    if (resPla.score == gamePlayer.getScore() && resPla.getContractWS().equals(gamePlayer.getContractWS())) {
                        playerContract = resPla.getContractWS();
                        if (useLead) {
                            if (resPla.begins == null && gamePlayer.getBegins() == null) {
                                idxPlayer = listResult.size() - 1;
                            }
                            else if (resPla.begins != null && gamePlayer.getBegins() != null && resPla.begins.equals(gamePlayer.getBegins())) {
                                idxPlayer = listResult.size() - 1;
                            }
                        } else {
                            idxPlayer = listResult.size() - 1;
                        }
                    }
                    if (rankPlayer > 0) {
                        // same rank with player and player position (with contract) not yet found
                        if (resDeal.getRank() == rankPlayer && idxSameRankBefore == -1 && idxPlayer == -1) {
                            idxSameRankBefore = listResult.size()-1;
                        }
                    }
                }
            }
            // line with same rank before
            if (getConfigIntValue("swapRankPlayer", 1) == 1 &&
                    idxPlayer >= 0 && idxSameRankBefore >= 0 &&
                    idxPlayer < listResult.size() && idxSameRankBefore < listResult.size() &&
                    idxPlayer > idxSameRankBefore) {
                Collections.swap(listResult, idxPlayer, idxSameRankBefore);
                idxPlayer = idxSameRankBefore;
            }
            resultDealTour.listResultDeal = listResult;
            resultDealTour.totalSize = listResult.size();
            WSTournament wstour = tour.toWS();
            wstour.remainingTime = tour.getEndDate() - System.currentTimeMillis();
            wstour.nbTotalPlayer = memTournament.getNbPlayersWhoFinished();
            wstour.resultPlayer = memTournament.getWSResultPlayer(playerCache, true);
            if (wstour.resultPlayer != null) {
                wstour.playerOffset = wstour.resultPlayer.getRank();
            }
            wstour.currentDealIndex = memTournament.getCurrentDealForPlayer(playerCache.ID);
            resultDealTour.tournament = wstour;
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_PLAYER_CONTRACT, playerContract));
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_INDEX_PLAYER, ""+idxPlayer));
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, ""+nbTotalPlayer));
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_DEAL_INDEX, ""+deal.index));
        }
        return resultDealTour;
    }

    /**
     * Return results on deal for a player. Deal must be played by this player.
     * @param dealID
     * @param playerCache
     * @param listPlaFilter
     * @param offset
     * @param nbMax
     * @return
     * @throws FBWSException
     */
    @SuppressWarnings("unchecked")
    public WSResultDealTournament getWSResultDealTournament(String dealID, PlayerCache playerCache, List<Long> listPlaFilter, int offset, int nbMax) throws FBWSException {
        TeamTournament tour = getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        TeamDeal deal = (TeamDeal)tour.getDeal(dealID);
        if (tour == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // check deal played by this player
        TeamGame gamePlayer = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
        if (gamePlayer == null) {
            log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
            throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
        }
        WSResultDealTournament wsResultDealTournament = new WSResultDealTournament();
        List<WSResultDeal> listResult = new ArrayList<WSResultDeal>();
        int idxPlayer = -1;
        int indexSameRank = -1; // important to initialize with -1 => indicate no yet done (index of the first same result with same rank)
        boolean plaProcess = false; // flag to indicate that the player result has been processed in the result list
        int nbTotalPlayer = 0;
        WSTournament wstour = toWSTournament(tour, playerCache);
        String playerContract = "";
        boolean firstElementScoreBestCompareToPlayer = false;
        if (listPlaFilter != null) {
            offset = 0;
            nbMax = 0;
        }
        int offsetPlayer = -1;
        //**************************************
        // TOURNAMENT Finished => data are in DB
        if (tour.isFinished()) {
            offsetPlayer = countGamesWithBetterScore(tour.getIDStr(), deal.index, gamePlayer.getScore());
            if (offset == -1 && nbMax > 0) {
                // center on the player result
                offset = gamePlayer.getRank() - (nbMax/2);
            }
            if (offset < 0) {
                offset = 0;
            }
            List<TeamGame> listGame = listGameFinishedOnTournamentAndDeal(tour.getIDStr(), deal.index, offset, nbMax);
            if (listGame != null) {
                for (TeamGame tg : listGame) {
                    if (listResult.size() == 0) {
                        if (tg.getRank() <= offsetPlayer) {
                            firstElementScoreBestCompareToPlayer = true;
                        }
                        if (offset < nbMax && tg.getScore() == gamePlayer.getScore()) {
                            firstElementScoreBestCompareToPlayer = true;
                        }
                    }
                    if (listPlaFilter != null) {
                        if (!listPlaFilter.contains(tg.getPlayerID())) {
                            continue;
                        }
                    }
                    WSResultDeal result = new WSResultDeal();
                    if (!tg.isLeaved()) {
                        result.setContract(tg.getContractWS());
                        result.setDeclarer(Character.toString(tg.getDeclarer()));
                        result.setNbTricks(tg.getTricks());
                    }
                    result.setResult(tg.getResult());
                    result.setRank(tg.getRank());
                    result.setScore(tg.getScore());
                    result.setDealIndex(tg.getDealIndex());
                    result.setNbTotalPlayer(tour.getNbPlayers());
                    result.setResultType(tour.getResultType());
                    result.setLead(tg.getBegins());
                    PlayerCache pc = playerCacheMgr.getPlayerCache(tg.getPlayerID());
                    result.setPlayerPseudo(pc.getPseudo());
                    result.setCountryCode(pc.countryCode);
                    result.setConnected(presenceMgr.isSessionForPlayerID(pc.ID));
                    if (tg.getPlayerID() == playerCache.ID) {
                        result.setAvatarPresent(pc.avatarPresent);
                    } else {
                        result.setAvatarPresent(pc.avatarPublic);
                    }
                    result.setPlayerID(tg.getPlayerID());
                    result.setGameIDstr(tg.getIDStr());
                    if (tg.getPlayerID() == playerCache.ID) {
                        idxPlayer = listResult.size();
                        playerContract = result.getContract();
                        plaProcess = true;
                    }
                    // set index of result with same rank
                    if (!plaProcess && result.getRank() == gamePlayer.getRank() && indexSameRank == -1) {
                        indexSameRank = listResult.size();
                    }
                    listResult.add(result);
                }
                if (listPlaFilter != null) {
                    nbTotalPlayer = listResult.size();
                } else {
                    nbTotalPlayer = tour.getNbPlayers();
                }
            }

        }
        //**************************************
        // TOURNAMENT NOT FINISHED => FIND RESULT IN MEMORY
        else {
            TeamMemTournament memTournament = memoryMgr.getMemTournament(tour.getIDStr());
            if (memTournament == null) {
                log.error("No memTournament found for tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            TeamMemDeal memDeal = memTournament.getDealWithID(dealID);
            if (memDeal == null) {
                log.error("No memDeal found for tour="+tour+" - dealID="+dealID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            TeamMemDealPlayer resPlayer = memDeal.getResultPlayer(playerCache.ID);
            offsetPlayer = -1;
            if (resPlayer != null) {
                offsetPlayer = resPlayer.nbPlayerBetterScore;
            } else {
                log.error("Oups no memResultDeal for player="+playerCache.ID+" - deal="+deal);
            }
            if (offset == -1 && nbMax > 0) {
                // center on the player result
                if (resPlayer != null) {
                    offset = (resPlayer.nbPlayerBetterScore+1) - (nbMax/2);
                }
            }
            if (offset < 0) {
                offset = 0;
            }
            List<TeamMemDealPlayer> listResultMem = memDeal.getResultListOrderByScore();
            if (nbMax > 0) {
                if (offset < listResultMem.size()) {
                    listResultMem = listResultMem.subList(offset, (offset+nbMax) < listResultMem.size()?(offset+nbMax):listResultMem.size());
                }
            }
            for (TeamMemDealPlayer resPla : listResultMem) {
                if (listResult.isEmpty()) {
                    if (resPla.nbPlayerBetterScore < offsetPlayer) {
                        firstElementScoreBestCompareToPlayer = true;
                    }
                    if (offset < nbMax && resPla.score == gamePlayer.getScore()) {
                        firstElementScoreBestCompareToPlayer = true;
                    }
                }
                if (listPlaFilter != null) {
                    if (!listPlaFilter.contains(resPla.playerID)) {
                        continue;
                    }
                }
                WSResultDeal resDeal = new WSResultDeal();
                resDeal.setContract(resPla.getContractWS());
                resDeal.setDealIDstr(memDeal.dealID);
                resDeal.setDealIndex(memDeal.dealIndex);
                resDeal.setDeclarer(resPla.declarer);
                resDeal.setRank(resPla.nbPlayerBetterScore+1);
                resDeal.setNbTotalPlayer(memDeal.getNbPlayers());
                resDeal.setNbTricks(resPla.nbTricks);
                resDeal.setResultType(tour.getResultType());
                resDeal.setScore(resPla.score);
                resDeal.setResult(resPla.result);
                resDeal.setPlayerID(resPla.playerID);
                resDeal.setLead(resPla.begins);
                PlayerCache pc = playerCacheMgr.getPlayerCache(resPla.playerID);
                resDeal.setPlayerPseudo(pc.getPseudo());
                resDeal.setCountryCode(pc.countryCode);
                resDeal.setConnected(presenceMgr.isSessionForPlayerID(pc.ID));
                if (resPla.playerID == playerCache.ID) {
                    resDeal.setAvatarPresent(pc.avatarPresent);
                } else {
                    resDeal.setAvatarPresent(pc.avatarPublic);
                }
                resDeal.setGameIDstr(resPla.gameID);
                if (resPla.playerID == playerCache.ID) {
                    idxPlayer = listResult.size();
                    playerContract = resDeal.getContract();
                    plaProcess = true;
                }
                // set index of result with same rank
                if (!plaProcess && indexSameRank == -1 && resDeal.getScore() == gamePlayer.getScore()) {
                    indexSameRank = listResult.size();
                }
                listResult.add(resDeal);
            }
            if (listPlaFilter != null) {
                nbTotalPlayer = listResult.size();
            } else {
                nbTotalPlayer = memDeal.getNbPlayers();
            }
        }
        // player is in the list and not the first with the same result => swap !
        if (indexSameRank != -1 && idxPlayer >=0 && idxPlayer > indexSameRank) {
            if (offsetPlayer >= 0 && offset <= offsetPlayer) {
                try {
                    Collections.swap(listResult, idxPlayer, indexSameRank);
                    idxPlayer = indexSameRank;
                } catch (Exception e) {
                    log.error("Failed to swap : idxPlayer=" + idxPlayer + " - indexSameRank=" + indexSameRank + " - listResult size=" + listResult.size(), e);
                }
            } else if (offsetPlayer >= 0){
                // place of player is behind ! (with offset <= offsetPlayer) => so swap the player position with the first at the same result
                TeamGame gameToReplace = getFirstGameOnTournamentAndDealWithScoreAndContract(tour.getIDStr(), deal.index, gamePlayer.getScore(), gamePlayer.getContract(), gamePlayer.getContractType());
                if (gameToReplace != null) {
                    WSResultDeal resDealPla = listResult.get(idxPlayer);
                    PlayerCache pc = playerCacheMgr.getPlayerCache(gameToReplace.getPlayerID());
                    if (resDealPla != null && pc != null) {
                        resDealPla.setContract(gameToReplace.getContractWS());
                        resDealPla.setAvatarPresent(pc.avatarPublic);
                        resDealPla.setDeclarer(Character.toString(gameToReplace.getDeclarer()));
                        resDealPla.setNbTricks(gameToReplace.getTricks());
                        resDealPla.setCountryCode(pc.countryCode);
                        resDealPla.setGameIDstr(gameToReplace.getIDStr());
                        resDealPla.setPlayerID(pc.ID);
                        resDealPla.setPlayerPseudo(pc.getPseudo());
                        resDealPla.setLead(gameToReplace.getBegins());
                    }
                }
                idxPlayer = -1;
            }
        }
        // player is not in the list but same result present & first element score > player result => swap !
        else if (indexSameRank != -1 && idxPlayer == -1 && firstElementScoreBestCompareToPlayer) {
            // replace some data at this index with player data
            if (indexSameRank >= 0 && indexSameRank < listResult.size()) {
                WSResultDeal resDealPla = listResult.get(indexSameRank);
                if (resDealPla != null) {
                    resDealPla.setContract(gamePlayer.getContractWS());
                    resDealPla.setAvatarPresent(playerCache.avatarPresent);
                    resDealPla.setDeclarer(Character.toString(gamePlayer.getDeclarer()));
                    resDealPla.setNbTricks(gamePlayer.getTricks());
                    resDealPla.setCountryCode(playerCache.countryCode);
                    resDealPla.setGameIDstr(gamePlayer.getIDStr());
                    resDealPla.setPlayerID(playerCache.ID);
                    resDealPla.setPlayerPseudo(playerCache.getPseudo());
                    resDealPla.setLead(gamePlayer.getBegins());
                    idxPlayer = indexSameRank;
                }
            }
        }
        wsResultDealTournament.offset = offset;
        wsResultDealTournament.totalSize = nbTotalPlayer;
        wsResultDealTournament.listResultDeal = listResult;
        wsResultDealTournament.tournament = wstour;
        wsResultDealTournament.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_PLAYER_CONTRACT, playerContract));
        wsResultDealTournament.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_INDEX_PLAYER, ""+idxPlayer));
        wsResultDealTournament.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_DEAL_INDEX, ""+deal.index));
        wsResultDealTournament.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, ""+nbTotalPlayer));
        return wsResultDealTournament;
    }

    /**
     * Count games for player
     * @param playerID
     * @return
     */
    public int countGamesForPlayer(long playerID) {
        try {
            Query q = new Query();
            Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            Criteria cFinished = Criteria.where("finished").is(true);
            q.addCriteria(new Criteria().andOperator(cPlayerID, cFinished));
            return (int) mongoTemplate.count(q, TeamGame.class);
        } catch (Exception e) {
            log.error("Failed to count games for player - playerID="+ playerID, e);
        }
        return 0;
    }

    /**
     * Count game with score > param value
     * @param tournamentID
     * @param dealIndex
     * @param score
     * @return
     */
    public int countGamesWithBetterScore(String tournamentID, int dealIndex, int score) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            Criteria cFinished = Criteria.where("finished").is(true);
            Criteria cScore = Criteria.where("score").gt(score);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cFinished, cScore));
            return (int) mongoTemplate.count(q, TeamGame.class);
        } catch (Exception e) {
            log.error("Failed to count game with better score - tourID="+ tournamentID +" - dealIndex="+dealIndex+" - score="+score, e);
        }
        return -1;
    }

    /**
     * Count game with score = param value
     * @param tourID
     * @param dealIndex
     * @param score
     * @return
     */
    public int countGamesWithSameScore(String tourID, int dealIndex, int score) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            Criteria cFinished = Criteria.where("finished").is(true);
            Criteria cScore = Criteria.where("score").is(score);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cFinished, cScore));
            return (int) mongoTemplate.count(q, TeamGame.class);
        } catch (Exception e) {
            log.error("Failed to count game with same score - tourID="+tourID+" - dealIndex="+dealIndex+" - score="+score, e);
        }
        return -1;
    }

    /**
     * Count game with score = param value
     * @param tourID
     * @param dealIndex
     * @param score
     * @return
     */
    public int countGamesWithSameScoreAndContract(String tourID, int dealIndex, int score, String contract, int contractType) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            Criteria cFinished = Criteria.where("finished").is(true);
            Criteria cScore = Criteria.where("score").is(score);
            Criteria cContract = Criteria.where("contract").is(contract);
            Criteria cContractType = Criteria.where("contractType").is(contractType);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cFinished, cScore, cContract, cContractType));
            return (int) mongoTemplate.count(q, TeamGame.class);
        } catch (Exception e) {
            log.error("Failed to count game with same score and contract - tourID="+tourID+" - dealIndex="+dealIndex+" - score="+score+" - contract="+contract+" - contractType="+contractType, e);
        }
        return -1;
    }

    /**
     * Compute score average on deal (only use game finished with score != LEAVE)
     * @param tournamentID
     * @param dealIndex
     * @return
     */
    public ComputeScoreAverageResult computeScoreAverage(String tournamentID, int dealIndex) {
        try {
            Criteria criteria = new Criteria().andOperator(
                    Criteria.where("tournament.$id").is(new ObjectId(tournamentID)),
                    Criteria.where("dealIndex").is(dealIndex),
                    Criteria.where("finished").is(true),
                    Criteria.where("score").ne(Constantes.GAME_SCORE_LEAVE));
            TypedAggregation<TeamGame> aggGame = Aggregation.newAggregation(
                    TeamGame.class,
                    Aggregation.match(criteria),
                    Aggregation.group().avg("score").as("scoreAverage").count().as("nbScore")
            );
            AggregationResults<Document> results = mongoTemplate.aggregate(aggGame, Document.class);
            if (results != null && results.getMappedResults().size() > 0) {
                Document e = results.getMappedResults().get(0);
                ComputeScoreAverageResult result = new ComputeScoreAverageResult();
                result.scoreAverage = e.getDouble("scoreAverage").intValue();
                result.nbScore = e.getInteger("nbScore");
                return result;
            }
            else {
                log.error("Result is null or empty for aggregate operation ! - tournamentID=" + tournamentID + " - dealIndex=" + dealIndex);
            }
        } catch (Exception e) {
            log.error("Failed to computeScoreAverage on tournamentID="+tournamentID+" - dealIndex="+dealIndex, e);
        }
        return null;
    }

    /**
     * Return the first game with these parameters
     * @param tourID
     * @param dealIndex
     * @param score
     * @param contract
     * @param contractType
     * @return
     */
    public TeamGame getFirstGameOnTournamentAndDealWithScoreAndContract(String tourID, int dealIndex, int score, String contract, int contractType) {
        try {
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            Criteria cFinished = Criteria.where("finished").is(true);
            Criteria cContract = Criteria.where("contract").is(contract);
            Criteria cContractType = Criteria.where("contractType").is(contractType);
            Criteria cScore = Criteria.where("score").is(score);
            Query q = new Query();
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cFinished, cScore, cContract, cContractType));
            q.with(new Sort(Sort.Direction.DESC, "score"));
            return mongoTemplate.findOne(q, TeamGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament deal and playerID", e);
        }
        return null;
    }

    /**
     * List archive for player
     * @param playerID
     * @param offset
     * @param nbMax
     * @return
     * @throws FBWSException
     */
    public ResultListTournamentArchive listTournamentArchive(long playerID, int offset, int nbMax) throws FBWSException {
        ResultListTournamentArchive result = new ResultListTournamentArchive();
        result.archives = new ArrayList<>();
        Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (getConfigIntValue("resultArchiveNbDayBefore", 60)));
        long dateRef = dateBefore.getTimeInMillis();
        int offsetBDD = 0;
        result.nbTotal = (int)mongoTemplate.count(Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef))), TeamTournamentPlayer.class);
        TeamMemTournamentPlayer memTournamentPlayer = memoryMgr.getTournamentPlayer(playerID);
        if (memTournamentPlayer != null) {
            if (offset > 0) {
                // not used data from memory ...
                offsetBDD = offset - 1;
            } else {
                WSTournamentArchive wst = toWSTournamentArchive(memTournamentPlayer);
                if (wst != null) {
                    result.archives.add(wst);
                    result.nbTotal++;
                }
            }
        }

        // if list not full, use data from BDD
        if (result.archives.size() < nbMax) {
            if (offsetBDD < 0) {
                offsetBDD = 0;
            }
            List<TeamTournamentPlayer> listTP = listTournamentPlayerAfterDate(playerID, dateRef, offsetBDD, nbMax - result.archives.size());
            if (listTP != null) {
                for (TeamTournamentPlayer tp :listTP) {
                    if (result.archives.size() == nbMax) { break;}
                    WSTournamentArchive wst = toWSTournamentArchive(tp);
                    if (wst != null) {
                        result.archives.add(wst);
                    }
                }
            }
        }
        return result;
    }

    public WSTournamentArchive toWSTournamentArchive(TeamMemTournamentPlayer e) {
        if (e != null) {
            WSTournamentArchive ws = new WSTournamentArchive();
            ws.date = e.dateStart;
            ws.nbPlayers = e.memTour.getNbPlayersWhoFinished();
            ws.name = e.memTour.name;
            if (e.isPlayerFinish()) {
                ws.rank = e.rankingFinished;
            } else {
                ws.rank = -1;
            }
            ws.result = e.result;
            ws.resultType = Constantes.TOURNAMENT_RESULT_IMP;
            ws.tournamentID = e.memTour.tourID;
            if (getCurrentPeriod() != null) {
                ws.teamPeriodID = getCurrentPeriod().getID();
                ws.tourID = getCurrentPeriod().getCurrentTourIndex();
            }
            ws.teamPoints = e.points;
            ws.finished = e.isPlayerFinish();
            ws.countDeal = e.memTour.getNbDeals();
            ws.listPlayedDeals = new ArrayList<>(e.playedDeals);
            Collections.sort(ws.listPlayedDeals);
            return ws;
        }
        return null;
    }

    public WSTournamentArchive toWSTournamentArchive(TeamTournamentPlayer e) {
        if (e != null) {
            WSTournamentArchive ws = new WSTournamentArchive();
            ws.date = e.getStartDate();
            ws.nbPlayers = e.getTournament().getNbPlayers();
            ws.name = e.getTournament().getName();
            ws.rank = e.getRank();
            ws.result = e.getResult();
            ws.resultType = e.getTournament().getResultType();
            ws.tournamentID = e.getTournament().getIDStr();
            ws.finished = true;
            ws.countDeal = e.getTournament().getNbDeals();
            ws.listPlayedDeals = new ArrayList<>();
            ws.teamPoints = e.getPoints();
            ws.teamPeriodID = extractPeriodIDFromPeriodTour(e.getPeriodTour());
            ws.tourID = extractTourIDFromPeriodTour(e.getPeriodTour());
            for (Deal d : e.getTournament().getDeals()) {
                ws.listPlayedDeals.add(d.getDealID(e.getTournament().getIDStr()));
            }
            Collections.sort(ws.listPlayedDeals);
            return ws;
        }
        return null;
    }

    /**
     * List tournamentPlayer for a player and after a date. Order by startDate desc
     * @param playerID
     * @param dateRef
     * @param offset
     * @param nbMax
     * @return
     */
    public List<TeamTournamentPlayer> listTournamentPlayerAfterDate(long playerID, long dateRef, int offset, int nbMax) {
        return mongoTemplate.find(
                Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef))).
                        skip(offset).
                        limit(nbMax).
                        with(new Sort(Sort.Direction.DESC, "startDate")),
                TeamTournamentPlayer.class);
    }

    /**
     * Leave the current tournament. All deals not played are set to leave
     * @param session
     * @param tourIDstr
     * @return
     * @throws FBWSException
     */
    public int leaveTournament(FBSession session, String tourIDstr) throws FBWSException {
        if (session == null) {
            log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        Table table = session.getCurrentGameTable();
        if (table == null || table.getGame() == null) {
            log.error("Game or table is null in session table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (table.getTournament() == null || !(table.getTournament() instanceof TeamTournament)) {
            log.error("Touranment on table is not TEAM table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!(table.getGame() instanceof TeamGame)) {
            log.error("Game on table is not TEAM table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TeamGame game = (TeamGame)session.getCurrentGameTable().getGame();
        if (game == null) {
            log.error("Game is null in session loginID="+session.getLoginID());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // no leave for replay
        if (game.isReplay()) {
            log.error("No leave for replay game="+game);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!game.getTournament().getIDStr().equals(tourIDstr)) {
            log.error("Tournament of current game is not same as tourIDstr="+tourIDstr+" - game="+game);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        checkGame(game);
        Player p = session.getPlayer();
        Team team = teamMgr.getTeamForPlayer(p.getID());
        if (team == null) {
            log.error("No team found for player="+p);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_NO_TEAM);
        }
        TeamPlayer teamPlayer = team.getPlayer(p.getID());
        if (teamPlayer == null) {
            log.error("No teamPlayer found in team=["+team+"] for player=["+p+"]");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (teamPlayer.isSubstitute()) {
            log.error("Player is subsitute ! team="+team+" - teamPlayer="+teamPlayer);
            throw new FBWSException(FBExceptionType.TEAM_PLAYER_SUBSTITUTE);
        }
        if (teamPlayer.getGroup() == null) {
            log.error("Player group is null ! team="+team+" - teamPlayer="+teamPlayer);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // check if player has enough credit to leave tournament
        int nbDealToLeave = game.getTournament().getNbDeals() - table.getNbPlayedDeals() - 1; // -1 because credit already decrement for current game !
        if (nbDealToLeave > 0) {
            playerMgr.checkPlayerCredit(p, game.getTournament().getNbCreditsPlayDeal() * nbDealToLeave);
        }

        // leave current game
        synchronized (getGameMgr().getLockOnGame(game.getIDStr())) {
            if (game.getBidContract() == null) {
                // no contract => leave
                game.setLeaveValue();
            } else {
                // contract exist => claim with 0 tricks
                game.claimForPlayer(table.getPlayerPosition(session.getPlayer().getID()), 0);
                game.setFinished(true);
                game.setLastDate(Calendar.getInstance().getTimeInMillis());
                // compute score
                GameMgr.computeScore(game);
            }
            table.addPlayedDeal(game.getDealID());
            // update data game and table in DB
            updateGameDB(game);
            // update tournament play in memory
            memoryMgr.addResult(game);
        }
        // leave game for other deals
        List<TeamGame> listOtherGamesLeaved = createGamesLeaved(session.getPlayer().getID(), team.getIDStr(), game.getTournament(), game.getDealIndex()+1, session.getDeviceID());
        int nbDealNotPlayed = 0;
        if (listOtherGamesLeaved != null) {
            for (TeamGame gameLeave : listOtherGamesLeaved) {
                memoryMgr.addResult(gameLeave);
                table.addPlayedDeal(gameLeave.getDealID());
                nbDealNotPlayed++;
            }
        }

        // remove game from session
        session.removeGame();
        // remove player from set
        gameMgr.removePlayerRunning(p.getID());

        // update player data
        if (nbDealNotPlayed > 0) {
            playerMgr.updatePlayerCreditDeal(p, game.getTournament().getNbCreditsPlayDeal()*nbDealNotPlayed, nbDealNotPlayed);
            session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_TEAM, nbDealNotPlayed);
        }

        // return credit remaining of player
        return p.getCreditAmount();
    }

    /**
     * Create a game for player on tournament and dealIndex.
     * @param playerID
     * @param teamID
     * @param tour
     * @param dealIndexToStartLeave
     * @param deviceID
     * @return
     * @throws FBWSException if an existing game existed for player, tournament and dealIndex or if an exception occurs
     */
    private List<TeamGame> createGamesLeaved(long playerID, String teamID, TeamTournament tour, int dealIndexToStartLeave, long deviceID) throws FBWSException {
        synchronized (lockCreateGame.getLock(tour.getIDStr()+"-"+playerID)) {
            List<TeamGame> listGameLeaved = new ArrayList<>();
            int dealIdx = dealIndexToStartLeave;
            while (dealIdx <= tour.getNbDeals()) {
                if (getGameOnTournamentAndDealForPlayer(tour.getIDStr(), dealIdx, playerID) == null) {
                    TeamGame game = new TeamGame(playerID, tour, dealIdx, teamID);
                    game.setStartDate(Calendar.getInstance().getTimeInMillis());
                    game.setLastDate(Calendar.getInstance().getTimeInMillis());
                    game.setDeviceID(deviceID);
                    game.setLeaveValue();
                    listGameLeaved.add(game);
                    dealIdx++;
                }
            }
            if (listGameLeaved.size() > 0) {
                try {
                    // try to find bug of not valid game ...
                    if (getConfigIntValue("findBugGameNotValid", 0) == 1) {
                        for (TeamGame game : listGameLeaved) {
                            if (game.isFinished() && game.getContractType() == 0 && game.getBids().length() == 0) {
                                log.error("Game not valid !!! - game=" + game);
                                Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                                log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                            }
                        }
                    }
                    mongoTemplate.insertAll(listGameLeaved);
                } catch (Exception e) {
                    log.error("Exception to create leave games for player=" + playerID + " - tour=" + tour + " - dealIndexToStartLeave=" + dealIndexToStartLeave, e);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            }
            return listGameLeaved;
        }
    }

    /**
     * Prepare data to replay a deal
     * @param player
     * @param gamePlayed
     * @param conventionProfil
     * @param conventionValue
     * @param cardsConventionProfil
     * @param cardsConventionValue
     * @return
     * @throws FBWSException
     */
    public Table createReplayTable(Player player, Game gamePlayed, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) throws FBWSException {
        if (player == null) {
            log.error("Param player is null");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        // create a table
        Table table = new Table(player, gamePlayed.getTournament());
        table.setReplay(true);
        // create game
        TeamGame replayGame = new TeamGame(player.getID(), (TeamTournament)gamePlayed.getTournament(), gamePlayed.getDealIndex(), null);
        replayGame.setReplay(true);
        synchronized (this) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                log.error("Exception to sleep to generate new replay gameID", e);
            }
            replayGame.setReplayGameID(""+System.currentTimeMillis());
        }
        replayGame.setConventionSelection(conventionProfil, conventionValue);
        replayGame.setCardsConventionSelection(cardsConventionProfil, cardsConventionValue);
        table.setGame(replayGame);
        return table;
    }

    @Override
    public Game createReplayGame(long playerID, Tournament tour, int dealIndex, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) {
        return null;
    }

    /**
     * Compute the division evolution from the original accoring to evolution (+1, 0 or -1)
     * @param divisionOriginal
     * @param evolution
     * @return
     */
    public static String computeDivisionEvolution(String divisionOriginal, int evolution) {
        if (evolution == 0) {
            return divisionOriginal;
        }
        // first division and evolution > 0 => always first division
        if (divisionOriginal.equals(DIVISION_TAB[0]) && evolution > 0) {
            return DIVISION_TAB[0];
        }
        // last division and evolution < 0 => always last division
        if (divisionOriginal.equals(DIVISION_TAB[DIVISION_TAB.length-1]) && evolution < 0) {
            return DIVISION_TAB[DIVISION_TAB.length-1];
        }

        for (int i=0; i < DIVISION_TAB.length; i++){
            if (divisionOriginal.equals(DIVISION_TAB[i])) {
                if (evolution > 0 && i > 0) {
                    return DIVISION_TAB[i-1];
                }
                else if (evolution < 0 && i < (DIVISION_TAB.length-1)) {
                    return DIVISION_TAB[i+1];
                }
                return divisionOriginal;
            }
        }
        return divisionOriginal;
    }

    /**
     *  Upgrade handicap value and set group for all teams
     */
    public void upgradeHandicapAndGroupForAllTeams() {
        for (String division : TourTeamMgr.DIVISION_TAB) {
            List<Team> teams = teamMgr.getTeamsForDivision(division);
            for (Team t : teams) {
                upgradeHandicapAndGroupForTeam(t, true);
            }
            log.warn("Upgrade teams for division="+division+" - nb teams="+teams.size());
        }
    }

    /**
     * Upgrade handicap value and set group for each player
     * @param team
     * @param saveDB
     */
    public void upgradeHandicapAndGroupForTeam(Team team, boolean saveDB) {
        if (team != null) {
            // get players handicap
            List<PlayerHandicap> listPlayerHandicap = playerMgr.getPlayersHandicap(team.getListPlayerID());
            // update handicap for each teamPlayer
            for (TeamPlayer e : team.getPlayers()) {
                double value = 0;
                for (PlayerHandicap ph : listPlayerHandicap) {
                    if (ph.getPlayerId() == e.getPlayerID()) {
                        value = ph.getHandicap();
                        break;
                    }
                }
                e.setHandicap(value);
            }

            // sort list of players according to handicap
            Collections.sort(team.getPlayers(), new Comparator<TeamPlayer>() {
                @Override
                public int compare(TeamPlayer o1, TeamPlayer o2) {
                    if (o1.getHandicap() == o2.getHandicap()) {
                        // same handicap => at first older player in the team
                        return Long.compare(o1.getDateJoinedTeam(), o2.getDateJoinedTeam());
                    }
                    // at first player with low handicap
                    return Double.compare(o1.getHandicap(), o2.getHandicap());
                }
            });

            // set group for each players
            int idx = 0;
            if (log.isDebugEnabled()) {
                log.debug("Set group for each players for Team : "+team);
            }
            for (TeamPlayer e : team.getPlayers()) {
                String group = null;
                if (!e.isSubstitute()) {
                    if (idx < TourTeamMgr.GROUP_TAB.length) {
                        group = TourTeamMgr.GROUP_TAB[idx];
                    }
                    e.setGroup(group);
                    if (log.isDebugEnabled()) {
                        log.debug("Group "+group+" -> teamPlayer="+e);
                    }
                    idx++;
                }
            }

            // update value in memory
            TeamMemDivisionResult divisionResult = memoryMgr.getMemDivisionResult(team.getDivision());
            if (divisionResult != null) {
                TeamMemDivisionResultTeam resultTeam = divisionResult.getTeamResult(team.getIDStr());
                if (resultTeam != null) {
                    resultTeam.averageHandicap = team.getAverageHandicap();
                }
            }

            // save team object to DB
            if (saveDB) {
                mongoTemplate.save(team);
            }
        }
    }

    /**
     * For a division, find the team with the lowest average performance
     * @param division
     * @return
     */
    public Team getTeamWithLowestAveragePerformanceForDivision(String division) {
        Team teamLowestPerformance = null;
        double value = 0;
        if (division != null && currentPeriod != null) {
            List<Team> teams = teamMgr.getActiveTeamsForDivision(division, currentPeriod.getID());
            for (Team t : teams) {
                if (teamLowestPerformance == null) {
                    teamLowestPerformance = t;
                    value = teamLowestPerformance.getAverageHandicap();
                }
                else if (t.getAverageHandicap() < value) {
                    teamLowestPerformance = t;
                    value = teamLowestPerformance.getAverageHandicap();
                }
            }
        }
        return teamLowestPerformance;
    }

    public List<TeamTourTeamAggregatePoint> listTeamPointsForDivisionAndPeriod(String division, String periodID) {
        TypedAggregation<TeamTourTeam> aggTourTeam = Aggregation.newAggregation(
                TeamTourTeam.class,
                Aggregation.match(Criteria.where("division").is(division).andOperator(Criteria.where("periodID").is(periodID))),
                Aggregation.group("teamID").sum("points").as("nbPoints").sum("result").as("cumulResult"),
                Aggregation.project("nbPoints", "cumulResult").and("_id").as("teamID"),
                Aggregation.sort(Sort.Direction.DESC, "nbPoints")
        );
        AggregationResults<TeamTourTeamAggregatePoint> results = mongoTemplate.aggregate(aggTourTeam, TeamTourTeamAggregatePoint.class);
        return results.getMappedResults();
    }

    /**
     * Build division history for team
     * @param team
     * @return
     */
    public String getDivisionHistoryForTeam(Team team) {
        String historic = "";
        if (isChampionshipStarted()) {
            // retrieve period with activity for team
            Query query = Query.query(Criteria.where("teamID").is(team.getIDStr()));
            query.with(new Sort(Sort.Direction.ASC, "periodID"));
            List<TeamPeriodTeam> listPeriodResult = mongoTemplate.find(query, TeamPeriodTeam.class);

            if (listPeriodResult != null && listPeriodResult.size() > 0) {
                // list all period finished
                query = new Query(Criteria.where("finished").is(true)).with(new Sort(Sort.Direction.ASC, "periodID"));
                List<TeamPeriod> listPeriod = mongoTemplate.find(query, TeamPeriod.class);
                // for each period, set the division associated
                List<String> listPeriodTeam = new ArrayList<>();
                int idxPeriod = 0;
                // Start at first period played
                for (TeamPeriodTeam e : listPeriodResult) {
                    // loop for period not activity
                    while (idxPeriod < listPeriod.size()) {
                        String period = listPeriod.get(idxPeriod).getID();
                        idxPeriod++;
                        // Activity on this period ?
                        if (period.equals(e.getPeriodID())) {
                            break;
                        }
                        if (listPeriodTeam.isEmpty()) {
                            continue;
                        }
                        // Team has no activity => DNO
                        listPeriodTeam.add(DIVISION_NO);
                    }

                    listPeriodTeam.add(e.getDivision());
                }

                // complete with last period if necessary
                while (idxPeriod < listPeriod.size()) {
                    idxPeriod++;
                    if (listPeriodTeam.isEmpty()) {
                        break;
                    }
                    // Team has no activity => DNO
                    listPeriodTeam.add(DIVISION_NO);
                }

                // add current period
                listPeriodTeam.add(team.getDivision());

                // transform list to historic string and group period with same division
                String previous = "";
                int nbSame = 0;
                for (String s : listPeriodTeam) {
                    if (s.equals(previous)) {
                        nbSame++;
                    } else {
                        if (previous.length() > 0 && nbSame > 0) {
                            if (historic.length() > 0) {
                                historic += ";";
                            }
                            historic += previous + "-" + nbSame;
                        }
                        previous = s;
                        nbSame = 1;
                    }
                }
                if (previous.length() > 0 && nbSame > 0) {
                    if (historic.length() > 0) {
                        historic += ";";
                    }
                    historic += previous + "-" + nbSame;
                }
            }
        }
        if (historic.length() == 0) {
            String div = team.getDivision();
            if (div == null) {
                div = DIVISION_NO;
            }
            historic = div + "-1";
        }
        return historic;
    }

    /**
     * TEST - simulate game for test
     * @param division
     * @return
     */
    public String testUpdateResult(String division, boolean allTournament) {
        String[] arrayContractScore = getConfigStringValue("testArrayContractScore","3N_500_1;4S_450_1;4S_450_1;1N_150_1;1N_150_1;2H_100_1;1H_100_1;1S_200_1;1N_200_1;2H_100_1;3C_200_1;4N_200_1").split(";");
        List<Player> listPlayersTest = playerMgr.listPlayersTest();

        int nbUpdateOK = 0, nbUpdateFailed = 0;
        long tsBegin = System.currentTimeMillis();
        List<TeamGame> listGame = new ArrayList<>();
        Random random = new Random(System.nanoTime());
        Set<String> setTeamID = new HashSet<>();
        Set<Long> setPlayerID = new HashSet<>();
        int nbDeals = 0;
        while (true) {
            int nbNewGames = 0;
            for (Player p : listPlayersTest) {
                Team team = teamMgr.getTeamForPlayer(p.getID());
                if (team != null && !team.getDivision().equals(DIVISION_NO)) {
                    if (division != null && division.length() > 0) {
                        if (!team.getDivision().equals(division)) {
                            continue;
                        }
                    }
                    TeamPlayer teamPlayer = team.getPlayer(p.getID());
                    if (!teamPlayer.isSubstitute()) {
                        TeamMemTournament memTournament = memoryMgr.getMemTournamentForDivisionAndGroup(team.getDivision(), teamPlayer.getGroup());
                        if (memTournament != null) {
                            TeamMemTournamentPlayer memTournamentPlayer = memTournament.getTournamentPlayer(p.getID());
                            if (memTournamentPlayer == null || !memTournamentPlayer.isPlayerFinish()) {
                                int idxDeal = 1;
                                if (memTournamentPlayer != null) {
                                    idxDeal = memTournamentPlayer.getNbPlayedDeals() + 1;
                                }
                                int idxArray = random.nextInt(arrayContractScore.length);
                                String[] contractScore = arrayContractScore[idxArray].split("_");
                                TeamGame game = new TeamGame(p.getID(), getTournament(memTournament.tourID), idxDeal, team.getIDStr());
                                game.setScore(Integer.parseInt(contractScore[1]));
                                game.setContract(contractScore[0]);
                                game.setContractType(Integer.parseInt(contractScore[2]));
                                game.setDeclarer('S');
                                game.setTricks(7);
                                game.setFinished(true);
                                try {
                                    memoryMgr.addResult(game);
                                    listGame.add(game);
                                    setTeamID.add(team.getIDStr());
                                    setPlayerID.add(p.getID());
                                    nbUpdateOK++;
                                    nbNewGames++;
                                } catch (Exception e) {
                                    nbUpdateFailed++;
                                    log.error("Exception to update result for game=" + game, e);
                                }
                            }
                        }
                    }
                } else {
                    log.error("Team null or division is DNO - team=" + team + " - for playerID=" + p.getID());
                }
            }
            nbDeals++;
            if (!allTournament || nbNewGames == 0) {
                break;
            }
        }

        long tsBeforeInsertGame = System.currentTimeMillis();
        try {
            insertListGameDB(listGame);
        } catch (FBWSException e) {
            log.error("Exception to insert games", e);
        }
        String result = "Division="+division+" - Nb Teams="+setTeamID.size()+" - nbDeals="+nbDeals+" - nb players="+setPlayerID.size()+" - nbUpdateOK="+nbUpdateOK+" - nbUpdateFailed="+nbUpdateFailed+" - time to insert to DB ="+(System.currentTimeMillis() - tsBeforeInsertGame)+" - Total time = "+(System.currentTimeMillis()-tsBegin);
        return result;
    }

    @Override
    public void updateGamePlayArgineFinished(Game game) throws FBWSException {

    }

    @Override
    public void updateGamePlayArgine(Game game) {

    }
}
