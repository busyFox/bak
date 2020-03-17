package com.funbridge.server.tournament.serie;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.engine.ArgineEngineMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotifGroup;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.tournament.DealGenerator;
import com.funbridge.server.tournament.IDealGeneratorCallback;
import com.funbridge.server.tournament.game.*;
import com.funbridge.server.tournament.generic.GameGroupMapping;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.serie.data.*;
import com.funbridge.server.tournament.serie.memory.*;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.game.WSGameView;
import com.funbridge.server.ws.game.WSTableGame;
import com.funbridge.server.ws.player.WSSerieStatus;
import com.funbridge.server.ws.result.*;
import com.funbridge.server.ws.tournament.WSTableTournament;
import com.funbridge.server.ws.tournament.WSTournament;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.tools.JSONTools;
import com.gotogames.common.tools.StringTools;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by pserent on 31/03/2014.
 * Manager for tournament serie (new formule)
 */
@Component(value="tourSerieMgr")
@Scope(value="singleton")
public class TourSerieMgr extends TournamentGenericMgr implements IDealGeneratorCallback, ITournamentMgr {
    /** STATIC DECLARATIONS **/
    public static final String SERIE_NC = "SNC";
    public static final String SERIE_TOP = "TOP";
    public static final String SERIE_SNO = "SNO";
    public static final String SERIE_01 = "S01", SERIE_02 = "S02", SERIE_03 = "S03", SERIE_04 = "S04", SERIE_05 = "S05";
    public static final String SERIE_06 = "S06", SERIE_07 = "S07", SERIE_08 = "S08", SERIE_09 = "S09", SERIE_10 = "S10";
    public static final String SERIE_11 = "S11";
    public static final String[] SERIE_TAB = new String[]{SERIE_NC,
            SERIE_11, SERIE_10, SERIE_09, SERIE_08, SERIE_07, SERIE_06,
            SERIE_05, SERIE_04, SERIE_03, SERIE_02, SERIE_01, SERIE_TOP};
    private final String configName = "NEWSERIE";
    public static final String PERIOD_ID_SEPARATOR = "-";
    public static final String PERIOD_ID_INDICE_SEPARATOR = "_";
    private static SimpleDateFormat dateFormatPeriod = new SimpleDateFormat("yyyyMMdd");

    /** CLASS DECLARATION **/

