package com.funbridge.server.tournament.duel;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.message.MessageMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.dao.PlayerDuelDAO;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.PlayerDuel;
import com.funbridge.server.player.data.PlayerDuelStat;
import com.funbridge.server.player.data.PlayerLink;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.tournament.DealGenerator;
import com.funbridge.server.tournament.IDealGeneratorCallback;
import com.funbridge.server.tournament.duel.data.*;
import com.funbridge.server.tournament.duel.memory.DuelMemDeal;
import com.funbridge.server.tournament.duel.memory.DuelMemGame;
import com.funbridge.server.tournament.duel.memory.DuelMemTournament;
import com.funbridge.server.tournament.duel.memory.DuelMemTournamentPlayer;
import com.funbridge.server.tournament.game.*;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.game.WSGameView;
import com.funbridge.server.ws.game.WSTableGame;
import com.funbridge.server.ws.player.WSPlayerDuel;
import com.funbridge.server.ws.result.*;
import com.funbridge.server.ws.tournament.*;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.tools.StringTools;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Created by pserent on 07/07/2015.
 */
@Component(value="duelMgr")
@Scope(value="singleton")
public class DuelMgr extends TournamentGenericMgr implements IDealGeneratorCallback, ITournamentMgr {
    @Resource(name = "playerDuelDAO")
    private PlayerDuelDAO playerDuelDAO = null;
    @Resource(name = "presenceMgr")
    private PresenceMgr presenceMgr = null;
    @Resource(name = "messageNotifMgr")
    private MessageNotifMgr notifMgr = null;
    @Resource(name = "messageMgr")
    private MessageMgr messageMgr = null;
    @Resource(name = "playerMgr")
    private PlayerMgr playerMgr = null;
    @Resource(name = "mongoDuelTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name="playerCacheMgr")
    private PlayerCacheMgr playerCacheMgr = null;

    private static final String DUEL_ORIGIN_ARGINE = "ARGINE";
    private static final String DUEL_ORIGIN_MACTHMAKING = "MATCHMAKING";
    private static final String DUEL_ORIGIN_REQUEST = "REQUEST";
    private ScheduledExecutorService schedulerPurgeRequestExpired = Executors.newScheduledThreadPool(1), schedulerNotifReminder = Executors.newScheduledThreadPool(1), schedulerFinish = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> schedulerPurgeRequestExpiredFuture = null, schedulerNotifReminderFuture = null, schedulerFinishFuture = null;
    private ExecutorService threadPoolMatchMaking = null;
    private ConcurrentLinkedQueue<MatchMakingElement> matchMakingQueue = new ConcurrentLinkedQueue<>();
    private PurgeRequestExpiredTask purgeRequestExpiredTask = new PurgeRequestExpiredTask();
    private NotifReminderTask notifReminderTask = new NotifReminderTask();
    private FinisherTask finisherTask = new FinisherTask();
    private DuelMemoryMgr duelMemoryMgr;
    private LockWeakString lockDuelPlayer = new LockWeakString();
    private LockWeakString lockCreateTournamentDuel = new LockWeakString();
    private LockWeakString lockDuelStat = new LockWeakString();
    private DealGenerator dealGenerator = null;
    private GameMgr gameMgr;
    private Scheduler scheduler;
    private boolean argineDuelTaskRunning = false;
    private String statCurrentPeriodID, statPreviousPeriodID;
    private SimpleDateFormat sdfStatPeriod = new SimpleDateFormat("yyyyMM");

    public DuelMemoryMgr getMemoryMgr() {
        return duelMemoryMgr;
    }

    /**
     * Build the deal ID using tourID and deal index
     *
     * @param tourID
     * @param index
     * @return
     */
    public static String buildDealID(String tourID, int index) {
        return tourID + "-" + (index < 10 ? "0" : "") + index;
    }

    /**
     * Build the deal ID using tourID and deal index
     *
     * @param tourID
     * @param index
     * @return
     */
    public String _buildDealID(String tourID, int index) {
        return buildDealID(tourID, index);
    }

    /**
     * Extract tourID string from dealID
     *
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
     *
     * @param dealID
     * @return
     */
    public String _extractTourIDFromDealID(String dealID) {
        return extractTourIDFromDealID(dealID);
    }

    /**
     * Extract dealIndex from dealID
     *
     * @param dealID
     * @return
     */
    public static int extractDealIndexFromDealID(String dealID) {
        if (dealID != null && dealID.indexOf("-") >= 0) {
            try {
                return Integer.parseInt(dealID.substring(dealID.indexOf("-") + 1));
            } catch (Exception e) {
                ContextManager.getDuelMgr().getLogger().error("Failed to retrieve dealIndex from dealID=" + dealID, e);
            }
        }
        return -1;
    }

    /**
     * Extract dealIndex from dealID
     *
     * @param dealID
     * @return
     */
    public int _extractDealIndexFromDealID(String dealID) {
        return extractDealIndexFromDealID(dealID);
    }

    /**
     * Compute the result on a deal for player
     *
     * @param scoreAverage
     * @param scorePlayer
     * @return
     */
    public static double computeResultDealPlayer(int scoreAverage, int scorePlayer) {
        return Constantes.computeResultIMP((2 * scoreAverage - scorePlayer), scorePlayer);
    }

    @Override
    public GameMgr getGameMgr() {
        return gameMgr;
    }

    @Override
    public Class<? extends Game> getGameEntity() {
        return DuelGame.class;
    }

    @Override
    public String getGameCollectionName() {
        return "duel_game";
    }

    @Override
    public Class<DuelTournament> getTournamentEntity() {
        return DuelTournament.class;
    }

    @Override
    public Class<DuelTournamentPlayer> getTournamentPlayerEntity() {
        return DuelTournamentPlayer.class;
    }

    @Override
    public boolean finishTournament(Tournament tour) {
        return false;
    }

    @Override
    @PostConstruct
    public void init() {
        gameMgr = new GameMgr(this);
        duelMemoryMgr = new DuelMemoryMgr(this);
        dealGenerator = new DealGenerator(this, getStringResolvEnvVariableValue("generatorParamFile", null));
        threadPoolMatchMaking = Executors.newFixedThreadPool(5);
    }

    @Override
    public void startUp() {
        duelMemoryMgr.loadTournamentDuelNotFinish();

        initStatPeriod();

        // timer to finish tournament
        try {
            int finishPeriod = getConfigIntValue("finishPeriodSeconds", 300);
            int initDelay = getConfigIntValue("finishInitDelaySeconds", 60);
            schedulerFinishFuture = schedulerFinish.scheduleWithFixedDelay(finisherTask, initDelay, finishPeriod, TimeUnit.SECONDS);
            log.info("Schedule finisher - next run at " + Constantes.getStringDateForNextDelayScheduler(schedulerFinishFuture) + " - period (second)=" + finishPeriod);
        } catch (Exception e) {
            log.error("Exception to start finisher task", e);
        }
        // timer to purge request expired 清除请求的计时器已过期
        try {
            Calendar dateNext = Constantes.getNextDateForPeriod(Calendar.getInstance(), 10);
            schedulerPurgeRequestExpiredFuture = schedulerPurgeRequestExpired.scheduleAtFixedRate(purgeRequestExpiredTask, dateNext.getTimeInMillis() - System.currentTimeMillis(), (long) 5 * 60 * 1000, TimeUnit.MILLISECONDS);
            log.info("Schedule purgeRequest - next run at " + Constantes.getStringDateForNextDelayScheduler(schedulerPurgeRequestExpiredFuture) + " - period (minutes)=5");
        } catch (Exception e) {
            log.error("Exception to start purge request task", e);
        }
        // timer to send notif reminder duel
        try {
            Calendar dateNext = Calendar.getInstance();
            dateNext.add(Calendar.MINUTE, 30);
            int periodReminder = getReminderPeriodMinutes();
            schedulerNotifReminderFuture = schedulerNotifReminder.scheduleAtFixedRate(notifReminderTask, dateNext.getTimeInMillis() - System.currentTimeMillis(), (long) periodReminder * Constantes.TIMESTAMP_MINUTE, TimeUnit.MILLISECONDS);
            log.info("Schedule reminder - next run at " + Constantes.getStringDateForNextDelayScheduler(schedulerNotifReminderFuture) + " - period (minutes)=" + periodReminder);
        } catch (Exception e) {
            log.error("Exception to start reminder task", e);
        }

        // task thread for duel Argine
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
            // Enable task - triger every 5 mn
            JobDetail jobProcess = JobBuilder.newJob(DuelArgineProcessTask.class).withIdentity("duelArgineProcessTask", "Duel").build();
            CronTrigger triggerProcess = TriggerBuilder.newTrigger().withIdentity("triggerDuelArgineProcessTask", "Duel").withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?")).build();
            Date dateNextJobProcess = scheduler.scheduleJob(jobProcess, triggerProcess);
            log.warn("Sheduled for job=" + jobProcess.getKey() + " run at="+dateNextJobProcess+" - cron expression=" + triggerProcess.getCronExpression() + " - next fire=" + triggerProcess.getNextFireTime());
        } catch (Exception e) {
            log.error("Exception to start DuelArgine process task", e);
        }

        // task thread for duelStat period
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
            // Enable task - triger every first day of month
            JobDetail jobProcess = JobBuilder.newJob(DuelPeriodStatTask.class).withIdentity("duelPeriodStatTask", "Duel").build();
            CronTrigger triggerProcess = TriggerBuilder.newTrigger().withIdentity("triggerDuelPeriodStatTask", "Duel").withSchedule(CronScheduleBuilder.cronSchedule("0 1 0 1 * ?")).build();
            Date dateNextJobProcess = scheduler.scheduleJob(jobProcess, triggerProcess);
            log.warn("Sheduled for job=" + jobProcess.getKey() + " run at="+dateNextJobProcess+" - cron expression=" + triggerProcess.getCronExpression() + " - next fire=" + triggerProcess.getNextFireTime());
        } catch (Exception e) {
            log.error("Exception to start PeriodStat process task", e);
        }

