package com.funbridge.server.tournament.game;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.engine.ArgineEngineMgr;
import com.funbridge.server.engine.BridgeEngineParam;
import com.funbridge.server.engine.BridgeEngineResult;
import com.funbridge.server.engine.EngineRest;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.tournament.data.SpreadGameData;
import com.funbridge.server.tournament.data.TournamentGamePlayer;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.engine.EngineService;
import com.funbridge.server.ws.engine.PlayGameStepData;
import com.funbridge.server.ws.event.Event;
import com.funbridge.server.ws.event.EventField;
import com.gotogames.common.bridge.*;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.tools.StringTools;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by pserent on 20/02/2015.
 */
public class GameMgr {
    protected Logger log = LogManager.getLogger(this.getClass());
    private ExecutorService threadPoolPlay = null;
    private ExecutorService threadPoolReplay = null;
    // map of thread running for game : gameID => Thread
    private ConcurrentHashMap<String, GameThread> mapThreadPlayRunning = new ConcurrentHashMap<String, GameThread>();
    // map of spread game for game : gameID => Spread data
    private ConcurrentHashMap<String, SpreadGameData> mapSpreadPlayWaiting = new ConcurrentHashMap<String, SpreadGameData>();
    private EngineRest engine = null;
    private boolean spreadEnable = false;
    private ITournamentMgr tournamentMgr = null;
    private ArgineEngineMgr argineEngineMgr = null;
    private LockWeakString lockParDeal = new LockWeakString();
    private LockWeakString lockGame = new LockWeakString();
    private EngineService engineService = null;
    private SpreadPlayWaitingPurgeTask spreadPlayWaitingPurgeTask = new SpreadPlayWaitingPurgeTask();
    private ScheduledExecutorService schedulerSpreadPlayWaitingPurge = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> schedulerSpreadPlayWaitingPurgeFuture = null;
    private Set<Long> listRunningPlayer = Collections.synchronizedSet(new HashSet<Long>());
    // Map to store current game for ArgineDuel - gameID => Game
    private ConcurrentHashMap<String, Game> mapGamePlayArgine = new ConcurrentHashMap<>();

    /**
     * Task to purge spreadPlayWaiting
     * @author pserent
     *
     */
    private class SpreadPlayWaitingPurgeTask implements Runnable {
        boolean running = false;
        @Override
        public void run() {
            if (!running) {
                running = true;
                try {
                    if (tournamentMgr.getConfigIntValue("taskPurgeSpreadPlayWaitingEnable", 1) == 1) {
                        purgeSpreadPlayExpired();
                    } else {
                        log.error("Task purge spreadPlayWaiting is not enable");
                    }
                } catch (Exception e) {
                    log.error("Exception to execute purgeSpreadPlayExpired", e);
                }
                running = false;
            } else {
                log.error("Task already running ...");
            }
        }
    }

    public GameMgr(ITournamentMgr tourMgr) {
        this.tournamentMgr = tourMgr;
        this.engine = new EngineRest(tournamentMgr.getTournamentCategoryName());
        int nbThreadPlay = tournamentMgr.getConfigIntValue("nbThreadPoolPlay", 10);
        int nbThreadReplay = tournamentMgr.getConfigIntValue("nbThreadPoolReplay", 5);
        this.spreadEnable = tournamentMgr.getConfigIntValue("spreadEnable", 1) == 1;
        this.threadPoolPlay = Executors.newFixedThreadPool(nbThreadPlay);
        this.threadPoolReplay = Executors.newFixedThreadPool(nbThreadReplay);

        // start task for purge spreadPlayWaiting
        try {
            schedulerSpreadPlayWaitingPurgeFuture = schedulerSpreadPlayWaitingPurge.scheduleAtFixedRate(spreadPlayWaitingPurgeTask, 10*Constantes.TIMESTAMP_MINUTE, Constantes.TIMESTAMP_HOUR, TimeUnit.MILLISECONDS);
            log.info("Schedule purge spreadPlayWaiting task for tournament category="+tournamentMgr.getTournamentCategoryName()+" - next run at "+Constantes.getStringDateForNextDelayScheduler(schedulerSpreadPlayWaitingPurgeFuture)+" - period (minutes)=60");
        } catch (Exception e) {
            log.error("Exception to start generator task", e);
        }

        log.info("Create GameMgr for tournament category="+tournamentMgr.getTournamentCategoryName()+" - pool nb thread for play="+nbThreadPlay+" - nb thread for replay="+nbThreadReplay+" - this="+this);
    }

    public String toString() {
        return "GameMgr for category="+tournamentMgr.getTournamentCategoryName()+" - hashCode="+ this.hashCode();
    }

