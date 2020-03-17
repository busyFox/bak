package com.funbridge.server.tournament.federation;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.DateUtils;
import com.funbridge.server.common.MailMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.message.data.MessageNotifGroup;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.texts.TextUIMgr;
import com.funbridge.server.tournament.DealGenerator;
import com.funbridge.server.tournament.federation.data.*;
import com.funbridge.server.tournament.federation.memory.TourFederationMemDeal;
import com.funbridge.server.tournament.federation.memory.TourFederationMemDealPlayer;
import com.funbridge.server.tournament.federation.memory.TourFederationMemTournament;
import com.funbridge.server.tournament.federation.memory.TourFederationMemTournamentPlayer;
import com.funbridge.server.tournament.game.*;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.generic.memory.GenericMemDeal;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.result.*;
import com.funbridge.server.ws.tournament.WSTableTournament;
import com.funbridge.server.ws.tournament.WSTournament;
import com.funbridge.server.ws.tournament.WSTournamentFederation;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.session.Session;
import com.gotogames.common.session.SessionMgr;
import com.gotogames.common.tools.JSONTools;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.tomcat.util.bcel.Const;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.*;

/**
 * Created by bplays on 10/01/17.
 * ???
 */
public abstract class TourFederationMgr extends TournamentGenericMgr {

    @Resource(name="messageNotifMgr")
    protected MessageNotifMgr notifMgr = null;
    @Resource(name="presenceMgr")
    protected PresenceMgr presenceMgr = null;
    @Resource(name="mailMgr")
    protected MailMgr mailMgr = null;
    @Resource(name="mongoTemplate")
    protected MongoTemplate funbridgeMongoTemplate;
    @Resource(name = "tourFederationStatPeriodMgr")
    protected TourFederationStatPeriodMgr tourFederationStatPeriodMgr;
    @Resource(name="textUIMgr")
    private TextUIMgr textUIMgr;

    private LockWeakString lockUpdatePlayer = new LockWeakString();

    public boolean generatorTaskRunning = false, enableTaskRunning = false, finishTaskRunning = false, monthlyTaskRunning = false;
    protected JSONTools jsonTools = new JSONTools();
    protected Scheduler scheduler;
    protected SimpleDateFormat sdfDayMonth = new SimpleDateFormat("dd/MM");
    protected MongoCollection collectionPlayerFederation;

    public static final int TOUR_STATUS_NO_REGISTER = 0;
    public static final int TOUR_STATUS_REGISTER_ENABLED = 1;
    public static final int TOUR_STATUS_IN_PROGRESS = 2;
    public static final int TOUR_STATUS_PLAYED = 3;
    public static final int TOUR_STATUS_CANCELED = 4;
    public static final int NB_DEALS_TO_PLAY = 20;

    public static final int RESULT_TYPE = Constantes.TOURNAMENT_RESULT_PAIRE;

    protected LockWeakString lockPlayer = new LockWeakString();
    protected LockWeakString lockCreateGame = new LockWeakString();
    protected LockWeakString lockTourFederationStat = new LockWeakString();
    protected LockWeakString lockTourFunbridgePointsStat = new LockWeakString();
    protected Object lockFinishTournament = new Object();

    public TourFederationMgr(int category) {
        super(category);
    }

    public Object getLockUpdatePlayer(long playerId) {
        return lockUpdatePlayer.getLock(""+playerId);
    }

    public int getNbDealsToPlay() {
        return getConfigIntValue("nbDealsToPlay", NB_DEALS_TO_PLAY);
    }

    public abstract String getFederationName();

    public abstract String getPlayerFederationCollectionName();

    public abstract String getTournamentPlayerCollectionName();

    public abstract String getTourFederationStatCollectionName();

    @Override
    public abstract Class<? extends TourFederationTournament> getTournamentEntity();

    public abstract Class<? extends TourFederationGeneratorTask> getTourFederationGeneratorTaskEntity();

    public abstract Class<? extends TourFederationEnableTask> getTourFederationEnableTaskEntity();

    public abstract Class<? extends TourFederationFinishTask> getTourFederationFinishTaskEntity();

    public abstract Class<? extends TourFederationMonthlyReportTask> getTourFederationMonthlyReportTask();

    public abstract Class<? extends TourFederationGenerateSettings> getTourFederationGenerateSettingsEntity();

    public abstract Class<? extends TourFederationStat> getTourFederationStatEntity();

    public abstract String getTriggerGroup();

    public abstract boolean testLicenceValidity(String licence);

    public abstract TourFederationGenerateSettingsElement buildGenerateSettingsElement();

    protected abstract TourFederationTournament buildTournament();

    protected abstract TourFederationGame buildGame(long playerID, TourFederationTournament tour, int dealIndex, int playerIndex);

    protected abstract TourFederationGame buildGame(long playerID, TourFederationTournament tour, int dealIndex);

    protected abstract TourFederationTournamentPlayer buildTournamentPlayer();

    protected abstract TourFederationStat buildStat();

    public abstract boolean computePoints(List<TourFederationTournamentPlayer> playerResults);

    public abstract void generateReportForTournament(TourFederationTournament tournament, List<TourFederationTournamentPlayer> playerResults, boolean sendMail) throws FBWSException;

    public abstract boolean generateMonthlyReport(int month, int year, boolean sendMail);

    protected boolean scheduleSpecificTasks() throws SchedulerException {
        return true;
    }

    @Override
    public int getNbPlayersOnTournament(Tournament tour, List<Long> listFollower, boolean onlyFinisher) {
        return super.getNbPlayersOnTournament(tour, listFollower, onlyFinisher);
    }