        // task thread for duel notif ranking
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
            // Enable task - triger every first day of month
            JobDetail jobProcess = JobBuilder.newJob(DuelNotifRankingTask.class).withIdentity("duelNotifRankingTask", "Duel").build();
            CronTrigger triggerProcess = TriggerBuilder.newTrigger().withIdentity("triggerDuelNotifRankingTask", "Duel").withSchedule(CronScheduleBuilder.cronSchedule("0 0 1 1 * ?")).build();
            Date dateNextJobProcess = scheduler.scheduleJob(jobProcess, triggerProcess);
            log.warn("Sheduled for job=" + jobProcess.getKey() + " run at="+dateNextJobProcess+" - cron expression=" + triggerProcess.getCronExpression() + " - next fire=" + triggerProcess.getNextFireTime());
        } catch (Exception e) {
            log.error("Exception to start duel notif ranking process task", e);
        }

        // task thread for duel Argine notif ranking
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();
            // Enable task - triger every first day of month
            JobDetail jobProcess = JobBuilder.newJob(DuelArgineNotifRankingTask.class).withIdentity("duelArgineNotifRankingTask", "Duel").build();
            CronTrigger triggerProcess = TriggerBuilder.newTrigger().withIdentity("triggerDuelArgineNotifRankingTask", "Duel").withSchedule(CronScheduleBuilder.cronSchedule("0 0 1 1 * ?")).build();
            Date dateNextJobProcess = scheduler.scheduleJob(jobProcess, triggerProcess);
            log.warn("Sheduled for job=" + jobProcess.getKey() + " run at="+dateNextJobProcess+" - cron expression=" + triggerProcess.getCronExpression() + " - next fire=" + triggerProcess.getNextFireTime());
        } catch (Exception e) {
            log.error("Exception to start duel Argine notif ranking process task", e);
        }
    }

    public long getDateNextJobDuelNotifRanking() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerDuelNotifRankingTask", "Duel"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    public long getDateNextJobDuelArgineNotifRanking() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerDuelArgineNotifRankingTask", "Duel"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    @Override
    @PreDestroy
    public void destroy() {
        updateAllGameArgineInProgress(true);
        gameMgr.destroy();
        stopScheduler(schedulerFinish);
        stopScheduler(schedulerPurgeRequestExpired);
        stopScheduler(schedulerNotifReminder);
        int nbDuelInMemory = duelMemoryMgr.countDuelTournament();
        long ts = System.currentTimeMillis();
        int nbBackup = duelMemoryMgr.backupAllDuel(true);
        log.error("nbDuelInMemory=" + nbDuelInMemory + " - nbBackup=" + nbBackup + " - ts=" + (System.currentTimeMillis() - ts));
    }

    public class MatchMakingElement {
        public long playerID;
        public long requestDate;

        public MatchMakingElement(long playerID) {
            this.playerID = playerID;
            this.requestDate = System.currentTimeMillis();
        }

        public String toString() {
            return "playerID=" + playerID + " - requestDate=" + Constantes.timestamp2StringDateHour(requestDate);
        }
    }

    /**
     * Return next TS for enable JOB
     * @return
     */
    public long getDateNextJobDuelArgineProcess() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerDuelArgineProcessTask", "Duel"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    public String getStringDateNextPurgeRequestScheduler() {
        return Constantes.getStringDateForNextDelayScheduler(schedulerPurgeRequestExpiredFuture);
    }

    public String getStringDateNextReminderScheduler() {
        return Constantes.getStringDateForNextDelayScheduler(schedulerNotifReminderFuture);
    }

    public String getStringDateNextFinishScheduler() {
        return Constantes.getStringDateForNextDelayScheduler(schedulerFinishFuture);
    }

    @Override
    public void checkTournamentMode(boolean isReplay, long playerId) throws FBWSException {}


    /**
     * List playerDuel for playerID with status PLAYING
     * @param playerID
     * @return
     */
    public List<PlayerDuel> listPlayerDuelInProgressForPlayer(long playerID) {
        return playerDuelDAO.listInProgressForPlayer(playerID);
    }

    /**
     * List playerDuel waiting answer from playerID for request duel
     * @param playerID
     * @return
     */
    public List<PlayerDuel> listPlayerDuelRequestWaitingForPlayer(long playerID) {
        return playerDuelDAO.listRequestWaitingForPlayer(playerID);
    }

    /**
     * List playerDuel waiting answer from playerID for reset duel
     * @param playerID
     * @return
     */
    public List<PlayerDuel> listPlayerDuelResetWaitingForPlayer(long playerID) {
        return playerDuelDAO.listResetWaitingForPlayer(playerID);
    }

    /**
     * Return the playerDuel associated to this ID
     *
     * @param playerDuelID
     * @return
     */
    public PlayerDuel getPlayerDuel(long playerDuelID) {
        return playerDuelDAO.getPlayerDuelForID(playerDuelID);
    }

    /**
     * Count playerDuel status for request expired
     * @return
     */
    public int countDuelRequestExpired() {
        long dateCreationLimit = System.currentTimeMillis() - getDuelRequestDuration();
        return playerDuelDAO.countDuelRequestExpired(dateCreationLimit);
    }

    /**
     * Change playerDuel status for request expired
     * @return nb playerDuel updated
     */

    public int purgeDuelRequestExpired() {
        long dateCreationLimit = System.currentTimeMillis() - getDuelRequestDuration();
        return ContextManager.getPlayerDuelDAO().updateDuelRequestExpired(dateCreationLimit);
    }

    public PlayerDuel updatePlayerDuelOnFinish(DuelMemTournament duelMemTournament) {
        if (duelMemTournament != null) {
            PlayerDuel playerDuel = getPlayerDuel(duelMemTournament.playerDuelID);
            if (playerDuel != null) {
                playerDuel.incrementNbWinForPlayer(duelMemTournament.getWinner());
                playerDuel.setDateLastDuel(System.currentTimeMillis());
                playerDuel.setStatus(Constantes.PLAYER_DUEL_STATUS_NONE);
                playerDuelDAO.updatePlayerDuel(playerDuel);
            }

            if (duelMemTournament.isDuelWithArgine()) {
                updateDuelArgineStat(duelMemTournament);
            }

            // update duel stats for each player
            updateDuelStat(duelMemTournament.player1ID, duelMemTournament.getWinner(), duelMemTournament.isDuelWithArgine());
            updateDuelStat(duelMemTournament.player2ID, duelMemTournament.getWinner(), duelMemTournament.isDuelWithArgine());

            Double resultPlayer1 = duelMemTournament.getResultPlayer(duelMemTournament.player1ID);
            Double resultPlayer2 = duelMemTournament.getResultPlayer(duelMemTournament.player2ID);
            updateBestScoreDuelStat(duelMemTournament.player1ID, duelMemTournament.player2ID, resultPlayer1 - resultPlayer2);
            if(duelMemTournament.player2ID != -2){
                updateBestScoreDuelStat(duelMemTournament.player2ID, duelMemTournament.player1ID, resultPlayer2 - resultPlayer1);
            }

            return playerDuel;
        }
        return null;
    }

    public DuelStat getDuelStat(long playerID) {
        return mongoTemplate.findById(playerID, DuelStat.class);
    }

    /**
     * Update duel stat for playerID. duelWinner is the winner. 0 if no winner.
     * If no stat exists for this player, a new one is created using the PlayerDuelStat from MySQL
     * @param playerID
     * @param duelWinner
     * @param duelWithArgine
     * @return
     */
    public void updateDuelStat(long playerID, long duelWinner, boolean duelWithArgine) {
        synchronized (lockDuelStat.getLock(""+playerID)) {
            try {
                DuelStat duelStat = getDuelStat(playerID);
                if (duelStat == null) {
                    duelStat = new DuelStat(playerID);
                    PlayerDuelStat playerDuelStat = playerMgr.getDuelStat(playerID);
                    if (playerDuelStat != null && playerDuelStat.nbPlayed > 0) {
                        duelStat.setData(playerDuelStat, getDuelArgineStat(playerID));
                        mongoTemplate.insert(duelStat);
                    }
                } else {
                    duelStat.update(duelWinner, duelWithArgine, statCurrentPeriodID, statPreviousPeriodID);
                    mongoTemplate.save(duelStat);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Update duelStat for playerID="+playerID);
                }
            } catch (Exception e) {
                log.error("Failed to update duel stat - playerID="+playerID+" - duelWinner="+duelWinner, e);
            }
        }
    }

    public void updateBestScoreDuelStat(long playerId, long rivalId, Double score){
        synchronized ((lockDuelStat.getLock("" + playerId))){
            DuelStat duelStat = getDuelStat(playerId);
            if(duelStat.scoring == null){ duelStat.scoring = new DuelStatScoring(); }
            duelStat.scoring.addBestScore(rivalId, score);
            mongoTemplate.save(duelStat);
        }
    }

    public void synchronizeDuelStat(long playerID) {
        synchronized (lockDuelStat.getLock(""+playerID)) {
            PlayerDuelStat playerDuelStat = playerMgr.getDuelStat(playerID);
            if (playerDuelStat != null && playerDuelStat.nbPlayed > 0) {
                try {
                    DuelStat duelStat = getDuelStat(playerID);
                    if (duelStat == null) {
                        duelStat = new DuelStat(playerID);
                        duelStat.setData(playerDuelStat, getDuelArgineStat(playerID));
                        mongoTemplate.insert(duelStat);
                    } else {
                        if (duelStat.synchronizeData(playerDuelStat, getDuelArgineStat(playerID))) {
                            mongoTemplate.save(duelStat);
                        }
                    }
                } catch (Exception e) {
                        log.error("Failed to synchronize duel stat - playerID="+playerID, e);
                }
            }
        }
    }

    /**
     * Return an object to lock a playerDuel
     *
     * @param playerDuelID
     * @return
     */
    public Object getLockForPlayerDuel(long playerDuelID) {
        return lockDuelPlayer.getLock("" + playerDuelID);
    }

    /**
     * Return an object to lock creation of new playerDuel for these 2 players
     *
     * @param player1
     * @param player2
     * @return
     */
    public Object getLockForNewPlayerDuel(long player1, long player2) {
        String plKey = "duel-";
        if (player1 < player2) {
            plKey += player1 + "-" + player2;
        } else {
            plKey += player2 + "-" + player1;
        }
        return lockDuelPlayer.getLock(plKey);
    }

    @Override
    public boolean distributionExists(String cards) {
        return mongoTemplate.findOne(Query.query(Criteria.where("deals.cards").is(cards)), DuelTournament.class) != null;
    }

    /**
     * Read string value for parameter in name (tournament.DUEL.paramName) in config file
     *
     * @param paramName
     * @param defaultValue
     * @return
     */
    public String getConfigStringValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringValue("tournament.DUEL." + paramName, defaultValue);
    }

    public String getStringResolvEnvVariableValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringResolvEnvVariableValue("tournament.DUEL." + paramName, defaultValue);
    }

    public long computeDateFinish(DuelTournamentPlayer duelTournamentPlayer) {
        if (duelTournamentPlayer != null) {
            if (duelTournamentPlayer.getFinishDate() > 0) {
                return duelTournamentPlayer.getFinishDate();
            }
            return duelTournamentPlayer.getCreationDate() + getDuelDuration();
        }
        return -1;
    }

    /**
     * Read int value for parameter in name (tournament.DUEL.paramName) in config file
     *
     * @param paramName
     * @param defaultValue
     * @return
     */
    @Override
    public int getConfigIntValue(String paramName, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("tournament.DUEL." + paramName, defaultValue);
    }

    /**
     * Read Long value for parameter in name (tournament.DUEL.paramName) in config file
     *
     * @param paramName
     * @param defaultValue
     * @return
     */
    public long getConfigLongValue(String paramName, long defaultValue) {
        return FBConfiguration.getInstance().getLongValue("tournament.DUEL." + paramName, defaultValue);
    }

    /**
     * Task to remove all expired request for duel
     *
     * @author pserent
     */
    public class PurgeRequestExpiredTask implements Runnable {
        public boolean running = false;

        @Override
        public void run() {
            if (!running) {
                running = true;
                try {
                    if (getConfigIntValue("removeRequestExpired", 1) == 1) {
                        if (countDuelRequestExpired() > 0) {
                            int temp = purgeDuelRequestExpired();
                            log.debug("Nb playerDuel updated for request expired = " + temp);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to purgeDuelRequestExpired", e);
                    ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to purgeDuelRequestExpired", e.getMessage(), null);
                }
                running = false;
            } else {
                log.error("Process purgeDuel already running");
                ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for purgeDuelRequestExpired", null, null);
            }
        }
    }

    /**
     * Task to send notif reminder for duel soon expired
     *
     * @author pserent
     */
    public class NotifReminderTask implements Runnable {
        public boolean running = false;

        @Override
        public void run() {
            if (!running) {
                running = true;
                try {
                    if (getConfigIntValue("reminderTaskEnable", 0) == 1) {
                        int temp = processReminder(System.currentTimeMillis());
                        if (log.isDebugEnabled()) {
                            log.debug("Nb duel reminder process=" + temp);
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

    /**
     * Run process for match making
     */
    public class DuelMatchMakingThread implements Runnable {
        long playerID;

        public DuelMatchMakingThread(long playerID) {
            this.playerID = playerID;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
                processMatchMakingForPlayer(playerID);
            } catch (Exception e) {
                log.error("Exception for playerID=" + playerID, e);
            }
        }
    }

    /**
     * Task to finish duel
     */
    public class FinisherTask implements Runnable {
        public boolean running = false;

        @Override
        public void run() {
            if (!running) {
                running = true;
                try {
                    finishExpiredDuels();
                } catch (Exception e) {
                    log.error("Exception to finshExpiredDuels", e);
                    ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to finshExpiredDuels", e.getMessage(), null);
                }
                running = false;
            } else {
                log.error("Process finish task already running");
                ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for finshExpiredDuels", null, null);
            }
        }
    }

    public void finishExpiredDuels() {
        List<DuelTournamentPlayer> listDuelExpired = listDuelTournamentPlayerExpired(getConfigIntValue("finishNbMax", 5));
        for (DuelTournamentPlayer duel : listDuelExpired) {
            try {
                finishDuel(duel);
                // send notif for player1
                if (duel.getPlayer1ID() != Constantes.PLAYER_ARGINE_ID) {
                    MessageNotif notif1 = notifMgr.createNotifDuelFinishClose(duel, duel.getPlayer1ID());
                    if (notif1 != null) {
                        FBSession session = presenceMgr.getSessionForPlayerID(duel.getPlayer1ID());
                        if (session != null) {
                            // push event for notif
                            session.pushEvent(notifMgr.buildEvent(notif1, duel.getPlayer1ID()));
                            // push event to update duel history
                            session.pushEvent(messageMgr.buildEventDuelUpdate(session.getPlayer(), createDuelHistory(duel.getPlayerDuelID(), session.getPlayer().getID(), null, true), buildDuelResult(duel, session.getPlayer().getID())));
                        }
                    }
                }
                // send notif for player2
                if (duel.getPlayer2ID() != Constantes.PLAYER_ARGINE_ID) {
                    MessageNotif notif2 = notifMgr.createNotifDuelFinishClose(duel, duel.getPlayer2ID());
                    if (notif2 != null) {
                        FBSession session = presenceMgr.getSessionForPlayerID(duel.getPlayer2ID());
                        if (session != null) {
                            // push event for notif
                            session.pushEvent(notifMgr.buildEvent(notif2, duel.getPlayer2ID()));
                            // push event to update duel history
                            session.pushEvent(messageMgr.buildEventDuelUpdate(session.getPlayer(), createDuelHistory(duel.getPlayerDuelID(), session.getPlayer().getID(), null, true), buildDuelResult(duel, session.getPlayer().getID())));
                        }
                    }
                }

                // send notif to commons friend
                if (notifMgr.isNotifEnable() && !duel.isDuelWithArgine()) {
                    Player player1 = playerMgr.getPlayer(duel.getPlayer1ID());
                    Player player2 = playerMgr.getPlayer(duel.getPlayer2ID());
                    // check the flag notification friend duel is set for the two players
                    if (player1 != null && player2 != null) {
                        List<Player> listCommonFriend = playerMgr.listCommonFriendForPlayers(duel.getPlayer1ID(), duel.getPlayer2ID());
                        if (listCommonFriend != null && listCommonFriend.size() > 0) {
                            for (Player pFriend : listCommonFriend) {
                                MessageNotif notifFriend = notifMgr.createNotifFriendDuelFinish(pFriend, duel);
                                if (notifFriend != null) {
                                    FBSession sessionFriend = presenceMgr.getSessionForPlayerID(pFriend.getID());
                                    if (sessionFriend != null) {
                                        // push event for notif
                                        sessionFriend.pushEvent(notifMgr.buildEvent(notifFriend, pFriend));
                                    }
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Exception to finish tournamentDuel=" + duel, e);
            }
        }
    }

    /**
     * Return the duration of a duel
     *
     * @return
     */
    public long getDuelDuration() {
        return ((long) getConfigIntValue("duelDurationInMinutes", 4320) * 60 * 1000);
    }

    /**
     * Return the duration of a request for duel
     *
     * @return
     */
    public long getDuelRequestDuration() {
        return ((long) getConfigIntValue("requestDurationInMinutes", 1440) * 60 * 1000);
    }

    /**
     * List duel not finish and expired
     *
     * @param nbMax
     * @return
     */
    public List<DuelTournamentPlayer> listDuelTournamentPlayerExpired(int nbMax) {
        long dateCreationLimit = System.currentTimeMillis() - getDuelDuration();
        Query q = new Query(Criteria.where("creationDate").lt(dateCreationLimit).andOperator(Criteria.where("finishDate").is(0)));
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return mongoTemplate.find(q, DuelTournamentPlayer.class);
    }


    public boolean finishDuel(String duelTournamentPlayerID) {
        return finishDuel(getTournamentPlayer(duelTournamentPlayerID));
    }

    /**
     * Finish duel between 2 players
     * @param duelTournamentPlayer
     */
    public boolean finishDuel(DuelTournamentPlayer duelTournamentPlayer) {
        if (duelTournamentPlayer != null) {
            synchronized (getLockForPlayerDuel(duelTournamentPlayer.getPlayerDuelID())) {
                DuelTournamentPlayer duelTournamentPlayerToUpdate = getTournamentPlayer(duelTournamentPlayer.getIDStr());
                if (duelTournamentPlayerToUpdate != null && !duelTournamentPlayerToUpdate.isFinished()) {
                    // finish duel in memory
                    DuelMemTournament memTournament = duelMemoryMgr.finishDuel(duelTournamentPlayerToUpdate);
                    if (memTournament != null) {
                        // update tournamentDuel fields
                        duelTournamentPlayerToUpdate.setFinishDate(System.currentTimeMillis());
                        duelTournamentPlayerToUpdate.setResultPlayer1(memTournament.getResultPlayer(duelTournamentPlayerToUpdate.getPlayer1ID()));
                        duelTournamentPlayerToUpdate.setResultPlayer2(memTournament.getResultPlayer(duelTournamentPlayerToUpdate.getPlayer2ID()));

                        // copy fields to original object
                        duelTournamentPlayer.setFinishDate(duelTournamentPlayerToUpdate.getFinishDate());
                        duelTournamentPlayer.setResultPlayer1(duelTournamentPlayerToUpdate.getResultPlayer1());
                        duelTournamentPlayer.setResultPlayer2(duelTournamentPlayerToUpdate.getResultPlayer2());

                        // update objects
                        mongoTemplate.save(duelTournamentPlayerToUpdate);
                        updatePlayerDuelOnFinish(memTournament);

                        // remove duelArgineInProgress
                        removeDuelArgineInProgress(memTournament.tourID);

                        if (log.isDebugEnabled()) {
                            log.debug("Finish duelTournamentPlayer="+duelTournamentPlayerToUpdate);
                        }
                        Player player1 = playerMgr.getPlayer(duelTournamentPlayerToUpdate.getPlayer1ID());
                        Player player2 = playerMgr.getPlayer(duelTournamentPlayerToUpdate.getPlayer2ID());
                        // send push
                        if (player1 != null && player2 != null) {
                            if (!player1.isArgine()) {
                                FBSession sessionPla1 = presenceMgr.getSessionForPlayerID(player1.getID());
                                if (sessionPla1 != null) {
                                    // remove duel in progress for player sessions
                                    sessionPla1.removeDuelInProgress(duelTournamentPlayerToUpdate.getPlayerDuelID());
                                }
                            }
                            if (!player2.isArgine()) {
                                FBSession sessionPla2 = presenceMgr.getSessionForPlayerID(player2.getID());
                                if (sessionPla2 != null) {
                                    // remove duel in progress for player sessions
                                    sessionPla2.removeDuelInProgress(duelTournamentPlayerToUpdate.getPlayerDuelID());
                                }
                            }
                        }
                        return true;
                    } else {
                        log.error("No DuelMemTournament found in memory for duelTournamentPlayer="+duelTournamentPlayerToUpdate);
                    }
                } else {
                    log.error("DuelTournamentPlayer is already finished - duelTournamentPlayer="+duelTournamentPlayerToUpdate);
                }
            }
        } else {
            log.error("duelTournamentPlayer is null");
        }
        return false;
    }

    /**
     * Generate new tournament
     *
     * @param argineDuel
     * @return
     */
    public DuelTournament generateTournament(boolean argineDuel) {
        Calendar calEnd = Calendar.getInstance();
        calEnd.set(Calendar.MILLISECOND, 0);
        calEnd.set(Calendar.SECOND, 0);

        int nbDeal = getConfigIntValue("nbDeal", 5);

        int durationMinute = (int)((getDuelDuration() + 10*Constantes.TIMESTAMP_MINUTE)/(1000*60));
        if (argineDuel) {
            durationMinute += (int)(getConfigIntValue("tourDurationExtraNbMinutesForArgine",24*60)*Constantes.TIMESTAMP_MINUTE/1000);
        }
        calEnd.add(Calendar.MINUTE, durationMinute);

        try {
            Random r = new Random(System.nanoTime());
            int offsetIndex = r.nextInt(20);
            // generate distribution
            DealGenerator.DealGenerate[] tabDeal = dealGenerator.generateDeal(nbDeal, offsetIndex);
            if (tabDeal != null) {
                DuelTournament t = new DuelTournament();
                t.setCreationDate(System.currentTimeMillis());
                t.setEndDate(calEnd.getTimeInMillis());
                t.setDealGenerate(tabDeal);
                t.setNbCreditsPlayDeal(getConfigIntValue("nbCreditPlayDeal", 1));
                t.setCreationDateISO(new Date());
                t.setArgineDuel(argineDuel);
                mongoTemplate.insert(t);
                if (log.isDebugEnabled()) {
                    log.debug("Generate new tournament with success - tour=" + t);
                }
                return t;
            }
        } catch (Exception e) {
            log.error("Exception to generate new tournament", e);
        }
        return null;
    }

    /**
     * Get (or create) tournament & create tournamentPlayer for duel
     * @param duel
     * @return duelTournamentPlayer associated
     */
    public DuelTournamentPlayer createTournamentPlayerForDuel(PlayerDuel duel, String origin) {
        if (duel != null) {
            // find a tournament already created and not played by this playerDuel
            DuelTournament tour = generateTournament(false);
            if (tour != null) {
                // lock tournament and check players not already played this tournament
                synchronized (lockCreateTournamentDuel.getLock(tour.getIDStr())) {
                    boolean bGenerateNewTour = false;
                    // check players not with the same tournament on other duel (two create tournament for duel on the same time with two different friends A-B et A-C)
                    if (!bGenerateNewTour) {
                        if (getTournamentPlayer(tour.getIDStr(), duel.getPlayer1().getID()) != null ||
                                getTournamentPlayer(tour.getIDStr(), duel.getPlayer2().getID()) != null) {
                            bGenerateNewTour = true;
                        }
                    }

                    // tour already played by a player => generate a new to be sure !
                    if (bGenerateNewTour) {
                        log.error("Need to generate new tour ! player1=" + duel.getPlayer1() + " - player2=" + duel.getPlayer2());
                        tour = generateTournament(false);
                    }
                    // now tour is OK => create DuelTournamentPlayer
                    DuelTournamentPlayer dtp = new DuelTournamentPlayer();
                    dtp.setCreationDate(System.currentTimeMillis());
                    dtp.setPlayerDuelID(duel.getID());
                    dtp.setPlayer1ID(duel.getPlayer1().getID());
                    dtp.setPlayer2ID(duel.getPlayer2().getID());
                    dtp.setTournament(tour);
                    dtp.setCreationDateISO(new Date());
                    dtp.setOrigin(origin);
                    mongoTemplate.insert(dtp);
                    return dtp;
                }
            } else {
                log.error("Failed to generate tournament for PlayerDuel=" + duel);
            }
        } else {
            log.error("Duel is null !");
        }
        return null;
    }

    /**
     * Get (or create) tournament & create tournamentPlayer for duel
     * @param duel
     * @return duelTournamentPlayer associated
     */
    public DuelTournamentPlayer createTournamentPlayerForArgineDuel(PlayerDuel duel, long playerAskID) {
        if (duel != null && duel.isDuelArgine()) {
            DuelTournament tour = null;
            // get tournament already played by Argine
            DuelArgineInProgress duelArgineInProgress = findAndReserveDuelArgineInProgress(playerAskID);
            if (duelArgineInProgress != null) {
                tour = getTournament(duelArgineInProgress.tourID);
            }
            // no tour found => generate a new one
            if (tour == null) {
                tour = generateTournament(true);
            }
            if (tour != null) {
                DuelTournamentPlayer dtp = new DuelTournamentPlayer();
                dtp.setCreationDate(System.currentTimeMillis());
                dtp.setPlayerDuelID(duel.getID());
                dtp.setPlayer1ID(duel.getPlayer1().getID());
                dtp.setPlayer2ID(duel.getPlayer2().getID());
                dtp.setTournament(tour);
                dtp.setCreationDateISO(new Date());
                dtp.setOrigin(DUEL_ORIGIN_ARGINE);
                mongoTemplate.insert(dtp);
                return dtp;
            } else {
                log.error("Failed to generate tournament for PlayerDuel=" + duel);
            }
        } else {
            log.error("Duel is null !");
        }
        return null;
    }

    public DuelTournamentPlayer getTournamentPlayer(String tourID, long playerID) {
        Criteria cPlayer = new Criteria().orOperator(Criteria.where("player1ID").is(playerID), Criteria.where("player2ID").is(playerID));
        Criteria cTour = Criteria.where("tournament.$id").is(new ObjectId(tourID));
        Query q = new Query().addCriteria(new Criteria().andOperator(cTour, cPlayer));
        return mongoTemplate.findOne(q, DuelTournamentPlayer.class);
    }

    public DuelTournamentPlayer getTournamentPlayer(String tournamentPlayerID) {
        return mongoTemplate.findById(new ObjectId(tournamentPlayerID), DuelTournamentPlayer.class);
    }

    /**
     * Create duelHistory for playerDuel and playerAsk. If mdr is null && checkIfNull, then try to find MemDuelResult from memory
     *
     * @param pd
     * @param playerAsk
     * @param memTournament
     * @param checkIfNull
     * @return
     */
    public WSDuelHistory createDuelHistory(PlayerDuel pd, long playerAsk, DuelMemTournament memTournament, boolean checkIfNull) {
        if (pd != null) {
            WSDuelHistory temp = new WSDuelHistory();
            temp.player1 = WSGamePlayer.createGamePlayerHuman(pd.getPlayer1ForAsk(playerAsk), playerAsk);
            temp.player1.connected = true;
            temp.player2 = WSGamePlayer.createGamePlayerHuman(pd.getPlayer2ForAsk(playerAsk), playerAsk);
            temp.player2.connected = presenceMgr.isSessionForPlayerID(temp.player2.playerID);
            temp.status = pd.getStatusForAsk(playerAsk);
            temp.setPlayerDuelID(pd.getID());
            temp.nbWinPlayer1 = pd.getNbWinForPlayer1(playerAsk);
            temp.nbWinPlayer2 = pd.getNbWinForPlayer2(playerAsk);
            temp.nbDraw = pd.getNbDuelDraw();
            temp.dateLastDuel = pd.getDateLastDuel();
            temp.setTourID(0);
            if (pd.isRequest()) {
                temp.expirationDate = pd.getDateCreation() + getDuelRequestDuration();
            }
            if (memTournament == null && checkIfNull) {
                memTournament = duelMemoryMgr.getDuelMemTournamentForPlayers(pd.getPlayer1().getID(), pd.getPlayer2().getID());
            }
            if (memTournament != null) {
                temp.tourIDstr = memTournament.tourID;
                if (memTournament.isAllPlayed()) {
                    temp.status = Constantes.PLAYER_DUEL_STATUS_NONE;
                }
                else if (memTournament.isAllPlayedForPlayer(playerAsk)) {
                    temp.status = Constantes.PLAYER_DUEL_STATUS_PLAYED;
                }
                else {
                    temp.status = Constantes.PLAYER_DUEL_STATUS_PLAYING;
                }
                temp.expirationDate = memTournament.dateFinish;
            }
            return temp;
        }
        return null;
    }

    /**
     * Create duelHistory for playerDuel and playerAsk. If mdr is null && checkIfNull, then try to find MemDuelResult from memory
     *
     * @param playerDuelID
     * @param playerAsk
     * @param memTournament
     * @param checkIfNull
     * @return
     */
    public WSDuelHistory createDuelHistory(long playerDuelID, long playerAsk, DuelMemTournament memTournament, boolean checkIfNull) {
        return createDuelHistory(getPlayerDuel(playerDuelID), playerAsk, memTournament, checkIfNull);
    }

    /**
     * Create a duel between two players
     * @param plaReq
     * @param player
     */
    public PlayerDuel createDuel(Player plaReq, Player player) {
        PlayerDuel pd = null;
        // create or update player duel
        pd = playerDuelDAO.getPlayerDuelBetweenPlayer(plaReq.getID(), player.getID());
        if (pd == null) {
            // no duel existing => synchronise of new player duel
            synchronized (getLockForNewPlayerDuel(plaReq.getID(), player.getID())) {
                // no player duel existing for these 2 players ... create it
                pd = new PlayerDuel();
                pd.setPlayer1(plaReq);
                pd.setPlayer2(player);
                pd.requestFromPlayer(plaReq.getID());
                pd.setDateCreation(System.currentTimeMillis());
                pd.setStatus(Constantes.PLAYER_DUEL_STATUS_PLAYING);
                try {
                    playerDuelDAO.persistPlayerDuel(pd);
                } catch (Exception e) {
                    log.error("Exception to persist playerDuel="+pd, e);
                    pd = null;
                }
            }
        } else {
            // player duel already exist
            if (pd.isPlaying()) {
                // nothing is done
                log.error("A player duel already exist and status is playing ! pd="+pd);
            } else {
                pd.requestFromPlayer(plaReq.getID());
                pd.setDateCreation(System.currentTimeMillis());
                pd.setStatus(Constantes.PLAYER_DUEL_STATUS_PLAYING);
                playerDuelDAO.updatePlayerDuel(pd);
            }
        }

        // create tournament duel
        if (pd != null && pd.isPlaying()) {
            DuelTournamentPlayer duelTournamentPlayer = createTournamentPlayerForDuel(pd, DUEL_ORIGIN_REQUEST);
            if (duelTournamentPlayer == null) {
                pd.setStatus(Constantes.PLAYER_DUEL_STATUS_NONE);
                playerDuelDAO.updatePlayerDuel(pd);
                log.error("Failed to create tournament for playerDuel="+pd);
            } else {
                // add duel in memory
                DuelMemTournament dmt = duelMemoryMgr.addTournamentPlayer(duelTournamentPlayer);

                // add notif
                MessageNotif notif = notifMgr.createNotifDuelAnswerOK(player, plaReq, getDuelDuration(), duelTournamentPlayer.getTournament().getIDStr());
                // push event for partner
                FBSession sessionPartner = presenceMgr.getSessionForPlayerID(plaReq.getID());
                if (sessionPartner != null) {
                    sessionPartner.addDuelInProgress(pd.getID());
                    sessionPartner.pushEvent(messageMgr.buildEventDuelRequestAnswer(player, plaReq, true, createDuelHistory(pd, plaReq.getID(), dmt, true)));
                    if (notif != null) {
                        sessionPartner.pushEvent(notifMgr.buildEvent(notif, plaReq));
                    }
                }
            }
        }
        return pd;
    }

    /**
     * Create a duel between two players for match making.
     *
     * @param p1
     * @param p2
     */
    public PlayerDuel createDuelMatchMaking(Player p1, Player p2) {
        synchronized (getLockForNewPlayerDuel(p1.getID(), p2.getID())) {
            if (log.isDebugEnabled()) {
                log.debug("Create duel for match making for p1=[" + p1 + "] - p2=[" + p2 + "]");
            }
            PlayerDuel pd = playerDuelDAO.getPlayerDuelBetweenPlayer(p1.getID(), p2.getID());
            if (pd == null) {
                // no duel existing => synchronise of new player duel
                pd = new PlayerDuel();
                pd.setPlayer1(p1);
                pd.setPlayer2(p2);
                pd.requestFromPlayer(p1.getID());
                pd.setDateCreation(System.currentTimeMillis());
                pd.setStatus(Constantes.PLAYER_DUEL_STATUS_PLAYING);
                try {
                    playerDuelDAO.persistPlayerDuel(pd);
                } catch (Exception e) {
                    log.error("Exception to persist playerDuel=" + pd, e);
                    pd = null;
                }
            } else {
                // player duel already exist
                if (pd.isPlaying()) {
                    // nothing is done
                    log.error("A player duel already exist and status is playing ! pd=" + pd);
                } else {
                    pd.requestFromPlayer(p1.getID());
                    pd.setDateCreation(System.currentTimeMillis());
                    pd.setStatus(Constantes.PLAYER_DUEL_STATUS_PLAYING);
                    playerDuelDAO.updatePlayerDuel(pd);
                }
            }

            // find (or create) a tournamentDuel & create duelTournamentPlayer associed for this duel
            if (pd != null && pd.isPlaying()) {
                DuelTournamentPlayer tournamentPlayer = createTournamentPlayerForDuel(pd, DUEL_ORIGIN_MACTHMAKING);
                if (tournamentPlayer == null) {
                    pd.setStatus(Constantes.PLAYER_DUEL_STATUS_NONE);
                    playerDuelDAO.updatePlayerDuel(pd);
                    log.error("Failed to create tournamentPlayer for playerDuel=" + pd);
                } else {
                    // insert tournamentPlayer in memory
                    DuelMemTournament dmt = duelMemoryMgr.addTournamentPlayer(tournamentPlayer);
                    // add notif
                    MessageNotif notifP1 = notifMgr.createNotifDuelMatchMaking(p2, p1, getDuelDuration());
                    MessageNotif notifP2 = notifMgr.createNotifDuelMatchMaking(p1, p2, getDuelDuration());
                    // push event for P1
                    FBSession sessionP1 = presenceMgr.getSessionForPlayerID(p1.getID());
                    if (sessionP1 != null) {
                        sessionP1.addDuelInProgress(pd.getID());
                        sessionP1.pushEvent(messageMgr.buildEventDuelMatchMaking(p1, createDuelHistory(pd, p1.getID(), dmt, true)));
                        if (notifP1 != null) {
                            sessionP1.pushEvent(notifMgr.buildEvent(notifP1, p1));
                        }
                    }
                    // push event for P2
                    FBSession sessionP2 = presenceMgr.getSessionForPlayerID(p2.getID());
                    if (sessionP2 != null) {
                        sessionP2.addDuelInProgress(pd.getID());
                        sessionP2.pushEvent(messageMgr.buildEventDuelMatchMaking(p2, createDuelHistory(pd, p2.getID(), dmt, true)));
                        if (notifP2 != null) {
                            sessionP2.pushEvent(notifMgr.buildEvent(notifP2, p2));
                        }
                    }
                }
            } else {
                log.error("PlayerDuel object not valid (null or status not playing!) pd=" + pd);
            }
            return pd;
        }
    }

    /**
     * Change reset request value. If no playerDuel exist, create a new one. Use reset field to remove duel or player from the duel list
     * @param playerRequest
     * @param playerPartner
     * @throws FBWSException
     */
    public void setResetStatusDuelBetweenPlayer(Player playerRequest, Player playerPartner) throws FBWSException {
        if (playerRequest != null && playerPartner != null) {
            synchronized (getLockForNewPlayerDuel(playerRequest.getID(), playerPartner.getID())) {
                PlayerDuel pd = playerDuelDAO.getPlayerDuelBetweenPlayer(playerRequest.getID(), playerPartner.getID());
                if (pd != null) {
                    if (pd.isPlaying()) {
                        DuelMemTournament duelMemTournament = duelMemoryMgr.getDuelMemTournamentForPlayers(pd.getPlayer1().getID(), pd.getPlayer2().getID());
                        if (duelMemTournament != null) {
                            if (duelMemTournament.isExpired()) {
                                // try to finish it !
                                DuelTournamentPlayer tournamentPlayer = getTournamentPlayer(duelMemTournament.tourPlayerID);
                                if (tournamentPlayer != null) {
                                    finishDuel(tournamentPlayer);
                                } else {
                                    // remove from memory
                                    duelMemoryMgr.removeMemTournament(duelMemTournament.tourPlayerID);
                                }
                            } else {
                                //not expired
                                log.error("Player duel status is already playing pd=" + pd);
                                throw new FBWSException(FBExceptionType.PLAYER_DUEL_STATUS_NOT_VALID);
                            }
                        } else {
                            log.error("No duel result found in memory for playerDuel=" + pd);
                            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                        }
                    }
                    if (pd.getStatus() != Constantes.PLAYER_DUEL_STATUS_NONE) {
                        log.error("Player duel is not NONE (0) - pd="+pd);
                        throw new FBWSException(FBExceptionType.PLAYER_DUEL_STATUS_NOT_VALID);
                    }
                    // player duel already existing => synchronize on it
                    synchronized (getLockForPlayerDuel(pd.getID())) {
                        // reload player duel to be sure to have the last
                        pd = playerDuelDAO.getPlayerDuelForID(pd.getID());

                        // check request already existing
                        if (!pd.isRequestExpired()) {
                            log.error("Player duel status is request pd=" + pd);
                            throw new FBWSException(FBExceptionType.PLAYER_DUEL_STATUS_NOT_VALID);
                        }
                        pd.resetFromPlayer(playerRequest.getID());
                        try {
                            playerDuelDAO.updatePlayerDuel(pd);
                        } catch (Exception e) {
                            log.error("Exception to update playerDuel=" + pd, e);
                            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                        }
                    }
                } else {
                    // no player duel existing for these 2 players ... create it
                    pd = new PlayerDuel();
                    pd.setPlayer1(playerRequest);
                    pd.setPlayer2(playerPartner);
                    pd.resetFromPlayer(playerRequest.getID());
                    pd.setDateCreation(System.currentTimeMillis());
                    try {
                        playerDuelDAO.persistPlayerDuel(pd);
                    } catch (Exception e) {
                        log.error("Exception to persist playerDuel=" + pd, e);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                }
            }
        } else {
            log.error("Param players null - playerRequest=" + playerRequest + " - playerPartner=" + playerPartner);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Request duel between 2 players. Create a new duel or update status of existing duel
     *
     * @param playerRequest
     * @param playerPartner
     * @throws FBWSException
     */
    public WSDuelHistory requestDuelBetweenPlayer(Player playerRequest, Player playerPartner) throws FBWSException {
        if (playerRequest != null && playerPartner != null) {
            PlayerLink playerLink = playerMgr.getLinkBetweenPlayer(playerRequest.getID(), playerPartner.getID());
            // check partner choose to receive only request duel from friend
            if (playerPartner.hasFriendFlag(Constantes.FRIEND_MASK_DUEL_ONLY_FRIEND)) {
                // check sender & recipient are friend
                if (playerLink == null || !playerLink.isLinkFriend()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Players are not friend and recipient only want duel from friend - playerRequest=" + playerRequest + " - playerPartner=" + playerPartner);
                    }
                    throw new FBWSException(FBExceptionType.PLAYER_DUEL_ONLY_FRIEND);
                }
            }
            // check partner choose to receive only request duel from friend
            // check recipient has not blocked this player
            if (playerLink != null) {
                if (playerLink.hasBlocked(playerPartner.getID())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Recipient has blocked this player playerLink=" + playerLink + " - playerRequest=" + playerRequest + " - playerPartner=" + playerPartner);
                    }
                    throw new FBWSException(FBExceptionType.PLAYER_LINK_BLOCKED);
                }
            }
            FBSession sessionPartner = presenceMgr.getSessionForPlayerID(playerPartner.getID());
            PlayerDuel pd = playerDuelDAO.getPlayerDuelBetweenPlayer(playerRequest.getID(), playerPartner.getID());
            if (pd == null) {
                // no duel existing => synchronise of new player duel
                synchronized (getLockForNewPlayerDuel(playerRequest.getID(), playerPartner.getID())) {
                    // no player duel existing for these 2 players ... create it
                    pd = new PlayerDuel();
                    pd.setPlayer1(playerRequest);
                    pd.setPlayer2(playerPartner);
                    pd.requestFromPlayer(playerRequest.getID());
                    pd.setDateCreation(System.currentTimeMillis());
                    try {
                        playerDuelDAO.persistPlayerDuel(pd);
                    } catch (Exception e) {
                        log.error("Exception to persist playerDuel=" + pd, e);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                }
            } else {
                if (pd.isPlaying()) {
                    DuelMemTournament duelMemTournament = duelMemoryMgr.getDuelMemTournamentForPlayers(pd.getPlayer1().getID(), pd.getPlayer2().getID());
                    if (duelMemTournament != null) {
                        if (duelMemTournament.isExpired()) {
                            // try to finish it !
                            DuelTournamentPlayer tournamentPlayer = getTournamentPlayer(duelMemTournament.tourPlayerID);
                            if (tournamentPlayer != null) {
                                finishDuel(tournamentPlayer);
                            } else {
                                // remove from memory
                                duelMemoryMgr.removeMemTournament(duelMemTournament.tourPlayerID);
                            }
                        } else {
                            //not expired
                            log.error("Player duel status is already playing pd=" + pd);
                            throw new FBWSException(FBExceptionType.PLAYER_DUEL_STATUS_NOT_VALID);
                        }
                    } else {
                        // no duel found in memory ... try to find last tournament player and check status
                        DuelTournamentPlayer lastTourPlay = getLastTournamentPlayer(pd.getID());
                        if (lastTourPlay == null || lastTourPlay.isFinished()) {
                            log.error("Player Duel status is playing and no duel found in memory and in DB for playerDuel=" + pd);
                            // last tournament is finished => change status
                            pd.setStatus(Constantes.PLAYER_DUEL_STATUS_NONE);
                            playerDuelDAO.updatePlayerDuel(pd);
                            log.error("Set player Duel status to none for playerDuel=" + pd);
                        } else {
                            log.error("Player Duel status is playing and no duel found in memory but lastTourPlay in DB is not finished ! playerDuel=" + pd);
                            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                        }
                    }
                }
                // player duel already existing => synchronize on it
                synchronized (getLockForPlayerDuel(pd.getID())) {
                    // reload player duel to be sure to have the last
                    pd = playerDuelDAO.getPlayerDuelForID(pd.getID());

                    // check request already existing
                    if (!pd.isRequestExpired()) {
                        log.warn("Player duel status is already request pd=" + pd);
                        throw new FBWSException(FBExceptionType.PLAYER_DUEL_STATUS_NOT_VALID);
                    }
                    pd.requestFromPlayer(playerRequest.getID());
                    pd.setDateCreation(System.currentTimeMillis());
                    try {
                        playerDuelDAO.updatePlayerDuel(pd);
                    } catch (Exception e) {
                        log.error("Exception to update playerDuel=" + pd, e);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                }
            }
            // add notif
            MessageNotif notif = notifMgr.createNotifDuelRequest(playerRequest, playerPartner, getDuelRequestDuration());
            // push event for partner
            if (sessionPartner != null) {
                sessionPartner.addDuelRequest(pd.getID());
                sessionPartner.pushEvent(messageMgr.buildEventDuelRequest(playerRequest, playerPartner, createDuelHistory(pd, playerPartner.getID(), null, true)));
                if (notif != null) {
                    sessionPartner.pushEvent(notifMgr.buildEvent(notif, playerPartner));
                }
            }
            // create duel history
            return createDuelHistory(pd, playerRequest.getID(), null, true);
        } else {
            log.error("Param players null - p1=" + playerRequest + " - p2=" + playerPartner);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Request duel with Argine. Create a new duel or update status of existing duel
     *
     * @param playerRequest
     * @throws FBWSException
     */
    public WSDuelHistory requestDuelWithArgine(Player playerRequest) throws FBWSException {
        Player playerArgine = playerMgr.getPlayerArgine();
        if (playerRequest != null) {
            PlayerDuel pd = playerDuelDAO.getPlayerDuelBetweenPlayer(playerRequest.getID(), playerArgine.getID());
            if (pd == null) {
                // no duel existing => synchronise of new player duel
                synchronized (getLockForNewPlayerDuel(playerRequest.getID(), playerArgine.getID())) {
                    // no player duel existing for these 2 players ... create it
                    pd = new PlayerDuel();
                    pd.setPlayer1(playerRequest);
                    pd.setPlayer2(playerArgine);
                    pd.requestFromPlayer(playerRequest.getID());
                    pd.setDateCreation(System.currentTimeMillis());
                    try {
                        playerDuelDAO.persistPlayerDuel(pd);
                    } catch (Exception e) {
                        log.error("Exception to persist playerDuel=" + pd, e);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                }
            } else {
                if (pd.isPlaying()) {
                    DuelMemTournament duelMemTournament = duelMemoryMgr.getDuelMemTournamentForPlayers(pd.getPlayer1().getID(), pd.getPlayer2().getID());
                    if (duelMemTournament != null) {
                        if (duelMemTournament.isExpired()) {
                            // try to finish it !
                            DuelTournamentPlayer tournamentPlayer = getTournamentPlayer(duelMemTournament.tourPlayerID);
                            if (tournamentPlayer != null) {
                                finishDuel(tournamentPlayer);
                            } else {
                                // remove from memory
                                duelMemoryMgr.removeMemTournament(duelMemTournament.tourPlayerID);
                            }
                        } else {
                            //not expired
                            log.error("Player duel status is already playing pd=" + pd);
                            throw new FBWSException(FBExceptionType.PLAYER_DUEL_STATUS_NOT_VALID);
                        }
                    } else {
                        // no duel found in memory ... try to find last tournament player and check status
                        DuelTournamentPlayer lastTourPlay = getLastTournamentPlayer(pd.getID());
                        if (lastTourPlay == null || lastTourPlay.isFinished()) {
                            log.error("Player Duel status is playing and no duel found in memory and in DB for playerDuel=" + pd);
                            // last tournament is finished => change status
                            pd.setStatus(Constantes.PLAYER_DUEL_STATUS_NONE);
                            playerDuelDAO.updatePlayerDuel(pd);
                            log.error("Set player Duel status to none for playerDuel=" + pd);
                        } else {
                            log.error("Player Duel status is playing and no duel found in memory but lastTourPlay in DB is not finished ! playerDuel=" + pd);
                            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                        }
                    }
                }
            }
            // change duel status
            pd.setStatus(Constantes.PLAYER_DUEL_STATUS_PLAYING);
            try {
                playerDuelDAO.updatePlayerDuel(pd);
            } catch (Exception e) {
                log.error("Exception to update playerDuel="+pd, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            // create tournament
            DuelTournamentPlayer duelTournamentPlayer = createTournamentPlayerForArgineDuel(pd, playerRequest.getID());
            if (duelTournamentPlayer == null) {
                pd.setStatus(Constantes.PLAYER_DUEL_STATUS_NONE);
                playerDuelDAO.updatePlayerDuel(pd);
                log.error("Failed to create tournament for playerDuel="+pd);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            // set tournamentPlayerID for Argine games
            List<DuelGame> listGameArgine = listGameFinishedOnTournamentForPlayer(duelTournamentPlayer.getTournament().getIDStr(), Constantes.PLAYER_ARGINE_ID);
            if (listGameArgine.size() > 0) {
                for (DuelGame e : listGameArgine) {
                    e.setTournamentPlayerID(duelTournamentPlayer.getIDStr());
                }
                updateListGameDB(listGameArgine);
            }

            // insert tournamentPlayer in memory
            DuelMemTournament dmt = duelMemoryMgr.addTournamentPlayer(duelTournamentPlayer);
            // add Argine games already played in memory
            if (dmt != null) {
                if (listGameArgine.size() > 0) {
                    for (DuelGame e : listGameArgine) {
                        dmt.addGamePlayer(e);
                    }
                }
            }
            // update duelArgineInProgress
            if (dmt != null) {
                DuelArgineInProgress duelArgineInProgress = getDuelArgineInProgressForTournament(dmt.tourID);
                if (duelArgineInProgress != null && duelArgineInProgress.playerID == playerRequest.getID() && duelArgineInProgress.duelTourPlayerID == null) {
                    duelArgineInProgress.duelTourPlayerID = dmt.tourPlayerID;
                    mongoTemplate.save(duelArgineInProgress);
                }
            }
            WSDuelHistory duelHist = createDuelHistory(pd, playerRequest.getID(), dmt, true);
            if (listGameArgine.size() == duelTournamentPlayer.getTournament().getNbDeals()) {
                duelHist.arginePlayAll = true;
            }
            return duelHist;
        } else {
            log.error("Param players null - p1=" + playerRequest + " - p2=" + playerArgine);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Set answer for request for duel between player
     * @param playerDuelID duel id
     * @param playerResponse player who made the response
     * @param response response of playerResponse
     * @throws FBWSException
     */
    public WSDuelHistory answerRequestDuel(long playerDuelID, Player playerResponse, boolean response) throws FBWSException {
        if (playerResponse == null) {
            log.error("Player is null");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        synchronized (getLockForPlayerDuel(playerDuelID)) {
            PlayerDuel pd = playerDuelDAO.getPlayerDuelForID(playerDuelID);
            if (pd == null) {
                log.error("No duel found for playerDuelID="+playerDuelID);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            if (!pd.isPlayerDuel(playerResponse.getID())) {
                log.error("This player is not in playerDuel duel=" + pd + " - player=" + playerResponse);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            if (pd.getStatusForAsk(playerResponse.getID()) != Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2) {
                log.error("Status of player duel not valid pd="+pd);
                throw new FBWSException(FBExceptionType.PLAYER_DUEL_STATUS_NOT_VALID);
            }
            // treat response
            if (response) {
                // change duel status
                pd.setStatus(Constantes.PLAYER_DUEL_STATUS_PLAYING);
                try {
                    playerDuelDAO.updatePlayerDuel(pd);
                } catch (Exception e) {
                    log.error("Exception to update playerDuel="+pd, e);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                // create tournament
                DuelTournamentPlayer duelTournamentPlayer = createTournamentPlayerForDuel(pd, DUEL_ORIGIN_REQUEST);
                if (duelTournamentPlayer == null) {
                    pd.setStatus(Constantes.PLAYER_DUEL_STATUS_NONE);
                    playerDuelDAO.updatePlayerDuel(pd);
                    log.error("Failed to create tournament for playerDuel="+pd);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                // insert tournamentPlayer in memory
                DuelMemTournament dmt = duelMemoryMgr.addTournamentPlayer(duelTournamentPlayer);

                WSDuelHistory duelHist = createDuelHistory(pd, playerResponse.getID(), dmt, true);

                Player partner = pd.getPartner(playerResponse.getID());
                // add notif
                MessageNotif notif = notifMgr.createNotifDuelAnswerOK(playerResponse, partner, getDuelDuration(), duelTournamentPlayer.getTournament().getIDStr());
                // push event for partner
                FBSession sessionPartner = presenceMgr.getSessionForPlayerID(partner.getID());
                if (sessionPartner != null) {
                    sessionPartner.addDuelInProgress(pd.getID());
                    sessionPartner.pushEvent(messageMgr.buildEventDuelRequestAnswer(playerResponse, partner, true, createDuelHistory(pd, partner.getID(), dmt, true)));
                    if (notif != null) {
                        sessionPartner.pushEvent(notifMgr.buildEvent(notif, partner));
                    }
                }
                return duelHist;
            } else {
                pd.setStatus(Constantes.PLAYER_DUEL_STATUS_NONE);
                try {
                    playerDuelDAO.updatePlayerDuel(pd);
                } catch (Exception e) {
                    log.error("Exception to update playerDuel="+pd, e);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                Player partner = pd.getPartner(playerResponse.getID());
                // push event for partner
                FBSession sessionPartner = presenceMgr.getSessionForPlayerID(partner.getID());
                if (sessionPartner != null) {
                    sessionPartner.addDuelInProgress(pd.getID());
                    sessionPartner.pushEvent(messageMgr.buildEventDuelRequestAnswer(playerResponse, partner, false, createDuelHistory(pd, partner.getID(), null, true)));
                }
                return createDuelHistory(pd, playerResponse.getID(), null, true);
            }
        }
    }

    /**
     * Process to find a player for a duel
     *
     * @param playerID
     */
    public void processMatchMakingForPlayer(long playerID) {
        MatchMakingElement elementFind = null;
        int expiration = getConfigIntValue("matchMaking.expirationNbSeconds", 180);
        if (log.isDebugEnabled()) {
            log.debug("Start process matchMaking for player=" + playerID + " - expiration=" + expiration);
        }
        List<MatchMakingStat> listStat = new ArrayList<>();
        synchronized (matchMakingQueue) {
            Iterator<MatchMakingElement> it = matchMakingQueue.iterator();
            while (it.hasNext()) {
                MatchMakingElement e = it.next();
                if (log.isDebugEnabled()) {
                    log.debug("Try with e=" + e);
                }
                if (e.playerID == playerID) {
                    continue;
                }
                if ((e.requestDate + (expiration * 1000)) < System.currentTimeMillis()) {
                    MatchMakingStat stat = new MatchMakingStat();
                    stat.dateRequest = e.requestDate;
                    stat.player1 = e.playerID;
                    listStat.add(stat);
                    it.remove();
                } else {
                    // check player no link block
                    PlayerLink pl = playerMgr.getLinkBetweenPlayer(playerID, e.playerID);
                    if (pl != null) {
                        if (pl.hasBlocked()) {
                            continue;
                        }
                    }
                    // check duel is not in progress for these 2 players
                    if (duelMemoryMgr.getDuelMemTournamentForPlayers(playerID, e.playerID) == null) {
                        elementFind = e;
                        MatchMakingStat stat = new MatchMakingStat();
                        stat.dateRequest = e.requestDate;
                        stat.player1 = e.playerID;
                        stat.player2 = playerID;
                        stat.dateMatch = System.currentTimeMillis();
                        listStat.add(stat);
                        break;
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("A duel already is progress for these 2 players : playerID=" + playerID + " - e=" + e);
                        }
                    }
                }
            }
            if (elementFind == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No player found => add a new element with playerID=" + playerID);
                }
                matchMakingQueue.add(new MatchMakingElement(playerID));
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Player found elementFind=" + elementFind + " - remove it from the queue");
                }
                matchMakingQueue.remove(elementFind);
            }
        }
        if (elementFind != null) {
            Player p1 = playerMgr.getPlayer(playerID);
            Player p2 = playerMgr.getPlayer(elementFind.playerID);
            if (p1 != null && p2 != null) {
                createDuelMatchMaking(p1, p2);
            } else {
                log.error("No player found for player1(ID" + playerID + ")=" + p1 + " - player2(ID" + elementFind.playerID + ")=" + p2);
            }
        }
        if (listStat != null && listStat.size() > 0 && getConfigIntValue("matchMakingStat", 1) == 1) {
            mongoTemplate.insertAll(listStat);
        }
    }

    /**
     * Run thread to find another player waiting for duel
     * @param playerID
     * @return date expiration
     */
    public long enableMatchMakingForPlayer(long playerID) {
        int expiration = getConfigIntValue("matchMaking.expirationNbSeconds", 180);
        DuelMatchMakingThread t = new DuelMatchMakingThread(playerID);
        threadPoolMatchMaking.execute(t);
        return System.currentTimeMillis() + (expiration*1000);
    }

    /**
     * Remove the player from the queue of waiting for duel
     * @param playerID
     */
    public void disableMatchMakingForPlayer(long playerID) {
        synchronized (matchMakingQueue) {
            Iterator<MatchMakingElement> it = matchMakingQueue.iterator();
            while (it.hasNext()) {
                MatchMakingElement e = it.next();
                if (e.playerID == playerID) {
                    matchMakingQueue.remove(e);
                    break;
                }
            }
        }
    }

    private int getReminderPeriodMinutes() {
        return getConfigIntValue("reminder.period", 30);
    }

    /**
     * Send reminder notif for duel not finish and soon expired
     *
     * @return nb duels for which notif are sent to players
     */
    public int processReminder(long date) {
        int nbDuelReminder = 0;
        if (notifMgr.isNotifEnable() && !presenceMgr.isServiceMaintenance()) {
            String remainingUnit = getConfigStringValue("reminder.remainingTimeUnit", TimeUnit.HOURS.toString());
            long tsRemaining = ((long) getConfigIntValue("reminder.remainingTime", 4) * Constantes.getTimestampByTimeUnit(remainingUnit));
            long tsRemainingLow = tsRemaining - (getReminderPeriodMinutes()/2 * Constantes.TIMESTAMP_MINUTE);
            long tsRemainingHigh = tsRemaining + (getReminderPeriodMinutes()/2 * Constantes.TIMESTAMP_MINUTE);

            List<String> listAlert = new ArrayList<>();
            List<DuelMemTournament> listDuelReminder = duelMemoryMgr.listDuelToReminder(date, tsRemainingLow, tsRemainingHigh);
            if (listDuelReminder != null && listDuelReminder.size() > 0) {
                for (DuelMemTournament dmt : listDuelReminder) {
                    // check duel not finish, not expired and not yet reminder process
                    if (!dmt.isAllPlayed() && !dmt.isExpired() && dmt.dateReminder == 0) {
                        dmt.dateReminder = System.currentTimeMillis();
                        nbDuelReminder++;
                        PlayerDuel pd = getPlayerDuel(dmt.playerDuelID);
                        if (pd != null) {
                            // check player1 not play all
                            if (!dmt.isAllPlayedForPlayer(pd.getPlayer1().getID())) {
                                int nbGame = countGameFinishedOnTournamentAndPlayer(dmt.tourID, pd.getPlayer1().getID());
                                if (nbGame != dmt.getNbDealPlayed(pd.getPlayer1().getID())) {
                                    listAlert.add("NbGame in memory and in BDD not the same !! TournamentPlayerID="+dmt.tourPlayerID+" - player="+pd.getPlayer1().getID()+" - NbGameBDD="+nbGame+" - nbGameMemory="+dmt.getNbDealPlayed(pd.getPlayer1().getID()));
                                    // need to reload
                                    if (getConfigIntValue("reminderReloadForNbGameNotValid", 0) == 1) {
                                        reloadDuelInMemory(dmt.tourPlayerID);
                                        continue;
                                    }
                                } else {
                                    if (!pd.isPlayer1(Constantes.PLAYER_ARGINE_ID)) {
                                        // send notif for player1
                                        MessageNotif notif = notifMgr.createNotifDuelReminder(pd.getPlayer2(), pd.getPlayer1(), dmt.dateFinish - System.currentTimeMillis(), dmt.tourID);
                                        if (notif != null) {
                                            FBSession session = presenceMgr.getSessionForPlayerID(pd.getPlayer1().getID());
                                            if (session != null) {
                                                session.pushEvent(notifMgr.buildEvent(notif, pd.getPlayer1()));
                                            }
                                        }
                                    } else {
                                        listAlert.add("Argine not finished ! - TournamentPlayerID="+dmt.tourPlayerID+" - player=Argine - Argine not finished to play Duel !! - NbGameBDD="+nbGame+" - nbGameMemory="+dmt.getNbDealPlayed(pd.getPlayer1().getID()));
                                        // argine not finshed to play => try to restart it
                                        if (getConfigIntValue("reminderStartArgineNotFinished", 0) == 1 && ((System.currentTimeMillis() - dmt.getMemTournamentPlayer(Constantes.PLAYER_ARGINE_ID).dateLastPlay) > 5 * Constantes.TIMESTAMP_MINUTE)) {
                                            try {
                                                startPlayArgine(dmt.tourID);
                                            } catch (Exception e) {
                                                log.error("Failed to startPlayArgine for duel="+dmt, e);
                                            }
                                        }
                                    }
                                }
                            }
                            // check player2 not play all
                            if (!dmt.isAllPlayedForPlayer(pd.getPlayer2().getID()) && !pd.isPlayer2(Constantes.PLAYER_ARGINE_ID)) {
                                int nbGame = countGameFinishedOnTournamentAndPlayer(dmt.tourID, pd.getPlayer2().getID());
                                if (nbGame != dmt.getNbDealPlayed(pd.getPlayer2().getID())) {
                                    listAlert.add("NbGame in memory and in BDD not the same !! TournamentPlayerID="+dmt.tourPlayerID+" - player="+pd.getPlayer2().getID()+" - NbGameBDD="+nbGame+" - nbGameMemory="+dmt.getNbDealPlayed(pd.getPlayer2().getID()));
                                    // need to reload
                                    if (getConfigIntValue("reminderReloadForNbGameNotValid", 0) == 1) {
                                        reloadDuelInMemory(dmt.tourPlayerID);
                                        continue;
                                    }
                                } else {
                                    if (!pd.isPlayer2(Constantes.PLAYER_ARGINE_ID)) {
                                        // send notif for player2
                                        MessageNotif notif = notifMgr.createNotifDuelReminder(pd.getPlayer1(), pd.getPlayer2(), dmt.dateFinish - System.currentTimeMillis(), dmt.tourID);
                                        if (notif != null) {
                                            FBSession session = presenceMgr.getSessionForPlayerID(pd.getPlayer2().getID());
                                            if (session != null) {
                                                session.pushEvent(notifMgr.buildEvent(notif, pd.getPlayer2()));
                                            }
                                        }
                                    } else {
                                        listAlert.add("Argine not finished ! - TournamentPlayerID="+dmt.tourPlayerID+" - player=Argine - Argine not finished to play Duel !! - NbGameBDD="+nbGame+" - nbGameMemory="+dmt.getNbDealPlayed(pd.getPlayer2().getID()));
                                        // argine not finshed to play => try to restart it
                                        if (getConfigIntValue("reminderStartArgineNotFinished", 0) == 1 && ((System.currentTimeMillis() - dmt.getMemTournamentPlayer(Constantes.PLAYER_ARGINE_ID).dateLastPlay) > 5 * Constantes.TIMESTAMP_MINUTE)) {
                                            try {
                                                startPlayArgine(dmt.tourID);
                                            } catch (Exception e) {
                                                log.error("Failed to startPlayArgine for duel="+dmt, e);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (listAlert.size() > 0) {
                String listAlertStr = "<b>"+listAlert.size()+" duel(s) with problems:</b><br>"+StringTools.listToString(listAlert, "<br>");
                ContextManager.getAlertMgr().addAlert(Constantes.TOURNAMENT_CATEGORY_NAME_DUEL, FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "DUEL ProcessReminder - "+listAlert.size()+" duel(s) with problems", listAlertStr, null);
            }
        }
        return nbDuelReminder;
    }

    /**
     * Return if existing game on tournament and deal for a player
     * @param tourID
     * @param dealIndex
     * @param playerID
     * @return
     * @throws FBWSException
     */
    public DuelGame getGameOnTournamentAndDealForPlayer(String tourID, int dealIndex, long playerID) {
        Query q = new Query();
        Criteria cPlayerID = Criteria.where("playerID").is(playerID);
        Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
        Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
        q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cPlayerID));
        List<DuelGame> list = mongoTemplate.find(q, DuelGame.class);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
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
            return (int) mongoTemplate.count(q, DuelGame.class);
        } catch (Exception e) {
            log.error("Failed to count games for player - playerID="+ playerID, e);
        }
        return 0;
    }

    /**
     * Count game on tournament and player
     * @param tourID
     * @param playerID
     * @return
     */
    public int countGameFinishedOnTournamentAndPlayer(String tourID, long playerID) {
        Query q = new Query();
        Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
        Criteria cPlayerID = Criteria.where("playerID").is(playerID);
        Criteria cFinished = Criteria.where("finished").is(true);
        q.addCriteria(new Criteria().andOperator(cTourID, cPlayerID, cFinished));
        return (int)mongoTemplate.count(q, DuelGame.class);
    }


    /**
     * Create a duelGame
     * @param tournamentPlayer
     * @param playerID
     * @param dealIndex
     * @param conventionProfil
     * @param conventionValue
     * @param cardsConventionProfil
     * @param cardsConventionValue
     * @param deviceID
     * @return
     */
    public DuelGame createGame(DuelTournamentPlayer tournamentPlayer, long playerID, int dealIndex, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue, long deviceID) {
        DuelGame game = new DuelGame(playerID, tournamentPlayer.getTournament(), dealIndex);
        game.setStartDate(System.currentTimeMillis());
        game.setLastDate(System.currentTimeMillis());
        game.setConventionSelection(conventionProfil, conventionValue);
        game.setCardsConventionSelection(cardsConventionProfil, cardsConventionValue);
        game.setDeviceID(deviceID);
        game.setArginePlayAlone(false);
        game.setTournamentPlayerID(tournamentPlayer.getIDStr());
        mongoTemplate.insert(game);
        return game;
    }

    /**
     * Create a duelGame
     * @param tournament
     * @param playerID
     * @param dealIndex
     * @param conventionProfil
     * @param conventionValue
     * @param cardsConventionProfil
     * @param cardsConventionValue
     * @param deviceID
     * @return
     */
    public DuelGame createGameArgineAlone(DuelTournament tournament, long playerID, int dealIndex, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue, long deviceID) {
        DuelGame game = new DuelGame(playerID, tournament, dealIndex);
        game.setStartDate(System.currentTimeMillis());
        game.setLastDate(System.currentTimeMillis());
        game.setConventionSelection(conventionProfil, conventionValue);
        game.setCardsConventionSelection(cardsConventionProfil, cardsConventionValue);
        game.setDeviceID(deviceID);
        game.setArginePlayAlone(true);
        mongoTemplate.insert(game);
        return game;
    }

    /**
     * Update game in Database
     * @param game
     */
    public void updateGameDB(DuelGame game) {
        if (game != null && !game.isReplay()) {
            // try to find bug of not valid game ...
            if (getConfigIntValue("findBugGameNotValid", 1) == 1) {
                if (game.isFinished() && game.getContractType() == 0 && game.getBids().length() == 0) {
                    log.error("Game not valid !!! - game=" + game);
                    Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                    if (getConfigIntValue("blockBugGameNotValid", 1) == 1) {
                        log.error("Game not saved ! game="+game);
                        return;
                    }
                }
            }
            mongoTemplate.save(game);
        }
    }


    /**
     * Return list of dealDuel for tournamentDuel and playerAsk
     * @param duelTournamentPlayer
     * @param playerAsk
     * @return
     */
    public List<WSDealDuel> getListDealForTournament(DuelTournamentPlayer duelTournamentPlayer, long playerAsk) {
        List<WSDealDuel> listDealDuel = new ArrayList<WSDealDuel>();
        if (duelTournamentPlayer != null) {
            if (!duelTournamentPlayer.isFinished()) {
                // tournament not finished => try to find result in memory
                DuelMemTournament mdr = duelMemoryMgr.getDuelMemTournament(duelTournamentPlayer.getIDStr());
                if (mdr != null) {
                    int lastIndexPlayedByPlayerAsk = 0;
                    for (DuelMemDeal memDeal : mdr.tabDeal) {
                        DuelMemGame memGame1 = memDeal.getForPlayer(duelTournamentPlayer.getPlayer1IDForAsk(playerAsk));
                        WSDealDuel dealDuel = new WSDealDuel();
                        dealDuel.dealIDstr = memDeal.dealID;
                        if (memGame1 != null) {
                            lastIndexPlayedByPlayerAsk = memDeal.dealIndex;
                            dealDuel.playedPlayer1 = true;
                            dealDuel.contractPlayer1 = memGame1.getContractWS();
                            dealDuel.declarerPlayer1 = memGame1.declarer;
                            dealDuel.gameIDPlayer1str = memGame1.gameID;
                            dealDuel.nbTricksPlayer1 = memGame1.nbTricks;
                            dealDuel.scorePlayer1 = memGame1.score;
                            dealDuel.leadPlayer1 = memGame1.begins;
                        }
                        DuelMemGame memGame2 = memDeal.getForPlayer(duelTournamentPlayer.getPlayer2IDForAsk(playerAsk));
                        if (memGame2 != null) {
                            if (mdr.isDuelWithArgine()) {
                                if (memDeal.dealIndex - lastIndexPlayedByPlayerAsk <= 1) {
                                    dealDuel.playedPlayer2 = true;
                                }
                            } else {
                                dealDuel.playedPlayer2 = true;
                            }
                            // set data for player2 only if player1 has played it
                            if (dealDuel.playedPlayer1) {
                                dealDuel.contractPlayer2 = memGame2.getContractWS();
                                dealDuel.declarerPlayer2 = memGame2.declarer;
                                dealDuel.gameIDPlayer2str = memGame2.gameID;
                                dealDuel.nbTricksPlayer2 = memGame2.nbTricks;
                                dealDuel.scorePlayer2 = memGame2.score;
                                dealDuel.result = memGame1.result;
                                dealDuel.leadPlayer2 = memGame2.begins;
                            }
                        }
                        listDealDuel.add(dealDuel);
                    }
                } else {
                    log.error("No result found in memory for duelTournamentPlayer="+duelTournamentPlayer);
                }

            } else {
                String tourID = duelTournamentPlayer.getTournament().getIDStr();
                List<DuelGame> listGamePlayer1 = listGameOnTournamentForPlayer(tourID, duelTournamentPlayer.getPlayer1IDForAsk(playerAsk));
                List<DuelGame> listGamePlayer2 = listGameOnTournamentForPlayer(tourID, duelTournamentPlayer.getPlayer2IDForAsk(playerAsk));

                for (DuelDeal deal : duelTournamentPlayer.getTournament().getDeals()) {
                    WSDealDuel e = new WSDealDuel();
                    e.dealIDstr = deal.getDealID(tourID);
                    double res1 = 0, res2 = 0;
                    if (listGamePlayer1 != null && listGamePlayer1.size()>= deal.getIndex() && deal.getIndex() > 0) {
                        DuelGame game1 = listGamePlayer1.get(deal.getIndex()-1);
                        if (game1.getDealIndex() == deal.getIndex()) {
                            e.playedPlayer1 = true;
                            res1 = game1.getResult();
                            e.gameIDPlayer1str = game1.getIDStr();
                            e.scorePlayer1 = game1.getScore();
                            e.contractPlayer1 = game1.getContractWS();
                            e.declarerPlayer1 = Character.toString(game1.getDeclarer());
                            e.nbTricksPlayer1 = game1.getTricks();
                            e.leadPlayer1 = game1.getBegins();
                        }
                    }
                    if (listGamePlayer2 != null && listGamePlayer2.size()>= deal.getIndex() && deal.getIndex() > 0) {
                        DuelGame game2 = listGamePlayer2.get(deal.getIndex()-1);
                        if (game2.getDealIndex() == deal.getIndex()) {
                            e.playedPlayer2 = true;
                            res2 = game2.getResult();
                            e.gameIDPlayer2str = game2.getIDStr();
                            e.scorePlayer2 = game2.getScore();
                            e.contractPlayer2 = game2.getContractWS();
                            e.declarerPlayer2 = Character.toString(game2.getDeclarer());
                            e.nbTricksPlayer2 = game2.getTricks();
                            e.leadPlayer2 = game2.getBegins();
                        }
                    }
                    // if res1 or res2 = 0 => game result old method
                    if (res2 == 0 && res1 > 0) {
                        e.result = res1;
                    } else if (res1 == 0 && res2 > 0) {
                        e.result = -res2;
                    }
                    // else result is value from game1
                    else {
                        e.result = res1;
                    }
                    listDealDuel.add(e);
                }
            }
        } else {
            log.error("Param duelTournamentPlayer is null");
        }
        return listDealDuel;
    }

    /**
     * Return the list of tournamentDuelResult for playerDuel
     * @param playerDuel
     * @param playerAsk
     * @return
     */
    public List<WSTournamentDuelResult> listTournamentDuelResult(PlayerDuel playerDuel, long playerAsk) {
        if (playerDuel != null) {
            List<WSTournamentDuelResult> listTourDuelResult = new ArrayList<WSTournamentDuelResult>();
            Query q = new Query(Criteria.where("playerDuelID").is(playerDuel.getID()));
            int nbDayBefore = getConfigIntValue("listTournamentDuelResultNbDayBefore", 30);
            if (nbDayBefore > 0) {
                Calendar curDate = Calendar.getInstance();
                curDate.add(Calendar.DAY_OF_YEAR, -nbDayBefore);
                q.addCriteria(Criteria.where("creationDate").gt(curDate.getTimeInMillis()));
            }
            q.with(new Sort(Sort.Direction.DESC, "creationDate"));
            int nbMax = getConfigIntValue("listTournamentDuelResultNbMax", 50);
            if (nbMax > 0) {
                q.limit(nbMax);
            }
            List<DuelTournamentPlayer> listTournamentPlayer = mongoTemplate.find(q, DuelTournamentPlayer.class);
            for (DuelTournamentPlayer dtp : listTournamentPlayer) {
                WSTournamentDuelResult tdr = null;
                DuelMemTournament memTournament = duelMemoryMgr.getDuelMemTournament(dtp.getIDStr());
                if (memTournament != null) {
                    tdr = buildDuelResult(memTournament, playerAsk);
                } else {
                    tdr = buildDuelResult(dtp, playerAsk);
                }
                if (tdr != null) {
                    listTourDuelResult.add(tdr);
                }
            }
            return listTourDuelResult;
        }
        return null;
    }

    public WSTournamentDuelResult buildDuelResult(DuelMemTournament memTour, long playerAsk) {
        if (memTour != null) {
            WSTournamentDuelResult tdr = new WSTournamentDuelResult();
            tdr.tourIDstr = memTour.tourID;
            tdr.dateFinish = memTour.dateFinish;
            DuelMemTournamentPlayer memTourPla1 = memTour.getMemTournamentPlayer(memTour.getPlayer1IDForAsk(playerAsk));
            if (memTourPla1 != null) {
                tdr.resultPlayer1 = memTourPla1.result;
            }
            DuelMemTournamentPlayer memTourPla2 = memTour.getMemTournamentPlayer(memTour.getPlayer2IDForAsk(playerAsk));
            if (memTourPla2 != null) {
                tdr.resultPlayer2 = memTourPla2.result;
            }
            return tdr;
        }
        return null;
    }

    public WSTournamentDuelResult buildDuelResult(DuelTournamentPlayer dtp, long playerAsk) {
        if (dtp != null && dtp.getTournament() != null) {
            WSTournamentDuelResult tdr = new WSTournamentDuelResult();
            tdr.tourIDstr = dtp.getTournament().getIDStr();
            tdr.dateFinish = dtp.computeDateFinish();
            tdr.resultPlayer1 = dtp.getResultForPlayer1(playerAsk);
            tdr.resultPlayer2 = dtp.getResultForPlayer2(playerAsk);
            return tdr;
        } return null;
    }

    /**
     * List duelHistory for only friend
     * @param player
     * @param offset
     * @param nbMax
     * @return
     */
    public List<WSDuelHistory> listDuelHistoryFriend(Player player, int offset, int nbMax) {
        WSGamePlayer gamePlayer = WSGamePlayer.createGamePlayerHuman(player, player.getID());
        List<WSDuelHistory> listDuelHist = playerDuelDAO.listDuelHistoryFriend(gamePlayer, offset, nbMax);
        if (listDuelHist != null) {
            for (WSDuelHistory duelHist : listDuelHist) {
                duelHist.player1.connected = true;
                duelHist.player2.connected = presenceMgr.isSessionForPlayerID(duelHist.player2.playerID);
                // if status play => set tournament id
                if (duelHist.status == Constantes.PLAYER_DUEL_STATUS_PLAYING) {
                    DuelMemTournament duelMemTour = duelMemoryMgr.getDuelMemTournamentForPlayers(duelHist.player1.playerID, duelHist.player2.playerID);
                    duelHist.expirationDate = 0;
                    if (duelMemTour != null) {
                        // it is expired ... set status none and when request is received, finish it
                        if (duelMemTour.isExpired()) {
                            duelHist.status = Constantes.PLAYER_DUEL_STATUS_NONE;
                        } else {
                            duelHist.tourIDstr = duelMemTour.tourID;
                            duelHist.expirationDate = duelMemTour.dateFinish;
                            if (duelMemTour.isAllPlayedForPlayer(player.getID())) {
                                duelHist.status = Constantes.PLAYER_DUEL_STATUS_PLAYED;
                            }
                        }
                    } else {
                        duelHist.status = Constantes.PLAYER_DUEL_STATUS_NONE;
                        log.error("No memDuelResult found for playerDuelID=" + duelHist.playerDuelID);
                    }
                }
                // if status request => check expiration
                else if (duelHist.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1 || duelHist.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2) {
                    if ((duelHist.creationDate + getDuelRequestDuration()) < System.currentTimeMillis()) {
                        duelHist.status = Constantes.PLAYER_DUEL_STATUS_NONE;
                        duelHist.expirationDate = 0;
                    } else {
                        duelHist.expirationDate = duelHist.creationDate + getDuelRequestDuration();
                    }
                }
            }
        }
        return listDuelHist;
    }

    public int countListDuelHistory(long playerId){
        return playerDuelDAO.countDuelHistoric(playerId);
    }

    /**
     * List duelHistory for a player.
     * @param player
     * @param offset
     * @param nbMax
     * @return
     */
    public List<WSDuelHistory> listDuelHistory(Player player, int offset, int nbMax) {
        if (player != null && nbMax > 0) {
            WSGamePlayer gamePlayer = WSGamePlayer.createGamePlayerHuman(player, player.getID());
            List<WSDuelHistory> listDuelHist =  playerDuelDAO.listDuelHistory(gamePlayer, offset, nbMax);
            if (listDuelHist != null) {
                for (WSDuelHistory duelHist : listDuelHist) {
                    duelHist.player1.connected = true;
                    duelHist.player2.connected = presenceMgr.isSessionForPlayerID(duelHist.player2.playerID);
                    // if status play => set tournament id
                    if (duelHist.status == Constantes.PLAYER_DUEL_STATUS_PLAYING) {
                        DuelMemTournament duelMemTour = duelMemoryMgr.getDuelMemTournamentForPlayers(duelHist.player1.playerID, duelHist.player2.playerID);
                        duelHist.expirationDate = 0;
                        if (duelMemTour != null) {
                            // it is expired ... set status none and when request is received, finish it
                            if (duelMemTour.isExpired()) {
                                duelHist.status = Constantes.PLAYER_DUEL_STATUS_NONE;
                            } else {
                                duelHist.tourIDstr = duelMemTour.tourID;
                                duelHist.expirationDate = duelMemTour.dateFinish;
                                if (duelMemTour.isAllPlayedForPlayer(player.getID())) {
                                    duelHist.status = Constantes.PLAYER_DUEL_STATUS_PLAYED;
                                }
                            }
                        } else {
                            duelHist.status = Constantes.PLAYER_DUEL_STATUS_NONE;
                            log.error("No memDuelResult found for playerDuelID=" + duelHist.playerDuelID);
                        }
                    }
                    // if status request => check expiration
                    else if (duelHist.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1 || duelHist.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2) {
                        if ((duelHist.creationDate + getDuelRequestDuration()) < System.currentTimeMillis()) {
                            duelHist.status = Constantes.PLAYER_DUEL_STATUS_NONE;
                            duelHist.expirationDate = 0;
                        } else {
                            duelHist.expirationDate = duelHist.creationDate + getDuelRequestDuration();
                        }
                    }

                    if (duelHist.status == Constantes.PLAYER_DUEL_STATUS_NONE) {
                        duelHist.orderList = 0;
                    } else if (duelHist.status == Constantes.PLAYER_DUEL_STATUS_PLAYED) {
                        duelHist.orderList = 1;
                    } else if (duelHist.status == Constantes.PLAYER_DUEL_STATUS_PLAYING) {
                        duelHist.orderList = 3;
                    } else if (duelHist.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1) {
                        duelHist.orderList = 2;
                    } else if (duelHist.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2) {
                        duelHist.orderList = 4;
                    }
                }
            }

            // order list
            Collections.sort(listDuelHist, new Comparator<WSDuelHistory>() {
                @Override
                public int compare(WSDuelHistory o1, WSDuelHistory o2) {
                    // ORDER :
                    // - REQUEST RECEIVE (date ASC)
                    // - PLAYING (date ASC)
                    // - REQUEST SENT (date ASC)
                    // - PLAYED (date DESC)
                    // - Others (date desc or pseudo ASC)
                    // Checked by J�j� le 30/06/2015

                    // order by orderList DESC
                    int compareOrder = Integer.compare(o1.orderList,o2.orderList);
                    if (compareOrder != 0) {
                        return -compareOrder;
                    }
                    // same => order by date
                    // if REQUEST RECEIVE, PLAYING, REQUEST SENT then order by expiration date asc
                    if (o1.orderList == 4 || o1.orderList == 3 || o1.orderList == 2) {
                        long date1 = o1.expirationDate;
                        long date2 = o2.expirationDate;
                        if (date1 > 0 || date2 > 0) {
                            int compareDate = Long.compare(date1, date2);
                            if (compareDate != 0) {
                                return compareDate;
                            }
                        }
                    }

                    if(o1.player1.playerID == Constantes.PLAYER_ARGINE_ID || o1.player2.playerID == Constantes.PLAYER_ARGINE_ID){
                        return -1;
                    }
                    if(o2.player1.playerID == Constantes.PLAYER_ARGINE_ID || o2.player2.playerID == Constantes.PLAYER_ARGINE_ID){
                        return 1;
                    }

                    // else order by date last duel desc
                    long date1 = o1.dateLastDuel;
                    long date2 = o2.dateLastDuel;
                    if (date1 > 0 || date2 > 0) {
                        int compareDate = Long.compare(date1, date2);
                        if (compareDate != 0) {
                            return -compareDate;
                        }
                    }

                    // at end order by pseudo asc (at player2)
                    return o1.player2.pseudo.compareTo(o2.player2.pseudo);
                }
            });

            // replace Argine player
            int idxDuelArgine = -1;
            WSDuelHistory duelHistoryArgine = null;
            boolean duelArgineEnable = getConfigIntValue("duelArgineEnable", 1) == 1;

            for (int i = 0; i < listDuelHist.size(); i++) {
                WSDuelHistory e = listDuelHist.get(i);
                if (e.player1.playerID == Constantes.PLAYER_ARGINE_ID || e.player2.playerID == Constantes.PLAYER_ARGINE_ID) {
                    idxDuelArgine= i;
                    duelHistoryArgine = e;
                    break;
                }
            }
            if (idxDuelArgine >= 0) {
                if (!duelArgineEnable) {
                    listDuelHist.remove(idxDuelArgine);
                }
            }
            else {
                if (duelArgineEnable) {
                    try {
                        duelHistoryArgine = playerMgr.getDuelHistoryBetweenPlayers(player, playerMgr.getPlayerArgine(), true);
                        if (duelHistoryArgine != null) {
                            boolean argineAdded = false;
                            for (int i = 0; i < listDuelHist.size(); i++) {
                                WSDuelHistory e = listDuelHist.get(i);
                                if (e.orderList == 0) {
                                    listDuelHist.add(i, duelHistoryArgine);
                                    argineAdded = true;
                                    break;
                                }
                            }
                            if(!argineAdded){
                                listDuelHist.add(duelHistoryArgine);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to get playerArgine !", e);
                    }
                }
            }
            while (listDuelHist.size() > nbMax) {
                listDuelHist.remove(listDuelHist.size()-1);
            }
            return listDuelHist;
        } else {
            log.error("Parameter not valid - player="+player+" - nbMax="+nbMax);
        }
        return null;
    }

    /**
     * Method to play tournamentDuel. If first time, the table is created else the associated existing is returned.
     * @param tournamentID
     * @param session
     * @param conventionProfil
     * @param conventionValue
     * @param cardsConventionProfil
     * @param cardsConventionValue
     * @return
     * @throws FBWSException
     */
    public WSTableTournament playTournament(String tournamentID, FBSession session, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) throws FBWSException {
        if (session == null) {
            log.error("Parameter is null - session=" + session);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        DuelTournamentPlayer duelTournamentPlayer = getTournamentPlayer(tournamentID, session.getPlayer().getID());
        if (duelTournamentPlayer == null) {
            log.error("duelTournamentPlayer is null - session="+session+" - tournamentID="+tournamentID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (duelTournamentPlayer.isExpired() || duelTournamentPlayer.isFinished()) {
            throw new FBWSException(FBExceptionType.TOURNAMENT_FINISHED);
        }
        DuelMemTournament memTour = duelMemoryMgr.getDuelMemTournament(duelTournamentPlayer.getIDStr());
        if (memTour == null) {
            log.error("No duelMemTournament found for duelTournamentPlayer="+duelTournamentPlayer);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Table table = null;
        DuelTournament tournament = duelTournamentPlayer.getTournament();
        if (session.getCurrentGameTable() != null &&
                !session.getCurrentGameTable().isReplay() &&
                session.getCurrentGameTable().getTournament() != null &&
                session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_DUEL &&
                session.getCurrentGameTable().getTournament().getIDStr().equals(tournament.getIDStr())) {
            table = session.getCurrentGameTable();
        }
        else {
            if (!duelTournamentPlayer.isPlayerDuel(session.getPlayer().getID())) {
                log.error("Player is not defined on duel="+duelTournamentPlayer+" - player="+session.getPlayer());
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            if (memTour.isAllPlayedForPlayer(session.getPlayer().getID())) {
                log.error("All deals played for player="+session.getPlayer()+" - memTour="+memTour);
                throw new FBWSException(FBExceptionType.TOURNAMENT_FINISHED);
            }
            table = new Table(session.getPlayer(), tournament);
            DuelMemTournamentPlayer memTourPlayer = memTour.getMemTournamentPlayer(session.getPlayer().getID());
            if (memTourPlayer != null) {
                table.setPlayedDeals(memTourPlayer.dealsPlayed);
            }
            // retrieve game if exist
            DuelGame game = getGameNotFinishedOnTournamentForPlayer(tournament.getIDStr(), session.getPlayer().getID());
            if (game != null) {
                table.setGame(game);
            }
        }
        session.setCurrentGameTable(table);
        //*******************
        // retrieve game
        DuelGame game = null;
        if (table.getGame() != null &&
                !table.getGame().isReplay() &&
                !table.getGame().isFinished() &&
                table.getGame().getTournament().getIDStr().equals(tournament.getIDStr())) {
            game = (DuelGame)table.getGame();
        } else {
            // need to retrieve an existing game
            int lastIndexPlayed = 0;
            List<DuelGame> listGame = listGameOnTournamentForPlayer(tournament.getIDStr(), session.getPlayer().getID());
            if (listGame != null) {
                for (DuelGame g : listGame) {
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
                int nbDealPlayedMemory = memTour.getNbDealPlayed(session.getPlayer().getID());
                // need to create a new game
                if (lastIndexPlayed >= tournament.getNbDeals()) {
                    log.error("lastIndexPlayed to big to play another deal ! player="+session.getPlayer()+" - lastIndexPlayed="+lastIndexPlayed+" - tournament="+tournament);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                // check player credit
                playerMgr.checkPlayerCredit(session.getPlayer(), tournament.getNbCreditsPlayDeal());
                // create game
                game = createGame(duelTournamentPlayer, session.getPlayer().getID(), lastIndexPlayed+1, conventionProfil, conventionValue, cardsConventionProfil, cardsConventionValue, session.getDeviceID());

                if (game != null) {
                    ContextManager.getPlayerMgr().updatePlayerCreditDeal(session.getPlayer(), tournament.getNbCreditsPlayDeal(), 1);
                } else {
                    log.error("Failed to create game - player="+session.getPlayer().getID()+" - tour="+tournament);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_DUEL, 1);
            }
            table.setGame(game);
        }

        // check if a thread is not running for this game
        GameThread robotThread = gameMgr.getThreadPlayRunning(game.getIDStr());
        if (robotThread != null) {
            if (log.isDebugEnabled()) {
                log.debug("A thread is currently running for gameID=" + game.getIDStr() + " - stop it !");
            }
            robotThread.interruptRun();
        }

        //*********************
        // Build data to return
        WSTableTournament tableTour = new WSTableTournament();
        tableTour.tournament = toWSTournament(duelTournamentPlayer, session.getPlayerCache());
        tableTour.tournament.currentDealIndex = game.getDealIndex();
        tableTour.currentDeal = new WSGameDeal();
        tableTour.currentDeal.setDealData(game.getDeal(), tournament.getIDStr());
        tableTour.currentDeal.setGameData(game);
        tableTour.table = table.toWSTableGame();
        tableTour.gameIDstr = game.getIDStr();
        tableTour.conventionProfil = game.getConventionProfile();
        tableTour.creditAmount = session.getPlayer().getCreditAmount();
        tableTour.nbPlayedDeals = session.getPlayer().getNbPlayedDeals();
        tableTour.replayEnabled = playerMgr.isReplayEnabledForPlayer(session);
        tableTour.freemium = session.isFreemium();
        return tableTour;
    }

    public WSTournament toWSTournament(DuelTournament duelTournament, PlayerCache playerCache) {
        DuelTournamentPlayer duelTournamentPlayer = getTournamentPlayer(duelTournament.getIDStr(), playerCache.ID);
        if (duelTournamentPlayer != null) {
            return toWSTournament(duelTournamentPlayer, playerCache);
        }
        return duelTournament.toWS();
    }

    /**
     * Transform tournament for webservice object
     * @param duelTournamentPlayer
     * @param playerCache
     * @return
     */
    public WSTournament toWSTournament(DuelTournamentPlayer duelTournamentPlayer, PlayerCache playerCache) {
        if (duelTournamentPlayer != null) {
            WSTournament wst = duelTournamentPlayer.getTournament().toWS();
            wst.endDate = duelTournamentPlayer.computeDateFinish();
            DuelMemTournament dmt = duelMemoryMgr.getDuelMemTournament(duelTournamentPlayer.getIDStr());
            if (dmt != null) {
                wst.resultPlayer = dmt.getWSResultTournamentPlayer(playerCache);
                wst.currentDealIndex = dmt.getCurrentDealForPlayer(playerCache.ID);
                wst.remainingTime = duelTournamentPlayer.computeDateFinish() - System.currentTimeMillis();
                if (wst.remainingTime < 0) {
                    wst.remainingTime = 0;
                }
            } else {
                wst.resultPlayer = duelTournamentPlayer.toWSResultTournamentPlayer(playerCache);
            }
            if (wst.resultPlayer != null) {
                wst.playerOffset = wst.resultPlayer.getRank();
            }
            return wst;
        }
        return null;
    }


    /**
     * List game for a player on a tournament (order by dealIndex asc)
     * @param tourID
     * @param playerID
     * @return
     */
    public List<DuelGame> listGameOnTournamentForPlayer(String tourID, long playerID){
        Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("playerID").is(playerID)));
        List<DuelGame> result = mongoTemplate.find(q, DuelGame.class);
        if (result != null) {
            Collections.sort(result, new Comparator<DuelGame>() {
                @Override
                public int compare(DuelGame o1, DuelGame o2) {
                    return Integer.compare(o1.getDealIndex(), o2.getDealIndex());
                }
            });
        }
        return result;
    }

    /**
     * Count game on tournament and player
     * @param tourID
     * @param playerID
     * @return
     */
    public List<DuelGame> listGameFinishedOnTournamentForPlayer(String tourID, long playerID) {
        Query q = new Query();
        Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
        Criteria cPlayerID = Criteria.where("playerID").is(playerID);
        Criteria cFinished = Criteria.where("finished").is(true);
        q.addCriteria(new Criteria().andOperator(cTourID, cPlayerID, cFinished));
        List<DuelGame> result = mongoTemplate.find(q, DuelGame.class);
        if (result != null) {
            Collections.sort(result, new Comparator<DuelGame>() {
                @Override
                public int compare(DuelGame o1, DuelGame o2) {
                    return Integer.compare(o1.getDealIndex(), o2.getDealIndex());
                }
            });
        }
        return result;
    }

    /**
     * Return the game not finished for a player on tournament.
     * @param tourID
     * @param playerID
     * @return
     */
    protected DuelGame getGameNotFinishedOnTournamentForPlayer(String tourID, long playerID) {
        List<DuelGame> listGame = listGameOnTournamentForPlayer(tourID, playerID);
        if (listGame != null) {
            for (DuelGame g : listGame) {
                if (!g.isFinished()) {
                    return g;
                }
            }
        }
        return null;
    }

    @Override
    public String getTournamentCategoryName() {
        return Constantes.TOURNAMENT_CATEGORY_NAME_DUEL;
    }

    @Override
    public DuelTournament getTournament(String tourID) {
        return mongoTemplate.findById(new ObjectId(tourID), DuelTournament.class);
    }

    @Override
    public boolean isBidAnalyzeEnable() {
        return false;
    }

    @Override
    public boolean isDealParEnable() {
        return false;
    }

    @Override
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    @Override
    public void updateTournamentDB(Tournament tour) throws FBWSException {
        if (tour != null && tour instanceof DuelTournament) {
            DuelTournament duelTour = (DuelTournament)tour;
            try {
                mongoTemplate.save(duelTour);
            } catch (Exception e) {
                log.error("Exception to save tour=" + duelTour, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.error("Tour not valid ! tour="+tour);
        }
    }

    /**
     * Add game in memory, finishDuel if all is played and send notifications
     * @param game
     */
    public DuelMemTournament updateMemoryDataForGame(DuelGame game) {
        if (game != null) {
            if (!game.isReplay()) {
                if (game.isFinished()) {
                    // update tournament play in memory
                    DuelMemTournament duelMemTournament = duelMemoryMgr.addGamePlayer(game);
                    if (duelMemTournament != null) {
                        // case of all deals played by players
                        if (duelMemTournament.isAllPlayed()) {
                            // player have all played => finish duel !
                            finishDuel(duelMemTournament.tourPlayerID);

                            DuelTournamentPlayer tournamentPlayer = getTournamentPlayer(duelMemTournament.tourPlayerID);
                            PlayerDuel playerDuel = getPlayerDuel(duelMemTournament.playerDuelID);
                            if (tournamentPlayer == null || playerDuel == null) {
                                log.error("Failed to retrieve tournamentPlayer or playerDuel in DB after finishDuel for ID="+duelMemTournament.tourPlayerID+" - tournamentPlayer="+tournamentPlayer+" - playerDuel="+playerDuel);
                            } else {
                                Player partner = playerDuel.getPartner(game.getPlayerID());
                                Player player = playerDuel.getPlayerWithID(game.getPlayerID());
                                // only for player or partner not Argine
                                if (!player.isArgine()) {
                                    // send event duel update
                                    if (getConfigIntValue("eventAtEndForCurrentPlayer", 1) == 1) {
                                        FBSession sessionPlayer = presenceMgr.getSessionForPlayerID(player.getID());
                                        if (sessionPlayer != null) {
                                            // push event to update duel history
                                            sessionPlayer.pushEvent(messageMgr.buildEventDuelUpdate(
                                                    player,
                                                    createDuelHistory(playerDuel, player.getID(), duelMemTournament, false),
                                                    buildDuelResult(duelMemTournament, player.getID())));
                                        }
                                    }
                                }
                                if (!partner.isArgine()) {
                                    // send notif for partner player who has already finished all game
                                    if (!partner.isArgine() && !player.isArgine()) {
                                        MessageNotif notifPartner = notifMgr.createNotifDuelFinishPlayAll(tournamentPlayer, partner, true, false);
                                        if (notifPartner != null) {
                                            FBSession sessionPartner = presenceMgr.getSessionForPlayerID(partner.getID());
                                            if (sessionPartner != null) {
                                                // push event for notif
                                                sessionPartner.pushEvent(notifMgr.buildEvent(notifPartner, partner));
                                                // push event to update duel history
                                                sessionPartner.pushEvent(messageMgr.buildEventDuelUpdate(
                                                        partner,
                                                        createDuelHistory(playerDuel, partner.getID(), duelMemTournament, false),
                                                        buildDuelResult(duelMemTournament, partner.getID())));
                                            }
                                        }
                                    }
                                }

                                if (!partner.isArgine() && !player.isArgine()) {
                                    // send notif to commons friend
                                    if (notifMgr.isNotifEnable()) {
                                        // check the flag notification friend duel is set for the two players
                                        List<Player> listCommonFriend = playerMgr.listCommonFriendForPlayers(game.getPlayerID(), partner.getID());
                                        if (listCommonFriend != null && listCommonFriend.size() > 0) {
                                            for (Player pFriend : listCommonFriend) {
                                                MessageNotif notifFriend = notifMgr.createNotifFriendDuelFinish(pFriend, tournamentPlayer);
                                                if (notifFriend != null) {
                                                    FBSession sessionFriend = presenceMgr.getSessionForPlayerID(pFriend.getID());
                                                    if (sessionFriend != null) {
                                                        // push event for notif
                                                        sessionFriend.pushEvent(notifMgr.buildEvent(notifFriend, pFriend));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // player has finish all deals and not the partner
                        else if (duelMemTournament.isAllPlayedForPlayer(game.getPlayerID())) {
                            DuelTournamentPlayer tournamentPlayer = getTournamentPlayer(duelMemTournament.tourPlayerID);
                            PlayerDuel playerDuel = getPlayerDuel(duelMemTournament.playerDuelID);
                            if (tournamentPlayer == null || playerDuel == null) {
                                log.error("Failed to retrieve tournamentPlayer or playerDuel in DB after finishDuel for ID="+duelMemTournament.tourPlayerID+" - tournamentPlayer="+tournamentPlayer+" - playerDuel="+playerDuel);
                            } else {
                                Player partner = playerDuel.getPartner(game.getPlayerID());
                                Player player = playerDuel.getPlayerWithID(game.getPlayerID());
                                if (partner != null && !partner.isArgine() && !player.isArgine()) {
                                    // send notif for partner to indicate player has finish all deals
                                    MessageNotif notif = notifMgr.createNotifDuelPartnerPlayAll(player, partner, duelMemTournament.dateFinish - System.currentTimeMillis(), tournamentPlayer.getTournament().getIDStr());
                                    if (notif != null) {
                                        FBSession sessionPartner = presenceMgr.getSessionForPlayerID(partner.getID());
                                        if (sessionPartner != null) {
                                            sessionPartner.pushEvent(notifMgr.buildEvent(notif, partner));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return duelMemTournament;
                } else {
                    log.error("Game is not finished");
                }
            } else {
                log.error("Game is replay mode !!");
            }
        } else {
            log.error("Game is null");
        }
        return null;
    }

    @Override
    public void updateGameFinished(FBSession session) throws FBWSException {
        if (session == null) {
            log.error("Session parameter is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (session.getCurrentGameTable() == null || session.getCurrentGameTable().getGame() == null || !(session.getCurrentGameTable().getGame() instanceof DuelGame)) {
            log.error("Table or game in session not valid ! - table="+session.getCurrentGameTable());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        DuelGame game = (DuelGame)session.getCurrentGameTable().getGame();
        if (!game.isReplay()) {
            if (game.isFinished()) {
                // update data game and table in DB
                updateGameDB(game);

                DuelMemTournament duelMemTournament = updateMemoryDataForGame(game);

                // remove game from session
                session.removeGame();
                // remove player from set
                gameMgr.removePlayerRunning(session.getPlayer().getID());

                if (duelMemTournament != null) {
                    if (duelMemTournament.isDuelWithArgine() && duelMemTournament.isAllPlayed()) {
                        removeDuelArgineInProgress(duelMemTournament.tourID);
                    }
                }
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
    public void updateGamePlayArgineFinished(Game game) throws FBWSException {
        if (game instanceof DuelGame) {
            DuelGame duelGame = (DuelGame) game;
            if (duelGame.isFinished()) {
                // update data game and table in DB
                updateGameDB(duelGame);

                if (duelGame.isArginePlayAlone()) {
                    gameMgr.removeArgineGameRunning(duelGame.getIDStr());
                    if (duelGame.getDealIndex() < game.getTournament().getNbDeals()) {
                        startPlayArgineAlone((DuelTournament)game.getTournament());
                    }
                } else {
                    DuelMemTournament duelMemTournament = updateMemoryDataForGame(duelGame);
                    gameMgr.removeArgineGameRunning(duelGame.getIDStr());
                    if (duelMemTournament != null) {
                        if (!duelMemTournament.isAllPlayedForPlayer(Constantes.PLAYER_ARGINE_ID)) {
                            startPlayArgine(duelMemTournament.tourID);
                        } else {
                            if (duelMemTournament.isAllPlayed()) {
                                removeDuelArgineInProgress(duelMemTournament.tourID);
                            }
                        }
                    }
                }

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Nothing to do ... game not finished - game=" + game);
                }
            }
        } else {
            log.error("Game not instance of DuelGame => game="+game);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void checkGame(Game game) throws FBWSException {
        if (game != null) {
            if (!game.isReplay()) {
                if (game.getTournament().isFinished()) {
                    throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
                }
                if (!game.getTournament().isDateValid(System.currentTimeMillis())) {
                    throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
                }
                if (getConfigIntValue("checkDuel", 1) == 1) {
                    checkDuel((DuelGame)game);
                }
            }
        } else {
            log.error("Tournament is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Check duel is not finished (or expired). A duelMemTournament must be present !!
     * @param game
     * @throws FBWSException
     */
    public void checkDuel(DuelGame game) throws FBWSException {
        if (game != null) {
            if (!game.isReplay()) {
                if (game.getTournamentPlayerID() != null) {
                    DuelMemTournament duelMemTournament = duelMemoryMgr.getDuelMemTournament(game.getTournamentPlayerID());
                    if (duelMemTournament == null) {
                        log.error("No duelMemTournament for tournamentPlayerID=" + game.getTournamentPlayerID() + " - game=" + game);
                        throw new FBWSException(FBExceptionType.TOURNAMENT_FINISHED);
                    } else if (duelMemTournament.isExpired()) {
                        throw new FBWSException(FBExceptionType.TOURNAMENT_FINISHED);
                    }
                }
            }
        } else {
            log.error("Game is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Return the game with this ID
     * @param gameID
     * @return
     */
    @Override
    public DuelGame getGame(String gameID) {
        return mongoTemplate.findById(new ObjectId(gameID), DuelGame.class);
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
        DuelGame game = getGame(gameID);
        if (game == null) {
            log.error("GameID not found - gameID="+gameID+" - player="+player);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // check player has played this deal
        if ((game.getPlayerID() != player.getID()) && getGameOnTournamentAndDealForPlayer(game.getTournament().getIDStr(), game.getDealIndex(), player.getID()) == null) {
            log.error("Deal not played by player - gameID=" + gameID + " - playerID=" + player.getID() + " - tour=" + game.getTournament() + " - deal index=" + game.getDealIndex());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (game.getPlayerID() == player.getID() && !game.isFinished()) {
            log.error("Deal not finished by player - gameID="+gameID+" - playerID="+player.getID());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        WSTableGame table = new WSTableGame();
        table.tableID = -1;
        table.playerSouth = WSGamePlayer.createGamePlayerHuman(game.getPlayerID() == player.getID() ? player : playerMgr.getPlayer(game.getPlayerID()), Constantes.PLAYER_STATUS_PRESENT, player.getID());
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
        DuelGame replayGame = new DuelGame(player.getID(), (DuelTournament)gamePlayed.getTournament(), gamePlayed.getDealIndex());
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
     * Delete duel for list of playerID : playerDuel & tournamentDuel
     * @param listPlaID
     * @return
     */
    public boolean deleteDataForPlayerList(List<Long> listPlaID) {
        for (Long playerID : listPlaID) {
            List<PlayerDuel> listDuel = playerDuelDAO.listDuelForPlayer(playerID);
            if (listDuel != null && listDuel.size() > 0) {
                for (PlayerDuel pd : listDuel) {
                    DuelMemTournament duelMemTournament = duelMemoryMgr.getDuelMemTournamentForPlayers(pd.getPlayer1().getID(), pd.getPlayer2().getID());
                    if (duelMemTournament != null) {
                        duelMemoryMgr.removeMemTournament(duelMemTournament.tourPlayerID);
                    }
                    playerDuelDAO.removePlayerDuel(pd.getID());
                    mongoTemplate.remove(new Query(Criteria.where("playerDuelID").is(pd.getID())), DuelTournamentPlayer.class);
                }
            }
            mongoTemplate.remove(new Query(Criteria.where("playerID").is(playerID)), DuelGame.class);
        }
        return true;
    }

    /**
     * List DuelTournamentPlayer with finishDate = 0
     * @return
     */
    public List<DuelTournamentPlayer> listTournamentPlayerNotFinish() {
        Query q = new Query(Criteria.where("finishDate").is(0));
        return mongoTemplate.find(q, DuelTournamentPlayer.class);
    }

    /**
     * Leave the current tournament. All deals not played are set to leave
     * @param session
     * @param tournamentID
     * @return
     * @throws FBWSException
     */
    public int leaveTournament(FBSession session, String tournamentID) throws FBWSException {
        if (session == null) {
            log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        Table table = session.getCurrentGameTable();
        if (table == null || table.getGame() == null) {
            log.error("Game or table is null in session table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (table.getTournament() == null || !(table.getTournament() instanceof DuelTournament)) {
            log.error("Touranment on table is not DUEL table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!(table.getGame() instanceof DuelGame)) {
            log.error("Game on table is not DUEL table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        DuelGame game = (DuelGame)table.getGame();
        if (!table.getTournament().getIDStr().equals(tournamentID)) {
            log.error("TournamentID  of current game ("+table.getTournament().getIDStr()+") is not same as tournamentID="+tournamentID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }

        checkGame(game);
        Player p = session.getPlayer();
        DuelTournament tour = game.getTournament();
        DuelTournamentPlayer duelTournamentPlayer = getTournamentPlayer(tournamentID, p.getID());
        if (duelTournamentPlayer == null) {
            log.error("No duelTournamentPlayer found for this tournament="+tournamentID+" - player="+p.getID());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        int nbGameFinished = countGameFinishedOnTournamentAndPlayer(tour.getIDStr(), p.getID());
        // check if player has enough credit to leave tournament
        int nbDealToLeave = tour.getNbDeals() - nbGameFinished - 1; // -1 because credit already decrement for current game !
        if (nbDealToLeave > 0) {
            playerMgr.checkPlayerCredit(p, tour.getNbCreditsPlayDeal() * nbDealToLeave);
        }

        // leave current game
        synchronized (getGameMgr().getLockOnGame(game.getIDStr())) {
            if (!game.isFinished()) {
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
                // update memory
                updateMemoryDataForGame(game);
            }
        }
        // leave game for other deals
        int nbDealNotPlayed = 0;
        for (int i= 1; i<= tour.getNbDeals(); i++) {
            if (getGameOnTournamentAndDealForPlayer(tournamentID, i, p.getID()) == null) {
                DuelGame g = new DuelGame(p.getID(), tour, i);
                g.setTournamentPlayerID(duelTournamentPlayer.getIDStr());
                g.setStartDate(System.currentTimeMillis());
                g.setLastDate(System.currentTimeMillis());
                g.setLeaveValue();
                g.setDeviceID(session.getDeviceID());
                // insert game in DB
                mongoTemplate.insert(g);
                // update memory
                updateMemoryDataForGame(g);
                table.addPlayedDeal(g.getDealID());
                nbDealNotPlayed++;
            }
        }

        if (tour.isArgineDuel()) {
            DuelArgineInProgress duelArgineInProgress = getDuelArgineInProgressForTournament(tour.getIDStr());
            if (duelArgineInProgress != null && duelArgineInProgress.argineFinish) {
                removeDuelArgineInProgress(tour.getIDStr());
            }
        }
        // remove game from session
        session.removeGame();
        // remove player from set
        gameMgr.removePlayerRunning(p.getID());

        // update player data
        if (nbDealNotPlayed > 0) {
            playerMgr.updatePlayerCreditDeal(p, tour.getNbCreditsPlayDeal()*nbDealNotPlayed, nbDealNotPlayed);
            session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_DUEL, nbDealNotPlayed);
        }

        // return credit remaining of player
        return p.getCreditAmount();
    }

    public <T extends Game> void updateListGameDB(List<T> listGame) {
        for(T game : listGame){
            try {
                updateGameDB(game);
            } catch (Exception e) {
                log.error("Failed to update game in DB - game="+game, e);
            }
        }
    }

    public List<DuelTournamentPlayer> listTournamentPlayer(long playerID, int offset, int nbMax) {
        return mongoTemplate.find(
                Query.query(new Criteria().orOperator(Criteria.where("player1ID").is(playerID), Criteria.where("player2ID").is(playerID))).
                        skip(offset).
                        limit(nbMax).
                        with(new Sort(Sort.Direction.DESC, "creationDate")),
                DuelTournamentPlayer.class);
    }

    public DuelTournamentPlayer getLastTournamentPlayer(long playerDuelID) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("playerDuelID").is(playerDuelID)).with(new Sort(Sort.Direction.DESC, "creationDate")),
                DuelTournamentPlayer.class);
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
        if (session.getCurrentGameTable() == null) {
            log.error("No gameTable in session !!");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        DuelGame replayGame = (DuelGame) session.getCurrentGameTable().getGame();
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
        DuelGame originalGame = getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        if (originalGame == null) {
            log.error("Original game not found ! dealID="+dealID+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }

        int nbTotalPlayer = -1;
        double resultOriginal = 0;
        int rankOriginal = 0;
        DuelTournamentPlayer duelTournamentPlayer = getTournamentPlayer(tourID, playerID);
        if (duelTournamentPlayer == null) {
            log.error("No duelTournamentPlayer found for tourID="+tourID+" - playerID="+playerID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        int scorePartner = 0;
        boolean gamePartnerExist = false;
        double resultPartner = 0;
        DuelGame gameOther = getGameOnTournamentAndDealForPlayer(duelTournamentPlayer.getTournament().getIDStr(), originalGame.getDealIndex(), duelTournamentPlayer.getPartner(playerID));
        if (gameOther != null) {
            scorePartner = gameOther.getScore();
            gamePartnerExist = true;
            resultPartner = gameOther.getResult();
        }
        DuelMemTournament duelMemTournament = duelMemoryMgr.getDuelMemTournament(duelTournamentPlayer.getIDStr());
        if (duelMemTournament != null) {
            DuelMemDeal duelMemDeal = duelMemTournament.getMemDeal(originalGame.getDealIndex());
            if (duelMemDeal == null) {
                log.error("No memDeal found for dealID="+dealID+" - on duelMemTournament="+duelMemTournament);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            DuelMemGame memGame = duelMemDeal.getForPlayer(playerID);
            if (memGame == null) {
                log.error("No memGame found for player="+playerID+" on duelMemTournament="+duelMemTournament+" - duelMemDeal="+duelMemDeal+" - replayGame="+replayGame);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            nbTotalPlayer = duelMemDeal.getNbScore();
            resultOriginal = memGame.result;
            rankOriginal = memGame.getRank();
            DuelMemGame memGamePartner = duelMemDeal.getForPlayer(duelTournamentPlayer.getPartner(playerID));
            if (memGamePartner != null) {
                resultPartner = memGame.result;
            }
        } else {
            nbTotalPlayer = 2;
            resultOriginal = originalGame.getResult();
            rankOriginal = originalGame.getRank();
        }

        if (gamePartnerExist) {
            if (resultOriginal == 0) {
                resultOriginal = -resultPartner;
            }
        }

        // result original
        WSResultDeal resultPlayer = new WSResultDeal();
        resultPlayer.setDealIDstr(dealID);
        resultPlayer.setDealIndex(dealIndex);
        resultPlayer.setResultType(Constantes.TOURNAMENT_RESULT_IMP);
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
        resultReplay.setResultType(Constantes.TOURNAMENT_RESULT_IMP);
        resultReplay.setNbTotalPlayer(nbTotalPlayer); // + 1 to count replay game !
        resultReplay.setContract(replayGame.getContractWS());
        resultReplay.setDeclarer(Character.toString(replayGame.getDeclarer()));
        resultReplay.setNbTricks(replayGame.getTricks());
        resultReplay.setScore(replayGame.getScore());
        resultReplay.setLead(replayGame.getBegins());
        if (gamePartnerExist) {
            if (replayGame.getScore() >= scorePartner) {
                resultReplay.setRank(1);
            } else {
                resultReplay.setRank(2);
            }
            if (scorePartner != Constantes.GAME_SCORE_LEAVE) {
                double temp = computeResultDealPlayer((scorePartner+resultReplay.getScore())/2, resultReplay.getScore());
                resultReplay.setResult(temp);
            } else {
                resultReplay.setResult(15);
            }
        } else {
            resultReplay.setRank(-1);
            resultReplay.setResult(0);
        }

        WSResultReplayDealSummary replayDealSummary = new WSResultReplayDealSummary();
        replayDealSummary.setResultPlayer(resultPlayer);
        replayDealSummary.setResultMostPlayed(resultPlayer);
        replayDealSummary.setResultReplay(resultReplay);
        replayDealSummary.getAttributes().add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, ""+nbTotalPlayer));
        replayDealSummary.getAttributes().add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_RESULT_TYPE, ""+Constantes.TOURNAMENT_RESULT_IMP));
        return replayDealSummary;
    }

    /**
     * Remove duel from memory and optionnaly all data
     * @param tournamentPlayerID
     * @param removeGamePlayer1
     * @param removeGamePlayer2
     * @param resetPlayerDuel
     */
    public void removeDuel(String tournamentPlayerID, boolean removeGamePlayer1, boolean removeGamePlayer2, boolean resetPlayerDuel) {
        DuelTournamentPlayer tourPlayer = getTournamentPlayer(tournamentPlayerID);
        if (tourPlayer != null) {
            if (resetPlayerDuel) {
                PlayerDuel playerDuel = getPlayerDuel(tourPlayer.getPlayerDuelID());
                if (playerDuel != null) {
                    playerDuel.setStatus(Constantes.PLAYER_DUEL_STATUS_NONE);
                    playerDuelDAO.updatePlayerDuel(playerDuel);
                }
            }
            if (removeGamePlayer1) {
                Query q = Query.query(
                        Criteria.where("tournament.$id").is(new ObjectId(tourPlayer.getTournament().getIDStr())).andOperator(
                                Criteria.where("playerID").is(tourPlayer.getPlayer1ID())));
                mongoTemplate.remove(q, DuelGame.class);
            }
            if (removeGamePlayer2) {
                Query q = Query.query(
                        Criteria.where("tournament.$id").is(new ObjectId(tourPlayer.getTournament().getIDStr())).andOperator(
                                Criteria.where("playerID").is(tourPlayer.getPlayer2ID())));
                mongoTemplate.remove(q, DuelGame.class);
            }
            duelMemoryMgr.removeMemTournament(tournamentPlayerID);
            mongoTemplate.remove(tourPlayer);
        }
    }

    /**
     * Set status for player duel to NONE
     * @param playerDuelID
     * @return
     */
    public boolean resetPlayerDuel(long playerDuelID) {
        PlayerDuel playerDuel = getPlayerDuel(playerDuelID);
        if (playerDuel != null) {
            playerDuel.setStatus(Constantes.PLAYER_DUEL_STATUS_NONE);
            playerDuelDAO.updatePlayerDuel(playerDuel);
            return true;
        }
        return false;
    }

    /**
     * Remove game for player on tournament
     * @param tourID
     * @param playerID
     */
    public void removeGameForPlayerOnTournament(String tourID, long playerID) {
        mongoTemplate.remove(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("playerID").is(playerID))));
    }

    /**
     * Remove tournamentPlayer object from DB
     * @param tournamentPlayerID
     * @return
     */
    public boolean removeTournamentPlayer(String tournamentPlayerID) {
        DuelTournamentPlayer tourPlayer = getTournamentPlayer(tournamentPlayerID);
        if (tourPlayer != null) {
            mongoTemplate.remove(tourPlayer);
            return true;
        }
        return false;
    }

    public List<WSPlayerDuel> listDuelsForPlayer(long playerID, boolean excludeArgine, int offset, int nbMax) {
        List<WSPlayerDuel> results = playerDuelDAO.listDuelsForPlayer(playerID, excludeArgine, offset, nbMax);
        if (results != null) {
            for (WSPlayerDuel e : results) {
                if (presenceMgr.isSessionForPlayerID(e.playerID)) {
                    e.connected = true;
                }
            }
        }
        return results;
    }

    /**
     * count duel with nb played > 0
     * @param playerID
     * @param excludeArgine
     * @return
     */
    public int countDuelsForPlayerWithNbPlayedSup0(long playerID, boolean excludeArgine) {
        return playerDuelDAO.countDuelsForPlayerWithNbPlayedSup0(playerID, excludeArgine);
    }

    /**
     * Start thread for argine to play tournament
     * @param tourID
     * @throws FBWSException
     */
    public boolean startPlayArgine(String tourID) throws FBWSException {
        DuelTournamentPlayer duelTournamentPlayer = getTournamentPlayer(tourID, Constantes.PLAYER_ARGINE_ID);
        if (duelTournamentPlayer == null) {
            log.error("duelTournamentPlayer is null - tourID="+tourID);
            return false;
        }
        if (duelTournamentPlayer.isExpired() || duelTournamentPlayer.isFinished()) {
            return false;
        }
        DuelMemTournament memTour = duelMemoryMgr.getDuelMemTournament(duelTournamentPlayer.getIDStr());
        if (memTour == null) {
            log.error("No duelMemTournament found for duelTournamentPlayer="+duelTournamentPlayer);
            return false;
        }
        DuelTournament tournament = duelTournamentPlayer.getTournament();
        if (!duelTournamentPlayer.isPlayerDuel(Constantes.PLAYER_ARGINE_ID)) {
            log.error("Player is not defined on duel="+duelTournamentPlayer);
            return false;
        }
        if (memTour.isAllPlayedForPlayer(Constantes.PLAYER_ARGINE_ID)) {
            log.error("All deals played for playerArgine - memTour="+memTour);
            return false;
        }
        // retrieve game if exist
        DuelGame game = getGameNotFinishedOnTournamentForPlayer(tournament.getIDStr(), Constantes.PLAYER_ARGINE_ID);

        //*******************
        // retrieve game
        if (game == null) {
            // need to retrieve an existing game
            int lastIndexPlayed = 0;
            List<DuelGame> listGame = listGameOnTournamentForPlayer(tournament.getIDStr(), Constantes.PLAYER_ARGINE_ID);
            if (listGame != null) {
                for (DuelGame g : listGame) {
                    // BUG with game finished, contractType=0 (PA) and bids = ""
                    if (g.isInitFailed() && g.isFinished() && g.getContractType() == 0 && g.getBids().length() == 0) {
                        if (getConfigIntValue("fixBugGameNotValidOnPlayTournament", 0) == 1) {
                            log.error("BugGameNotValid - init failed => Reset game data g="+g);
                            g.resetData();
                            g.setConventionSelection(getPlayArgineConventionBidsProfil(), getPlayArgineConventionBidsValue());
                            g.setDeviceID(Constantes.DEVICE_ID_PLAYER_ARGINE);
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
                int nbDealPlayedMemory = memTour.getNbDealPlayed(Constantes.PLAYER_ARGINE_ID);
                // need to create a new game
                if (lastIndexPlayed >= tournament.getNbDeals()) {
                    log.error("lastIndexPlayed to big to play another deal ! playerArgine - lastIndexPlayed="+lastIndexPlayed+" - tournament="+tournament);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                // create game
                game = createGame(duelTournamentPlayer, Constantes.PLAYER_ARGINE_ID, lastIndexPlayed+1, getPlayArgineConventionBidsProfil(), getPlayArgineConventionBidsValue(), getPlayArgineConventionCardsProfil(), getPlayArgineConventionCardsValue(), Constantes.DEVICE_ID_PLAYER_ARGINE);
                gameMgr.addArgineGameRunning(game);
            }
        }

        setDuelArgineInProgress(game, memTour.tourPlayerID, memTour.getOtherPlayer(Constantes.PLAYER_ARGINE_ID));

        gameMgr.startThreadPlayArgine(game);
        return true;
    }

    /**
     * Start thread for argine to play tournament
     * @param tournament
     * @throws FBWSException
     */
    public void startPlayArgineAlone(DuelTournament tournament) throws FBWSException {
        //*******************
        // retrieve game not finished on tournament
        DuelGame game = getGameNotFinishedOnTournamentForPlayer(tournament.getIDStr(), Constantes.PLAYER_ARGINE_ID);
        if (game == null) {
            // need to retrieve an existing game
            int lastIndexPlayed = 0;
            List<DuelGame> listGame = listGameOnTournamentForPlayer(tournament.getIDStr(), Constantes.PLAYER_ARGINE_ID);
            if (listGame != null) {
                for (DuelGame g : listGame) {
                    // BUG with game finished, contractType=0 (PA) and bids = ""
                    if (g.isInitFailed() && g.isFinished() && g.getContractType() == 0 && g.getBids().length() == 0) {
                        if (getConfigIntValue("fixBugGameNotValidOnPlayTournament", 0) == 1) {
                            log.error("BugGameNotValid - init failed => Reset game data g="+g);
                            g.resetData();
                            g.setConventionSelection(getPlayArgineConventionBidsProfil(), getPlayArgineConventionBidsValue());
                            g.setDeviceID(Constantes.DEVICE_ID_PLAYER_ARGINE);
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
                // need to create a new game
                if (lastIndexPlayed >= tournament.getNbDeals()) {
                    log.error("lastIndexPlayed to big to play another deal ! playerArgine - lastIndexPlayed="+lastIndexPlayed+" - tournament="+tournament);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                // create game
                game = createGameArgineAlone(tournament, Constantes.PLAYER_ARGINE_ID, lastIndexPlayed+1, getPlayArgineConventionBidsProfil(), getPlayArgineConventionBidsValue(), getPlayArgineConventionCardsProfil(), getPlayArgineConventionCardsValue(), Constantes.DEVICE_ID_PLAYER_ARGINE);
                gameMgr.addArgineGameRunning(game);
            }
        } else {
            // game not present or last date is too old ! => add game in map
            if (gameMgr.getArgineGameRunning(game.getIDStr()) == null || gameMgr.getArgineGameRunning(game.getIDStr()).getLastDate() < (System.currentTimeMillis() - 10*Constantes.TIMESTAMP_MINUTE)) {
                gameMgr.addArgineGameRunning(game);
            }
        }

        setDuelArgineInProgress(game, null, 0);

        gameMgr.startThreadPlayArgine(game);
    }

    public int getPlayArgineConventionBidsProfil() {
        return getConfigIntValue("playArgineConventionBidsProfil", 5);
    }

    public String getPlayArgineConventionBidsValue() {
        return getConfigStringValue("playArgineConventionBidsValue", "");
    }

    public int getPlayArgineConventionCardsProfil() {
        return getConfigIntValue("playArgineConventionCardsProfil", 1);
    }

    public String getPlayArgineConventionCardsValue() {
        return getConfigStringValue("playArgineConventionCardsValue", "");
    }

    public DuelArgineInProgress getDuelArgineInProgressForTournament(String tourID) {
        return mongoTemplate.findOne(Query.query(Criteria.where("tourID").is(tourID)), DuelArgineInProgress.class);
    }

    public DuelArgineInProgress findAndReserveDuelArgineInProgress(long playerID) {
        Criteria cTourNotExpired = Criteria.where("tournamentExpirationDate").gt(System.currentTimeMillis() + getDuelDuration() + 5*Constantes.TIMESTAMP_MINUTE);
        Criteria cNotUsed = Criteria.where("playerID").is(0);
        Criteria cArgineFinish = Criteria.where("argineFinish").is(true);
        Query q = new Query();
        q.addCriteria(new Criteria().andOperator(cNotUsed, cTourNotExpired, cArgineFinish));
        DuelArgineInProgress duelFound = mongoTemplate.findOne(q, DuelArgineInProgress.class);
        if (duelFound != null) {
            duelFound.playerID = playerID;
            mongoTemplate.save(duelFound);
        }
        return duelFound;
    }

    public List<DuelArgineInProgress> listDuelArgineInProgress(boolean onlyFree, int offset, int nbMax) {
        Query q = new Query();
        Criteria cTourNotExpired = Criteria.where("tournamentExpirationDate").gt(System.currentTimeMillis() + getDuelDuration() + 5*Constantes.TIMESTAMP_MINUTE);
        if (onlyFree) {
            Criteria cNotUsed = Criteria.where("playerID").is(0);
            Criteria cArgineFinish = Criteria.where("argineFinish").is(true);
            q.addCriteria(new Criteria().andOperator(cNotUsed, cTourNotExpired, cArgineFinish));
        } else {
            q.addCriteria(cTourNotExpired);
        }
        q.with(new Sort(Sort.Direction.ASC, "dateCreation"));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return mongoTemplate.find(q, DuelArgineInProgress.class);
    }

    public int countDuelArgineInProgress(boolean onlyFree, boolean argineFinish) {
        Query q = new Query();
        Criteria cTourNotExpired = Criteria.where("tournamentExpirationDate").gt(System.currentTimeMillis() + getDuelDuration() + 5*Constantes.TIMESTAMP_MINUTE);
        if (onlyFree) {
            Criteria cNotUsed = Criteria.where("playerID").is(0);
            Criteria cArgineFinish = Criteria.where("argineFinish").is(argineFinish);
            q.addCriteria(new Criteria().andOperator(cNotUsed, cTourNotExpired, cArgineFinish));
        } else {
            q.addCriteria(cTourNotExpired);
        }
        return (int)mongoTemplate.count(q, DuelArgineInProgress.class);
    }

    public List<DuelArgineInProgress> listDuelArgineInProgressWithNoUpdate(int nbMax) {
        Query q = new Query();
        Criteria cTourNoUpdate = Criteria.where("dateLastUpdate").lt(System.currentTimeMillis() - 5*Constantes.TIMESTAMP_MINUTE);
        Criteria cArgineFinish = Criteria.where("argineFinish").is(false);
        q.addCriteria(new Criteria().andOperator(cTourNoUpdate, cArgineFinish));
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        q.with(new Sort(Sort.Direction.ASC, "dateLastUpdate"));
        return mongoTemplate.find(q, DuelArgineInProgress.class);
    }

    public int countDuelArgineInProgressWithNoUpdate() {
        Query q = new Query();
        Criteria cTourNoUpdate = Criteria.where("dateLastUpdate").lt(System.currentTimeMillis() - 5*Constantes.TIMESTAMP_MINUTE);
        Criteria cArgineFinish = Criteria.where("argineFinish").is(false);
        q.addCriteria(new Criteria().andOperator(cTourNoUpdate, cArgineFinish));
        return (int)mongoTemplate.count(q, DuelArgineInProgress.class);
    }

    public int countDuelArgineInProgressWithTournamentExpired() {
        return (int)mongoTemplate.count(Query.query(Criteria.where("tournamentExpirationDate").lt(System.currentTimeMillis())), DuelArgineInProgress.class);
    }

    public void removeDuelArgineInProgressWithTournamentExpired() {
        mongoTemplate.remove(Query.query(Criteria.where("tournamentExpirationDate").lt(System.currentTimeMillis())), DuelArgineInProgress.class);
    }

    /**
     * Update DuelArgineInProgress data with this games. playerID is the partner of Argine on this duel. tourPlayerID can be null and playerID can be 0.
     * @param game
     * @param tourPlayerID
     * @param playerID
     * @return
     */
    public DuelArgineInProgress setDuelArgineInProgress(Game game, String tourPlayerID, long playerID) {
        if (game != null) {
            DuelArgineInProgress duelArgineInProgress = getDuelArgineInProgressForTournament(game.getTournament().getIDStr());
            if (duelArgineInProgress == null) {
                duelArgineInProgress = new DuelArgineInProgress();
                duelArgineInProgress.tourID = game.getTournament().getIDStr();
                duelArgineInProgress.tournamentExpirationDate = game.getTournament().getEndDate();
                duelArgineInProgress.dateCreation = System.currentTimeMillis();
            }
            if (tourPlayerID != null) {
                duelArgineInProgress.duelTourPlayerID = tourPlayerID;
            }
            if (playerID > 0) {
                duelArgineInProgress.playerID = playerID;
            }
            duelArgineInProgress.dateLastUpdate = System.currentTimeMillis();
            if (game.isFinished()) {
                duelArgineInProgress.listDealsPlayedByArgine.add(game.getDealID());
                if (duelArgineInProgress.countDealsPlayedByArgine() == game.getTournament().getNbDeals()) {
                    duelArgineInProgress.argineFinish = true;
                }
            } else {
                duelArgineInProgress.currentDealIndexArgine = game.getDealIndex();
            }

            // save data in DB
            if (duelArgineInProgress.ID == null) {
                mongoTemplate.insert(duelArgineInProgress);
            } else {
                mongoTemplate.save(duelArgineInProgress);
            }
            return duelArgineInProgress;
        } else {
            log.error("Parameter game is null !!!");
        }
        return null;
    }

    public void removeDuelArgineInProgress(String tourID) {
        mongoTemplate.remove(Query.query(Criteria.where("tourID").is(tourID)), DuelArgineInProgress.class);
    }

    /**
     * save game each 4 steps and update duelArgineInProgress object
     * @param game
     */
    public void updateGamePlayArgine(Game game) {
        if (game != null &&  game.getPlayerID() == Constantes.PLAYER_ARGINE_ID) {
            if ((game.getStep()%4 == 0) && getConfigIntValue("saveGamePlayArgineInDB", 0) == 1) {
                mongoTemplate.save(game);
            }
            setDuelArgineInProgress(game, null, 0);
        }
    }

    public void updateAllGameArgineInProgress(boolean clearMapGame) {
        List<DuelGame> listGame = new ArrayList<>();
        for (Game g : gameMgr.getMapArgineGameRunning().values()) {
            listGame.add((DuelGame)g);
        }
        if (listGame.size() > 0) {
            updateListGameDB(listGame);
        }
        if (clearMapGame) {
            gameMgr.getMapArgineGameRunning().clear();
        }
    }

    /**
     * Create 1 tournament for argine Duel and start process play for Argine
     * @return
     */
    public DuelTournament createArgineDuelAndStartPlay() {
        DuelTournament tour = generateTournament(true);
        if (tour != null) {
            try {
                startPlayArgineAlone(tour);
            } catch (Exception e) {
                log.error("Exception to startPlayArgineAlone for tournament="+tour, e);
            }
        }
        return tour;
    }

    public void runThreadArgineDuelInProgress() {
        if (!presenceMgr.isServiceMaintenance()) {
            if (!argineDuelTaskRunning) {
                argineDuelTaskRunning = true;
                try {
                    // check nb duel free
                    int nbDuelFreeLimit = getConfigIntValue("duelArgineThread.nbFreeLimit", 100);
                    int nbDuelFreeReady = countDuelArgineInProgress(true, true);
                    if (nbDuelFreeReady < nbDuelFreeLimit) {
                        int nbDuelFreeNotReady = countDuelArgineInProgress(true, false);
                        if (nbDuelFreeNotReady < getConfigIntValue("duelArgineThread.nbFreeNotReadyLimit", 10)) {
                            int nbDuelFreeNew = getConfigIntValue("duelArgineThread.nbFreeNew", 10);
                            for (int i = 0; i < nbDuelFreeNew; i++) {
                                createArgineDuelAndStartPlay();
                            }
                            log.warn("Nb duelAgine created = " + nbDuelFreeNew);
                        } else {
                            log.warn("Nb duelAgine freeNotReady = " + nbDuelFreeNotReady+" => too many duelArgine in progress, wait !");
                        }

                    }
                } catch (Exception e) {
                    log.error("Exception to check nb duel free !", e);
                }

                // check duels with no update and need to restart
                try {
                    int nbMaxWithNoUpdate = getConfigIntValue("duelArgineThread.nbMaxWithNoUpdate", 10);
                    if (nbMaxWithNoUpdate > 0) {
                        List<DuelArgineInProgress> duelsNoUpdate = listDuelArgineInProgressWithNoUpdate(nbMaxWithNoUpdate);
                        for (DuelArgineInProgress e : duelsNoUpdate) {
                            if (countGameFinishedOnTournamentAndPlayer(e.tourID, Constantes.PLAYER_ARGINE_ID) == 5) {
                                e.argineFinish = true;
                                mongoTemplate.save(e);
                            }
                            else {
                                DuelTournament tour = getTournament(e.tourID);
                                if (tour != null) {
                                    try {
                                        startPlayArgineAlone(tour);
                                    } catch (Exception ex) {
                                        log.error("Exception to startPlayArgineAlone for tournament=" + tour, ex);
                                    }
                                }
                            }
                        }
                        log.warn("Nb duelArgine with noUpdate restart = " + duelsNoUpdate.size());
                    }
                    log.warn("Nb duelArgine with noUpdate  = " + countDuelArgineInProgressWithNoUpdate());
                } catch (Exception e) {
                    log.error("Exception to check duels with no update and need to restart !", e);
                }

                // remove old duels
                try {
                    removeDuelArgineInProgressWithTournamentExpired();
                } catch (Exception e) {
                    log.error("Exception to removeDuelArgineInProgressWithTournamentExpired !", e);
                }
                argineDuelTaskRunning = false;
            } else {
                log.error("Task already running !");
            }
        }
    }

    public DuelArgineStat getDuelArgineStat(long playerID) {
        return mongoTemplate.findById(playerID, DuelArgineStat.class);
    }

    /**
     * Update duelArgineStat for player. If stat not existing, create it
     * @param duelMemTournament
     */
    public void updateDuelArgineStat(DuelMemTournament duelMemTournament) {
        if (duelMemTournament != null) {
            DuelArgineStat argineStat = getDuelArgineStat(duelMemTournament.getOtherPlayer(Constantes.PLAYER_ARGINE_ID));
            boolean newdata = false;
            if (argineStat == null) {
                newdata = true;
                argineStat = new DuelArgineStat();
                argineStat.playerID = duelMemTournament.getOtherPlayer(Constantes.PLAYER_ARGINE_ID);
                argineStat.dateFirstDuel = duelMemTournament.dateStart;
            }
            argineStat.dateLastDuel = duelMemTournament.dateStart;
            long playerWinner = duelMemTournament.getWinner();
            int result = 0;
            if (playerWinner == Constantes.PLAYER_ARGINE_ID) { result = -1;}
            else if (playerWinner == argineStat.playerID) { result = 1;}
            argineStat.addResult(result);

            if (newdata) {
                mongoTemplate.insert(argineStat);
            } else {
                mongoTemplate.save(argineStat);
            }

            if (log.isDebugEnabled()) {
                log.debug("Argine stat update = "+argineStat);
            }
        }
    }

    public boolean reloadDuelInMemory(String tournamentPlayerID) {
        DuelTournamentPlayer duelTournamentPlayer = getTournamentPlayer(tournamentPlayerID);
        DuelMemTournament duelMemTournament = duelMemoryMgr.getDuelMemTournament(tournamentPlayerID);
        if (duelTournamentPlayer != null && duelMemTournament != null) {
            presenceMgr.closeSessionForPlayer(duelTournamentPlayer.getPlayer1ID(), null);
            presenceMgr.closeSessionForPlayer(duelTournamentPlayer.getPlayer2ID(), null);
            // remove from memory
            duelMemoryMgr.removeMemTournament(tournamentPlayerID);

            // create mem tournament
            duelMemoryMgr.addTournamentPlayer(duelTournamentPlayer);

            // add game for player1
            List<DuelGame> listGame1 = listGameOnTournamentForPlayer(duelTournamentPlayer.getTournament().getIDStr(), duelTournamentPlayer.getPlayer1ID());
            for (DuelGame g : listGame1) {
                if (g.isFinished()) {
                    updateMemoryDataForGame(g);
                }
            }
            // add game for player2
            List<DuelGame> listGame2 = listGameOnTournamentForPlayer(duelTournamentPlayer.getTournament().getIDStr(), duelTournamentPlayer.getPlayer2ID());
            for (DuelGame g : listGame2) {
                if (g.isFinished()) {
                    updateMemoryDataForGame(g);
                }
            }

            log.warn("Reload duel for tournamentPlayer="+duelTournamentPlayer);
            return true;
        } else {
            log.error("No duelTournamentPlayer found with tourPlayerID = "+tournamentPlayerID+" or duelMemTournament not found");
            return false;
        }
    }

    public void initStatPeriod() {
        Calendar calendar = Calendar.getInstance();
        statCurrentPeriodID = sdfStatPeriod.format(calendar.getTime());
        calendar.add(Calendar.MONTH, -1);
        statPreviousPeriodID = sdfStatPeriod.format(calendar.getTime());

        MongoCollection collectionDuelStat = mongoTemplate.getCollection("duel_stat");
        if (collectionDuelStat != null) {
            ListIndexesIterable<Document> indexes = collectionDuelStat.listIndexes();
            List<String> indexesToRemove = new ArrayList<>();
            boolean existIdxCurrentPeriod = false, existIdxPreviousPeriod = false;
            boolean existIdxArgineCurrentPeriod = false, existIdxArginePreviousPeriod = false;
            boolean existIdxTotal = false, existIdxArgineTotal = false;
            for (Document idx : indexes) {
                String idxName = (String)idx.get("name");
                if (idxName != null && idxName.startsWith("idx_stat_")) {
                    if (idxName.equals("idx_stat_" + statCurrentPeriodID)) {
                        existIdxCurrentPeriod = true;
                    } else if (idxName.equals("idx_stat_" + statPreviousPeriodID)) {
                        existIdxPreviousPeriod = true;
                    } else if (idxName.equals("idx_stat_argine_" + statCurrentPeriodID)) {
                        existIdxArgineCurrentPeriod = true;
                    } else if (idxName.equals("idx_stat_argine_" + statPreviousPeriodID)) {
                        existIdxArginePreviousPeriod = true;
                    } else if (idxName.equals("idx_stat_total")) {
                        existIdxTotal = true;
                    } else if (idxName.equals("idx_stat_argine_total")) {
                        existIdxArgineTotal = true;
                    }
                    else {
                        indexesToRemove.add(idxName);
                    }
                }
            }
            if (!existIdxCurrentPeriod) {
                collectionDuelStat.createIndex(Indexes.descending("resultPeriod." + statCurrentPeriodID + ".nbWin"), new IndexOptions().name("idx_stat_"+statCurrentPeriodID).background(true));
            }
            if (!existIdxPreviousPeriod) {
                collectionDuelStat.createIndex(Indexes.descending("resultPeriod."+statPreviousPeriodID+".nbWin"), new IndexOptions().name("idx_stat_"+statPreviousPeriodID).background(true));
            }
            if (!existIdxArgineCurrentPeriod) {
                collectionDuelStat.createIndex(Indexes.descending("resultArginePeriod." + statCurrentPeriodID + ".nbWin"), new IndexOptions().name("idx_stat_argine_"+statCurrentPeriodID).background(true));
            }
            if (!existIdxArginePreviousPeriod) {
                collectionDuelStat.createIndex(Indexes.descending("resultArginePeriod."+statPreviousPeriodID+".nbWin"), new IndexOptions().name("idx_stat_argine_"+statPreviousPeriodID).background(true));
            }
            if (!existIdxTotal) {
                collectionDuelStat.createIndex(Indexes.descending("total.nbWin"), new IndexOptions().name("idx_stat_total").background(true));
            }
            if (!existIdxArgineTotal) {
                collectionDuelStat.createIndex(Indexes.descending("totalArgine.nbWin"), new IndexOptions().name("idx_stat_argine_total").background(true));
            }
            if (indexesToRemove.size() > 0) {
                for (String e : indexesToRemove) {
                    collectionDuelStat.dropIndex(e);
                }
            }
        }
    }

    public String getStatCurrentPeriodID() {
        return statCurrentPeriodID;
    }

    public String getStatPreviousPeriodID() {
        return statPreviousPeriodID;
    }

    public void updatePlayerStatCountryCode(Player player) {
        if (player != null) {
            try {
                mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(player.getID())),
                        Update.update("countryCode", player.getDisplayCountryCode()),
                        DuelStat.class);
            } catch (Exception e) {
                log.error("Failed to update country code for player="+player, e);
            }
        }
    }

    /**
     * Count player in ranking for periodID
     * @param periodID
     * @param selectionPlayerID
     * @return
     */
    public int countRanking(String periodID, List<Long> selectionPlayerID, String countryCode) {
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(Criteria.where("_id").ne(Constantes.PLAYER_ARGINE_ID));
        if (periodID != null && periodID.length() > 0) {
            listCriteria.add(Criteria.where("resultPeriod."+periodID+".nbPlayed").gt(0));
        } else {
            listCriteria.add(Criteria.where("total.nbPlayed").gt(0));
        }
        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
            listCriteria.add(Criteria.where("_id").in(selectionPlayerID));
        }
        if (countryCode != null && countryCode.length() > 0) {
            listCriteria.add(Criteria.where("countryCode").is(countryCode));
        }
        Query query = new Query();
        if (listCriteria.size() > 0) {
            query = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
        }
        return (int)mongoTemplate.count(query, DuelStat.class);
    }

    /**
     * Count player on Argine duels for periodID
     * @param periodID
     * @param selectionPlayerID
     * @return
     */
    public int countRankingArgine(String periodID, List<Long> selectionPlayerID, String countryCode) {
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(Criteria.where("_id").ne(Constantes.PLAYER_ARGINE_ID));
        if (periodID != null && periodID.length() > 0) {
            listCriteria.add(Criteria.where("resultArginePeriod."+periodID+".nbPlayed").gt(0));
        } else {
            listCriteria.add(Criteria.where("totalArgine.nbPlayed").gt(0));
        }
        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
            listCriteria.add(Criteria.where("_id").in(selectionPlayerID));
        }
        if (countryCode != null && countryCode.length() > 0) {
            listCriteria.add(Criteria.where("countryCode").is(countryCode));
        }
        Query query = new Query();
        if (listCriteria.size() > 0) {
            query = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
        }
        return (int)mongoTemplate.count(query, DuelStat.class);
    }

    /**
     * Count player with nbWin > value in parameter for this periodID
     * @param periodID
     * @param selectionPlayerID
     * @param countryCode
     * @param nbWin
     * @return
     */
    public int countRankingBestNbWin(String periodID, List<Long> selectionPlayerID, String countryCode, int nbWin) {
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(Criteria.where("_id").ne(Constantes.PLAYER_ARGINE_ID));
        if (periodID != null && periodID.length() > 0) {
            listCriteria.add(Criteria.where("resultPeriod." + periodID + ".nbWin").gt(nbWin));
        } else {
            listCriteria.add(Criteria.where("total.nbWin").gt(nbWin));
        }
        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
            listCriteria.add(Criteria.where("_id").in(selectionPlayerID));
        }
        if (countryCode != null && countryCode.length() > 0) {
            listCriteria.add(Criteria.where("countryCode").is(countryCode));
        }
        return (int)mongoTemplate.count(Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()]))), DuelStat.class);
    }

    /**
     * Count player on Argine duels with nbWin > value in parameter for this periodID
     * @param periodID
     * @param selectionPlayerID
     * @param countryCode
     * @param nbWin
     * @return
     */
    public int countRankingArgineBestNbWin(String periodID, List<Long> selectionPlayerID, String countryCode, int nbWin) {
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(Criteria.where("_id").ne(Constantes.PLAYER_ARGINE_ID));
        if (periodID != null && periodID.length() > 0) {
            listCriteria.add(Criteria.where("resultArginePeriod." + periodID + ".nbWin").gt(nbWin));
        } else {
            listCriteria.add(Criteria.where("totalArgine.nbWin").gt(nbWin));
        }
        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
            listCriteria.add(Criteria.where("_id").in(selectionPlayerID));
        }
        if (countryCode != null && countryCode.length() > 0) {
            listCriteria.add(Criteria.where("countryCode").is(countryCode));
        }
        return (int)mongoTemplate.count(Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()]))), DuelStat.class);
    }

    /**
     * Count player with same nbWin for this periodID
     * @param periodID
     * @param selectionPlayerID
     * @param countryCode
     * @param nbWin
     * @return
     */
    public int countRankingSameNbWin(String periodID, List<Long> selectionPlayerID, String countryCode, int nbWin) {
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(Criteria.where("_id").ne(Constantes.PLAYER_ARGINE_ID));
        if (periodID != null && periodID.length() > 0) {
            listCriteria.add(Criteria.where("resultPeriod." + periodID + ".nbWin").is(nbWin));
        } else {
            listCriteria.add(Criteria.where("total.nbWin").is(nbWin));
        }
        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
            listCriteria.add(Criteria.where("_id").in(selectionPlayerID));
        }
        if (countryCode != null && countryCode.length() > 0) {
            listCriteria.add(Criteria.where("countryCode").is(countryCode));
        }
        return (int)mongoTemplate.count(Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()]))), DuelStat.class);
    }

    /**
     * Count player on Argine duels with same nbWin for this periodID
     * @param periodID
     * @param selectionPlayerID
     * @param countryCode
     * @param nbWin
     * @return
     */
    public int countRankingArgineSameNbWin(String periodID, List<Long> selectionPlayerID, String countryCode, int nbWin) {
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(Criteria.where("_id").ne(Constantes.PLAYER_ARGINE_ID));
        if (periodID != null && periodID.length() > 0) {
            listCriteria.add(Criteria.where("resultArginePeriod." + periodID + ".nbWin").is(nbWin));
        } else {
            listCriteria.add(Criteria.where("totalArgine.nbWin").is(nbWin));
        }
        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
            listCriteria.add(Criteria.where("_id").in(selectionPlayerID));
        }
        if (countryCode != null && countryCode.length() > 0) {
            listCriteria.add(Criteria.where("countryCode").is(countryCode));
        }
        return (int)mongoTemplate.count(Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()]))), DuelStat.class);
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
    public ResultServiceRest.GetMainRankingResponse getRanking(PlayerCache playerAsk, String periodID, List<Long> selectionPlayerID, String countryCode, int offset, int nbMax) {
        if (nbMax == 0) {
            nbMax = 50;
        }
        ResultServiceRest.GetMainRankingResponse response = new ResultServiceRest.GetMainRankingResponse();
        response.totalSize = countRanking(periodID, selectionPlayerID, countryCode);
        response.nbRankedPlayers = countRanking(periodID, null, countryCode);
        response.ranking = new ArrayList<>();
        boolean usePeriodID = periodID != null && periodID.length() > 0;
        // rankingPlayer
        if (playerAsk != null) {
            WSMainRankingPlayer rankingPlayerDuel = new WSMainRankingPlayer(playerAsk, true, playerAsk.ID);
            rankingPlayerDuel.rank = -1;
            DuelStat duelStatPlayer = getDuelStat(playerAsk.ID);
            if (duelStatPlayer != null) {
                boolean computeRank = false;
                if (usePeriodID) {
                    if (duelStatPlayer.resultPeriod.containsKey(periodID)) {
                        rankingPlayerDuel.value = duelStatPlayer.resultPeriod.get(periodID).nbWin;
                        computeRank = true;
                    }
                } else {
                    rankingPlayerDuel.value = duelStatPlayer.total.nbWin;
                    computeRank = true;
                }
                if (computeRank) {
                    rankingPlayerDuel.rank = countRankingBestNbWin(periodID, null, countryCode, (int) rankingPlayerDuel.value) + 1;
                    if (offset == -1) {
                        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
                            offset = countRankingBestNbWin(periodID, selectionPlayerID, countryCode, (int) rankingPlayerDuel.value) - (nbMax / 2);
                        } else {
                            offset = rankingPlayerDuel.rank - (nbMax / 2);
                        }
                    }
                }
            }
            response.rankingPlayer = rankingPlayerDuel;

        }
        // build query
        List<Criteria> listCriteria = new ArrayList<>();
        if (usePeriodID) {
            listCriteria.add(Criteria.where("resultPeriod." + periodID + ".nbPlayed").gt(0));
        } else {
            listCriteria.add(Criteria.where("total.nbPlayed").gt(0));
        }
        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
            listCriteria.add(Criteria.where("_id").in(selectionPlayerID));
        }
        if (countryCode != null && countryCode.length() > 0) {
            listCriteria.add(Criteria.where("countryCode").is(countryCode));
        }
        Query query = new Query();
        if (listCriteria.size() > 0) {
            query = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
        }
        if (usePeriodID) {
            query.with(new Sort(Sort.Direction.DESC, "resultPeriod." + periodID + ".nbWin"));
        } else {
            query.with(new Sort(Sort.Direction.DESC, "total.nbWin"));
        }
        if (offset < 0) {
            offset = 0;
        }
        response.offset = offset;
        if (offset > 0) {
            query.skip(offset);
        }
        if (nbMax > 0) {
            query.limit(nbMax);
        }
        query.skip(offset);
        query.limit(nbMax);
        List<DuelStat> listData = mongoTemplate.find(query, DuelStat.class);
        int currentRank = -1, currentNbWin = 0, nbWithSame = 0;
        boolean changeRankAfterInit = false;
        for (DuelStat e : listData) {
            WSMainRankingPlayer data = new WSMainRankingPlayer(playerCacheMgr.getPlayerCache(e.playerID), presenceMgr.isSessionForPlayerID(e.playerID), playerAsk.ID);
            DuelStatResult resPeriod = null;
            data.countryCode = e.countryCode;
            if (usePeriodID) {
                resPeriod = e.resultPeriod.get(periodID);
            } else {
                resPeriod = e.total;
            }
            if (resPeriod != null) {
                // init current counter
                if (currentRank == -1) {
                    currentRank = countRankingBestNbWin(periodID, null, countryCode, resPeriod.nbWin) + 1;
                    currentNbWin = resPeriod.nbWin;
//                    nbWithSame = 1;
                    nbWithSame = countRankingSameNbWin(periodID, null, countryCode, currentNbWin);
                } else {
                    if (resPeriod.nbWin == currentNbWin) {
                        if (changeRankAfterInit) {
                            nbWithSame++;
                        }
                    } else {
                        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
//                            currentRank = currentRank + countRankingSameNbWin(periodID, null, countryCode, currentNbWin);
                            currentRank = countRankingBestNbWin(periodID, null, countryCode, resPeriod.nbWin) + 1;
                        } else {
                            currentRank = currentRank + nbWithSame;
                        }
                        changeRankAfterInit = true;
                        currentNbWin = resPeriod.nbWin;
                        nbWithSame = 1;
                    }
                }
                data.value = resPeriod.nbWin;
                if (data.playerID == Constantes.PLAYER_ARGINE_ID) {
                    data.rank = 0;
                } else {
                    data.rank = currentRank;
                }
            }
            response.ranking.add(data);
        }
        return response;
    }

    /**
     * Ranking for duels with argine on period
     * @param playerAsk
     * @param periodID
     * @param selectionPlayerID
     * @param countryCode
     * @param offset
     * @param nbMax
     * @return
     */
    public ResultServiceRest.GetMainRankingResponse getRankingArgine(PlayerCache playerAsk, String periodID, List<Long> selectionPlayerID, String countryCode, int offset, int nbMax) {
        ResultServiceRest.GetMainRankingResponse response = new ResultServiceRest.GetMainRankingResponse();
        response.totalSize = countRankingArgine(periodID, selectionPlayerID, countryCode);
        response.nbRankedPlayers = countRankingArgine(periodID, null, countryCode);
        response.ranking = new ArrayList<>();
        boolean usePeriodID = periodID != null && periodID.length() > 0;
        // rankingPlayer
        if (playerAsk != null) {
            WSMainRankingPlayer rankingPlayerDuel = new WSMainRankingPlayer(playerAsk, true, playerAsk.ID);
            rankingPlayerDuel.rank = -1;
            DuelStat duelStatPlayer = getDuelStat(playerAsk.ID);
            if (duelStatPlayer != null) {
                boolean computeRank = false;
                if (usePeriodID) {
                    if (duelStatPlayer.resultArginePeriod.containsKey(periodID)) {
                        rankingPlayerDuel.value = duelStatPlayer.resultArginePeriod.get(periodID).nbWin;
                        computeRank = true;
                    }
                } else {
                    rankingPlayerDuel.value = duelStatPlayer.totalArgine.nbWin;
                    computeRank = true;
                }
                if (computeRank) {
                    rankingPlayerDuel.rank = countRankingArgineBestNbWin(periodID, null, countryCode, (int) rankingPlayerDuel.value) + 1;
                    if (offset == -1) {
                        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
                            offset = countRankingArgineBestNbWin(periodID, selectionPlayerID, countryCode, (int) rankingPlayerDuel.value) - (nbMax / 2);
                        } else {
                            offset = rankingPlayerDuel.rank - (nbMax / 2);
                        }
                    }
                }
            }
            response.rankingPlayer = rankingPlayerDuel;

        }
        // build query
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(Criteria.where("_id").ne(Constantes.PLAYER_ARGINE_ID));
        if (usePeriodID) {
            listCriteria.add(Criteria.where("resultArginePeriod." + periodID + ".nbPlayed").gt(0));
        } else {
            listCriteria.add(Criteria.where("totalArgine.nbPlayed").gt(0));
        }
        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
            listCriteria.add(Criteria.where("_id").in(selectionPlayerID));
        }
        if (countryCode != null && countryCode.length() > 0) {
            listCriteria.add(Criteria.where("countryCode").is(countryCode));
        }
        Query query = new Query();
        if (listCriteria.size() > 0) {
            query = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
        }
        if (usePeriodID) {
            query.with(new Sort(Sort.Direction.DESC, "resultArginePeriod." + periodID + ".nbWin"));
        } else {
            query.with(new Sort(Sort.Direction.DESC, "totalArgine.nbWin"));
        }
        if (offset < 0) {
            offset = 0;
        }
        response.offset = offset;
        if (offset > 0) {
            query.skip(offset);
        }
        if (nbMax > 0) {
            query.limit(nbMax);
        }
        List<DuelStat> listData = mongoTemplate.find(query, DuelStat.class);
        int currentRank = -1, currentNbWin = 0, nbWithSame = 0;
        boolean changeRankAfterInit = false;
        for (DuelStat e : listData) {
            WSMainRankingPlayer data = new WSMainRankingPlayer(playerCacheMgr.getPlayerCache(e.playerID), presenceMgr.isSessionForPlayerID(e.playerID), playerAsk.ID);
            DuelStatResult resPeriod = null;
            data.countryCode = e.countryCode;
            if (usePeriodID) {
                resPeriod = e.resultArginePeriod.get(periodID);
            } else {
                resPeriod = e.totalArgine;
            }
            if (resPeriod != null) {
                // init current counter
                if (currentRank == -1) {
                    currentRank = countRankingArgineBestNbWin(periodID, null, countryCode, resPeriod.nbWin) + 1;
                    currentNbWin = resPeriod.nbWin;
                    nbWithSame = countRankingArgineSameNbWin(periodID, null, countryCode, currentNbWin);
                } else {
                    if (resPeriod.nbWin == currentNbWin) {
                        if (changeRankAfterInit) {
                            nbWithSame++;
                        }
                    } else {
                        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
                            currentRank = currentRank + countRankingArgineSameNbWin(periodID, null, countryCode, currentNbWin);
                        } else {
                            currentRank = currentRank + nbWithSame;
                        }
                        changeRankAfterInit = true;
                        currentNbWin = resPeriod.nbWin;
                        nbWithSame = 1;
                    }
                }
                data.value = resPeriod.nbWin;
                data.rank = currentRank;
            }
            response.ranking.add(data);
        }
        return response;
    }

    /**
     * Process notif ranking
     */
    public void processNotifRanking() {
        String rankingType = Constantes.RANKING_TYPE_DUEL;
        String rankingOptions = Constantes.RANKING_OPTIONS_PREVIOUS_PERIOD;

        ResultServiceRest.GetMainRankingResponse resp;
        String periodID = statPreviousPeriodID;
        int nbPlayers = countRanking(periodID, null, null);
        int offset = 0;
        int nbMax = 100;
        while (offset < nbPlayers) {
            resp = getRanking(new PlayerCache(0), periodID, null, null, offset, nbMax);
            if (resp != null) {
                for (WSMainRankingPlayer rankingPlayer : resp.ranking) {
                    MessageNotif notif;
                    // Top 3
                    if (rankingPlayer.getRank() <= 3) {
                        notif = notifMgr.createNotifGroupRankingTypeTop3(rankingPlayer.getPlayerID(), rankingPlayer.getPlayerPseudo(), rankingPlayer.getRank(), resp.totalSize, rankingPlayer.value, rankingType, rankingOptions);
                    }
                    // Top 10%
                    else if (rankingPlayer.getRank() <= (resp.totalSize * 0.1)) {
                        notif = notifMgr.createNotifGroupRankingTypeTop10Percent(rankingPlayer.getPlayerID(), rankingPlayer.getPlayerPseudo(), rankingPlayer.getRank(), resp.totalSize, rankingPlayer.value, rankingType, rankingOptions);
                    }
                    // Others
                    else {
                        notif = notifMgr.createNotifGroupRankingTypeOthers(rankingPlayer.getPlayerID(), rankingPlayer.getPlayerPseudo(), rankingPlayer.getRank(), resp.totalSize, rankingPlayer.value, rankingType, rankingOptions);
                    }
                    if (notif != null) {
                        FBSession session = presenceMgr.getSessionForPlayerID(rankingPlayer.getPlayerID());
                        if (session != null) {
                            // push event for notif
                            session.pushEvent(notifMgr.buildEvent(notif, rankingPlayer.getPlayerID()));
                        }
                    }
                }
                offset += resp.ranking.size();
            } else {
                return;
            }
        }
    }

    /**
     * Process notif ranking Argine
     */
    public void processNotifRankingArgine() {
        String rankingType = Constantes.RANKING_TYPE_DUEL_ARGINE;
        String rankingOptions = Constantes.RANKING_OPTIONS_PREVIOUS_PERIOD;

        ResultServiceRest.GetMainRankingResponse resp;
        String periodID = statPreviousPeriodID;
        int nbPlayers = countRankingArgine(periodID, null, null);
        int offset = 0;
        int nbMax = 100;
        while (offset < nbPlayers) {
            resp = getRankingArgine(new PlayerCache(0), periodID, null, null, offset, nbMax);
            if (resp != null) {
                for (WSMainRankingPlayer rankingPlayer : resp.ranking) {
                    MessageNotif notif;
                    // Top 3
                    if (rankingPlayer.getRank() <= 3) {
                        notif = notifMgr.createNotifGroupRankingTypeTop3(rankingPlayer.getPlayerID(), rankingPlayer.getPlayerPseudo(), rankingPlayer.getRank(), resp.totalSize, rankingPlayer.value, rankingType, rankingOptions);
                    }
                    // Top 10%
                    else if (rankingPlayer.getRank() <= (resp.totalSize * 0.1)) {
                        notif = notifMgr.createNotifGroupRankingTypeTop10Percent(rankingPlayer.getPlayerID(), rankingPlayer.getPlayerPseudo(), rankingPlayer.getRank(), resp.totalSize, rankingPlayer.value, rankingType, rankingOptions);
                    }
                    // Others
                    else {
                        notif = notifMgr.createNotifGroupRankingTypeOthers(rankingPlayer.getPlayerID(), rankingPlayer.getPlayerPseudo(), rankingPlayer.getRank(), resp.totalSize, rankingPlayer.value, rankingType, rankingOptions);
                    }
                    if (notif != null) {
                        FBSession session = presenceMgr.getSessionForPlayerID(rankingPlayer.getPlayerID());
                        if (session != null) {
                            // push event for notif
                            session.pushEvent(notifMgr.buildEvent(notif, rankingPlayer.getPlayerID()));
                        }
                    }
                }
                offset += resp.ranking.size();
            } else {
                return;
            }
        }
    }

    public Map<Long, Double> getBestScoreEver(String field, Long rivalId) {
        Map<Long, Double> podium = new HashMap();
        try {
            getBestScoreIfFieldExist(field + "." + rivalId).forEach(player -> podium.put(player.playerID, player.scoring.getBestEver().get(rivalId)));
            return sortDuelPodium(podium);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return podium;
    }

    public Map<Long, Double> getBestScoreMonthly(String field, String periodId, Long rivalId) {
        Map<Long, Double> podium = new HashMap();
        try {
            getBestScoreIfFieldExist(field + "." + periodId + "." + rivalId).forEach(player -> podium.put(player.playerID, player.scoring.getBestMonthly().get(periodId).get(rivalId)));
            return sortDuelPodium(podium);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return podium;
    }

    public Map<Long, Double> getBestScoreWeekly(long rivalId, Calendar startDate, Calendar endDate){
        Map<Long, Double> podium = new HashMap();
        SimpleDateFormat sdf = new SimpleDateFormat(("yyyyMMdd"));
        try {
            // Init startDate at first day of week
            startDate.add(Calendar.DATE, -(startDate.get(Calendar.DAY_OF_WEEK) - startDate.getFirstDayOfWeek()));

            // loop while endDate isn't past
            while(startDate.before(endDate)){
                // build bestScoreWeek key
                String weeklyKey = sdf.format(startDate.getTime());

                // Catch best score of player period into map
                getBestScoreIfFieldExist("scoring.bestWeekly." + weeklyKey + "." + rivalId).forEach(p -> {
                    Double score = p.scoring.getBestWeekly().get(weeklyKey).get(rivalId);
                    Double lastScore = podium.get(p.playerID);
                    if (lastScore == null || lastScore < score){
                        podium.put(p.playerID, score);
                    }
                });

                // Add 7 Days for the next weeklyKey
                startDate.add(Calendar.DATE, 7);
            }
            return sortDuelPodium(podium);
        }catch(Exception e){
            log.error(e.getMessage(), e);
        }
        return podium;
    }

    // Create request recover duel stat for one peiod (ever / month / week)
    private List<DuelStat> getBestScoreIfFieldExist(String field){
        Query query = new Query(Criteria.where(field).exists(true));
        query.fields().include(field);
        return mongoTemplate.find(query, DuelStat.class);
    }

    // Sort map podium by value
    private Map<Long, Double> sortDuelPodium(Map<Long, Double> podium){
        return  podium.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
    }
}