    public void destroy() {
        if (engine != null) {
            engine.destroy();
        }
        schedulerSpreadPlayWaitingPurge.shutdown();
        try {
            if (schedulerSpreadPlayWaitingPurge.awaitTermination(30, TimeUnit.SECONDS)) {
                schedulerSpreadPlayWaitingPurge.shutdownNow();
            }
        } catch (InterruptedException e) {
            schedulerSpreadPlayWaitingPurge.shutdownNow();
        }
        mapThreadPlayRunning.clear();
        mapSpreadPlayWaiting.clear();
        // shutdown thread pool
        threadPoolPlay.shutdown();
        try {
            if (threadPoolPlay.awaitTermination(30, TimeUnit.SECONDS)) {
                threadPoolPlay.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPoolPlay.shutdownNow();
        }
        threadPoolReplay.shutdown();
        try {
            if (threadPoolReplay.awaitTermination(30, TimeUnit.SECONDS)) {
                threadPoolReplay.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPoolReplay.shutdownNow();
        }
        listRunningPlayer.clear();
    }

    public ArgineEngineMgr getArgineEngineMgr() {
        if (argineEngineMgr == null) {
            argineEngineMgr = ContextManager.getArgineEngineMgr();
        }
        return argineEngineMgr;
    }

    public EngineService getEngineService() {
        if (engineService == null) {
            engineService = ContextManager.getEngineService();
        }
        return engineService;
    }

    public void addPlayerRunning(long playerID) {
        listRunningPlayer.add(playerID);
    }

    public void removePlayerRunning(long playerID) {
        listRunningPlayer.remove(playerID);
    }

    public int getPlayerRunningSize() {
        return listRunningPlayer.size();
    }

    /**
     * Return the date of the next spreadPlayWaitingPurge task run.
     * @return
     */
    public String getStringDateNextSpreadPlayWaitingPurgeTask() {
        return Constantes.getStringDateForNextDelayScheduler(schedulerSpreadPlayWaitingPurgeFuture);
    }

    public ITournamentMgr getTournamentMgr() {
        return tournamentMgr;
    }


    public class ThreadPoolData {
        public int activeCount = 0;
        public int queueSize = 0;
        public int poolSize = 0;
        public String toString() {
            return "activeCount="+activeCount+" - queueSize="+queueSize+" - poolSize="+poolSize;
        }
    }

    public void addSpreadPlay(String gameID, SpreadGameData spreadData) {
        spreadData.setDateLastAddSpreadPlay(System.currentTimeMillis());
        mapSpreadPlayWaiting.put(gameID, spreadData);
        log.debug("Add spread for gameID=" + gameID + " - this=" + toString() + " - mapSpreadPlayWaiting=" + mapSpreadPlayWaiting + " - spreadData=" + spreadData);
    }

    public void removeSpreadPlay(String gameID) {
        mapSpreadPlayWaiting.remove(gameID);
        log.debug("Remove spread for gameID=" + gameID + " - this=" + toString() + " - mapSpreadPlayWaiting="+mapSpreadPlayWaiting);
    }

    public SpreadGameData getSpreadPlay(String gameID) {
        log.debug("Get spread for gameID="+gameID+" - this="+toString()+" - mapSpreadPlayWaiting="+mapSpreadPlayWaiting);
        return mapSpreadPlayWaiting.get(gameID);
    }

    public boolean isSpreadPlay(String gameID) {
        return mapSpreadPlayWaiting.containsKey(gameID);
    }

    public int purgeSpreadPlayExpired() {
        long nbSecondsExpiredSpreadPlay = tournamentMgr.getConfigIntValue("nbSecondsExpiredSpreadPlay", 60*60);
        int nbRemove = 0;
        if (nbSecondsExpiredSpreadPlay > 0) {
            for (Iterator<Map.Entry<String, SpreadGameData>> it = mapSpreadPlayWaiting.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, SpreadGameData> e = it.next();
                if (e.getValue() != null && (System.currentTimeMillis() - e.getValue().getDateLastAddSpreadPlay()) > (nbSecondsExpiredSpreadPlay*1000)) {
                    it.remove();
                    nbRemove++;
                }
            }
        }
        return nbRemove;
    }

    public EngineRest getEngine() {
        return engine;
    }

    public void addThreadPlayRunning(String gameID, GameThread thread) {
        thread.setDateLastThreadPlayRunning(System.currentTimeMillis());
        mapThreadPlayRunning.put(gameID, thread);
    }

    public GameThread getThreadPlayRunning(String gameID) {
        return mapThreadPlayRunning.get(gameID);
    }

    public void removeThreadPlayRunning(String gameID, GameThread thread) {
        if (thread != null) {
            thread.setDateLastThreadPlayRunning(0);
            if (!mapThreadPlayRunning.remove(gameID, thread)) {
                mapThreadPlayRunning.remove(gameID);
            }
        }
        else {
            mapThreadPlayRunning.remove(gameID);
        }
    }

    public Object getLockOnGame(String gameID) {
        return lockGame.getLock(gameID);
    }

    /**
     * Remove expired gameThread existing for this gameID. If existing, check dateLastThreadPlayRunning and if expired, then remove it from map.
     * @param gameID
     * @return true if gameThread not exiting or it is expired and so removed, else false
     *
     */
    public boolean removeExpiredThreadPlayRunning(String gameID) {
        GameThread gt = getThreadPlayRunning(gameID);
        if (gt != null) {
            if (FBConfiguration.getInstance().getIntValue("general.gameCheckDateOnRemoveExpiredThread", 1) == 1) {
                long nbSecondsBeforeRemoveOnCheckThreadPlayRunning = tournamentMgr.getConfigIntValue("nbSecondsBeforeRemoveOnCheckThreadPlayRunning", 5 * 60);
                if (nbSecondsBeforeRemoveOnCheckThreadPlayRunning > 0) {
                    if (System.currentTimeMillis() - gt.getDateLastThreadPlayRunning() > (nbSecondsBeforeRemoveOnCheckThreadPlayRunning * 1000)) {
                        removeThreadPlayRunning(gameID, gt);
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

    public boolean isThreadPlayRunning(String gameID) {
        return mapThreadPlayRunning.containsKey(gameID);
    }

    public Map<String, GameThread> getMapThreadRunning() {
        return mapThreadPlayRunning;
    }

    public Map<String, SpreadGameData> getMapSpreadWaiting() {
        return mapSpreadPlayWaiting;
    }

    public ExecutorService getThreadPoolPlay() {
        return threadPoolPlay;
    }

    public ExecutorService getThreadPoolReplay() {
        return threadPoolReplay;
    }

    public ThreadPoolData getThreadPoolDataPlay() {
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor)threadPoolPlay;
        ThreadPoolData data = new ThreadPoolData();
        data.activeCount = threadPool.getActiveCount();
        data.queueSize = threadPool.getQueue().size();
        data.poolSize = threadPool.getPoolSize();
        return data;
    }

    public ThreadPoolData getThreadPoolDataReplay() {
        ThreadPoolExecutor threadPool = (ThreadPoolExecutor)threadPoolReplay;
        ThreadPoolData data = new ThreadPoolData();
        data.activeCount = threadPool.getActiveCount();
        data.queueSize = threadPool.getQueue().size();
        data.poolSize = threadPool.getPoolSize();
        return data;
    }

    /**
     * Start a thread to play the game by robot. Conventions can be changed if profil > 0 and only if start is true
     * @param session
     * @param start flag to indicate if caller is startGame (send event begin_game)
     * @param conventionProfil (0 if not defined)
     * @param conventionValue
     * @param cardsConventionProfil (0 if not defined)
     * @param cardsConventionValue
     * @throws com.funbridge.server.ws.FBWSException
     */
    public void startThreadPlay(FBSession session, boolean start, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) throws FBWSException {
        Table table = session.getCurrentGameTable();
        if (table == null) {
            log.error("No table found in session="+session.getLoginID());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Game game = table.getGame();
        if (game == null) {
            log.error("Game is null in table="+table+" -  session="+session.getLoginID());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        boolean replay = game.isReplay();
        if (!replay) {
            tournamentMgr.checkGame(game);
        }
        if (start) {
            game.changeConventionsSelection(conventionProfil, conventionValue, cardsConventionProfil, cardsConventionValue);
            if (tournamentMgr.getConfigIntValue("eventGameAfterStart", 1) == 0) {
                // send event BEGIN_GAME
                session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_BEGIN_GAME, null, null);
                SpreadGameData spreadData = getSpreadPlay(game.getIDStr());
                if (spreadData != null) {
                    // waiting for spread ?
                    if (!spreadData.isWaitingForCheckSpread()) {
                        // a spread claim is possible. Send event to ask player agreement
                        session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_SPREAD, Character.toString(spreadData.getRequester()), null);
                    }
                } else {
                    // send event CURRENT_PLAYER
                    session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Before start thread Pare : game="+game);
            }
            startThreadPare(game);
        }
        GameThread gameThread = new GameThread(session, replay, this, start);
        gameThread.setSynchroMethod(isPlaySynchroMethod());
        if (replay) {
            threadPoolReplay.execute(gameThread);
        } else {
            threadPoolPlay.execute(gameThread);
        }
    }

    public void startThreadPlayArgine(Game game) throws FBWSException {
        if (game == null) {
            log.error("Game is null !!");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!ContextManager.getPresenceMgr().isServiceMaintenance()) {
            tournamentMgr.checkGame(game);

            GameThreadArgine gameThread = new GameThreadArgine(game, this);
            gameThread.setSynchroMethod(isPlaySynchroMethod());
            threadPoolPlay.execute(gameThread);
        }
    }

    /**
     * Start thread to compute pare
     * @param game
     */
    public void startThreadPare(Game game) {
        if (game != null && !game.isReplay() &&
                (game.getDeal().engineParInfo == null || game.getDeal().engineParInfo.length() == 0) &&
                tournamentMgr.isDealParEnable()) {
            if (log.isDebugEnabled()) {
                log.debug("Get par for game="+game);
            }
            GameParThread pareThread = new GameParThread(this, game);
            threadPoolPlay.execute(pareThread);
        }
    }

    /**
     * Start game execution.
     * Call step1 method while it is robot to play (play bid or card without engine response : singleton, last trick)
     * @param session
     * @throws Exception
     */
    public void playGameStep1(FBSession session) throws Exception {
        while (true) {
            if (!_playGameStep1(session)) {
                break;
            }
        }
    }

    /**
     * Start game execution.
     * Call step1 method while it is robot to play (play bid or card without engine response : singleton, last trick)
     * @param game
     * @throws Exception
     */
    public void playArgineGameStep1(Game game) throws Exception {
        while (true) {
            if (!_playArgineGameStep1(game)) {
                break;
            }
        }
    }

    /**
     * Step1 : call engine server
     * @param session
     * @return
     * @throws Exception
     */
    private boolean _playGameStep1(FBSession session) throws Exception {
        if (ContextManager.getPresenceMgr().isServiceMaintenance()) {
            log.warn("Service is in maintenance !");
            return false;
        }
        if (session == null) {
            log.error("Parameter session is null !");
            throw new Exception("Parameter session is null !");
        }
        Table table = session.getCurrentGameTable();
        if (table == null) {
            log.error("Parameter table in session is null !");
            throw new Exception("Parameter table in session is null !");
        }
        Game game = table.getGame();
        if (game == null) {
            log.error("Parameter game in session is null !");
            throw new Exception("Parameter game in session is null !");
        }
        BridgeEngineParam param = null;
        boolean bReqBid = false;
        String resContent = null;
        boolean bReqMotor = false;

        try {
            //-----------------------------------------
            // PREPARE REQUEST MOTOR

            synchronized (getLockOnGame(game.getIDStr())) {
                if (isRobotToPlay(table, game)) {
                    bReqBid = !game.isEndBid();
                    // BIDS
                    if (bReqBid) {
                        String nextBidDeal = game.getDeal().getNextBid(game.bids);
                        // anticipation => bid set on deal (TDC)
                        if (nextBidDeal != null) {
                            List<Event> eventList = new ArrayList<>();
                            processResult(game, table, session, nextBidDeal, eventList, null);
                            if (eventList.size() > 0) {
                                for (Event e : eventList) {
                                    session.pushEvent(e);
                                }
                            }
                            return isRobotToPlay(table, game);
                        } else {
                            bReqMotor = true;
                            param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_BID, false);
                        }
                    }
                    // CARDS
                    else {
                        if (!game.isFinished()) {
                            // anticipation => card set on deal (TDC)
                            String nextCardDeal = game.getDeal().getNextCard(game.bids, game.cards);
                            if (nextCardDeal != null) {
                                List<Event> eventList = new ArrayList<>();
                                processResult(game, table, session, nextCardDeal, eventList, null);
                                if (eventList.size() > 0) {
                                    for (Event e : eventList) {
                                        session.pushEvent(e);
                                    }
                                }
                                return isRobotToPlay(table, game);
                            }
                            else {
                                BridgeCard card = null;
                                // OPTIMISATIONS - Last trick - simply play last card
                                if (game.getNbTricks() == 12) {
                                    // IF LAST TRICK, PLAY THE LAST CARD !
                                    card = GameBridgeRule.getLastCardForPlayer(game.getListCard(),
                                            GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                            game.getCurrentPlayer());
                                    if (card == null) {
                                        log.error("Last trick : No card found for player=" + game.getCurrentPlayer() + " - game=" + game.toString());
                                    }
                                }
                                // PLAYER HAS ONLY ONE CARD IN THIS COLOR => PLAY IT !
                                else {
                                    // not at begin trick !
                                    if (!GameBridgeRule.isBeginTrick(game.getListCard())) {
                                        BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                                        if (firstcardTrick != null) {
                                            card = GameBridgeRule.getOnlyOneCardForPlayerAndColor(game.getListCard(),
                                                    GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                                    game.getCurrentPlayer(), firstcardTrick.getColor());
                                        }
                                    }
                                }
                                if (card == null) {
                                    bReqMotor = true;
                                    param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_CARD, false);
                                } else {
                                    List<Event> eventList = new ArrayList<>();
                                    if (!playValidCard(table, game, card, session, eventList, false)) {
                                        throw new Exception("Method playValidCard return false !");
                                    }
                                    if (eventList.size() > 0) {
                                        for (Event e : eventList) {
                                            session.pushEvent(e);
                                        }
                                    }
                                    if (!game.isReplay()) {
                                        if (game.isFinished()) {
                                            tournamentMgr.updateGameFinished(session);
                                        }
                                    }
                                    return isRobotToPlay(table, game);
                                }
                            }
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("It is not robot to play : position=" + game.getCurrentPlayer());
                    }
                    return false;
                }
            }// end of synchornization

            //----------------------------------------------
            // SEND REQUEST TO MOTOR
            if (bReqMotor && param != null) {
                String operation = "";
                long ts = System.currentTimeMillis();
                PlayGameStepData stepData = new PlayGameStepData();
                if (param.getAsyncID() != null) {
                    stepData.asyncID = param.getAsyncID();
                } else {
                    stepData.asyncID = PlayGameStepData.buildAsyncID(game.getIDStr(), game.getStep());
                    param.setAsyncID(stepData.asyncID);
                }
                if (isEngineWebSocketEnable()) {
                    if (param.getAsyncID() != null) {
                        param.setUseWebsocket(true);
                    }
                }
                stepData.gameMgr = this;
                stepData.sessionID = session.getID();
                stepData.step = game.getStep();
                stepData.timestamp = System.currentTimeMillis();
                stepData.param = param.toString();
                stepData.requestType = bReqBid ? Constantes.ENGINE_REQUEST_TYPE_BID : Constantes.ENGINE_REQUEST_TYPE_CARD;
                getEngineService().putStepData(stepData);
                BridgeEngineResult engineResult = null;
                if (bReqBid) {
                    operation = "nextBid";
                    engineResult = engine.getNextBid(param);
                } else {
                    operation = "nextCard";
                    engineResult = engine.getNextCard(param);
                }

                if (log.isDebugEnabled()) {
                    log.debug("engineResult="+engineResult);
                }
                // response from engine server != ASYNC => result found from cache => process result
                if (engineResult != null) {
                    if (!engineResult.isError()) {
                        if (!engineResult.getContent().equals("ASYNC")) {
                            // result found in cache !
                            List<Event> eventList = new ArrayList<>();
                            processResult(game, table, session, engineResult.getContent(), eventList, param.toString());
                            if (eventList.size() > 0) {
                                for (Event e : eventList) {
                                    session.pushEvent(e);
                                }
                            }
                            if (!game.isReplay()) {
                                if (game.isFinished()) {
                                    tournamentMgr.updateGameFinished(session);
                                }
                            }
                            getEngineService().removeStepData(stepData.asyncID);
                            return isRobotToPlay(table, game);
                        }
                    } else {
                        log.error("Error from engine - engineResult="+engineResult);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Exception on thread playRobotGame - game=" + game, e);
            // send event
            session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_ENGINE_ERROR, null, null);
        }
        return false;
    }

    /**
     * Update game with result from engine
     * @param game
     * @param table
     * @param session
     * @param result
     * @param eventList
     * @param param
     * @throws Exception
     */
    private void processResult(Game game, Table table, FBSession session, String result, List<Event> eventList, String param) throws Exception {
        // Process BID
        if (!game.isEndBid()) {
            // result can contains many bids (anticipation)
            if (result == null) {
                if (tournamentMgr.getConfigIntValue("gameThreadBidNotValidPlayPA", 0) == 1) {
                    result = "PA";
                    log.error("getNexBid not valid result=" + result + " - game=" + game + "  => play PA - param=" + param);
                } else {
                    throw new Exception("Bid computed by engine not valid ! result=" + result + " - game=" + game + " - engineParam=" + param);
                }
            }

            int idxRes = 0;
            boolean bContinue = true;
            // loop on each bid in anticipation
            while ((idxRes + 1) < result.length() && bContinue) {
                String tempBid = result.substring(idxRes, idxRes + 2);
                idxRes += 2;
                boolean bidAlert = false;
                if (idxRes < result.length() && result.charAt(idxRes) == 'a') {
                    bidAlert = true;
                    idxRes++;
                }
                BridgeBid bid = BridgeBid.createBid(tempBid, game.getCurrentPlayer(), bidAlert);
                if (bid == null || !GameBridgeRule.isBidValid(game.getListBid(), bid)) {
                    if (tournamentMgr.getConfigIntValue("gameThreadBidNotValidPlayPA", 0) == 1) {
                        log.error("Bid computed by engine not valid - result=" + tempBid + " - bid=" + bid + " - game=" + game + " - engineParam=" + param + " -> Play PA");
                        bid = BridgeBid.createBid("PA", game.getCurrentPlayer());
                        bContinue = false;
                    } else {
                        throw new Exception("Bid computed by engine not valid ! bid=" + bid + " - tempBid=" + tempBid + " - result=" + result + " - game=" + game + " - engineParam=" + param);
                    }
                }
                if (!playValidBid(table, game, bid, session, eventList)) {
                    throw new Exception("Method playValidBid return false !");
                }
                // check it is always robot to play
                if (game.isEndBid() || !isRobotToPlay(table, game)) {
                    bContinue = false;
                }
            }
        }
        // Process CARD
        else {
            BridgeCard card = null;
            // result can contains many cards (anticipation)
            if (result == null) {
                if (tournamentMgr.getConfigIntValue("gameThreadCardNotValidPlaySmall", 0) == 1) {
                    // error => play the smallest card !
                    BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                    card = GameBridgeRule.getSmallestCardForPlayerAndColor(game.getListCard(),
                            GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                            game.getCurrentPlayer(),
                            (firstcardTrick != null ? firstcardTrick.getColor() : null));
                    log.error("getNextCard not valid result=" + result + " - game=" + game + "  => play smallest card=" + card + " - engineParam=" + param);
                } else {
                    throw new Exception("Card computed by engine not valid ! card=" + card + " - result=" + result + " - game=" + game + " - engineParam=" + param);
                }
                if (!playValidCard(table, game, card, session, eventList, false)) {
                    throw new Exception("Method playValidCard return false !");
                }
            } else {
                int idxRes = 0;
                boolean bContinue = true;
                // loop on each card in anticipation
                while (idxRes < result.length() && bContinue) {
                    String tempCard = result.substring(idxRes, idxRes + 2);
                    card = BridgeCard.createCard(tempCard, game.getCurrentPlayer());
                    List<BridgeCard> listCardDeal = GameBridgeRule.convertCardDealToList(game.getDeal().getCards());
                    if (card == null || !GameBridgeRule.isCardValid(game.getListCard(), card, listCardDeal)) {
                        game.setEngineFailed(true);
                        log.error("Failed isCardValid => game=["+game+"] - tricksWinner="+game.getTricksWinner()+" - listCard=["+ StringTools.listToString(game.getListCard())+"] - card="+card);
                        log.error("TricksWinnerHistoric="+game.getTricksWinnerHistoric());
                        if (tournamentMgr.getConfigIntValue("gameThreadCardNotValidPlaySmall", 0) == 1) {
                            // error => play the smallest card !
                            BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                            card = GameBridgeRule.getSmallestCardForPlayerAndColor(game.getListCard(),
                                    GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                    game.getCurrentPlayer(),
                                    (firstcardTrick != null ? firstcardTrick.getColor() : null));
                            log.error("Card computed by engine not valid - result=" + tempCard + " - game=" + game + " - engineParam=" + param + " => play smallest card=" + card);
                            bContinue = false;
                        } else {
                            throw new Exception("Card computed by engine not valid ! card=" + card + " - tempCard=" + tempCard + " - result=" + result + " - game=[" + game + "] - engineParam=[" + param+"]");
                        }
                    }
                    idxRes += 2;
                    boolean spreadResultArgine = false;
                    if (result.length() > idxRes && result.charAt(idxRes) == 's') {
                        spreadResultArgine = true;
                        idxRes++;
                    }
                    if (!playValidCard(table, game, card, session, eventList, spreadResultArgine)) {
                        throw new Exception("Method playValidCard return false !");
                    }
                    // check it is always robot to play
                    if (game.isFinished() || !isRobotToPlay(table, game)) {
                        bContinue = false;
                    }

                }
            }
        }
    }

    /***
     * Step2 of playGame => result is received from engine server. Process result and call playGameStep1 if it is robot to play
     * @param stepData
     * @param result
     */
    public void playGameStep2(PlayGameStepData stepData, String result) {
        if (log.isDebugEnabled()) {
            log.debug("stepData="+stepData+" - result="+result);
        }
        if (stepData != null) {
            if (stepData.playArgine) {
                if (stepData.getDataID() != null) {
                    Game game = getArgineGameRunning(stepData.getDataID());
                    if (game != null) {
                        try {
                            synchronized (getLockOnGame(game.getIDStr())) {
                                // check game status
                                if (game.getStep() != stepData.step) {
                                    throw new Exception("Error gameStep is not the same before sending request ! current=" + game.getStep() + " - previous=" + stepData.step + " - game=" + game.toString());
                                }
                                if (!isRobotToPlayArgine(game)) {
                                    throw new Exception("Error no robot to play ! position=" + game.getCurrentPlayer() + " - game=" + game.toString());
                                }
                                //------------------------------
                                // PROCESS ENGINE RESULT
                                processResultPlayArgine(game, result, stepData.param);
                                if (!game.isReplay()) {
                                    // after play card or bid, update tournament
                                    if (game.isFinished()) {
                                        tournamentMgr.updateGamePlayArgineFinished(game);
                                    }
                                }
                            } // end of synchronization

                            //***********************************
                            // next robot to play => call step1
                            if (isRobotToPlayArgine(game)) {
                                playArgineGameStep1(game);
                            }
                        } catch (Exception e) {
                            log.error("Exception on thread playRobotGame - game=" + game, e);
                        }
                    }
                }
            } else {
                FBSession session = (FBSession) (ContextManager.getPresenceMgr().getSession(stepData.sessionID));
                if (session != null) {
                    //-----------------------------------------------
                    // TREAT RESPONSE
                    Table table = session.getCurrentGameTable();
                    Game game = table.getGame();
                    String gameID = stepData.getDataID();
                    if (game != null && gameID != null && gameID.equals(game.getIDStr())) {
                        try {
                            List<Event> eventList = new ArrayList<>();

                            synchronized (getLockOnGame(game.getIDStr())) {
                                // check game status
                                if (game.getStep() != stepData.step) {
                                    throw new Exception("Error gameStep is not the same before sending request ! current=" + game.getStep() + " - previous=" + stepData.step + " - game=" + game.toString());
                                }
                                if (!isRobotToPlay(table, game)) {
                                    throw new Exception("Error no robot to play ! position=" + game.getCurrentPlayer() + " - game=" + game.toString());
                                }
                                //------------------------------
                                // PROCESS ENGINE RESULT
                                processResult(game, table, session, result, eventList, stepData.param);
                                if (!game.isReplay()) {
                                    // after play card or bid, update tournament
                                    if (game.isFinished()) {
                                        tournamentMgr.updateGameFinished(session);
                                    }
                                }
                            } // end of synchronization

                            // push event list
                            if (eventList.size() > 0) {
                                for (Event e : eventList) {
                                    session.pushEvent(e);
                                }
                            }
                            //***********************************
                            // next robot to play => call step1
                            if (isRobotToPlay(table, game)) {
                                playGameStep1(session);
                            }
                        } catch (Exception e) {
                            log.error("Exception on thread playRobotGame - game=" + game, e);
                            // send event
                            session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_ENGINE_ERROR, null, null);
                        }
                    } else {
                        log.error("No game found in session - stepData=" + stepData + " - session=" + session);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("No session found for stepData=" + stepData);
                    }
                }
            }
        } else {
            log.error("Step Data is null !");
        }
    }

    /**
     * Retrieve advice from argine on current step
     * @param session
     * @return
     * @throws Exception
     */
    public String getArgineAdvice(FBSession session) throws FBWSException{
        if (session == null) {
            log.error("Parameter session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, "Parameter session is null !");
        }
        Table table = session.getCurrentGameTable();
        if (table == null) {
            log.error("Parameter table in session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, "Parameter table in session is null ! - session="+session);
        }
        Game game = table.getGame();
        if (game == null) {
            log.error("Parameter game in session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, "Parameter game in session is null ! - session="+session);
        }
        // check if a thread is not running for this game
        if (isThreadPlayRunning(game.getIDStr())) {
            log.warn("A thread is currently running for game=" + game);
            if (!removeExpiredThreadPlayRunning(game.getIDStr())) {
                log.error("Thread not yet expired ... game="+game);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        char playerPosition = table.getPlayerPosition(session.getLoginID());
        char currentPlayer = game.getCurrentPlayer();
        // check current position
        if (currentPlayer != playerPosition) {
            // Partner is playing : position is in declarer side and current player is a robot and player is partner of current player
            boolean bPartnerAuth = (GameBridgeRule.isPositionInDeclarerSide(playerPosition, game.getDeclarer()) && !table.isPlayerHumanAtPosition(playerPosition) && GameBridgeRule.isPartenaire(playerPosition, currentPlayer));
            if (!bPartnerAuth){
                log.error("CURRENT POSITION IS NOT PLAYER POSITION : current="+currentPlayer+" and not "+playerPosition+" - game="+game);
                throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
            }
        }
        BridgeEngineParam param = null;
        boolean bReqBid = false;
        boolean synchroMethod = isPlaySynchroMethod();//tournamentMgr.getConfigIntValue("playSynchroMethod", 0) == 1;
        try{
            // PREPARE ENGINE REQUEST
            synchronized(getLockOnGame(game.getIDStr())){
                if(isRobotToPlay(table, game)){
                    log.error("It's the robot's turn to play !");
                    return null;
                }
                bReqBid = !game.isEndBid();
                if(bReqBid){
                    param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_BID, synchroMethod);
                } else {
                    BridgeCard card = null;
                    // OPTIMISATIONS - Last trick - simply play last card
                    if (game.getNbTricks() == 12) {
                        // IF LAST TRICK, PLAY THE LAST CARD !
                        card = GameBridgeRule.getLastCardForPlayer(game.getListCard(),
                                GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                game.getCurrentPlayer());
                        if (card == null) {
                            log.error("Last trick : No card found for player=" + game.getCurrentPlayer() + " - game=" + game.toString());
                        }
                    }
                    // PLAYER HAS ONLY ONE CARD IN THIS COLOR => PLAY IT !
                    else {
                        // not at begin trick !
                        if (!GameBridgeRule.isBeginTrick(game.getListCard())) {
                            BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                            if (firstcardTrick != null) {
                                card = GameBridgeRule.getOnlyOneCardForPlayerAndColor(game.getListCard(),
                                        GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                        game.getCurrentPlayer(), firstcardTrick.getColor());
                            }
                        }
                    }
                    if (card != null) {
                        return card.toString();
                    } else {
                        param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_CARD, synchroMethod);
                    }
                }
            }

            // SEND REQUEST TO ENGINE
            if(param != null){
                // IMPORTANT DO NOT USE CACHE FOR ADVISE !!
                param.setUseCache(false);
                if (!synchroMethod) {
                    PlayGameStepData stepData = new PlayGameStepData();
                    if (param.getAsyncID() != null) {
                        stepData.asyncID = param.getAsyncID();
                    } else {
                        stepData.asyncID = PlayGameStepData.buildAsyncID(game.getIDStr(), game.getStep());
                        param.setAsyncID(stepData.asyncID);
                    }
                    if (isEngineWebSocketEnable()) {
                        if (param.getAsyncID() != null) {
                            param.setUseWebsocket(true);
                        }
                    }
                    stepData.gameMgr = this;
                    stepData.sessionID = session.getID();
                    stepData.step = game.getStep();
                    stepData.timestamp = System.currentTimeMillis();
                    stepData.param = param.toString();
                    stepData.requestType = Constantes.ENGINE_REQUEST_TYPE_ADVICE;
                    getEngineService().putStepData(stepData);
                }

                BridgeEngineResult engineResult = null;
                if (bReqBid) {
                    engineResult = engine.getNextBid(param);
                } else {
                    engineResult = engine.getNextCard(param);
                }
                if(engineResult != null && !engineResult.isError() && engineResult.getContent().length() > 1 ){
                    if (!engineResult.getContent().equals("ASYNC")) {
                        // synchro method
                        return engineResult.getContent().substring(0,2);
                    }
                    // asynchro method => result on webscoket
                    return "";
                } else {
                    log.error("Error from engine - engineResult="+engineResult);
                }
            }

        } catch (Exception e) {
            log.error("Exception on thread playRobotGame - game=" + game, e);
        }
        return null;
    }

    /**
     * Process answer for PAR on deal
     * @param stepData
     * @param result
     */
    public void answerPar(PlayGameStepData stepData, String result) {
        if (log.isDebugEnabled()) {
            log.debug("stepData="+stepData+" - result="+result);
        }
        if (result == null || result.length() == 0 || result.equals(Constantes.ENGINE_RESULT_NULL)) {
            result = Constantes.ENGINE_RESULT_EMPTY;
        }

        String tourID = tournamentMgr._extractTourIDFromDealID(stepData.getDataID());
        Tournament tour = tournamentMgr.getTournament(tourID);
        if (tour != null) {
            int dealIDx = tournamentMgr._extractDealIndexFromDealID(stepData.getDataID());
            Deal deal = tour.getDealAtIndex(dealIDx);
            if (deal != null) {
                synchronized(lockParDeal.getLock(stepData.getDataID())) {
                    // save pare result in deal
                    deal.setEngineParInfo(result);
                    try {
                        tournamentMgr.updateTournamentDB(tour);
                    } catch (Exception e) {
                        log.error("Failed to update tour=" + tour, e);
                    }
                }
            } else {
                log.error("Deal not found - stepDate="+stepData);
            }
        } else {
            log.error("Tour not found - stepData="+stepData);
        }
    }


    /**
     * The core method called by thread play. Run next bid or card.
     * @param session
     * @param gameThread
     * @return
     * @throws Exception
     */
    public boolean playGameThread(FBSession session, GameThread gameThread) throws Exception {
        boolean bNextPlayerEngine = false;
        if (session == null) {
            log.error("Parameter session is null !");
            return false;
        }
        boolean replay = gameThread.isReplay();
        Table table = session.getCurrentGameTable();
        if (table == null) {
            log.error("Parameter table in session is null !");
            return false;
        }
        Game game = table.getGame();
        if (game == null) {
            log.error("Parameter game in session is null !");
            return false;
        }
        BridgeEngineParam param = null;
        BridgeEngineResult result = null;
        boolean bReqBid = false;
        String resContent = null;
        boolean bReqMotor = false;
        String gameID = "";
        int gameStep = 0;

        try {
            //-----------------------------------------
            // PREPARE REQUEST MOTOR
            synchronized (getLockOnGame(game.getIDStr())) {
                if (gameThread.isInterrupt()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Interrupt current thread ! " + gameThread.getThreadName());
                    }
                    return false;
                }
                gameID = game.getIDStr();
                gameStep = game.getStep();
                bNextPlayerEngine = isRobotToPlay(table, game);
                if (bNextPlayerEngine) {
                    bNextPlayerEngine = false;
                    bReqBid = !game.isEndBid();
                    // BIDS
                    if (bReqBid) {
                        bReqMotor = true;
                        param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_BID, true);
                    }
                    // CARDS
                    else {
                        if (!game.isFinished()) {
                            BridgeCard card = null;
                            // OPTIMISATIONS - Last trick - simply play last card
                            if (game.getNbTricks() == 12) {
                                // IF LAST TRICK, PLAY THE LAST CARD !
                                card = GameBridgeRule.getLastCardForPlayer(game.getListCard(),
                                        GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                        game.getCurrentPlayer());
                                if (card == null) {
                                    log.error("Last trick : No card found for player="+game.getCurrentPlayer()+" - game="+game.toString());
                                }
                            }
                            // PLAYER HAS ONLY ONE CARD IN THIS COLOR => PLAY IT !
                            else {
                                // not at begin trick !
                                if (!GameBridgeRule.isBeginTrick(game.getListCard())) {
                                    BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                                    if (firstcardTrick != null) {
                                        card = GameBridgeRule.getOnlyOneCardForPlayerAndColor(game.getListCard(),
                                                GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                                game.getCurrentPlayer(), firstcardTrick.getColor());
                                    }
                                }
                            }
                            if (card == null) {
                                bReqMotor = true;
                                param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_CARD, true);
                            } else {
                                List<Event> eventList = new ArrayList<>();
                                if (!playValidCard(table, game, card, session, eventList, false)) {throw new Exception("Method playValidCard return false !");}
                                if (eventList.size() > 0) {
                                    for (Event e : eventList) {
                                        session.pushEvent(e);
                                    }
                                }
                                if (!replay) {
                                    if (game.isFinished()) {
                                        tournamentMgr.updateGameFinished(session);
                                    }
                                }
                                return isRobotToPlay(table, game);
                            }
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("It is not robot to play : position=" + game.getCurrentPlayer());
                    }
                    return false;
                }
            }// end of synchornization

            //----------------------------------------------
            // SEND REQUEST TO MOTOR
            if (gameThread.isInterrupt()) {
                if (log.isDebugEnabled()) {
                    log.debug("Interrupt current thread ! " + gameThread.getThreadName());
                }
                return false;
            }
            if (bReqMotor && param != null) {
                String operation = "";
                long ts = System.currentTimeMillis();
                if (bReqBid) {
                    operation = "nextBid";
                    String nextBidDeal = game.getDeal().getNextBid(game.bids);
                    if (nextBidDeal != null) {
                        result = new BridgeEngineResult(nextBidDeal);
                    } else {
                        result = engine.getNextBid(param);
                    }
                } else {
                    operation = "nextCard";
                    String nextCardDeal = game.getDeal().getNextCard(game.bids, game.cards);
                    if (nextCardDeal != null) {
                        result = new BridgeEngineResult(nextCardDeal);
                    } else {
                        result = engine.getNextCard(param);
                    }
                }
                if (result != null && !result.isError()) {
                    resContent = result.getContent();
                } else {
                    log.error("Error operation="+operation+" - game="+game.toString()+" - engineParam="+param);
                }

                if (resContent == null) {
                    throw new Exception("operation="+operation+" result is null - param="+param+" => throw exception");
                }
            }

            //-----------------------------------------------
            // TREAT RESPONSE
            if (gameThread.isInterrupt()) {
                if (log.isDebugEnabled()) {
                    log.debug("Interrupt current thread ! " + gameThread.getThreadName());
                }
                return false;
            }
            table = session.getCurrentGameTable();
            game = table.getGame();
            if (game != null && gameID.equals(game.getIDStr())) {
                synchronized (getLockOnGame(game.getIDStr())) {
                    // check game status
                    boolean bStatusOK = true;
                    if (game.getStep() != gameStep) {
                        bStatusOK = false;
                        log.error("Error gameStep is not the same before sending request ! current="+game.getStep()+" - previous="+gameStep+" - game="+game.toString());
                    }
                    if (!isRobotToPlay(table, game)) {
                        bStatusOK = false;
                        log.error("Error no robot to play ! position="+game.getCurrentPlayer()+" - game="+game.toString());
                    }
                    if (!bStatusOK) {
                        return false;
                    }
                    List<Event> eventList = new ArrayList<>();
                    //------------------------------
                    // NEXT BID
                    if (bReqBid) {
                        // result can contains many bids (anticipation)
                        if (resContent == null) {
                            if (tournamentMgr.getConfigIntValue("gameThreadBidNotValidPlayPA", 0) == 1) {
                                resContent = "PA";
                                log.error("getNexBid not valid result="+resContent+" - game="+game+"  => play PA - param="+param);
                            } else {
                                throw new Exception("Bid computed by engine not valid ! resContent="+resContent+" - game="+game+" - engineParam="+param);
                            }
                        }

                        int idxRes = 0;
                        boolean bContinue = true;
                        // loop on each bid in anticipation
                        while ((idxRes+1) < resContent.length() && bContinue) {
                            String tempBid = resContent.substring(idxRes, idxRes+2);
                            idxRes += 2;
                            boolean bidAlert = false;
                            if (idxRes < resContent.length() && resContent.charAt(idxRes) == 'a') {
                                bidAlert = true;
                                idxRes++;
                            }
                            BridgeBid bid = BridgeBid.createBid(tempBid, game.getCurrentPlayer(), bidAlert);
                            if (bid == null || !GameBridgeRule.isBidValid(game.getListBid(), bid)) {
                                if (tournamentMgr.getConfigIntValue("gameThreadBidNotValidPlayPA", 0) == 1) {
                                    log.error("Bid computed by engine not valid - result="+tempBid+" - bid="+bid+" - game="+game+" - engineParam="+param+" -> Play PA");
                                    bid = BridgeBid.createBid("PA", game.getCurrentPlayer());
                                    bContinue = false;
                                } else {
                                    throw new Exception("Bid computed by engine not valid ! bid="+bid+" - tempBid="+tempBid+" - resContent="+resContent+" - game="+game+" - engineParam="+param);
                                }
                            }
                            if (!playValidBid(table, game, bid, session, eventList)) {throw new Exception("Method playValidBid return false !");}
                            // check it is always robot to play
                            if (game.isEndBid() || !isRobotToPlay(table, game)) {
                                bContinue = false;
                            }

                        }
                    }
                    // -----------------------
                    // NEXT CARD
                    else {
                        BridgeCard card = null;
                        // result can contains many cards (anticipation)
                        if (resContent == null) {
                            if (tournamentMgr.getConfigIntValue("gameThreadCardNotValidPlaySmall", 0) == 1) {
                                // error => play the smallest card !
                                BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                                card = GameBridgeRule.getSmallestCardForPlayerAndColor(game.getListCard(),
                                        GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                        game.getCurrentPlayer(),
                                        (firstcardTrick != null ? firstcardTrick.getColor() : null));
                                log.error("getNextCard not valid result=" + resContent + " - game=" + game + "  => play smallest card=" + card + " - engineParam=" + param);
                            } else {
                                throw new Exception("Card computed by engine not valid ! card="+card+" - resContent="+resContent+" - game="+game+" - engineParam="+param);
                            }
                            if (!playValidCard(table, game, card, session, eventList, false)) {throw new Exception("Method playValidCard return false !");}
                        } else {
                            int idxRes = 0;
                            boolean bContinue = true;
                            // loop on each card in anticipation
                            while (idxRes < resContent.length() && bContinue) {
                                String tempCard = resContent.substring(idxRes, idxRes+2);
                                card = BridgeCard.createCard(tempCard, game.getCurrentPlayer());
                                if (card == null  || !GameBridgeRule.isCardValid(game.getListCard(), card, GameBridgeRule.convertCardDealToList(game.getDeal().getCards()))) {
                                    if (tournamentMgr.getConfigIntValue("gameThreadCardNotValidPlaySmall", 0) == 1) {
                                        // error => play the smallest card !
                                        BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                                        card = GameBridgeRule.getSmallestCardForPlayerAndColor(game.getListCard(),
                                                GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                                game.getCurrentPlayer(),
                                                (firstcardTrick != null ? firstcardTrick.getColor() : null));
                                        log.error("Card computed by engine not valid - result=" + tempCard + " - game=" + game + " - engineParam=" + param + " => play smallest card=" + card);
                                        bContinue = false;
                                    } else {
                                        throw new Exception("Card computed by engine not valid ! card="+card+" - tempCard="+tempCard+" - resContent="+resContent+" - game="+game+" - engineParam="+param);
                                    }
                                }
                                idxRes += 2;
                                boolean spreadResultArgine = false;
                                if (resContent.length() > idxRes && resContent.charAt(idxRes) == 's') {
                                    spreadResultArgine = true;
                                    idxRes++;
                                }
                                if (!playValidCard(table, game, card, session, eventList, spreadResultArgine)) {throw new Exception("Method playValidCard return false !");}
                                // check it is always robot to play
                                if (game.isFinished() || !isRobotToPlay(table, game)) {
                                    bContinue = false;
                                }

                            }
                        }
                    }
                    if (!replay) {
                        // after play card or bid, update tournament
                        if (game.isFinished()) {
                            tournamentMgr.updateGameFinished(session);
                        }
                    }
                    if (eventList.size() > 0) {
                        for (Event e : eventList) {
                            session.pushEvent(e);
                        }
                    }
                    bNextPlayerEngine = isRobotToPlay(table, game);
                } // end of synchronization
            }
        } catch (Exception e) {
            log.error("Exception on thread playRobotGame - game="+game, e);
            // send event
            session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_ENGINE_ERROR, null, null);
        }

        return bNextPlayerEngine;
    }

    /**
     * Check if it is engine to play
     * @param table
     * @param game
     * @return
     */
    private boolean isRobotToPlay(Table table, Game game) {
        if (game == null) {
            log.error("Game is null !");
            return false;
        }
        if (table == null) {
            log.error("Table is null !");
            return false;
        }
        // game finished => no need to play
        if (game.isFinished()) {
            if (log.isDebugEnabled()) {
                log.debug("Game is already finished !");
            }
            return false;
        }
        // check tournamnent not finished
        if (!game.isReplay()) {
            if ( table.getTournament().getCategory() != Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE
                    && table.getTournament().getCategory() != Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE
                    && table.getTournament().isFinished() ) {
                if (log.isDebugEnabled()) {
                    log.debug("Tournament is finished !");
                }
                return false;
            }
        }
        // all player must be present
        if (table.isAllPlayerPresent()) {
            // only for robot player
            TournamentGamePlayer tgp = table.getGamePlayerAtPosition(game.getCurrentPlayer());
            if (tgp != null && !tgp.isHuman()) {
                if (isSpreadPlay(game.getIDStr())) {
                    // waiting for spread ...
                    return false;
                }
                // after bids, north is playing by south only if declarer is north or south
                return !game.isEndBid() || game.getCurrentPlayer() != BridgeConstantes.POSITION_NORTH ||
                        (game.getDeclarer() != BridgeConstantes.POSITION_SOUTH && game.getDeclarer() != BridgeConstantes.POSITION_NORTH);
            }
        }
        return false;
    }

    /**
     * Check if it is engine to play
     * @param game
     * @return
     */
    private boolean isRobotToPlayArgine(Game game) {
        if (game == null) {
            log.error("Game is null !");
            return false;
        }
        // game finished => no need to play
        if (game.isFinished()) {
            if (log.isDebugEnabled()) {
                log.debug("Game is already finished !");
            }
            return false;
        }
        // check tournamnent not finished
        if ( game.getTournament().getCategory() != Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE
                && game.getTournament().getCategory() != Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE
                && game.getTournament().isFinished()) {
            if (log.isDebugEnabled()) {
                log.debug("Tournament is finished !");
            }
            return false;
        }
        return true;
    }

    /**
     * Play the bid and set the game data
     * @param table
     * @param game
     * @param bid
     * @param session
     * @param eventListAfterMemoryUpdate
     * @return true if no error else false
     */
    private boolean playValidBid(Table table, Game game, BridgeBid bid, FBSession session, List<Event> eventListAfterMemoryUpdate) {
        if (table != null && game != null && bid != null) {
            // game is finished => no update to do !
            if (game.isFinished()) {
                if (log.isDebugEnabled()) {
                    log.debug("Game is already finished !");
                }
                return true;
            }

            // game bids is finished
            if (game.isEndBid()) {
                log.error("Bids are already finished ! - gameID="+game.getIDStr()+" - bid="+bid+" - bids="+game.getBids());
                return false;
            }

            // the bid is valid => add to the played bids
            game.addBid(bid);

            // push event play bid
            session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_BID, /*bid.toString()+bid.getOwner()*/bid.getStringWithOwnerAndAlert(), null);

            // is it the end of bid ?
            if (GameBridgeRule.isBidsFinished(game.getListBid())) {
                if (game.buildContract()) {
                    String msgEvent = game.getContractWS();
                    if (game.getContractType() != Constantes.CONTRACT_TYPE_PASS) {
                        msgEvent += "-"+game.getDeclarer();
                    }
                    // push event end of bids
                    session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_END_BIDS, msgEvent, null);
                    // all info for South bids received => start process analyze
                    if (game.checkAllBidInfoSouth()) {
                        processAnalyzeForSouthBids(game);
                    }

                    // bid is PASS
                    if (game.getContractType() == Constantes.CONTRACT_TYPE_PASS) {
                        updateAtEndGame(table, game, session, eventListAfterMemoryUpdate);
                    } else {
                        // send event current player
                        session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(GameBridgeRule.getNextPosition(game.getDeclarer())), null);
                    }
                } else {
                    log.error("Failed to buildContract on game - game="+game);
                }
            } else {
                // send event current player
                session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null);
            }
            game.setLastDate(Calendar.getInstance().getTimeInMillis());
            return true;
        }
        log.error("A PARAMETER IS NULL ! : bid="+bid);
        return false;
    }

    /**
     * Play the bid and set the game data
     * @param game
     * @param bid
     * @return true if no error else false
     */
    private boolean playArgineValidBid(Game game, BridgeBid bid) {
        if (game != null && bid != null) {
            // game is finished => no update to do !
            if (game.isFinished()) {
                if (log.isDebugEnabled()) {
                    log.debug("Game is already finished !");
                }
                return true;
            }

            // game bids is finished
            if (game.isEndBid()) {
                log.error("Bids are already finished ! - gameID="+game.getIDStr()+" - bid="+bid+" - bids="+game.getBids());
                return false;
            }

            // the bid is valid => add to the played bids
            game.addBid(bid);

            // is it the end of bid ?
            if (GameBridgeRule.isBidsFinished(game.getListBid())) {
                if (game.buildContract()) {
                    // all info for South bids received => start process analyze
                    if (game.checkAllBidInfoSouth()) {
                        processAnalyzeForSouthBids(game);
                    }

                    // bid is PASS
                    if (game.getContractType() == Constantes.CONTRACT_TYPE_PASS) {
                        updateAtEndGamePlayArgine(game);
                    }
                } else {
                    log.error("Failed to buildContract on game - game="+game);
                }
            }
            game.setLastDate(Calendar.getInstance().getTimeInMillis());
            tournamentMgr.updateGamePlayArgine(game);
            return true;
        }
        log.error("A PARAMETER IS NULL ! : bid="+bid);
        return false;
    }

    /**
     * Play the card and set the game data
     * @param table
     * @param game
     * @param card
     * @param session
     * @param eventListAfterMemoryUpdate
     * @param spreadResultArgine
     * @return true if all is correct else false
     */
    private boolean playValidCard(Table table, Game game, BridgeCard card, FBSession session, List<Event> eventListAfterMemoryUpdate, boolean spreadResultArgine) {
        if (table != null && game != null && card != null) {
            // game is finished => no update to do !
            if (game.isFinished()) {
                if (log.isDebugEnabled()) {
                    log.debug("Game is already finished !");
                }
                return true;
            }

            // block to check bid contract
            BridgeBid bidContract = game.getBidContract();
            if (bidContract == null || bidContract.isPass()) {
                log.warn("Bid contract is not valid - bidContract=" + bidContract + " - game=" + game);
                bidContract = game.getBidContract();
                if (bidContract == null || bidContract.isPass()) {
                    if (!game.buildContract()) {
                        log.error("Failed to reload bidContract - bidContract=" + bidContract + " - game=" + game);
                        return false;
                    } else {
                        log.warn("Success to reload contract on game="+game);
                    }
                } else {
                    log.warn("Success to reload bidContract - bidContract=" + bidContract + " - game=" + game);
                }
            }

            boolean beginOfTrick = GameBridgeRule.isBeginTrick(game.getListCard());

            if ((card.getOwner() == BridgeConstantes.POSITION_WEST || card.getOwner() == BridgeConstantes.POSITION_EAST) && !game.isSpreadResultArgine() && spreadResultArgine) {
                game.setSpreadResultArgine(spreadResultArgine);
            }

            if (beginOfTrick &&
                    spreadResultArgine &&
                    (card.getOwner() == BridgeConstantes.POSITION_WEST || card.getOwner() == BridgeConstantes.POSITION_EAST) &&
                    (FBConfiguration.getInstance().getIntValue("game.enableSpreadArgineBeginTrick", 1) == 1) &&
                    (FBConfiguration.getInstance().getIntValue("game.enableSpreadArgineBeginTrickBeforeAddCard", 1) == 1)) {
                if (spreadEnable &&
                        (game.getNbTricks() >= FBConfiguration.getInstance().getIntValue("game.nbTricksForSpread", 5)) &&
                        (game.getNbTricks() != 12) &&
                        (FBConfiguration.getInstance().getIntValue("game.enableSpreadArgine", 1) == 1)) {
                    // check player has not already refuse this spread !
                    if (game.getSpreadRefuseStep() != game.getStep()) {
                        addSpreadPlay(game.getIDStr(), new SpreadGameData(BridgeConstantes.POSITION_EAST, false, 1));
                        // a spread claim is possible. Send event to ask player agreement
                        session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_SPREAD, Character.toString(BridgeConstantes.POSITION_EAST), null);
                        return true;
                    }
                }
            }


            // the card is valid => add to the played cards
            game.addCard(card);

            // push event play card
            session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_CARD, card.toString() + card.getOwner(), null);

            // is it the end of trick ?
            if (GameBridgeRule.isEndTrick(game.getListCard())) {
                BridgeCard trickWinner = GameBridgeRule.getLastWinnerTrick(game.getListCard(), bidContract);
                // add winner to game
                game.addTrickWinner(trickWinner.getOwner(), FBConfiguration.getInstance().getIntValue("game.tricksWinnerHistoricEnable", 0) == 1);
                // push event end of bids
                session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_END_TRICK, Character.toString(trickWinner.getOwner()), null);

                // is it the end of game ?
                if (GameBridgeRule.isEndGame(game.getListCard())) {
                    updateAtEndGame(table, game, session, eventListAfterMemoryUpdate);
                } else {
                    // end of trick => test if spread can be claimed
                    if (spreadEnable &&
                            (game.getNbTricks() >= FBConfiguration.getInstance().getIntValue("game.nbTricksForSpread", 5)) &&
                            (game.getNbTricks() != 12)) {

                        boolean bClaim = false;
                        char claimPosition = BridgeConstantes.POSITION_NOT_VALID;
                        boolean computeClaim = false;
                        if (FBConfiguration.getInstance().getIntValue("game.enableSpreadArgine", 1) == 1) {
                            if (game.isSpreadResultArgine()) {
                                claimPosition = BridgeConstantes.POSITION_EAST;
                            } else {
                                computeClaim = true;
                            }
                        } else {
                            computeClaim = true;
                        }
                        if (computeClaim) {
                            BridgeGame bg = BridgeGame.create(game.getDeal().getString(),
                                    game.getListBid(),
                                    game.getListCard());

                            if (bg != null) {
                                claimPosition = GameBridgeRule.claimGame(bg);
                            } else {
                                log.error("Error to create BridgeGame : deal="+game.getDeal().getString()+" game="+game.toString());
                            }
                        }
                        if (claimPosition != BridgeConstantes.POSITION_NOT_VALID) {
                            addSpreadPlay(game.getIDStr(), new SpreadGameData(claimPosition, false, 1));
                            // a spread claim is possible. Send event to ask player agreement
                            session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_SPREAD, Character.toString(claimPosition), null);
                            bClaim = true;
                        }

                        if (!bClaim) {
                            // no spread claim .. continue normal game : send event current position
                            session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null);
                        }
                    } else {
                        // send event current player
                        session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(trickWinner.getOwner()), null);
                    }
                }
            }
            // Check spread on begin of trick if first card played is marked with spread flag to true
            else if (beginOfTrick &&
                    spreadResultArgine &&
                    (card.getOwner() == BridgeConstantes.POSITION_WEST || card.getOwner() == BridgeConstantes.POSITION_EAST) &&
                    (FBConfiguration.getInstance().getIntValue("game.enableSpreadArgineBeginTrick", 1) == 1) &&
                    (FBConfiguration.getInstance().getIntValue("game.enableSpreadArgineBeginTrickBeforeAddCard", 1) == 0)) {
                if (spreadEnable &&
                        (game.getNbTricks() >= FBConfiguration.getInstance().getIntValue("game.nbTricksForSpread", 5)) &&
                        (game.getNbTricks() != 12) &&
                        (FBConfiguration.getInstance().getIntValue("game.enableSpreadArgine", 1) == 1)) {
                    addSpreadPlay(game.getIDStr(), new SpreadGameData(BridgeConstantes.POSITION_EAST, false, 1));
                    // a spread claim is possible. Send event to ask player agreement
                    session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_SPREAD, Character.toString(BridgeConstantes.POSITION_EAST), null);
                } else {
                    // send event current player
                    session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null);
                }
            }
            // not end of trick
            else {
                // send event current player
                session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null);
            }
            game.setLastDate(System.currentTimeMillis());
            return true;
        }
        log.error("A PARAMETER IS NULL !");
        return false;
    }

    /**
     * Play the card and set the game data
     * @param game
     * @param card
     * @param spreadResultArgine
     * @return true if all is correct else false
     */
    private boolean playArgineValidCard(Game game, BridgeCard card, boolean spreadResultArgine) {
        if (game != null && card != null) {
            // game is finished => no update to do !
            if (game.isFinished()) {
                if (log.isDebugEnabled()) {
                    log.debug("Game is already finished !");
                }
                return true;
            }

            // block to check bid contract
            BridgeBid bidContract = game.getBidContract();
            if (bidContract == null || bidContract.isPass()) {
                log.error("Bid contract is not valid - bidContract=" + bidContract + " - game=" + game);
                bidContract = game.getBidContract();
                if (bidContract == null || bidContract.isPass()) {
                    if (!game.buildContract()) {
                        log.error("Failed to reload bidContract - bidContract=" + bidContract + " - game=" + game);
                        return false;
                    } else {
                        log.warn("Success to reload contract on game="+game);
                    }
                } else {
                    log.warn("Sucess to reload bidContract - bidContract=" + bidContract + " - game=" + game);
                }
            }

            // the card is valid => add to the played card
            game.addCard(card);

            if ((card.getOwner() == BridgeConstantes.POSITION_WEST || card.getOwner() == BridgeConstantes.POSITION_EAST) && !game.isSpreadResultArgine() && spreadResultArgine) {
                game.setSpreadResultArgine(spreadResultArgine);
            }

            // is it the end of trick ?
            if (GameBridgeRule.isEndTrick(game.getListCard())) {
                BridgeCard trickWinner = GameBridgeRule.getLastWinnerTrick(game.getListCard(), bidContract);
                // add winner to game
                game.addTrickWinner(trickWinner.getOwner(), FBConfiguration.getInstance().getIntValue("game.tricksWinnerHistoricEnable", 0) == 1);

                // is it the end of game ?
                if (GameBridgeRule.isEndGame(game.getListCard())) {
                    updateAtEndGamePlayArgine(game);
                } else {
                    // end of trick => test if spread can be claimed
                    if (spreadEnable &&
                            (game.getNbTricks() >= FBConfiguration.getInstance().getIntValue("game.nbTricksForSpread", 5)) &&
                            (game.getNbTricks() != 12)) {
                        char claimPosition = BridgeConstantes.POSITION_NOT_VALID;
                        boolean computeClaim = false;
                        if (FBConfiguration.getInstance().getIntValue("game.enableSpreadArgine", 1) == 1) {
                            if (game.isSpreadResultArgine()) {
                                claimPosition = BridgeConstantes.POSITION_EAST;
                            } else {
                                computeClaim = true;
                            }
                        } else {
                            computeClaim = true;
                        }
                        if (computeClaim) {
                            BridgeGame bg = BridgeGame.create(game.getDeal().getString(),
                                    game.getListBid(),
                                    game.getListCard());

                            if (bg != null) {
                                claimPosition = GameBridgeRule.claimGame(bg);
                            } else {
                                log.error("Error to create BridgeGame : deal="+game.getDeal().getString()+" game="+game.toString());
                            }
                        }
                        if (claimPosition != BridgeConstantes.POSITION_NOT_VALID) {
                            // claim !
                            game.claimAllForPlayer(claimPosition);
                            updateAtEndGamePlayArgine(game);
                            game.setLastDate(Calendar.getInstance().getTimeInMillis());
                        }
                    }
                }
            }
            game.setLastDate(System.currentTimeMillis());
            tournamentMgr.updateGamePlayArgine(game);
            return true;
        }
        log.error("A PARAMETER IS NULL !");
        return false;
    }

    /**
     * update the game and table object at the end of game. The game must be ended. No check is done !
     * @param table
     * @param game
     * @param session
     * @param eventListAfterMemoryUpdate event list to send after memory update
     */
    private void updateAtEndGame(Table table, Game game, FBSession session, List<Event> eventListAfterMemoryUpdate) {
        if (table != null && game != null) {
            removeSpreadPlay(game.getIDStr());
            game.setFinished(true);
            computeScore(game);
            if (FBConfiguration.getInstance().getIntValue("general.gameEventAfterMemoryUpdate", 1) == 1 && eventListAfterMemoryUpdate != null) {
                eventListAfterMemoryUpdate.add(createEvent(session.getPlayer().getID(), table, Constantes.EVENT_TYPE_GAME_END_GAME, ""+game.getScore(), null));
            } else {
                session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_END_GAME, "" + game.getScore(), null);
            }
            table.addPlayedDeal(game.getDealID());
            if (!game.isReplay()) {
                // is all deal of tournament played ?
                if (table.isAllTournamentDealPlayed()) {
                    if (FBConfiguration.getInstance().getIntValue("general.gameEventAfterMemoryUpdate", 1) == 1 && eventListAfterMemoryUpdate != null) {
                        eventListAfterMemoryUpdate.add(createEvent(session.getPlayer().getID(), table, Constantes.EVENT_TYPE_GAME_END_TOURNAMENT, null, null));
                    } else {
                        session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_END_TOURNAMENT, null, null);
                    }
                }
            }
        }
    }

    /**
     * update the game at the end of game. The game must be ended. No check is done !
     * @param game
     */
    private void updateAtEndGamePlayArgine(Game game) {
        if (game != null) {
            game.setFinished(true);
            computeScore(game);
        }
    }

    /**
     * Return the current time stamp. A sleep time of 1 ms is do to be sure to have different TS
     * @return
     */
    private long getTimeStamp() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
        }
        return System.currentTimeMillis();
    }

    /**
     * Create event with category game on this table
     * @param receiverID
     * @param table
     * @param type
     * @param typeData
     * @param events
     * @return
     */
    public Event createEvent(long receiverID, Table table, String type, String typeData, EventField[] events) {
        Event evt = new Event();
        evt.timestamp = getTimeStamp();
        evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
        evt.receiverID = receiverID;
        String gameStep = null;
        if (table.getGame() != null) {
            gameStep = ""+table.getGame().getStep();
        }
        evt.addFieldCategory(Constantes.EVENT_CATEGORY_GAME, gameStep);
        evt.addFieldType(type, typeData);
        evt.addField(new EventField(Constantes.EVENT_FIELD_TABLE_ID, ""+table.getID(), null));
        if (table.getGame() != null) {
            evt.addField(new EventField(Constantes.EVENT_FIELD_GAME_ID, ""+table.getGame().getIDStr(), null));
        }
        if (events != null) {
            for (int i = 0; i < events.length; i++) {
                evt.addField(events[i]);
            }
        }
        return evt;
    }

    /**
     * Compute the score on this game for player S
     * @param game
     */
    public static void computeScore(Game game) {
        if (game.isFinished()) {
            if (game.getBidContract().isPass()) {
                game.setScore(Constantes.GAME_SCORE_PASS);
                game.setTricks(0);
            } else {
                int contreValue = GameBridgeRule.isX2(game.getListBid())?2:(GameBridgeRule.isX1(game.getListBid())?1:0);
                int score = GameBridgeRule.getGameScore(game.getNbTricksWinByPlayerAndPartenaire(game.getDeclarer()),
                        game.getBidContract(),
                        contreValue,
                        game.getDeal().getVulnerability());
                if (!GameBridgeRule.isPartenaire(game.getBidContract().getOwner(), BridgeConstantes.POSITION_SOUTH) ) {
                    score = -score;
                }
                game.setScore(score);
                int nbTricks = 0;
                nbTricks = StringUtils.countMatches(game.getTricksWinner(), Character.toString(game.getDeclarer()));
                nbTricks += StringUtils.countMatches(game.getTricksWinner(), Character.toString(GameBridgeRule.getPositionPartenaire(game.getDeclarer())));
                game.setTricks(nbTricks);
            }
        }
    }

    /**
     * Play a bid. Check if the bid is valid and can be played.
     * @param session
     * @param bidStr The bid as string (2C or PA ...)
     * @param step
     * @return true if game is not finished and next player must played, false if game is finished
     * @throws FBWSException
     */
    public boolean playBid(FBSession session, String bidStr, int step) throws FBWSException {
        if (session == null) {
            log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Table table = session.getCurrentGameTable();
        if (table == null) {
            log.error("No table in session !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Game game = table.getGame();
        if (game == null) {
            log.error("Game is null for table="+table);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!game.isReplay()) {
            tournamentMgr.checkGame(game);
        }

        synchronized (getLockOnGame(game.getIDStr())) {
            if (FBConfiguration.getInstance().getIntValue("general.checkStep", 1) == 1 && (step != -1) && game.getStep() != step) {
                if (FBConfiguration.getInstance().getIntValue("general.checkStepException", 0) == 1) {
                    log.error("Game step is not the same ! gameStep=" + game.getStep() + " - param step=" + step + " - game=" + game + " - session=" + session);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                } else {
                    return !game.isFinished();
                }
            }
            long playerID = session.getPlayer().getID();
            char playerPosition = table.getPlayerPosition(playerID);
            // check current position
            if (game.getCurrentPlayer() != playerPosition) {
                log.error("CURRENT POSITION IS NOT PLAYER POSITION");
                throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
            }
            // check if a thread is not running for this game
            if (isThreadPlayRunning(game.getIDStr())) {
                log.warn("A thread is currently running for game=" + game+" - try to remove expired thread");
                if (!removeExpiredThreadPlayRunning(game.getIDStr())) {
                    log.error("Thread not yet expired ... game="+game);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            }

            // check bid value
            BridgeBid bid = BridgeBid.createBid(bidStr, playerPosition);
            if (bid == null) {
                log.error("BID UNKNOWN : " + bidStr + " - position : " + playerPosition);
                throw new FBWSException(FBExceptionType.GAME_BID_NOT_VALID);
            }

            // Check game
            if (game.isEndBid()) {
                log.error("BIDS ARE COMPLETED");
                throw new FBWSException(FBExceptionType.GAME_BID_NOT_VALID);
            }
            if (!GameBridgeRule.isBidValid(game.getListBid(), bid)) {
                log.error("THE BID PLAYED IS NOT AUTHORIZED - game=" + game + " - plaID=" + playerID + " - bid:" + bid.toString() + " - current bid played:" + game.getBidListStrWithoutPosition());
                throw new FBWSException(FBExceptionType.GAME_BID_NOT_VALID);
            }

            if ((bid.isX1() || bid.isX2()) && game.getListBid().size() == 0) {
                log.error("THE BID X1 or X2 IS NOT AUTHORIZED (Bid List empty !) - game=" + game + " - plaID=" + playerID + " - bid:" + bid.toString() + " - current bid played:" + game.getBidListStrWithoutPosition()+" - session="+session);
                throw new FBWSException(FBExceptionType.GAME_BID_NOT_VALID);
            }

            if (playerPosition == BridgeConstantes.POSITION_SOUTH) {
                String bidInfoSouth = game.getBidInfoForSouth(game.getBidListStrWithoutPosition() + bid.getString());
                if (bidInfoSouth != null && isAlertBidInfo(bidInfoSouth)) {
                    bid.setAlert(true);
                }
            }
            // Every is checked ! so play the bid !
            List<Event> eventList = new ArrayList<>();
            if (!playValidBid(table, game, bid, session, eventList)) {
                log.error("ERROR WHEN PLAYING BID");
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            if (eventList.size() > 0) {
                for (Event e : eventList) {
                    session.pushEvent(e);
                }
            }
            if (!game.isReplay()) {
                if (game.isFinished()) {
                    tournamentMgr.updateGameFinished(session);
                }
            }

            return !game.isFinished();
        }
    }

    /**
     * Play a card. Check if the card is valid and can be played.
     * @param session
     * @param cardStr
     * @return true if game is not finished and next player must played, false if game is finished
     * @throws FBWSException
     */
    public boolean playCard(FBSession session, String cardStr, int step) throws FBWSException {
        if (session == null) {
            log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Table table = session.getCurrentGameTable();
        if (table == null) {
            log.error("No table in session !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Game game = table.getGame();
        if (game == null) {
            log.error("Game is null for table="+table);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!game.isReplay()) {
            tournamentMgr.checkGame(game);
        }
        // check if a thread is not running for this game
        if (isThreadPlayRunning(game.getIDStr())) {
            log.warn("A thread is currently running for game=" + game);
            if (!removeExpiredThreadPlayRunning(game.getIDStr())) {
                log.error("Thread not yet expired ... game="+game);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        if (FBConfiguration.getInstance().getIntValue("general.checkStep", 1) == 1 && (step != -1) && game.getStep() != step) {
            if (FBConfiguration.getInstance().getIntValue("general.checkStepException", 0) == 1) {
                log.error("Game step is not the same ! gameStep=" + game.getStep() + " - param step=" + step + " - game=" + game);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            } else {
                return !game.isFinished();
            }
        }
        char playerPosition = table.getPlayerPosition(session.getLoginID());
        char currentPlayer = game.getCurrentPlayer();
        // check current position
        if (currentPlayer != playerPosition) {
            // Partner is playing : position is in declarer side and current player is a robot and player is partner of current player
            boolean bPartnerAuth = (GameBridgeRule.isPositionInDeclarerSide(playerPosition, game.getDeclarer()) && !table.isPlayerHumanAtPosition(playerPosition) && GameBridgeRule.isPartenaire(playerPosition, currentPlayer));
            if (!bPartnerAuth){
                log.error("CURRENT POSITION IS NOT PLAYER POSITION : current="+currentPlayer+" and not "+playerPosition+" - game="+game);
                throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
            }
        }

        // check if waiting for spread
        if (isSpreadPlay(game.getIDStr())) {
            log.error("A SPREAD IS WAITING FOR THIS GAME="+game);
            throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
        }

        // check card value
        BridgeCard card = BridgeCard.createCard(cardStr, currentPlayer);
        if (card == null) {
            log.error("CARD UNKNOWN : "+cardStr+" - position : "+currentPlayer);
            throw new FBWSException(FBExceptionType.GAME_CARD_NOT_VALID);
        }

        // Check game
        if (!game.isEndBid()) {
            log.error("BIDS ARE NOT COMPLETED");
            throw new FBWSException(FBExceptionType.GAME_PLAY_NOT_VALID);
        }
        if (game.isFinished()) {
            log.error("GAME IS ENDED");
            throw new FBWSException(FBExceptionType.GAME_PLAY_NOT_VALID);
        }
        if (!GameBridgeRule.isCardValid(game.getListCard(), card, GameBridgeRule.convertCardDealToList(game.getDeal().cards))) {
            log.error("THE CARD PLAYED IS NOT AUTHORIZED - game="+game+" - plaID="+session.getLoginID()+" - card:"+card.toString()+" - current card played:"+game.getCardListStrWithoutPosition()+" - distrib="+game.getDeal().cards);
            throw new FBWSException(FBExceptionType.GAME_CARD_NOT_VALID);
        }

        // Every is checked ! so play the card !
        List<Event> eventList = new ArrayList<>();
        if (!playValidCard(table, game, card, session, eventList, false)) {
            log.error("ERROR WHEN PLAYING CARD");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (eventList.size() > 0) {
            for (Event e : eventList) {
                session.pushEvent(e);
            }
        }
        if (!game.isReplay()) {
            if (game.isFinished()) {
                tournamentMgr.updateGameFinished(session);
            }
        }
        return !game.isFinished();
    }

    /**
     * Send request to get PAR data on deal
     * @param game
     * @return
     */
    public String getPar(Game game) {
        if (log.isDebugEnabled()) {
            log.debug("Begin getPar - game="+game);
        }
        String parData = null;
        if (game != null) {
            try {
                synchronized (lockParDeal.getLock(game.getDealID())) {
                    Tournament tour = tournamentMgr.getTournament(game.getTournament().getIDStr());
                    if (tour != null) {
                        Deal deal = tour.getDeal(game.getDealID());
                        if (deal != null) {
                            if (deal.getEngineParInfo() == null || deal.getEngineParInfo().length() == 0) {
                                BridgeEngineParam engineParam = BridgeEngineParam.createParamPar(game.getDeal().getDealer(),
                                        game.getDeal().getVulnerability(),
                                        game.getDeal().getCards(),
                                        game.getTournament().getResultType(),
                                        getArgineEngineMgr().getEngineVersion(game.getTournament().getCategory()));

                                if (!isPlaySynchroMethod()) {
                                    PlayGameStepData stepData = new PlayGameStepData();
                                    stepData.asyncID = PlayGameStepData.buildAsyncID(deal.getDealID(tour.getIDStr()));
                                    stepData.gameMgr = this;
                                    stepData.sessionID = null;
                                    stepData.timestamp = System.currentTimeMillis();
                                    stepData.param = engineParam.toString();
                                    stepData.requestType = Constantes.ENGINE_REQUEST_TYPE_PAR;
                                    engineParam.setAsyncID(stepData.asyncID);
                                    if (isEngineWebSocketEnable()) {
                                        if (engineParam.getAsyncID() != null) {
                                            engineParam.setUseWebsocket(true);
                                        }
                                    }
                                    getEngineService().putStepData(stepData);
                                }

                                if (log.isDebugEnabled()) {
                                    log.debug("Call engine.getPar - engineParam="+engineParam);
                                }

                                BridgeEngineResult result = engine.getPar(engineParam);
                                if (result.isError()) {
                                    log.error("Error on result : " + result.getContent() + " - deal=" + game.getDeal() + " - engineVersion=" + game.getEngineVersion());
                                }
                                else {
                                    parData = result.getContent();
                                }

                                if (parData != null && !parData.equals("ASYNC")) {
                                    if (parData.length() == 0 || parData.equals(Constantes.ENGINE_RESULT_NULL)) {
                                        parData = Constantes.ENGINE_RESULT_EMPTY;
                                    }
                                    // save pare result in deal
                                    deal.setEngineParInfo(parData);
                                    try {
                                        tournamentMgr.updateTournamentDB(tour);
                                    } catch (Exception e) {
                                        log.error("Failed to update tour=" + tour, e);
                                    }
                                }
                            } else {
                                // PAR already present
                                parData = deal.getEngineParInfo();
                            }
                        } else {
                            log.error("Deal not found with ID="+game.getDealID()+" in tournament="+tour+" - game="+game);
                        }
                    } else {
                        log.error("Tourmanent is null with ID="+game.getTournament().getIDStr()+" - game="+game);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to get PAR for game="+game, e);
            }
        } else {
            log.error("Game is null !");
        }
        return parData;
    }

    /**
     * The player leave the game.
     * @param session
     * @return
     * @throws FBWSException
     */
    public boolean leaveGame(FBSession session) throws FBWSException {
        if (session == null) {
            log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Table table = session.getCurrentGameTable();
        if (table == null) {
            log.error("No table found in session="+session);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Game game = table.getGame();
        if (game == null) {
            log.error("Game is null for table="+table);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // no leave for replay
        if (game.isReplay()) {
            log.error("No leave for replay - game="+game);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        tournamentMgr.checkGame(game);
        synchronized (getLockOnGame(game.getIDStr())) {
            if (game.getBidContract() == null) {
                // no contract => leave
                game.setLeaveValue();
            } else {
                // contract exist => claim with 0 tricks
                game.claimForPlayer(table.getPlayerPosition(session.getPlayer().getID()), 0);
                game.setFinished(true);
                game.setLastDate(Calendar.getInstance().getTimeInMillis());
                // compute score
                computeScore(game);
            }
            table.addPlayedDeal(game.getDealID());
            // is all deal of tournament played ?
            if (table.isAllTournamentDealPlayed()) {
                session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_END_TOURNAMENT, null, null);
            }
            if (game.isFinished()) {
                tournamentMgr.updateGameFinished(session);
            }
        }
        return true;
    }

    /**
     * Return a string info for this bid according to the current game
     * @param session
     * @param bids
     * @return
     * @throws FBWSException
     */
    public String getBidInfo(FBSession session, String bids) throws FBWSException {
        if (session == null) {
            log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Table table = session.getCurrentGameTable();
        if (table == null) {
            log.error("No table found in session="+session);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Game game = table.getGame();
        if (game == null) {
            log.error("Game is null for table="+table);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!game.isReplay()) {
            tournamentMgr.checkGame(game);
        }

        BridgeEngineParam paramEngine = null;
        List<BridgeBid> listBid = null;
        String bidList = "";
        String info = "";
        String gameID = game.getIDStr();

        synchronized (getLockOnGame(game.getIDStr())) {
            try {
                if (bids != null && bids.length() > 0) {
                    String[] temp = bids.split("-");
                    for (int i = 0; i < temp.length; i++) {
                        bidList += temp[i].substring(0, 2);
                    }
                }
            } catch (Exception e) {
                log.error("Error to parse bids=" + bids + " - session=" + session.getInfo());
                throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
            }

            listBid = GameBridgeRule.convertPlayBidsStringToList(bidList, game.getDeal().dealer);
            if (listBid == null) {
                log.error("Bids sequence not validd ! bids=" + bids + " - bidList=" + bidList + " - session=" + session.getInfo());
                throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
            }

            // if bids sequence not valid => no bid info !
            if (!GameBridgeRule.isBidsSequenceValid(listBid)) {
                log.error("Bids sequence is not valid ! bids=" + bids + " - bidList=" + bidList + " - session=" + session.getInfo());
                throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
            }

            boolean bFixBUG4PA = false;
            if (FBConfiguration.getInstance().getIntValue("game.fixBug4PA", 1) == 1) {
                if (bidList.equals("PAPAPAPA")) {
                    bFixBUG4PA = true;
                    log.warn("BUG 4PA => Exception - bids=" + bids + " - session=" + session);
                    info = "0;13;0;13;0;13;0;13;0;40;0;40;#faible;0";
                }
            }
            if (!bFixBUG4PA) {
                paramEngine = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_BID_INFO, true);
                paramEngine.setCardList("");
                paramEngine.setBidList(bidList);
            }
        }// end synchro

        if (paramEngine != null) {
            BridgeEngineResult result = engine.getBidInformation(paramEngine);
            if (result.isError()) {
                log.error("Error on result : " + result.getContent());
            } else {
                info = result.getContent();
            }
            log.debug("Bid info=" + info);
            if (info == null || info.length() == 0 || info.equals(Constantes.ENGINE_RESULT_NULL)) {
                log.error("Info for bid not valid : info=" + info + " - game=" + game + " - bids=" + bids+" - param="+paramEngine+" - result="+result);
                throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
            }
        }

        table = session.getCurrentGameTable();
        if (table == null) {
            log.error("No table found in session="+session);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        game = table.getGame();
        if (game == null) {
            log.error("Game is null for table="+table);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (game != null && gameID.equals(game.getIDStr())) {
            synchronized (getLockOnGame(game.getIDStr())) {
                // bid info for SUD => save info to analyze bid
                if (listBid.size() > 0 /*&& !game.isReplay()*/) {
                    BridgeBid lastBid = listBid.get(listBid.size() - 1);
                    if (lastBid.getOwner() == BridgeConstantes.POSITION_SOUTH && !game.isAnalyzeSouthBidDone()) {
                        game.addBidInfoSouth(bidList, info);

                        // if bid is alert => update the bid played from the bid list of the game
                        if (isAlertBidInfo(info)) {
                            // check if bid exists in bids played
                            BridgeBid bid = game.getBidPlayForSequence(bidList);
                            if (bid != null &&
                                    bid.getOwner() == BridgeConstantes.POSITION_SOUTH &&
                                    bid.getString().equals(lastBid.getString()) &&
                                    !bid.isAlert()) {
                                bid.setAlert(true);
                                game.generateBidsField();
                            }
                        }
                        if (game.isEndBid()) {
                            // bids finished => start process analyze
                            processAnalyzeForSouthBids(game);
                        }
                    }
                }
            }
        }
        return info;
    }

    /**
     * Receive response for claim spread for this game
     * @param session
     * @param response
     * @return true if game is not finished and next player must played, false if game is finished
     * @throws FBWSException
     */
    public boolean setClaimSpreadResponse(FBSession session, boolean response) throws FBWSException{
        if (session == null) {
            log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Table table = session.getCurrentGameTable();
        if (table == null) {
            log.error("No table found in session="+session);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Game game = table.getGame();
        if (game == null) {
            log.error("Game is null for table="+table);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!game.isReplay()) {
            getTournamentMgr().checkGame(game);
        }

        boolean bPlayNextPlayer = false;
        // check spread data status
        SpreadGameData spreadData = getSpreadPlay(game.getIDStr());
        if (spreadData == null) {
            log.error("No spread data waiting for game="+game);
            throw new FBWSException(FBExceptionType.GAME_NO_CLAIM_TO_SPREAD);
        }
        if (spreadData.isAllResponseReceived()) {
            log.error("All response received for this spread data for game="+game);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        // update response for spread data
        spreadData.incrementResponseReceived();
        if (response) {
            spreadData.incrementResponseOK();
        } else {
            game.setSpreadRefuseStep(game.getStep());
        }
        if (spreadData.isAllResponseReceived()) {
            if (spreadData.isAllResponseOK()) {
                // accept to spread
                game.claimAllForPlayer(spreadData.getRequester());
                List<Event> eventList = new ArrayList<>();
                updateAtEndGame(table, game, session, eventList);
                game.setLastDate(Calendar.getInstance().getTimeInMillis());
                if (eventList.size() > 0) {
                    for (Event e : eventList) {
                        session.pushEvent(e);
                    }
                }
                if (!game.isReplay()) {
                    if (game.isFinished()) {
                        tournamentMgr.updateGameFinished(session);
                    }
                }
                bPlayNextPlayer = false;
            } else {
                // remove game from list spread waiting
                removeSpreadPlay(game.getIDStr());

                // claim spread is refused => continue normal play : send event current position
                session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null);
                bPlayNextPlayer = true;
            }
        }
        return bPlayNextPlayer;
    }

    /**
     * Check claim is possible for game in session with nb tricks
     * @param session
     * @param nbTricks
     * @param step
     * @return
     * @throws FBWSException
     */
    public boolean checkClaim(FBSession session, int nbTricks, int step) throws FBWSException {
        if (session == null) {
            log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Table table = session.getCurrentGameTable();
        if (table == null) {
            log.error("No table found in session="+session);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        Game game = table.getGame();
        if (game == null) {
            log.error("Game is null for table="+table);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!game.isReplay()) {
            tournamentMgr.checkGame(game);
        }
        if (step != -1) {
            if (game.getStep() != step) {
                log.warn("Step not the same ! game="+game+" - param step="+step+" - nbTricks="+nbTricks);
                return false;
            }
        }
        char currentPlayer = game.getCurrentPlayer();
        char playerPosition = table.getPlayerPosition(session.getLoginID());
        // check current position
        if (currentPlayer != playerPosition) {
            // Partner is playing : position is in declarer side and current player is a robot and player is partner of current player
            if (!GameBridgeRule.isPartenaire(playerPosition, currentPlayer)){
                log.error("CURRENT POSITION IS NOT PLAYER POSITION : current="+currentPlayer+" and not "+playerPosition+" - game="+game);
                throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
            }
        }

        // check if a thread is not running for this game
        if (isThreadPlayRunning(game.getIDStr())) {
            log.warn("A thread is currently running for game="+game);
            if (!removeExpiredThreadPlayRunning(game.getIDStr())) {
                log.error("Thread not yet expired ... game="+game);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }

        // check if waiting for spread
        if (isSpreadPlay(game.getIDStr())) {
            log.error("A SPREAD IS WAITING FOR THIS GAME="+game);
            throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
        }

        if (game.isFinished()) {
            log.error("GAME IS ENDED");
            throw new FBWSException(FBExceptionType.GAME_PLAY_NOT_VALID);
        }
        boolean result = false;
        if (nbTricks < 0 || nbTricks > 13) {
            log.error("nbTricks not valid ("+nbTricks+") game="+game.toString());
        } else if (nbTricks == 0) {
            log.debug("Claim always possible for nbTricks=0");
            result = true;
        } else {
            BridgeGame bg = BridgeGame.create(game.getDeal().getString(), game.getListBid(), game.getListCard());
            if (bg != null) {
                if (FBConfiguration.getInstance().getIntValue("game.checkClaimArgine", 1) == 1) {
                    // check game phase card and begin trick
                    if (!bg.isPhaseCard()) {
                        log.error("Not phase card - game="+bg);
                        return false;
                    }
                    char declarer = bg.getDeclarer();
                    BridgeBid contract = bg.getContract();
                    if (declarer == BridgeConstantes.POSITION_NOT_VALID || contract == null) {
                        log.error("Declarer or contract not valid for game=" + bg);
                        return false;
                    }
                    int nbTricksThreshold = FBConfiguration.getInstance().getIntValue("game.claimArgineNbTricksThreshold", 0);
                    if (nbTricksThreshold > 0 && game.getNbTricks() < nbTricksThreshold) {
                        return false;
                    }

                    BridgeEngineParam param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_CLAIM, true);
                    param.setNbTricksForClaim(nbTricks);
                    param.setClaimPlayer(BridgeConstantes.POSITION_SOUTH);
                    BridgeEngineResult engineResult = engine.checkClaim(param);
                    if (engineResult != null && !engineResult.isError()) {
                        result = engineResult.getContent().equals("1");
                    } else {
                        log.error("Engine result not valid for claim with param="+param+" - engineResult="+engineResult);
                    }
                } else {
                    result = checkClaimGame(bg, nbTricks);
                }
            }
        }
        if (result) {
            // accept to claim
            game.claimForPlayer(playerPosition, nbTricks);
            List<Event> eventList = new ArrayList<>();
            updateAtEndGame(table, game, session, eventList);
            game.setLastDate(Calendar.getInstance().getTimeInMillis());
            if (eventList.size() > 0) {
                for (Event e : eventList) {
                    session.pushEvent(e);
                }
            }
            if (!game.isReplay()) {
                if (game.isFinished()) {
                    tournamentMgr.updateGameFinished(session);
                }
            }
        }
        return result;
    }

    /**
     * Check claim on bridge game with nbtricks
     * @param bg
     * @param nbTricks
     * @return
     */
    public boolean checkClaimGame(BridgeGame bg, int nbTricks) {
        if (bg == null) {
            log.error("BridgeGame is null !");
            return false;
        }

        try {
            // check game phase card and begin trick
            if (!bg.isPhaseCard() || !bg.isBeginTrick()) {
                log.error("Not phase card or not begin of trick - game="+bg);
                return false;
            }
            char declarer = bg.getDeclarer();
            BridgeBid contract = bg.getContract();
            if (declarer == BridgeConstantes.POSITION_NOT_VALID || contract == null) {
                log.error("Declarer or contract not valid for game="+bg);
                return false;
            }
            // check declarer ... must be N or S
            if (declarer != BridgeConstantes.POSITION_SOUTH && declarer != BridgeConstantes.POSITION_NORTH) {
                log.error("Declarer is not N or S game="+bg);
                return false;
            }

            if (nbTricks == 0) {
                log.debug("Claim always possible for nbTricks=0");
                return true;
            }
            int nbTricksRemaining = GameBridgeRule.getNbTrickRemaining(bg);
            if (nbTricks > nbTricksRemaining) {
                log.error("nbTricks > nbTricksRemaining ("+nbTricks+" > "+nbTricksRemaining+") game="+bg);
                return false;
            }

            if (FBConfiguration.getInstance().getIntValue("general.claimThomas", 0) == 0) {
                // check no contract color in E or W hand
                if (contract.getColor() != BidColor.NoTrump) {
                    CardColor colorContract = GameBridgeRule.bidColor2CardColor(contract.getColor());
                    if (colorContract != null) {
                        int nbCardColorE = bg.getNbCardForColor(BridgeConstantes.POSITION_EAST, colorContract);
                        int nbCardColorW = bg.getNbCardForColor(BridgeConstantes.POSITION_WEST, colorContract);
                        if (nbCardColorE > 0 || nbCardColorW > 0) {
                            if (log.isDebugEnabled()) {
                                log.debug("Nb contract color card for W=" + nbCardColorW + " - E=" + nbCardColorE + " - not 0 => claim not possible");
                            }
                            return false;
                        }
                    }
                }
            }

            // compute nb max tricks to claim
            int nbTrickMaxClaim = 0;
            if (FBConfiguration.getInstance().getIntValue("general.claimThomas", 0) == 1) {
                nbTrickMaxClaim = GameClaimThomas.getTotalNbTricks(bg);
            } else {
                nbTrickMaxClaim = GameClaim.getTotalNbTricks(bg);
            }
            return nbTricks <= nbTrickMaxClaim;
        } catch (Exception e) {
            log.error("Exception to check claim for game="+bg, e);
        }
        return false;
    }

    public void processAnalyzeForSouthBids(Game game) {
        if (!game.isAnalyzeSouthBidDone()) {
            if (tournamentMgr.isBidAnalyzeEnable()) {
                if (game.getAnalyzeBid() == null || game.getAnalyzeBid().length() == 0) {
                    game.setAnalyzeBid(getArgineEngineMgr().analyzeGameBid(
                            game.getDeal().getCards(),
                            game.getDeal().getDealer(),
                            game.getDeal().getVulnerability(),
                            game.getListBid(),
                            game.getMapBidInfoSouth(),
                            game.getConventionProfile(),
                            game.getConventionData(),
                            game.getTournament().getResultType(),
                            argineEngineMgr.getEngineVersion(game.getTournament().getCategory()),
                            engine
                    ));
                }
            }
            // parse bid info for south to check alert
            game.scanBidAlertForSouth();
            // clear map bid info
            game.clearBidInfoSouth();

            game.setAnalyzeSouthBidDone(true);
        }
    }

    public static boolean isAlertBidInfo(String bidInfo) {
        String[] dataReglette = bidInfo.split(";");
        if (dataReglette.length == 14) {
            int forcingValue = 0;
            try {
                forcingValue = Integer.parseInt(dataReglette[13]);
            } catch (Exception e) {
            }
            // bid alert => update the bid in the list
            return forcingValue >= 20;
        }
        return false;
    }

    /**
     * check in config if websocket for engine is enable
     * @return
     */
    public boolean isEngineWebSocketEnable() {
        if (FBConfiguration.getInstance().getIntValue("game.engineWebSocket", 1) == 1) {
             return tournamentMgr.getConfigIntValue("engineWebSocket", 1) == 1;
        } else {
            return tournamentMgr.getConfigIntValue("engineWebSocket", 0) == 1;
        }
    }

    /**
     * check in config if playSynchroMethod is enable
     * @return
     */
    public boolean isPlaySynchroMethod() {
        if (FBConfiguration.getInstance().getIntValue("game.playSynchroMethod", 0) == 1) {
            return tournamentMgr.getConfigIntValue("playSynchroMethod", 1) == 1;
        } else {
            return tournamentMgr.getConfigIntValue("playSynchroMethod", 0) == 1;
        }
    }

    /**
     * Process answer for advice => send event with result
     * @param stepData
     * @param result
     */
    public void answerAdvice(PlayGameStepData stepData, String result) {
        if (log.isDebugEnabled()) {
            log.debug("stepData="+stepData+" - result="+result);
        }

        if (stepData != null) {
            FBSession session = (FBSession) (ContextManager.getPresenceMgr().getSession(stepData.sessionID));
            if (session != null) {
                //-----------------------------------------------
                // TREAT RESPONSE
                Table table = session.getCurrentGameTable();
                Game game = table.getGame();
                String gameID = stepData.getDataID();
                if (game != null && gameID != null && gameID.equals(game.getIDStr())) {
                    if (result == null) {
                        result = "";
                    }
                    session.pushEventGame(table, Constantes.EVENT_TYPE_GAME_ADVICE, result, null);
                } else {
                    log.error("No game found in session - stepData=" + stepData + " - session=" + session);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("No session found for stepData=" + stepData);
                }
            }
        } else {
            log.error("Step Data is null !");
        }
    }

    /**
     * PlayArgine Step1 : call engine server
     * @param game
     * @return
     * @throws Exception
     */
    private boolean _playArgineGameStep1(Game game) throws Exception {
        if (game == null) {
            log.error("Parameter game in table is null !");
            throw new Exception("Parameter game in table is null !");
        }
        BridgeEngineParam param = null;
        boolean bReqBid = false;
        boolean bReqMotor = false;

        try {
            //-----------------------------------------
            // PREPARE REQUEST MOTOR
            synchronized (getLockOnGame(game.getIDStr())) {
                if (isRobotToPlayArgine(game)) {
                    bReqBid = !game.isEndBid();
                    // BIDS
                    if (bReqBid) {
                        String nextBidDeal = game.getDeal().getNextBid(game.bids);
                        // anticipation => bid set on deal (TDC)
                        if (nextBidDeal != null) {
                            processResultPlayArgine(game, nextBidDeal, null);
                            return isRobotToPlayArgine(game);
                        } else {
                            bReqMotor = true;
                            param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_BID, false);
                        }
                    }
                    // CARDS
                    else {
                        if (!game.isFinished()) {
                            // anticipation => card set on deal (TDC)
                            String nextCardDeal = game.getDeal().getNextCard(game.bids, game.cards);
                            if (nextCardDeal != null) {
                                processResultPlayArgine(game, nextCardDeal, null);
                                return isRobotToPlayArgine(game);
                            }
                            else {
                                BridgeCard card = null;
                                // OPTIMISATIONS - Last trick - simply play last card
                                if (game.getNbTricks() == 12) {
                                    // IF LAST TRICK, PLAY THE LAST CARD !
                                    card = GameBridgeRule.getLastCardForPlayer(game.getListCard(),
                                            GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                            game.getCurrentPlayer());
                                    if (card == null) {
                                        log.error("Last trick : No card found for player=" + game.getCurrentPlayer() + " - game=" + game.toString());
                                    }
                                }
                                // PLAYER HAS ONLY ONE CARD IN THIS COLOR => PLAY IT !
                                else {
                                    // not at begin trick !
                                    if (!GameBridgeRule.isBeginTrick(game.getListCard())) {
                                        BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                                        if (firstcardTrick != null) {
                                            card = GameBridgeRule.getOnlyOneCardForPlayerAndColor(game.getListCard(),
                                                    GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                                    game.getCurrentPlayer(), firstcardTrick.getColor());
                                        }
                                    }
                                }
                                if (card == null) {
                                    bReqMotor = true;
                                    param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_CARD, false);
                                } else {

                                    if (!playArgineValidCard(game, card, false)) {
                                        throw new Exception("Method playValidCard return false !");
                                    }

                                    if (!game.isReplay()) {
                                        if (game.isFinished()) {
                                            tournamentMgr.updateGamePlayArgineFinished(game);
                                        }
                                    }
                                    return isRobotToPlayArgine(game);
                                }
                            }
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("It is not robot to play : position=" + game.getCurrentPlayer());
                    }
                    return false;
                }
            }// end of synchornization

            //----------------------------------------------
            // SEND REQUEST TO MOTOR
            if (bReqMotor && param != null) {
                String operation = "";
                PlayGameStepData stepData = new PlayGameStepData();
                if (param.getAsyncID() != null) {
                    stepData.asyncID = param.getAsyncID();
                } else {
                    stepData.asyncID = PlayGameStepData.buildAsyncID(game.getIDStr(), game.getStep());
                    param.setAsyncID(stepData.asyncID);
                }
                if (isEngineWebSocketEnable()) {
                    if (param.getAsyncID() != null) {
                        param.setUseWebsocket(true);
                    }
                }
                stepData.gameMgr = this;
                stepData.playArgine = true;
                stepData.step = game.getStep();
                stepData.timestamp = System.currentTimeMillis();
                stepData.param = param.toString();
                stepData.requestType = bReqBid ? Constantes.ENGINE_REQUEST_TYPE_BID : Constantes.ENGINE_REQUEST_TYPE_CARD;
                getEngineService().putStepData(stepData);
                BridgeEngineResult engineResult = null;
                if (bReqBid) {
                    operation = "nextBid";
                    engineResult = engine.getNextBid(param);
                } else {
                    operation = "nextCard";
                    engineResult = engine.getNextCard(param);
                }

                if (log.isDebugEnabled()) {
                    log.debug("engineResult="+engineResult);
                }
                // response from engine server != ASYNC => result found from cache => process result
                if (engineResult != null) {
                    if (!engineResult.isError()) {
                        if (!engineResult.getContent().equals("ASYNC")) {
                            // result found in cache !
                            processResultPlayArgine(game, engineResult.getContent(), param.toString());
                            if (!game.isReplay()) {
                                if (game.isFinished()) {
                                    tournamentMgr.updateGamePlayArgineFinished(game);
                                }
                            }
                            getEngineService().removeStepData(stepData.asyncID);
                            return isRobotToPlayArgine(game);
                        }
                    } else {
                        log.error("Error from engine - engineResult="+engineResult);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Exception on thread playRobotGame - game=" + game, e);
        }
        return false;
    }

    /**
     * Update game with result from engine
     * @param game
     * @param result
     * @param param
     * @throws Exception
     */
    private void processResultPlayArgine(Game game, String result, String param) throws Exception {
        // Process BID
        if (!game.isEndBid()) {
            // result can contains many bids (anticipation)
            if (result == null) {
                if (tournamentMgr.getConfigIntValue("gameThreadBidNotValidPlayPA", 0) == 1) {
                    result = "PA";
                    log.error("getNexBid not valid result=" + result + " - game=" + game + "  => play PA - param=" + param);
                } else {
                    throw new Exception("Bid computed by engine not valid ! result=" + result + " - game=" + game + " - engineParam=" + param);
                }
            }

            int idxRes = 0;
            boolean bContinue = true;
            // loop on each bid in anticipation
            while ((idxRes + 1) < result.length() && bContinue) {
                String tempBid = result.substring(idxRes, idxRes + 2);
                idxRes += 2;
                boolean bidAlert = false;
                if (idxRes < result.length() && result.charAt(idxRes) == 'a') {
                    bidAlert = true;
                    idxRes++;
                }
                BridgeBid bid = BridgeBid.createBid(tempBid, game.getCurrentPlayer(), bidAlert);
                if (bid == null || !GameBridgeRule.isBidValid(game.getListBid(), bid)) {
                    if (tournamentMgr.getConfigIntValue("gameThreadBidNotValidPlayPA", 0) == 1) {
                        log.error("Bid computed by engine not valid - result=" + tempBid + " - bid=" + bid + " - game=" + game + " - engineParam=" + param + " -> Play PA");
                        bid = BridgeBid.createBid("PA", game.getCurrentPlayer());
                        bContinue = false;
                    } else {
                        throw new Exception("Bid computed by engine not valid ! bid=" + bid + " - tempBid=" + tempBid + " - result=" + result + " - game=" + game + " - engineParam=" + param);
                    }
                }
                if (!playArgineValidBid(game, bid)) {
                    throw new Exception("Method playValidBid return false !");
                }
                // check it is always robot to play
                if (game.isEndBid() || !isRobotToPlayArgine(game)) {
                    bContinue = false;
                }
            }
        }
        // Process CARD
        else {
            BridgeCard card = null;
            // result can contains many cards (anticipation)
            if (result == null) {
                if (tournamentMgr.getConfigIntValue("gameThreadCardNotValidPlaySmall", 0) == 1) {
                    // error => play the smallest card !
                    BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                    card = GameBridgeRule.getSmallestCardForPlayerAndColor(game.getListCard(),
                            GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                            game.getCurrentPlayer(),
                            (firstcardTrick != null ? firstcardTrick.getColor() : null));
                    log.error("getNextCard not valid result=" + result + " - game=" + game + "  => play smallest card=" + card + " - engineParam=" + param);
                } else {
                    throw new Exception("Card computed by engine not valid ! card=" + card + " - result=" + result + " - game=" + game + " - engineParam=" + param);
                }
                if (!playArgineValidCard(game, card, false)) {
                    throw new Exception("Method playValidCard return false !");
                }
            } else {
                int idxRes = 0;
                boolean bContinue = true;
                // loop on each card in anticipation
                while (idxRes < result.length() && bContinue) {
                    String tempCard = result.substring(idxRes, idxRes + 2);
                    card = BridgeCard.createCard(tempCard, game.getCurrentPlayer());
                    List<BridgeCard> listCardDeal = GameBridgeRule.convertCardDealToList(game.getDeal().getCards());
                    if (card == null || !GameBridgeRule.isCardValid(game.getListCard(), card, listCardDeal)) {
                        game.setEngineFailed(true);
                        log.error("Failed isCardValid => game=["+game+"] - tricksWinner="+game.getTricksWinner()+" - listCard=["+ StringTools.listToString(game.getListCard())+"] - card="+card);
                        log.error("TricksWinnerHistoric="+game.getTricksWinnerHistoric());
                        if (tournamentMgr.getConfigIntValue("gameThreadCardNotValidPlaySmall", 0) == 1) {
                            // error => play the smallest card !
                            BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                            card = GameBridgeRule.getSmallestCardForPlayerAndColor(game.getListCard(),
                                    GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                    game.getCurrentPlayer(),
                                    (firstcardTrick != null ? firstcardTrick.getColor() : null));
                            log.error("Card computed by engine not valid - result=" + tempCard + " - game=" + game + " - engineParam=" + param + " => play smallest card=" + card);
                            bContinue = false;
                        } else {
                            throw new Exception("Card computed by engine not valid ! card=" + card + " - tempCard=" + tempCard + " - result=" + result + " - game=[" + game + "] - engineParam=[" + param+"]");
                        }
                    }
                    idxRes += 2;
                    boolean spreadResultArgine = false;
                    if (result.length() > idxRes && result.charAt(idxRes) == 's') {
                        spreadResultArgine = true;
                        idxRes++;
                    }
                    if (!playArgineValidCard(game, card, spreadResultArgine)) {
                        throw new Exception("Method playValidCard return false !");
                    }
                    // check it is always robot to play
                    if (game.isFinished() || !isRobotToPlayArgine(game)) {
                        bContinue = false;
                    }
                }
            }
            // TODO update data of game in progress on tournamentMgr
        }
    }

    /**
     * The core method called by thread play. Run next bid or card.
     * @param game
     * @param gameThread
     * @return
     * @throws Exception
     */
    public boolean playArgineGameThread(Game game, GameThreadArgine gameThread) throws Exception {
        if (ContextManager.getPresenceMgr().isServiceMaintenance()) {
            log.warn("Service is in maintenance !");
            return false;
        }
        boolean bNextPlayerEngine = false;
        if (game == null) {
            log.error("Parameter game in session is null !");
            return false;
        }
        BridgeEngineParam param = null;
        BridgeEngineResult result = null;
        boolean bReqBid = false;
        String resContent = null;
        boolean bReqMotor = false;
        String gameID = "";
        int gameStep = 0;

        try {
            //-----------------------------------------
            // PREPARE REQUEST MOTOR
            synchronized (getLockOnGame(game.getIDStr())) {
                if (gameThread.isInterrupt()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Interrupt current thread ! " + gameThread.getThreadName());
                    }
                    return false;
                }
                gameID = game.getIDStr();
                gameStep = game.getStep();
                bNextPlayerEngine = isRobotToPlayArgine(game);
                if (bNextPlayerEngine) {
                    bNextPlayerEngine = false;
                    bReqBid = !game.isEndBid();
                    // BIDS
                    if (bReqBid) {
                        bReqMotor = true;
                        param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_BID, true);
                    }
                    // CARDS
                    else {
                        if (!game.isFinished()) {
                            BridgeCard card = null;
                            // OPTIMISATIONS - Last trick - simply play last card
                            if (game.getNbTricks() == 12) {
                                // IF LAST TRICK, PLAY THE LAST CARD !
                                card = GameBridgeRule.getLastCardForPlayer(game.getListCard(),
                                        GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                        game.getCurrentPlayer());
                                if (card == null) {
                                    log.error("Last trick : No card found for player="+game.getCurrentPlayer()+" - game="+game.toString());
                                }
                            }
                            // PLAYER HAS ONLY ONE CARD IN THIS COLOR => PLAY IT !
                            else {
                                // not at begin trick !
                                if (!GameBridgeRule.isBeginTrick(game.getListCard())) {
                                    BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                                    if (firstcardTrick != null) {
                                        card = GameBridgeRule.getOnlyOneCardForPlayerAndColor(game.getListCard(),
                                                GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                                game.getCurrentPlayer(), firstcardTrick.getColor());
                                    }
                                }
                            }
                            if (card == null) {
                                bReqMotor = true;
                                param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_CARD, true);
                            } else {
                                if (!playArgineValidCard(game, card, false)) {throw new Exception("Method playValidCard return false !");}
                                return isRobotToPlayArgine(game);
                            }
                        }
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("It is not robot to play : position=" + game.getCurrentPlayer());
                    }
                    return false;
                }
            }// end of synchornization

            //----------------------------------------------
            // SEND REQUEST TO MOTOR
            if (gameThread.isInterrupt()) {
                if (log.isDebugEnabled()) {
                    log.debug("Interrupt current thread ! " + gameThread.getThreadName());
                }
                return false;
            }
            if (bReqMotor && param != null) {
                String operation = "";
                if (bReqBid) {
                    operation = "nextBid";
                    String nextBidDeal = game.getDeal().getNextBid(game.bids);
                    if (nextBidDeal != null) {
                        result = new BridgeEngineResult(nextBidDeal);
                    } else {
                        result = engine.getNextBid(param);
                    }
                } else {
                    operation = "nextCard";
                    String nextCardDeal = game.getDeal().getNextCard(game.bids, game.cards);
                    if (nextCardDeal != null) {
                        result = new BridgeEngineResult(nextCardDeal);
                    } else {
                        result = engine.getNextCard(param);
                    }
                }
                if (result != null && !result.isError()) {
                    resContent = result.getContent();
                } else {
                    log.error("Error operation="+operation+" - game="+game.toString()+" - engineParam="+param);
                }

                if (resContent == null) {
                    throw new Exception("operation="+operation+" result is null - param="+param+" => throw exception");
                }
            }

            //-----------------------------------------------
            // TREAT RESPONSE
            if (gameThread.isInterrupt()) {
                if (log.isDebugEnabled()) {
                    log.debug("Interrupt current thread ! " + gameThread.getThreadName());
                }
                return false;
            }
            if (game != null && gameID.equals(game.getIDStr())) {
                synchronized (getLockOnGame(game.getIDStr())) {
                    // check game status
                    boolean bStatusOK = true;
                    if (game.getStep() != gameStep) {
                        bStatusOK = false;
                        log.error("Error gameStep is not the same before sending request ! current="+game.getStep()+" - previous="+gameStep+" - game="+game.toString());
                    }
                    if (!isRobotToPlayArgine(game)) {
                        bStatusOK = false;
                        log.error("Error no robot to play ! position="+game.getCurrentPlayer()+" - game="+game.toString());
                    }
                    if (!bStatusOK) {
                        return false;
                    }
                    //------------------------------
                    // NEXT BID
                    if (bReqBid) {
                        // result can contains many bids (anticipation)
                        if (resContent == null) {
                            if (tournamentMgr.getConfigIntValue("gameThreadBidNotValidPlayPA", 0) == 1) {
                                resContent = "PA";
                                log.error("getNexBid not valid result="+resContent+" - game="+game+"  => play PA - param="+param);
                            } else {
                                throw new Exception("Bid computed by engine not valid ! resContent="+resContent+" - game="+game+" - engineParam="+param);
                            }
                        }

                        int idxRes = 0;
                        boolean bContinue = true;
                        // loop on each bid in anticipation
                        while ((idxRes+1) < resContent.length() && bContinue) {
                            String tempBid = resContent.substring(idxRes, idxRes+2);
                            idxRes += 2;
                            boolean bidAlert = false;
                            if (idxRes < resContent.length() && resContent.charAt(idxRes) == 'a') {
                                bidAlert = true;
                                idxRes++;
                            }
                            BridgeBid bid = BridgeBid.createBid(tempBid, game.getCurrentPlayer(), bidAlert);
                            if (bid == null || !GameBridgeRule.isBidValid(game.getListBid(), bid)) {
                                if (tournamentMgr.getConfigIntValue("gameThreadBidNotValidPlayPA", 0) == 1) {
                                    log.error("Bid computed by engine not valid - result="+tempBid+" - bid="+bid+" - game="+game+" - engineParam="+param+" -> Play PA");
                                    bid = BridgeBid.createBid("PA", game.getCurrentPlayer());
                                    bContinue = false;
                                } else {
                                    throw new Exception("Bid computed by engine not valid ! bid="+bid+" - tempBid="+tempBid+" - resContent="+resContent+" - game="+game+" - engineParam="+param);
                                }
                            }
                            if (!playArgineValidBid(game, bid)) {throw new Exception("Method playValidBid return false !");}
                            // check it is always robot to play
                            if (game.isEndBid() || !isRobotToPlayArgine(game)) {
                                bContinue = false;
                            }

                        }
                    }
                    // -----------------------
                    // NEXT CARD
                    else {
                        BridgeCard card = null;
                        // result can contains many cards (anticipation)
                        if (resContent == null) {
                            if (tournamentMgr.getConfigIntValue("gameThreadCardNotValidPlaySmall", 0) == 1) {
                                // error => play the smallest card !
                                BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                                card = GameBridgeRule.getSmallestCardForPlayerAndColor(game.getListCard(),
                                        GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                        game.getCurrentPlayer(),
                                        (firstcardTrick != null ? firstcardTrick.getColor() : null));
                                log.error("getNextCard not valid result=" + resContent + " - game=" + game + "  => play smallest card=" + card + " - engineParam=" + param);
                            } else {
                                throw new Exception("Card computed by engine not valid ! card="+card+" - resContent="+resContent+" - game="+game+" - engineParam="+param);
                            }
                            if (!playArgineValidCard(game, card, false)) {throw new Exception("Method playValidCard return false !");}
                        } else {
                            int idxRes = 0;
                            boolean bContinue = true;
                            // loop on each card in anticipation
                            while (idxRes < resContent.length() && bContinue) {
                                String tempCard = resContent.substring(idxRes, idxRes+2);
                                card = BridgeCard.createCard(tempCard, game.getCurrentPlayer());
                                if (card == null  || !GameBridgeRule.isCardValid(game.getListCard(), card, GameBridgeRule.convertCardDealToList(game.getDeal().getCards()))) {
                                    if (tournamentMgr.getConfigIntValue("gameThreadCardNotValidPlaySmall", 0) == 1) {
                                        // error => play the smallest card !
                                        BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
                                        card = GameBridgeRule.getSmallestCardForPlayerAndColor(game.getListCard(),
                                                GameBridgeRule.convertCardDealToList(game.getDeal().getCards()),
                                                game.getCurrentPlayer(),
                                                (firstcardTrick != null ? firstcardTrick.getColor() : null));
                                        log.error("Card computed by engine not valid - result=" + tempCard + " - game=" + game + " - engineParam=" + param + " => play smallest card=" + card);
                                        bContinue = false;
                                    } else {
                                        throw new Exception("Card computed by engine not valid ! card="+card+" - tempCard="+tempCard+" - resContent="+resContent+" - game="+game+" - engineParam="+param);
                                    }
                                }
                                idxRes += 2;
                                boolean spreadResultArgine = false;
                                if (resContent.length() > idxRes && resContent.charAt(idxRes) == 's') {
                                    spreadResultArgine = true;
                                    idxRes++;
                                }
                                if (!playArgineValidCard(game, card, spreadResultArgine)) {throw new Exception("Method playValidCard return false !");}
                                // check it is always robot to play
                                if (game.isFinished() || !isRobotToPlayArgine(game)) {
                                    bContinue = false;
                                }

                            }
                        }
                    }
                    // after play card or bid, update tournament
                    if (game.isFinished()) {
                        tournamentMgr.updateGamePlayArgineFinished(game);
                    }
                    bNextPlayerEngine = isRobotToPlayArgine(game);
                } // end of synchronization
            }
        } catch (Exception e) {
            log.error("Exception on thread playRobotGame - game="+game, e);
        }

        return bNextPlayerEngine;
    }

    public void addArgineGameRunning(Game game) {
        if (game != null) {
            mapGamePlayArgine.put(game.getIDStr(), game);
        }
    }

    public boolean removeArgineGameRunning(String gameID) {
        return mapGamePlayArgine.remove(gameID) != null;
    }

    public Game getArgineGameRunning(String gameID) {
        return mapGamePlayArgine.get(gameID);
    }

    public Map<String, Game> getMapArgineGameRunning() {
        return mapGamePlayArgine;
    }
}