    @Override
    public void startUp() {
        log.info("startup");
        collectionPlayerFederation = getMongoTemplate().getCollection(getPlayerFederationCollectionName());

        // check tournament to generate ????????
        List<? extends TourFederationTournament> listTour = checkTournamentToGenerate();
        log.warn("checkTournamentToGenerate => listTour="+(listTour!=null?listTour.size():"null"));
        // Prepare tournament ????
        listTour = processPrepareTournament();
        log.warn("processPrepareTournament => listTour="+(listTour!=null?listTour.size():"null"));

        // load tournament in progress ??????
        List<? extends TourFederationTournament> listTourInProgress = listTournamentInProgress();
        if (listTourInProgress != null && listTourInProgress.size() > 0) {
            log.error("Nb tournament "+getFederationName()+" to load : "+listTourInProgress.size());
            int nbLoadFromFile = 0, nbLoadFromDB = 0;
            // loop on each tournament ??????
            for (TourFederationTournament tour : listTourInProgress) {
                boolean loadFromDB = false;
                if (getConfigIntValue("backupMemoryLoad", 1) == 1) {
                    // try to load from json file ???json????
                    String filePath = buildBackupFilePathForTournament(tour.getIDStr(), false);
                    if (filePath != null) {
                        if (memoryMgr.loadMemTourFromFile(filePath) != null) {
                            nbLoadFromFile++;
                        } else {
                            loadFromDB = true;
                        }
                    }
                } else {
                    loadFromDB = true;
                }
                // load from DB ??????
                if (loadFromDB) {
                    GenericMemTournament memTour = memoryMgr.addTournament(tour);
                    if (memTour != null) {
                        List<TourFederationGame> listGame = listGameOnTournament(tour.getIDStr());
                        if (listGame != null) {
                            // load each game in memory ???????????
                            for (TourFederationGame tourGame : listGame) {
                                if (tourGame.isFinished()) {
                                    memTour.addResult(tourGame, false, false);
                                } else {
                                    memTour.addPlayer(tourGame.getPlayerID(), tourGame.getStartDate());
                                }
                            }
                            // compute result & ranking ???????
                            memTour.computeResult();
                            memTour.computeRanking(true);
                        }
                        nbLoadFromDB++;
                    } else {
                        log.error("Failed to add tournament in memory ... tour="+tour);
                    }
                }
            }
            log.error("Nb Tournament loaded : "+listTour.size()+" - loadFromFile="+nbLoadFromFile+" - loadFromDB="+nbLoadFromDB);
        } else {
            log.info("No tournament "+getFederationName()+" to load");
        }

        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();

            // Generator task - triger every hour ?????-???????
            JobDetail jobGenerator = JobBuilder.newJob(getTourFederationGeneratorTaskEntity()).withIdentity("generatorTask", getTriggerGroup()).build();
            CronTrigger triggerGenerator = TriggerBuilder.newTrigger().withIdentity("triggerGeneratorTask", getTriggerGroup()).withSchedule(CronScheduleBuilder.cronSchedule("0 45 */2 * * ?")).build();// second minute hour ...
            Date dateNextJobGenerator = scheduler.scheduleJob(jobGenerator, triggerGenerator);
            log.warn("Scheduled for job=" + jobGenerator.getKey() + " run at="+dateNextJobGenerator+" - cron expression=" + triggerGenerator.getCronExpression() + " - next fire=" + triggerGenerator.getNextFireTime());

            // Enable task - triger every 5 mn ????-?5??????
            JobDetail jobEnable = JobBuilder.newJob(getTourFederationEnableTaskEntity()).withIdentity("enableTask", getTriggerGroup()).build();
            CronTrigger triggerEnable = TriggerBuilder.newTrigger().withIdentity("triggerEnableTask", getTriggerGroup()).withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?")).build();
            Date dateNextJobEnable = scheduler.scheduleJob(jobEnable, triggerEnable);
            log.warn("Scheduled for job=" + jobEnable.getKey() + " run at="+dateNextJobEnable+" - cron expression=" + triggerEnable.getCronExpression() + " - next fire=" + triggerEnable.getNextFireTime());

            // Finish task - triger every 30 mn ????-?30??????
            JobDetail jobFinish = JobBuilder.newJob(getTourFederationFinishTaskEntity()).withIdentity("finishTask", getTriggerGroup()).build();
            CronTrigger triggerFinish = TriggerBuilder.newTrigger().withIdentity("triggerFinishTask", getTriggerGroup()).withSchedule(CronScheduleBuilder.cronSchedule("0 2/30 * * * ?")).build();
            Date dateNextJobFinish = scheduler.scheduleJob(jobFinish, triggerFinish);
            log.warn("Scheduled for job=" + jobFinish.getKey() + " run at="+dateNextJobFinish+" - cron expression=" + triggerFinish.getCronExpression() + " - next fire=" + triggerFinish.getNextFireTime());

            // Monthly report task ??????
            scheduleMonthlyReportTask();

            // Schedule specific task for federation ?????????
            scheduleSpecificTasks();
        } catch (Exception e) {
            log.error("Exception to init scheduler", e);
        }
    }

    @Override
    @PreDestroy
    public void destroy() {
        log.info("destroy");
        super.destroy();
    }

    /*************
     * SCHEDULER
     */
    /**
     * Schedule monthly report task ????????
     * @throws SchedulerException
     */
    public void scheduleMonthlyReportTask() throws SchedulerException {
        // Monthly report task - trigger on the first day of each month, at 8:00 AM ??????-?????????8:00??
        if (getTourFederationMonthlyReportTask() != null) {
            JobDetail jobMonthlyReport = JobBuilder.newJob(getTourFederationMonthlyReportTask()).withIdentity("monthlyReportTask", getTriggerGroup()).build();
            CronTrigger triggerMonthlyReport = TriggerBuilder.newTrigger().withIdentity("triggerMonthlyReport", getTriggerGroup()).withSchedule(CronScheduleBuilder.cronSchedule("0 0 8 1 1/1 ? *")).build();
            Date dateNextJobMonthlyReport = scheduler.scheduleJob(jobMonthlyReport, triggerMonthlyReport);
            log.warn("Scheduled for job=" + jobMonthlyReport.getKey() + " run at=" + dateNextJobMonthlyReport + " - cron expression=" + triggerMonthlyReport.getCronExpression() + " - next fire=" + triggerMonthlyReport.getNextFireTime());
        }
    }
    /**
     * Return next TS for generator JOB
     * ?????TS?generator JOB
     * @return
     */
    public long getDateNextJobGenerator() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerGeneratorTask", getTriggerGroup()));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    /**
     * Return next TS for enable JOB
     * ?????TS?enable JOB
     * @return
     */
    public long getDateNextJobEnable() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerEnableTask", getTriggerGroup()));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    /**
     * Return next TS for finish JOB
     * ?????TS?finish JOB
     * @return
     */
    public long getDateNextJobFinish() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerFinishTask", getTriggerGroup()));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    /**
     * Return next TS for finish JOB
     * ?????TS?????
     * @return
     */
    public long getDateNextJobMonthlyReport() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerMonthlyReport", getTriggerGroup()));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    /**************************
     * Cycle tournaments
     */

    /**
     * Get generate tour settings and generate new tournament if necessary
     * ?????????????????????
     * @return list of new tournament generate
     */
    public synchronized List<TourFederationTournament> checkTournamentToGenerate() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        List<TourFederationTournament> listTour = new ArrayList<>();
        if (getConfigIntValue("taskGenerator", 1) == 0) {
            return listTour;
        }
        int nbDaysForLoop = getConfigIntValue("generatorTour.nbDays", 21);
        for (int i = 0; i < nbDaysForLoop; i++) {
            log.info("Check tournament for day="+ Constantes.timestamp2StringDate(cal.getTimeInMillis()));
            TourFederationGenerateSettings settings = getTourFederationGenerateSettingsForDate(cal.getTimeInMillis());
            if (settings != null) {
                for (Object e : settings.settings) {
                    TourFederationGenerateSettingsElement settingsElement = (TourFederationGenerateSettingsElement) e;
                    try {
                        // check tournament not existing for this date
                        long tsStartDate = settingsElement.getStartDateForDate(cal.getTimeInMillis());
                        if (tsStartDate != 0) {
                            TourFederationTournament tour = getTournamentForStartDateAndResultType(tsStartDate, settingsElement.resultType);
                            if (tour == null) {
                                // no tournament existing => generate a new one !
                                tour = createTournament(settingsElement, cal, getFederationName() + "Mgr");
                                if (tour != null) {
                                    listTour.add(tour);
                                }
                                log.warn("New tournament created : tour="+tour+", creationDate="+cal.getTimeInMillis());
                            } else {
                                log.debug("Tour already existing for date=" + Constantes.timestamp2StringDateHour(tsStartDate));
                            }
                        } else {
                            log.error("No startDate compute for settings element=" + e);
                        }
                    } catch (Exception ex) {
                        log.error("Failed to check for settings="+e, ex);
                    }
                }
            }
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            if (getConfigStringValue("generatorType", "PROD").equals("DEV") && i == 1) {
                // For DEV only for current day and next day
                break;
            }
        }

        return listTour;
    }



    /**
     * Get settings for date. Read config file to get settings for this weekday
     * ??????? ????????????????
     * @param tsDate
     * @return
     */
    public TourFederationGenerateSettings getTourFederationGenerateSettingsForDate(long tsDate) {
        TourFederationGenerateSettings settings = null;
        Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(tsDate);
        Calendar endCal = (Calendar) calendar.clone();
        endCal.add(Calendar.HOUR_OF_DAY, 1);
        String type = getConfigStringValue("generatorType", "PROD");
        if (type.equals("DEV")) {
            // DEV => one every hour for each day
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            settings = new TourFederationGenerateSettings();
            settings.settings = new ArrayList<>();
            while (true) {
                TourFederationGenerateSettingsElement e = buildGenerateSettingsElement();
                e.startHour = calendar.get(Calendar.HOUR_OF_DAY)+"h";
                e.endHour = endCal.get(java.util.Calendar.HOUR_OF_DAY)+"h";
                e.registrationDurationHour = 7*24;
                e.name = calendar.getDisplayName(Calendar.DAY_OF_WEEK, java.util.Calendar.LONG, new Locale("FR", "fr"))+" - "+calendar.get(java.util.Calendar.HOUR_OF_DAY);
                e.resultType = (calendar.get(Calendar.HOUR_OF_DAY)%2)+1;
                settings.settings.add(e);
                if (calendar.get(java.util.Calendar.HOUR_OF_DAY) == 23) {
                    break;
                }
                calendar.add(Calendar.HOUR_OF_DAY, 1);
                endCal.add(Calendar.HOUR_OF_DAY, 1);
            }
        } else {
            String strDay = sdfDayMonth.format(calendar.getTime());
            String selSettings;
            // check special day
            String listHolidayStr = getConfigStringValue("holidayList", null);
            List<String> listHoliday = new ArrayList<>();
            if (listHolidayStr != null && listHolidayStr.length() > 0) {
                listHoliday = Arrays.asList(listHolidayStr.split(","));
            }

            if (listHoliday.contains(strDay)) {
                selSettings = "HOLIDAY";
            } else {
                switch (calendar.get(java.util.Calendar.DAY_OF_WEEK)) {
                    case java.util.Calendar.MONDAY: {
                        selSettings = "MONDAY";
                        break;
                    }
                    case java.util.Calendar.TUESDAY: {
                        selSettings = "TUESDAY";
                        break;
                    }
                    case java.util.Calendar.WEDNESDAY: {
                        selSettings = "WEDNESDAY";
                        break;
                    }
                    case java.util.Calendar.THURSDAY: {
                        selSettings = "THURSDAY";
                        break;
                    }
                    case java.util.Calendar.FRIDAY: {
                        selSettings = "FRIDAY";
                        break;
                    }
                    case java.util.Calendar.SATURDAY: {
                        selSettings = "SATURDAY";
                        break;
                    }
                    case java.util.Calendar.SUNDAY:
                    default: {
                        selSettings = "SUNDAY";
                        break;
                    }
                }
            }
            settings = getTourFederationGenerateDaySettings(selSettings);
            if (settings == null) {
                log.info("No settings for selSettings=" + selSettings + " - date=" + Constantes.timestamp2StringDate(tsDate));
            }
        }
        return settings;
    }

    /**
     * return tournament settings of days ???????
     * @param day
     * @return
     */
    public TourFederationGenerateSettings getTourFederationGenerateDaySettings(String day){
        String settings = getConfigStringValue("generateTour" + day, null);
        if (settings != null) {
            try {
                return new JSONTools().mapData(settings, getTourFederationGenerateSettingsEntity());
            } catch (Exception e) {
                log.error("Failed to mapData for settings=" + settings);
            }
        }
        return null;
    }

    /**
     * Get tournament with settings to be prepare for playing
     * @return
     */
    public List<? extends TourFederationTournament> processPrepareTournament() {
        long curTS = System.currentTimeMillis();
        // get tournaments ready to play ???? ????
        Criteria c1 = Criteria.where("processPrepareDate").is(0); // ?????
        Criteria c2 = Criteria.where("startDate").lte(curTS); //  start date <= current time ????<=????
        Criteria c3 = Criteria.where("endDate").gt(curTS); // end date > current time ????>????
        Query q = Query.query(new Criteria().andOperator(c3, c2, c1));
        q.with(new Sort(Sort.Direction.ASC, "startDate"));
        List<? extends TourFederationTournament> listTour = getMongoTemplate().find(q, this.getTournamentEntity());
        for (TourFederationTournament tour : listTour) {
            if (tour.getProcessPrepareDate() == 0 && tour.getEndDate() >= curTS) {
                if (prepareTournamentToPlay(tour)) {
                    if (tour.isEnableToPlay()) {
                        // send notif tournament ready to play
                        MessageNotifGroup notifGroup = notifMgr.createNotifGroupTourFederationReadyToPlay(tour, getFederationName());
                        if (notifGroup != null) {
                            notifMgr.setNotifGroupForPlayer(notifGroup, new ArrayList<Long>(tour.getMapRegistration().keySet()));
                            for (Long l : tour.getMapRegistration().keySet()) {
                                FBSession session = presenceMgr.getSessionForPlayerID(l);
                                if (session != null) {
                                    session.pushEvent(notifMgr.buildEvent(notifGroup, l));
                                }
                            }
                        } else {
                            log.error("Failed to create notifGroup ... tour=" + tour);
                        }
                    } else {
                        // recredit players
                        if (tour.getMapRegistration() != null && tour.getMapRegistration().size() > 0) {
                            // send notif tournament canceled ... not enough player !
                            MessageNotifGroup notifGroup = notifMgr.createNotifGroupTourFederationCanceled(tour, getNextTournament(tour.getStartDate()), getFederationName());
                            if (notifGroup != null) {
                                notifMgr.setNotifGroupForPlayer(notifGroup, new ArrayList<Long>(tour.getMapRegistration().keySet()));
                                for (Long l : tour.getMapRegistration().keySet()) {
                                    FBSession session = presenceMgr.getSessionForPlayerID(l);
                                    if (session != null) {
                                        session.pushEvent(notifMgr.buildEvent(notifGroup, l));
                                    }
                                }
                            } else {
                                log.error("Failed to create notifGroup ... tour=" + tour);
                            }
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Process return false for tour="+tour);
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No process for tour="+tour);
                }
            }
        }
        return listTour;
    }

    /**
     * Get nb deals to generate
     * Specs on : https://gotogames.atlassian.net/wiki/spaces/SPECS/pages/1231749125/Nombre+de+donnes+dans+les+tournois
     * @param nbDealsToPlay
     * @param nbPlayersRegisteredOnLastTour
     * @return
     */
    public int getNbDealsToGenerate(int nbDealsToPlay, int nbPlayersRegisteredOnLastTour) {
        // Default : nbDeals to generate = nbDeals to play
        int nbDeals = nbDealsToPlay;

        // If nbDeals to generate != nbDeals to play
        boolean nbDealsEqualsNbDealsToPlay = getConfigBooleanValue("nbDealsEqualsNbDealsToPlay", false);
        if (!nbDealsEqualsNbDealsToPlay && !getConfigStringValue("generatorType", "PROD").equals("DEV")) {
            // Compute nb Deals
            nbDeals = nbDealsToPlay * (2 + nbPlayersRegisteredOnLastTour / 100);

            // Limit nb deals to a max number
            int maxNbDeals = getConfigIntValue("maxNbDeals", 200);
            nbDeals = (nbDeals > maxNbDeals) ? maxNbDeals : nbDeals;

            // If not enough deals
            if (nbDeals < nbDealsToPlay) {
                nbDeals = nbDealsToPlay;
            }
        }
        return nbDeals;
    }

    /**
     * Prepare tournament to be played : generate deals and update status
     * N = le nombre d'inscrits au dernier tournoi - D = Nombre de donnes en circulation
     * Si N<nbDealsThreshold2, D=nbDealsThreshold2. Si le nombre de joueurs est infrieur  30, nous prparerons 30 donnes
     * Si nbDealsThreshold2<N<nbDealsThreshold3, D=N. Si le nombre de joueurs inscrits est compris entre 30 et 150, Nous prparerons autant de donnes que de joueurs.
     * Si N>nbDealsThreshold3, D=200. Si le nombre de joueurs est suprieur  150, nous prparerons 200 donnes.
     * @param tour
     * @return true if tournament is process
     */
    public boolean prepareTournamentToPlay(TourFederationTournament tour) {
        if (tour != null) {
            if (tour.getNbDeals() == 0 && tour.getProcessPrepareDate() == 0) {
                // Get nb deals to generate ??????
                int nbPlayersRegisteredOnLastTour = 0;
                TourFederationTournament lastTour = findLastFinishedTournamentWithFreeStatus(tour.isFree());
                if (lastTour != null) {
                    nbPlayersRegisteredOnLastTour = lastTour.countNbPlayersRegistered();
                } else {
                    tour.addInfo("First tour");
                    log.info("First tour - tour=" + tour);
                }
                int nbDeals = getNbDealsToGenerate(tour.getNbDealsToPlay(), nbPlayersRegisteredOnLastTour);

                if (nbDeals > 0) {
                    // generate distribution
                    DealGenerator.DealGenerate[] tabDeal = dealGenerator.generateDeal(nbDeals, 0);
                    if (tabDeal != null) {
                        tour.setDealGenerate(tabDeal);
                        tour.setEnableToPlay(true);
                        memoryMgr.addTournament(tour);
                    } else {
                        log.error("Failed to generate deals for tour=" + tour);
                    }
                } else {
                    log.error("Nb deals is 0 !!");
                }

                tour.setProcessPrepareDate(System.currentTimeMillis());
                getMongoTemplate().save(tour);
                return true;
            } else {
                log.warn("Process prepare tournament already done for this tour="+tour);
            }
        } else {
            log.error("Parameter tour is null !!");
        }
        return false;
    }

    /**
     * Get data to play a deal on tournament
     * @param session
     * @param tournamentID
     * @param conventionProfil
     * @param conventionValue
     * @param cardsConventionProfil
     * @param cardsConventionValue
     * @return
     * @throws FBWSException
     */
    public WSTableTournament playTournament(FBSession session, String tournamentID, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) throws FBWSException {
        if (session == null) {
            log.error("Session is null");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        Table table = null;
        TourFederationTournament tour = null;
        TourFederationMemTournament memTour = null;

        //***********************
        // Get Tournament -> check table from session ?????????->???
        if (session.getCurrentGameTable() != null &&
                !session.getCurrentGameTable().isReplay() &&
                session.getCurrentGameTable().getTournament() != null &&
                session.getCurrentGameTable().getTournament().getCategory() == category &&
                session.getCurrentGameTable().getTournament().getIDStr().equals(tournamentID)) {
            table = session.getCurrentGameTable();
            tour = (TourFederationTournament)table.getTournament();
            if (tour == null) {
                log.error("No tournament found on table ... tourID="+tournamentID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        else {
            tour = (TourFederationTournament) getTournament(tournamentID);
            if (tour == null) {
                log.error("No tournament found with this ID="+tournamentID);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
        }

        //*************************
        // Check tournament
        if (tour.isFinished()) {
            throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
        }
        if (!tour.isDateValid(System.currentTimeMillis())) {
            throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
        }
        if (!tour.isEnableToPlay()) {
            log.error("Tournament is not yet enable to play ... tour="+tour);
            throw new FBWSException(FBExceptionType.TOURNAMENT_COMING_SOON);
        }
        memTour = (TourFederationMemTournament) memoryMgr.getTournament(tour.getIDStr());
        if (memTour == null) {
            log.error("No tournament found in memory ... tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }

        // check player is register on tournament ...
        if (tour.getPlayerRegistration(session.getPlayer().getID()) == null) {
            log.error("Player is not register !! tour="+tour+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.TOURNAMENT_PLAYER_NOT_REGISTERED);
        }

        //********************
        // Need to build table
        if (table == null) {
            table = new Table(session.getPlayer(), tour);
            // set nb deals played
            GenericMemTournamentPlayer memTourPlayer = memTour.getTournamentPlayer(session.getPlayer().getID());
            if (memTourPlayer != null) {
                table.setPlayedDeals(memTourPlayer.playedDeals);
            }
            // retrieve game if exist
            TourFederationGame game = getGameNotFinishedOnTournamentForPlayer(tour.getIDStr(), session.getPlayer().getID());
            if (game != null) {
                table.setGame(game);
            }
        }
        session.setCurrentGameTable(table);

        //*******************
        // retrieve game
        TourFederationGame game = null;
        if (table.getGame() != null &&
                !table.getGame().isReplay() &&
                !table.getGame().isFinished() &&
                table.getGame().getTournament().getIDStr().equals(tournamentID)) {
            game = (TourFederationGame)table.getGame();
        } else {
            // need to retrieve an existing game
            game = getGameNotFinishedOnTournamentForPlayer(tour.getIDStr(), session.getPlayer().getID());

            // no game in progress ...
            // need to create a new game
            if (game == null) {
                Player player = playerMgr.getPlayer(session.getPlayer().getID());
                if(!player.isDateSubscriptionValid() && player.getTotalCreditAmount() < 1){
                    throw new FBWSException(FBExceptionType.GAME_PLAYER_CREDIT_EMPTY);
                }

                GenericMemTournamentPlayer memTournamentPlayer = memTour.getTournamentPlayer(session.getPlayer().getID());
                if (memTournamentPlayer != null && memTournamentPlayer.getNbPlayedDeals() >= tour.getNbDealsToPlay()) {
                    log.error("Player has played "+memTournamentPlayer.getNbPlayedDeals()+" deals. No more deals to play for this tour="+tour);
                    throw new FBWSException(FBExceptionType.TOURNAMENT_FINISHED);
                }
                String nextDealID = memTour.getNextDealIDToPlay(session.getPlayer().getID());
                if (nextDealID == null) {
                    log.error("Failed to get nextDealID for player="+session.getPlayer()+" - memTour="+memTour);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                int idxNextDealID = _extractDealIndexFromDealID(nextDealID);
                if (idxNextDealID <= 0 || idxNextDealID > tour.getNbDeals()) {
                    log.error("Failed to extract deal index from id="+nextDealID);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                // create game
                int playerIndex = 1;
                if (memTournamentPlayer != null) {
                    playerIndex = memTournamentPlayer.getNbPlayedDeals()+1;
                }

                game = createGame(session.getPlayer().getID(), tour, idxNextDealID, playerIndex, conventionProfil, conventionValue, cardsConventionProfil, cardsConventionValue, session.getDeviceID());

                if (game != null) {
                    ContextManager.getPlayerMgr().updatePlayerNbDealPlayed(session.getPlayer(), 1);
                } else {
                    log.error("Failed to create game - player="+session.getPlayer().getID()+" - tour="+tour);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                if (!player.isDateSubscriptionValid()){
                    session.getPlayer().decrementCreditAmount(1);
                    player.decrementCreditAmount(1);
                    playerMgr.updatePlayerToDB(player,PlayerMgr.PlayerUpdateType.CREDIT);
                }

                session.incrementNbDealPlayed(category, 1);
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

        TourFederationMemTournamentPlayer memTourPlayer = null;
        // add player on tournament memory
        synchronized (getLockOnTournament(tour.getIDStr())) {
            memTourPlayer = (TourFederationMemTournamentPlayer) memTour.getOrCreateTournamentPlayer(session.getPlayer().getID());
            memTourPlayer.currentDealID = game.getDealID();
        }

        //*********************
        // Build data to return
        WSTableTournament tableTour = new WSTableTournament();
        tableTour.tournament = toWSTournament(tour, session.getPlayerCache());
//        tableTour.tournament.currentDealIndex = memTourPlayer.getNbPlayedDeals()+1;
        tableTour.currentDeal = new WSGameDeal();
        tableTour.currentDeal.setDealData(game.getDeal(), tournamentID);
        tableTour.currentDeal.setGameData(game);
        tableTour.table = table.toWSTableGame();
        tableTour.gameIDstr = game.getIDStr();
        tableTour.conventionProfil = game.getConventionProfile();
        tableTour.creditAmount = session.getPlayer().getTotalCreditAmount();
        tableTour.nbPlayedDeals = session.getPlayer().getNbPlayedDeals();
        tableTour.replayEnabled = playerMgr.isReplayEnabledForPlayer(session);
        tableTour.freemium = session.isFreemium();
        return tableTour;
    }

    /**
     * Call the finish method for each tournament with finished false and dateEnd expired
     * @return
     */
    public List<TourFederationTournament> processFinishTournament() {
        // list of tournament with status not finish and expired
        Query q = Query.query(Criteria.where("finished").is(false).andOperator(Criteria.where("endDate").lt(System.currentTimeMillis())));
        q.with(new Sort(Sort.Direction.ASC, "startDate"));
        List<TourFederationTournament> tournamentsToFinish = (List<TourFederationTournament>)getMongoTemplate().find(q, getTournamentEntity());
        List<TourFederationTournament> tournamentsFinished = new ArrayList<>();
        for (TourFederationTournament t : tournamentsToFinish) {
            if (finishTournament(t)) {
                tournamentsFinished.add(t);
            }
        }
        return tournamentsFinished;
    }

    /**
     * Finish the tournament associated to this memory data
     * @param tour
     */
    public boolean finishTournament(Tournament tour) {
        synchronized (lockFinishTournament) {
            long tsBegin = System.currentTimeMillis();
            if (tour != null && !tour.isFinished() && tour.getEndDate() < System.currentTimeMillis()) {
                TourFederationMemTournament memTour = (TourFederationMemTournament) memoryMgr.getTournament(tour.getIDStr());
                if (memTour != null) {
                    if (!memTour.closeInProgress) {
                        memTour.closeInProgress = true;
                        List<TourFederationTournamentPlayer> listTourPlay = null;
                        // list with all player on tournament
                        Set<Long> listPlayer = memTour.tourPlayer.keySet();
                        try {
                            if (tour.getNbDeals() > 0) {
                                // save game in progress
                                int nbGamesInSession = 0;
                                List<Session> listSession = presenceMgr.getAllCurrentSession();
                                for (Session s : listSession) {
                                    FBSession e = (FBSession) s;
                                    if (e.getCurrentGameTable() != null &&
                                            e.getCurrentGameTable().getGame() != null &&
                                            e.getCurrentGameTable().getGame().getTournament().getCategory() == category &&
                                            e.getCurrentGameTable().getGame().getTournament().getIDStr().equals(tour.getIDStr())) {
                                        if (e.getCurrentGameTable().getGame() instanceof TourFederationGame) {
                                            getMongoTemplate().save(e.getCurrentGameTable().getGame());
                                            e.removeGame();
                                            nbGamesInSession++;
                                        }
                                    }
                                }
                                log.warn("Nb games in session saved before process finish - tour=" + tour + " - nbGamesInSession=" + nbGamesInSession);

                                // loop on all deals to add in memory all game not finished (set leave or claim)
                                for (TourFederationMemDeal memDeal : (TourFederationMemDeal[]) memTour.deals) {
                                    List<TourFederationGame> listGameNotFinished = listGameOnTournamentForDealNotFinished(memTour.tourID, memDeal.dealIndex);
                                    if (listGameNotFinished != null) {
                                        for (TourFederationGame game : listGameNotFinished) {
                                            game.setFinished(true);
                                            if (game.getBidContract() != null) {
                                                game.claimForPlayer(BridgeConstantes.POSITION_SOUTH, 0);
                                                GameMgr.computeScore(game);
                                            } else {
                                                game.setLeaveValue();
                                                game.setLastDate(tour.getEndDate() - 1);
                                            }
                                            TourFederationMemTournamentPlayer mtrPla = (TourFederationMemTournamentPlayer) memTour.addResult(game, false, true);
                                        }
                                    }
                                    if (listGameNotFinished.size() > 0) {
                                        updateListGameDB(listGameNotFinished);
                                    }
                                }

                                // For each player check nb deals played and complete with game leaved
                                List<TourFederationGame> listGameToInsert = new ArrayList<>();
                                for (TourFederationMemTournamentPlayer memTournamentPlayer : new ArrayList<TourFederationMemTournamentPlayer>(memTour.tourPlayer.values())) {
                                    while (memTournamentPlayer.getNbPlayedDeals() < tour.getNbDealsToPlay()) {
                                        // need to be sure that nbDealsToPlay <= nbDeals on tournament
                                        String dealID = memTour.getNextDealIDToPlay(memTournamentPlayer.playerID);
                                        if (dealID == null) {
                                            log.error("Failed to get next dealId to play for playerID="+memTournamentPlayer.playerID+" - memTour="+memTour);
                                            break;
                                        }
                                        if (dealID != null) {
                                            TourFederationGame g = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), extractDealIndexFromDealID(dealID), memTournamentPlayer.playerID);
                                            if (g == null) {
                                                g = buildGame(memTournamentPlayer.playerID, (TourFederationTournament) tour, extractDealIndexFromDealID(dealID), memTournamentPlayer.getNbPlayedDeals() + 1);
                                                g.setStartDate(tour.getEndDate() - 1);
                                                g.setLastDate(tour.getEndDate() - 1);
                                                g.setLeaveValue();
                                                g.setLastDate(tour.getEndDate());
                                                listGameToInsert.add(g);
                                            }
                                            memTour.addResult(g, false, true);
                                        }
                                    }
                                }
                                // case of registered players who have not played tournament
                                for (Long e : ((TourFederationTournament) tour).getMapRegistration().keySet()) {
                                    if (memTour.getTournamentPlayer(e) == null) {
                                        TourFederationMemTournamentPlayer memTournamentPlayer = (TourFederationMemTournamentPlayer) memTour.getOrCreateTournamentPlayer(e);
                                        while (memTournamentPlayer.getNbPlayedDeals() < tour.getNbDealsToPlay()) {
                                            String dealID = memTour.getNextDealIDToPlay(memTournamentPlayer.playerID);
                                            if (dealID == null) {
                                                log.error("Failed to get next dealId to play for playerID="+memTournamentPlayer.playerID+" - memTour="+memTour);
                                                break;
                                            }
                                            if (dealID != null && dealID.length() > 0) {
                                                TourFederationGame g = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), extractDealIndexFromDealID(dealID), memTournamentPlayer.playerID);
                                                if (g == null) {
                                                    g = buildGame(memTournamentPlayer.playerID, (TourFederationTournament) tour, extractDealIndexFromDealID(dealID), memTournamentPlayer.getNbPlayedDeals() + 1);
                                                    g.setStartDate(tour.getEndDate() - 1);
                                                    g.setLastDate(tour.getEndDate() - 1);
                                                    g.setLeaveValue();
                                                    g.setLastDate(tour.getEndDate());
                                                    listGameToInsert.add(g);
                                                }
                                                memTour.addResult(g, false, true);
                                            } else if (dealID != null && dealID.length() == 0) {
                                                // no more deals available ... stop !
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (listGameToInsert.size() > 0) {
                                    insertListGameDB(listGameToInsert);
                                }

                                // loop on all deals to update rank & result
                                for (TourFederationMemDeal memDeal : (TourFederationMemDeal[]) memTour.deals) {
                                    // loop all player of the deal to update game in DB (rank & result)
                                    List<TourFederationGame> listGameToUpdate = new ArrayList<>();
                                    for (Map.Entry<Long, TourFederationMemDealPlayer> e : ((Map<Long, TourFederationMemDealPlayer>) memDeal.mapPlayerResult).entrySet()) {
                                        TourFederationGame game = getGameOnTournamentAndDealForPlayer(memTour.tourID, memDeal.dealIndex, e.getKey());
                                        if (game != null) {
                                            game.setRank(e.getValue().nbPlayerBetterScore + 1);
                                            game.setResult(e.getValue().result);
                                            listGameToUpdate.add(game);
                                        } else {
                                            log.error("No game found for player=" + e.getKey() + " on tour=" + memTour + " - deal=" + memDeal);
                                        }
                                    }
                                    if (listGameToUpdate.size() > 0) {
                                        updateListGameDB(listGameToUpdate);
                                    }

                                    // set nb players on tournament deal
                                    TourFederationDeal deal = (TourFederationDeal) tour.getDeal(memDeal.dealID);
                                    if (deal != null) {
                                        deal.setNbPlayers(memDeal.getNbPlayers());
                                    }
                                }
                                //******************************************
                                // compute tournament result & ranking
                                memTour.computeResult();
                                memTour.computeRanking(false);

                                //******************************************
                                // insert tournament result to DB
                                listTourPlay = new ArrayList<>();
                                for (TourFederationMemTournamentPlayer mtp : (List<TourFederationMemTournamentPlayer>) memTour.getListMemTournamentPlayer()) {
                                    TourFederationTournamentPlayer tp = buildTournamentPlayer();
                                    tp.setPlayerID(mtp.playerID);
                                    tp.setTournament((TourFederationTournament) tour);
                                    tp.setRank(mtp.ranking);
                                    tp.setResult(mtp.result);
                                    tp.setLastDate(mtp.dateLastPlay);
                                    tp.setStartDate(mtp.dateStart);
                                    tp.setCreationDateISO(new Date());
                                    tp.getPlayedDeals().addAll(mtp.playedDeals);
                                    listTourPlay.add(tp);
                                }
                                // compute points before insert list data
                                computePoints(listTourPlay);
                                // compute funbridge points
                                computeFunbridgePoints(listTourPlay);
                                // sort on player rank to the good order for notif
                                Collections.sort(listTourPlay, new Comparator<TourFederationTournamentPlayer>() {
                                    @Override
                                    public int compare(TourFederationTournamentPlayer o1, TourFederationTournamentPlayer o2) {
                                        return Integer.compare(o1.getRank(), o2.getRank());
                                    }
                                });
                                // generate and send report file to the federation
                                generateReportForTournament((TourFederationTournament) tour, listTourPlay, true);
                                // insert results in DB
                                getMongoTemplate().insertAll(listTourPlay);
                                // update playerFederation (increment points & nb tour played)
                                for (TourFederationTournamentPlayer e : listTourPlay) {
                                    incrementNbTourAndPointsOnPlayerFederation(e.getPlayerID(), 1, e.getPoints());
                                    incrementNbTourAndFunbridgePointsOnPlayer(e.getPlayerID(), 1, e.getFunbridgePoints());
                                    updateTourFederationStat(e.getPlayerID(), e.getFunbridgePoints(), e.getPoints(), e.getStartDate());
                                    updateTourFunbridgePointsStat(e.getPlayerID(), e.getFunbridgePoints(), e.getStartDate());
                                }
                            }
                            //******************************************
                            // set tournament finished & update tournament
                            tour.setFinished(true);
                            tour.setNbPlayers(listPlayer.size());
                            updateTournamentDB(tour);

                            // remove tournament from memory
                            memoryMgr.removeTournament(memTour.tourID);
                            log.warn("Finish tournament=" + tour + " - TS=" + (System.currentTimeMillis() - tsBegin));

                            TourFederationTournament tourFederation =  (TourFederationTournament) tour;
                            if (listTourPlay != null && !listTourPlay.isEmpty()) {
                                // add notif result
                                Map<Integer, String> mapPodium = new HashMap<>();
                                int limit = listTourPlay.size() > 2 ? 3 : listTourPlay.size();
                                for (int i = 0; i < limit; i++) {
                                    TourFederationTournamentPlayer e = listTourPlay.get(i);
                                    PlayerCache pc = playerCacheMgr.getOrLoadPlayerCache(e.getPlayerID());
                                    mapPodium.put(i , (pc != null ? pc.getPseudo() : "???") + "_" + e.getStringPoints());
                                }

                                TourFederationTournament nextTournament = getNextTournament(tour.getStartDate());
                                for (TourFederationTournamentPlayer toutFederationPlayer : listTourPlay) {
                                    MessageNotif notif = notifMgr.createNotifTourFederationFinish(tourFederation, toutFederationPlayer, listPlayer.size(), mapPodium, nextTournament, tourFederation.isEndowed());
                                    FBSession session = presenceMgr.getSessionForPlayerID(toutFederationPlayer.getPlayerID());
                                    if (session != null) {
                                        session.pushEvent(notifMgr.buildEvent(notif, toutFederationPlayer.getPlayerID()));
                                    }
                                }
                            }
                            return true;
                        } catch (Exception e) {
                            log.error("Exception to finish tournament=" + tour, e);
                            memTour.closeInProgress = false;
                        }
                    } else {
                        log.error("MemTournament close in progress - tour=" + tour);
                    }
                } else {
                    // check not game on this tournament played ...
                    List<TourFederationGame> listGame = listGameOnTournament(tour.getIDStr());
                    if (listGame == null || listGame.size() == 0) {
                        log.warn("Nobody played this tour=" + tour);
                        try {
                            tour.setFinished(true);
                            updateTournamentDB(tour);
                            return true;
                        } catch (Exception e) {
                            log.error("Exception to finish tournament=" + tour, e);
                        }
                    } else {
                        log.error("No memTour found while games found for tour=" + tour);
                    }
                }
            } else {
                log.error("Tournament null or already finished or not yet time finished - tour=" + tour);
            }
            return false;
        }
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
        if (table.getTournament() == null || !(table.getTournament() instanceof TourFederationTournament)) {
            log.error("Tournament on table is not Federation table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!(table.getGame() instanceof TourFederationGame)) {
            log.error("Game on table is not Federation table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TourFederationGame game = (TourFederationGame)table.getGame();
        if (!table.getTournament().getIDStr().equals(tournamentID)) {
            log.error("TournamentID  of current game ("+table.getTournament().getIDStr()+") is not same as tournamentID="+tournamentID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TourFederationMemTournament memTour = (TourFederationMemTournament) memoryMgr.getTournament(tournamentID);
        if (memTour == null) {
            log.error("No TourFederationMemTournament "+getFederationName()+" found for tournamentID="+tournamentID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        checkGame(game);
        Player p = session.getPlayer();
        TourFederationTournament tour = game.getTournament();

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
            memoryMgr.updateResult(game);
        }
        // leave game for other deals
        int nbDealNotPlayed = 0;
        TourFederationMemTournamentPlayer memTournamentPlayer = (TourFederationMemTournamentPlayer) memTour.getTournamentPlayer(p.getID());
        if (memTournamentPlayer == null) {
            log.error("No memTournamentPlayer found for tour="+tour+" - player="+p);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        while (memTournamentPlayer.getNbPlayedDeals() < tour.getNbDealsToPlay()) {
            String dealID = memTour.getNextDealIDToPlay(p.getID());
            if (dealID == null) {
                log.error("No next dealID found for playerID="+p.getID()+" on memTour="+memTour);
                break;
            }
            int dealIdx = _extractDealIndexFromDealID(dealID);
            if (dealIdx == -1) {
                log.error("Failed to extract deal index for dealID="+dealID+" - playerID="+p.getID()+" - tour="+tour);
                break;
            }
            if (getGameOnTournamentAndDealForPlayer(tournamentID, dealIdx, p.getID()) == null) {
                TourFederationGame g = buildGame(p.getID(), tour, dealIdx, memTournamentPlayer.getNbPlayedDeals()+1);
                g.setStartDate(System.currentTimeMillis());
                g.setLastDate(System.currentTimeMillis());
                g.setLeaveValue();
                g.setDeviceID(session.getDeviceID());
                getMongoTemplate().insert(g);
                memoryMgr.updateResult(g);
                table.addPlayedDeal(g.getDealID());
                nbDealNotPlayed++;
            } else {
                log.error("A game for this dealIdx already existed ! - dealID="+dealID+" - playerID="+p.getID()+" - tour="+tour);
                break;
            }
        }

        // remove game from session
        session.removeGame();
        // remove player from set
        gameMgr.removePlayerRunning(p.getID());

        // update player data
//        if (nbDealNotPlayed > 0) {
//            playerMgr.updatePlayerNbDealPlayed(p, nbDealNotPlayed);
//            session.incrementNbDealPlayed(category, nbDealNotPlayed);
//
//        }
        if (!p.isDateSubscriptionValid() && nbDealNotPlayed > 0){
            playerMgr.updatePlayerCreditDeal(p,
                    game.getTournament().getNbCreditsPlayDeal()*nbDealNotPlayed,
                    nbDealNotPlayed);
        }

        // return credit remaining of player
        return p.getTotalCreditAmount();
    }

    /**
     * Compute the 'Points Experts' won by each player in a closed tournament
     * @param tourID
     * @throws FBWSException
     */
    public void computePointsForTournament(String tourID) throws FBWSException{
        // Find tournament
        TourFederationTournament tournament = (TourFederationTournament) getTournament(tourID);
        if(tournament == null){
            log.error("No "+getFederationName()+" tournament found for tourID="+tourID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, "No "+getFederationName()+" tournament found for tourID="+tourID);
        }
        if(!tournament.isFinished()){
            log.error(getFederationName()+" tournament "+tourID+" is not finished ! Can't compute the points earned by the players");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, getFederationName()+" tournament "+tourID+" is not finished ! Can't compute the points earned by the players");
        }
        // Get the players results
        List<TourFederationTournamentPlayer> playerResults = listTournamentPlayerForTournament(tourID, 0, -1);
        if(playerResults == null){
            log.error(getFederationName()+" tournament "+tourID+" has no TourFederationTournamentPlayer. Can't compute the points earned by the players");
            return;
        }

        if (computePoints(playerResults)) {
            for(TourFederationTournamentPlayer playerResult : playerResults){
                getMongoTemplate().save(playerResult);
            }
        }
    }

    protected boolean computeFunbridgePoints(List<TourFederationTournamentPlayer> playerResults){
        if (playerResults != null && playerResults.size() > 0) {
            // Compute funbridge points earned for each player
            for(TourFederationTournamentPlayer playerResult : playerResults){
                int points = (int) Math.ceil(2 * ((playerResults.size()+1) - playerResult.getRank()) / Math.log10(playerResult.getRank()+2));
                float coef = (playerResult.getTournament() != null && playerResult.getTournament().getCoefPF() != 0) ? playerResult.getTournament().getCoefPF() : 1;
                playerResult.setFunbridgePoints(Math.round(points*coef));
            }
            return true;
        }
        return false;
    }

    /*************************
     * Create/Update tournaments
     */

    /**
     * Create tournament with settings and for day set by calDay
     * @param settings
     * @param calDay
     * @return
     */
    public TourFederationTournament createTournament(TourFederationGenerateSettingsElement settings, Calendar calDay, String author) {
        if (settings != null && settings.isDateActive(calDay.getTimeInMillis())) {
            TourFederationTournament tour = buildTournament();
            tour.setName(settings.name+" "+sdfDayMonth.format(calDay.getTime()));
            tour.setSubName(settings.subName);
            tour.setNbDealsToPlay(getNbDealsToPlay());
            tour.setResultType(settings.resultType);
            tour.setCreationDateISO(new Date());
            tour.setStartDate(settings.getStartDateForDate(calDay.getTimeInMillis()));
            tour.setEndDate(settings.getEndDateForDate(tour.getStartDate()));
            tour.setRegistrationStartDate(settings.getRegistrationStartDate(tour.getStartDate()));
            int endRegistrationMinutePerDeal = getConfigIntValue("endRegistrationMinutePerDeal", 0);
            tour.setRegistrationEndDate(settings.getRegistrationEndDate(tour.getEndDate(), tour.getNbDealsToPlay(), endRegistrationMinutePerDeal));
            tour.setFree(settings.free);
            tour.setEndowed(settings.endowed);
            tour.setSpecial(settings.special);
            tour.setCoef(settings.coef);
            tour.setAuthor(author);
            tour.setCoefPF(settings.coefPF);
            getMongoTemplate().insert(tour);
            return tour;
        }
        return null;
    }

    @Override
    public void updateTournamentDB(Tournament tour) throws FBWSException {
        if (tour != null && tour instanceof TourFederationTournament) {
            TourFederationTournament tournament = (TourFederationTournament) tour;
            try {
                getMongoTemplate().save(tournament);
            } catch (Exception e) {
                log.error("Exception to save tournament=" + tour, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.error("Tournament not valid ! tournament="+tour);
        }
    }

    public boolean removeTournament(String tourID) {
        TourFederationTournament tour = (TourFederationTournament) getTournament(tourID);
        if (tour != null) {
            if (!tour.isEnableToPlay()) {
                getMongoTemplate().remove(tour);
                memoryMgr.removeTournament(tour.getIDStr());
                return true;
            }
        }
        return false;
    }

    /*************************
     * Get & list tournaments
     */

    /**
     * Return tournament with this startDate
     * @param tsStartDate
     * @return
     */
    public TourFederationTournament getTournamentWithStartDate(long tsStartDate) {
        Query q = Query.query(Criteria.where("startDate").is(tsStartDate));
        List<? extends TourFederationTournament> list = getMongoTemplate().find(q, getTournamentEntity());
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Return tournament with this startDate and resultType
     * @param tsStartDate
     * @return
     */
    public TourFederationTournament getTournamentForStartDateAndResultType(long tsStartDate, int resultType){
        Query q = new Query();
        q.addCriteria(new Criteria().andOperator(Criteria.where("startDate").is(tsStartDate), Criteria.where("resultType").is(resultType)));
        List<? extends TourFederationTournament> list = getMongoTemplate().find(q, getTournamentEntity());
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Return last finished tournament
     * @return
     */
    public WSTournament getLastFinishedTournament(FBSession session) {
        TourFederationTournament tour = findLastFinishedTournament();
        if(tour != null) return toWSTournament(tour, session.getPlayerCache());
        return null;
    }

    /**
     * Return last finished tournament
     * @return
     */
    public TourFederationTournament findLastFinishedTournament() {
        Query q = new Query();
        q.addCriteria(Criteria.where("endDate").lt(System.currentTimeMillis()));
        q.addCriteria(Criteria.where("finished").is(true));
        q.addCriteria(Criteria.where("enableToPlay").is(true));
        q.with(new Sort(Sort.Direction.DESC, "startDate"));
        q.limit(2);
        List<? extends TourFederationTournament> tournaments = getMongoTemplate().find(q, getTournamentEntity());

        if(CollectionUtils.isNotEmpty(tournaments)) {
            // if last tournaments have the same date, take tournament with the most players
            if (tournaments.size() == 2 && tournaments.get(0).getStartDate() == tournaments.get(1).getStartDate()
                    && tournaments.get(0).getNbPlayers() < tournaments.get(1).getNbPlayers()){
                return tournaments.get(1);
            }
            return tournaments.get(0);
        }
        return null;
    }

    /**
     * Return last finished tournament with free status
     * @return
     */
    public TourFederationTournament findLastFinishedTournamentWithFreeStatus(boolean free) {
        Query q = new Query();
        q.addCriteria(Criteria.where("endDate").lt(System.currentTimeMillis()));
        q.addCriteria(Criteria.where("finished").is(true));
        q.addCriteria(Criteria.where("enableToPlay").is(true));
        q.addCriteria(Criteria.where("free").is(free));
        q.with(new Sort(Sort.Direction.DESC, "startDate"));
        q.limit(1);
        return getMongoTemplate().findOne(q, getTournamentEntity());
    }

    public List<TourFederationTournament> listTournamentsBetweenTwoDates(long start, long end){
        Query q = new Query();
        q.addCriteria(new Criteria().andOperator(Criteria.where("startDate").gte(start), Criteria.where("startDate").lt(end)));
        q.with(new Sort(Sort.Direction.ASC, "startDate"));
        return (List<TourFederationTournament>)getMongoTemplate().find(q, getTournamentEntity());
    }

    public List<TourFederationTournament> listOldTournament(int offset, int nbMax) {
        Query q = Query.query(Criteria.where("endDate").lt(System.currentTimeMillis()));
        if (nbMax == 0) {
            nbMax = 50;
        }
        q.with(new Sort(Sort.Direction.DESC, "startDate"));
        if (offset > 0) {
            q.skip(offset);
        }
        q.limit(nbMax);
        return (List<TourFederationTournament>)getMongoTemplate().find(q, getTournamentEntity());
    }

    /**
     * List tournament where status finished is false and enableToPlayer is true
     * @return
     */
    public List<TourFederationTournament> listTournamentInProgress() {
        Criteria c1 = Criteria.where("finished").is(false);
        Criteria c2 = Criteria.where("enableToPlay").is(true);
        Query q = new Query(new Criteria().andOperator(c1, c2));
        q.with(new Sort(Sort.Direction.ASC, "startDate"));
        return (List<TourFederationTournament>)getMongoTemplate().find(q, getTournamentEntity());
    }

    /**
     * List next tournament available
     * @return
     */
    public List<TourFederationTournament> listAvailableTournaments(int nbMaxDay) {
        // tournament not yet finished
        Query q = Query.query(Criteria.where("endDate").gt(System.currentTimeMillis()));
        Calendar limitDate = Calendar.getInstance();
        limitDate.add(Calendar.DATE, nbMaxDay);
        q.addCriteria(Criteria.where("startDate").lte(limitDate.getTimeInMillis()));
        q.with(new Sort(Sort.Direction.ASC, "startDate"));
        return (List<TourFederationTournament>)getMongoTemplate().find(q, getTournamentEntity());
    }

    public List<TourFederationTournament> findAvailableTournament(int nbMaxDay, String category, String author, long startDate, long endDate){
        // tournament not yet finished
        Query q = new Query();
        q.with(new Sort(Sort.Direction.ASC, "startDate"));

        if(category != null && !category.isEmpty()) {
            switch (category){
                case "FREE": q.addCriteria(Criteria.where("free").is(true)); break;
                case "ENDOWED": q.addCriteria(Criteria.where("endowed").is(true)); break;
                case "SPECIAL": q.addCriteria(Criteria.where("special").is(true)); break;
            }
        }

        if(author != null && !author.isEmpty()){ q.addCriteria(Criteria.where("author").regex(author, "i")); }
        if(startDate > 0){ q.addCriteria(Criteria.where("startDate").gte(startDate)); }
        else {
            Calendar startSearch = Calendar.getInstance();
            startSearch.add(Calendar.DATE, -2);
            Calendar limitDate = Calendar.getInstance();
            limitDate.add(Calendar.DATE, nbMaxDay);
            q.addCriteria(Criteria.where("startDate").lte(limitDate.getTimeInMillis()).andOperator(Criteria.where("startDate").gte(startSearch.getTimeInMillis())));
        }

        if(endDate > 0){ q.addCriteria(Criteria.where("endDate").lte(endDate)); }
        else{ Criteria.where("endDate").gt(System.currentTimeMillis()); }

        return (List<TourFederationTournament>)getMongoTemplate().find(q, getTournamentEntity());
    }

    public TourFederationTournament getNextTournament(long currentTourStartDate) {
        Query q = Query.query(Criteria.where("startDate").gt(currentTourStartDate));
        q.with(new Sort(Sort.Direction.ASC, "startDate"));
        return getMongoTemplate().findOne(q, getTournamentEntity());
    }

    /**
     * List tournament available for player
     * @param playerID
     * @return
     */
    public List<WSTournamentFederation> listTournamentForPlayer(long playerID) {
        long curTS = System.currentTimeMillis();
        List<WSTournamentFederation> result = new ArrayList<>();
        for (TourFederationTournament e : listAvailableTournaments(getConfigIntValue("nbMaxDayListTournament", 8))) {
            // Check tournament : register open or enable to play
            if ((e.getRegistrationEndDate() > curTS && e.getRegistrationStartDate() < curTS) || (e.isDateValid(curTS))) {
                WSTournamentFederation ws = tournament2WS(e, playerID);
                if (ws != null) {
                    result.add(ws);
                }
            }
        }
        return result;
    }

    /**
     * List game for a player on a tournament (order by playerIndex asc)
     * @param tourID
     * @param playerID
     * @return
     */
    public List<TourFederationGame> listGameOnTournamentForPlayer(String tourID, long playerID) {
        Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("playerID").is(playerID)));
        q.with(new Sort(Sort.Direction.ASC, "playerIndex"));
        List<TourFederationGame> result = (List<TourFederationGame>)getMongoTemplate().find(q, getGameEntity());
        return result;
    }

    /**
     * Transform tournament to WS object
     * @param tour
     * @param playerID
     * @return
     */
    public WSTournamentFederation tournament2WS(TourFederationTournament tour, long playerID) {
        if (tour != null) {
            WSTournamentFederation ws = new WSTournamentFederation();
            ws.tourID = tour.getIDStr();
            ws.dateStart = tour.getStartDate();
            ws.dateEnd = tour.getEndDate();
            ws.nbPlayersRegistered = tour.countNbPlayersRegistered();
            ws.resultType = tour.getResultType();
            List<Long> friendsIDs = playerMgr.listFriendIDForPlayer(playerID);
            for(Long friendID : friendsIDs){
                if(tour.getMapRegistration().containsKey(friendID)) ws.nbFriendsRegistered++;
            }
            TourFederationMemTournamentPlayer memTourPlayer = null;
            if (playerID > 0) {
                ws.isPlayerRegistered = tour.getPlayerRegistration(playerID) != null;
                GenericMemTournament memTournament = memoryMgr.getTournament(tour.getIDStr());
                if (memTournament != null) {
                    memTourPlayer = (TourFederationMemTournamentPlayer) memTournament.getTournamentPlayer(playerID);
                    if (memTourPlayer != null) {
                        ws.listPlayedDeals.addAll(memTourPlayer.playedDeals);
                    }
                }
            }
            ws.status = getTournamentStatus(tour, memTourPlayer, presenceMgr.getSessionForPlayerID(playerID));
            ws.free = tour.isFree();
            ws.endowed = tour.isEndowed();
            ws.special = tour.isSpecial();
            ws.subName = tour.getSubName();
            ws.nbDeals = tour.getNbDealsToPlay();
            return ws;
        }
        return null;
    }

    public int getTournamentStatus(TourFederationTournament tour, TourFederationMemTournamentPlayer memTourPlayer, FBSession session) {
        long curTS = System.currentTimeMillis();
        if (curTS < tour.getRegistrationStartDate()) {
            return TOUR_STATUS_NO_REGISTER;
        }
        if (curTS > tour.getRegistrationStartDate() && curTS < tour.getRegistrationEndDate()) {
            if (tour.isEnableToPlay()) {
                if (tour.isDateValid(curTS)) {
                    if (memTourPlayer != null && memTourPlayer.isPlayerFinish()){
                        return TOUR_STATUS_PLAYED;
                    }
                    if(session != null && tour.getPlayerRegistration(session.getPlayer().getID()) != null){
                        return TOUR_STATUS_IN_PROGRESS;
                    }
                }
            } else {
                if (tour.isDateValid(curTS) && tour.getProcessPrepareDate() != 0) {
                    return TOUR_STATUS_CANCELED;
                }
            }
            return TOUR_STATUS_REGISTER_ENABLED;
        }
        if (curTS > tour.getRegistrationEndDate()) {
            if (tour.isEnableToPlay()) {
                if (tour.isDateValid(curTS)) {
                    if (memTourPlayer != null && memTourPlayer.isPlayerFinish()) {
                        return TOUR_STATUS_PLAYED;
                    }
                    return TOUR_STATUS_IN_PROGRESS;
                }
            } else {
                if (tour.isDateValid(curTS) && tour.getProcessPrepareDate() != 0) {
                    return TOUR_STATUS_CANCELED;
                }
            }
        }
        return TOUR_STATUS_NO_REGISTER;
    }

    /**
     * Transform tournament for webservice object
     * @param tour
     * @param playerCache
     * @return
     */
    @Override
    public <T1 extends Tournament, T2 extends TournamentPlayer> WSTournament toWSTournament(T1 tour, PlayerCache playerCache) {
        if (tour != null) {
            WSTournament wst = tour.toWS();
            GenericMemTournament memTournament = memoryMgr.getTournament(tour.getIDStr());
            if (memTournament != null) {
                wst.nbTotalPlayer = memTournament.getNbPlayer();
                TourFederationMemTournamentPlayer memTourPlayer = (TourFederationMemTournamentPlayer) memTournament.getTournamentPlayer(playerCache.ID);
                if (memTourPlayer != null) {
                    wst.resultPlayer = memTourPlayer.toWSResultTournamentPlayer(true);
                    wst.resultPlayer.setPlayerID(playerCache.ID);
                    wst.resultPlayer.setPlayerPseudo(playerCache.getPseudo());
                    wst.resultPlayer.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(playerCache.ID));
                    wst.resultPlayer.setAvatarPresent(playerCache.avatarPresent);
                    wst.resultPlayer.setCountryCode(playerCache.countryCode);
                    wst.resultPlayer.setPlayerSerie(playerCache.serie);
                    // arrayDealIDStr contains list of played deals
                    for (int i = 0; i < memTourPlayer.playedDeals.size(); i++) {
                        if (i < wst.arrayDealIDstr.length) {
                            wst.arrayDealIDstr[i] = memTourPlayer.playedDeals.get(i);
                        }
                    }

                    if (memTourPlayer.currentDealID != null) {
                        if (!memTourPlayer.playedDeals.contains(memTourPlayer.currentDealID)) {
                            if (memTourPlayer.playedDeals.size() < wst.arrayDealIDstr.length) {
                                wst.arrayDealIDstr[memTourPlayer.playedDeals.size()] = memTourPlayer.currentDealID;
                            }
                        }
                    }
                    if (memTourPlayer.currentDealID != null) {
                        wst.currentDealIndex = memTourPlayer.getNbPlayedDeals() + 1;
                    }
                }
                if (wst.resultPlayer != null) {
                    wst.nbTotalPlayer = memTournament.getNbPlayerFinishAll();
                }
                wst.remainingTime = tour.getEndDate() - System.currentTimeMillis();
                if (tour.getEndDate() < System.currentTimeMillis()) {
                    // tournament closed but process finish not yet run
                    wst.resultPlayer.setMasterPoints(-2);
                    wst.resultPlayer.setFbPoints(-2);
                }
            } else {
                wst.nbTotalPlayer = tour.getNbPlayers();
                TourFederationTournamentPlayer tourPlayer = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
                if (tourPlayer != null) {
                    wst.resultPlayer = tourPlayer.toWSResultTournamentPlayer(playerCache, playerCache.ID);
                    wst.arrayDealIDstr = tourPlayer.getArrayPlayDeals();
                    wst.resultPlayer.setMasterPoints(tourPlayer.getPoints());
                    wst.resultPlayer.setFbPoints(tourPlayer.getFunbridgePoints());
                }
            }
            if (wst.resultPlayer != null) {
                wst.playerOffset = wst.resultPlayer.getRank();
            }
            return wst;
        }
        return null;
    }

    @Override
    public <T extends TournamentPlayer> WSTournamentArchive toWSTournamentArchive(T e) {
        WSTournamentArchive archive = super.toWSTournamentArchive(e);
        if(archive != null){
            archive.countDeal = ((TourFederationTournamentPlayer)e).getTournament().getNbDealsToPlay();
            archive.listPlayedDeals = new ArrayList<>(((TourFederationTournamentPlayer)e).getPlayedDeals());
            archive.masterPoints = ((TourFederationTournamentPlayer)e).getPoints();
        }
        return archive;
    }

    /**
     * List the first 3 (order by rank) on tournament
     * @param tourID
     * @return
     */
    public <T extends TourFederationTournamentPlayer> List<T> listTournamentPlayerPodium(String tourID) {
        return (List<T>)getMongoTemplate().find(
                Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID))).
                        with(new Sort(Sort.Direction.ASC, "rank")).
                        limit(3),
                getTournamentPlayerEntity()
        );
    }

    @Override
    public <T extends GenericMemTournamentPlayer> WSTournamentArchive toWSTournamentArchive(T e) {
        WSTournamentArchive archive = super.toWSTournamentArchive(e);
        if (archive != null) {
            archive.countDeal = ((TourFederationMemTournament)e.memTour).nbDealsToPlay;
            archive.listPlayedDeals = new ArrayList<>(e.playedDeals);
            if (e.memTour.endDate < System.currentTimeMillis()) {
                // tournament closed but process finish not yet run
                archive.masterPoints = -2;
            }
            return archive;
        }
        return null;
    }

    /***********
     * Results
     */

    /**
     * Return list of result deal on tournament for player
     * @param tour
     * @param playerID
     * @return
     */
    public List<WSResultDeal> resultListDealForTournamentForPlayer(Tournament tour, long playerID) throws FBWSException {
        if (tour == null) {
            log.error("Parameter not valid");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        List<WSResultDeal> listResultDeal = new ArrayList<WSResultDeal>();
        TourFederationMemTournament mtr = (TourFederationMemTournament) memoryMgr.getTournament(tour.getIDStr());

        if (mtr != null) {
            // tour in memory
            TourFederationMemTournamentPlayer memTournamentPlayer = (TourFederationMemTournamentPlayer) mtr.getTournamentPlayer(playerID);
            if (memTournamentPlayer != null && memTournamentPlayer.playedDeals != null) {
                for (String dealID : memTournamentPlayer.playedDeals) {
                    GenericMemDeal memDeal = mtr.getMemDeal(dealID);
                    if (memDeal != null) {
                        TourFederationMemDealPlayer memDealPlayer = (TourFederationMemDealPlayer) memDeal.getResultPlayer(playerID);
                        if (memDealPlayer != null) {
                            WSResultDeal resultPlayer = memDealPlayer.toWSResultDeal(memDeal);
                            listResultDeal.add(resultPlayer);
                        }
                    }
                }
                // complete with not played deals : played=false + dealIndex
                if (listResultDeal.size() < tour.getNbDealsToPlay()) {
                    while (listResultDeal.size() < tour.getNbDealsToPlay()) {
                        WSResultDeal resultPlayer = new WSResultDeal();
                        resultPlayer.setPlayed(false);
                        resultPlayer.setDealIndex(listResultDeal.size()+1);
                        listResultDeal.add(resultPlayer);
                    }
                }
            }
        }
        else {
            // get data from DB
            TourFederationTournamentPlayer tournamentPlayer = getTournamentPlayer(tour.getIDStr(), playerID);
            if (tournamentPlayer != null) {
                List<? extends TourFederationGame> listGame = listGameOnTournamentForPlayer(tour.getIDStr(), playerID);
                for (TourFederationGame game : listGame) {
                    if (!game.isFinished()) {
                        continue;
                    }
                    WSResultDeal resultPlayer = game.toWSResultDeal();
                    listResultDeal.add(resultPlayer);
                }
            }
        }
        return listResultDeal;
    }

    @Override
    public <T1 extends Game> WSResultDealTournament getWSResultDealTournamentGroupped(String dealID, PlayerCache playerCache, boolean useLead) throws FBWSException {
        // Generic stuff
        WSResultDealTournament result = super.getWSResultDealTournamentGroupped(dealID, playerCache, useLead);
        // Fill the arrayDealIDstr
        TourFederationTournamentPlayer tp = getTournamentPlayer(result.tournament.tourIDstr, playerCache.ID);
        if(tp != null){
            result.tournament.arrayDealIDstr = tp.getArrayPlayDeals();
        }
        return result;
    }

    /***********
     * Players
     */

    /**
     * Extract data from DBObject to build PlayerFederation
     * @param obj
     * @return
     */
    private PlayerFederation transformDBObjectToPlayerFederation(Document obj) {
        if (obj != null) {
            try {
                PlayerFederation result = new PlayerFederation();
                result.playerId = obj.getLong("_id");
                if (obj.containsKey("playerTouchId")) {
                    result.playerTouchId = obj.getLong("playerTouchId");
                }
                result.licence = obj.getString("licence");
                result.nbBonusCredits = obj.getInteger("nbBonusCredits");
                result.nbBoughtCredits = obj.getInteger("nbBoughtCredits");
                result.nbBoughtCreditsPlayed = obj.getInteger("nbBoughtCreditsPlayed");
                result.nbBonusCreditsPlayed = obj.getInteger("nbBonusCreditsPlayed");
                if (obj.containsKey("pointsEarned")) {
                    if (obj.get("pointsEarned") instanceof Long) {
                        result.pointsEarned = obj.getLong("pointsEarned");
                    } else {
                        result.pointsEarned = obj.getDouble("pointsEarned");
                    }
                }
                if (obj.containsKey("tournamentsPlayed")) {
                    result.tournamentsPlayed = obj.getInteger("tournamentsPlayed");
                }
                if (obj.containsKey("firstname")) {
                    result.firstname = obj.getString("firstname");
                }
                if (obj.containsKey("lastname")) {
                    result.lastname = obj.getString("lastname");
                }
                if (obj.containsKey("club")) {
                    result.club = obj.getString("club");
                }
                return result;
            } catch (Exception e) {
                log.error("Failed to transform DBObject to PlayerFederation "+getFederationName()+" - obj="+obj, e);
            }
        }
        return null;
    }

    /**
     * Increment nb tournament played and nb points earned
     * @param playerTouchID
     * @param nbTourPlayed
     * @param nbPoints
     * @return
     */
    public boolean incrementNbTourAndPointsOnPlayerFederation(long playerTouchID, int nbTourPlayed, double nbPoints) {
        if (collectionPlayerFederation != null) {
            BasicDBObject updateFields = new BasicDBObject();
            updateFields.append("$inc", new BasicDBObject().append("tournamentsPlayed", nbTourPlayed).append("pointsEarned", nbPoints));
            BasicDBObject query = new BasicDBObject();
            query.append("playerTouchId", playerTouchID);
            UpdateResult ur = collectionPlayerFederation.updateMany(query, updateFields);
            return ur != null && ur.getModifiedCount() > 0;
        } else {
            log.error("Collection PlayerFederation "+getFederationName()+" is null !!");
        }
        return false;
    }

    /**
     * Increment nb tournament played and nb points earned
     * @param playerID
     * @param nbTourPlayed
     * @param nbPoints
     * @return
     */
    protected boolean incrementNbTourAndFunbridgePointsOnPlayer(long playerID, int nbTourPlayed, int nbPoints) {
        synchronized (lockPlayer.getLock(""+playerID)) {
            PlayerFunbridgePoints playerFunbridgePoints = getPlayerFunbridgePoints(playerID);
            boolean insert = false;
            if (playerFunbridgePoints == null) {
                playerFunbridgePoints = new PlayerFunbridgePoints();
                playerFunbridgePoints.setPlayerID(playerID);
                insert = true;
            }
            playerFunbridgePoints.setPointsEarned(playerFunbridgePoints.getPointsEarned() + nbPoints);
            playerFunbridgePoints.setTournamentsPlayed(playerFunbridgePoints.getTournamentsPlayed() + nbTourPlayed);
            if (!insert) {
                funbridgeMongoTemplate.save(playerFunbridgePoints);
                return true;
            } else {
                funbridgeMongoTemplate.insert(playerFunbridgePoints);
                return true;
            }
        }
    }

    /**
     * Return PlayerFederation data for this touchID
     * @param playerTouchID
     * @return
     * @throws FBWSException
     */
    public PlayerFederation getPlayerFederationwithTouchID(long playerTouchID) throws FBWSException{
        if (collectionPlayerFederation != null) {
            FindIterable<Document> objResult = collectionPlayerFederation.find(BasicDBObject.parse("{playerTouchId:"+playerTouchID+"}"));
            if (objResult != null) {
                return transformDBObjectToPlayerFederation(objResult.first());
            }
            return null;
        } else {
            log.error("Collection PlayerFederation is null !!");
        }
        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
    }

    /**
     * Return PlayerFunbridgePoints data for playerID.
     * @param playerID
     * @return
     */
    private PlayerFunbridgePoints getPlayerFunbridgePoints(long playerID) {
        return funbridgeMongoTemplate.findOne(Query.query(Criteria.where("playerID").is(playerID)), PlayerFunbridgePoints.class);
    }

    /***************
     * Registering
     */

    /**
     * Register a player in a tournament
     * @param playerID
     * @param tourID
     * @return
     * @throws FBWSException
     */
    public synchronized RegisterTourFederationResult registerPlayerInTournament(long playerID, String tourID, boolean checkTournament) throws FBWSException {
        // Find tournament
        TourFederationTournament tournament = (TourFederationTournament) getTournament(tourID);
        if(tournament == null){
            log.error(getFederationName()+" tournament not found for ID="+tourID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID, getFederationName()+" tournament not found for ID="+tourID);
        }
        if (checkTournament) {
            // Check if registrations are open ????????
            if (tournament.getRegistrationStartDate() > System.currentTimeMillis() || tournament.getRegistrationEndDate() < System.currentTimeMillis()) {
                log.error("Registrations for "+getFederationName()+" tournament " + tourID + " are closed");
                throw new FBWSException(FBExceptionType.TOURNAMENT_FEDERATION_REGISTRATION_CLOSED, "Registrations for "+getFederationName()+" tournament " + tourID + " are closed");
            }
            // Check if player is not already registered ???????????
            if (tournament.getPlayerRegistration(playerID) != null) {
                log.error("Player " + playerID + " already registered in tournament " + tourID);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID, "Player " + playerID + " already registered in tournament " + tourID);
            }
        }
        PlayerFederation playerFederation = null;
        final TourFederationMgr mgr = tournament.getTourFederationMgr();
        if(mgr == null) {
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        playerFederation = mgr.getOrCreatePlayerFederation(playerID, mgr.getFederationName());
//        if (!tournament.isFree() && !playerMgr.getPlayer(playerID).isDateSubscriptionValid() && playerMgr.getPlayer(playerID).getCreditAmount() < tournament.getNbDealsToPlay()) {
//            throw new FBWSException(FBExceptionType.GAME_PLAYER_CREDIT_EMPTY);
//        }


        tournament.addPlayerRegistration(playerID);
        getMongoTemplate().save(tournament);
        if (playerFederation != null) {
            RegisterTourFederationResult result = new RegisterTourFederationResult();
            result.credit = 0;
            result.nbPlayersRegistered = tournament.countNbPlayersRegistered();
            return result;
        } else {
            log.error("playerFederation "+getFederationName()+" is null for playerID="+playerID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Unregister a player from a tournament
     * @param playerID
     * @param tourID
     * @return
     * @throws FBWSException
     */
    public synchronized RegisterTourFederationResult unregisterPlayerFromTournament(long playerID, String tourID, boolean checkTournament) throws FBWSException {
        // Find tournament
        TourFederationTournament tournament = (TourFederationTournament) getTournament(tourID);
        if(tournament == null){
            log.error(getFederationName()+" tournament not found for ID="+tourID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID, getFederationName()+" tournament not found for ID="+tourID);
        }
        if (checkTournament) {
            // Check if registrations are open
            if (tournament.getRegistrationStartDate() > System.currentTimeMillis() || tournament.getRegistrationEndDate() < System.currentTimeMillis()) {
                log.error("Registrations for "+getFederationName()+" tournament " + tourID + " are closed");
                throw new FBWSException(FBExceptionType.TOURNAMENT_FEDERATION_REGISTRATION_CLOSED, "Registrations for "+getFederationName()+" tournament " + tourID + " are closed");
            }
        }
        // Check if player is registered to tournament and what type of credit he used
        TourFederationRegistration registration = tournament.getPlayerRegistration(playerID);
        if (registration == null) {
            log.error("Player " + playerID + " not yet registered in tournament " + tourID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID, "Player " + playerID + " not yet registered in tournament " + tourID);
        }

        final TourFederationMgr mgr = tournament.getTourFederationMgr();
        if(mgr == null) {
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        PlayerFederation playerFederation = mgr.getOrCreatePlayerFederation(playerID, mgr.getFederationName());

        // Unregister player from tournament
        tournament.removePlayerRegistration(playerID);
        getMongoTemplate().save(tournament);
        if (playerFederation != null) {
            RegisterTourFederationResult result = new RegisterTourFederationResult();
            result.credit = 0;
            result.nbPlayersRegistered = tournament.countNbPlayersRegistered();
            return result;
        } else {
            log.error("playerFederation "+getFederationName()+" is null for playerID="+playerID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }


    /***********
     * Games
     */

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
    protected TourFederationGame createGame(long playerID, TourFederationTournament tour, int dealIndex, int playerIndex, int conventionProfil, String conventionData, int cardsConventionProfil, String cardsConventionValue, long deviceID) throws FBWSException {
        synchronized (lockCreateGame.getLock(tour.getIDStr()+"-"+playerID)) {
            if (getGameOnTournamentAndDealForPlayer(tour.getIDStr(), dealIndex, playerID) != null) {
                log.error("A game already exist on tour="+tour+" - dealIndex="+dealIndex+" - playerID="+playerID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            try {
                TourFederationGame game = buildGame(playerID, tour, dealIndex, playerIndex);
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
                getMongoTemplate().insert(game);
                return game;
            } catch (Exception e) {
                log.error("Exception to create game player="+playerID+" - tour="+tour+" - dealIndex="+dealIndex, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    @Override
    public void updateGameFinished(FBSession session) throws FBWSException {
        if (session == null) {
            log.error("Session parameter is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (session.getCurrentGameTable() == null || session.getCurrentGameTable().getGame() == null || !(session.getCurrentGameTable().getGame() instanceof TourFederationGame)) {
            log.error("Table or game in session not valid ! - table="+session.getCurrentGameTable());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TourFederationGame game = (TourFederationGame) session.getCurrentGameTable().getGame();
        if (!game.isReplay()) {
            if (game.isFinished()) {
                // update data game and table in DB
                updateGameDB(game);

                // update tournament play in memory
                memoryMgr.updateResult(game);

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
        Table table = super.createReplayTable(player, gamePlayed, conventionProfil, conventionValue, cardsConventionProfil, cardsConventionValue);
        TourFederationGame game = (TourFederationGame) table.getGame();
        game.setPlayerIndex(((TourFederationGame)gamePlayed).getPlayerIndex());
        return table;
    }

    @Override
    public Game createReplayGame(long playerID, Tournament tour, int dealIndex, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) {
        if (tour instanceof TourFederationTournament) {
            TourFederationGame replayGame = buildGame(playerID, (TourFederationTournament) tour, dealIndex);
            return replayGame;
        }
        else {
            log.error("Tournament is not instance of TourFederationTournament - tour="+tour+" - playerID="+playerID);
            return null;
        }
    }

    @Override
    public void updateGamePlayArgineFinished(Game game) throws FBWSException {

    }

    @Override
    public void updateGamePlayArgine(Game game) {

    }

    /*************
     *  Ranking
     */

    /**
     * Get federation ranking for players with offset & limit
     * @param playerAsk
     * @param offset
     * @param nbMax
     * @param periodID
     * @return
     */
    public ResultServiceRest.GetMainRankingResponse getRankingFederation(PlayerCache playerAsk, String periodID, List<Long> selectionPlayerID, String countryCode, int offset, int nbMax) {
        if (nbMax == 0) {
            nbMax = 50;
        }
        ResultServiceRest.GetMainRankingResponse response = new ResultServiceRest.GetMainRankingResponse();
        response.totalSize = countRankingFederation(periodID, selectionPlayerID, countryCode);
        response.nbRankedPlayers = countRankingFederation(periodID, null, countryCode);

        // rankingPlayer
        if (playerAsk != null) {
            WSMainRankingPlayer rankingPlayerFederation = new WSMainRankingPlayer(playerAsk, true, playerAsk.ID);
            TourFederationStat tourFederationStat = getMongoTemplate().findById(playerAsk.ID, getTourFederationStatEntity());
            if (tourFederationStat != null) {
                TourFederationStatResult statResult = tourFederationStat.getStatResult(periodID, tourFederationStatPeriodMgr.getStatCurrentPeriodID());
                if (statResult != null) {
                    rankingPlayerFederation.value = Math.floor(statResult.federationPoints*100)/100;
                    rankingPlayerFederation.rank = countNbMoreFederationPoints(periodID, statResult, countryCode, null) + 1;
                    if (offset == -1) {
                        int playerOffset = countNbMoreFederationPoints(periodID, statResult, countryCode, selectionPlayerID) + 1;
                        offset = playerOffset - (nbMax / 2);
                    }
                }
            }
            rankingPlayerFederation.rank = (rankingPlayerFederation.rank == 0)?-1:rankingPlayerFederation.rank;
            response.rankingPlayer = rankingPlayerFederation;
        }
        if (offset < 0) {
            offset = 0;
        }

        response.ranking = getRankingPlayers(playerAsk, periodID, selectionPlayerID, countryCode, offset, nbMax);
        response.offset = offset;
        return response;
    }

    /**
     * return ranking player switch periodId
     */
    public List<WSMainRankingPlayer> getRankingPlayerByPeriod(String periodID){
        if(getFederationName().equals(Constantes.TOURNAMENT_CBO)){
            return getFunbridgePointRankingPlayers(null, periodID, null, null, 0, 0);
        }else{
            return getRankingPlayers(null, periodID, null, null, 0, 0);
        }
    }

    /**
     * Return ranking player for federation (except Funbridge Point federation)
     */
    private List<WSMainRankingPlayer> getRankingPlayers(PlayerCache playerAsk, String periodID, List<Long> selectionPlayerID, String countryCode, int offset, int nbMax) {
        List<WSMainRankingPlayer> rankingPlayers = new ArrayList<>();

        // list stat
        List<TourFederationStat> listStat = this.listTourFederationStat(periodID, offset, nbMax, selectionPlayerID, countryCode);

        int currentRank = -1, nbWithSameFederationPoints = 0;
        double currentNbFederationPoints = -1;
        for (TourFederationStat stat : listStat) {
            long playerAskId = playerAsk != null ? playerAsk.ID : 0;
            WSMainRankingPlayer data = new WSMainRankingPlayer(playerCacheMgr.getPlayerCache(stat.playerID), presenceMgr.isSessionForPlayerID(stat.playerID), playerAskId);
            TourFederationStatResult statResult = stat.getStatResult(periodID, tourFederationStatPeriodMgr.getStatCurrentPeriodID());
            if (statResult != null) {
                // init current counter
                if (currentRank == -1) {
                    currentRank = countNbMoreFederationPoints(periodID, statResult, countryCode, null) + 1;
                    nbWithSameFederationPoints = (offset + 1) - currentRank + 1;
                } else {
                    if (statResult.federationPoints == currentNbFederationPoints) {
                        nbWithSameFederationPoints++;
                    } else {
                        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
                            currentRank = countNbMoreFederationPoints(periodID, statResult, countryCode, null) + 1;
                        } else {
                            currentRank = currentRank + nbWithSameFederationPoints;
                        }
                        nbWithSameFederationPoints = 1;
                    }
                }
                currentNbFederationPoints = statResult.federationPoints;

                data.value = Math.floor(statResult.federationPoints*100)/100;
                data.pfValue =  Math.floor(statResult.funbridgePoints*100)/100;
                data.nbTournmanents = statResult.nbTournaments;
                data.rank = currentRank;
            }
            rankingPlayers.add(data);
        }
        return rankingPlayers;
    }

    /**
     * Count nb players with federation points for the period
     * @param periodID
     * @return
     */
    public int countRankingFederation(String periodID, List<Long> listFollower, String countryCode) {
        Criteria criteria = new Criteria();
        if (listFollower != null && !listFollower.isEmpty()) {
            criteria = Criteria.where("_id").in(listFollower);
        }
        if (StringUtils.isNotBlank(periodID)) {
            criteria = criteria.and("resultPeriod." + periodID).exists(true);
        }
        if (countryCode != null) {
            criteria = criteria.and("countryCode").is(countryCode);
        }
        Query query = new Query(criteria);
        return (int)getMongoTemplate().count(query, getTourFederationStatEntity());
    }

    /**
     * List tour federation stat for a period with offset and nbMax.
     * If period null, return stat total results.
     * @param periodID
     * @param offset
     * @param nbMax
     * @return
     */
    public List<TourFederationStat> listTourFederationStat(String periodID, int offset, int nbMax, List<Long> listFollower, String countryCode) {
        Criteria criteria = new Criteria();
        if (listFollower != null && !listFollower.isEmpty()) {
            criteria = Criteria.where("_id").in(listFollower);
        }

        Sort sort;
        if (StringUtils.isNotBlank(periodID)) {
            criteria = criteria.andOperator(Criteria.where("resultPeriod."+ periodID).exists(true));
            sort = new Sort(Sort.Direction.DESC, "resultPeriod." + periodID + ".federationPoints");
        } else {
            criteria = criteria.andOperator(Criteria.where("totalPeriod."+ tourFederationStatPeriodMgr.getStatCurrentPeriodID()).exists(true));
            sort = new Sort(Sort.Direction.DESC, "totalPeriod."+ tourFederationStatPeriodMgr.getStatCurrentPeriodID() +".federationPoints");
        }
        if (StringUtils.isNotBlank(countryCode)) {
            criteria = criteria.and("countryCode").is(countryCode);
        }

        Query query = new Query(criteria).with(sort).skip(offset).limit(nbMax);
        return (List<TourFederationStat>)getMongoTemplate().find(query, getTourFederationStatEntity());
    }

    /**
     * Count nb players with more federation points than player
     * @param periodID
     * @param statResult
     * @param countryCode
     * @return
     */
    public int countNbMoreFederationPoints(String periodID, TourFederationStatResult statResult, String countryCode, List<Long> selectionPlayerID) {
        Criteria criteria = new Criteria();
        if (StringUtils.isNotBlank(periodID)) {
            criteria = criteria.and("resultPeriod." + periodID).exists(true)
                    .and("resultPeriod." + periodID + ".federationPoints").gt(statResult.federationPoints);
        } else {
            criteria = criteria.and("totalPeriod." + tourFederationStatPeriodMgr.getStatCurrentPeriodID()).exists(true)
                    .and("totalPeriod." + tourFederationStatPeriodMgr.getStatCurrentPeriodID() + ".federationPoints").gt(statResult.federationPoints);
        }
        if (countryCode != null) {
            criteria = criteria.and("countryCode").is(countryCode);
        }
        if (selectionPlayerID != null && !selectionPlayerID.isEmpty()) {
            criteria = criteria.and("playerId").in(selectionPlayerID);
        }

        Query query = new Query(criteria);
        return (int)getMongoTemplate().count(query, getTourFederationStatEntity());
    }

    public boolean initStatPeriod(String statCurrentPeriodID, String statPreviousPeriodID) {
        synchronized (lockFinishTournament) {
            boolean processOK = true;
            MongoCollection collectionFederationStat = getMongoTemplate().getCollection(getTourFederationStatCollectionName());
            if (collectionFederationStat != null) {
                ListIndexesIterable<Document> indexes = collectionFederationStat.listIndexes();
                List<String> indexesToRemove = new ArrayList<>();
                boolean existIdxCurrentPeriod = false, existIdxPreviousPeriod = false, existIdxCurrentTotalPeriod = false;
                for (Document idx : indexes) {
                    String idxName = (String) idx.get("name");
                    if (idxName != null && (idxName.startsWith("idx_stat_") || idxName.startsWith("idx_stat_total_"))) {
                        if (idxName.equals("idx_stat_" + statCurrentPeriodID)) {
                            existIdxCurrentPeriod = true;
                        } else if (idxName.equals("idx_stat_" + statPreviousPeriodID)) {
                            existIdxPreviousPeriod = true;
                        } else if (idxName.equals("idx_stat_total_" + statCurrentPeriodID)) {
                            existIdxCurrentTotalPeriod = true;
                        } else {
                            indexesToRemove.add(idxName);
                        }
                    }
                }
                if (!existIdxCurrentPeriod) {
                    collectionFederationStat.createIndex(Indexes.descending("resultPeriod." + statCurrentPeriodID + ".federationPoints"), new IndexOptions().name("idx_stat_" + statCurrentPeriodID).background(true));
                }
                if (!existIdxPreviousPeriod) {
                    collectionFederationStat.createIndex(Indexes.descending("resultPeriod." + statPreviousPeriodID + ".federationPoints"), new IndexOptions().name("idx_stat_" + statPreviousPeriodID).background(true));
                }
                if (!existIdxCurrentTotalPeriod) {
                    collectionFederationStat.createIndex(Indexes.descending("totalPeriod." + statCurrentPeriodID + ".federationPoints"), new IndexOptions().name("idx_stat_total_" + statCurrentPeriodID).background(true));
                }
                if (indexesToRemove.size() > 0) {
                    for (String e : indexesToRemove) {
                        collectionFederationStat.dropIndex(e);
                    }
                }
            }

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, -1);
            String statPeriodIDToRemove = tourFederationStatPeriodMgr.sdfStatPeriod.format(calendar.getTime());

            // Funbridge points reduction
            List<TourFederationStat> statList = (List<TourFederationStat>) getMongoTemplate().findAll(getTourFederationStatEntity());
            for (TourFederationStat stat : statList) {
                synchronized (lockTourFederationStat.getLock("" + stat.playerID)) {
                    try {
                        TourFederationStatResult currentTotalPeriod = stat.totalPeriod.get(statCurrentPeriodID);
                        if (currentTotalPeriod == null) {
                            currentTotalPeriod = new TourFederationStatResult();
                            TourFederationStatResult previousTotalPeriod = stat.totalPeriod.get(statPreviousPeriodID);
                            if (previousTotalPeriod != null) {
                                currentTotalPeriod.funbridgePoints = (long) Math.ceil(previousTotalPeriod.funbridgePoints * 0.8);
                                currentTotalPeriod.federationPoints = previousTotalPeriod.federationPoints;
                                currentTotalPeriod.nbTournaments = previousTotalPeriod.nbTournaments;
                            }
                            stat.totalPeriod.put(statCurrentPeriodID, currentTotalPeriod);
                            stat.totalPeriod.remove(statPeriodIDToRemove);

                            getMongoTemplate().save(stat);
                        }
                    } catch (Exception e) {
                        log.error("Failed to create current totalPeriod - playerID=" + stat.playerID, e);
                        processOK = false;
                    }
                }
            }

            processOK = processOK & initStatPeriodSpecific(statCurrentPeriodID, statPreviousPeriodID);

            return processOK;
        }
    }

    public boolean initStatPeriodSpecific(String statCurrentPeriodID, String statPreviousPeriodID) {
        return true;
    }

    /**
     * Update tour federation stat for playerID.
     * If no stat exists for this player, a new one is created
     * @param playerID
     * @param funbridgePoints
     * @param federationPoints
     * @return
     */
    public TourFederationStat updateTourFederationStat(long playerID, int funbridgePoints, double federationPoints, long tourDate) {
        Calendar now = Calendar.getInstance();

        Calendar currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        currentMonth.set(Calendar.HOUR, 0);
        currentMonth.set(Calendar.MINUTE, 0);
        currentMonth.set(Calendar.SECOND, 0);
        currentMonth.set(Calendar.MILLISECOND, 0);

        Calendar previousMonth = (Calendar)currentMonth.clone();
        previousMonth.add(Calendar.MONTH, -1);

        synchronized (lockTourFederationStat.getLock(""+playerID)) {
            try {
                TourFederationStat tourFederationStat = getMongoTemplate().findById(playerID, getTourFederationStatEntity());
                if (tourFederationStat == null) {
                    // Create stat
                    tourFederationStat = buildStat();
                    tourFederationStat.playerID = playerID;
                    TourFederationPlayerPoints totalPlayerPoints = getFederationPlayerPoints(playerID, 0, 0);
                    initTourFederationStat(playerID, tourFederationStat, totalPlayerPoints, now.getTimeInMillis(), currentMonth.getTimeInMillis(), previousMonth.getTimeInMillis());
                    getMongoTemplate().insert(tourFederationStat);
                } else {
                    // Update stat
                    tourFederationStat.update(funbridgePoints, federationPoints, tourDate, tourFederationStatPeriodMgr.getStatCurrentPeriodID());
                    getMongoTemplate().save(tourFederationStat);
                }

                return tourFederationStat;
            } catch (Exception e) {
                log.error("Failed to update federation stat - playerID="+playerID+" - funbridgePoints="+funbridgePoints+" - federationPoints="+federationPoints+" - tourDate="+ tourDate, e);
            }
        }
        return null;
    }

    /**
     * Update tour funbridge points stat for playerID.
     * If no stat exists for this player, a new one is created
     * @param playerID
     * @param funbridgePoints
     * @param tourDate
     * @return
     */
    public TourFederationStat updateTourFunbridgePointsStat(long playerID, int funbridgePoints, long tourDate) {
        Calendar now = Calendar.getInstance();

        Calendar currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        currentMonth.set(Calendar.HOUR, 0);
        currentMonth.set(Calendar.MINUTE, 0);
        currentMonth.set(Calendar.SECOND, 0);
        currentMonth.set(Calendar.MILLISECOND, 0);

        Calendar previousMonth = (Calendar)currentMonth.clone();
        previousMonth.add(Calendar.MONTH, -1);

        synchronized (lockTourFunbridgePointsStat.getLock(""+playerID)) {
            try {
                TourFunbridgePointsStat tourFunbridgePointsStat = funbridgeMongoTemplate.findById(playerID, TourFunbridgePointsStat.class);
                if (tourFunbridgePointsStat == null) {
                    // Create stat
                    tourFunbridgePointsStat = new TourFunbridgePointsStat();
                    tourFunbridgePointsStat.playerID = playerID;

                    // Get total player points (for all federations)
                    TourFederationPlayerPoints totalPlayerPoints = new TourFederationPlayerPoints();
                    totalPlayerPoints.playerID = playerID;
                    for (TourFederationMgr tourFederationMgr : ContextManager.getListTourFederationMgr()) {
                        TourFederationPlayerPoints playerPoints = tourFederationMgr.getFederationPlayerPoints(playerID, 0, 0);
                        if (playerPoints != null) {
                            totalPlayerPoints.funbridgePoints += playerPoints.funbridgePoints;
                            totalPlayerPoints.nbTournaments += playerPoints.nbTournaments;
                        }
                    }

                    // Init TourFunbridgePointsStat
                    initTourFunbridgePointsStat(playerID, tourFunbridgePointsStat, totalPlayerPoints, now.getTimeInMillis(), currentMonth.getTimeInMillis(), previousMonth.getTimeInMillis());
                    funbridgeMongoTemplate.insert(tourFunbridgePointsStat);
                } else {
                    // Update stat
                    tourFunbridgePointsStat.update(funbridgePoints, tourDate, tourFederationStatPeriodMgr.getStatCurrentPeriodID());
                    funbridgeMongoTemplate.save(tourFunbridgePointsStat);
                }

                return tourFunbridgePointsStat;
            } catch (Exception e) {
                log.error("Failed to update funbridge points stat - playerID="+playerID+" - funbridgePoints="+funbridgePoints+" - tourDate="+ tourDate, e);
            }
        }
        return null;
    }

    /**
     * Update country in tour federation stat for playerID.
     * @param player
     * @return
     */
    public void updatePlayerFederationStatCountryCode(Player player) {
        if (player != null) {
            try {
                getMongoTemplate().updateFirst(Query.query(Criteria.where("_id").is(player.getID())),
                        Update.update("countryCode", player.getDisplayCountryCode()),
                        getTourFederationStatEntity());
            } catch (Exception e) {
                log.error("Failed to update country code for player="+player, e);
            }
        }
    }

    /**
     * Get funbridge points and federation points for a player and a period.
     * If start and end set to 0, return total.
     * @param playerID
     * @return
     */
    public TourFederationPlayerPoints getFederationPlayerPoints(long playerID, long start, long end) {
        Criteria criteria = new Criteria().where("playerID").is(playerID);
        if (start > 0 && end == 0) {
            criteria = criteria.and("startDate").gte(start);
        } else if (start > 0 && end > 0){
            criteria = criteria.andOperator(Criteria.where("startDate").gte(start), Criteria.where("startDate").lt(end));
        }
        MatchOperation match = Aggregation.match(criteria);

        GroupOperation group = Aggregation.group("playerID")
                .sum("funbridgePoints").as("funbridgePoints")
                .sum("points").as("points")
                .count().as("nbTournaments");

        Aggregation aggregation = Aggregation.newAggregation(match, group);
        AggregationResults<TourFederationPlayerPoints> result = getMongoTemplate().aggregate(aggregation, getTournamentPlayerCollectionName(), TourFederationPlayerPoints.class);

        return result.getUniqueMappedResult();
    }

    /**
     * List the tournament player sum of points for a period.
     * If start and end set to 0, return total.
     * @param start
     * @param end
     * @return
     */
    public List<TourFederationPlayerPoints> listFederationPlayerPoints(long start, long end) {
        Criteria criteria = new Criteria();
        if (start > 0 && end == 0) {
            criteria = criteria.and("startDate").gte(start);
        } else if (start > 0 && end > 0){
            criteria = criteria.andOperator(Criteria.where("startDate").gte(start), Criteria.where("startDate").lt(end));
        }
        MatchOperation match = Aggregation.match(criteria);

        GroupOperation group = Aggregation.group("playerID")
                .sum("funbridgePoints").as("funbridgePoints")
                .sum("points").as("points")
                .count().as("nbTournaments");

        Aggregation aggregation = Aggregation.newAggregation(match, group);
        AggregationResults<TourFederationPlayerPoints> result = getMongoTemplate().aggregate(aggregation, getTournamentPlayerCollectionName(), TourFederationPlayerPoints.class);

        return result.getMappedResults();
    }

    /**
     * Init tour federation stat for playerID.
     * If no stat exists for this player, a new one is created
     * @return
     */
    public void initTourFederationStat(long playerID, TourFederationStat tourFederationStat, TourFederationPlayerPoints totalPlayerPoints, long now, long currentMonth, long previousMonth) {
        try {
            // Total
            PlayerCache player = playerCacheMgr.getOrLoadPlayerCache(playerID);
            TourFederationTournament lastTour = findLastFinishedTournament();
            long lastTourDate = 0;
            if (lastTour != null) {
                lastTourDate = lastTour.getStartDate();
            }
            tourFederationStat.setData(totalPlayerPoints, lastTourDate, player.countryCode, tourFederationStatPeriodMgr.getStatCurrentPeriodID());

            // Current
            TourFederationPlayerPoints currentPlayerPoints = getFederationPlayerPoints(playerID, currentMonth, now);
            tourFederationStat.addOrUpdatePeriod(currentPlayerPoints, tourFederationStatPeriodMgr.getStatCurrentPeriodID());

            // Previous
            TourFederationPlayerPoints previousPlayerPoints = getFederationPlayerPoints(playerID, previousMonth, currentMonth);
            tourFederationStat.addOrUpdatePeriod(previousPlayerPoints, tourFederationStatPeriodMgr.getStatPreviousPeriodID());
        } catch (Exception e) {
            log.error("Failed to complete federation stat - playerID=" + totalPlayerPoints.playerID, e);
        }
    }

    /**
     * Init tour funbridge points stat for playerID.
     * If no stat exists for this player, a new one is created
     * @return
     */
    public void initTourFunbridgePointsStat(long playerID, TourFunbridgePointsStat tourFunbridgePointsStat, TourFederationPlayerPoints totalPlayerPoints, long now, long currentMonth, long previousMonth) {
        try {
            // Total
            PlayerCache player = playerCacheMgr.getOrLoadPlayerCache(playerID);
            TourFederationTournament lastTour = findLastFinishedTournament();
            long lastTourDate = 0;
            if (lastTour != null) {
                lastTourDate = lastTour.getStartDate();
            }
            tourFunbridgePointsStat.setData(totalPlayerPoints, lastTourDate, player.countryCode, tourFederationStatPeriodMgr.getStatCurrentPeriodID());

            // Current
            TourFederationPlayerPoints currentPlayerPoints = null;
            for (TourFederationMgr tourFederationMgr : ContextManager.getListTourFederationMgr()) {
                TourFederationPlayerPoints tempCurrentPlayerPoints = tourFederationMgr.getFederationPlayerPoints(playerID, currentMonth, now);
                if (currentPlayerPoints == null && tempCurrentPlayerPoints != null) {
                    currentPlayerPoints = tempCurrentPlayerPoints;
                    currentPlayerPoints.points = 0;
                } else if (tempCurrentPlayerPoints != null) {
                    currentPlayerPoints.funbridgePoints += tempCurrentPlayerPoints.funbridgePoints;
                    currentPlayerPoints.nbTournaments += tempCurrentPlayerPoints.nbTournaments;
                }
            }
            tourFunbridgePointsStat.addOrUpdatePeriod(currentPlayerPoints, tourFederationStatPeriodMgr.getStatCurrentPeriodID());

            // Previous
            TourFederationPlayerPoints previousPlayerPoints = null;
            for (TourFederationMgr tourFederationMgr : ContextManager.getListTourFederationMgr()) {
                TourFederationPlayerPoints tempPreviousPlayerPoints = tourFederationMgr.getFederationPlayerPoints(playerID, previousMonth, currentMonth);
                if (previousPlayerPoints == null && tempPreviousPlayerPoints != null) {
                    previousPlayerPoints = tempPreviousPlayerPoints;
                    previousPlayerPoints.points = 0;
                } else if (tempPreviousPlayerPoints != null) {
                    previousPlayerPoints.funbridgePoints += tempPreviousPlayerPoints.funbridgePoints;
                    previousPlayerPoints.nbTournaments += tempPreviousPlayerPoints.nbTournaments;
                }
            }
            tourFunbridgePointsStat.addOrUpdatePeriod(previousPlayerPoints, tourFederationStatPeriodMgr.getStatPreviousPeriodID());
        } catch (Exception e) {
            log.error("Failed to complete funbridge points stat - playerID=" + totalPlayerPoints.playerID, e);
        }
    }

    /**
     * Update tour federation stat for all players.
     * If no stat exists for a player, a new one is created
     * @return
     */
    public boolean createOrUpdateTourFederationStat() {
        Calendar now = Calendar.getInstance();

        Calendar currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        currentMonth.set(Calendar.HOUR, 0);
        currentMonth.set(Calendar.MINUTE, 0);
        currentMonth.set(Calendar.SECOND, 0);
        currentMonth.set(Calendar.MILLISECOND, 0);

        Calendar previousMonth = (Calendar)currentMonth.clone();
        previousMonth.add(Calendar.MONTH, -1);

        // For each player in a federation, add or update TourFederationStat
        List<TourFederationPlayerPoints> listTotalPlayerPoints = listFederationPlayerPoints(0, 0);
        for (TourFederationPlayerPoints totalPlayerPoints : listTotalPlayerPoints) {
            if (totalPlayerPoints != null) {
                long playerID = totalPlayerPoints.playerID;
                synchronized (lockTourFederationStat.getLock("" + playerID)) {
                    try {
                        TourFederationStat tourFederationStat = getMongoTemplate().findById(playerID, getTourFederationStatEntity());
                        if (tourFederationStat == null) {
                            // Create stat
                            tourFederationStat = buildStat();
                            tourFederationStat.playerID = playerID;
                        }
                        initTourFederationStat(playerID, tourFederationStat, totalPlayerPoints, now.getTimeInMillis(), currentMonth.getTimeInMillis(), previousMonth.getTimeInMillis());

                        getMongoTemplate().save(tourFederationStat);
                    } catch (Exception e) {
                        log.error("Failed to create or update federation stat - playerID=" + totalPlayerPoints.playerID, e);
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Count nb players with funbridge points for the period
     * @param periodID
     * @return
     */
    public int countRankingFunbridgePoints(String periodID, List<Long> listFollower, String countryCode) {
        Criteria criteria = new Criteria();
        if (listFollower != null && !listFollower.isEmpty()) {
            criteria = Criteria.where("_id").in(listFollower);
        }
        if (StringUtils.isNotBlank(periodID)) {
            criteria = criteria.and("resultPeriod." + periodID).exists(true);
        } else {
            criteria = criteria.and("totalPeriod." + tourFederationStatPeriodMgr.getStatCurrentPeriodID()).exists(true);
        }
        if (countryCode != null) {
            criteria = criteria.and("countryCode").is(countryCode);
        }
        Query query = new Query(criteria);
        return (int)funbridgeMongoTemplate.count(query, TourFunbridgePointsStat.class);
    }

    /**
     * Get funbridge points ranking for players with filter players, country, offset & limit
     * @param playerAsk
     * @param periodID
     * @param selectionPlayerID
     * @param countryCode
     * @param offset
     * @param nbMax
     * @return
     */
    public ResultServiceRest.GetMainRankingResponse getRankingFunbridgePoints(PlayerCache playerAsk, String periodID, List<Long> selectionPlayerID, String countryCode, int offset, int nbMax) {
        if (nbMax == 0) {
            nbMax = 50;
        }
        ResultServiceRest.GetMainRankingResponse response = new ResultServiceRest.GetMainRankingResponse();
        if (getConfigIntValue("disableMainRanking", 0) == 1) {
            return response;
        }
        response.totalSize = countRankingFunbridgePoints(periodID, selectionPlayerID, countryCode);
        response.nbRankedPlayers = countRankingFunbridgePoints(periodID, null, countryCode);
        response.ranking = new ArrayList<>();
        // rankingPlayer
        if (playerAsk != null) {
            WSMainRankingPlayer rankingPlayerFunbridgePoints = new WSMainRankingPlayer(playerAsk, true, playerAsk.ID);
            TourFunbridgePointsStat tourFunbridgePointsStat = funbridgeMongoTemplate.findById(playerAsk.ID, TourFunbridgePointsStat.class);
            if (tourFunbridgePointsStat != null) {
                TourFederationStatResult statResult = tourFunbridgePointsStat.getStatResult(periodID, tourFederationStatPeriodMgr.getStatCurrentPeriodID());
                if (statResult != null) {
                    rankingPlayerFunbridgePoints.value = statResult.funbridgePoints;
                    rankingPlayerFunbridgePoints.rank = countNbMoreFunbridgePoints(periodID, statResult, countryCode, null) + 1;
                    if (offset == -1) {
                        int playerOffset = countNbMoreFunbridgePoints(periodID, statResult, countryCode, selectionPlayerID) + 1;
                        offset = playerOffset - (nbMax / 2);
                    }
                }
            }
            rankingPlayerFunbridgePoints.rank = (rankingPlayerFunbridgePoints.rank == 0)?-1:rankingPlayerFunbridgePoints.rank;
            response.rankingPlayer = rankingPlayerFunbridgePoints;
        }
        if (offset < 0) {
            offset = 0;
        }

        response.ranking = getFunbridgePointRankingPlayers(playerAsk, periodID, selectionPlayerID, countryCode, offset, nbMax);
        response.offset = offset;
        return response;
    }

    /**
     * Return ranking player of funrbidge Point federation
     */
    private List<WSMainRankingPlayer> getFunbridgePointRankingPlayers(PlayerCache playerAsk, String periodID, List<Long> selectionPlayerID, String countryCode, int offset, int nbMax) {
        // list stat
        List<TourFunbridgePointsStat> listStat = this.listTourFunbridgePointsStat(periodID, offset, nbMax, selectionPlayerID, countryCode);

        int currentRank = -1, nbWithSameFunbridgePoints = 0;
        double currentNbFunbridgePoints = -1;
        List<WSMainRankingPlayer> rankinPlayers = new ArrayList<>();

        for (TourFunbridgePointsStat stat : listStat) {
            long playerAskId = playerAsk != null ? playerAsk.ID : 0;
            WSMainRankingPlayer data = new WSMainRankingPlayer(playerCacheMgr.getPlayerCache(stat.playerID), presenceMgr.isSessionForPlayerID(stat.playerID), playerAskId);
            TourFederationStatResult statResult = stat.getStatResult(periodID, tourFederationStatPeriodMgr.getStatCurrentPeriodID());
            if (statResult != null) {
                // init current counter
                if (currentRank == -1) {
                    currentRank = countNbMoreFunbridgePoints(periodID, statResult, countryCode, null) + 1;
                    nbWithSameFunbridgePoints = (offset + 1) - currentRank + 1;
                } else {
                    if (statResult.funbridgePoints == currentNbFunbridgePoints) {
                        nbWithSameFunbridgePoints++;
                    } else {
                        if (selectionPlayerID != null && selectionPlayerID.size() > 0) {
                            currentRank = countNbMoreFunbridgePoints(periodID, statResult, countryCode, null) + 1;
                        } else {
                            currentRank = currentRank + nbWithSameFunbridgePoints;
                        }
                        nbWithSameFunbridgePoints = 1;
                    }
                }
                currentNbFunbridgePoints = statResult.funbridgePoints;

                data.value = statResult.funbridgePoints;
                data.pfValue = statResult.funbridgePoints;
                data.nbTournmanents = statResult.nbTournaments;
                data.rank = currentRank;
            }
            rankinPlayers.add(data);
        }
        return rankinPlayers;
    }


    /**
     * Count nb players with more funbridge points than player
     * @param periodID
     * @param statResult
     * @param countryCode
     * @return
     */
    private int countNbMoreFunbridgePoints(String periodID, TourFederationStatResult statResult, String countryCode, List<Long> selectionPlayerID) {
        Criteria criteria = new Criteria();
        if (StringUtils.isNotBlank(periodID)) {
            criteria = criteria.and("resultPeriod." + periodID).exists(true)
                    .and("resultPeriod." + periodID + ".funbridgePoints").gt(statResult.funbridgePoints);
        } else {
            criteria = criteria.and("totalPeriod." + tourFederationStatPeriodMgr.getStatCurrentPeriodID()).exists(true)
                    .and("totalPeriod." + tourFederationStatPeriodMgr.getStatCurrentPeriodID() + ".funbridgePoints").gt(statResult.funbridgePoints);
        }
        if (countryCode != null) {
            criteria = criteria.and("countryCode").is(countryCode);
        }
        if (selectionPlayerID != null && !selectionPlayerID.isEmpty()) {
            criteria = criteria.and("playerId").in(selectionPlayerID);
        }

        Query query = new Query(criteria);
        return (int)funbridgeMongoTemplate.count(query, TourFunbridgePointsStat.class);
    }

    /**
     * List tour funbridge points stat for a period with offset and nbMax.
     * If period null, return stat total results.
     * @param resultPeriod
     * @param offset
     * @param nbMax
     * @return
     */
    private List<TourFunbridgePointsStat> listTourFunbridgePointsStat(String resultPeriod, int offset, int nbMax, List<Long> listFollower, String countryCode) {
        Criteria criteria = new Criteria();
        if (listFollower != null && !listFollower.isEmpty()) {
            criteria = Criteria.where("_id").in(listFollower);
        }

        Sort sort;
        if (StringUtils.isNotBlank(resultPeriod)) {
            criteria = criteria.andOperator(Criteria.where("resultPeriod."+resultPeriod).exists(true));
            sort = new Sort(Sort.Direction.DESC, "resultPeriod." + resultPeriod + ".funbridgePoints");
        } else {
            criteria = criteria.andOperator(Criteria.where("totalPeriod."+ tourFederationStatPeriodMgr.getStatCurrentPeriodID()).exists(true));
            sort = new Sort(Sort.Direction.DESC, "totalPeriod."+ tourFederationStatPeriodMgr.getStatCurrentPeriodID() +".funbridgePoints");
        }
        if (StringUtils.isNotBlank(countryCode)) {
            criteria = criteria.and("countryCode").is(countryCode);
        }

        Query query = new Query(criteria).with(sort).skip(offset).limit(nbMax);

        if (StringUtils.isNotBlank(resultPeriod)) {
            query.fields().include("resultPeriod." + resultPeriod + ".funbridgePoints");
        } else {
            query.fields().include("totalPeriod."+ tourFederationStatPeriodMgr.getStatCurrentPeriodID() +".funbridgePoints");
        }
        return funbridgeMongoTemplate.find(query, TourFunbridgePointsStat.class);
    }

    public String getApplicationName(String lang){
        return textUIMgr.getTextUIForLang("federation.applicationName." + getFederationName(), lang);
    }

    /**
     * return tournament number during last month
     * @return
     */
    public long getNbTounamentsByPeriod(String periodId){
        int month = Integer.parseInt(periodId.substring(4)) - 1;
        Query query = new Query();
        Calendar startDate = Calendar.getInstance();
        DateUtils.initAtStartOfDay(startDate);

        startDate.set(Calendar.MONTH, month);
        startDate.set(Calendar.DATE,startDate.getActualMinimum(Calendar.DAY_OF_MONTH));

        Calendar endDate = Calendar.getInstance();
        endDate.set(Calendar.MONTH, month);
        DateUtils.initAtStartOfDay(endDate);
        endDate.set(Calendar.DATE,startDate.getActualMaximum(Calendar.DAY_OF_MONTH));
        endDate.add(Calendar.DATE, 1);

        query.addCriteria(Criteria.where("startDate").gte(startDate.getTimeInMillis()).lte(endDate.getTimeInMillis()));

        return getMongoTemplate().count(query, getTournamentEntity());
    }

    public void insertPlayerFederation(final PlayerFederation federation) {
        this.getMongoTemplate().insert(federation, this.getPlayerFederationCollectionName());
    }

    public void savePlayerFederation(final PlayerFederation federation) {
        this.getMongoTemplate().save(federation, this.getPlayerFederationCollectionName());
    }


    public void savePlayerFederation(PlayerFederation playerFederation, String federation) throws FBWSException {
        final TourFederationMgr tourMgr = getTourFederationMgr(federation);
        if(tourMgr != null){
            if(playerFederation.playerId == 0){
                tourMgr.insertPlayerFederation(playerFederation);
            } else {
                tourMgr.savePlayerFederation(playerFederation);
            }
        } else {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID, MessageFormat.format("Cannot save player federation {0}", federation));
        }
    }

    public PlayerFederation getPlayerFederation(long playerId, String federation) throws FBWSException {
        final TourFederationMgr tourMgr = getTourFederationMgr(federation);
        return tourMgr.getPlayerFederationwithTouchID(playerId);

    }

    public abstract PlayerFederation getPlayerFederationEntity();

    public PlayerFederation createPlayerFederation(final long playerID, final String federation) throws FBWSException {
        final PlayerFederation pf = FederationMgr.getTourFederationMgr(federation).getPlayerFederationEntity();
        pf.playerTouchId = playerID;
        pf.playerId = playerID;
        this.getMongoTemplate().insert(pf, this.getPlayerFederationCollectionName());
        return pf;
    }


    public PlayerFederation getOrCreatePlayerFederation(final long playerID,  String federation) throws FBWSException {
        // Get PlayerFederation
        PlayerFederation playerFederation = getPlayerFederation(playerID, federation);
        // If no PlayerFederation is found, create a new one
        if(playerFederation == null){
            playerFederation = this.createPlayerFederation(playerID, federation);
        }
        // Check if playerFederation has to be updated with a new touchId
        if(playerFederation != null && playerID != 0 && playerFederation.playerTouchId == 0){
            playerFederation.playerTouchId = playerID;
            playerFederation.playerId = playerID;
            this.savePlayerFederation(playerFederation, federation);
        }
        return playerFederation;
    }

    /**
     * Get a federation tournament manager by its name
     * @param federation
     * @return
     * @throws FBWSException
     */
    public static TourFederationMgr getTourFederationMgr(String federation) throws FBWSException {
        if(federation.equalsIgnoreCase(Constantes.TOURNAMENT_CBO)) {
            return ContextManager.getTourCBOMgr();
        } else {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
    }

    public PlayerFederation refundFederationCredit(final PlayerFederation playerFederation, final String federation, final int nbBonusCredits, final int nbBoughtCredits) throws FBWSException {
        if(playerFederation == null) {
            throw new FBWSException(FBExceptionType.PLAYER_NOT_EXISTING);
        }
        synchronized (getLockUpdatePlayer(playerFederation.playerTouchId)) {
            // Check if the number of credits refunded isn't superior to the number of credits played. If not, refund
            if (nbBonusCredits > playerFederation.nbBonusCreditsPlayed) {
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            } else {
                playerFederation.nbBonusCreditsPlayed -= nbBonusCredits;
            }
            if (nbBoughtCredits > playerFederation.nbBoughtCreditsPlayed) {
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            } else {
                playerFederation.nbBoughtCreditsPlayed -= nbBoughtCredits;
            }
            // Update PlayerFederation
            savePlayerFederation(playerFederation, federation);
            return playerFederation;
        }
    }
}
