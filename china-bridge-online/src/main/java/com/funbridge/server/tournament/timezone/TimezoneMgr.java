package com.funbridge.server.tournament.timezone;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.tournament.DealGenerator;
import com.funbridge.server.tournament.game.*;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.generic.memory.GenericMemDeal;
import com.funbridge.server.tournament.generic.memory.GenericMemDealPlayer;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer;
import com.funbridge.server.tournament.timezone.data.TimezoneGame;
import com.funbridge.server.tournament.timezone.data.TimezoneTournament;
import com.funbridge.server.tournament.timezone.data.TimezoneTournamentPlayer;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.result.WSResultArchive;
import com.funbridge.server.ws.tournament.WSTableTournament;
import com.funbridge.server.ws.tournament.WSTournament;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.lock.LockWeakString;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by pserent on 15/04/2015.
 */
@Component(value="timezoneMgr")
@Scope(value="singleton")
public class TimezoneMgr extends TournamentGenericMgr {

    @Resource(name = "mongoTimezoneTemplate")
    private MongoTemplate mongoTemplate;

    @Resource(name="messageNotifMgr")
    protected MessageNotifMgr notifMgr = null;

    private TimezoneGeneratorTask generatorTask = new TimezoneGeneratorTask();
    private TimezoneFinishTask finishTask = new TimezoneFinishTask();
    private ScheduledExecutorService schedulerGenerator = Executors.newScheduledThreadPool(1);
    private ScheduledExecutorService schedulerFinish = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> schedulerGeneratorFuture = null, schedulerFinishFuture = null;
    private LockWeakString lockCreateGame = new LockWeakString();
    private LockWeakString lockTournament = new LockWeakString();

    public static final int INDEX_ARRAY_TYPE_IMP = 0;
    public static final int INDEX_ARRAY_TYPE_PAIRE = 1;

    /**
     * Task to generate tournament timezone
     * @author pserent
     *
     */
    private class TimezoneGeneratorTask implements Runnable {
        boolean running = false;
        @Override
        public void run() {
            if (!running) {
                running = true;
                try {
                    if (getConfigIntValue("taskGeneratorEnable", 1) == 1) {
                        // check and create if ncessary tournament for tomorrow
                        checkAndCreateTournaments(false);
                    } else {
                        log.error("Task generator is not enable");
                    }
                } catch (Exception e) {
                    log.error("Exception to execute checkAndCreateTournaments", e);
                    ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to checkAndCreateTournaments", e.getMessage(), null);
                }
                running = false;
            } else {
                log.error("Task already running ...");
                ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for checkAndCreateTournaments", null, null);
            }
        }
    }

    /**
     * Task to finish tournament timezone
     * @author pserent
     *
     */
    private class TimezoneFinishTask implements Runnable {
        boolean running = false;
        @Override
        public void run() {
            if (!running) {
                running = true;
                try {
                    if (getConfigIntValue("taskFinishEnable", 1) == 1) {
                        finishExpiredTournament();
                    } else {
                        log.error("Task finish is not enable");
                    }
                } catch (Exception e) {
                    log.error("Exception to execute finishExpiredTournament", e);
                }
                running = false;
            } else {
                log.error("Task already running ...");
            }
        }
    }

    public TimezoneMgr() {
        super(Constantes.TOURNAMENT_CATEGORY_TIMEZONE);
    }

    @Override
    @PostConstruct
    public void init() {
        super.init(GenericMemTournament.class, GenericMemTournamentPlayer.class, GenericMemDeal.class, GenericMemDealPlayer.class);
    }

    @Override
    @PreDestroy
    public void destroy() {
        stopScheduler(schedulerGenerator);
        stopScheduler(schedulerFinish);
        super.destroy();
    }

