package com.funbridge.server.tournament.learning;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.texts.TextUIMgr;
import com.funbridge.server.tournament.game.*;
import com.funbridge.server.tournament.learning.data.*;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.game.WSGameView;
import com.funbridge.server.ws.game.WSTableGame;
import com.funbridge.server.ws.tournament.WSDealCommented;
import com.funbridge.server.ws.tournament.WSTableTournament;
import com.funbridge.server.ws.tournament.WSTournament;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.bridge.BridgeDeal;
import com.gotogames.common.bridge.BridgeGame;
import com.gotogames.common.bridge.PBNConvertion;
import com.gotogames.common.lock.LockWeakString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bson.types.ObjectId;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * Created by ldelbarre on 30/05/2018.
 */
@Component(value="tourLearningMgr")
@Scope(value="singleton")
public class TourLearningMgr extends FunbridgeMgr implements ITournamentMgr {
    @Resource(name = "mongoTourLearningTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name = "playerMgr")
    private PlayerMgr playerMgr = null;
    @Resource(name = "textUIMgr")
    private TextUIMgr textUIMgr = null;
    @Resource(name="playerCacheMgr")
    private PlayerCacheMgr playerCacheMgr = null;
    private GameMgr gameMgr;
    private LockWeakString lockCreateGame = new LockWeakString();
    private LockWeakString lockTournament = new LockWeakString();
    private int category = Constantes.TOURNAMENT_CATEGORY_LEARNING;

    private final String TEXTUI_TEXT_BIDS = "learning.textBids.";
    private final String TEXTUI_TEXT_CARDS = "learning.textCards.";

    public static final int DEAL_STATUS_START = 1;
    public static final int DEAL_STATUS_FINISH = 2;

    @Override
    public void startUp() {
        log.info("startUp");
    }

    @Override
    @PreDestroy
    public void destroy() {
        if (gameMgr != null) {
            gameMgr.destroy();
        }
    }

    @PostConstruct
    public void init() {
        gameMgr = new GameMgr(this);
    }

    @Override
    public String getTournamentCategoryName() {
        return Constantes.TOURNAMENT_CATEGORY_NAME_LEARNING;
    }

    @Override
    public LearningTournament getTournament(String tourID) {
        return mongoTemplate.findById(new ObjectId(tourID), LearningTournament.class);
    }

    @Override
    public LearningGame getGame(String gameID) {
        return mongoTemplate.findById(new ObjectId(gameID), LearningGame.class);
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
                ContextManager.getTourLearningMgr().getLogger().error("Failed to retrieve dealIndex from dealID=" + dealID, e);
            }
        }
        return -1;
    }

    @Override
    public String _buildDealID(String tournamentId, int index) {
        return tournamentId+"-"+(index<10?"0":"")+index;
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
    public int getConfigIntValue(String paramName, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("tournament.LEARNING." + paramName, defaultValue);
    }

    @Override
    public void checkGame(Game game) throws FBWSException {
        if (game == null) {
            log.error("Game is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    public GameMgr getGameMgr() {
        return gameMgr;
    }

    @Override
    public void checkTournamentMode(boolean isReplay, long playerId) throws FBWSException {}

    public Class<LearningGame> getGameEntity() {
        return LearningGame.class;
    }

    public Class<LearningTournament> getTournamentEntity() {
        return LearningTournament.class;
    }

    public Class<LearningTournamentPlayer> getTournamentPlayerEntity() {
        return LearningTournamentPlayer.class;
    }

    @Override
    public void updateTournamentDB(Tournament tour) throws FBWSException {
        if (tour != null && tour instanceof LearningTournament) {
            LearningTournament tourLearning = (LearningTournament)tour;
            try {
                mongoTemplate.save(tourLearning);
            } catch (Exception e) {
                log.error("Exception to save tour=" + tour, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.error("Tour not valid ! tour="+tour);
        }
    }

    /**
     * Save the game to DB. if already existing an update is done, else an insert
     * @param game
     * @throws FBWSException
     */
    public void updateGameDB(Game game) throws FBWSException {
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
    public void updateListGameDB(List<LearningGame> listGame) throws FBWSException {
        if (listGame != null && listGame.size() > 0) {
            try {
                long ts = System.currentTimeMillis();
                for (Game g : listGame) {
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

    @Override
    public void updateGameFinished(FBSession session) throws FBWSException {
        if (session == null) {
            log.error("Session parameter is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (session.getCurrentGameTable() == null || session.getCurrentGameTable().getGame() == null || !(session.getCurrentGameTable().getGame() instanceof LearningGame)) {
            log.error("Table or game in session not valid ! - table="+session.getCurrentGameTable());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        LearningGame game = (LearningGame)session.getCurrentGameTable().getGame();
        if (!game.isReplay()) {
            if (game.isFinished()) {
                // update data game and table in DB
                updateGameDB(game);

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
    public boolean isBidAnalyzeEnable() {
        return getConfigIntValue("engineAnalyzeBid", 1) == 1;
    }

    @Override
    public boolean isDealParEnable() {
        return getConfigIntValue("enginePar", 1) == 1;
    }

    /**
     * Get data to play a deal on tournament
     * @param session
     * @param chapterID
     * @return
     * @throws FBWSException
     */
    public WSTableTournament playTournament(FBSession session, String chapterID, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) throws FBWSException {
        if (session == null) {
            log.error("Session is null");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        Table table = null;
        LearningTournament tour = null;

        //***********************
        // if tournament in progress in session
        if (session.getCurrentGameTable() != null &&
                !session.getCurrentGameTable().isReplay() &&
                session.getCurrentGameTable().getTournament() != null &&
                session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_LEARNING &&
                ((LearningTournament)session.getCurrentGameTable().getTournament()).getChapterID().equals(chapterID) &&
                session.getCurrentGameTable().getGame() != null && !session.getCurrentGameTable().getGame().isFinished()) {
            table = session.getCurrentGameTable();
            tour = (LearningTournament) table.getTournament();
            if (tour == null) {
                log.error("No tournament found on table ... chapterID="+chapterID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        else {
            tour = getFirstTournamentForChapter(chapterID);
            if (tour == null) {
                log.error("No tournament found with this chapterID="+chapterID);
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

        //********************
        // Need to build table
        if (table == null) {
            table = new Table(session.getPlayer(), tour);
            // retrieve game if exist
            LearningGame game = getGameNotFinishedOnTournamentForPlayer(tour.getIDStr(), session.getPlayer().getID());
            if (game != null) {
                table.setGame(game);
            }
        }
        session.setCurrentGameTable(table);

        //*******************
        // retrieve game
        LearningGame game = null;
        if (table.getGame() != null &&
                !table.getGame().isReplay() &&
                !table.getGame().isFinished() &&
                table.getGame().getTournament().getIDStr().equals(tour.getIDStr())) {
            game = (LearningGame)table.getGame();
        } else {
            // need to retrieve an existing game else create a new
            int lastIndexPlayed = 0;
            List<LearningGame> listGame = listGameOnTournamentForPlayer(tour.getIDStr(), session.getPlayer().getID());
            if (listGame != null) {
                for (LearningGame g : listGame) {
                    if (!g.isFinished()) {
                        game = g;
                        break;
                    }
                    lastIndexPlayed = g.getDealIndex();
                }
            }
            // need to create a new game
            if (game == null) {
                // restart from 1st deal
                if (lastIndexPlayed >= tour.getNbDeals()) {
                    lastIndexPlayed = 0;
                }

                // create game
                game = createGame(session.getPlayer().getID(), tour, lastIndexPlayed+1, conventionProfil, conventionValue, cardsConventionProfil, cardsConventionValue, session.getDeviceID(), chapterID);

                if (game == null) {
                    log.error("Failed to create game - player="+session.getPlayer().getID()+" - tour="+tour);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_LEARNING, 1);
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
        tableTour.tournament = toWSTournament(tour);
        tableTour.tournament.currentDealIndex = game.getDealIndex();
        tableTour.currentDeal = new WSGameDeal();
        tableTour.currentDeal.setDealData(game.getDeal(), tour.getIDStr());
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
     * Create a game for player on tournament and dealIndex.
     * @param playerID
     * @param tournament
     * @param dealIndex
     * @param conventionProfil
     * @param conventionData
     * @param cardsConventionProfil
     * @param cardsConventionData
     * @param deviceID
     * @return
     * @throws FBWSException if an exception occurs
     */
    private LearningGame createGame(long playerID, LearningTournament tournament, int dealIndex, int conventionProfil, String conventionData, int cardsConventionProfil, String cardsConventionData, long deviceID, String chapterID) throws FBWSException {
        synchronized (lockCreateGame.getLock(tournament.getIDStr()+"-"+playerID)) {
            try {
                LearningGame game = new LearningGame(playerID, tournament, dealIndex);
                game.setStartDate(Calendar.getInstance().getTimeInMillis());
                game.setLastDate(Calendar.getInstance().getTimeInMillis());
                game.setConventionSelection(conventionProfil, conventionData);
                game.setCardsConventionSelection(cardsConventionProfil, cardsConventionData);
                game.setDeviceID(deviceID);

                LearningCommentsDeal learningCommentsDeal = getLearningCommentsDealForChapter(chapterID);
                if (learningCommentsDeal != null && learningCommentsDeal.useBids) {
                    game.setBids(learningCommentsDeal.bids);
                    game.initData();
                    game.buildContract();
                }

                // try to find bug of not valid game ...
                if (game.isFinished() && game.getContractType() == 0 && game.getBids().length() == 0) {
                    log.error("Game not valid !!! - game=" + game);
                    Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                mongoTemplate.insert(game);
                return game;
            } catch (Exception e) {
                log.error("Exception to create game player="+playerID+" - tour="+ tournament +" - dealIndex="+dealIndex, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Return game associated to this tournament, deal index and player
     * @param tournamentID
     * @param dealIndex
     * @param playerID
     * @return
     */
    public LearningGame getGameOnTournamentAndDealForPlayer(String tournamentID, int dealIndex, long playerID){
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
            Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cPlayerID));
            q.with(Sort.by(Sort.Direction.DESC, "startDate"));
            return mongoTemplate.findOne(q, LearningGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament deal and playerID", e);
        }
        return null;
    }

    /**
     * Retrieve tournament wich include this deal
     * @param dealID
     * @return
     */
    public LearningTournament getTournamentWithDeal(String dealID) {
        String tourID = _extractTourIDFromDealID(dealID);
        if (tourID != null) {
            return getTournament(tourID);
        }
        return null;
    }

    /**
     * Return WS DealCommented object for this dealID
     * @param dealID
     * @return
     */
    public WSDealCommented getWSDealCommentedForDealID(String dealID, String lang) {
        LearningTournament tour = getTournamentWithDeal(dealID);
        if (tour != null) {
            int dealIndex = _extractDealIndexFromDealID(dealID);
            LearningDeal deal = (LearningDeal)tour.getDealAtIndex(dealIndex);
            if (deal != null) {
                LearningCommentsDeal learningCommentsDeal = getLearningCommentsDeal(deal.learningCommentsDeal);
                if (learningCommentsDeal != null) {
                    WSDealCommented dealCommented = new WSDealCommented();
                    dealCommented.deal = new WSGameDeal();
                    dealCommented.deal.setDealData(deal, tour.getIDStr());
                    dealCommented.deal.bidList = learningCommentsDeal.bids;
                    dealCommented.deal.contract = learningCommentsDeal.contract;
                    dealCommented.deal.declarer = learningCommentsDeal.declarer;
                    dealCommented.deal.playList = learningCommentsDeal.begins;
                    dealCommented.nbTricks = learningCommentsDeal.nbTricks;
                    dealCommented.commentsBids = getCommentsBids(learningCommentsDeal.chapterID, lang);
                    dealCommented.commentsCards = getCommentsCards(learningCommentsDeal.chapterID, lang);
                    return dealCommented;
                } else {
                    log.error("No LearningDealAuthor found for deal="+deal);
                }
            } else {
                log.error("No LearningDeal found for dealIndex="+dealIndex);
            }
        } else {
            log.error("No LearningTournament found for dealID="+dealID);
        }
        return null;
    }

    public String getCommentsBids(String chapterID, String lang) {
        String commentsBids = textUIMgr.getTextUIForLang(TEXTUI_TEXT_BIDS + chapterID, lang);
        if (commentsBids == null || commentsBids.length() == 0) {
            commentsBids = textUIMgr.getTextUIForLang(TEXTUI_TEXT_BIDS + "empty", lang);
        }
        return commentsBids;
    }

    public String getCommentsCards(String chapterID, String lang) {
        String commentsCards = textUIMgr.getTextUIForLang(TEXTUI_TEXT_CARDS + chapterID, lang);
        if (commentsCards == null || commentsCards.length() == 0) {
            commentsCards = textUIMgr.getTextUIForLang(TEXTUI_TEXT_CARDS + "empty", lang);
        }
        return commentsCards;
    }

    /**
     * Transform tournament for webservice object
     * @param tour
     * @return
     */
    public WSTournament toWSTournament(LearningTournament tour) {
        if (tour != null) {
            WSTournament wst = tour.toWS();
            if (tour.isFinished()) {
                // used fata from DB
                wst.nbTotalPlayer = tour.getNbPlayers();
            }
            return wst;
        }
        return null;
    }

    /**
     * Return the game not finished for a player on tournament.
     * @param tournamentID
     * @param playerID
     * @return
     */
    private LearningGame getGameNotFinishedOnTournamentForPlayer(String tournamentID, long playerID) throws FBWSException{
        List<LearningGame> listGames = listGameOnTournamentForPlayer(tournamentID, playerID);
        if (listGames != null) {
            for (LearningGame g : listGames) {
                if (!g.isFinished()) {
                    return g;
                }
            }
        }
        return null;
    }

    /**
     * List game for a player on a tournament (order by dealIndex asc)
     * @param tournamentID
     * @param playerID
     * @return
     */
    public List<LearningGame> listGameOnTournamentForPlayer(String tournamentID, long playerID) throws FBWSException{
        try {
            Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tournamentID)).andOperator(Criteria.where("playerID").is(playerID)));
            q.with(new Sort(Sort.Direction.ASC, "dealIndex"));
            return mongoTemplate.find(q, LearningGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament="+tournamentID+" and playerID="+playerID, e);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
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
            return (int) mongoTemplate.count(q, LearningGame.class);
        } catch (Exception e) {
            log.error("Failed to count games for player - playerID="+ playerID, e);
        }
        return 0;
    }
    
    /**
     * Create LearningCommentsDeal from PBN file
     * @param pbnFile
     * @return
     */
    public LearningCommentsDeal createLearningCommentsDealFromPBNFile(File pbnFile) {
        BridgeGame bg = PBNConvertion.PBNToGame(pbnFile);
        if (bg != null) {
            String contract = bg.getContractWithType();
            String bids = bg.getStringBidsWithPosition();
            String begins = null;
            if (bg.getCardList() != null && !bg.getCardList().isEmpty()) {
                begins = bg.getCardList().get(0).getStringWithOwner();
            }
            String strNbTricks = bg.getResult();
            String chapter = bg.getMetadata("Chapter");
            boolean useBids = Boolean.valueOf(bg.getMetadata("UseBids"));
            String dealFileID = pbnFile.getName();
            String dealNumber = "1";
            String declarer = "" + bg.getDeclarer();

            if (chapter == null || chapter.length() == 0) {
                log.error("No chapter define for this pbnFile="+pbnFile);
                chapter = "unknown";
            }
            LearningCommentsDeal cda = new LearningCommentsDeal();
            cda.chapterID = chapter;
            cda.useBids = useBids;
            cda.dealer = bg.getDealer();
            cda.vulnerability = bg.getVulnerability();
            cda.distribution = bg.getDistributionString();
            // check contract
            if (contract == null || contract.length() == 0) {
                log.error("No contract define for this pbnFile=" + pbnFile);
                cda.addImportProcessError("No contract found");
                contract = "";
            }
            cda.contract = contract;
            // check declarer
            if (declarer == null || declarer.equals("" + BridgeConstantes.POSITION_NOT_VALID)) {
                log.error("Declarer not valid : " + declarer + " for this pbnFile=" + pbnFile);
                cda.addImportProcessError("Declarer not valid");
                declarer = "";
            }
            cda.declarer = declarer;
            // check bids
            if (bids == null || bids.length() == 0) {
                log.error("No bids define for this pbnFile=" + pbnFile);
                cda.addImportProcessError("No bids found");
                bids = "";
            }
            cda.bids = bids;
            // check begins
            if (begins == null || begins.length() == 0) {
                log.error("No begins define for this pbnFile=" + pbnFile);
                cda.addImportProcessError("No begins card found");
                begins = "";
            }
            cda.begins = begins;
            // check nb tricks
            if (strNbTricks == null || strNbTricks.length() == 0 || !StringUtils.isNumeric(strNbTricks)) {
                log.error("NbTricks not valid : NbTricks=" + strNbTricks + " for this pbnFile=" + pbnFile);
                cda.addImportProcessError("Nb tricks not valid : " + strNbTricks);
                strNbTricks = "0";
            }
            cda.nbTricks = Integer.parseInt(strNbTricks);
            // check dealFileID
            if (dealFileID == null || dealFileID.length() == 0) {
                log.error("No DealFileID define for this pbnFile=" + pbnFile);
                cda.addImportProcessError("No dealFileID found");
                dealFileID = "";
            }
            cda.importDealFileID = dealFileID;
            // check dealNumber
            if (dealNumber == null || dealNumber.length() == 0) {
                log.error("No DealNumber define for this pbnFile=" + pbnFile);
                cda.addImportProcessError("No DealNumber found");
                dealNumber = "0";
            }
            int dealNumberInt = 0;
            try {
                dealNumberInt=Integer.parseInt(dealNumber);
            } catch (Exception e) {
                log.error("Failed to parse dealNumber="+dealNumber+" for this pbnFile=" + pbnFile);
                cda.addImportProcessError("Failed to parse dealNumber="+dealNumber);
            }
            cda.importDealNumber = dealNumberInt;
            mongoTemplate.insert(cda);
            return cda;
        } else {
            log.error("Failed to build BridgeGame for pbnFile=" + pbnFile);
        }
        return null;
    }

    /**
     * Import all deals from path
     * @param pathFile
     * @return
     * @throws Exception
     */
    public ImportDealResult importDealFromPath(String pathFile) throws Exception {
        File fPath = new File(pathFile);
        if (!fPath.exists() || !fPath.isDirectory()) {
            throw new Exception("Path file not existing or not directory - pathFile="+pathFile);
        }

        if (log.isDebugEnabled()) {
            log.debug("Begin import from path="+pathFile);
        }
        // get list of pbn
        List<File> files = (List<File>)FileUtils.listFiles(fPath, new String[] {"pbn"}, false);
        // sort list by filename
        files.sort(Comparator.comparing(File::getName));
        if (log.isDebugEnabled()) {
            log.debug("Nb files to import="+files.size());
        }
        ImportDealResult result = new ImportDealResult();
        result.nbFilesInPath = files.size();
        // process import for all files
        for (File fDeal : files) {
            try {
                if (createLearningCommentsDealFromPBNFile(fDeal) != null) {
                    result.nbFilesOK++;
                    // import OK => remove file
                    FileUtils.forceDelete(fDeal);
                } else {
                    result.nbFilesError++;
                    result.filesError.add(fDeal.getPath());
                    // import failed => rename file with error extension
                    fDeal.renameTo(new File(fDeal.getPath() + "_error"));
                }
            } catch (Exception e) {
                log.error("Failed to process file="+fDeal.getPath(), e);
                result.nbFilesError++;
                result.filesError.add(fDeal.getPath());
                // import failed => rename file with error extension
                fDeal.renameTo(new File(fDeal.getPath() + "_error"));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("End import from path="+pathFile+" - result="+result);
        }
        return result;
    }

    public class ImportDealResult {
        public int nbFilesInPath = 0;
        public int nbFilesError = 0;
        public int nbFilesOK = 0;
        public List<String> filesError = new ArrayList<>();
        public String toString() {
            return "nbFilesInPath="+nbFilesInPath+" - nbFilesOK="+nbFilesOK+" - nbFilesError="+nbFilesError+" - filesError size="+filesError.size();
        }
    }

    /**
     * Count learningCommentsDeal object
     * @return
     */
    public long countLearningCommentsDeal() {
        Query q = new Query();
        return mongoTemplate.count(q, LearningCommentsDeal.class);
    }

    /**
     * List learningCommentsDeal objects
     * @param offset
     * @param nbMax
     * @return
     */
    public List<LearningCommentsDeal> listLearningCommentsDeal(int offset, int nbMax) {
        Query q = new Query();
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        q.with(new Sort(Sort.Direction.ASC, "date").and(new Sort(Sort.Direction.ASC, "importDealFileID")).and(new Sort(Sort.Direction.ASC, "importDealNumber")));
        return mongoTemplate.find(q, LearningCommentsDeal.class);
    }

    /**
     * Return first tournament for chapter (used creationDate field)
     * @param chapterID
     * @return
     */
    public LearningTournament getFirstTournamentForChapter(String chapterID) {
        Query q = Query.query(Criteria.where("chapterID").is(chapterID));
        q.with(new Sort(Sort.Direction.ASC, "creationDate"));
        return mongoTemplate.findOne(q, LearningTournament.class);
    }

    /**
     * Get the first deal for this chapter with dealer and vulnerability.
     * @param chapter
     * @param dealer if null criteria dealer is not used
     * @param vulnerability if null criteria vulnerability is not used
     * @param distributionToExclude list distribution to exclude from search
     * @return
     */
    public LearningCommentsDeal getLearningCommentsDealToCreateTournament(String chapter, String dealer, String vulnerability, List<String> distributionToExclude) {
        Query q = new Query();
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(Criteria.where("chapterID").is(chapter));
        if (distributionToExclude != null && distributionToExclude.size() > 0) {
            listCriteria.add(Criteria.where("distribution").nin(distributionToExclude));
        }
        if (dealer != null && dealer.length() > 0) {
            listCriteria.add(Criteria.where("dealer").is(dealer));
        }
        if (vulnerability != null && vulnerability.length() > 0) {
            listCriteria.add(Criteria.where("vulnerability").is(vulnerability));
        }
        q.addCriteria(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
        q.with(new Sort(Sort.Direction.ASC, "date").and(new Sort(Sort.Direction.ASC, "importDealFileID")).and(new Sort(Sort.Direction.ASC, "importDealNumber")));
        q.limit(1);
        List<LearningCommentsDeal> list = mongoTemplate.find(q, LearningCommentsDeal.class);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    /**
     * Get the deal for this chapter.
     * @param chapter
     * @return
     */
    public LearningCommentsDeal getLearningCommentsDealForChapter(String chapter) {
        Query q = new Query(Criteria.where("chapterID").is(chapter));
        q.with(new Sort(Sort.Direction.ASC, "importDealNumber"));
        LearningCommentsDeal learningCommentsDeal = mongoTemplate.findOne(q, LearningCommentsDeal.class);
        return learningCommentsDeal;
    }

    /**
     * Create a tournament for this chapter
     * @param chapter
     * @return
     * @throws Exception
     */
    public CreateTournamentResult createTournamentForChapter(String chapter) {
        CreateTournamentResult result = new CreateTournamentResult();
        int nbDeal = getConfigIntValue("tourNbDeal", 1);
        // check authorTourIndex not already existing
        if (getFirstTournamentForChapter(chapter) != null) {
            result.error = "Tournament already exist with chapter="+ chapter;
            log.error(result.error);
            return result;
        }

        // search all deals for the tournament
        List<LearningCommentsDeal> listCommentsDeals = new ArrayList<>();
        List<String> listDistribution = new ArrayList<>();
        for (int idx=1; idx<= nbDeal; idx++) {
            char cVulnerability = BridgeDeal.getVulnerability(idx);
            char cDealer = BridgeDeal.getDealer(idx);
            LearningCommentsDeal deal = getLearningCommentsDealToCreateTournament(chapter, ""+cDealer,""+cVulnerability, listDistribution);
            if (deal == null) {
                log.error("No deal found (validate and not used) for chapter="+ chapter +" - dealer="+cDealer+" - vulnerability="+cVulnerability+" - idx="+idx+" - try to find another with other vulnerability");
                deal = getLearningCommentsDealToCreateTournament(chapter, ""+cDealer, null, listDistribution);
                if (deal == null) {
                    log.error("No deal found (validate and not used) for chapter="+ chapter +" - dealer="+cDealer+" - idx="+idx+" - try to find another with no restrictions on dealer and vulnerability");
                    deal = getLearningCommentsDealToCreateTournament(chapter, null, null, listDistribution);
                    if (deal == null) {
                        result.error = "No deal found (validate and not used) for chapter="+ chapter +" - idx="+idx;
                        log.error(result.error);
                        return result;
                    }
                }
            }

            listCommentsDeals.add(deal);
            listDistribution.add(deal.distribution);
        }

        if (listCommentsDeals.size() != nbDeal) {
            result.error = "Nb deal ("+nbDeal+") not reached for chapter="+ chapter;
            log.error(result.error);
            return result;
        }
        // create tournament
        result.tournament = new LearningTournament();
        result.tournament.setCreationDate(new Date());
        result.tournament.setChapterID(chapter);
        result.tournament.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
        for (int i=0; i < listCommentsDeals.size(); i++) {
            result.tournament.getDeals().add(listCommentsDeals.get(i).transformDeal(i+1));
        }
        mongoTemplate.insert(result.tournament);
        log.warn("Create tournament for chapter="+ chapter +" - tour="+result.tournament);

        // mark all deals as used in tournament
        for (int i = 0; i < listCommentsDeals.size(); i++) {
            LearningCommentsDeal deal = listCommentsDeals.get(i);
            mongoTemplate.save(deal);
        }
        return result;
    }

    public class CreateTournamentResult {
        public LearningTournament tournament;
        public String error;
    }

    /**
     * Return LearningCommentsDeal object with this ID
     * @param id
     * @return
     */
    public LearningCommentsDeal getLearningCommentsDeal(String id) {
        if (id != null && id.length() > 0) {
            return mongoTemplate.findById(new ObjectId(id), LearningCommentsDeal.class);
        }
        return null;
    }

    /**
     * List all tournaments (order by date creation ASC)
     * @return
     */
    public List<LearningTournament> listAllTournaments(int offset, int limit) {
        Query q = new Query();
        q.with(new Sort(Sort.Direction.ASC, "creationDate"));
        q.skip(offset);
        q.limit(limit);
        return mongoTemplate.find(q, LearningTournament.class);
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
        LearningGame game = getGame(gameID);
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
        gameView.tournament = toWSTournament(game.getTournament());

        return gameView;
    }

    /**
     * Return a gameView for the deal, score and contract. If player has played this deal with this contract and score, return the game of this player else return the game of unknown player
     * @param dealID
     * @param score
     * @param contractString
     * @param lead
     * @param playerCache
     * @return
     * @throws FBWSException
     */
    public WSGameView viewGameForDealScoreAndContract(String dealID, int score, String contractString, String lead, PlayerCache playerCache) throws FBWSException {
        LearningTournament tour = getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        LearningDeal deal = (LearningDeal)tour.getDeal(dealID);
        if (tour == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // check player played the deal of this game
        LearningGame game = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
        if (game == null) {
            log.error("PLAYER DOESN'T PLAY THIS DEAL : dealID="+dealID+" - playerID="+playerCache.ID);
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
                game = mongoTemplate.findOne(q, LearningGame.class);
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
        table.playerSouth = WSGamePlayer.createGamePlayerHuman(playerCacheMgr.getPlayerCache(game.getPlayerID()), Constantes.PLAYER_STATUS_PRESENT, playerCache.ID);
        table.playerWest = WSGamePlayer.createGamePlayerRobot();
        table.playerNorth = WSGamePlayer.createGamePlayerRobot();
        table.playerEast = WSGamePlayer.createGamePlayerRobot();

        WSGameDeal gameDeal = new WSGameDeal();
        gameDeal.setDealData(game.getDeal(), game.getTournament().getIDStr());
        gameDeal.setGameData(game);

        WSGameView gameView = new WSGameView();
        gameView.game = gameDeal;
        gameView.table = table;
        gameView.tournament = toWSTournament(game.getTournament());

        return gameView;
    }

    @Override
    public void updateGamePlayArgineFinished(Game game) throws FBWSException {

    }

    @Override
    public void updateGamePlayArgine(Game game) {

    }


    public LearningProgression getLearningProgression(long playerID) {
        return mongoTemplate.findById(playerID, LearningProgression.class);
    }

    public void updateLearningProgression(long playerID, String sb, int chapter, int deal, int status, int step) {
        LearningProgression learningProgression = getLearningProgression(playerID);
        if (learningProgression == null) {
            learningProgression = new LearningProgression();
            learningProgression.playerID = playerID;
            learningProgression.addElement(sb, chapter, deal, status, step);
            mongoTemplate.insert(learningProgression);
        } else {
            if (learningProgression.addElement(sb, chapter, deal, status, step)) {
                mongoTemplate.save(learningProgression);
            }
        }
    }
}