    @Resource(name="mongoSerieTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name="playerMgr")
    private PlayerMgr playerMgr = null;
    @Resource(name = "playerCacheMgr")
    private PlayerCacheMgr playerCacheMgr = null;
    @Resource(name="messageNotifMgr")
    private MessageNotifMgr notifMgr;
    @Resource(name="argineEngineMgr")
    private ArgineEngineMgr argineEngineMgr;
    @Resource(name="presenceMgr")
    private PresenceMgr presenceMgr = null;

    private Map<String, TourSerieMemPeriodRanking> mapSerieRanking = new ConcurrentHashMap<>();
    private Map<String, Integer> mapNbPlayerSeriePreviousPeriod = new HashMap<>();

    private DealGenerator dealGenerator = null;
    private GameMgr gameMgr = null;
    private TourSerieMemoryMgr memoryMgr = null;
    private TourSeriePeriod currentPeriod = null;
    private String periodIDBefore3 = null, periodIDBefore2 = null, periodIDPrevious = null, periodIDNext1 = null;
    private LockWeakString lockCreateGame = new LockWeakString();
    private LockWeakString lockCreateSeriePlayer = new LockWeakString();
    private JSONTools jsonTools = new JSONTools();
    private boolean periodChangeProcessing = false;
    private boolean enable = true;
    private TourSerieChangePeriodTask changePeriodTask = new TourSerieChangePeriodTask();
    private ScheduledExecutorService schedulerChangePeriod = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> schedulerChangePeriodFuture = null;
    private NotifReminderTask notifReminderTask = new NotifReminderTask();
    private ScheduledExecutorService schedulerNotifReminder = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> schedulerNotifReminderFuture = null;

    // distribution of player in serie : S01, S02, S03 ... S11
    private double[] percentPlayerDistribution = new double[]{0.58, 0.88, 1.32, 1.97, 2.96, 4.44, 6.66, 10, 15, 22.5, 33.7};

    // notif to send during a period
    private MessageNotifGroup notifNewNoPlay = null;

    public class UpdateBestResult {
        public long playerID;
        public int rank;
    }

    @Override
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    public static boolean isSerieValid(String serie) {
        for (String e : SERIE_TAB) {
            if (e.equals(serie)) {
                return true;
            }
        }
        return false;
    }

    public JSONTools getJsonTools() {
        return jsonTools;
    }

    /**
     * Task to change period
     */
    public class TourSerieChangePeriodTask implements Runnable {
        private boolean running = false;

        @Override
        public void run() {
            if (!running) {
                running = true;
                try {
                    if (getConfigIntValue("changePeriodThreadEnable", 1) == 1) {
                        if (currentPeriod != null && currentPeriod.getTsDateEnd() < System.currentTimeMillis()) {
                            changePeriod();
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("No yet time to change period ...");
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to run changePeriod", e);
                    ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to changePeriod", e.getMessage(), null);
                }
                running = false;
            } else {
                log.error("Thread to change period already running !");
                ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for changePeriod", null, null);
            }
        }
    }

    /**
     * Task to remind player
     */
    public class NotifReminderTask implements Runnable {
        boolean running = false;

        @Override
        public void run() {
            if (!running) {
                running = true;
                try {
                    if (getConfigIntValue("reminderTaskEnable", 1) == 1) {
                        int temp = processReminder(System.currentTimeMillis());
                        if (log.isDebugEnabled()) {
                            log.debug("Nb serie reminder process=" + temp);
                        }
                    } else {
                        log.debug("Reminder task is not enable ...");
                    }
                } catch (Exception e) {
                    log.error("Failed to processReminder", e);
                    ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to processReminder", e.getMessage(), null);
                }
                running = false;
            } else {
                log.error("Process reminderDuel already running");
                ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for processReminder", null, null);
            }
        }
    }

    @Override
    @PostConstruct
    public void init() {
        gameMgr = new GameMgr(this);
        memoryMgr = new TourSerieMemoryMgr(this);
        // generator of deal
        dealGenerator = new DealGenerator(this, getStringResolvEnvVariableValue("generatorParamFile", null));


    }

    @Override
    @PreDestroy
    public void destroy() {
        log.info("destroy");
        stopScheduler(schedulerChangePeriod);
        stopScheduler(schedulerNotifReminder);
        if (gameMgr != null) {
            gameMgr.destroy();
        }
        if (getConfigBooleanValue("backupMemoryEnable")) {
            backupAllSerieRanking();
        }
        if (memoryMgr != null) {
            memoryMgr.destroy();
        }
        clearMapRanking();
    }

    @Override
    public void startUp() {
        log.info("startUp");

        if (getConfigIntValue("startUp", 1) == 1) {
            if (currentPeriod == null || getConfigIntValue("resetEnable", 0) == 1) {
                // INIT MAP SERIE RANKING
                clearMapRanking();
                for (String serie : SERIE_TAB) {
                    TourSerieMemPeriodRanking serieRanking = new TourSerieMemPeriodRanking();
                    serieRanking.serie = serie;
                    mapSerieRanking.put(serie, serieRanking);
                }

                // 1 get last period
                // 2 load tournament not finished
                // 3 load serie ranking
                // 4 check period and finish it if necessary
                // 5 create new period if necessary
                // 6 create tournament for current period if necessary

                // get last period
                List<TourSeriePeriod> listPeriod = listPeriodFromDatabase(0, 1);
                if (listPeriod != null && listPeriod.size() > 0) {
                    currentPeriod = listPeriod.get(0);
                }

                // no period found current period
                if (currentPeriod == null) {
                    String periodID = buildSeriePeriodID(System.currentTimeMillis());
                    // create new period for current timestamp
                    currentPeriod = getPeriodForID(periodID);
                    if (currentPeriod == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("No period found for ID=" + periodID + " - create a new one ...");
                        }
                        currentPeriod = createNewPeriod(periodID);
                        if (currentPeriod == null) {
                            log.error("Failed to create period with ID=" + periodID);
                        }
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug("Current period=" + currentPeriod);
                }

                initCurrentPeriod();

                if (getConfigIntValue("loadDataOnStartup", 1) == 1) {
                    // Load serie result from JSON back files
                    loadAllSerieRankingFromFile();

                    // LOAD TOURNAMENT IN PROGRESS
                    memoryMgr.loadTournamentForPeriod(currentPeriod.getPeriodID());
                    // check if current period is valid
                    if (!currentPeriod.isPeriodValidForTS(System.currentTimeMillis())) {
                        changePeriod();
                    }
                }

                // task thread for period management ????????
                try {
                    int timerPeriod = 5;
                    Calendar dateNext = Constantes.getNextDateForPeriod(Calendar.getInstance(), timerPeriod);
                    schedulerChangePeriodFuture = schedulerChangePeriod.scheduleAtFixedRate(changePeriodTask, dateNext.getTimeInMillis() - System.currentTimeMillis(), (long) timerPeriod * 60 * 1000, TimeUnit.MILLISECONDS);
                    log.debug("Schedule changePeriod - next run at " + Constantes.getStringDateForNextDelayScheduler(schedulerChangePeriodFuture) + " - period (minutes)=30");
                } catch (Exception e) {
                    log.error("Exception to start change period task", e);
                }

                // task to send notif reminder serie
                try {
                    int periodReminder = getReminderPeriodMinutes();
                    Calendar dateNext = Calendar.getInstance();
                    dateNext.add(Calendar.MINUTE, periodReminder);
                    schedulerNotifReminderFuture = schedulerNotifReminder.scheduleAtFixedRate(notifReminderTask, dateNext.getTimeInMillis() - System.currentTimeMillis(), (long) periodReminder * Constantes.TIMESTAMP_MINUTE, TimeUnit.MILLISECONDS);
                    log.info("Schedule reminder - next run at " + Constantes.getStringDateForNextDelayScheduler(schedulerNotifReminderFuture) + " - period (minutes)=" + periodReminder);
                } catch (Exception e) {
                    log.error("Exception to start reminder task", e);
                }
            } else {
                log.error("Current period is not null or config param resetEnable is not set on - currentPeriod=" + currentPeriod);
            }
        } else {
            log.error("Startup parameter is 0 !");
        }
    }

    private int getReminderPeriodMinutes() {
        return getConfigIntValue("reminder.period", 30);
    }

    /**
     * Return the date of the next notif reminder task run.
     * @return
     */
    public String getStringDateNextReminderScheduler() {
        return Constantes.getStringDateForNextDelayScheduler(schedulerNotifReminderFuture);
    }

    @Override
    public boolean distributionExists(String cards) {
        // check if a tournament with a deal containing this distribution exists
        return getTournamentWithDistribution(cards) != null;
    }

    /**
     * Remove all data in mapSerieRanking
     */
    private void clearMapRanking() {
        if (mapSerieRanking != null) {
            for (TourSerieMemPeriodRanking sr : mapSerieRanking.values()) {
                sr.clearData();
            }
            mapSerieRanking.clear();
        }
    }

    /**
     * Read string value for parameter in name (tournament."+configName+".paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    @Override
    public String getConfigStringValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringValue("tournament." + configName + "." + paramName, defaultValue);
    }

    @Override
    public String getStringResolvEnvVariableValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringResolvEnvVariableValue("tournament." + configName + "." + paramName, defaultValue);
    }

    /**
     * Read int value for parameter in name (tournament."+configName+".paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    @Override
    public int getConfigIntValue(String paramName, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("tournament." + configName + "." + paramName, defaultValue);
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

    /**
     * Read int value for parameter in name (tournament."+configName+".paramName) in config file and return true if value is 1.
     * @param paramName
     * @return
     */
    public boolean getConfigBooleanValue(String paramName) {
        return FBConfiguration.getInstance().getIntValue("tournament."+configName+"."+paramName, 0) >= 1;
    }

    @Override
    public String getTournamentCategoryName() {
        return Constantes.TOURNAMENT_CATEGORY_NAME_NEWSERIE;
    }

    @Override
    public boolean isBidAnalyzeEnable() {
        return getConfigIntValue("engineAnalyzeBid", 1) == 1;
    }

    @Override
    public boolean isDealParEnable() {
        return getConfigIntValue("enginePar", 1) == 1;
    }

    public GameMgr getGameMgr() {
        return gameMgr;
    }

    @Override
    public Class<? extends Game> getGameEntity() {
        return TourSerieGame.class;
    }

    @Override
    public String getGameCollectionName() {
        return "serie_game";
    }

    @Override
    public Class<TourSerieTournament> getTournamentEntity() {
        return TourSerieTournament.class;
    }

    @Override
    public Class<TourSerieTournamentPlayer> getTournamentPlayerEntity() {
        return TourSerieTournamentPlayer.class;
    }

    @Override
    public boolean finishTournament(Tournament tour) {
        return false;
    }

    @Override
    public TourSerieMemoryMgr getMemoryMgr() {
        return memoryMgr;
    }

    public TourSeriePeriod getCurrentPeriod() {
        return currentPeriod;
    }

    public String getPeriodIDBefore2() {
        return periodIDBefore2;
    }

    public String getPeriodIDBefore3() {
        return periodIDBefore3;
    }

    public String getPeriodIDPrevious() {
        return periodIDPrevious;
    }

    public String getPeriodIDNext1() {return periodIDNext1;}

    public String getSchedulerChangePeriodDateNextRun() {
        return Constantes.getStringDateForNextDelayScheduler(schedulerChangePeriodFuture);
    }

    @Override
    public void checkTournamentMode(boolean isReplay, long playerId) throws FBWSException {
        checkPeriodValid();
    }

    /**
     * Transform the date to timestamp. The date must have the format yyyyMMdd
     * @param strDate
     * @param beginPeriod flag to indicate if the date is the begin of the period
     */
    public static long transformPeriodDate2TS(String strDate, boolean beginPeriod) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(dateFormatPeriod.parse(strDate).getTime());
            cal.set(Calendar.MILLISECOND, 0);
            if (beginPeriod) {
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
            }
            return cal.getTimeInMillis();
        } catch (Exception e) {
            // Do nothing for now
            // Do nothing for now
        }
        return 0;
    }

    /**
     * Retrieve the timestamp of begin or end period for a periodID
     * @param periodID
     * @param beginPeriod
     * @return
     */
    public static long transformPeriodID2TS(String periodID, boolean beginPeriod) {
        long tsValue = 0;
        if (periodID != null) {
            String temp = periodID;
            if (temp.contains(PERIOD_ID_INDICE_SEPARATOR)) {
                temp = temp.substring(0, temp.indexOf(PERIOD_ID_INDICE_SEPARATOR));
            }
            String[] tab = temp.split(PERIOD_ID_SEPARATOR);
            if (tab.length == 2) {
                if (beginPeriod) {
                    tsValue = transformPeriodDate2TS(tab[0], beginPeriod);
                } else {
                    tsValue = transformPeriodDate2TS(tab[1], beginPeriod);
                }
            }
        }
        return tsValue;
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

    /**
     * Build the deal ID using tourID and deal index
     * @param tourID
     * @param index
     * @return
     */
    public String _buildDealID(String tourID, int index) {
        return buildDealID(tourID,index);
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
     * Extract tourID string from dealID
     * @param dealID
     * @return
     */
    public String _extractTourIDFromDealID(String dealID) {
        return extractTourIDFromDealID(dealID);
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
                ContextManager.getTourSerieMgr().getLogger().error("Failed to retrieve dealIndex from dealID=" + dealID, e);
            }
        }
        return -1;
    }

    @Override
    public void checkGame(Game game) throws FBWSException {
        if (game != null) {
            if (game.getTournament().isFinished()) {
                log.error("CheckGame failed - Tournament is finished ! - game="+game);
                throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
            }
            if (!game.getTournament().isDateValid(System.currentTimeMillis())) {
                log.error("CheckGame failed - Tournament dates not valid ! - game="+game);
                throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
            }
        } else {
            log.error("CheckGame failed - Game is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Extract dealIndex from dealID
     * @param dealID
     * @return
     */
    public int _extractDealIndexFromDealID(String dealID) {
        return extractDealIndexFromDealID(dealID);
    }

    /**
     * Compute the serie evolution from the original accoring to evolution (+1, 0 or -1)
     * @param serieOriginal
     * @param evolution
     * @param checkSNO  if true and serie is NC and evolution > 0 return SNO
     * @return
     */
    public static String computeSerieEvolution(String serieOriginal, int evolution, boolean checkSNO) {
        if (evolution == 0) {
            return serieOriginal;
        }
        if (checkSNO && serieOriginal.equals(SERIE_NC) && evolution > 0) {
            return SERIE_SNO;
        }
        if (serieOriginal.equals(SERIE_NC)) {
            return SERIE_NC;
        }
        if (serieOriginal.equals(SERIE_TOP) && evolution > 0) {
            return SERIE_TOP;
        }
        if (serieOriginal.equals(SERIE_TOP) && evolution < 0) {
            return SERIE_01;
        }
        if (serieOriginal.equals(SERIE_11) && evolution < 0) {
            // No down from serie 11
            return SERIE_11;
        }
        int idx = -1;
        for (int i=0; i < SERIE_TAB.length; i++){
            if (serieOriginal.equals(SERIE_TAB[i])) {
                idx = i;
                break;
            }
        }
        if (evolution > 0 && idx < (SERIE_TAB.length-1)) {
            idx++;
        } else if (evolution < 0 && idx > 0) {
            idx--;
        }
        if (idx >= 0 && idx < SERIE_TAB.length) {
            return SERIE_TAB[idx];
        }
        return serieOriginal;
    }

    /**
     * Compute the trend for player with no activity the current period.
     * @param currentSerie
     * @param lastPeriodPlayed
     * @return
     */
    public int computeTrendForPlayerNoActivityThisPeriod(String currentSerie, String lastPeriodPlayed) {
        if (periodIDPrevious == null) {
            return 0;
        }
        if (lastPeriodPlayed == null) {
            return 0;
        }
        // case of serie NEW
        if (currentSerie.equals(SERIE_NC)) {
            // always maintain
            return 0;
        }
        // case of serie TOP
        if (currentSerie.equals(SERIE_TOP)) {
            // maintain only one period without play
            if (lastPeriodPlayed.equals(periodIDPrevious)) {
                return 0;
            }
            // else down
            return -1;
        }
        // case of serie 11
        if (currentSerie.equals(SERIE_11)) {
            // always maintain
            return 0;
        }
        // other series
        // down if no play 2 periods
        if (periodIDBefore2 != null && lastPeriodPlayed.equals(periodIDBefore2)) {
            return -1;
        }
        // always maintain in all other cases
        return 0;
    }

    /**
     * Compare two series for order. Returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
     * TOP > S01 > S02 > S03 > ... > S11 > SNC
     * @param serie1
     * @param serie2
     * @throws
     * @return
     */
    public static int compareSerie(String serie1, String serie2) throws Exception {
        if (serie1.equals(serie2)) {
            return 0;
        }
        // case of NC
        if (serie1.equals(SERIE_NC)) {
            return -1;
        }
        if (serie2.equals(SERIE_NC)) {
            return 1;
        }
        // case of TOP
        if (serie1.equals(SERIE_TOP)) {
            return 1;
        }
        if (serie2.equals(SERIE_TOP)) {
            return -1;
        }
        int idx1 = Arrays.asList(SERIE_TAB).indexOf(serie1);
        int idx2 = Arrays.asList(SERIE_TAB).indexOf(serie2);
        if (idx1 >= 0 && idx2 >= 0) {
            if (idx1 > idx2) {
                return 1;
            } else {
                return -1;
            }
        }
        throw new Exception("Param serie1="+serie1+" or serie2="+serie2+" not valid");
    }

    /**
     * Create a periodID associated to this timestamp : date begin + SEPARATOR + date end (date format : yyyyMMdd)
     * @param ts
     * @return
     */
    public String buildSeriePeriodID(long ts) {
        String part1 = "", part2 = "";
        Calendar calCurrent = Calendar.getInstance();
        calCurrent.setTimeInMillis(ts);
        int dayCurrent = calCurrent.get(Calendar.DAY_OF_MONTH);

        int devMode = getConfigIntValue("devMode", 0);
        // devMode ?
        if (devMode > 0) {
            // devMode can be only 1 or 2
            if (devMode > 2) {devMode = 1;}
            if (devMode == 1) {
                // PERIOD 1 DAY
                part1 += calCurrent.get(Calendar.YEAR);
                part1 += String.format("%02d", (calCurrent.get(Calendar.MONTH)+1));
                part1 += String.format("%02d", calCurrent.get(Calendar.DAY_OF_MONTH));
                part2 = part1;
            } else if (devMode == 2) {
                // PERIOD 7 DAY
                part1 += calCurrent.get(Calendar.YEAR);
                part1 += String.format("%02d", (calCurrent.get(Calendar.MONTH)+1));
                part1 += String.format("%02d", calCurrent.get(Calendar.DAY_OF_MONTH));
                calCurrent.add(Calendar.DAY_OF_YEAR, 6);
                part2 += calCurrent.get(Calendar.YEAR);
                part2 += String.format("%02d", (calCurrent.get(Calendar.MONTH)+1));
                part2 += String.format("%02d", calCurrent.get(Calendar.DAY_OF_MONTH));
            }


        }
        // Prod mode
        else {
            int dayBegin = 0, dayEnd = 0;
            part1 += calCurrent.get(Calendar.YEAR);
            part1 += String.format("%02d", (calCurrent.get(Calendar.MONTH)+1));
            part2 = part1;
            if (dayCurrent > 15) {
                dayBegin = 16;
                dayEnd = calCurrent.getActualMaximum(Calendar.DAY_OF_MONTH);
            } else {
                dayBegin = 1;
                dayEnd = 15;
            }
            part1 += String.format("%02d", dayBegin);
            part2 += String.format("%02d", dayEnd);
        }
        return part1+PERIOD_ID_SEPARATOR+part2;
    }

    /**
     * Return the period before the current period
     * @param nbPeriodBefore
     * @return
     */
    public TourSeriePeriod getPreviousPeriod(int nbPeriodBefore) {
        try {
            Query q = new Query();
            q.with(new Sort(Sort.Direction.DESC, "tsDateStart").and(new Sort(Sort.Direction.DESC, "tsDateCreation")));
            q.limit(nbPeriodBefore+1);
            List<TourSeriePeriod> listPeriod = mongoTemplate.find(q, TourSeriePeriod.class);
            if (listPeriod != null && listPeriod.size() == (nbPeriodBefore+1)) {
                return listPeriod.get(nbPeriodBefore);
            }
        } catch (Exception e) {
            log.error("Failed to get previous period - nbPeriodBefore="+nbPeriodBefore, e);
        }
        return null;
    }


    /**
     * Initialization of the current period : set nb player for each serie in memory, load tournament in progress, and create tournament if necessary
     */
    public void initCurrentPeriod() {
        if (currentPeriod != null) {
            // set periodIDBefore2
            TourSeriePeriod periodBefore2 = getPreviousPeriod(2);
            if (periodBefore2 != null) {
                periodIDBefore2 = periodBefore2.getPeriodID();
            } else {
                periodIDBefore2 = null;
            }
            // set periodIDBefore3
            TourSeriePeriod periodBefore3 = getPreviousPeriod(3);
            if (periodBefore3 != null) {
                periodIDBefore3 = periodBefore3.getPeriodID();
            } else {
                periodIDBefore3 = null;
            }
            // set periodIDPrevious (Before1 ;-))
            TourSeriePeriod periodPrevious = getPreviousPeriod(1);
            if (periodPrevious != null) {
                periodIDPrevious = periodPrevious.getPeriodID();
            } else {
                periodIDPrevious = null;
            }
            // set periodIDNext
            long tsPeriodNext = currentPeriod.getTsDateEnd() + 60 * 60 * 1000;
            periodIDNext1 = buildSeriePeriodID(tsPeriodNext);

            // init nb player for each serie & load threshold
            int thresholdNbTourUpNC = 5;
            for (TourSerieMemPeriodRanking rs : mapSerieRanking.values()) {
                int nbPlayerSerie = 0;
                if (!rs.serie.equals(SERIE_NC)) {
                    nbPlayerSerie = countPlayerInSerieExcludeReserve(rs.serie, true);
                }
                rs.updateNbPlayerSerie(nbPlayerSerie);
                rs.loadThresholdFromConfiguration();
                if (rs.serie.equals(SERIE_NC)) {
                    thresholdNbTourUpNC = rs.thresholdNbUp;
                }
            }

            // init nb player in serie for previous period
            initMapNbPlayerSeriePreivousPeriod();
            // CREATE TOURNAMENT FOR CURRENT PERIOD
            int nbTourGenerated = generateTournamentForPeriod(currentPeriod);
            if (log.isDebugEnabled()) {
                log.debug("Nb tournament generated for current period=" + currentPeriod + " - nbTourGenerated=" + nbTourGenerated);
            }

            // init notif newNoPlay
            initNotifNewNoPlay(thresholdNbTourUpNC);
        } else {
            log.error("Failed !! currentPeriod is null");
        }
    }

    /**
     * init nb player in serie for previous period
     */
    public void initMapNbPlayerSeriePreivousPeriod() {
        mapNbPlayerSeriePreviousPeriod.clear();
        for (String s : SERIE_TAB) {
            mapNbPlayerSeriePreviousPeriod.put(s, countPlayerOnPeriodResult(periodIDPrevious, s));
        }
    }

    /**
     * Initialize notif new No play
     */
    public void initNotifNewNoPlay(int thresholdNbTourUpNC) {
        notifNewNoPlay = null;
        List<String> listFieldName = new ArrayList<>();
        listFieldName.add(MessageNotifMgr.NOTIF_PARAM_TEMPLATE);
        listFieldName.add(MessageNotifMgr.NOTIF_PARAM_PERIOD);
        List<String> listFieldValue = new ArrayList<>();
        listFieldValue.add(MessageNotifMgr.NOTIF_SERIE_NEW_NEW_NO_PLAY);
        listFieldValue.add(currentPeriod.getPeriodID());
        notifNewNoPlay = notifMgr.getLastNotifGroupWithField(listFieldName, listFieldValue);
        if (notifNewNoPlay == null) {
            notifNewNoPlay = notifMgr.createNotifGroupSerieNewNewNoPlay(currentPeriod.getTsDateEnd(), thresholdNbTourUpNC, currentPeriod.getPeriodID());
        }
    }

    /**
     * Create the new period associated to this periodID
     * @param periodID
     * @return
     */
    public synchronized TourSeriePeriod createNewPeriod(String periodID) {
        // check period not existing
        TourSeriePeriod period = new TourSeriePeriod(periodID);
        try {
            mongoTemplate.insert(period);
            if (log.isDebugEnabled()) {
                log.debug("Create and insert period="+period);
            }
        } catch (Exception e) {
            log.error("Failed to create new period with ID="+periodID, e);
            period = null;
        }
        return period;
    }

    /**
     * Return the existing period for this periodID or null if not found
     * @param periodID
     * @return
     */
    public TourSeriePeriod getPeriodForID(String periodID) {
        try {
            return mongoTemplate.findOne(Query.query(Criteria.where("periodID").regex("^" + periodID + "*")).with(new Sort(Sort.Direction.DESC, "tsDateCreation")), TourSeriePeriod.class);
        } catch (Exception e) {
            log.error("Failed to get period for periodID="+periodID, e);
        }
        return null;
    }

    /**
     * Return list of period from database. List is ordered by dateStart desc
     * @return
     */
    public List<TourSeriePeriod> listPeriodFromDatabase(int offset, int nbMax) {
        try {
            Query q = new Query();
            if (nbMax > 0) {
                q.limit(1);
            }
            if (offset > 0) {
                q.skip(offset);
            }
            q.with(new Sort(Sort.Direction.DESC, "tsDateCreation"));
            return mongoTemplate.find(q, TourSeriePeriod.class);
        } catch (Exception e) {
            log.error("Exception to list period from database - offset="+offset+" - nbMax="+nbMax, e);
        }
        return null;
    }

    /**
     * Count number of tournament created on period for a serie
     * @param periodID
     * @param serie
     * @return
     */
    public int countTournamentForPeriodAndSerie(String periodID, String serie) {
        return (int)mongoTemplate.count(Query.query(Criteria.where("serie").is(serie).andOperator(Criteria.where("period").is(periodID))), TourSerieTournament.class);
    }

    /**
     * Cont number of player who play the tournament and include in the list
     * @param tourID
     * @param listPlaID
     * @return
     */
    public int countPlayerForTournamentFinished(String tourID, List<Long> listPlaID) {
        if (listPlaID != null) {
            return (int) mongoTemplate.count(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("playerID").in(listPlaID))), TourSerieTournamentPlayer.class);
        } else {
            return (int) mongoTemplate.count(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID))), TourSerieTournamentPlayer.class);
        }
    }

    /**
     * Return the tournament with this ID
     * @param tourID
     * @return
     */
    public TourSerieTournament getTournament(String tourID) {
        return mongoTemplate.findById(new ObjectId(tourID), TourSerieTournament.class);
    }

    /**
     * Return the game with this ID
     * @param gameID
     * @return
     */
    public TourSerieGame getGame(String gameID) {
        return mongoTemplate.findById(new ObjectId(gameID), TourSerieGame.class);
    }

    /**
     * Return the number of tournament played for current period (tournament not yet finished)
     * @param playerID
     * @param serie
     * @return
     */
    public int countTournamentMemoryPlayedForPlayer(long playerID, String serie) {
        TourSerieMemPeriodRanking serieRanking = getSerieRanking(serie);
        if (serieRanking != null) {
            TourSerieMemPeriodRankingPlayer rankPla = serieRanking.getPlayerRank(playerID);
            if (rankPla != null) {
                return rankPla.getNbTournamentPlayed();
            }
        }
        return 0;
    }

    /**
     * Return list of tournament not finished for period and serie
     * @param periodID
     * @param serie
     * @return
     */
    public List<TourSerieTournament> listTournamentNotFinishedForPeriodAndSerie(String periodID, String serie) {
        Query q = new Query();
        Criteria cSerie = Criteria.where("serie").is(serie);
        Criteria cPeriod = Criteria.where("period").is(periodID);
        Criteria cFinished = Criteria.where("isFinished").is(false);
//        q.addCriteria(Criteria.where("serie").is(serie).andOperator(Criteria.where("period").is(periodID)).andOperator(Criteria.where("isFinished").is(false)));
        q.addCriteria(new Criteria().andOperator(cSerie, cPeriod, cFinished));
        return mongoTemplate.find(q, TourSerieTournament.class);
    }

    /**
     * Return list of tournament for period and serie
     * @param periodID
     * @param serie
     * @return
     */
    public List<TourSerieTournament> listTournamentForPeriodAndSerie(String periodID, String serie) {
        Query q = new Query();
        Criteria cSerie = Criteria.where("serie").is(serie);
        Criteria cPeriod = Criteria.where("period").is(periodID);
        q.addCriteria(new Criteria().andOperator(cSerie, cPeriod));
        return mongoTemplate.find(q, TourSerieTournament.class);
    }

    /**
     * Generate tournament for a period
     * @param period
     * @return
     */
    public int generateTournamentForPeriod(TourSeriePeriod period) {
        int nbTourGenerated = -1;
        if (period != null && period.isPeriodValidForTS(System.currentTimeMillis())) {
            log.warn("Generate tournaent for period=" + period);
            int nbDeal = getConfigIntValue("tourNbDeal", 4);
            int nbTourDefault = getConfigIntValue("periodNbTour.default", 10);
            int engineVersion = argineEngineMgr.getEngineVersion(Constantes.TOURNAMENT_CATEGORY_NEWSERIE);
            int nbCreditPlayDeal = getConfigIntValue("nbCreditPlayDeal", 1);

            if (engineVersion > 0) {
                nbTourGenerated = 0;
                for (String strSerie : SERIE_TAB) {
                    int nbTourConf = getConfigIntValue("periodNbTour.serie" + strSerie, nbTourDefault);
                    int nbTourExisting = countTournamentForPeriodAndSerie(period.getPeriodID(), strSerie);
                    long tsPeriodStart = period.getTsDateStart();
                    long tsPeriodEnd = period.getTsDateEnd();
                    if (nbTourExisting < nbTourConf) {
                        for (int i = 0; i < nbTourConf - nbTourExisting; i++) {
                            Random r = new Random(System.nanoTime());
                            int offsetIndex = r.nextInt(20);
                            DealGenerator.DealGenerate[] tabDeal = dealGenerator.generateDeal(nbDeal, offsetIndex);
                            if (tabDeal != null && tabDeal.length == nbDeal) {
                                TourSerieTournament tour = new TourSerieTournament();
                                tour.setSerie(strSerie);
                                tour.setPeriod(period.getPeriodID());
                                tour.setTsDateStart(tsPeriodStart);
                                tour.setTsDateEnd(tsPeriodEnd);
                                tour.setDealGenerate(tabDeal);
                                tour.setNbCreditsPlayDeal(nbCreditPlayDeal);
                                tour.setCreationDateISO(new Date());
                                try {
                                    mongoTemplate.insert(tour);
                                    memoryMgr.addTournament(tour);
                                    nbTourGenerated++;
                                } catch (Exception e) {
                                    log.error("Failed to insert new tour="+tour, e);
                                }
                            }
                        }
                        log.warn("Nb tour generated for serie=" + strSerie + " - nbTour=" + (nbTourConf - nbTourExisting));
                    }
                }
            } else {
                log.error("Engine version not valid ... engineVersion="+engineVersion);
            }
        } else {
            log.error("Period is not valid ! period="+period);
        }
        return nbTourGenerated;
    }

    /**
     * Return the tournament with a deal has this distribution
     * @param cards
     * @return
     */
    public TourSerieTournament getTournamentWithDistribution(String cards) {
        return mongoTemplate.findOne(Query.query(Criteria.where("listDeal.cards").is(cards)), TourSerieTournament.class);
    }

    /**
     * Check if period is valid
     * @throws FBWSException
     */
    public void checkPeriodValid() throws FBWSException {
        checkSerieEnable();
        if (!currentPeriod.isPeriodValidForTS(System.currentTimeMillis())) {
            log.warn("Current period is not valid ... changing period is coming ...");
            throw new FBWSException(FBExceptionType.SERIE_PERIOD_PROCESSING);
        }
    }

    public void checkSerieEnable() throws FBWSException {
        if (!isEnable()) {
            throw new FBWSException(FBExceptionType.SERIE_CLOSED);
        }
        if (periodChangeProcessing) {
            throw new FBWSException(FBExceptionType.SERIE_PERIOD_PROCESSING);
        }
    }

    public boolean isEnable() {
        if (!getConfigBooleanValue("enable")) {
            return false;
        }
        return enable;
    }

    public void setEnable(boolean value) {
        enable = value;
    }

    public void setPeriodChangeProcessing(boolean value) {
        periodChangeProcessing = value;
    }

    public boolean isPeriodChangeProcessing() {
        return periodChangeProcessing;
    }


    /**
     * Save the game to DB. if already existing an update is done, else an insert
     * @param game
     * @throws FBWSException
     */
    public void updateGameDB(TourSerieGame game) throws FBWSException {
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
     * Insert games to DB
     * @param listGame
     * @throws FBWSException
     */
    @Override
    public <T extends Game> void insertListGameDB(List<T> listGame) throws FBWSException {
        if (listGame != null && !listGame.isEmpty()) {
            try {
                if (getConfigIntValue("useBulk", 1) == 1) {
                    BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, TourSerieGame.class);
                    bulkOperations.insert(listGame);
                    bulkOperations.execute();
                } else {
                    mongoTemplate.insertAll(listGame);
                }
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
    public void insertListTourPlayDB(List<TourSerieTournamentPlayer> listTourPlay) throws FBWSException {
        if (listTourPlay != null && listTourPlay.size() > 0) {
            try {
                if (getConfigIntValue("useBulk", 1) == 1) {
                    BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, TourSerieTournamentPlayer.class);
                    bulkOperations.insert(listTourPlay);
                    bulkOperations.execute();
                } else {
                    mongoTemplate.insertAll(listTourPlay);
                }
            } catch (Exception e) {
                log.error("Exception to save listTourPlay", e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Update games to DB
     * @param listGame
     * @throws FBWSException
     */
    @Override
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

    public int updateListGameBulk(List<TourSerieGameUpdateBulk> listData) {
        int nbWrite = -1;
        if (listData != null && listData.size() > 0) {
            MongoCollection colSerie = mongoTemplate.getCollection("serie_game");
            List<UpdateOneModel> listBulk = new ArrayList<>();
            for (TourSerieGameUpdateBulk e : listData) {
                listBulk.add(new UpdateOneModel(new BasicDBObject().append("_id", new ObjectId(e.gameID)), new BasicDBObject().append("$set", new BasicDBObject().append("rank", e.rank).append("result", e.result))));
            }
            colSerie.bulkWrite(listBulk);
            nbWrite = listData.size();
        }
        return nbWrite;
    }

    /**
     * Save the tournament to DB
     * @param tour
     */
    @Override
    public void updateTournamentDB(Tournament tour) throws FBWSException {
        if (tour != null && tour instanceof TourSerieTournament) {
            TourSerieTournament tourTZ = (TourSerieTournament)tour;
            try {
                mongoTemplate.save(tour);
            } catch (Exception e) {
                log.error("Exception to save tour=" + tour, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.error("Tour not valid ! tour="+tour);
        }
    }

    /**
     * Called by game mgr when game is finished. Save game data in DB.
     * @param session
     */
    @Override
    public void updateGameFinished(FBSession session) throws FBWSException {
        if (session == null) {
            log.error("Session parameter is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (session.getCurrentGameTable() == null || session.getCurrentGameTable().getGame() == null || !(session.getCurrentGameTable().getGame() instanceof TourSerieGame)) {
            log.error("Table or game in session not valid ! - table="+session.getCurrentGameTable());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TourSerieGame game = (TourSerieGame)session.getCurrentGameTable().getGame();
        if (!game.isReplay()) {
            if (game.isFinished()) {
                // update data game and table in DB
                updateGameDB(game);

                // update tournament play in memory
                memoryMgr.updateResult(game, session.getPlayerCache(), true);

                // remove game from session
                session.removeGame();
                // remove player from set
                gameMgr.removePlayerRunning(session.getPlayer().getID());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Nothing to do ... game not finished - game="+game);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Nothing to do ... replay game - game="+game);
            }
        }
    }

    /**
     * Find tournament in progress for a player session
     * @param session
     * @return
     */
    public TourSerieMemTour getMemTourInProgressForPlayer(FBSession session) {
        String serie = session.getSerie();
        if (session.getCurrentGameTable() != null && session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_NEWSERIE && !session.getCurrentGameTable().isReplay() && !session.getCurrentGameTable().isAllTournamentDealPlayed()) {
            return memoryMgr.getMemTour(serie, session.getCurrentGameTable().getTournament().getIDStr());
        }
        return memoryMgr.getTournamentInProgressForPlayer(serie, session.getPlayer().getID());
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
        String serie = session.getSerie();
        Table table = null;
        TourSerieTournament tour = null;
        TourSerieMemTour memTour = null;

        //**********************
        // use current table in session ?
        if (session.getCurrentGameTable() != null &&
                !session.getCurrentGameTable().isReplay() &&
                session.getCurrentGameTable().getTournament() != null &&
                session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_NEWSERIE &&
                !session.getCurrentGameTable().isAllTournamentDealPlayed()) {
            // continue to play the current tournament
            table = session.getCurrentGameTable();
            tour = (TourSerieTournament)table.getTournament();
            memTour = memoryMgr.getMemTour(serie, tour.getIDStr());
            if (memTour == null) {
                log.error("Failed to retrieve MemTour for tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        //********************
        // need to get tournament in progress or one not yet played for this player
        //需要进行比赛或尚未为该玩家进行的比赛
        else {
            memTour = memoryMgr.getTournamentInProgressForPlayer(serie, player.getID());
            if (memTour == null) {
                // no tournament in progress, search a new tournament not yet played by player and device
                //没有正在进行的锦标赛，请搜索尚未由玩家和设备玩过的新锦标赛
                memTour = getMemTourNotPlayedForPlayerAndDevice(serie, player.getID(), session.getDeviceID());
                if (memTour == null) {
                    // no tournament found ... all played !
                    throw new FBWSException(FBExceptionType.SERIE_ALL_TOUR_PLAYED);
                }
            }

            tour = getTournament(memTour.tourID);
            if (tour == null) {
                log.error("Failed to load tournament with ID="+memTour.tourID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }

        //*******************
        // build table
        if (table == null) {
            table = new Table(player, tour);
            // set deals played
            TourSerieMemTourPlayer memRanking = memTour.getRankingPlayer(player.getID());
            if (memRanking != null) {
                table.setPlayedDeals(memRanking.dealsPlayed);
            }
            // retrieve game if exist
            TourSerieGame game = getGameNotFinishedOnTournamentForPlayer(tour.getIDStr(), player.getID());
            if (game != null) {
                table.setGame(game);
            }
            session.setCurrentGameTable(table);
        }

        //*******************
        // retrieve game
        TourSerieGame game = null;
        if (table.getGame() != null && !table.getGame().isFinished() && table.getGame().getTournament().getIDStr().equals(tour.getIDStr())) {
            game = (TourSerieGame)table.getGame();
        } else {
            // need to retrieve an existing game
            int lastIndexPlayed = 0;
            List<TourSerieGame> listGame = listGameOnTournamentForPlayer(tour.getIDStr(), player.getID());
            if (listGame != null) {
                for (TourSerieGame g : listGame) {
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

            // Suite � gros bug de games perdus en base de donn�es, s'il nous manque des games alors on supprime tout et le joueur rejoue le tournoi de 0
            if(lastIndexPlayed > listGame.size()){
                int deletedGamesInDB = 0, deletedResultsInMemory = 0;
                for(TourSerieGame g : listGame){
                    if(deleteGame(g)){
                        deletedGamesInDB++;
                        if(memTour.removeResult(g.getDealIndex(), player.getID())){
                           deletedResultsInMemory++;
                        }
                    }
                }
                log.warn("LastIndexPlayed was too big for tourID="+tour.getIDStr()+" - player="+session.getPlayer().getID()+" - lastIndexPlayed="+lastIndexPlayed+". Deleted games in database : "+deletedGamesInDB+" - Deleted results in memory : "+deletedResultsInMemory);
                lastIndexPlayed = 0;
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
                game = createGame(player.getID(), tour, lastIndexPlayed+1, conventionProfil, conventionValue, cardsConventionProfil, cardsConventionValue, session.getDeviceID());

                if (game != null) {
                    ContextManager.getPlayerMgr().updatePlayerCreditDeal(player, tour.getNbCreditsPlayDeal(), 1);
                } else {
                    log.error("Failed to create game - player="+player.getID()+" - tour="+tour);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_NEWSERIE, 1);
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
            TourSerieMemTourPlayer mtp = memTour.getOrCreateRankingPlayer(player.getID());
            mtp.currentDealIndex = game.getDealIndex();
        }

        //*********************
        // Build data to return
        WSTableTournament tableTour = new WSTableTournament();
        tableTour.tournament = tour.toWS();
        tableTour.tournament.arrayDealIDstr = memTour.getArrayDealID();
        tableTour.tournament.nbTotalPlayer = memTour.getNbPlayersFinish();
        tableTour.tournament.resultPlayer = memTour.getWSResultPlayer(session.getPlayerCache(), true);
        if (tableTour.tournament.resultPlayer != null) {
            tableTour.tournament.playerOffset = tableTour.tournament.resultPlayer.getRank();
        }
        tableTour.tournament.currentDealIndex = game.getDealIndex();
        tableTour.currentDeal = new WSGameDeal();
        tableTour.currentDeal.setDealData(game.getDeal(), tour.getIDStr());
        tableTour.currentDeal.setGameData(game);
        tableTour.table = table.toWSTableGame();
        tableTour.gameIDstr = game.getIDStr();
        tableTour.conventionProfil = game.getConventionProfile();
        tableTour.tournament.remainingTime = tour.getTsDateEnd() - System.currentTimeMillis();
        tableTour.creditAmount = session.getPlayer().getTotalCreditAmount();
        tableTour.nbPlayedDeals = session.getPlayer().getNbPlayedDeals();
        tableTour.replayEnabled = playerMgr.isReplayEnabledForPlayer(session);
        tableTour.freemium = session.isFreemium();
        return tableTour;
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
    @Override
    public Table createReplayTable(Player player, Game gamePlayed, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) throws FBWSException {
        if (player == null) {
            log.error("Param player is null");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // create a table
        Table table = new Table(player, gamePlayed.getTournament());
        table.setReplay(true);
        // create game
        TourSerieGame replayGame = new TourSerieGame(player.getID(), (TourSerieTournament)gamePlayed.getTournament(), gamePlayed.getDealIndex());
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
     * Return summary at end replay game
     * @param session
     * @param dealID
     * @return
     * @throws FBWSException
     */
    @Override
    public WSResultReplayDealSummary resultReplaySummary(FBSession session, String dealID) throws FBWSException {
        if (session == null) {
            log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TourSerieGame replayGame = null;
        if (session.getCurrentGameTable() != null && session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
            replayGame = (TourSerieGame)session.getCurrentGameTable().getGame();
        }
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
        TourSerieGame originalGame = getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        if (originalGame == null) {
            log.error("Original game not found ! dealID="+dealID+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }

        int nbTotalPlayer = -1;
        double resultOriginal = 0;
        int rankOriginal = 0;
        TourSerieMemTour memTour = memoryMgr.getMemTour(replayGame.getTournament().getSerie(), tourID);
        if (memTour != null) {
            TourSerieMemDeal memDeal = memTour.getResultDeal(dealID);
            if (memDeal == null) {
                log.error("No memDeal found for dealID="+dealID+" - on memTour="+memTour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            TourSerieMemDealPlayer memDealPlayer = memDeal.getResultPlayer(playerID);
            if (memDealPlayer == null) {
                log.error("No memDealPlayer found for player="+playerID+" on memTour="+memTour+" - memDeal="+memDeal+" - replayGame="+replayGame);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            nbTotalPlayer = memDeal.getNbPlayer();
            resultOriginal = memDealPlayer.result;
            rankOriginal = memDealPlayer.nbPlayerBestScore+1;
        } else {
            nbTotalPlayer = replayGame.getTournament().getNbPlayers();
            resultOriginal = originalGame.getResult();
            rankOriginal = originalGame.getRank();
        }

        // result original
        WSResultDeal resultPlayer = new WSResultDeal();
        resultPlayer.setDealIDstr(dealID);
        resultPlayer.setDealIndex(dealIndex);
        resultPlayer.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
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
        resultReplay.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
        resultReplay.setNbTotalPlayer(nbTotalPlayer+1); // + 1 to count replay game !
        resultReplay.setContract(replayGame.getContractWS());
        resultReplay.setDeclarer(Character.toString(replayGame.getDeclarer()));
        resultReplay.setNbTricks(replayGame.getTricks());
        resultReplay.setScore(replayGame.getScore());
        int nbPlayerWithBestScoreReplay = countGameWithBestScore(tourID, dealIndex, replayGame.getScore());
        resultReplay.setRank(nbPlayerWithBestScoreReplay + 1);
        resultReplay.setNbPlayerSameGame(countGameWithSameScoreAndContract(tourID, dealIndex, replayGame.getScore(), replayGame.getContract(), replayGame.getContractType()));
        double replayResult = -1;
        int nbPlayerSameScore = countGameWithSameScore(tourID, dealIndex, replayGame.getScore());
        replayResult = Constantes.computeResultPaire(nbTotalPlayer, nbPlayerWithBestScoreReplay, nbPlayerSameScore);
        resultReplay.setResult(replayResult);
        resultReplay.setLead(replayGame.getBegins());

        // result for most played
        WSResultDeal resultMostPlayed = buildWSResultGameMostPlayed(tourID, dealIndex, nbTotalPlayer);
        if (resultMostPlayed == null) {
            log.error("No result for most played game for tourID="+tourID+" - dealIndex="+dealIndex);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        resultMostPlayed.setDealIDstr(dealID);
        resultMostPlayed.setDealIndex(replayGame.getDealIndex());
        resultMostPlayed.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
        resultMostPlayed.setNbTotalPlayer(nbTotalPlayer);
        if (!replayGame.getTournament().isFinished()) {
            int nbPlayerWithBestScoreMP = countGameWithBestScore(tourID, dealIndex, resultMostPlayed.getScore());
            resultMostPlayed.setRank(nbPlayerWithBestScoreMP + 1);
            nbPlayerSameScore = countGameWithSameScore(tourID, dealIndex, resultMostPlayed.getScore());
            resultMostPlayed.setResult(Constantes.computeResultPaire(nbTotalPlayer, nbPlayerWithBestScoreMP, nbPlayerSameScore));
        }

        WSResultReplayDealSummary replayDealSummary = new WSResultReplayDealSummary();
        replayDealSummary.setResultPlayer(resultPlayer);
        replayDealSummary.setResultMostPlayed(resultMostPlayed);
        replayDealSummary.setResultReplay(resultReplay);
        replayDealSummary.getAttributes().add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, ""+(nbTotalPlayer+1)));
        replayDealSummary.getAttributes().add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_RESULT_TYPE, ""+Constantes.TOURNAMENT_RESULT_PAIRE));
        return replayDealSummary;
    }

    /**
     * Find tournament in memory not played by player and by device (option)
     * @param serie
     * @param playerID
     * @param deviceID
     * @return
     */
    private TourSerieMemTour getMemTourNotPlayedForPlayerAndDevice(String serie, long playerID, long deviceID) {
        TourSerieMemTour memTour = null;
        List<String> listTourIgnore = new ArrayList<>();
        while (true) {
            memTour = memoryMgr.getTournamentNotPlayedForPlayer(serie, playerID, listTourIgnore);
            if (memTour != null) {
                if (getConfigBooleanValue("checkDevicePlayTournament")) {
                    // check tournament not played by device
                    try {
                        if (isGameExistedOnTournamentForDevice(memTour.tourID, deviceID)) {
                            // tour already played by this device
                            listTourIgnore.add(memTour.tourID);
                        } else {
                            // tournament found
                            break;
                        }
                    } catch (Exception e) {
                        log.error("Exception to check game existed for tournament and device", e);
                        listTourIgnore.add(memTour.tourID);
                    }
                } else {
                    // no check on device => tournament found
                    break;
                }
            } else {
                // no tournament found
                break;
            }
        }
        return memTour;
    }

    /**
     * Create a game for player on tournament and dealIndex.
     * @param playerID
     * @param tour
     * @param dealIndexToStartLeave
     * @param deviceID
     * @return
     * @throws FBWSException if an existing game existed for player, tournament and dealIndex or if an exception occurs
     */
    private List<TourSerieGame> createGamesLeaved(long playerID, TourSerieTournament tour, int dealIndexToStartLeave, long deviceID) throws FBWSException {
        synchronized (lockCreateGame.getLock(tour.getIDStr()+"-"+playerID)) {
            List<TourSerieGame> listGameLeaved = new ArrayList<>();
            int dealIdx = dealIndexToStartLeave;
            while (dealIdx <= tour.getNbDeals()) {
                if (getGameOnTournamentAndDealForPlayer(tour.getIDStr(), dealIdx, playerID) == null) {
                    TourSerieGame game = new TourSerieGame(playerID, tour, dealIdx);
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
                        for (TourSerieGame game : listGameLeaved) {
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
    private TourSerieGame createGame(long playerID, TourSerieTournament tour, int dealIndex, int conventionProfil, String conventionData, int cardsConventionProfil, String cardsConventionValue, long deviceID) throws FBWSException {
        synchronized (lockCreateGame.getLock(tour.getIDStr()+"-"+playerID)) {
            if (getGameOnTournamentAndDealForPlayer(tour.getIDStr(), dealIndex, playerID) != null) {
                log.error("A game already exist on tour="+tour+" - dealIndex="+dealIndex+" - playerID="+playerID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            try {
                TourSerieGame game = new TourSerieGame(playerID, tour, dealIndex);
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

    /**
     * List game on a tournament and deal not finished
     * @param tourID
     * @param dealIndex
     * @return
     */
    @Override
    public List<TourSerieGame> listGameOnTournamentForDealNotFinished(String tourID, int dealIndex) {
        try {
            Query q = new Query();
            Criteria cNotFinished = Criteria.where("finished").is(false);
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cNotFinished));
            return mongoTemplate.find(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to list game not finished for tournament="+tourID+" and dealIndex="+dealIndex, e);
        }
        return null;
    }

    public List<TourSerieGame> listGameFinishedOnTournamentAndDeal(String tourID, int dealIndex, int offset, int nbMax) {
        Query q = new Query();
        Criteria cNotFinished = Criteria.where("finished").is(true);
        Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
        Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
        q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cNotFinished));
        q.with(new Sort(Sort.Direction.DESC, "score"));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return mongoTemplate.find(q, TourSerieGame.class);
    }

    @Override
    public TourSerieGame getFirstGameOnTournamentAndDealWithScoreAndContract(String tourID, int dealIndex, int score, String contract, int contractType) {
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
            return mongoTemplate.findOne(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament deal and playerID", e);
        }
        return null;
    }

    public void deleteGameOnTournamentForDealNotFinished(String tourID, int dealIndex) {
        try {
            Query q = new Query();
            Criteria cNotFinished = Criteria.where("finished").is(false);
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cNotFinished));
            mongoTemplate.remove(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to remove game not finished for tournament="+tourID+" and dealIndex="+dealIndex, e);
        }
    }

    public void deleteGameOnTournamentForDeal(String tourID, int dealIndex) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex));
            mongoTemplate.remove(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to remove game for tournament="+tourID+" and dealIndex="+dealIndex, e);
        }
    }

    /**
     * List game for player
     * @param playerID
     * @param offset
     * @param nbMax
     * @return
     */
    public List<TourSerieGame> listGameForPlayer(long playerID, int offset, int nbMax) {
        try {
            Query q = Query.query(Criteria.where("playerID").is(playerID)).with(new Sort(Sort.Direction.DESC, "startDate")).skip(offset).limit(nbMax);
            return mongoTemplate.find(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to list game for player="+playerID, e);
        }
        return null;
    }

    /**
     * Count games for player
     * @param playerID
     * @return
     */
    @Override
    public int countGamesForPlayer(long playerID) {
        try {
            Query q = new Query();
            Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            Criteria cFinished = Criteria.where("finished").is(true);
            q.addCriteria(new Criteria().andOperator(cPlayerID, cFinished));
            return (int) mongoTemplate.count(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to count games for player - playerID="+ playerID, e);
        }
        return 0;
    }

    /**
     * Count game with score > param value
     * @param tourID
     * @param dealIndex
     * @param score
     * @return
     */
    public int countGameWithBestScore(String tourID, int dealIndex, int score) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            Criteria cFinished = Criteria.where("finished").is(true);
            Criteria cScore = Criteria.where("score").gt(score);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cFinished, cScore));
            return (int)mongoTemplate.count(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to count game with best score - tourID="+tourID+" - dealIndex="+dealIndex+" - score="+score, e);
        }
        return -1;
    }

    /**
     * Count game with score > param value
     * @param tourID
     * @param result
     * @return
     */
    public int countTournamentPlayerWithBetterResult(String tourID, double result) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cResult = Criteria.where("result").gt(result);
            q.addCriteria(new Criteria().andOperator(cTourID, cResult));
            return (int)mongoTemplate.count(q, TourSerieTournamentPlayer.class);
        } catch (Exception e) {
            log.error("Failed to count tournament Player with better result - tourID="+tourID+" - result="+result, e);
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
    public int countGameWithSameScore(String tourID, int dealIndex, int score) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            Criteria cFinished = Criteria.where("finished").is(true);
            Criteria cScore = Criteria.where("score").is(score);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cFinished, cScore));
            return (int)mongoTemplate.count(q, TourSerieGame.class);
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
    public int countGameWithSameScoreAndContract(String tourID, int dealIndex, int score, String contract, int contractType) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            Criteria cFinished = Criteria.where("finished").is(true);
            Criteria cScore = Criteria.where("score").is(score);
            Criteria cContract = Criteria.where("contract").is(contract);
            Criteria cContractType = Criteria.where("contractType").is(contractType);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cFinished, cScore, cContract, cContractType));
            return (int)mongoTemplate.count(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to count game with same score and contract - tourID="+tourID+" - dealIndex="+dealIndex+" - score="+score+" - contract="+contract+" - contractType="+contractType, e);
        }
        return -1;
    }

    /**
     * List game on a tournament
     * @param tourID
     * @return
     */
    @Override
    public List<TourSerieGame> listGameOnTournament(String tourID){
        try {
            Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)));
            return mongoTemplate.find(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament and playerID", e);
        }
        return null;
    }

    /**
     * List game on a tournament
     * @param tourID
     * @return
     */
    @Override
    public List<TourSerieGame> listGameOnTournamentForPlayer(final String tourID, final long playerID){
        try {
            Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)))
                    .addCriteria(Criteria.where("playerID").is(playerID));
            return mongoTemplate.find(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament and playerID", e);
        }
        return null;
    }


    /**
     * List game on a tournament
     * @param tourID
     * @return
     */
    public List<TourSerieGameLoadData> listGameDataOnTournament(String tourID){
        try {
            List<TourSerieGameLoadData> result = new ArrayList<>();
            BasicDBObject req = BasicDBObject.parse("{\"tournament.$id\":{$oid:\"" + tourID + "\"}}") ;
            FindIterable<Document> dbCursor = mongoTemplate.getCollection("serie_game").find(req);
            if (dbCursor != null) {
                for (Document e : dbCursor) {
                    TourSerieGameLoadData gameLoadData = TourSerieGameLoadData.loadFromDBObject(e);
                    if (gameLoadData != null) {
                        result.add(gameLoadData);
                    } else {
                        log.error("Failed to load TourSerieGameLoadData from Document="+e+" - _id="+e.getObjectId("_id"));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to find game for tournament and playerID", e);
        }
        return null;
    }

    /**
     * Return the game not finished for a player on tournament.
     * @param tourID
     * @param playerID
     * @return
     */
    @Override
    protected TourSerieGame getGameNotFinishedOnTournamentForPlayer(String tourID, long playerID) {
        List<TourSerieGame> listGame = listGameOnTournamentForPlayer(tourID, playerID);
        if (listGame != null) {
            for (TourSerieGame g : listGame) {
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
    @Override
    public TourSerieGame getGameOnTournamentAndDealForPlayer(String tourID, int dealIndex, long playerID) {
        try {
            Query q = new Query();
            Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cPlayerID));
            return mongoTemplate.findOne(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament deal and playerID", e);
        }
        return null;
    }

    /**
     * Check if game existed for tournament and device
     * @param tourID
     * @param deviceID
     * @return
     * @throws FBWSException
     */
    public boolean isGameExistedOnTournamentForDevice(String tourID, long deviceID) throws FBWSException {
        try {
            Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("deviceID").is(deviceID)));
            return mongoTemplate.exists(q, TourSerieGame.class);
        } catch (Exception e) {
            log.error("Failed to check if game exist for tournament="+tourID+" and deviceID="+deviceID, e);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
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
        if (table.getTournament() == null || !(table.getTournament() instanceof TourSerieTournament)) {
            log.error("Touranment on table is not TIMEZONE table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!(table.getGame() instanceof TourSerieGame)) {
            log.error("Game on table is not TIMEZONE table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TourSerieGame game = (TourSerieGame)session.getCurrentGameTable().getGame();
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
        // check if player has enough credit to leave tournament
        int nbDealToLeave = game.getTournament().getNbDeals() - table.getNbPlayedDeals() - 1; // -1 because credit already decrement for current game !
        if (nbDealToLeave > 0) {
            playerMgr.checkPlayerCredit(p, game.getTournament().getNbCreditsPlayDeal() * nbDealToLeave);
        }

        // leave current game
//        synchronized (game) {
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
            memoryMgr.updateResult(game, session.getPlayerCache(), true);
        }
        // leave game for other deals
        List<TourSerieGame> listOtherGamesLeaved = createGamesLeaved(session.getPlayer().getID(), game.getTournament(), game.getDealIndex()+1, session.getDeviceID());
        int nbDealNotPlayed = 0;
        if (listOtherGamesLeaved != null) {
            for (TourSerieGame gameLeave : listOtherGamesLeaved) {
                memoryMgr.updateResult(gameLeave, session.getPlayerCache(), true);
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
            playerMgr.updatePlayerCreditDeal(p,game.getTournament().getNbCreditsPlayDeal()*nbDealNotPlayed, nbDealNotPlayed);
            session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_NEWSERIE, nbDealNotPlayed);
        }

        // return credit remaining of player
        return p.getTotalCreditAmount();
    }

    /**
     * Return the serie ranking
     * @param serie
     * @return
     */
    public TourSerieMemPeriodRanking getSerieRanking(String serie) {
        return mapSerieRanking.get(serie);
    }

    public List<GameGroupMapping> testSelectGame(String tourID, int dealIndex) {
        TypedAggregation<TourSerieGame> aggGame = Aggregation.newAggregation(
                TourSerieGame.class,
                Aggregation.match(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("dealIndex").is(dealIndex))),
                Aggregation.group("score","contract","contractType").addToSet("declarer").as("declarer").addToSet("tricks").as("tricks").addToSet("result").as("result").addToSet("rank").as("rank").count().as("nbPlayer"),
                Aggregation.sort(Sort.Direction.DESC, "score")
        );
        AggregationResults<GameGroupMapping> results = mongoTemplate.aggregate(aggGame, GameGroupMapping.class);
        return results.getMappedResults();
    }

    public String testUpdateResult() {
        int nbGameByPlayer = getConfigIntValue("nbGameByPlayer", 4);
        if (nbGameByPlayer <= 0 || nbGameByPlayer > 4) {
            nbGameByPlayer = 1;
        }
        String[] arrayContractScore = getConfigStringValue("testArrayContractScore","1H-100-1;1S-200-1;1N-200-1;2H-100-1;3C-200-1;4N-200-1").split(";");
        long[] testArrayPlayerID = new long[getConfigIntValue("testNbPlayer", 1000)];
        for (int i = 0; i < testArrayPlayerID.length; i++) {
            testArrayPlayerID[i] = 1000+i;
            PlayerCache pc = playerCacheMgr.getPlayerCache(testArrayPlayerID[i]);
            if (pc.pseudo == null) {
                getOrCreatePlayerSerie(testArrayPlayerID[i]);
                pc.pseudo = "playerPseudo1234567890-" + testArrayPlayerID[i];
                pc.serie = SERIE_NC;
                pc.serieLastPeriodPlayed = "";
                pc.countryCode = "FR";
            }
        }

        long[] arrayTS = new long[testArrayPlayerID.length];
        int nbUpdateOK = 0, nbUpdateFailed = 0;
        long tsBegin = System.currentTimeMillis();
        List<TourSerieGame> listGame = new ArrayList<>();
        for (int j = 0; j < nbGameByPlayer; j++) {
            for (int i = 0; i < testArrayPlayerID.length; i++) {
                long playerID = testArrayPlayerID[i];
                long ts = System.currentTimeMillis();
                PlayerCache pc = playerCacheMgr.getPlayerCache(playerID);
                TourSerieMemTour memTour = memoryMgr.getTournamentInProgressForPlayer(pc.serie, playerID);
                if (memTour == null) {
                    memTour = memoryMgr.getTournamentNotPlayedForPlayer(pc.serie, playerID, null);
                }
                if (memTour != null) {
                    TourSerieMemTourPlayer mtp = memTour.getRankingPlayer(playerID);
                    int index = 1;
                    if (mtp != null) {
                        index = mtp.getNbDealsPlayed() + 1;
                    }
                    Random random = new Random(System.nanoTime());
                    int idxArray = random.nextInt(arrayContractScore.length);
                    String[] contractScore = arrayContractScore[idxArray].split("_");

                    TourSerieGame game = new TourSerieGame(playerID, getTournament(memTour.tourID), index);
                    game.setScore(Integer.parseInt(contractScore[1]));
                    game.setContract(contractScore[0]);
                    game.setContractType(Integer.parseInt(contractScore[2]));
                    game.setDeclarer('S');
                    game.setTricks(7);
                    game.setFinished(true);
                    try {
                        memoryMgr.updateResult(game, pc, true);
                        listGame.add(game);
                        nbUpdateOK++;
                    } catch (FBWSException e) {
                        nbUpdateFailed++;
                        log.error("Exception to update result for game=" + game, e);
                    }
                }
                arrayTS[i] = System.currentTimeMillis() - ts;
            }
        }
        long tsBeforeInsertGame = System.currentTimeMillis();
        try {
            insertListGameDB(listGame);
        } catch (FBWSException e) {
            log.error("Exception to insert games", e);
        }
        long tsInsertGame = System.currentTimeMillis() - tsBeforeInsertGame;

        String result = "nbUpdateOK="+nbUpdateOK+" - nbUpdateFailed="+nbUpdateFailed+" - time to insert to DB ="+tsInsertGame+" - Total time = "+(System.currentTimeMillis()-tsBegin);
        long tsAverage = 0;
        long temp = 0;
        for (long e : arrayTS) {
            temp +=e;
        }
        tsAverage = temp / arrayTS.length;
        result += " - average ts="+tsAverage;
        result += " - ts first="+arrayTS[0]+" - ts last="+arrayTS[arrayTS.length-1];
        return result;
    }

    /**
     * Return the ranking for player on current period
     * @param serie
     * @param playerID
     * @return
     */
    public TourSerieMemPeriodRankingPlayer getCurrentRankingPlayer(String serie, long playerID) {
        TourSerieMemPeriodRanking serieRanking = getSerieRanking(serie);
        if (serieRanking != null) {
            return serieRanking.getPlayerRank(playerID);
        }
        return null;
    }

    public WSTournament toWSTournament(TourSerieTournament tour, PlayerCache playerCache) {
        WSTournament wst = tour.toWS();
        TourSerieMemTour memTour = memoryMgr.getMemTour(tour.getSerie(), tour.getIDStr());
        if (memTour != null) {
            wst.nbTotalPlayer = memTour.getNbPlayersFinish();
            wst.resultPlayer = memTour.getWSResultPlayer(playerCache, true);
            if (wst.resultPlayer != null) {
                wst.playerOffset = wst.resultPlayer.getRank();
                wst.nbTotalPlayer = wst.resultPlayer.getNbTotalPlayer();
            }
            wst.currentDealIndex = memTour.getCurrentDealForPlayer(playerCache.ID);
        } else {
            wst.nbTotalPlayer = tour.getNbPlayers();
            TourSerieTournamentPlayer tp = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
            if (tp != null) {
                wst.resultPlayer = tp.toWSResultTournamentPlayer(playerCache, playerCache.ID);
            }
        }
        return wst;
    }

    /**
     * Return list of result deal on tournament for player
     * @param tour
     * @param playerID
     * @return
     */
    public List<WSResultDeal> resultListDealForTournamentForPlayer(final TourSerieTournament tour, final long playerID) throws FBWSException {
        if (tour == null) {
            log.error("Parameter not valid");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        final TourSerieMemTour mtr = memoryMgr.getMemTour(tour.getSerie(), tour.getIDStr());
        int dealIndex = -1;
        if(mtr != null) {
            final TourSerieMemTourPlayer player = mtr.getOrCreateRankingPlayer(playerID);
            if (player != null) {
                dealIndex = player.currentDealIndex;
            }
            if (dealIndex == -1) {
                memoryMgr.resetCurrentDealForPlayerOnTournament(tour, playerID);
                if (player != null) {
                    dealIndex = player.currentDealIndex;
                }
            }
        }
        final List<WSResultDeal> listResultDeal = new ArrayList<>();
        int loopDealIndex = 1;
        for (TourSerieDeal deal : tour.listDeal) {
            String dealID = deal.getDealID(tour.getIDStr());
            WSResultDeal resultPlayer = new WSResultDeal();
            resultPlayer.setDealIDstr(dealID);
            resultPlayer.setDealIndex(deal.index);
            resultPlayer.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
            // Tour in memory
            if (mtr != null) {
                TourSerieMemDeal mrd = mtr.getResultDeal(dealID);
                if (mrd != null) {
                    TourSerieMemDealPlayer memResPla = mrd.getResultPlayer(playerID);
                    if(loopDealIndex < dealIndex && memResPla == null){
                        // Cannot get the game from memory, refresh from db
                        memoryMgr.reloadDealsForPlayerOnTournament(playerID, mtr);
                        memResPla = mrd.getResultPlayer(playerID);
                        log.warn("PlayerID {} : The game cannot be found in memory, retrieving from database ...", playerID);
                    }
                    if (memResPla != null) {
                        resultPlayer.setPlayed(true);
                        resultPlayer.setContract(memResPla.getContractWS());
                        resultPlayer.setDeclarer(memResPla.declarer);
                        resultPlayer.setNbTricks(memResPla.nbTricks);
                        resultPlayer.setScore(memResPla.score);
                        resultPlayer.setRank(memResPla.nbPlayerBestScore+1);
                        resultPlayer.setResult(memResPla.result);
                        resultPlayer.setNbTotalPlayer(mrd.getNbPlayer());
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
                TourSerieGame game = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerID);
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
            loopDealIndex += 1;
        }
        return listResultDeal;
    }

    /**
     * Retrieve tournament wich include this deal
     * @param dealID
     * @return
     */
    @Override
    public TourSerieTournament getTournamentWithDeal(String dealID) {
        String tourID = extractTourIDFromDealID(dealID);
        if (tourID != null) {
            return getTournament(tourID);
        }
        return null;
    }

    /**
     * Return tournamentPlayer object for a player on a tournament
     * @param tourID
     * @param playerID
     * @return
     */
    @Override
    public TourSerieTournamentPlayer getTournamentPlayer(String tourID, long playerID) {
        return mongoTemplate.findOne(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("playerID").is(playerID))), TourSerieTournamentPlayer.class);
    }

    /**
     * List tournamentPlayer object for a player (order by lastDate desc)
     * @param playerID
     * @param offset
     * @param nbMax
     * @return
     */
    public List<TourSerieTournamentPlayer> listTournamentPlayerOrderDateDes(long playerID, int offset, int nbMax) {
        return mongoTemplate.find(Query.query(Criteria.where("playerID").is(playerID)).with(new Sort(Sort.Direction.DESC, "lastDate")).skip(offset).limit(nbMax), TourSerieTournamentPlayer.class);
    }

    /**
     * Return results on deal for a player. Deal must be played by this player. Same contract are grouped
     * @param dealID
     * @param playerCache
     * @param useLead
     * @return
     * @throws FBWSException
     */
    public WSResultDealTournament resultDealGroup(String dealID, PlayerCache playerCache, boolean useLead) throws FBWSException {
        TourSerieTournament tour = getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        TourSerieDeal deal = (TourSerieDeal)tour.getDeal(dealID);
        if (deal == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // check deal played by this player
        TourSerieGame gamePlayer = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
        if (gamePlayer == null) {
            log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
            // if game not found, try to get last game of the tournament
            List<TourSerieGame> listGame = listGameOnTournamentForPlayer(tour.getIDStr(), playerCache.ID);
            if (listGame != null && !listGame.isEmpty()) {
                gamePlayer = listGame.get(listGame.size()-1);
            }
            if (gamePlayer == null) {
                log.error("No game found for tour="+tour+" - playerID="+playerCache.ID);
                throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
            }
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
                result.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
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

            TourSerieTournamentPlayer tp = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
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
            TourSerieMemTour memTour = memoryMgr.getMemTour(tour.getSerie(), tour.getIDStr());
            if (memTour == null) {
                log.error("No memTour found for tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            TourSerieMemDeal memDeal = memTour.getResultDeal(dealID);
            if (memDeal == null) {
                log.error("No memDeal found for tour="+tour+" - dealID="+dealID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            int rankPlayer = -1;
            List<TourSerieMemDealPlayer> listResultMem = memDeal.getListResultOrderScore();
            int nbTotalPlayer = listResultMem.size();
            // Map with nb game with same score and contract
            HashMap<String, Integer> mapRankNbPlayer = new HashMap<>(); /* Map to store the number of player for each score */
            for (TourSerieMemDealPlayer result : listResultMem) {
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
                    rankPlayer = result.nbPlayerBestScore+1;
                }
            }
            List<WSResultDeal> listResult = new ArrayList<>();
            List<String> listScoreContract = new ArrayList<>();
            int idxPlayer = -1;
            int idxSameRankBefore = -1;
            String playerContract = "";
            for (TourSerieMemDealPlayer resPla : listResultMem) {
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
                    resDeal.setRank(resPla.nbPlayerBestScore+1);
                    resDeal.setNbTotalPlayer(nbTotalPlayer);
                    resDeal.setLead(resPla.begins);
                    if (mapRankNbPlayer.get(key) != null) {
                        resDeal.setNbPlayerSameGame(mapRankNbPlayer.get(key));
                    }
                    resDeal.setNbTricks(resPla.nbTricks);
                    resDeal.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
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
            wstour.remainingTime = tour.getTsDateEnd() - System.currentTimeMillis();
            wstour.nbTotalPlayer = memTour.getNbPlayersFinish();
            wstour.resultPlayer = memTour.getWSResultPlayer(playerCache, true);
            if (wstour.resultPlayer != null) {
                wstour.playerOffset = wstour.resultPlayer.getRank();
            }
            final List<TourSerieGame> tournamentGamesForPlayer = this.listGameOnTournamentForPlayer(tour.getIDStr(), playerCache.ID);
            Collections.sort(tournamentGamesForPlayer, Comparator.comparingInt(Game::getDealIndex));
            if(tournamentGamesForPlayer.isEmpty()) {
                wstour.currentDealIndex = -1;
            } else {
                final Game lastGame = tournamentGamesForPlayer.get(tournamentGamesForPlayer.size()-1);
                if(lastGame.isFinished()) {
                    wstour.currentDealIndex = -1;
                } else {
                    wstour.currentDealIndex = lastGame.getDealIndex();
                }
            }
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
    public WSResultDealTournament resultDealNotGroup(String dealID, PlayerCache playerCache, List<Long> listPlaFilter, int offset, int nbMax) throws FBWSException {
        TourSerieTournament tour = getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        TourSerieDeal deal = (TourSerieDeal)tour.getDeal(dealID);
        if (tour == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // check deal played by this player
        TourSerieGame gamePlayer = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
        if (gamePlayer == null) {
            log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
            throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
        }
        WSResultDealTournament resultDealTour = new WSResultDealTournament();
        List<WSResultDeal> listResult = new ArrayList<>();
        int idxPlayer = -1;
        int indexSameRank = -1; // important to initialize with -1 => indicate no yet done (index of the first same result with same rank)
        boolean plaProcess = false; // flag to indicate that the player result has been processed in the result list
        int nbTotalPlayer = 0;
        WSTournament wstour = tour.toWS();
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
            offsetPlayer = countGameWithBestScore(tour.getIDStr(), deal.index, gamePlayer.getScore());
            if (offset == -1 && nbMax > 0) {
                // center on the player result
                offset = gamePlayer.getRank() - (nbMax/2);
            }
            if (offset < 0) {
                offset = 0;
            }
            List<TourSerieGame> listGame = listGameFinishedOnTournamentAndDeal(tour.getIDStr(), deal.index, offset, nbMax);

            if (listGame != null) {
                for (TourSerieGame tg : listGame) {
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
                    result.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
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
            wstour.remainingTime = 0;
            TourSerieTournamentPlayer tp = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
            if (tp != null) {
                WSResultTournamentPlayer resPla = tp.toWSResultTournamentPlayer(playerCache, playerCache.ID);
                wstour.resultPlayer = resPla;
                if (wstour.resultPlayer != null) {
                    wstour.playerOffset = wstour.resultPlayer.getRank();
                }
            } else {
                log.error("No tournament play found for player="+playerCache.ID+" - tournament="+tour);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }

        }
        //**************************************
        // TOURNAMENT NOT FINISHED => FIND RESULT IN MEMORY
        else {
            TourSerieMemTour memTour = memoryMgr.getMemTour(tour.getSerie(), tour.getIDStr());
            if (memTour == null) {
                log.error("No memTour found for tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            TourSerieMemDeal memDeal = memTour.getResultDeal(dealID);
            if (memDeal == null) {
                log.error("No memDeal found for tour="+tour+" - dealID="+dealID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            TourSerieMemDealPlayer resPlayer = memDeal.getResultPlayer(playerCache.ID);
            offsetPlayer = -1;
            if (resPlayer != null) {
                offsetPlayer = resPlayer.nbPlayerBestScore;
            } else {
                log.error("Oups no memResultDeal for player="+playerCache.ID+" - deal="+deal);
            }
            if (offset == -1 && nbMax > 0) {
                // center on the player result
                if (resPlayer != null) {
                    offset = (resPlayer.nbPlayerBestScore+1) - (nbMax/2);
                }
            }
            if (offset < 0) {
                offset = 0;
            }
            List<TourSerieMemDealPlayer> listResultMem = memDeal.getListResultOrderScore();
            if (nbMax > 0) {
                if (offset < listResultMem.size()) {
                    listResultMem = listResultMem.subList(offset, (offset+nbMax) < listResultMem.size()?(offset+nbMax):listResultMem.size());
                }
            }
            for (TourSerieMemDealPlayer resPla : listResultMem) {
                if (listResult.isEmpty()) {
                    if (resPla.nbPlayerBestScore < offsetPlayer) {
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
                resDeal.setRank(resPla.nbPlayerBestScore+1);
                resDeal.setNbTotalPlayer(memDeal.getNbPlayer());
                resDeal.setNbTricks(resPla.nbTricks);
                resDeal.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
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
                nbTotalPlayer = memDeal.getNbPlayer();
            }
            wstour.remainingTime = tour.getTsDateEnd() - System.currentTimeMillis();
            wstour.nbTotalPlayer = memTour.getNbPlayersFinish();
            wstour.resultPlayer = memTour.getWSResultPlayer(playerCache, true);
            if (wstour.resultPlayer != null) {
                wstour.playerOffset = wstour.resultPlayer.getRank();
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
                TourSerieGame gameToReplace = getFirstGameOnTournamentAndDealWithScoreAndContract(tour.getIDStr(), deal.index, gamePlayer.getScore(), gamePlayer.getContract(), gamePlayer.getContractType());
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
                        resDealPla.setConnected(presenceMgr.isSessionForPlayerID(pc.ID));
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
                    resDealPla.setConnected(presenceMgr.isSessionForPlayerID(playerCache.ID));
                    idxPlayer = indexSameRank;
                }
            }
        }
        resultDealTour.offset = offset;
        resultDealTour.totalSize = nbTotalPlayer;
        resultDealTour.listResultDeal = listResult;
        resultDealTour.tournament = wstour;
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_PLAYER_CONTRACT, playerContract));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_INDEX_PLAYER, ""+idxPlayer));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_DEAL_INDEX, ""+deal.index));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, ""+nbTotalPlayer));
        return resultDealTour;
    }

    /**
     * Return result for player on tournament
     * @param tour
     * @param playerCache
     * @param useRankingFinished
     * @return
     */
    public WSResultTournamentPlayer resultPlayerOnTournament(TourSerieTournament tour, PlayerCache playerCache, boolean useRankingFinished) {
        TourSerieMemTour memTour = memoryMgr.getMemTour(tour.getSerie(), tour.getIDStr());
        if (memTour != null) {
            return memTour.getWSResultPlayer(playerCache, useRankingFinished);
        }
        TourSerieTournamentPlayer tp = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
        if (tp != null) {
           return tp.toWSResultTournamentPlayer(playerCache, playerCache.ID);
        }
        return null;
    }

    /**
     * Return the number of player on tournament
     * @param tour
     * @param onlyFinisher
     * @return
     */
    public int getNbPlayerOnTournament(TourSerieTournament tour, List<Long> listFollower, boolean onlyFinisher) {
        int nbPlayer = 0;
        if (tour.isFinished()) {
            if (listFollower != null && listFollower.size() > 0) {
                nbPlayer = countPlayerForTournamentFinished(tour.getIDStr(), listFollower);
            } else {
                nbPlayer = tour.getNbPlayers();
            }
        } else {
            TourSerieMemTour memTour = memoryMgr.getMemTour(tour.getSerie(), tour.getIDStr());
            if (memTour != null) {
                nbPlayer = memTour.getNbPlayers(listFollower, onlyFinisher);
            }
        }
        return nbPlayer;
    }

    /**
     * Retun list of result player for tournament
     * @param tour
     * @param offset
     * @param nbMaxResult
     * @param listFollower
     * @param playerAsk
     * @return
     */
    public List<WSResultTournamentPlayer> resultListTournament(TourSerieTournament tour, int offset, int nbMaxResult, List<Long> listFollower, long playerAsk, boolean useRankingFinished, WSResultTournamentPlayer resultPlayerAsk) {
        List<WSResultTournamentPlayer> listResult = new ArrayList<>();
        TourSerieMemTour memTour = memoryMgr.getMemTour(tour.getSerie(), tour.getIDStr());
        int idxPlayer = -1;
        if (memTour != null) {
            List<TourSerieMemTourPlayer> listRanking = memTour.getRanking(offset, nbMaxResult, listFollower, useRankingFinished);
            boolean includePlayerAskResult = false;
            if (resultPlayerAsk != null && resultPlayerAsk.getNbDealPlayed() < memTour.getNbDeals() && useRankingFinished) {
                // case of ranking with only players finished and player asked has not finished => include the player result in the list and increment nb player
                includePlayerAskResult = true;
            }
            boolean resultPlayerAskAdded = false;
            for (TourSerieMemTourPlayer e : listRanking) {
                // only player with at leat one deal finished
                if (e.getNbDealsPlayed() > 0) {
                    WSResultTournamentPlayer resultPlayer = e.toWSResultTournamentPlayer();
                    int nbTotalPlayer = 0;
                    if (useRankingFinished) {
                        resultPlayer.setRank(e.rankingFinished);
                        nbTotalPlayer = memTour.getNbPlayersFinish();
                    } else {
                        resultPlayer.setRank(e.ranking);
                        nbTotalPlayer = memTour.getNbPlayersForRanking();
                    }
                    resultPlayer.setNbTotalPlayer(nbTotalPlayer);
                    // process for include player ask (tournament not finished and ranking with only finished results)
                    if (includePlayerAskResult && resultPlayer.getResult() <= resultPlayerAsk.getResult() && !resultPlayerAskAdded) {
                        idxPlayer = listResult.size();
                        listResult.add(resultPlayerAsk);
                        resultPlayerAskAdded = true;
                    }
                    PlayerCache pc = playerCacheMgr.getPlayerCache(e.playerID);
                    resultPlayer.setPlayerPseudo(pc.getPseudo());
                    resultPlayer.setConnected(presenceMgr.isSessionForPlayerID(pc.ID));
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
            List<TourSerieTournamentPlayer> listTP = mongoTemplate.find(q, TourSerieTournamentPlayer.class);
            if (listTP != null && listTP.size() > 0) {
                for (TourSerieTournamentPlayer tp : listTP) {
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

    private void addLogAndReport(List<String> listMsgReport, String msg, Exception e) {
        if (e != null) {
            log.error(msg, e);
        } else {
            log.warn(msg);
        }
        if (listMsgReport != null) {
            if (e != null) {
                listMsgReport.add(Constantes.timestamp2StringDateHour(System.currentTimeMillis())+" - "+msg+" - exception="+e.getMessage());
            } else {
                listMsgReport.add(Constantes.timestamp2StringDateHour(System.currentTimeMillis())+" - "+msg);
            }
        }
    }
    /**
     * Finish all tournament, compute serie ranking, update player serie and clear data from memory
     */
    public void finishCurrentPeriod() {
        if (currentPeriod != null && !currentPeriod.isFinished()) {
            boolean reportNotifEnable = getConfigBooleanValue("reportNotifEnable");
            long tsEndNextPeriod = transformPeriodID2TS(periodIDNext1, false);
            List<Long> listPlaNCUp = new ArrayList<>();
            List<Long> listPlaNCMaintain = new ArrayList<>();
            List<String> listMsgReport = new ArrayList<>();
            addLogAndReport(listMsgReport, "Finish current period starting ... period=" + currentPeriod + " - reportNotifEnable=" + reportNotifEnable, null);
            addLogAndReport(listMsgReport, "Nb player in series exclude NC & TOP =" + countPlayerInSeriesExcludeNC_TOP() + " - Nb player in series exclude NC & TOP & Reserve =" + countPlayerInSeriesExcludeNC_TOP_Reserve(false), null);
            for (String serie : SERIE_TAB) {
                addLogAndReport(listMsgReport, "---------------------------------------- "+serie+" ----------------------------------------", null);
                addLogAndReport(listMsgReport, "Process serie : " + serie, null);
                if (!serie.equals(SERIE_NC)) {
                    addLogAndReport(listMsgReport, "Nb player in serie =" + countPlayerInSerie(serie) + " - Nb player in serie exclude Reserve =" + countPlayerInSerieExcludeReserve(serie, false), null);
                }
                TourSerieMemPeriodRanking serieRanking = mapSerieRanking.get(serie);
                // count players with best serie (players active !)
                int nbPlayerWithBestSerie = 0;
                for (TourSerieMemPeriodRanking rs : mapSerieRanking.values()) {
                    try {
                        if (compareSerie(rs.serie, serie) > 0) {
                            nbPlayerWithBestSerie += rs.getNbPlayerActive(null, null);
                        }
                    } catch (Exception e) {
                        log.error("Exception to compare serie - rs.serie="+rs.serie+" - serie="+serie, e);
                    }
                }
                if (serieRanking != null) {
                    //*********************************
                    // Finish all tournament from memory for all series
                    long ts = System.currentTimeMillis();
                    int nbTourFinish = memoryMgr.finishAllTournamentForSerie(serie, currentPeriod.getPeriodID());
                    addLogAndReport(listMsgReport, "Time to finish tournament for serie="+serie+" - nbTourFinish="+nbTourFinish+" - ts="+(System.currentTimeMillis() - ts), null);
                    List<Long> listPlaUp = new ArrayList<>();
                    List<Long> listPlaDown = new ArrayList<>();
                    List<Long> listPlaMaintain = new ArrayList<>();
                    List<UpdateBestResult> listUpdateBestResult = new ArrayList<>();
                    // compute serie ranking
                    ts = System.currentTimeMillis();
                    serieRanking.computeRanking();
                    addLogAndReport(listMsgReport, "Time to compute ranking for serie=" + serie + " - nbPlayerActive=" + serieRanking.getNbPlayerActive(null, null) + " - ts=" + (System.currentTimeMillis() - ts), null);

                    //***************************************
                    // create period result for active player
                    ts = System.currentTimeMillis();
                    List<TourSeriePeriodResult> listPeriodResult = new ArrayList<>();
                    List<Long> listPlaID = serieRanking.getListPlayerOrderTrendAndResult();
//                    for (TourSerieMemPeriodRankingPlayer rankPla : serieRanking.getAllResult()) {
                    for (Long l : listPlaID) {
                        TourSerieMemPeriodRankingPlayer rankPla = serieRanking.getPlayerRank(l);
                        if (rankPla == null) {
                            continue;
                        }
                        if (rankPla.countryCode == null || rankPla.countryCode.length() == 0) {
                            PlayerCache pc = playerCacheMgr.getPlayerCache(rankPla.playerID);
                            if (pc != null) {
                                rankPla.countryCode = pc.countryCode;
                            }
                        }
                        if (rankPla.trend > 0) {
                            if (serie.equals(SERIE_NC)) {
                                listPlaNCUp.add(rankPla.playerID);
                            } else {
                                listPlaUp.add(rankPla.playerID);
                            }
                        } else if (rankPla.trend < 0) {
                            listPlaDown.add(rankPla.playerID);
                        } else if (rankPla.trend == 0) {
                            if (serie.equals(SERIE_NC)) {
                                listPlaNCMaintain.add(rankPla.playerID);
                            } else {
                                listPlaMaintain.add(rankPla.playerID);
                            }
                        }

                        TourSeriePeriodResult spr = new TourSeriePeriodResult();
                        spr.setPlayerID(rankPla.playerID);
                        spr.setCountryCode(rankPla.countryCode);
                        spr.setSerie(serie);
                        spr.setPeriodID(currentPeriod.getPeriodID());
                        spr.setEvolution(rankPla.trend);
                        spr.setNbTournamentPlayed(rankPla.getNbTournamentPlayed());
                        spr.setRank(rankPla.rank);
                        spr.setRankMain(nbPlayerWithBestSerie + rankPla.rank);
                        spr.setResult(rankPla.result);
                        spr.setNbPlayer(serieRanking.getNbPlayerActive(null, null));
                        listPeriodResult.add(spr);

                        if (!serie.equals(SERIE_NC)) {
                            if (rankPla.isToUpdateBestResult(serie)) {
                                UpdateBestResult e = new UpdateBestResult();
                                e.playerID = rankPla.playerID;
                                e.rank = rankPla.rank;
                                listUpdateBestResult.add(e);
                            }
                        }
                    }// end loop rankingPlayer on serie

                    // save period result list
                    try {
                        mongoTemplate.insertAll(listPeriodResult);
                    } catch (Exception e) {
                        addLogAndReport(listMsgReport, "Failed to insert list period result - listPeriodResult size=" + listPeriodResult.size() + " - serie=" + serie, e);
                    }
//                    addLogAndReport(listMsgReport, "Time to insert period result serie=" + serie + " - nb period result=" + listPeriodResult.size() + " - ts=" + (System.currentTimeMillis() - ts), null);

                    //*******************************
                    // update best result before
                    if (listUpdateBestResult.size() > 0) {
                        ts = System.currentTimeMillis();
                        List<Long> listPlaNotifUpdateBestResult = new ArrayList<>();
                        for (UpdateBestResult br : listUpdateBestResult) {
                            try {
                                mongoTemplate.updateFirst(Query.query(Criteria.where("playerID").is(br.playerID)),
                                        new Update().set("bestSerie", serie).set("bestRank", br.rank).set("bestPeriod", currentPeriod.getPeriodID()), TourSeriePlayer.class);
                                listPlaNotifUpdateBestResult.add(br.playerID);
                            } catch (Exception e) {
                                addLogAndReport(listMsgReport, "Failed to update TourSeriePlayer best result for playerID="+br.playerID, e);
                            }
                        }
                        if (listPlaNotifUpdateBestResult.size() > 0) {
                            for (Long playerID : listPlaNotifUpdateBestResult) {
                                notifMgr.createNotifSerieNewUpdateBestResult(playerID, tsEndNextPeriod);
                            }
                            addLogAndReport(listMsgReport, "Add notif update best result for nbPla=" + listPlaNotifUpdateBestResult.size() + " - result=" + true + " - ts=" + (System.currentTimeMillis() - ts), null);
                        }
                        addLogAndReport(listMsgReport, "Time to update best result for serie=" + serie + " - nb update=" + listUpdateBestResult.size() + " - ts=" + (System.currentTimeMillis() - ts), null);
                    }

                    //***************************************
                    // get podium for notifs
                    Map<String, String> podiumTemplateParameters = new HashMap<>();
                    List<Long> podiumPlayers = serieRanking.getListPlayerOrderTrendAndResult();
                    for (int i = 0; i < 3; i++) {
                        if (podiumPlayers.size() > i) {
                            PlayerCache playerCache = playerCacheMgr.getPlayerCache(podiumPlayers.get(i));
                            podiumTemplateParameters.put("PLA_PSEUDO" + i, playerCache.getPseudo());
                            podiumTemplateParameters.put("COUNTRY_PLA_PSEUDO" + i, playerCache.countryCode);
                        } else {
                            podiumTemplateParameters.put("PLA_PSEUDO" + i, "Unknown");
                            podiumTemplateParameters.put("COUNTRY_PLA_PSEUDO" + i, "FR");
                        }
                    }

                    //**********************
                    // update player serie (except NC serie)
                    if (!serie.equals(SERIE_NC)) {
                        String serieNext = serie;
                        // Move DOWN (not for serie 11)
                        if (!serie.equals(SERIE_11) && listPlaDown.size() > 0) {
                            serieNext = computeSerieEvolution(serie, -1, false);
                            if (log.isDebugEnabled()) {
                                log.debug("Down - Serie="+serie+" - serieNext="+serieNext+" - list="+StringTools.listToString(listPlaDown));
                            }
                            ts = System.currentTimeMillis();
                            if (!updateSerieForPlayers(listPlaDown, serieNext , true)) {
                                addLogAndReport(listMsgReport, "Failed to update player serie down for serie=" + serie+" - serieNext="+serieNext, null);
                            }
                            addLogAndReport(listMsgReport, "Update player serie="+serie+" - DOWN to "+serieNext+" - nb="+listPlaDown.size()+" - ts="+(System.currentTimeMillis() - ts), null);
                            // update player serie in cache
                            ts = System.currentTimeMillis();
                            playerCacheMgr.updatePlayerSerieForList(listPlaDown, serieNext);
//                            addLogAndReport(listMsgReport, "Update playerCache serie="+serie+" - move DOWN to "+serieNext+" - nb="+listPlaDown.size()+" - ts="+(System.currentTimeMillis() - ts), null);
                            // Send add notif
                            if (reportNotifEnable) {
                                ts = System.currentTimeMillis();
                                for (Long playerID : listPlaDown) {
                                    TourSerieMemPeriodRankingPlayer rankPla = serieRanking.getPlayerRank(playerID);
                                    if (rankPla != null) {
                                        notifMgr.createNotifSerieNewDown(playerID, serieNext, serie, rankPla, serieRanking.getNbPlayerActive(null, null), podiumTemplateParameters, tsEndNextPeriod);
                                    }
                                }
//                                addLogAndReport(listMsgReport, "Add notif NewDown for nbPla=" + listPlaDown.size() + " - result=" + bAddNotif + " - ts=" + (System.currentTimeMillis() - ts), null);
                            }
                        }
                        // Maintain
                        if (listPlaMaintain.size() > 0) {
                            if (log.isDebugEnabled()) {
                                log.debug("Maintain - Serie="+serie+" - serieNext="+serie+" - list="+StringTools.listToString(listPlaMaintain));
                            }
                            addLogAndReport(listMsgReport, "Update player serie=" + serie + " - MAINTAIN - nb=" + listPlaMaintain.size(), null);
                            // Send add notif
                            if (reportNotifEnable) {
                                ts = System.currentTimeMillis();
                                for (Long playerID : listPlaMaintain) {
                                    TourSerieMemPeriodRankingPlayer rankPla = serieRanking.getPlayerRank(playerID);
                                    if (rankPla != null) {
                                        notifMgr.createNotifSerieNewMaintain(playerID, serie, rankPla, serieRanking.getNbPlayerActive(null, null), podiumTemplateParameters, tsEndNextPeriod);
                                    }
                                }
                            }
                        }
                        // Move UP (not for serie TOP)
                        if (!serie.equals(SERIE_TOP) && listPlaUp.size() > 0) {
                            serieNext = computeSerieEvolution(serie, 1, false);
                            if (log.isDebugEnabled()) {
                                log.debug("Up - Serie="+serie+" - serieNext="+serieNext+" - list="+StringTools.listToString(listPlaUp));
                            }
                            ts = System.currentTimeMillis();
                            if (!updateSerieForPlayers(listPlaUp, serieNext, true)) {
                                addLogAndReport(listMsgReport, "Failed to update player serie up for serie=" + serie+" - serieNext="+serieNext, null);
                            }
                            addLogAndReport(listMsgReport, "Update player serie=" + serie + " - UP to "+serieNext+" - nb=" + listPlaUp.size() + " - ts=" + (System.currentTimeMillis() - ts), null);
                            // update player serie in cache
                            ts = System.currentTimeMillis();
                            playerCacheMgr.updatePlayerSerieForList(listPlaUp, serieNext);
                            // Send add notif
                            if (reportNotifEnable) {
                                ts = System.currentTimeMillis();
                                for (Long playerID : listPlaUp) {
                                    TourSerieMemPeriodRankingPlayer rankPla = serieRanking.getPlayerRank(playerID);
                                    if (rankPla != null) {
                                        notifMgr.createNotifSerieNewUp(playerID, serieNext, serie, rankPla, serieRanking.getNbPlayerActive(null, null), podiumTemplateParameters, tsEndNextPeriod);
                                    }
                                }
                            }
                        }

                        // Player with last period play = previous => maintain ... send notif
                        if (periodIDPrevious != null && reportNotifEnable) {
                            try {
                                List<TourSeriePlayer> list = mongoTemplate.find(Query.query(Criteria.where("serie").is(serie).andOperator(Criteria.where("lastPeriodPlayed").is(periodIDPrevious))), TourSeriePlayer.class);
                                if (list != null && list.size() > 0) {
                                    List<Long> listPlaNoActivity = new ArrayList<>();
                                    for (TourSeriePlayer tsp : list) {
                                        listPlaNoActivity.add(tsp.getPlayerID());
                                    }

                                    if (log.isDebugEnabled()) {
                                        log.debug("Maintain no activity - Serie="+serie+" - serieNext="+serie+" - list="+StringTools.listToString(listPlaNoActivity));
                                    }

                                    ts = System.currentTimeMillis();
                                    MessageNotifGroup notif = notifMgr.createNotifGroupSerieNewMaintainNoPlay(serie, computeSerieEvolution(serie, -1, false), tsEndNextPeriod);
                                    boolean bAddNotif = notifMgr.setNotifGroupForPlayer(notif, listPlaNoActivity);
                                    addLogAndReport(listMsgReport, "Add notif NewMaintainNoPlay for nbPla=" + listPlaNoActivity.size() + " - result=" + bAddNotif + " - ts=" + (System.currentTimeMillis() - ts), null);
                                }
                            } catch (Exception e) {
                                addLogAndReport(listMsgReport, "Failed to list player with lastPeriodPlayed=" + periodIDPrevious, e);
                            }
                        }

                        // Player with no activity : last period play = period-2 => down and RESERVE
                        if (periodIDBefore2 != null) {
                            // Query for player with last period played=current-2
                            Query q = new Query();
                            q.addCriteria(Criteria.where("serie").is(serie).andOperator(Criteria.where("lastPeriodPlayed").is(periodIDBefore2)));

                            // get playerID list to send notif
                            List<Long> listPlaNoActivity = new ArrayList<>();
                            try {
                                List<TourSeriePlayer> list = mongoTemplate.find(q, TourSeriePlayer.class);
                                if (list != null && list.size() > 0) {
                                    for (TourSeriePlayer tsp : list) {
                                        listPlaNoActivity.add(tsp.getPlayerID());
                                    }
                                }
                            } catch (Exception e) {
                                addLogAndReport(listMsgReport, "Failed to list player no activity on serie=" + serie + " since periodID=" + periodIDBefore2, e);
                            }
                            // update player serie
                            serieNext = computeSerieEvolution(serie, -1, false);
                            if (log.isDebugEnabled()) {
                                log.debug("Down to reserve - Serie="+serie+" - serieNext="+serieNext+" - list="+StringTools.listToString(listPlaNoActivity));
                            }
                            if (!serie.equals(serieNext)) {
                                ts = System.currentTimeMillis();
                                if (!updateSerieForPlayers(listPlaNoActivity, serieNext, false)) {
                                    addLogAndReport(listMsgReport, "Failed to update player serie down (no activity) for serie=" + serie, null);
                                }
                                addLogAndReport(listMsgReport, "Update player serie=" + serie + " - move down to " + serieNext + " by no activity - nb=" + listPlaNoActivity.size() + " - ts=" + (System.currentTimeMillis() - ts), null);

                                // update player serie in cache
                                ts = System.currentTimeMillis();
                                playerCacheMgr.updatePlayerSerieForList(listPlaNoActivity, serieNext);
//                                addLogAndReport(listMsgReport, "Update playerCache serie=" + serie + " - move down to "+serieNext+" by no activity - nb=" + listPlaNoActivity.size() + " - ts=" + (System.currentTimeMillis() - ts), null);
                            } else {
                                addLogAndReport(listMsgReport, "No update serie=serieNext="+serie, null);
                            }
                            // Send notif
                            if (reportNotifEnable) {
                                ts = System.currentTimeMillis();
                                MessageNotifGroup notif = notifMgr.createNotifGroupSerieNewDownNoPlay(serieNext, serie, tsEndNextPeriod);
                                boolean bAddNotif = notifMgr.setNotifGroupForPlayer(notif, listPlaNoActivity);
//                                addLogAndReport(listMsgReport, "Add notif NewDownNoPlay for nbPla=" + listPlaNoActivity.size() + " - result=" + bAddNotif + " - ts=" + (System.currentTimeMillis() - ts), null);
                            }
                        }
                    }
                    else {
                        //****************************************
                        // Send notif for player NC with trend = 0
                        addLogAndReport(listMsgReport, "Nb player NC with trend=0 - nb=" + listPlaNCMaintain.size(), null);
                        if (listPlaNCMaintain.size() > 0) {
                            // Send notif
                            if (reportNotifEnable) {
                                ts = System.currentTimeMillis();
                                MessageNotifGroup notif = notifMgr.createNotifGroupSerieNewMaintainNew(serieRanking.thresholdNbUp, tsEndNextPeriod);
                                boolean bAddNotif = notifMgr.setNotifGroupForPlayer(notif, listPlaNCMaintain);
//                                addLogAndReport(listMsgReport, "Add notif NewMaintainNew for nbPla=" + listPlaNCMaintain.size() + " - result=" + bAddNotif + " - ts=" + (System.currentTimeMillis() - ts), null);
                            }
                        }
                        addLogAndReport(listMsgReport, "Nb player NC with trend > 0 - nb=" + listPlaNCUp.size(), null);
                    }

                } else {
                    addLogAndReport(listMsgReport, "No serie ranking found for serie=" + serie, null);
                }
            } // end loop serie

            //***************************************
            // Integrated player NC with trend > 0
            addLogAndReport(listMsgReport, "Nb player NC with trend>0 - nb=" + listPlaNCUp.size(), null);
            int nbPlayerInSeries = countPlayerInSeriesExcludeNC_TOP_Reserve(false);
            addLogAndReport(listMsgReport, "Nb player in series exclude NC & TOP & Reserve - nb=" + nbPlayerInSeries + " - Nb Final=" + (listPlaNCUp.size() + nbPlayerInSeries), null);
            if (listPlaNCUp.size() > 0) {

                TourSerieMemPeriodRanking serieNCRanking = mapSerieRanking.get(SERIE_NC);
                // get podium for notifs
                Map<String, String> podiumTemplateParameters = new HashMap<>();
                List<Long> podiumPlayers = serieNCRanking.getListPlayerOrderResult();
                for (int i = 0; i < 3; i++) {
                    if (podiumPlayers.size() > i) {
                        PlayerCache playerCache = playerCacheMgr.getPlayerCache(podiumPlayers.get(i));
                        podiumTemplateParameters.put("PLA_PSEUDO" + i, playerCache.getPseudo());
                        podiumTemplateParameters.put("COUNTRY_PLA_PSEUDO" + i, playerCache.countryCode);
                    } else {
                        podiumTemplateParameters.put("PLA_PSEUDO" + i, "Unknown");
                        podiumTemplateParameters.put("COUNTRY_PLA_PSEUDO" + i, "FR");
                    }
                }

                int nbPlayerInSeriesFinal = listPlaNCUp.size()+nbPlayerInSeries;
                int nbPlayerNCUp = listPlaNCUp.size();
                boolean useDistributionOnNewUp = getConfigIntValue("distribution.onNewUp", 0) == 1; // true => percent is set on nb player NC up || false => percent is set on total nb player in serie
                if (useDistributionOnNewUp) {
                    // reverse list player to start with player having smaller score
                    Collections.reverse(listPlaNCUp);
                }
                addLogAndReport(listMsgReport, "useDistributionOnNewUp=" + useDistributionOnNewUp, null);
                for (int idxSerie = 0; idxSerie < SERIE_TAB.length; idxSerie++) {
                    String serie = "";
                    if (useDistributionOnNewUp) {
                        serie = SERIE_TAB[idxSerie];
                    } else {
                        serie = SERIE_TAB[SERIE_TAB.length-1-idxSerie];
                    }
                    // no treatment for serie NC & TOP
                    if (serie.equals(SERIE_NC) || serie.equals(SERIE_TOP)) {continue;}

                    double percentDistribSerie = getPercentDistributionPlayer(serie);
                    if (percentDistribSerie > 0) {
                        int nbPlayerToMove = 0;
                        int nbPlayerSerie = countPlayerInSerieExcludeReserve(serie, false);
                        if (useDistributionOnNewUp) {
                            nbPlayerToMove = ((int)(percentDistribSerie * nbPlayerNCUp)/100)+1; // +1 to finish with empty list
                            addLogAndReport(listMsgReport, "Move player from NC to serie=" + serie + " - percentDistribSerie=" + percentDistribSerie + " - nbPlayerSerie=" + nbPlayerSerie + " - nbToMove=" + nbPlayerToMove, null);
                        } else {
                            int nbPlayerSerieFinal = ((int) (percentDistribSerie * nbPlayerInSeriesFinal) / 100) + 1; // +1 to finish with empty list
                            nbPlayerToMove = nbPlayerSerieFinal - nbPlayerSerie;
                            if (nbPlayerToMove < 0) {
                                nbPlayerToMove = 0;
                            }
                            addLogAndReport(listMsgReport, "Move player from NC to serie=" + serie + " - percentDistribSerie=" + percentDistribSerie + " - nbPlayerSerie=" + nbPlayerSerie + " - nbPlayerSerieFinal=" + nbPlayerSerieFinal + " - nbToMove=" + nbPlayerToMove, null);
                        }

                        if (nbPlayerToMove > 0) {
                            if (nbPlayerToMove > listPlaNCUp.size()) {
                                nbPlayerToMove = listPlaNCUp.size();
                            }
                            List<Long> listPlaMove = listPlaNCUp.subList(0, nbPlayerToMove);
                            if (log.isDebugEnabled()) {
                                log.debug("NC to serie="+serie+" - list="+StringTools.listToString(listPlaMove));
                            }
                            long ts = System.currentTimeMillis();
                            if (!updateSerieForPlayers(listPlaMove, serie, true)) {
                                addLogAndReport(listMsgReport, "Failed to set player serie from NC to serie=" + serie + " - nbPlayer=" + listPlaMove.size(), null);
                            }
                            addLogAndReport(listMsgReport, "Update player serie=" + serie + " + move from NC - nb=" + listPlaMove.size() + " - ts=" + (System.currentTimeMillis() - ts), null);
                            // update player serie in cache
                            ts = System.currentTimeMillis();
                            playerCacheMgr.updatePlayerSerieForList(listPlaMove, serie);
                            // Send notif
                            if (reportNotifEnable) {
                                ts = System.currentTimeMillis();
                                for (Long playerID : listPlaMove) {
                                    TourSerieMemPeriodRankingPlayer rankPla = serieNCRanking.getPlayerRank(playerID);
                                    if (rankPla != null) {
                                        notifMgr.createNotifSerieNewUpNew(playerID, serie, SERIE_NC, rankPla, serieNCRanking.getNbPlayerActive(null, null), podiumTemplateParameters, tsEndNextPeriod);
                                    }
                                }
                            }

                            listPlaNCUp.removeAll(listPlaMove);
                        }
                    } else {
                        addLogAndReport(listMsgReport, "Percent distribution player not valid for serie=" + serie + " - percentDistribSerie=" + percentDistribSerie, null);
                    }

                    if (listPlaNCUp.size() == 0) {
                        // no more player to move in serie
                        break;
                    }
                }

                if (listPlaNCUp.size() > 0) {
                    addLogAndReport(listMsgReport, "List player up not empty ... => move to S11 - nbPlayer="+listPlaNCUp.size(), null);
                    long ts = System.currentTimeMillis();
                    if (!updateSerieForPlayers(listPlaNCUp, SERIE_11, true)) {
                        addLogAndReport(listMsgReport, "Failed to set player serie from NC to serie="+SERIE_11+" - nbPlayer="+listPlaNCUp.size(), null);
                    }
                    addLogAndReport(listMsgReport, "Update player serie=" + SERIE_11 + " + move from NC - nb=" + listPlaNCUp.size() + " - ts=" + (System.currentTimeMillis() - ts), null);
                    if (log.isDebugEnabled()) {
                        log.debug("NC to serie="+SERIE_11+" - list="+StringTools.listToString(listPlaNCUp));
                    }
                    // update player serie in cache
                    ts = System.currentTimeMillis();
                    playerCacheMgr.updatePlayerSerieForList(listPlaNCUp, SERIE_11);
                    addLogAndReport(listMsgReport, "Update playerCache serie=" + SERIE_11 + " + move from NC - nb=" + listPlaNCUp.size() + " - ts=" + (System.currentTimeMillis() - ts), null);
                    // send notif
                    if (reportNotifEnable) {
                        ts = System.currentTimeMillis();
                        MessageNotifGroup notif = notifMgr.createNotifGroupSerieNewUpNew(SERIE_11, SERIE_NC, tsEndNextPeriod);
                        boolean bAddNotif = notifMgr.setNotifGroupForPlayer(notif, listPlaNCUp);
                        addLogAndReport(listMsgReport, "Add notif NewUp for nbPla=" + listPlaNCUp.size() + " - result=" + bAddNotif + " - ts=" + (System.currentTimeMillis() - ts), null);
                    }
                }
            }

            //*********************************
            // set period finish & update in DB
            currentPeriod.setFinished(true);
            mongoTemplate.save(currentPeriod);

            //*********************************
            // clear data in memory
            for (TourSerieMemPeriodRanking sr : mapSerieRanking.values()) {
                sr.clearData();
            }
            memoryMgr.clearData();
            addLogAndReport(listMsgReport, "Finish period ending .... period="+currentPeriod, null);
            String pathReport = getConfigStringValue("reportChangePeriodPath", null);
            if (pathReport != null) {
                try {
                    File fPath = new File(pathReport);
                    fPath.mkdirs();
                    pathReport = FilenameUtils.concat(pathReport, "reportPeriod-" + currentPeriod.getPeriodID());
                    FileUtils.writeLines(new File(pathReport), listMsgReport);
                } catch (Exception e) {
                    log.error("Failed to write report change period - pathReport=" + pathReport, e);
                }
            } else {
                log.error("No reportChangePeriodPath found in config file ...");
            }
        }
    }

    /**
     * Return the player distribution percent for this serie. Used to include player from NC serie.
     * @param serie
     * @return
     */
    public double getPercentDistributionPlayer(String serie) {
        double value = -1;
        if (serie.length() == 3 && serie.startsWith("S")) {
            // value defined in configuration
            value = getConfigDoubleValue("distribution.serie"+serie, -1);
            // not found => get from static array
            if (value == -1) {
                try {
                    String temp = serie.substring(1);
                    int serieNumber = Integer.parseInt(temp);
                    if (serieNumber > 0 && serieNumber <= percentPlayerDistribution.length) {
                        value = percentPlayerDistribution[serieNumber - 1];
                    }
                } catch (Exception e) {
                    log.error("Failed to retrieve percent distribution for serie=" + serie);
                }
            }
        }
        return value;
    }

    /**
     * Do the job to change period (check current period date)
     * @return
     */
    public boolean changePeriod() {
        boolean result = false;
        if (periodChangeProcessing) {
            log.error("Period change already in process ...");
        } else {
            if ((currentPeriod != null) && (getConfigBooleanValue("devMode") || currentPeriod.getTsDateEnd() < System.currentTimeMillis())) {
                // set maintenance => close all session
                PresenceMgr presenceMgr = ContextManager.getPresenceMgr();
                presenceMgr.setServiceMaintenance(true);

                // block access to serie
                setPeriodChangeProcessing(true);

                try {
                    log.error("Wait 10s ...");
                    Thread.sleep(10*1000);

                    presenceMgr.closeAllSession(Constantes.EVENT_VALUE_DISCONNECT_MAINTENANCE);
                    log.error("All session are closed ... begin work !");

                    // maintenance during period change ?
                    if (getConfigIntValue("maintenanceOnPeriodChange", 1) == 0) {
                        presenceMgr.setServiceMaintenance(false);
                    }

                    long ts = System.currentTimeMillis();
                    // finish all tournaments
                    finishCurrentPeriod();
                    log.error("Time to finish current period = " + (System.currentTimeMillis() - ts));

                    // init the new period
                    String periodID = buildSeriePeriodID(System.currentTimeMillis());
                    TourSeriePeriod existingPeriod = getPeriodForID(periodID);
                    if (existingPeriod ==  null) {
                        currentPeriod = createNewPeriod(periodID);
                    } else {
                        //  if devMode, add indice to value _XXX
                        if (getConfigBooleanValue("devMode")) {
                            String existingPeriodID = existingPeriod.getPeriodID();
                            if (existingPeriodID.contains(PERIOD_ID_INDICE_SEPARATOR)) {
                                int indice = Integer.parseInt(existingPeriodID.substring(existingPeriodID.indexOf(PERIOD_ID_INDICE_SEPARATOR)+1));
                                periodID += PERIOD_ID_INDICE_SEPARATOR+String.format("%03d", indice+1);
                            } else {
                                periodID += PERIOD_ID_INDICE_SEPARATOR+"001";
                            }
                            currentPeriod = createNewPeriod(periodID);
                        } else {
                            throw new Exception("A period already existing with this ID="+periodID);
                        }
                    }
                    if (currentPeriod == null) {
                        throw new Exception("Failed to create new period with ID="+periodID);
                    }
                    initCurrentPeriod();

                    // change period on serie top challenge
                    ContextManager.getSerieTopChallengeMgr().startThreadChangePeriod();
                    // change period on serie easy challenge
                    ContextManager.getSerieEasyChallengeMgr().startThreadChangePeriod();

                    result = true;
                } catch (Exception e) {
                    log.error("Exception to change period ... ", e);
                }
                // open access to serie
                setPeriodChangeProcessing(false);
                // change period is finished ... stop maintenance
                if (presenceMgr.isServiceMaintenance()) {
                    presenceMgr.setServiceMaintenance(false);
                }
            } else {
                log.error("Current period is always valid for current time : currentPeriod="+currentPeriod);
            }
        }
        return result;
    }

    /**
     * Send reminder notif
     *
     * @return nb notif which are sent to players
     */
    public int processReminder(long date) {
        int nbReminder = 0;

        String remainingUnit = getConfigStringValue("reminder.remainingTimeUnit", TimeUnit.DAYS.toString());
        long tsRemaining = ((long) getConfigIntValue("reminder.remainingTime", 2) * Constantes.getTimestampByTimeUnit(remainingUnit));
        long tsRemainingLow = tsRemaining - (getReminderPeriodMinutes()/2 * Constantes.TIMESTAMP_MINUTE);
        long tsRemainingHigh = tsRemaining + (getReminderPeriodMinutes()/2 * Constantes.TIMESTAMP_MINUTE);

        if (currentPeriod != null && (date + tsRemainingLow) <= currentPeriod.getTsDateEnd() && (date + tsRemainingHigh) > currentPeriod.getTsDateEnd()) {
            for (String serie : SERIE_TAB) {
                // No notif reminder for NEW
                if (!serie.equals(SERIE_NC)) {
                    List<Long> listPlaUp = new ArrayList<>();
                    List<Long> listPlaDown = new ArrayList<>();
                    List<Long> listPlaMaintain = new ArrayList<>();

                    TourSerieMemPeriodRanking serieRanking = mapSerieRanking.get(serie);
                    List<Long> listPlaID = serieRanking.getListPlayerOrderTrendAndResult();
                    for (Long l : listPlaID) {
                        TourSerieMemPeriodRankingPlayer rankPla = serieRanking.getPlayerRank(l);
                        if (rankPla == null) {
                            continue;
                        }
                        if (rankPla.trend > 0) {
                            listPlaUp.add(rankPla.playerID);
                        } else if (rankPla.trend < 0) {
                            listPlaDown.add(rankPla.playerID);
                        } else if (rankPla.trend == 0) {
                            listPlaMaintain.add(rankPla.playerID);
                        }
                    }

                    String serieNext = serie;

                    // Move DOWN (not for serie 11)
                    if (!serie.equals(SERIE_11) && listPlaDown.size() > 0) {
                        serieNext = computeSerieEvolution(serie, -1, false);
                        MessageNotifGroup notif = notifMgr.createNotifGroupSerieReminderDown(serie, serieNext, currentPeriod.getTsDateEnd());
                        notifMgr.setNotifGroupForPlayer(notif, listPlaDown);
                        nbReminder += listPlaDown.size();
                    }

                    // Maintain
                    if (listPlaMaintain.size() > 0) {
                        if (serie.equals(SERIE_TOP)) {
                            MessageNotifGroup notif = notifMgr.createNotifGroupSerieReminderMaintainTop(serie, currentPeriod.getTsDateEnd());
                            notifMgr.setNotifGroupForPlayer(notif, listPlaMaintain);
                            nbReminder += listPlaMaintain.size();
                        } else {
                            serieNext = computeSerieEvolution(serie, 1, false);
                            MessageNotifGroup notif = notifMgr.createNotifGroupSerieReminderMaintain(serie, serieNext, currentPeriod.getTsDateEnd());
                            notifMgr.setNotifGroupForPlayer(notif, listPlaMaintain);
                            nbReminder += listPlaMaintain.size();
                        }
                    }

                    // Move UP (not for serie TOP)
                    if (!serie.equals(SERIE_TOP) && listPlaUp.size() > 0) {
                        serieNext = computeSerieEvolution(serie, 1, false);
                        MessageNotifGroup notif = notifMgr.createNotifGroupSerieReminderUp(serie, serieNext, currentPeriod.getTsDateEnd());
                        notifMgr.setNotifGroupForPlayer(notif, listPlaUp);
                        nbReminder += listPlaUp.size();
                    }

                    // Player with no activity : last period play = period-2 => down and RESERVE
                    if (periodIDBefore2 != null) {
                        // Query for player with last period played=current-2
                        Query q = new Query();
                        q.addCriteria(Criteria.where("serie").is(serie).andOperator(Criteria.where("lastPeriodPlayed").is(periodIDBefore2)));

                        // get playerID list to send notif
                        List<Long> listPlaNoActivity = new ArrayList<>();
                        try {
                            List<TourSeriePlayer> list = mongoTemplate.find(q, TourSeriePlayer.class);
                            if (list != null && list.size() > 0) {
                                for (TourSeriePlayer tsp : list) {
                                    listPlaNoActivity.add(tsp.getPlayerID());
                                }

                                MessageNotifGroup notif = notifMgr.createNotifGroupSerieReminder(serie, currentPeriod.getTsDateEnd());
                                notifMgr.setNotifGroupForPlayer(notif, listPlaNoActivity);
                                nbReminder += listPlaNoActivity.size();
                            }
                        } catch (Exception e) {
                            log.error("Failed to list player no activity on serie=" + serie + " since periodID=" + periodIDBefore2, e);
                        }
                    }
                }
            }
        }

        return nbReminder;
    }

    /**
     * Return the SeriePlayer for this player
     * @param playerID
     * @return null if no existing for this player
     */
    public TourSeriePlayer getTourSeriePlayer(long playerID) {
        if (isEnable()) {
            try {
                Query q = new Query();
                q.addCriteria(Criteria.where("playerID").is(playerID));
                return mongoTemplate.findOne(q, TourSeriePlayer.class);
            } catch (Exception e) {
                log.error("Failed to get player serie for playerID=" + playerID, e);
            }
        }
        return null;
    }

    /**
     * List serie player for a serie with lastPeriodPlayed is not current and exclude reserve => only period - 1 and period - 2. List of ordered by lastPeriodPlayed desc.
     * @param serie
     * @param listPlaFilter
     * @param countryCode
     * @param offset
     * @param nbMax
     * @return
     */
    public List<TourSeriePlayer> listTourSeriePlayerExcludeCurrentPeriodAndReserve(String serie, List<Long> listPlaFilter, String countryCode, int offset, int nbMax) {
        if (periodIDPrevious != null) {
            try {
                List<Criteria> listCriteria = new ArrayList<>();
                listCriteria.add(Criteria.where("serie").is(serie));
                if (periodIDBefore2 == null) {
                    // serie & lastPeriodPlayed = previous
                    listCriteria.add(Criteria.where("lastPeriodPlayed").is(periodIDPrevious));
                } else {
                    // serie & (lastPeriodPlayed = previous or lastPeriodPlayed = before2)
                    listCriteria.add(new Criteria().orOperator(Criteria.where("lastPeriodPlayed").is(periodIDPrevious),
                            Criteria.where("lastPeriodPlayed").is(periodIDBefore2)));
                }
                if (listPlaFilter != null && listPlaFilter.size() > 0) {
                    listCriteria.add(Criteria.where("playerID").in(listPlaFilter));
                }
                if (countryCode != null && countryCode.length() > 0) {
                    listCriteria.add(Criteria.where("countryCode").is(countryCode));
                }
                Query q = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
                q.with(new Sort(Sort.Direction.DESC, "lastPeriodPlayed"));
                if (offset > 0) {
                    q.skip(offset);
                }
                if (nbMax > 0) {
                    q.limit(nbMax);
                }
                return mongoTemplate.find(q, TourSeriePlayer.class);
            } catch (Exception e) {
                log.error("Failed to list player serie - serie=" + serie + " - offset=" + offset + " - nbMax=" + nbMax, e);
            }
        }
        return null;
    }

    /**
     * List seriePlayer for player with lastPeriodPlayed=periodSelected
     * @param serie
     * @param periodSelected value for lastPeriodPlayed
     * @return
     */
    public List<TourSeriePlayer> listTourSeriePlayerWithLastPeriodPlayed(String serie, String periodSelected, int offset, int nbMax) {
        if (nbMax > 0) {
            try {
                Query q = new Query();
                q.addCriteria(Criteria.where("serie").is(serie).andOperator(Criteria.where("lastPeriodPlayed").is(periodSelected)));
                if (offset >= 0) {
                    q.skip(offset);
                }
                if (nbMax >= 0) {
                    q.limit(nbMax);
                }
                return mongoTemplate.find(q, TourSeriePlayer.class);
            } catch (Exception e) {
                log.error("Failed to list all seriePlayer active for serie=" + serie, e);
            }
        }
        return null;
    }

    /**
     * List seriePlayer for serie
     * @param serie
     * @return
     */
    public List<TourSeriePlayer> listTourSeriePlayerForSerie(String serie, int offset, int nbMax, String countryCode) {
        if (nbMax > 0) {
            try {
                Query q = new Query();
                Criteria c = Criteria.where("serie").is(serie);
                if (countryCode != null && !countryCode.isEmpty()) {
                    c.andOperator(Criteria.where("countryCode").is(countryCode));
                }
                q.addCriteria(c);
                if (offset >= 0) {
                    q.skip(offset);
                }
                if (nbMax >= 0) {
                    q.limit(nbMax);
                }
                return mongoTemplate.find(q, TourSeriePlayer.class);
            } catch (Exception e) {
                log.error("Failed to list all seriePlayer for serie=" + serie, e);
            }
        }
        return null;
    }

    /**
     *
     * @param serie
     * @param periodSelected
     * @return
     */
    public int countTourSeriePlayerWithLastPeriodPlayed(String serie, String periodSelected) {
        return (int)mongoTemplate.count(Query.query(Criteria.where("serie").is(serie).andOperator(Criteria.where("lastPeriodPlayed").is(periodSelected))), TourSeriePlayer.class);
    }

    /**
     * Retieve the seriePlayer for this player. If not found create a new one and return it.
     * @param playerID
     * @return
     */
    public TourSeriePlayer getOrCreatePlayerSerie(long playerID) {
        if (isEnable()) {
            synchronized (lockCreateSeriePlayer.getLock("" + playerID)) {
                try {
                    TourSeriePlayer sp = getTourSeriePlayer(playerID);
                    if (sp == null) {
                        sp = new TourSeriePlayer();
                        sp.setPlayerID(playerID);
                        sp.setSerie(SERIE_NC);
                        PlayerCache pc = playerCacheMgr.getPlayerCache(playerID);
                        if (pc != null) {
                            sp.setCountryCode(pc.countryCode);
                        }
                        mongoTemplate.save(sp);
                    }
                    return sp;
                } catch (Exception e) {
                    log.error("Failed to get or save SeriePlayer for playerID=" + playerID, e);
                }
            }
        }
        return null;
    }

    /**
     * Count player in serie except NC and TOP
     * @return
     */
    public int countPlayerInSeriesExcludeNC_TOP() {
        return (int)mongoTemplate.count(
                Query.query(new Criteria().andOperator(
                                Criteria.where("serie").ne(SERIE_NC),
                                Criteria.where("serie").ne(SERIE_TOP))
                ), TourSeriePlayer.class);
    }

    /**
     * Count player in serie except NC, TOP and RESERVE
     * @param includePeriodBefore2
     * @return
     */
    public int countPlayerInSeriesExcludeNC_TOP_Reserve(boolean includePeriodBefore2) {
        List<String> listPeriod = new ArrayList<>();
        listPeriod.add(currentPeriod.getPeriodID());
        if (periodIDPrevious != null) {
            listPeriod.add(periodIDPrevious);
        }
        if (includePeriodBefore2 && periodIDBefore2 != null) {
            listPeriod.add(periodIDBefore2);
        }
        return (int)mongoTemplate.count(
                Query.query(new Criteria().andOperator(
                                Criteria.where("serie").ne(SERIE_NC),
                                Criteria.where("serie").ne(SERIE_TOP),
                                Criteria.where("lastPeriodPlayed").in(listPeriod))
                ), TourSeriePlayer.class);
    }

    /**
     * Count player in serie
     * @return
     */
    public int countPlayerInSerie(String serie) {
        return (int)mongoTemplate.count(Query.query(Criteria.where("serie").is(serie)), TourSeriePlayer.class);
    }

    /**
     * Count player in serie excluding player in reserve (lastPeriodPlayed = current, period-1 or period-2)
     * @param serie
     * @param includePeriodBefore2
     * @return
     */
    public int countPlayerInSerieExcludeReserve(String serie, boolean includePeriodBefore2) {
        List<String> listPeriod = new ArrayList<>();
        listPeriod.add(currentPeriod.getPeriodID());
        if (periodIDPrevious != null) {
            listPeriod.add(periodIDPrevious);
        }
        if (includePeriodBefore2 && periodIDBefore2 != null) {
            listPeriod.add(periodIDBefore2);
        }
        return (int)mongoTemplate.count(
                Query.query(Criteria.where("serie").is(serie).andOperator(Criteria.where("lastPeriodPlayed").in(listPeriod))),
                TourSeriePlayer.class);
    }

    /**
     * Count player in serie and country excluding player in reserve (lastPeriodPlayed = current, period-1 or period-2)
     * @param serie
     * @param countryCode
     * @param includePeriodBefore2
     * @return
     */
    public int countPlayerInSerieExcludeReserve(String serie, String countryCode, List<Long> listPlayerID, boolean includePeriodBefore2) {
        List<String> listPeriod = new ArrayList<>();
        listPeriod.add(currentPeriod.getPeriodID());
        if (periodIDPrevious != null) {
            listPeriod.add(periodIDPrevious);
        }
        if (includePeriodBefore2 && periodIDBefore2 != null) {
            listPeriod.add(periodIDBefore2);
        }
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(Criteria.where("serie").is(serie));
        listCriteria.add(Criteria.where("lastPeriodPlayed").in(listPeriod));
        if (countryCode != null && countryCode.length() > 0) {
            listCriteria.add(Criteria.where("countryCode").is(countryCode));
        }
        if (listPlayerID != null && listPlayerID.size() > 0) {
            listCriteria.add(Criteria.where("playerID").in(listPlayerID));
        }
        Query q = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
        return (int)mongoTemplate.count(q, TourSeriePlayer.class);
    }

    /**
     * Update player serie data : lastPeriodPlayed and increment nbTournamentPlayed with value incrementNbTournamentPlayed
     * @param playerID
     * @param incrementNbTournamentPlayed
     */
    public void updateSeriePlayerTournamentPlayed(long playerID, int incrementNbTournamentPlayed) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("playerID").is(playerID)),
                new Update().set("lastPeriodPlayed", currentPeriod.getPeriodID()).inc("nbTournamentPlayed", incrementNbTournamentPlayed),
                TourSeriePlayer.class);
    }

    /**
     * Update serie for payers in list and set lastPeriodPlayed value to current period
     * @param listPlayerID
     * @param newSerie
     * @return
     */
    public boolean updateSerieForPlayers(List<Long> listPlayerID, String newSerie, boolean updateLastPeriodPlayed) {
        if (listPlayerID != null && listPlayerID.size() > 0) {
            try {
                Query q = new Query();
                q.addCriteria(Criteria.where("playerID").in(listPlayerID));
                Update u = new Update();
                u.set("serie", newSerie);
                if (updateLastPeriodPlayed) {
                    u.set("lastPeriodPlayed", currentPeriod.getPeriodID());
                }
                mongoTemplate.updateMulti(q, u, TourSeriePlayer.class);
                return true;
            } catch (Exception e) {
                log.error("Failed to update serie for players - listPlayerID size=" + listPlayerID.size() + " - newSerie=" + newSerie + " - players=" + StringTools.listToString(listPlayerID), e);
                return false;
            }
        }
        return true;
    }

    public ResultListTournamentArchive listTournamentArchive(long playerID, String serie, int offset, int nbMax) throws FBWSException {
        TourSerieMemPeriodRanking ranking = getSerieRanking(serie);
        if (ranking == null) {
            log.error("No ranking found for serie="+serie);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        ResultListTournamentArchive result = new ResultListTournamentArchive();
        result.archives = new ArrayList<>();
        Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (getConfigIntValue("resultArchiveNbDayBefore", 30)));
        long dateRef = dateBefore.getTimeInMillis();
        result.nbTotal = (int)mongoTemplate.count(Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef))), TourSerieTournamentPlayer.class);
        TourSerieMemPeriodRankingPlayer rankingPlayer = ranking.getPlayerRank(playerID);
        int nbTourInProgress = 0;
        int offsetBDD = 0;
        TourSerieMemTour memTourInProgress = memoryMgr.getTournamentInProgressForPlayer(serie, playerID);
        if (memTourInProgress != null) {
            nbTourInProgress++;
        }
        if (rankingPlayer == null) {
            if (memTourInProgress != null) {
                result.nbTotal++;
                if (offset == 0) {
                    WSTournamentArchive wst = toWSTournamentArchive(memTourInProgress, playerID);
                    if (wst != null) {
                        result.archives.add(wst);
                    }
                }
            }
        }
        else if (rankingPlayer != null) {
            nbTourInProgress += rankingPlayer.getNbTournamentPlayed();
            result.nbTotal += nbTourInProgress;
            if (offset > nbTourInProgress) {
                // not used data from memory ...
                offsetBDD = offset - nbTourInProgress;
            }
            else if (rankingPlayer.listData.size() > 0 || memTourInProgress != null) {
                // use data from memory
                int idx = rankingPlayer.listData.size() - 1 + offset;
                boolean memTourInProgressDone = false;
                while (true) {
                    if (result.archives.size() >= nbMax) {
                        break;
                    }
                    if (!memTourInProgressDone && memTourInProgress != null) {
                        memTourInProgressDone = true;
                        WSTournamentArchive wst = toWSTournamentArchive(memTourInProgress, playerID);
                        if (wst != null) {
                            result.archives.add(wst);
                        }
                    }
                    else if (idx >= 0 && idx < rankingPlayer.listData.size()) {
                        TourSerieMemTour mt = memoryMgr.getMemTour(serie, rankingPlayer.listData.get(idx).tourID);
                        if (mt != null) {
                            WSTournamentArchive wst = toWSTournamentArchive(mt, playerID);
                            if (wst != null) {
                                result.archives.add(wst);
                            }
                        }
                        idx--;
                    } else {
                        break;
                    }
                }
            }
        }
        // if list not full, use data from BDD
        if (result.archives.size() < nbMax) {
            if (offsetBDD < 0) {
                offsetBDD = 0;
            }
            List<TourSerieTournamentPlayer> listTP = listTournamentPlayerAfterDate(playerID, dateRef, offsetBDD, nbMax - result.archives.size());
            if (listTP != null) {
                for (TourSerieTournamentPlayer tp :listTP) {
                    if (result.archives.size() >= nbMax) { break;}
                    WSTournamentArchive wst = toWSTournamentArchive(tp);
                    if (wst != null) {
                        result.archives.add(wst);
                    }
                }
            }
        }
        return result;
    }

    public WSTournamentArchive toWSTournamentArchive(TourSerieMemTour memTour, long playerID) {
        if (memTour != null) {
            TourSerieMemTourPlayer memTourPlayer = memTour.getRankingPlayer(playerID);
            if (memTourPlayer != null) {
                WSTournamentArchive ws = new WSTournamentArchive();
                ws.date = memTourPlayer.dateStartPlay;
                ws.nbPlayers = memTour.getNbPlayersFinish();
                if (memTourPlayer.getNbDealsPlayed() == memTour.getNbDeals()) {
                    ws.rank = memTourPlayer.rankingFinished;
                    ws.finished = true;
                } else {
                    ws.rank = -1;
                    ws.finished = false;
                }
                ws.result = memTourPlayer.result;
                ws.resultType = Constantes.TOURNAMENT_RESULT_PAIRE;
                ws.tournamentID = memTour.tourID;
                ws.periodID = TourSerieMgr.transformPeriodID2TS(memTour.periodID, true)+";"+TourSerieMgr.transformPeriodID2TS(memTour.periodID, false);
                ws.name = memTour.serie;
                ws.countDeal = memTour.getNbDeals();
                ws.listPlayedDeals = new ArrayList<>(memTourPlayer.dealsPlayed);
                Collections.sort(ws.listPlayedDeals);
                return ws;
            }
        }
        return null;
    }

    public WSTournamentArchive toWSTournamentArchive(TourSerieTournamentPlayer e) {
        if (e != null) {
            WSTournamentArchive ws = new WSTournamentArchive();
            ws.date = e.getStartDate();
            ws.nbPlayers = e.getTournament().getNbPlayers();
            ws.rank = e.getRank();
            ws.result = e.getResult();
            ws.resultType = e.getTournament().getResultType();
            ws.tournamentID = e.getTournament().getIDStr();
            ws.periodID = e.getTournament().getTsDateStart()+";"+e.getTournament().getTsDateEnd();
            ws.name = e.getTournament().getSerie();
            ws.finished = true;
            ws.countDeal = e.getTournament().getNbDeals();
            ws.listPlayedDeals = new ArrayList<>();
            for (Deal d : e.getTournament().getDeals()) {
                ws.listPlayedDeals.add(d.getDealID(e.getTournament().getIDStr()));
            }
            Collections.sort(ws.listPlayedDeals);
            return ws;
        }
        return null;
    }

    /**
     * Build result archive with only tournament played (all deals) by player
     * @param playerCache
     * @param serie
     * @param offset
     * @param count
     * @return
     * @throws FBWSException
     */
    public WSResultArchive resultArchiveTournament(PlayerCache playerCache, String serie, int offset, int count) throws FBWSException {
        WSResultArchive resArc = new WSResultArchive();
        resArc.setOffset(offset);
        List<WSTournament> listWSTour = new ArrayList<WSTournament>();
        Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (getConfigIntValue("resultArchiveNbDayBefore", 30)));
        long dateRef = dateBefore.getTimeInMillis();

        if (playerCache != null) {
            int nbTotal = (int)mongoTemplate.count(Query.query(Criteria.where("playerID").is(playerCache.ID).andOperator(Criteria.where("lastDate").gt(dateRef))), TourSerieTournamentPlayer.class);
            TourSerieMemPeriodRanking ranking = getSerieRanking(serie);
            if (ranking == null) {
                log.error("No ranking found for serie="+serie);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            TourSerieMemPeriodRankingPlayer rankingPlayer = ranking.getPlayerRank(playerCache.ID);
            int nbTourInProgressPlayed = 0;
            int offsetBDD = 0;
            if (rankingPlayer != null) {
                nbTourInProgressPlayed = rankingPlayer.getNbTournamentPlayed();
                nbTotal += nbTourInProgressPlayed;
                if (offset > nbTourInProgressPlayed) {
                    // not used data from memory ...
                    offsetBDD = offset - nbTourInProgressPlayed;
                }
                else if (nbTourInProgressPlayed > 0){
                    // use data from memory
                    int idx = nbTourInProgressPlayed - 1 - offset;
                    while (true) {
                        if (listWSTour.size() == count) {
                            break;
                        }
                        if (idx >= 0 && idx < rankingPlayer.listData.size()) {
                            TourSerieMemPeriodRankingPlayerData data = rankingPlayer.listData.get(idx);
                            TourSerieMemTour mt = memoryMgr.getMemTour(serie, data.tourID);
                            if (mt != null) {
                                WSTournament wst = mt.toWSTournament();
                                wst.resultPlayer = new WSResultTournamentPlayer();
                                wst.resultPlayer.setPlayerID(playerCache.ID);
                                wst.resultPlayer.setCountryCode(playerCache.countryCode);
                                wst.resultPlayer.setAvatarPresent(playerCache.avatarPresent);
                                wst.resultPlayer.setResult(data.result);
                                wst.resultPlayer.setDateLastPlay(data.dateResult);
                                wst.resultPlayer.setNbDealPlayed(mt.getNbDeals());
//                                wst.resultPlayer.setNbTotalPlayer(mt.getNbPlayers());
                                wst.resultPlayer.setNbTotalPlayer(mt.getNbPlayersForRanking());
                                wst.resultPlayer.setPlayerPseudo(playerCache.getPseudo());
                                wst.resultPlayer.setConnected(presenceMgr.isSessionForPlayerID(playerCache.ID));
                                wst.resultPlayer.setPlayerSerie(serie);
                                wst.resultPlayer.setRank(data.rank);
                                listWSTour.add(wst);
                            }
                            idx--;
                        } else {
                            break;
                        }
                    }
                }
            }

            // if list not full, use data from BDD
            if (listWSTour.size() < count) {
                if (offsetBDD < 0) {
                    offsetBDD = 0;
                }
                List<TourSerieTournamentPlayer> listTP = listTournamentPlayerAfterDate(playerCache.ID, dateRef, offsetBDD, count - listWSTour.size());
                if (listTP != null) {
                    for (TourSerieTournamentPlayer tp :listTP) {
                        if (listWSTour.size() == count) { break;}
                        WSTournament wst = tp.getTournament().toWS();
                        wst.resultPlayer = tp.toWSResultTournamentPlayer(playerCache, playerCache.ID);
                        listWSTour.add(wst);
                    }
                }
            }

            resArc.setTotalSize(nbTotal);
            resArc.setListTournament(listWSTour);
        } else {
            log.error("Parameter is null !");
        }
        return resArc;
    }

    /**
     * List tournamentPlayer for a player and after a date. Order by startDate desc
     * @param playerID
     * @param dateRef
     * @param offset
     * @param nbMax
     * @return
     */
    @Override
    public List<TourSerieTournamentPlayer> listTournamentPlayerAfterDate(long playerID, long dateRef, int offset, int nbMax) {
        return mongoTemplate.find(
                Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef))).
                        skip(offset).
                        limit(nbMax).
                        with(new Sort(Sort.Direction.DESC, "startDate")),
                TourSerieTournamentPlayer.class);
    }

    /**
     * Return the number of tournament to get the bonus
     * @return
     */
    public int getBonusNbTour() {
        return getConfigIntValue("bonusNbTour", 10);
    }

    /**
     * Return the number of tournament to remove for the bonus
     * @return
     */
    public int getBonusRemove() {
        return getConfigIntValue("bonusRemove", 2);
    }

    /**
     * Return period result for player on a period
     * @param periodID
     * @param playerID
     * @return
     */
    public TourSeriePeriodResult getPeriodResultForPlayer(String periodID, long playerID) {
        return mongoTemplate.findOne(Query.query(
                Criteria.where("periodID").is(periodID).
                        andOperator(Criteria.where("playerID").is(playerID))),
                TourSeriePeriodResult.class);
    }

    /**
     * List period result for a periodID and serie order by rank asc
     * @param periodID
     * @param serie
     * @param offset
     * @param nbMax
     * @return
     */
    public List<TourSeriePeriodResult> listPeriodResult(String periodID, String serie, int offset, int nbMax) {
        if (getConfigIntValue("fixBugRankingPreviousPeriod", 1) == 1) {
            return mongoTemplate.find(Query.query(
                            Criteria.where("periodID").is(periodID).andOperator(Criteria.where("serie").is(serie))).skip(offset).limit(nbMax).with(new Sort(Sort.Direction.ASC, "rank")),
                    TourSeriePeriodResult.class);
        } else {
            return mongoTemplate.find(Query.query(
                        Criteria.where("periodID").is(periodID).
                                andOperator(Criteria.where("serie").is(serie))).skip(offset).limit(nbMax).with(new Sort(Sort.Direction.DESC, "evolution").and(new Sort(Sort.Direction.DESC, "result"))),
                TourSeriePeriodResult.class);
        }
    }

    /**
     * Return nb player on period and serie
     * @param periodID
     * @param serie
     * @return
     */
    public int countPlayerOnPeriodResult(String periodID, String serie) {
        return (int)mongoTemplate.count(Query.query(
                        Criteria.where("periodID").is(periodID).
                                andOperator(Criteria.where("serie").is(serie))),
                TourSeriePeriodResult.class);
    }

    /**
     * Return nb period result for player
     * @param playerID
     * @return
     */
    public int countPeriodResultForPlayer(long playerID) {
        return (int)mongoTemplate.count(Query.query(Criteria.where("playerID").is(playerID)), TourSeriePeriodResult.class);
    }

    /**
     * Return the serie on previous period. Null if no result for previous period found.
     * @param playerID
     * @return
     */
    public String getSerieOnPreviousPeriodForPlayer(long playerID) {
        if (periodIDPrevious != null) {
            TourSeriePeriodResult pr = getPeriodResultForPlayer(periodIDPrevious, playerID);
            if (pr != null) {
                return pr.getSerie();
            }
        }
        return null;
    }

    /**
     * Return list of period result for player order by periodID desc
     * @param playerID
     * @param offset
     * @param nbMax
     * @return
     */
    public List<TourSeriePeriodResult> listPeriodResultForPlayer(long playerID, int offset, int nbMax) {
        Query q = Query.query(Criteria.where("playerID").is(playerID));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        q.with(new Sort(Sort.Direction.DESC, "periodID"));
        return mongoTemplate.find(q, TourSeriePeriodResult.class);
    }

    /**
     * List period order by periodID ASC (only period finished)
     * @return
     */
    public List<TourSeriePeriod> listPeriod() {
        Query q = new Query(Criteria.where("finished").is(true)).with(new Sort(Sort.Direction.ASC, "periodID"));
        return mongoTemplate.find(q, TourSeriePeriod.class);
    }

    public String getWSSerieHistoricForPlayer(long playerID) {
        // retrieve period with activity for player
        Query q = Query.query(Criteria.where("playerID").is(playerID));
        q.with(new Sort(Sort.Direction.ASC, "periodID"));
        List<TourSeriePeriodResult> listPeriodResult = mongoTemplate.find(q, TourSeriePeriodResult.class);
        String historic = "";
        if (listPeriodResult != null && listPeriodResult.size() > 0) {
            // list all period
            List<TourSeriePeriod> listPeriod = listPeriod();
            // for each period, set the serie associated
            List<String> listSeriePlayer = new ArrayList<>();
            int idxPeriod = 0;
            String currentSerie = "";
            int periodEvolution = 0;
            int nbPeriodNoActivity = 0;
            // Start at first period played
            for (TourSeriePeriodResult e : listPeriodResult) {
                // loop for period not activity
                while (idxPeriod < listPeriod.size()) {
                    String period = listPeriod.get(idxPeriod).getPeriodID();
                    idxPeriod++;
                    // Activity on this period ?
                    if (period.equals(e.getPeriodID())) {
                        break;
                    }
                    if (listSeriePlayer.isEmpty()) {
                        continue;
                    }
                    nbPeriodNoActivity++;
                    if (!currentSerie.equals(SERIE_NC)) {
                        currentSerie = computeSerieEvolution(currentSerie, periodEvolution, false);
                        String temp = currentSerie;
                        // 1 period with no activity => maintain
                        // a second period with no activity => reserve for the next period
                        if (nbPeriodNoActivity > 2) {
                            temp = buildSerieReserve(temp);
                        }
                        listSeriePlayer.add(temp);
                    }
                    if (nbPeriodNoActivity == 2) {
                        // serie down after one period with no activity
                        periodEvolution = -1;
                    } else {
                        // maintain in serie
                        periodEvolution = 0;
                    }
                }
                // case of NC => complete with serie after NC
                if (currentSerie.equals(SERIE_NC)) {
                    if (nbPeriodNoActivity == 1) {
                        listSeriePlayer.add(e.getSerie());
                    }
                    if (nbPeriodNoActivity >= 2) {
                        listSeriePlayer.add(computeSerieEvolution(e.getSerie(), 1, false));
                        listSeriePlayer.add(computeSerieEvolution(e.getSerie(), 1, false));
                        for (int i=3; i <= nbPeriodNoActivity; i++) {
                            listSeriePlayer.add(buildSerieReserve(e.getSerie()));
                        }
                    }
                }
                // period is same => activity
                nbPeriodNoActivity = 0;

                listSeriePlayer.add(e.getSerie());
                currentSerie = e.getSerie();
                periodEvolution = e.getEvolution();
            }

            // complete with last period if necessary
            while (idxPeriod < listPeriod.size()) {
                idxPeriod++;
                if (listSeriePlayer.isEmpty()) {
                    break;
                }
                nbPeriodNoActivity++;
                if (!currentSerie.equals(SERIE_NC)) {
                    currentSerie = computeSerieEvolution(currentSerie, periodEvolution, false);
                    String temp = currentSerie;
                    // 1 period with no activity => maintain
                    // a second period with no activity => reserve for the next period
                    if (nbPeriodNoActivity > 2) {
                        temp = buildSerieReserve(temp);
                    }
                    listSeriePlayer.add(temp);
                }
                if (nbPeriodNoActivity == 2) {
                    // serie down after one period with no activity
                    periodEvolution = -1;
                } else {
                    // maintain in serie
                    periodEvolution = 0;
                }
            }
            PlayerCache pc = playerCacheMgr.getPlayerCache(playerID);
            // case of NC => complete with serie after NC
            if (currentSerie.equals(SERIE_NC) && pc != null) {
                if (nbPeriodNoActivity == 1) {
                    listSeriePlayer.add(pc.serie);
                }
                if (nbPeriodNoActivity >= 2) {
                    listSeriePlayer.add(computeSerieEvolution(pc.serie, 1, false));
                    listSeriePlayer.add(computeSerieEvolution(pc.serie, 1, false));
                    for (int i=3; i <= nbPeriodNoActivity; i++) {
                        listSeriePlayer.add(buildSerieReserve(pc.serie));
                    }
                }
            }

            // At the end at the current period
            if (listSeriePlayer.size() > 0 && pc != null) {
                listSeriePlayer.add(buildWSSerie(pc));
            }
            // transform list to historic string and group period with same serie
            String previous = "";
            int nbSame = 0;
            for (String s : listSeriePlayer) {
                if (s.equals(previous)) {
                    nbSame++;
                } else {
                    if (previous.length() > 0 && nbSame > 0) {
                        if (historic.length() > 0) {
                            historic += ";";
                        }
                        historic += previous+"-"+nbSame;
                    }
                    previous = s;
                    nbSame = 1;
                }
            }
            if (previous.length() > 0 && nbSame > 0) {
                if (historic.length() > 0) {
                    historic += ";";
                }
                historic += previous+"-"+nbSame;
            }
        }
        return historic;
    }

    /**
     * BUild info serie for a player.
     * @param playerCache
     * @return
     */
    public WSSerieStatus buildSerieStatusForPlayer(PlayerCache playerCache) {
        WSSerieStatus result = new WSSerieStatus();
        if (playerCache != null && playerCache.serie != null && isEnable()) {
            result.serie = buildWSSerie(playerCache);
            TourSerieMemPeriodRanking rankSerie = getSerieRanking(playerCache.serie);
            if (rankSerie == null) {
                log.error("No ranking found for serie="+playerCache.serie);
            } else {
                result.nbPlayerInSerie = rankSerie.getNbPlayerForRanking();
                TourSerieMemPeriodRankingPlayer rankPlayer = rankSerie.getPlayerRank(playerCache.ID);
                if (rankPlayer != null) {
                    result.nbTournamentPlayed = rankPlayer.getNbTournamentPlayed();
                    result.rank = rankPlayer.rank;
                    result.trend = rankPlayer.trend;
                    result.result = rankPlayer.result;
                } else {
                    result.trend = computeTrendForPlayerNoActivityThisPeriod(playerCache.serie, playerCache.serieLastPeriodPlayed);
                }
            }
            result.historic = getWSSerieHistoricForPlayer(playerCache.ID);
        } else {
            result.serie = SERIE_NC;
            result.rank = -1;
            result.trend = 0;
        }
        return result;
    }

    /**
     * Return the backup path for this serie identify by the string
     * @param serieStr
     * @return
     */
    public String getPathBackupForSerieRanking(String serieStr) {
        if (serieStr != null && serieStr.length() > 0) {
            String pathBackup = getStringResolvEnvVariableValue("backupMemoryPath", null);
            if (pathBackup != null) {
                try {
                    File f = new File(pathBackup);
                    if (!f.exists()) {
                        f.mkdirs();
                    }
                } catch (Exception e) {
                    log.error("Failed to create dir - pathBackup="+pathBackup, e);
                }
                return FilenameUtils.concat(pathBackup, serieStr+".json");
            } else {
                log.error("No path to backup defined in configuration");
            }
        } else {
            log.error("Param serieStr is not valid : serieStr="+serieStr);
        }
        return null;
    }

    /**
     * Save all ranking serie to json files
     * @return
     */
    public int backupAllSerieRanking() {
        int nbBackup = 0;
        long ts = System.currentTimeMillis();
        for (TourSerieMemPeriodRanking pr : mapSerieRanking.values()) {
            long tsTemp = System.currentTimeMillis();
            if (backupSerieRanking(pr)) {
                log.error("Success to backup ranking for serie="+pr.serie+" - ts="+(System.currentTimeMillis() - tsTemp));
                nbBackup++;
            } else {
                log.error("Failed to backup ranking for serie="+pr.serie+" - ts="+(System.currentTimeMillis() - tsTemp));
            }
        }
        log.error("Backup ranking serie - nb OK="+nbBackup+" - ts="+(System.currentTimeMillis() - ts));
        return nbBackup;
    }

    /**
     * Save the serie ranking object to json file
     * @param pr
     * @return
     */
    public boolean backupSerieRanking(TourSerieMemPeriodRanking pr) {
        if (pr != null) {
            String pathBackup = getPathBackupForSerieRanking(pr.serie);
            if (pathBackup != null) {
                try {
                    jsonTools.transform2File(pathBackup, pr, true);
                    log.debug("Backup with success SerieRanking = "+pr+" - pathBackup="+pathBackup);
                    return true;
                } catch (Exception e) {
                    log.error("Failed to backup SerieRanking - sr="+pr, e);
                }
            } else {
                log.error("No path to backup SerieRanking="+pr);
            }
        } else {
            log.error("Param SerieRanking is null");
        }
        return false;
    }

    /**
     * Load all serie ranking from JSON file
     */
    public void loadAllSerieRankingFromFile() {
        log.error("Load all ranking serie - start");
        long ts = System.currentTimeMillis();
        for (String serie : SERIE_TAB) {
            long tsTemp = System.currentTimeMillis();
            if (loadSerieRankingFromFile(getPathBackupForSerieRanking(serie))) {
                log.warn("Success to load ranking serie="+serie+" - ts="+(System.currentTimeMillis() - tsTemp));
            } else {
                log.error("Failed to load ranking serie="+serie);
            }
        }
        log.warn("Load all ranking serie - end - ts="+(System.currentTimeMillis() - ts));

    }
    /**
     * Parse json file and load SerieRanking associated and add it to results of current period
     * @param filePath
     * @return
     */
    public boolean loadSerieRankingFromFile(String filePath) {
        if (filePath != null) {
            try {
                TourSerieMemPeriodRanking pr = jsonTools.mapDataFromFile(filePath, TourSerieMemPeriodRanking.class);
                if (pr != null) {
                    // add or replace serie in current map
                    mapSerieRanking.put(pr.serie, pr);
                    int nbPlayerSerie = 0;
                    if (!pr.serie.equals(SERIE_NC)) {
                        nbPlayerSerie = countPlayerInSerieExcludeReserve(pr.serie, true);
                    }
                    pr.updateNbPlayerSerie(nbPlayerSerie);
                    pr.loadThresholdFromConfiguration();
                    // compute serie
                    pr.computeRanking();
                    // order player data by date asc
                    pr.orderAllPlayerDataByDateAsc();
                    log.warn("Load with success SerieRanking = " + pr + " from file=" + filePath);
                    return true;
                } else {
                    log.error("SerieRanking is null !! loaded from filePath="+filePath);
                }
            } catch (Exception e) {
                log.error("Failed to load SerieRanking from filePath="+filePath+" - error="+e.getMessage());
            }
        } else {
            log.error("Param filePath is null");
        }
        return false;
    }

    /**
     * Check lastPeriodPlayed for a player to set reserve in a serie : players with lastPeriodPlayed not in (P, P-1, P-2) => reserve. No reserve for NC.
     * @param serie current serie
     * @param lastPeriodPlayed
     * @param trendNextPeriod true to have the projection for next period
     * @return
     */
    public boolean isPlayerReserve(String serie, String lastPeriodPlayed, boolean trendNextPeriod) {
        if (serie != null && lastPeriodPlayed != null) {
            if (serie.equals(SERIE_NC)) {
                // no reserve for NC
                return false;
            }
            // lastPeriodPlayed = current => not reserve
            if (currentPeriod.getPeriodID().equals(lastPeriodPlayed)) {
                return false;
            }
            // check only if period-1 and period-2 not null
            if (periodIDPrevious != null && periodIDBefore2 != null) {
                // lastPeriodPlayed = period-1 => not reserve (maintain)
                if (periodIDPrevious.equals(lastPeriodPlayed)) {
                    return false;
                }
                // lastPeriodPlayed = period-2 => not reserve (down at the end of current period)
                if (periodIDBefore2.equals(lastPeriodPlayed)) {
                    // at next period, reserve !
                    return trendNextPeriod;
                }
                // lastPeriodPlayed not current, not period-1 and not period-2 => reserve
                return true;
            }
        }
        return false;
    }

    /**
     * Check if last period played is current or previous
     * @param lastPeriodPlayed
     * @return
     */
    public boolean isPlayerActivePreviousPeriod(String lastPeriodPlayed) {
        if (lastPeriodPlayed != null) {
            if (currentPeriod.getPeriodID().equals(lastPeriodPlayed)) {
                return true;
            }
            return periodIDPrevious != null && periodIDPrevious.equals(lastPeriodPlayed);
        }
        return false;
    }

    /**
     * Build serie string for WebServices for a player cache
     * @param pc
     * @return
     */
    public String buildWSSerie(PlayerCache pc) {
        if (pc == null) {
            return SERIE_NC;
        }
        return buildWSSerie(pc.serie, pc.serieLastPeriodPlayed);
    }

    /**
     * Build serie string for WebServices
     * @param serie
     * @param lastPeriodPlayed
     * @return
     */
    public String buildWSSerie(String serie, String lastPeriodPlayed) {
        if (serie == null || !isEnable()) {
            return SERIE_NC;
        }
        if (isPlayerReserve(serie, lastPeriodPlayed, false)) {
            return buildSerieReserve(serie);
        }
        return serie;
    }

    /**
     * Transform serie to serie reserve
     * @param serie
     * @return
     */
    public static String buildSerieReserve(String serie) {
        if (!serie.equals(SERIE_NC) && !serie.equals(SERIE_TOP)) {
            return serie + "R";
        }
        return serie;
    }

    /**
     * Extract ranking on serie center on player
     * @param serie
     * @param rankingPlayerAsk
     * @param playerAskReserve
     * @param nbMax
     * @return
     */
    public List<WSRankingSeriePlayer> getWSRankingExtractForSerie(String serie, WSRankingSeriePlayer rankingPlayerAsk, boolean playerAskReserve, int nbMax) {
        List<WSRankingSeriePlayer> listWSRanking = new ArrayList<WSRankingSeriePlayer>();
        TourSerieMemPeriodRanking serieRanking = getSerieRanking(serie);
        if (nbMax <= 0) {
            nbMax = 5;
        }
        if (serieRanking != null) {
            if (rankingPlayerAsk != null) {
                long playerIDAsk = rankingPlayerAsk.getPlayerID();
                if (rankingPlayerAsk.getRank() > 0) {
                    int offset = rankingPlayerAsk.getRank() - 3;
                    // rank player is near the end of ranking => move offset to the last packet of nbMax size
                    if (offset > 0 && (offset + nbMax) > serieRanking.getNbPlayerForRanking()) {
                        offset = serieRanking.getNbPlayerForRanking() - nbMax;
                    }
                    if (offset < 0) {
                        offset = 0;
                    }
                    listWSRanking = getWSRankingForSerie(serie, rankingPlayerAsk, playerAskReserve, offset, nbMax);
                } else {
                    // no ranking for player => list first players and add ranking player
                    int offset = 0;
                    if (!serie.equals(TourSerieMgr.SERIE_NC) && serieRanking.getNbPlayerActive(null, null) > (nbMax/2)) {
                        offset = serieRanking.getNbPlayerActive(null, null) - (nbMax/2);
                    }
                    listWSRanking = getWSRankingForSerie(serie, rankingPlayerAsk, playerAskReserve, offset, nbMax);
                }

                if (!playerAskReserve) {
                    boolean playerPresent = false;
                    for (WSRankingSeriePlayer e : listWSRanking) {
                        if (e.getPlayerID() == playerIDAsk) {
                            playerPresent = true;
                            break;
                        }
                    }
                    // player not present in the list ... set at the end of list
                    if (!playerPresent && listWSRanking.size() > 0) {
                        if (listWSRanking.size() == nbMax) {
                            listWSRanking.set(listWSRanking.size() - 1, rankingPlayerAsk);
                        } else {
                            listWSRanking.add(rankingPlayerAsk);
                        }
                    }
                }
            }else {
                listWSRanking = getWSRankingForSerie(serie, null, false, 0, nbMax);
            }
        } else {
            log.error("No serie ranking for serie="+serie);
        }
        return listWSRanking;
    }

    /**
     * Get ranking of serie for Web Services
     * @param serie
     * @param rankingPlayerAsk ranking for player, use to swap with player with same rank = asked player always first at its rank
     * @param playerAskReserve if playerAsk is in reserve, he is not in ranking !
     * @param offset
     * @param nbMax
     * @return
     */
    public List<WSRankingSeriePlayer> getWSRankingForSerie(String serie, WSRankingSeriePlayer rankingPlayerAsk, boolean playerAskReserve, int offset, int nbMax) {
        List<WSRankingSeriePlayer> listWSRanking = new ArrayList<WSRankingSeriePlayer>();
        TourSerieMemPeriodRanking serieRanking = getSerieRanking(serie);
        if (offset < 0) {
            offset = 0;
        }
        if (serieRanking != null) {
            long playerIDAsk = -1;
            int idxPlayer = -1;
            int playerAskRank = -1;
            int idxSameRankPlayer = -1; // important to initialize with -1 => indicate no yet done (index of the first same result with same rank)
            boolean plaProcess = false; // flag to indicate that the player result has been processed in the result list
            if (rankingPlayerAsk != null) {
                playerIDAsk = rankingPlayerAsk.getPlayerID();
                playerAskRank = rankingPlayerAsk.getRank();
            }
            // list result for player playing in the current period => from memory
            if (offset < serieRanking.getNbPlayerActive(null, null)) {
                List<TourSerieMemPeriodRankingPlayer> listResultPlayer = serieRanking.getRankingOrderTrendAndResult(offset, nbMax, null, null);
                for (TourSerieMemPeriodRankingPlayer rp : listResultPlayer) {
                    if (listWSRanking.size() >= nbMax) break;
                    if (playerIDAsk != -1 && rp.playerID == playerIDAsk) {
                        plaProcess = true;
                        idxPlayer = listWSRanking.size();
                    }
                    if (playerIDAsk != -1 && rp.rank == playerAskRank && !plaProcess && idxSameRankPlayer == -1) {
                        idxSameRankPlayer = listWSRanking.size();
                    }
                    listWSRanking.add(rp.toWSRankingSeriePlayer(null, playerIDAsk, presenceMgr.isSessionForPlayerID(rp.playerID)));
                }
                // set player ask first between same rank
                if (idxSameRankPlayer != -1) {
                    // same rank find and player not in the list ... place rankingPlayer at the index
                    if (idxPlayer == -1 && rankingPlayerAsk != null) {
                        listWSRanking.set(idxSameRankPlayer, rankingPlayerAsk);
                    }
                    // same rank find and player in the list : swap elements to have player at first
                    else if (idxPlayer >= 0 && idxPlayer > idxSameRankPlayer) {
                        try {
                            Collections.swap(listWSRanking, idxPlayer, idxSameRankPlayer);
                            idxPlayer = idxSameRankPlayer;
                        } catch (Exception e) {
                            log.error("Failed to swap : idxPlayer=" + idxPlayer + " - indexSameRank=" + idxSameRankPlayer + " - listResult size=" + listWSRanking.size(), e);
                        }
                    }
                }
            }
            // complete with player no activity and not in reserve
            if (!serie.equals(TourSerieMgr.SERIE_NC)) {
                if (listWSRanking.size() < nbMax) {
                    int idxBdd = 0;
                    if (offset > serieRanking.getNbPlayerActive(null, null)) {
                        idxBdd = offset - serieRanking.getNbPlayerActive(null, null);
                    }
                    if (periodIDPrevious != null) {
                        // list ranking player with lastPeriodPlayed=P-1 : trend = 0
                        int nbPeriod1 = countTourSeriePlayerWithLastPeriodPlayed(serie, periodIDPrevious);
                        if (idxBdd < nbPeriod1) {
                            // player ask not in reserve and player has same trend(0) => place it at first
                            if (!playerAskReserve && rankingPlayerAsk != null && rankingPlayerAsk.rank == -1 && rankingPlayerAsk.trend == 0 && idxBdd == 0) {
                                listWSRanking.add(rankingPlayerAsk);
                                plaProcess = true;
                            }
                            List<TourSeriePlayer> listPlaNoActivityP1 = listTourSeriePlayerWithLastPeriodPlayed(serie, periodIDPrevious, idxBdd, nbMax - listWSRanking.size());
                            if (listPlaNoActivityP1 != null) {
                                for (TourSeriePlayer tsp : listPlaNoActivityP1) {
                                    if (listWSRanking.size() >= nbMax) break;
                                    if (playerIDAsk != -1 && tsp.getPlayerID() == playerIDAsk) {
                                        // result already added just before
                                        continue;
                                    }
                                    listWSRanking.add(buildWSRankingForPlayer(tsp.getPlayerID(), null, playerCacheMgr.getPlayerCache(tsp.getPlayerID()), playerIDAsk));
                                }
                            }
                        }
                        // list ranking player with lastPeriodPlayed=P-2 : trend = -1
                        if (periodIDBefore2 != null && listWSRanking.size() < nbMax) {
                            idxBdd = 0;
                            if (offset > (serieRanking.getNbPlayerActive(null, null)+nbPeriod1)) {
                                idxBdd = offset - (serieRanking.getNbPlayerActive(null, null)+nbPeriod1);
                            }
                            // player ask not in reserve and player has same trend(-1) => place it at first
                            if (!playerAskReserve && rankingPlayerAsk != null && rankingPlayerAsk.rank == -1 && ((rankingPlayerAsk.trend == -1 && !serie.equals(SERIE_11)) || (rankingPlayerAsk.trend == 0 && serie.equals(SERIE_11))) && idxBdd == 0 && !plaProcess) {
                                listWSRanking.add(rankingPlayerAsk);
                            }
                            List<TourSeriePlayer> listPlaNoActivityP2 = listTourSeriePlayerWithLastPeriodPlayed(serie, periodIDBefore2, idxBdd, nbMax - listWSRanking.size());
                            if (listPlaNoActivityP2 != null) {
                                for (TourSeriePlayer tsp : listPlaNoActivityP2) {
                                    if (listWSRanking.size() >= nbMax) break;
                                    if (playerIDAsk != -1 && tsp.getPlayerID() == playerIDAsk) {
                                        // result already added just before
                                        continue;
                                    }
                                    listWSRanking.add(buildWSRankingForPlayer(tsp.getPlayerID(), null, playerCacheMgr.getPlayerCache(tsp.getPlayerID()), playerIDAsk));
                                }
                            }
                        }
                    }
                }
            }
        } else {
            log.error("No serie ranking for serie=" + serie);
        }
        return listWSRanking;
    }

    /**
     * Build ranking player
     * @param memRankPlayer can be null for player not active on current period => use the playerCache
     * @param playerCache
     * @param playerIDAsk
     * @return
     */
    public WSRankingSeriePlayer buildWSRankingForPlayer(long playerID, TourSerieMemPeriodRankingPlayer memRankPlayer, PlayerCache playerCache, long playerIDAsk) {
        if (memRankPlayer != null) {
            return memRankPlayer.toWSRankingSeriePlayer(playerCache, playerIDAsk, presenceMgr.isSessionForPlayerID(playerCache.ID));
        }
        if (playerCache != null) {
            WSRankingSeriePlayer rankPla = new WSRankingSeriePlayer(playerCache, playerIDAsk, presenceMgr.isSessionForPlayerID(playerID));
            rankPla.setRank(-1);
            rankPla.setResult(0);
            rankPla.setTrend(computeTrendForPlayerNoActivityThisPeriod(playerCache.serie, playerCache.serieLastPeriodPlayed));
            return rankPla;
        }
        // no memory data, no player cache ... => load playerCache
        PlayerCache loadPC = playerCacheMgr.getOrLoadPlayerCache(playerID);
        if (loadPC != null) {
            WSRankingSeriePlayer rankPla = new WSRankingSeriePlayer(loadPC, playerIDAsk, presenceMgr.isSessionForPlayerID(playerID));
            rankPla.setRank(-1);
            rankPla.setResult(0);
            rankPla.setTrend(computeTrendForPlayerNoActivityThisPeriod(loadPC.serie, loadPC.serieLastPeriodPlayed));
            return rankPla;
        }
        // TODO if no playercache => load data from player database ...
        return null;
    }

    /**
     * Build the resultDeal for the game most played
     * @param tourID
     * @param dealIndex
     * @return
     */
    public WSResultDeal buildWSResultGameMostPlayed(String tourID, int dealIndex, int nbPlayer) {
        WSResultDeal result = new WSResultDeal();
        try {
            Criteria criteria = new Criteria().andOperator(Criteria.where("tournament.$id").is(new ObjectId(tourID)), Criteria.where("dealIndex").is(dealIndex), Criteria.where("finished").is(true));
            TypedAggregation<TourSerieGame> aggGame = Aggregation.newAggregation(
                    TourSerieGame.class,
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
                result.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
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
     * Return the gameView object for this game with game data and table. The player must have played the deal of the game.
     * @param gameID
     * @param player
     * @return
     * @throws FBWSException
     */
    @Override
    public WSGameView viewGame(String gameID, Player player) throws FBWSException {
        // check gameID
        TourSerieGame game = getGame(gameID);
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
        TourSerieTournament tour = getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        TourSerieDeal deal = (TourSerieDeal)tour.getDeal(dealID);
        if (tour == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // check player played the deal of this game
        TourSerieGame game = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, player.getID());
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
                game = mongoTemplate.findOne(q, TourSerieGame.class);
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

    /**
     * Return the notif for player in new Serie without play during last period
     * @return
     */
    public MessageNotifGroup getNotifGroupNewNoPlay() {
        return notifNewNoPlay;
    }


    /**
     * Remove game from Database
     * @param game
     * @return
     */
    public boolean deleteGame(TourSerieGame game) {
        if (game != null) {
            try {
                mongoTemplate.remove(game);
                return true;
            } catch (Exception e) {
                log.error("Failed to remove game=" + game, e);
            }
        } else {
            log.error("Parameter game is null !!");
        }
        return false;
    }

    /**
     * Delete game with ID
     * @param gameID
     * @return
     */
    public void deleteGame(String gameID) {
        deleteGame(getGame(gameID));
    }

    /**
     * Remove game & result for player on tournament and deal
     * @param tourID
     * @param dealIndex
     * @param playerID
     * @return
     */
    public boolean removeResultForTournamentInProgress(String tourID, int dealIndex, long playerID) {
        boolean result = false;
        TourSerieGame game = getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        if (game != null) {
            if (game.isFinished()) {
                if (memoryMgr.removeResult(tourID, dealIndex, playerID)) {
                    deleteGame(game);
                    return true;
                } else {
                    log.error("Failed to remove result from memory ! - player="+playerID+" - tourID="+tourID+" - dealIndex="+dealIndex);
                }
            } else {
                log.error("Game is not finished ! - games="+game);
            }
        } else {
            log.error("No game for player="+playerID+" - tourID="+tourID+" - dealIndex="+dealIndex);
        }
        return result;
    }

    /**
     * Remove all result (game) for player on serie on current period
     * @param serie
     * @param playerID
     * @return
     */
    public int removeAllResultForTournamentInProgress(String serie, long playerID) {
        int nbGameRemoved = 0;
        TourSerieMemPeriodRanking serieRanking = getSerieRanking(serie);
        if (serieRanking != null) {
            TourSerieMemPeriodRankingPlayer rankingPlayer = serieRanking.getPlayerRank(playerID);
            if (rankingPlayer != null) {
                if (rankingPlayer.listData != null && rankingPlayer.listData.size() > 0) {
                    // remove current tournament finished by player
                    List<String> listTourID = new ArrayList<>();
                    for (TourSerieMemPeriodRankingPlayerData data : rankingPlayer.listData) {
                        listTourID.add(data.tourID);
                    }
                    for (String tourID : listTourID) {
                        for (int i = 1; i <= 4; i++) {
                            if (removeResultForTournamentInProgress(tourID, i, playerID)) {
                                nbGameRemoved++;
                            } else {
                                log.error("Failed to remove result for tour=" + tourID + " - dealIndex=" + i + " - playerID=" + playerID);
                            }
                        }
                    }
                }
            } else {
                log.error("No rankingPlayer found in serie="+serie+" - playerID="+playerID);
            }
            TourSerieMemTour memTourInProgress = memoryMgr.getTournamentInProgressForPlayer(serie, playerID);
            if (memTourInProgress != null) {
                List<TourSerieGame> listGame = listGameOnTournamentForPlayer(memTourInProgress.tourID, playerID);
                for (TourSerieGame g : listGame) {
                    if (g.isFinished()) {
                        if (removeResultForTournamentInProgress(memTourInProgress.tourID, g.getDealIndex(), playerID)) {
                            nbGameRemoved++;
                        }
                    } else {
                        if (deleteGame(g)) {
                            nbGameRemoved++;
                        }
                    }
                }
            }
        } else {
            log.error("No serieRanking found for serie="+serie);
        }
        return nbGameRemoved;
    }

    @Override
    public void updateGamePlayArgineFinished(Game game) throws FBWSException {

    }

    @Override
    public void updateGamePlayArgine(Game game) {

    }

    /**
     * Will the player be relegated ?
     * @param playerID
     * @return
     */
    public boolean isPlayerNegativeTrend(long playerID, String serie) {
        TourSerieMemPeriodRankingPlayer currentRankingPlayer = getCurrentRankingPlayer(serie, playerID);
        if (currentRankingPlayer != null) {
            return currentRankingPlayer.trend < 0;
        }
        return false;
    }

    /**
     * Count total player in ranking for a period
     * @param periodID
     * @param selectionPlayerID
     * @param countryCode
     * @return
     */
    public int countPlayerRanking(String periodID, List<Long> selectionPlayerID, String countryCode) {
        if (periodID != null) {
            if (currentPeriod != null && periodID.equals(currentPeriod.getPeriodID())) {
                int nbPlayers = 0;
                for (TourSerieMemPeriodRanking rs : mapSerieRanking.values()) {
                    if ((countryCode != null && countryCode.length() > 0) || (selectionPlayerID != null && selectionPlayerID.size() > 0)) {
                        nbPlayers += countPlayerInSerieExcludeReserve(rs.serie, countryCode, selectionPlayerID, true);
                    } else {
                        nbPlayers += rs.getNbPlayerForRanking();
                    }
                }
                return nbPlayers;
            } else if (periodIDPrevious != null && periodID.equals(periodIDPrevious)) {
                List<Criteria> listCriteria = new ArrayList<>();
                listCriteria.add(Criteria.where("periodID").is(periodID));
                if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
                    listCriteria.add(Criteria.where("playerID").in(selectionPlayerID));
                }
                if (countryCode != null && countryCode.length() > 0) {
                    listCriteria.add(Criteria.where("countryCode").is(countryCode));
                }
                Query query = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
                return (int)mongoTemplate.count(query, TourSeriePeriodResult.class);
            }
        }
        return 0;
    }

    /**
     * Count player with serie best than serieNotIncluded on periodID (current or previous)
     * @param serieNotIncluded
     * @param periodID
     * @param countryCode
     * @param listPlayerFilter
     * @return
     */
    public int countPlayerInSeriesUp(String serieNotIncluded, String periodID, String countryCode, List<Long> listPlayerFilter) {
        if (serieNotIncluded.equals(SERIE_TOP)) {
            return 0;
        }
        if (currentPeriod != null && periodID.equals(currentPeriod.getPeriodID())) {
            int nbPlayers = 0;
            for (TourSerieMemPeriodRanking rs : mapSerieRanking.values()) {
                try {
                    if (compareSerie(rs.serie, serieNotIncluded) > 0) {
                        if ((countryCode != null && countryCode.length() > 0) || (listPlayerFilter != null && listPlayerFilter.size() > 0)) {
                            nbPlayers += countPlayerInSerieExcludeReserve(rs.serie, countryCode, listPlayerFilter, true);
                        } else {
                            nbPlayers += rs.getNbPlayerForRanking();
                        }
                    }
                } catch (Exception e) {
                    log.error("Exception to compare serie - rs.serie="+rs.serie+" - serieNotIncluded="+serieNotIncluded, e);
                }
            }
            return nbPlayers;
        }
        else if (periodIDPrevious != null && periodID.equals(periodIDPrevious)) {
            List<Criteria> listCriteria = new ArrayList<>();
            listCriteria.add(Criteria.where("periodID").is(periodID));
            List<String> listSerie = new ArrayList<>();
            listSerie.add(SERIE_TOP);
            for (String e : SERIE_TAB){
                try {
                    if (compareSerie(e, serieNotIncluded) > 0) {
                        listSerie.add(e);
                    }
                } catch (Exception ex) {
                    log.error("Exception to compare serie="+e+" - serieNotIncluded="+serieNotIncluded, ex);
                }
            }
            listCriteria.add(Criteria.where("serie").in(listSerie));
            if (listPlayerFilter != null && listPlayerFilter.size() > 0) {
                listCriteria.add(Criteria.where("playerID").in(listPlayerFilter));
            }
            if (countryCode != null && countryCode.length() > 0) {
                listCriteria.add(Criteria.where("countryCode").is(countryCode));
            }
            Query query = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
            return (int)mongoTemplate.count(query, TourSeriePeriodResult.class);
        }
        return 0;
    }

    /**
     * count player with best rankMain on previous period
     * @param countryCode
     * @param rank
     * @return
     */
    public int countInPreviousPeriodPlayerInCountryWithBestRank(String countryCode, int rank, List<Long> listPlayerID) {
        if (periodIDPrevious != null && periodIDPrevious.length() > 0) {
            List<Criteria> listCriteria = new ArrayList<>();
            listCriteria.add(Criteria.where("periodID").is(periodIDPrevious));
            if (listPlayerID != null && listPlayerID.size() > 0) {
                listCriteria.add(Criteria.where("playerID").in(listPlayerID));
            }
            listCriteria.add(Criteria.where("rankMain").lt(rank));
            listCriteria.add(Criteria.where("countryCode").is(countryCode));
            Query query = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
            return (int)mongoTemplate.count(query, TourSeriePeriodResult.class);
        }
        return 0;
    }

    /**
     * count player with same rankMain on previous period
     * @param countryCode
     * @param rank
     * @return
     */
    public int countInPreviousPeriodPlayerInCountryWithSameRank(String countryCode, int rank) {
        if (periodIDPrevious != null && periodIDPrevious.length() > 0) {
            List<Criteria> listCriteria = new ArrayList<>();
            listCriteria.add(Criteria.where("periodID").is(periodIDPrevious));
            listCriteria.add(Criteria.where("rankMain").is(rank));
            if (countryCode != null && countryCode.length() > 0) {
                listCriteria.add(Criteria.where("countryCode").is(countryCode));
            }
            Query query = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
            return (int)mongoTemplate.count(query, TourSeriePeriodResult.class);
        }
        return 0;
    }

    /**
     * Ranking on period
     * @param playerAsk
     * @param periodID
     * @param selectionPlayerID
     * @param countryCode
     * @param offset
     * @param nbMax
     * @return
     */
    public ResultServiceRest.GetMainRankingResponse getRanking(PlayerCache playerAsk, String periodID, List<Long> selectionPlayerID, String countryCode, int offset, int nbMax) throws FBWSException{
        ResultServiceRest.GetMainRankingResponse response = new ResultServiceRest.GetMainRankingResponse();
        response.totalSize = countPlayerRanking(periodID, selectionPlayerID, countryCode);
        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
            response.nbRankedPlayers = countPlayerRanking(periodID, null, countryCode);
        } else {
            response.nbRankedPlayers = response.totalSize;
        }
        response.ranking = new ArrayList<>();

        // ---------------------------------
        // rankingPlayer
        if (playerAsk != null) {
            WSRankingSeriePlayer rankingPlayer = new WSRankingSeriePlayer(playerAsk, playerAsk.ID, true);
            if (periodID.equals(currentPeriod.getPeriodID())) {
                TourSerieMemPeriodRanking rankSerie = getSerieRanking(playerAsk.serie);
                if (rankSerie != null) {
                    rankingPlayer.serie = rankSerie.serie;
                    TourSerieMemPeriodRankingPlayer playerSerieRanking = rankSerie.getPlayerRank(playerAsk.ID);
                    if (playerSerieRanking != null) {
                        // player playing current period
                        int playerCurrentRank = playerSerieRanking.rank;
                        if (countryCode != null && countryCode.length() > 0) {
                            playerCurrentRank = rankSerie.countPlayerWithCountryAndBestRank(countryCode, playerSerieRanking.rank, null);
                        }
                        // main rank = nb players in serie up + rank in current serie
                        rankingPlayer.rank = countPlayerInSeriesUp(rankSerie.serie, periodID, countryCode, null) + playerCurrentRank;
                        // compute offset position if offset = -1
                        if (rankingPlayer.rank > 0 && offset == -1) {
                            if ((countryCode != null && countryCode.length() > 0) || (selectionPlayerID != null && selectionPlayerID.size() > 0)) {
                                offset = countPlayerInSeriesUp(rankSerie.serie, periodID, countryCode, selectionPlayerID) + rankSerie.countPlayerWithCountryAndBestRank(countryCode, playerSerieRanking.rank, selectionPlayerID) - (nbMax / 2);
                            } else {
                                offset = rankingPlayer.rank - (nbMax / 2);
                            }
                        }
                    } else if (periodIDPrevious != null && playerAsk.serieLastPeriodPlayed != null && playerAsk.serieLastPeriodPlayed.equals(periodIDPrevious)) {
                        // player not playing current period but previous
                        rankingPlayer.rank = countPlayerInSeriesUp(rankSerie.serie, periodID, countryCode, null) + rankSerie.getNbPlayerActive(countryCode, null) + 1;
                        // compute offset position if offset = -1
                        if (rankingPlayer.rank > 0 && offset == -1) {
                            if ((countryCode != null && countryCode.length() > 0) || (selectionPlayerID != null && selectionPlayerID.size() > 0)) {
                                offset = countPlayerInSeriesUp(rankSerie.serie, periodID, countryCode, null) + rankSerie.getNbPlayerActive(countryCode, null) - (nbMax / 2);
                            } else {
                                offset = rankingPlayer.rank - (nbMax / 2);
                            }
                        }
                    } else {
                        // not playing since a long time ...
                        rankingPlayer.rank = -1;
                    }
                } else {
                    rankingPlayer.rank = -1;
                }
            } else if (periodID.equals(periodIDPrevious)) {
                TourSeriePeriodResult periodResult = getPeriodResultForPlayer(periodID, playerAsk.ID);
                if (periodResult != null) {
                    rankingPlayer.serie = periodResult.getSerie();
                    int playerCurrentRank = periodResult.getRankMain();
                    if (countryCode != null && countryCode.length() > 0) {
                        playerCurrentRank = countInPreviousPeriodPlayerInCountryWithBestRank(countryCode, periodResult.getRankMain(), null) + 1; // listPlaFilter is null, we need the rank of player over all player
                    }
                    rankingPlayer.rank = playerCurrentRank;
                    // set offset value
                    if (rankingPlayer.rank > 0 && offset == -1) {
                        if ((countryCode != null && countryCode.length() > 0) || (selectionPlayerID != null && selectionPlayerID.size() > 0)) {
                            // in the case ranking with FRIEND option, use selectionPlayerID list to compute offet
                            offset = countInPreviousPeriodPlayerInCountryWithBestRank(countryCode, periodResult.getRankMain(), selectionPlayerID) - (nbMax/2);
                        } else {
                            offset = rankingPlayer.rank - (nbMax / 2);
                        }
                    }
                } else {
                    rankingPlayer.rank = -1;
                }
            }
            response.rankingPlayer = rankingPlayer;

        }
        if (offset < 0) {
            offset = 0;
        }
        response.offset = offset;

        // -------------------------------------
        // build list ranking for current period
        // WARNING : list contains players active on current period and players not in reserve !!
        // -------------------------------------
        if (periodID.equals(currentPeriod.getPeriodID())) {
            //*****************************************
            // case of selection player => porcess all friend and select sublist according to offset & nbMax
            if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
                List<Long> listPlayerUpdate = new ArrayList<>();
                listPlayerUpdate.addAll(selectionPlayerID);
                String currentSerie = SERIE_TOP;
                int tempNbPlayer = 0;
                // fill the ranking list with all friend
                while (listPlayerUpdate.size() > 0) {
                    TourSerieMemPeriodRanking memPeriodRanking = mapSerieRanking.get(currentSerie);
                    if (memPeriodRanking != null) {
                        List<TourSerieMemPeriodRankingPlayer> rankingPlayers = memPeriodRanking.getRankingOrderTrendAndResult(0, 0, listPlayerUpdate, null);
                        for (TourSerieMemPeriodRankingPlayer e : rankingPlayers) {
                            WSRankingSeriePlayer ws = e.toWSRankingSeriePlayer(playerCacheMgr.getPlayerCache(e.playerID), playerAsk.ID, presenceMgr.isSessionForPlayerID(e.playerID));
                            ws.setRank(tempNbPlayer + e.rank);
                            ws.serie = currentSerie;
                            listPlayerUpdate.remove(e.playerID);
                            response.ranking.add(ws);
                        }
                        // add player no activiy current period and not in reserve
                        List<TourSeriePlayer> listPlayerNoActivity = listTourSeriePlayerExcludeCurrentPeriodAndReserve(currentSerie, listPlayerUpdate, null, 0, 0);
                        if (listPlayerNoActivity != null && listPlayerNoActivity.size() > 0) {
                            int rankNoActivity = tempNbPlayer + memPeriodRanking.getNbPlayerActive(null, null) + 1;
                            for (TourSeriePlayer tsp : listPlayerNoActivity) {
                                PlayerCache pc = playerCacheMgr.getPlayerCache(tsp.getPlayerID());
                                WSRankingSeriePlayer ws = new WSRankingSeriePlayer(pc, playerAsk.ID, presenceMgr.isSessionForPlayerID(tsp.getPlayerID()));
                                ws.serie = currentSerie;
                                ws.setRank(rankNoActivity);
                                response.ranking.add(ws);
                            }
                        }
                        tempNbPlayer += memPeriodRanking.getNbPlayerForRanking();
                    } else {
                        log.error("No memPeriodRanking for serie="+currentSerie);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                    if (currentSerie.equals(SERIE_NC)) {break;}
                    if (!currentSerie.equals(SERIE_11)) {
                        currentSerie = computeSerieEvolution(currentSerie, -1, false);
                    } else {
                        currentSerie = SERIE_NC;
                    }
                }
                // now extract according offset & nbMax
                if ((offset > 0 && offset < response.ranking.size()) || response.ranking.size() > nbMax) {
                    response.ranking = response.ranking.subList(offset, (offset+nbMax)<response.ranking.size()?offset+nbMax:offset+nbMax-response.ranking.size());
                }
            }

            //*****************************************
            // country selection and normal case
            else {
                // search serie corresponding to offset
                String currentSerie = SERIE_TOP;
                int tempNbPlayer = 0;
                while (tempNbPlayer < offset) {
                    if (currentSerie.equals(SERIE_NC)) {
                        break;
                    }
                    TourSerieMemPeriodRanking memPeriodRanking = mapSerieRanking.get(currentSerie);
                    if (memPeriodRanking == null) {
                        log.error("No memPeriodRanking for serie="+currentSerie);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                    int nbPlayerSerie = countPlayerInSerieExcludeReserve(currentSerie, countryCode, null, true);
                    if ((tempNbPlayer + nbPlayerSerie) > offset) {
                        break;
                    }
                    tempNbPlayer += nbPlayerSerie;
                    if (!currentSerie.equals(SERIE_11)) {
                        currentSerie = computeSerieEvolution(currentSerie, -1, false);
                    } else {
                        currentSerie = SERIE_NC;
                    }

                }
                // get memPeriod for serie
                TourSerieMemPeriodRanking memPeriodRanking = mapSerieRanking.get(currentSerie);
                if (memPeriodRanking == null) {
                    log.error("No memPeriodRanking for serie="+currentSerie);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                // list mem result for active players
                int offsetSerie = offset;
                if (tempNbPlayer < offset) {
                    offsetSerie = offset - tempNbPlayer;
                }
                List<TourSerieMemPeriodRankingPlayer> memPeriodRankingPlayerList = memPeriodRanking.getRankingOrderTrendAndResult(offsetSerie, nbMax, null, countryCode);
                // case of country code selection
                if (countryCode != null && countryCode.length() > 0) {
                    int tempRank = 0, tempSameValue = 0, tempValue = 0;
                    for (TourSerieMemPeriodRankingPlayer e : memPeriodRankingPlayerList) {
                        WSRankingSeriePlayer ws = e.toWSRankingSeriePlayer(playerCacheMgr.getPlayerCache(e.playerID), playerAsk.ID, presenceMgr.isSessionForPlayerID(e.playerID));
                        if (tempRank == 0) {
                            tempRank = memPeriodRanking.countPlayerWithCountryAndBestRank(countryCode, e.rank, null) + 1;
                            tempValue = e.rank;
                            tempSameValue = 1;
                        } else {
                            if (e.rank == tempValue) {
                                tempSameValue++;
                            } else {
                                tempRank = tempRank + tempSameValue;
                                tempSameValue = 1;
                                tempValue = e.rank;
                            }
                        }
                        ws.rank = tempNbPlayer + tempRank;
                        ws.serie = currentSerie;
                        response.ranking.add(ws);
                    }
                }
                // case normal
                else {
                    for (TourSerieMemPeriodRankingPlayer e : memPeriodRankingPlayerList) {
                        WSRankingSeriePlayer ws = e.toWSRankingSeriePlayer(playerCacheMgr.getPlayerCache(e.playerID), playerAsk.ID, presenceMgr.isSessionForPlayerID(e.playerID));
                        ws.rank = tempNbPlayer + ws.rank;
                        ws.serie = currentSerie;
                        response.ranking.add(ws);
                    }
                }

                // complete with players with no activity and not in reserve
                if (response.ranking.size() < nbMax) {
                    int nbPlayerActivity = memPeriodRanking.getNbPlayerActive(countryCode, null);
                    int offsetNoActivity = offset - tempNbPlayer - nbPlayerActivity;
                    List<TourSeriePlayer> listPlayerNoActivity = listTourSeriePlayerExcludeCurrentPeriodAndReserve(currentSerie, null, countryCode, offsetNoActivity, nbMax - response.ranking.size());
                    if (listPlayerNoActivity != null && listPlayerNoActivity.size() > 0) {
                        int rankNoActivity = tempNbPlayer+nbPlayerActivity + 1;
                        for (TourSeriePlayer tsp : listPlayerNoActivity) {
                            PlayerCache pc = playerCacheMgr.getPlayerCache(tsp.getPlayerID());
                            WSRankingSeriePlayer ws = new WSRankingSeriePlayer(pc, playerAsk.ID, presenceMgr.isSessionForPlayerID(tsp.getPlayerID()));
                            ws.serie = currentSerie;
                            ws.setRank(rankNoActivity);
                            response.ranking.add(ws);
                        }
                    }
                }

                // update tempNbPlayer with nb player for current serie
                if (countryCode != null && countryCode.length() > 0) {
                    tempNbPlayer += countPlayerInSerieExcludeReserve(currentSerie, countryCode, null, true);
                } else {
                    tempNbPlayer += memPeriodRanking.getNbPlayerForRanking();
                }

                // if list not full, complete with players for next series
                while (response.ranking.size() < nbMax) {
                    if (currentSerie.equals(SERIE_NC)) {break;}
                    if (!currentSerie.equals(SERIE_11)) {
                        currentSerie = computeSerieEvolution(currentSerie, -1, false);
                    } else {
                        currentSerie = SERIE_NC;
                    }
                    memPeriodRanking = mapSerieRanking.get(currentSerie);
                    if (memPeriodRanking == null) {
                        log.error("No memPeriodRanking for serie="+currentSerie);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                    memPeriodRankingPlayerList = memPeriodRanking.getRankingOrderTrendAndResult(0, nbMax - response.ranking.size(), null, countryCode);
                    for (TourSerieMemPeriodRankingPlayer e : memPeriodRankingPlayerList) {
                        WSRankingSeriePlayer ws = e.toWSRankingSeriePlayer(playerCacheMgr.getPlayerCache(e.playerID), playerAsk.ID, presenceMgr.isSessionForPlayerID(e.playerID));
                        ws.rank = tempNbPlayer+ws.rank;
                        ws.serie = currentSerie;
                        response.ranking.add(ws);
                    }

                    // complete with players with no actitivty and not in reserve
                    if (response.ranking.size() < nbMax) {
                        List<TourSeriePlayer> listPlayerNoActivity = listTourSeriePlayerExcludeCurrentPeriodAndReserve(currentSerie, null, countryCode, 0, nbMax - response.ranking.size());
                        if (listPlayerNoActivity != null && listPlayerNoActivity.size() > 0) {
                            int rankNoActivity = tempNbPlayer+memPeriodRanking.getNbPlayerActive(countryCode, null) + 1;
                            for (TourSeriePlayer tsp : listPlayerNoActivity) {
                                PlayerCache pc = playerCacheMgr.getPlayerCache(tsp.getPlayerID());
                                WSRankingSeriePlayer ws = new WSRankingSeriePlayer(pc, playerAsk.ID, presenceMgr.isSessionForPlayerID(tsp.getPlayerID()));
                                ws.serie = currentSerie;
                                ws.setRank(rankNoActivity);
                                response.ranking.add(ws);
                            }
                        }
                    }

                    // update tempNbPlayer with nb player for current serie
                    if (countryCode != null && countryCode.length() > 0) {
                        tempNbPlayer += countPlayerInSerieExcludeReserve(currentSerie, countryCode, null, true);
                    } else {
                        tempNbPlayer += memPeriodRanking.getNbPlayerForRanking();
                    }
                }
            }
        }

        // --------------------------------------
        // build list ranking for previous period
        else if (periodID.equals(periodIDPrevious)) {
            List<Criteria> listCriteria = new ArrayList<>();
            listCriteria.add(Criteria.where("periodID").is(periodID));
            if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
                listCriteria.add(Criteria.where("playerID").in(selectionPlayerID));
            }
            listCriteria.add(Criteria.where("rankMain").gt(0));
            if (countryCode != null && countryCode.length() > 0) {
                listCriteria.add(Criteria.where("countryCode").is(countryCode));
            }

            Query query = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
            query.with(new Sort(Sort.Direction.ASC, "rankMain"));
            if (offset > 0) {
                query.skip(offset);
            }
            if (nbMax > 0) {
                query.limit(nbMax);
            }
            List<TourSeriePeriodResult> listPeriodResult = mongoTemplate.find(query, TourSeriePeriodResult.class);
            int rankCountry = 0, currentRankMain = 0, nbSameRankMain = 0;
            boolean changeRankAfterInit = false;
            for (TourSeriePeriodResult e : listPeriodResult) {
                WSRankingSeriePlayer ws = new WSRankingSeriePlayer(e, playerCacheMgr.getPlayerCache(e.getPlayerID()), playerAsk.ID, presenceMgr.isSessionForPlayerID(e.getPlayerID()));
                if (countryCode != null && countryCode.length() > 0) {
                    if (rankCountry == 0) {
                        rankCountry = countInPreviousPeriodPlayerInCountryWithBestRank(countryCode, e.getRankMain(), null) + 1;
                        currentRankMain = e.getRankMain();
                        nbSameRankMain = countInPreviousPeriodPlayerInCountryWithSameRank(countryCode, e.getRankMain());
                    } else {
                        if (currentRankMain != e.getRankMain()) {
                            rankCountry = rankCountry + nbSameRankMain;
                            currentRankMain = e.getRankMain();
                            changeRankAfterInit = true;
                            nbSameRankMain = 1;
                        } else {
                            if (changeRankAfterInit) {
                                nbSameRankMain++;
                            }
                        }
                    }
                    ws.setRank(rankCountry);
                } else {
                    ws.setRank(e.getRankMain());
                }
                ws.serie = e.getSerie();
                response.ranking.add(ws);
            }
        }
        return response;
    }

    /**
     * Update player country code in TourSeriePeriodResult, TourSeriePlayer DB objects and update TourSerieMemPeriodRankingPlayer object in memory
     * @param player
     */
    public void updatePlayerCountryCode(Player player) {
        if (player != null) {
            try {
                // TourSeriePeriodResult
                Query query = Query.query(Criteria.where("playerID").is(player.getID()));
                if (periodIDPrevious != null && periodIDPrevious.length() > 0) {
                    query.addCriteria(Criteria.where("periodID").is(periodIDPrevious));
                }
                mongoTemplate.updateMulti(query,
                        Update.update("countryCode", player.getDisplayCountryCode()),
                        TourSeriePeriodResult.class);
                // TourSeriePlayer & memory object
                TourSeriePlayer seriePlayer = getTourSeriePlayer(player.getID());
                if (seriePlayer != null) {
                    seriePlayer.setCountryCode(player.getDisplayCountryCode());
                    mongoTemplate.save(seriePlayer);
                    TourSerieMemPeriodRanking memPeriodRanking = mapSerieRanking.get(seriePlayer.getSerie());
                    if (memPeriodRanking != null) {
                        TourSerieMemPeriodRankingPlayer rankingPlayer = memPeriodRanking.getPlayerRank(player.getID());
                        if (rankingPlayer != null) {
                            rankingPlayer.countryCode = player.getDisplayCountryCode();
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to update country code for player="+player, e);
            }
        }
    }
}