    @Override
    public void startUp() {
        log.info("startUp");
        // check for current time
        checkAndCreateTournaments(true);

        // load tournament not finished
        List<TimezoneTournament> listTour = listTournamentNotFinished();
        if (listTour != null && listTour.size() > 0) {
            log.info("Nb tournament TIMEZONE to load : "+listTour.size());
            int nbLoadFromFile = 0, nbLoadFromDB = 0;
            // loop on each tournament
            for (TimezoneTournament tour : listTour) {
                boolean loadFromDB = false;
                if (getConfigIntValue("backupMemoryLoad", 1) == 1) {
                    // try to load from json file
                    String filePath = buildBackupFilePathForTournament(tour.getIDStr(), false);
                    if (filePath != null) {
                        if (memoryMgr.loadMemTourFromFile(filePath) != null) {
                            nbLoadFromFile++;
                        } else {
                            loadFromDB = true;
                        }
                    } else {
                        loadFromDB = true;
                    }
                } else {
                    loadFromDB = true;
                }
                // load from DB
                if (loadFromDB) {
                    GenericMemTournament memTour = memoryMgr.addTournament(tour);
                    if (memTour != null) {
                        List<TimezoneGame> listGame = listGameOnTournament(tour.getIDStr());
                        if (listGame != null) {
                            // load each game in memory
                            for (TimezoneGame tourGame : listGame) {
                                if (tourGame.isFinished()) {
                                    memTour.addResult(tourGame, false, false);
                                } else {
                                    memTour.addPlayer(tourGame.getPlayerID(), tourGame.getStartDate());
                                }
                            }
                            // compute result & ranking
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
            log.info("No tournament TIMEZONE to load");
        }

        // start task for automatic generator tournament
        try {
            Calendar dateNext = Constantes.getNextDateForPeriod(Calendar.getInstance(), 10);
            schedulerGeneratorFuture = schedulerGenerator.scheduleAtFixedRate(generatorTask, dateNext.getTimeInMillis() - System.currentTimeMillis(), (long)30*60*1000, TimeUnit.MILLISECONDS);
            log.info("Schedule generator task - next run at "+Constantes.getStringDateForNextDelayScheduler(schedulerGeneratorFuture)+" - period (minutes)=30");
            generatorTask.run();
        } catch (Exception e) {
            log.error("Exception to start generator task", e);
        }

        // start task for finish expired tournament
        try {
            int finishPeriod = getConfigIntValue("finishPeriodSeconds", 300);
            int initDelay = getConfigIntValue("finishInitDelaySeconds", 60);
            schedulerFinishFuture = schedulerFinish.scheduleWithFixedDelay(finishTask, initDelay, finishPeriod, TimeUnit.SECONDS);
            log.info("Schedule finish task - next run at " + Constantes.getStringDateForNextDelayScheduler(schedulerFinishFuture) + " - period (seconds)=" + finishPeriod);
        } catch (Exception e) {
            log.error("Exception to start finish task", e);
        }
    }

    @Override
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    @Override
    public Class<? extends Game> getGameEntity() {
        return TimezoneGame.class;
    }

    @Override
    public String getGameCollectionName() {
        return "timezone_game";
    }

    @Override
    public Class<? extends Tournament> getTournamentEntity() {
        return TimezoneTournament.class;
    }

    @Override
    public Class<? extends TournamentPlayer> getTournamentPlayerEntity() {
        return TimezoneTournamentPlayer.class;
    }

    @Override
    public void checkTournamentMode(boolean isReplay, long playerId) throws FBWSException {}

    /**
     * Return the date of the next generator task run.
     * @return
     */
    public String getStringDateNextGeneratorScheduler() {
        return Constantes.getStringDateForNextDelayScheduler(schedulerGeneratorFuture);
    }

    /**
     * Return the date of the next finish task run.
     * @return
     */
    public String getStringDateNextFinishScheduler() {
        return Constantes.getStringDateForNextDelayScheduler(schedulerFinishFuture);
    }

    public Object getLockOnTournament(String tourID) {
        return lockTournament.getLock(tourID);
    }

    /**
     * Check if tournament for each timezone exist and create missing tournament
     * @param isStartUp
     * @return
     */
    public int checkAndCreateTournaments(boolean isStartUp) {
        Calendar date = Calendar.getInstance();
        if (!isStartUp) {
            date.add(Calendar.DAY_OF_YEAR, 1);
        }
        List<TimezoneTournament> listTour = listTournament(date.getTimeInMillis());
        boolean[] tourExist = new boolean[2];
        tourExist[INDEX_ARRAY_TYPE_PAIRE] = false;
        tourExist[INDEX_ARRAY_TYPE_IMP] = false;

        // check existing tournament and set flag in array
        for (TimezoneTournament tour : listTour) {
            if (tour.getResultType() == Constantes.TOURNAMENT_RESULT_PAIRE) {
                tourExist[INDEX_ARRAY_TYPE_PAIRE] = true;
            } else if (tour.getResultType() == Constantes.TOURNAMENT_RESULT_IMP) {
                tourExist[INDEX_ARRAY_TYPE_IMP] = true;
            }
        }
        int nbTourCreate = 0;

        // Create missing tournament
        if (!tourExist[INDEX_ARRAY_TYPE_PAIRE]) {
            if (generateTournament(Constantes.TOURNAMENT_RESULT_PAIRE, date.getTimeInMillis()) != null) {
                nbTourCreate++;
            }
        }
        if (!tourExist[INDEX_ARRAY_TYPE_IMP]) {
            if (generateTournament(Constantes.TOURNAMENT_RESULT_IMP,date.getTimeInMillis()) != null) {
                nbTourCreate++;
            }
        }

        return nbTourCreate;
    }

    /**
     * List tournament where beginDate < date < endDate
     * @param date
     * @return
     */
    public List<TimezoneTournament> listTournament(long date) {
        return mongoTemplate.find(Query.query(Criteria.where("startDate").lt(date).and("endDate").gt(date)), TimezoneTournament.class);
    }

    /**
     * List tournament where status isFinished is false
     * @return
     */
    public List<TimezoneTournament> listTournamentNotFinished() {
        return mongoTemplate.find(Query.query(Criteria.where("finished").is(false)), TimezoneTournament.class);
    }

    /**
     * Tournament with finished flag false and endDate < current date
     * @return
     */
    public List<TimezoneTournament> listTournamentNotFinishedAndExpired() {
        return mongoTemplate.find(Query.query(Criteria.where("finished").is(false).andOperator(Criteria.where("endDate").lt(System.currentTimeMillis()))), TimezoneTournament.class);
    }

    /**
     * List all expired tournament and finish them.
     */
    public void finishExpiredTournament() {
        List<TimezoneTournament> listTour = listTournamentNotFinishedAndExpired();
        if (listTour != null && listTour.size() > 0) {
            for (TimezoneTournament tour : listTour) {
                try {
                    finishTournament(tour);
                } catch (Exception e) {
                    log.error("Exception to finish tournament="+tour);
                }
            }
        }
    }

    /**
     * List game on a tournament
     * @param tourID
     * @return
     */
    public List<TimezoneGame> listGameOnTournament(String tourID){
        try {
            Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)));
            return mongoTemplate.find(q, TimezoneGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament="+tourID, e);
        }
        return null;
    }

    /**
     * Generate tournament for timezone and result type
     * @param resultType
     * @param dateRef
     * @return
     */
    public TimezoneTournament generateTournament(int resultType, long dateRef) {
        int nbDeal = getConfigIntValue("nbDeal", 20);
        try {
            // generate distribution
            DealGenerator.DealGenerate[] tabDeal = dealGenerator.generateDeal(nbDeal, 0);
            if (tabDeal != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(dateRef);
                int engineVersion = ContextManager.getArgineEngineMgr().getEngineVersion(Constantes.TOURNAMENT_CATEGORY_TIMEZONE);
                if (engineVersion > 0) {
                    int beginHour = getConfigIntValue( "beginHour", -1);
                    if (beginHour >= 0) {
                        if (cal.get(Calendar.HOUR_OF_DAY) < beginHour) {
                            cal.add(Calendar.DAY_OF_YEAR, -1);
                        }
                        cal.set(Calendar.HOUR_OF_DAY, beginHour);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        long beginDate = cal.getTimeInMillis();
                        TimezoneTournament tour = new TimezoneTournament();
                        tour.setStartDate(beginDate);
                        // by default duration is one day (24h)
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                        tour.setEndDate(cal.getTimeInMillis());
                        tour.setResultType(resultType);
                        tour.setDealGenerate(tabDeal);
                        tour.setNbCreditsPlayDeal(getConfigIntValue("nbCreditPlayDeal", 1));
                        tour.setCreationDateISO(new Date());
                        // persist date
                        mongoTemplate.insert(tour);
                        // add tour in memory
                        memoryMgr.addTournament(tour);
                        return tour;
                    } else {
                        log.error("No beginHour define for timezone");
                    }
                } else {
                    log.error("Engine version not valid ! - engineVersion="+engineVersion);
                }
            } else {
                log.error("Generate deal failed ... resultType="+resultType);
            }
        } catch (Exception e) {
            log.error("Exception to generate tournament - resultType="+resultType, e);
        }
        return null;
    }

    /**
     * Return the tournament associated with this ID
     * @param tourID
     * @return
     */
    public TimezoneTournament getTournament(String tourID) {
        return mongoTemplate.findById(new ObjectId(tourID), TimezoneTournament.class);
    }

    /**
     * Retrieve tournament wich include this deal
     * @param dealID
     * @return
     */
    public TimezoneTournament getTournamentWithDeal(String dealID) {
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
    public TimezoneTournamentPlayer getTournamentPlayer(String tourID, long playerID) {
        return mongoTemplate.findOne(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("playerID").is(playerID))), TimezoneTournamentPlayer.class);
    }

    /**
     * Return the game with this ID
     * @param gameID
     * @return
     */
    @Override
    public TimezoneGame getGame(String gameID) {
        return mongoTemplate.findById(new ObjectId(gameID), TimezoneGame.class);
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
        TimezoneTournament tour = null;
        GenericMemTournament memTour = null;

        //***********************
        // Get Tournament -> check table from session
        if (session.getCurrentGameTable() != null &&
                !session.getCurrentGameTable().isReplay() &&
                session.getCurrentGameTable().getTournament() != null &&
                session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TIMEZONE &&
                session.getCurrentGameTable().getTournament().getIDStr().equals(tournamentID)) {
            table = session.getCurrentGameTable();
            tour = (TimezoneTournament)table.getTournament();
            if (tour == null) {
                log.error("No tournament found on table ... tourID="+tournamentID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        else {
            tour = getTournament(tournamentID);
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
        memTour = memoryMgr.getTournament(tour.getIDStr());
        if (memTour == null) {
            log.error("No tournament found in memory ... tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
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
            TimezoneGame game = getGameNotFinishedOnTournamentForPlayer(tour.getIDStr(), session.getPlayer().getID());
            if (game != null) {
                table.setGame(game);
            }
            // if start playing this tournament => check device not yet played this tournament
            if (game == null) {
                if (existGameOnTournamentForDevice(tour.getIDStr(), session.getDeviceID())) {
                    log.error("Tournament already played on this device by another player ! tourID="+tour.getIDStr()+" - player="+session.getPlayer().getID()+" - deviceID="+session.getDeviceID());
                    throw new FBWSException(FBExceptionType.TOURNAMENT_ALREADY_PLAYED_DEVICE);
                }
            }
        }
        session.setCurrentGameTable(table);

        //*******************
        // retrieve game
        TimezoneGame game = null;
        if (table.getGame() != null &&
                !table.getGame().isReplay() &&
                !table.getGame().isFinished() &&
                table.getGame().getTournament().getIDStr().equals(tournamentID)) {
            game = (TimezoneGame)table.getGame();
        } else {
            // need to retrieve an existing game
            int lastIndexPlayed = 0;
            List<TimezoneGame> listGame = listGameOnTournamentForPlayer(tour.getIDStr(), session.getPlayer().getID());
            if (listGame != null) {
                for (TimezoneGame g : listGame) {
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
                playerMgr.checkPlayerCredit(session.getPlayer(), tour.getNbCreditsPlayDeal());
                // need to create a new game
                if (lastIndexPlayed >= tour.getNbDeals()) {
                    log.error("lastIndexPlayed to big to play another deal ! player="+session.getPlayer()+" - lastIndexPlayed="+lastIndexPlayed+" - tour="+tour);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                // create game
                game = createGame(session.getPlayer().getID(), tour, lastIndexPlayed+1, conventionProfil, conventionValue, cardsConventionProfil, cardsConventionValue, session.getDeviceID());

                if (game != null) {
                    ContextManager.getPlayerMgr().updatePlayerCreditDeal(session.getPlayer(), tour.getNbCreditsPlayDeal(), 1);
                } else {
                    log.error("Failed to create game - player="+session.getPlayer().getID()+" - tour="+tour);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_TIMEZONE, 1);
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

        // add player on tournament memory
        synchronized (getLockOnTournament(tour.getIDStr())) {
            GenericMemTournamentPlayer memTourPlayer = memTour.getOrCreateTournamentPlayer(session.getPlayer().getID());
            memTourPlayer.currentDealIndex = game.getDealIndex();
        }

        //*********************
        // Build data to return
        WSTableTournament tableTour = new WSTableTournament();
        tableTour.tournament = toWSTournament(tour, session.getPlayerCache());
        tableTour.tournament.currentDealIndex = game.getDealIndex();
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
     * Transform tournament for webservice object
     * @param tour
     * @param playerCache
     * @return
     */
    public WSTournament toWSTournament(TimezoneTournament tour, PlayerCache playerCache) {
        if (tour != null) {
            WSTournament wst = tour.toWS();
            GenericMemTournament memTournament = memoryMgr.getTournament(tour.getIDStr());
            if (memTournament != null) {
                wst.nbTotalPlayer = memTournament.getNbPlayer();
                wst.resultPlayer = memTournament.getWSResultPlayer(playerCache, true);
                if (wst.resultPlayer != null) {
                    wst.nbTotalPlayer = memTournament.getNbPlayerFinishAll();
                }
                wst.currentDealIndex = memTournament.getCurrentDealForPlayer(playerCache.ID);
                wst.remainingTime = tour.getEndDate() - System.currentTimeMillis();
            } else {
                wst.nbTotalPlayer = tour.getNbPlayers();
                TimezoneTournamentPlayer tourPlayer = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
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
     * List game for a player on a tournament (order by dealIndex asc)
     * @param tourID
     * @param playerID
     * @return
     */
    public List<TimezoneGame> listGameOnTournamentForPlayer(String tourID, long playerID) {
        Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("playerID").is(playerID)));
//        q.with(new Sort(Sort.Direction.ASC, "dealIndex"));
        List<TimezoneGame> result = mongoTemplate.find(q, TimezoneGame.class);
        if (result != null) {
            Collections.sort(result, new Comparator<TimezoneGame>() {
                @Override
                public int compare(TimezoneGame o1, TimezoneGame o2) {
                    return Integer.compare(o1.getDealIndex(), o2.getDealIndex());
                }
            });
        }
        return result;
    }

    /**
     * Insert tourPlay to DB
     * @param listTourPlay
     * @throws FBWSException
     */
    public void insertListTourPlayDB(List<TimezoneTournamentPlayer> listTourPlay) throws FBWSException {
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
    private TimezoneGame createGame(long playerID, TimezoneTournament tour, int dealIndex, int conventionProfil, String conventionData, int cardsConventionProfil, String cardsConventionValue, long deviceID) throws FBWSException {
        synchronized (lockCreateGame.getLock(tour.getIDStr()+"-"+playerID)) {
            if (getGameOnTournamentAndDealForPlayer(tour.getIDStr(), dealIndex, playerID) != null) {
                log.error("A game already exist on tour="+tour+" - dealIndex="+dealIndex+" - playerID="+playerID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            try {
                TimezoneGame game = new TimezoneGame(playerID, tour, dealIndex);
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

    @Override
    public void updateTournamentDB(Tournament tour) throws FBWSException{
        if (tour != null && tour instanceof TimezoneTournament) {
            TimezoneTournament tourTZ = (TimezoneTournament)tour;
            try {
                mongoTemplate.save(tourTZ);
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
        if (session.getCurrentGameTable() == null || session.getCurrentGameTable().getGame() == null || !(session.getCurrentGameTable().getGame() instanceof TimezoneGame)) {
            log.error("Table or game in session not valid ! - table="+session.getCurrentGameTable());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TimezoneGame game = (TimezoneGame)session.getCurrentGameTable().getGame();
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
     * Return list of tournament in progress for each timezone
     * @param playerCache
     * @return
     */
    public WSTournament[] getTournamentsForPlayer(PlayerCache playerCache) {
        WSTournament[] result = new WSTournament[2];
        List<TimezoneTournament> listTour = listTournament(System.currentTimeMillis());
        if (listTour != null) {
            for (TimezoneTournament t : listTour) {
                if (t.getResultType() == Constantes.TOURNAMENT_RESULT_IMP) {
                    WSTournament wst = toWSTournament(t, playerCache);
                    result[INDEX_ARRAY_TYPE_IMP] = wst;
                }
                else if (t.getResultType() == Constantes.TOURNAMENT_RESULT_PAIRE) {
                    WSTournament wst = toWSTournament(t, playerCache);
                    result[INDEX_ARRAY_TYPE_PAIRE] = wst;
                }
            }
        }
        return result;
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
        if (table.getTournament() == null || !(table.getTournament() instanceof TimezoneTournament)) {
            log.error("Touranment on table is not TIMEZONE table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!(table.getGame() instanceof TimezoneGame)) {
            log.error("Game on table is not TIMEZONE table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TimezoneGame game = (TimezoneGame)table.getGame();
        if (!table.getTournament().getIDStr().equals(tournamentID)) {
            log.error("TournamentID  of current game ("+table.getTournament().getIDStr()+") is not same as tournamentID="+tournamentID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        GenericMemTournament memTour = memoryMgr.getTournament(tournamentID);
        if (memTour == null) {
            log.error("No GenericMemTournament found for tournamentID="+tournamentID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        checkGame(game);
        Player p = session.getPlayer();
        TimezoneTournament tour = game.getTournament();
        // check if player has enough credit to leave tournament
        int nbDealToLeave = tour.getNbDeals() - table.getNbPlayedDeals() - 1; // -1 because credit already decrement for current game !
        if (nbDealToLeave > 0) {
            playerMgr.checkPlayerCredit(p, tour.getNbCreditsPlayDeal() * nbDealToLeave);
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
            memoryMgr.updateResult(game);
        }
        // leave game for other deals
        int nbDealNotPlayed = 0;
        for (int i= 1; i<= tour.getNbDeals(); i++) {
            if (getGameOnTournamentAndDealForPlayer(tournamentID, i, p.getID()) == null) {
                TimezoneGame g = new TimezoneGame(p.getID(), tour, i);
                g.setStartDate(System.currentTimeMillis());
                g.setLastDate(System.currentTimeMillis());
                g.setLeaveValue();
                g.setDeviceID(session.getDeviceID());
                mongoTemplate.insert(g);
                memoryMgr.updateResult(g);
                table.addPlayedDeal(g.getDealID());
                nbDealNotPlayed++;
            }
        }

        // remove game from session
        session.removeGame();
        // remove player from set
        gameMgr.removePlayerRunning(p.getID());

        // update player data
        if (nbDealNotPlayed > 0) {
            playerMgr.updatePlayerCreditDeal(p, tour.getNbCreditsPlayDeal()*nbDealNotPlayed, nbDealNotPlayed);
            session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_TIMEZONE, nbDealNotPlayed);
        }

        // return credit remaining of player
        return p.getTotalCreditAmount();
    }

    /**
     * Finish the tournament associated to this memory data
     *
     * @param tour
     */
    public boolean finishTournament(Tournament tour) {
        long tsBegin = System.currentTimeMillis();
        if (tour != null && !tour.isFinished()) {
            if (tour instanceof TimezoneTournament) {
                GenericMemTournament memTour = memoryMgr.getTournament(tour.getIDStr());
                if (memTour != null) {
                    if (!memTour.closeInProgress) {
                        memTour.closeInProgress = true;
                        // list with all player on tournament
                        Set<Long> listPlayer = memTour.tourPlayer.keySet();
                        try {
                            //******************************************
                            // loop on all deals to update rank & result
                            for (GenericMemDeal memDeal : memTour.deals) {
                                // add in memory all game not finished (set leave or claim)
                                List<TimezoneGame> listGameNotFinished = listGameOnTournamentForDealNotFinished(memTour.tourID, memDeal.dealIndex);
                                if (listGameNotFinished != null) {
                                    for (TimezoneGame game : listGameNotFinished) {
                                        game.setFinished(true);
                                        if (game.getBidContract() != null) {
                                            game.claimForPlayer(BridgeConstantes.POSITION_SOUTH, 0);
                                            GameMgr.computeScore(game);
                                        } else {
                                            game.setLeaveValue();
                                            game.setLastDate(tour.getEndDate() - 1);
                                        }
                                        GenericMemTournamentPlayer mtrPla = memTour.addResult(game, false, true);
                                    }
                                }
                                if (listGameNotFinished.size() > 0) {
                                    updateListGameDB(listGameNotFinished);
                                }

                                // insert game not existing for players started the tournament (players not playing some deals)
                                List<Long> listPlaNoResult = memDeal.getListPlayerStartedTournamentWithNoResult();
                                List<TimezoneGame> listGameToInsert = new ArrayList<>();
                                for (long pla : listPlaNoResult) {
                                    TimezoneGame g = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), memDeal.dealIndex, pla);
                                    if (g == null) {
                                        g = new TimezoneGame(pla, (TimezoneTournament)tour, memDeal.dealIndex);
                                        g.setStartDate(tour.getEndDate() - 1);
                                        g.setLastDate(tour.getEndDate() - 1);
                                        g.setLeaveValue();
                                        g.setLastDate(tour.getEndDate());
                                        listGameToInsert.add(g);
                                    }
                                    memTour.addResult(g, false, true);
                                }
                                if (listGameToInsert.size() > 0) {
                                    insertListGameDB(listGameToInsert);
                                }

                                // loop all player to update game in DB (rank & result)
                                List<TimezoneGame> listGameToUpdate = new ArrayList<>();
                                for (long pla : listPlayer) {
                                    GenericMemDealPlayer resPla = memDeal.getResultPlayer(pla);
                                    if (resPla != null) {
                                        TimezoneGame game = getGameOnTournamentAndDealForPlayer(memTour.tourID, memDeal.dealIndex, pla);
                                        if (game != null) {
                                            game.setRank(resPla.nbPlayerBetterScore + 1);
                                            game.setResult(resPla.result);
                                            listGameToUpdate.add(game);
                                        } else {
                                            log.error("No game found for player=" + pla + " on tour=" + memTour + " - deal=" + memDeal);
                                        }
                                    } else {
                                        log.error("Failed to find result on deal for player=" + pla);
                                    }
                                }
                                if (listGameToUpdate.size() > 0) {
                                    updateListGameDB(listGameToUpdate);
                                }
                            }
                            //******************************************
                            // compute tournament result & ranking
                            memTour.computeResult();
                            memTour.computeRanking(false);

                            //******************************************
                            // insert tournament result to DB
                            List<TimezoneTournamentPlayer> listTourPlay = new ArrayList<>();
                            for (GenericMemTournamentPlayer mtp : (List<GenericMemTournamentPlayer>)memTour.getListMemTournamentPlayer()) {
                                TimezoneTournamentPlayer tp = new TimezoneTournamentPlayer();
                                tp.setPlayerID(mtp.playerID);
                                tp.setTournament((TimezoneTournament)tour);
                                tp.setRank(mtp.ranking);
                                tp.setResult(mtp.result);
                                tp.setLastDate(mtp.dateLastPlay);
                                tp.setStartDate(mtp.dateStart);
                                tp.setResultType(memTour.resultType);
                                tp.setCreationDateISO(new Date());
                                listTourPlay.add(tp);
                            }
                            insertListTourPlayDB(listTourPlay);

                            //******************************************
                            // send notif
                            for (GenericMemTournamentPlayer mtp : (List<GenericMemTournamentPlayer>)memTour.getListMemTournamentPlayer()) {
                                MessageNotif notif = notifMgr.createNotifTourTimezoneFinish(mtp, memTour);
                                FBSession session = presenceMgr.getSessionForPlayerID(mtp.playerID);
                                if (session != null) {
                                    session.pushEvent(notifMgr.buildEvent(notif, mtp.playerID));
                                }
                            }

                            //******************************************
                            // set tournament finished & update tournament
                            tour.setFinished(true);
                            tour.setNbPlayers(listPlayer.size());
                            updateTournamentDB(tour);

                            // remove tournament from memory
                            memoryMgr.removeTournament(memTour.tourID);
                            log.debug("Finish tournament=" + tour + " - TS=" + (System.currentTimeMillis() - tsBegin));
                            return true;
                        } catch (Exception e) {
                            log.error("Exception to finish tournament=" + tour, e);
                        }
                    } else {
                        log.error("MemTournament close in progress - tour=" + tour);
                    }
                } else {
                    log.error("No memTour found for tour=" + tour);
                }
            } else {
                log.error("Tournament not instance of TimezoneTournament !! tour="+tour);
            }
        } else {
            log.error("Tournament null or already finished - tour=" + tour);
        }
        return false;
    }

    /**
     * Build result archive with only tournament played (all deals) by player
     * @param playerCache
     * @param offset
     * @param count
     * @return
     * @throws FBWSException
     */
    public WSResultArchive getWSResultArchiveTournament(PlayerCache playerCache, int offset, int count) throws FBWSException {
        WSResultArchive resArc = new WSResultArchive();
        resArc.setOffset(offset);
        List<WSTournament> listWSTour = new ArrayList<WSTournament>();
        Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (getConfigIntValue("resultArchiveNbDayBefore", 30)));
        long dateRef = dateBefore.getTimeInMillis();

        if (playerCache != null) {
            int nbTotal = (int)mongoTemplate.count(Query.query(Criteria.where("playerID").is(playerCache.ID).andOperator(Criteria.where("lastDate").gt(dateRef))), TimezoneTournamentPlayer.class);
            List<GenericMemTournamentPlayer> listMemTourFinished = memoryMgr.listMemTournamentForPlayer(playerCache.ID, true, 0, dateRef);
            int nbTourInProgressPlayed = listMemTourFinished.size();
            int offsetBDD = 0;
            nbTotal += nbTourInProgressPlayed;
            if (offset > nbTourInProgressPlayed) {
                // not used data from memory ...
                offsetBDD = offset - nbTourInProgressPlayed;
            }
            else if (listMemTourFinished.size() > 0){
                for (GenericMemTournamentPlayer e : listMemTourFinished) {
                    if (listWSTour.size() == count) {
                        break;
                    }
                    WSTournament wst = toWSTournament(getTournament(e.memTour.tourID), playerCache);
                    if (wst != null) {
                        listWSTour.add(wst);
                    }
                }
            }

            // if list not full, use data from BDD
            if (listWSTour.size() < count) {
                if (offsetBDD < 0) {
                    offsetBDD = 0;
                }
                List<TimezoneTournamentPlayer> listTP = listTournamentPlayerAfterDate(playerCache.ID, dateRef, offsetBDD, count - listWSTour.size());
                if (listTP != null) {
                    for (TimezoneTournamentPlayer tp :listTP) {
                        if (listWSTour.size() == count) { break;}
                        WSTournament wst = toWSTournament(tp.getTournament(), playerCache);
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

    @Override
    public Game createReplayGame(long playerID, Tournament tour, int dealIndex, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) {
        if (tour instanceof TimezoneTournament) {
            TimezoneGame replayGame = new TimezoneGame(playerID, (TimezoneTournament)tour, dealIndex);
            return replayGame;
        }
        else {
            log.error("Tournament is not instance of PrivateTournament - tour="+tour+" - playerID="+playerID);
            return null;
        }
    }

    @Override
    public void updateGamePlayArgineFinished(Game game) throws FBWSException {

    }

    @Override
    public void updateGamePlayArgine(Game game) {

    }
}
