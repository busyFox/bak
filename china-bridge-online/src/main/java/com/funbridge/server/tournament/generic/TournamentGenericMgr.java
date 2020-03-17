package com.funbridge.server.tournament.generic;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.tournament.DealGenerator;
import com.funbridge.server.tournament.IDealGeneratorCallback;
import com.funbridge.server.tournament.federation.FederationMgr;
import com.funbridge.server.tournament.federation.data.TourFederationDeal;
import com.funbridge.server.tournament.game.*;
import com.funbridge.server.tournament.generic.memory.*;
import com.funbridge.server.tournament.privatetournament.data.PrivateDeal;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.game.WSGameView;
import com.funbridge.server.ws.game.WSTableGame;
import com.funbridge.server.ws.result.*;
import com.funbridge.server.ws.tournament.WSTournament;
import com.gotogames.common.lock.LockWeakString;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * Created by pserent on 27/01/2017.
 */
public abstract class TournamentGenericMgr extends FunbridgeMgr implements ITournamentMgr, IDealGeneratorCallback {
    @Resource(name = "presenceMgr")
    protected PresenceMgr presenceMgr = null;
    @Resource(name="playerMgr")
    protected PlayerMgr playerMgr;
    @Resource(name="playerCacheMgr")
    protected PlayerCacheMgr playerCacheMgr;

    protected LockWeakString lockTournament = new LockWeakString();
    protected int category = 0;
    protected TournamentGenericMemoryMgr memoryMgr;
    protected GameMgr gameMgr;
    protected DealGenerator dealGenerator = null;

    public TournamentGenericMgr(){}

    public TournamentGenericMgr(int category) {
        this.category = category;
    }

    public <TMemTour extends GenericMemTournament,
            TMemTourPlayer extends GenericMemTournamentPlayer,
            TMemDeal extends GenericMemDeal,
            TMemDealPlayer extends GenericMemDealPlayer> void init(Class<TMemTour> classMemTour,
                                                                   Class<TMemTourPlayer> classMemTourPlayer,
                                                                   Class<TMemDeal> classMemDeal,
                                                                   Class<TMemDealPlayer> classMemDealPlayer) {
        gameMgr = new GameMgr(this);
        memoryMgr = new TournamentGenericMemoryMgr(this, classMemTour, classMemTourPlayer, classMemDeal, classMemDealPlayer);
        dealGenerator = new DealGenerator(this, getStringResolvEnvVariableValue("generatorParamFile", null));
    }

    public void destroy() {
        if (gameMgr != null) {
            gameMgr.destroy();
        }
        if (memoryMgr != null) {
            if (getConfigIntValue("backupMemorySave", 1) == 1) {
                int nbTourInMemory = memoryMgr.getSize();
                long ts = System.currentTimeMillis();
                int nbBackup = memoryMgr.backupAllTour();
                log.error("nbTourInMemory="+nbTourInMemory+" - nbBackup="+nbBackup+" - ts="+(System.currentTimeMillis() - ts));
            }
            memoryMgr.destroy();
        }
    }

    public int getCategory() {
        return category;
    }

    public void setCategory(int category) {
        this.category = category;
    }

    @Override
    public String getTournamentCategoryName() {
        return Constantes.tourCategory2Name(category);
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

    public String _buildDealID(String tourID, int index) {
        return buildDealID(tourID, index);
    }

    /**
     * Extract tourID string from dealID
     * @param dealID
     * @return
     */
    public static String extractTourIDFromDealID(String dealID) {
        if (dealID != null && dealID.indexOf('-') >= 0) {
            return dealID.substring(0, dealID.indexOf('-'));
        }
        return null;
    }

    /**
     * Extract dealIndex from dealID
     * @param dealID
     * @return
     */
    public static int extractDealIndexFromDealID(String dealID) {
        if (dealID != null && dealID.indexOf('-') >= 0) {
            try {
                return Integer.parseInt(dealID.substring(dealID.indexOf('-')+1));
            } catch (Exception e) {}
        }
        return -1;
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
    public int _extractDealIndexFromDealID(String dealID) {
        return extractDealIndexFromDealID(dealID);
    }

    /**
     * Read string value for parameter in name (tournament.CATEGORY_NAME.paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public String getConfigStringValue(String paramName, String defaultValue) {
        return getConfigStringValue(getTournamentCategoryName(), paramName, defaultValue);
    }

    public static String getConfigStringValue(String category, String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringValue("tournament." + category+"."+paramName, defaultValue);
    }

    public String getStringResolvEnvVariableValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringResolvEnvVariableValue("tournament." + getTournamentCategoryName()+"."+paramName, defaultValue);
    }

    /**
     * Read int value for parameter in name (tournament.CATEGORY_NAME.paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public int getConfigIntValue(String paramName, int defaultValue) {
        return getConfigIntValue(getTournamentCategoryName(), paramName, defaultValue);
    }

    public static int getConfigIntValue(String category, String paramName, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("tournament." + category+"."+paramName, defaultValue);
    }

    /**
     * Read boolean value for parameter in name (tournament.CATEGORY_NAME.paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public boolean getConfigBooleanValue(String paramName, boolean defaultValue) {
        return getConfigBooleanValue(getTournamentCategoryName(), paramName, defaultValue);
    }

    public static boolean getConfigBooleanValue(String category, String paramName, boolean defaultValue) {
        return FBConfiguration.getInstance().getConfigBooleanValue("tournament." + category+"."+paramName, defaultValue);
    }

    /**
     * Return the file path used to backup a tournament
     * @param tourID
     * @param mkdir if true, create dir if not existing
     * @return
     */
    public String buildBackupFilePathForTournament(String tourID, boolean mkdir) {
        String path = getStringResolvEnvVariableValue("backupMemoryPath", null);
        if (path != null) {
            if (mkdir) {
                try {
                    File f = new File(path);
                    if (!f.exists()) {
                        f.mkdirs();
                    }
                } catch (Exception e) {}
            }
            path = FilenameUtils.concat(path, tourID + ".json");
        }
        return path;
    }

    @Override
    public boolean isBidAnalyzeEnable() {
        return getConfigIntValue("engineAnalyzeBid", 1) == 1;
    }

    @Override
    public boolean isDealParEnable() {
        return getConfigIntValue("enginePar", 1) == 1;
    }

    public Object getLockOnTournament(String tourID) {
        return lockTournament.getLock(tourID);
    }

    public abstract MongoTemplate getMongoTemplate();

    public TournamentGenericMemoryMgr getMemoryMgr() {
        return memoryMgr;
    }

    @Override
    public GameMgr getGameMgr() {
        return gameMgr;
    }

    public abstract Class<? extends Game> getGameEntity();

    public Class<? extends Game> getAggregationGameEntity() {
        return getGameEntity();
    }

    public abstract String getGameCollectionName();

    public abstract Class<? extends Tournament> getTournamentEntity();

    public abstract Class<? extends TournamentPlayer> getTournamentPlayerEntity();

    public abstract boolean finishTournament(Tournament tour);

    public abstract int leaveTournament(FBSession session, String tournamentID) throws FBWSException;

    public boolean distributionExists(String cards) {
        return getMongoTemplate().findOne(Query.query(Criteria.where("deals.cards").is(cards)), getTournamentEntity()) != null;
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
     * Return the tournament associated with this ID
     * @param tourID
     * @return
     */
    public Tournament getTournament(String tourID) {
        return getMongoTemplate().findById(new ObjectId(tourID), getTournamentEntity());
    }

    /**
     * Return the game with this ID
     * @param gameID
     * @return
     */
    @Override
    public Game getGame(String gameID) {
        return getMongoTemplate().findById(new ObjectId(gameID), getGameEntity());
    }

    /**
     * Save the game to DB. if already existing an update is done, else an insert
     * @param game
     * @throws FBWSException
     */
    public void updateGameDB(Game game) throws FBWSException {
        if (game != null && !game.isReplay()) {
            // try to find bug of not valid game ...
            if (getConfigIntValue("findBugGameNotValid", 0) == 1
                    && game.isFinished() && game.getContractType() == 0 && game.getBids().length() == 0) {
                log.error("Game not valid !!! - game=" + game);
                Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                if (getConfigIntValue("blockBugGameNotValid", 0) == 1) {
                    log.error("Game not save ! game="+game);
                    return;
                }
            }
            try {
                getMongoTemplate().save(game);
            } catch (Exception e) {
                log.error("Exception to save game="+game, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Retrieve tournament wich include this deal
     * @param dealID
     * @return
     */
    public Tournament getTournamentWithDeal(String dealID) {
        String tourID = extractTourIDFromDealID(dealID);
        if (tourID != null) {
            return getTournament(tourID);
        }
        return null;
    }

    /**
     * Return the game not finished for a player on tournament.
     * @param tourID
     * @param playerID
     * @return
     */
    protected <T extends Game> T getGameNotFinishedOnTournamentForPlayer(String tourID, long playerID) {
        List<T> listGame = listGameOnTournamentForPlayer(tourID, playerID);
        if (listGame != null) {
            for (T g : listGame) {
                if (!g.isFinished()) {
                    return g;
                }
            }
        }
        return null;
    }

    /**
     * List game for a player on a tournament (order by dealIndex asc)
     * @param tourID
     * @param playerID
     * @return
     */
    public <T extends Game> List<T> listGameOnTournamentForPlayer(String tourID, long playerID) {
        Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("playerID").is(playerID)));
        q.with(new Sort(Sort.Direction.ASC, "dealIndex"));
        return (List<T>) getMongoTemplate().find(q, getGameEntity());
    }

    /**
     * Check if game exist on tournament with deviceID
     * @param tourID
     * @param deviceID
     * @return
     */
    protected boolean existGameOnTournamentForDevice(String tourID, long deviceID) {
        if (FBConfiguration.getInstance().getIntValue("general.checkPlayDealDevice", 1) == 0) {
            return false;
        }
        try {
            Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("deviceID").is(deviceID)));
            List l = getMongoTemplate().find(q, getGameEntity());
            if (!l.isEmpty()) {
                return true;
            }
        } catch (Exception e) {
            log.error("Exception to check if game exist for tour and device - tourID="+tourID+" - deviceID="+deviceID, e);
        }
        return false;
    }

    /**
     * Return if existing game on tournament and deal for a player
     * @param tourID
     * @param dealIndex
     * @param playerID
     * @return
     * @throws FBWSException
     */
    public <T extends Game> T getGameOnTournamentAndDealForPlayer(String tourID, int dealIndex, long playerID) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            q.addCriteria(new Criteria().andOperator(cTourID, cPlayerID, cDealIndex));
            return (T) getMongoTemplate().findOne(q, getGameEntity());
        } catch (Exception e) {
            log.error("Failed to find game for tournament deal and playerID", e);
        }
        return null;
    }

    /**
     * Return tournamentPlayer object for a player on a tournament
     * @param tourID
     * @param playerID
     * @return
     */
    public <T extends TournamentPlayer> T getTournamentPlayer(final String tourID, final long playerID) {
        return (T) getMongoTemplate().findOne(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)).andOperator(Criteria.where("playerID").is(playerID))), getTournamentPlayerEntity());
    }

    /**
     * List game on a tournament
     * @param tourID
     * @return
     */
    public <T extends Game> List<T> listGameOnTournament(String tourID){
        try {
            Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)));
            return (List<T>) getMongoTemplate().find(q, getGameEntity());
        } catch (Exception e) {
            log.error("Failed to find game for tournament="+tourID, e);
        }
        return null;
    }

    /**
     * List game on a tournament and deal not finished
     * @param tourID
     * @param dealIndex
     * @return
     */
    public <T extends Game> List<T> listGameOnTournamentForDealNotFinished(String tourID, int dealIndex) {
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tourID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            Criteria cNotFinished = Criteria.where("finished").is(false);
            q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex, cNotFinished));
            return (List<T>) getMongoTemplate().find(q, getGameEntity());
        } catch (Exception e) {
            log.error("Failed to list game not finished for tournament="+tourID+" and dealIndex="+dealIndex, e);
        }
        return null;
    }

    /**
     * List of finished game on tournament and deal
     * @param tourID
     * @param dealIndex
     * @param offset
     * @param nbMax
     * @return
     */
    public <T extends Game> List<T> listFinishedGameOnTournamentAndDeal(String tourID, int dealIndex, List<Long> listPlayerID, int offset, int nbMax) {
        List<Criteria> listCriteria = new ArrayList<>();
        listCriteria.add(Criteria.where("tournament.$id").is(new ObjectId(tourID)));
        if (listPlayerID != null) {
            listCriteria.add(Criteria.where("playerID").in(listPlayerID));
        }
        listCriteria.add(Criteria.where("dealIndex").is(dealIndex));
        listCriteria.add(Criteria.where("finished").is(true));
        Query q = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
        q.with(new Sort(Sort.Direction.DESC, "score"));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return (List<T>) getMongoTemplate().find(q, getGameEntity());
    }

    /**
     * Transform tournament for webservice object
     * @param tour
     * @param playerCache
     * @return
     */
    public <T1 extends Tournament, T2 extends TournamentPlayer> WSTournament toWSTournament(T1 tour, PlayerCache playerCache) {
        if (tour != null) {
            WSTournament wst = tour.toWS();
            GenericMemTournament memTournament = getMemoryMgr().getTournament(tour.getIDStr());
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
                T2 tourPlayer = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
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
     * Update games to DB
     * @param listGame
     * @throws FBWSException
     */
    public <T extends Game> void updateListGameDB(List<T> listGame) throws FBWSException {
        if (listGame != null && !listGame.isEmpty()) {
            try {
                for (T g : listGame) {
                    // try to find bug of not valid game ...
                    if (getConfigIntValue("findBugGameNotValid", 0) == 1
                            && g.isFinished() && g.getContractType() == 0 && g.getBids().length() == 0) {
                        log.error("Game not valid !!! - game=" + g);
                        Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                        log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                        if (getConfigIntValue("blockBugGameNotValid", 0) == 1) {
                            log.error("Game not save ! game="+g);
                            continue;
                        }
                    }
                    getMongoTemplate().save(g);
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
        if (listGame != null && !listGame.isEmpty()) {
            try {
                getMongoTemplate().insertAll(listGame);
            } catch (Exception e) {
                log.error("Exception to save listGame", e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Transform memTournamentPlayer to tournament archive
     * @param e
     * @return
     */
    public <T extends GenericMemTournamentPlayer> WSTournamentArchive toWSTournamentArchive(T e) {
        if (e != null) {
            WSTournamentArchive ws = new WSTournamentArchive();
            ws.date = e.dateStart;
            ws.nbPlayers = e.memTour.getNbPlayer();
            ws.name = e.memTour.name;
            if (e.isPlayerFinish()) {
                ws.rank = e.rankingFinished;
            } else {
                ws.rank = -1;
            }
            ws.result = e.result;
            ws.resultType = e.memTour.resultType;
            ws.tournamentID = e.memTour.tourID;
            ws.finished = e.isPlayerFinish();
            ws.countDeal = e.memTour.getNbDeal();
            ws.listPlayedDeals = new ArrayList<>(e.playedDeals);
            Collections.sort(ws.listPlayedDeals);
            return ws;
        }
        return null;
    }

    /**
     * Transform tournamentPlayer to tournament archive
     * @param e
     * @return
     */
    public <T extends TournamentPlayer> WSTournamentArchive toWSTournamentArchive(T e) {
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
            for (Deal d : e.getTournament().getDeals()) {
                ws.listPlayedDeals.add(d.getDealID(e.getTournament().getIDStr()));
            }
            Collections.sort(ws.listPlayedDeals);
            return ws;
        }
        return null;
    }

    /**
     * Return tournament played by player
     * @param session
     * @param offset
     * @param nbMax
     * @return
     */
    public <T extends TournamentPlayer> ResultListTournamentArchive listTournamentArchive(FBSession session, int offset, int nbMax) {
        long playerID = session.getPlayer().getID();
        ResultListTournamentArchive result = new ResultListTournamentArchive();
        result.archives = new ArrayList<>();
        Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (getConfigIntValue("resultArchiveNbDayBefore", 30)));
        long dateRef = dateBefore.getTimeInMillis();
        result.nbTotal = (int) getMongoTemplate().count(Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef))), getTournamentPlayerEntity());
        List<GenericMemTournamentPlayer> listMemTour = getMemoryMgr().listMemTournamentForPlayer(playerID, false, 0, dateRef);
        int nbTourInProgressPlayed = listMemTour.size();
        int offsetBDD = 0;
        result.nbTotal += nbTourInProgressPlayed;
        if (offset > nbTourInProgressPlayed) {
            // not used data from memory ...
            offsetBDD = offset - nbTourInProgressPlayed;
        }
        else if (!listMemTour.isEmpty()){
            for (GenericMemTournamentPlayer e : listMemTour) {
                if (result.archives.size() == nbMax) {
                    break;
                }
                WSTournamentArchive wst = toWSTournamentArchive(e);
                if (wst != null) {
                    result.archives.add(wst);
                }
            }
        }

        // if list not full, use data from BDD
        if (result.archives.size() < nbMax) {
            if (offsetBDD < 0) {
                offsetBDD = 0;
            }
            List<T> listTP = listTournamentPlayerAfterDate(playerID, dateRef, offsetBDD, nbMax - result.archives.size());
            if (listTP != null) {
                for (T tp :listTP) {
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

    /**
     * List tournamentPlayer for a player and after a date. Order by startDate desc
     * @param playerID
     * @param dateRef
     * @param offset
     * @param nbMax
     * @return
     */
    public <T extends TournamentPlayer> List<T> listTournamentPlayerAfterDate(long playerID, long dateRef, int offset, int nbMax) {
        return (List<T>) getMongoTemplate().find(
                Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef))).
                        skip(offset).
                        limit(nbMax).
                        with(new Sort(Sort.Direction.DESC, "startDate")),
                getTournamentPlayerEntity());
    }

    /**
     * List tournamentPlayer for a player. Order by startDate desc
     * @param playerID
     * @param offset
     * @param nbMax
     * @return
     */
    public <T extends TournamentPlayer> List<T> listTournamentPlayer(long playerID, int offset, int nbMax) {
        return (List<T>) getMongoTemplate().find(
                Query.query(Criteria.where("playerID").is(playerID)).
                        skip(offset).
                        limit(nbMax).
                        with(new Sort(Sort.Direction.DESC, "startDate")),
                getTournamentPlayerEntity());
    }

    /**
     * List tournamentPlayer for a tournament. Order by rank asc
     * @param tournamentID
     * @param offset
     * @param nbMax
     * @return
     */
    public <T extends TournamentPlayer> List<T> listTournamentPlayerForTournament(String tournamentID, int offset, int nbMax) {
        Query q= new Query();
        Criteria cTour = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
        q.addCriteria(cTour);
        if(nbMax > 0){
            q.limit(nbMax);
        }
        q.skip(offset);
        q.with(new Sort(Sort.Direction.ASC, "rank"));
        return (List<T>) getMongoTemplate().find(q, getTournamentPlayerEntity());
    }

    /**
     * Count nb tournamentPlayer for a tournament
     * @param tournamentID
     * @return
     */
    public int countTournamentPlayerForTournament(String tournamentID) {
        Query q= new Query();
        Criteria cTour = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
        q.addCriteria(cTour);
        return (int) getMongoTemplate().count(q, getTournamentPlayerEntity());
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
        Game game = getGame(gameID);
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

        if (!game.getTournament().isFinished()) {
            GenericMemTournament memTour = getMemoryMgr().getTournament(game.getTournament().getIDStr());
            if (memTour != null) {
                GenericMemDeal memDeal = memTour.getMemDeal(game.getDealID());
                if (memDeal != null) {
                    GenericMemDealPlayer memDealPlayer = memDeal.getResultPlayer(game.getPlayerID());
                    if (memDealPlayer != null) {
                        gameDeal.result = memDealPlayer.result;
                    }
                }
            }
        }

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
     * @param playerCache
     * @return
     * @throws FBWSException
     */
    public WSGameView viewGameForDealScoreAndContract(String dealID, int score, String contractString, String lead, PlayerCache playerCache) throws FBWSException {
        Tournament tour = getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        Deal deal = tour.getDeal(dealID);
        if (tour == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // check player played the deal of this game
        Game game = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
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
                game = getMongoTemplate().findOne(q, getGameEntity());
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
        gameView.tournament = toWSTournament(game.getTournament(), playerCache);

        return gameView;
    }

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
        List<WSResultDeal> listResultDeal = new ArrayList<>();
        GenericMemTournament mtr = getMemoryMgr().getTournament(tour.getIDStr());
        for (Deal deal : tour.getDeals()) {
            String dealID = deal.getDealID(tour.getIDStr());
            WSResultDeal resultPlayer = new WSResultDeal();
            resultPlayer.setDealIDstr(dealID);
            resultPlayer.setDealIndex(deal.index);
            resultPlayer.setResultType(tour.getResultType());
            if(deal instanceof PrivateDeal && ((PrivateDeal) deal).getChatroomID() != null){
                resultPlayer.setNbUnreadMessages(ContextManager.getPrivateTournamentMgr().getChatMgr().getNbUnreadMessagesForPlayerAndChatroom(playerID, ((PrivateDeal) deal).getChatroomID(), false));
            }
            // Tour in memory
            if (mtr != null) {
                GenericMemDeal memDeal = mtr.getMemDeal(dealID);
                if (memDeal != null) {
                    GenericMemDealPlayer memDealPlayer = memDeal.getResultPlayer(playerID);
                    if (memDealPlayer != null) {
                        resultPlayer = memDealPlayer.toWSResultDeal(memDeal);
                    } else {
                        resultPlayer.setPlayed(false);
                        resultPlayer.setRank(-1);
                    }
                }
            }
            // Tour not in memory => search in DB
            else {
                Game game = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerID);
                if (game != null) {
                    resultPlayer = game.toWSResultDeal();
                } else {
                    resultPlayer.setPlayed(false);
                    resultPlayer.setRank(-1);
                }
                resultPlayer.setNbTotalPlayer(tour.getNbPlayers());
            }
            listResultDeal.add(resultPlayer);
        }
        return listResultDeal;
    }

    /**
     * Count nb contract on tourID for deal
     * @param tourID
     * @param dealIndex
     * @param useLead
     * @return
     */
    public int countContractGroup(String tourID, int dealIndex, boolean useLead) {
        final List<GameGroupMapping> results = this.getResults(tourID, dealIndex, useLead);
        if (results != null) {
            return results.size();
        }
        return 0;
    }

    /**
     * Get the game groups for a deal in a tournament using the lead (or not)
     * @param tournamentID the tournament ID
     * @param dealIndex the deal index
     * @param useLead trueif the lead has to be used in the group operation
     * @return a list of GameGroupMapping (from the mongoDB)
     */
    public List<GameGroupMapping> getResults(final String tournamentID, final int dealIndex, final boolean useLead){
        // MongoDB query
        // Use aggregation. Respect the pipeline order : match, projection, group and sort
        GroupOperation groupOperation;
        MatchOperation matchOperation;
        ProjectionOperation projectionOperation;
        if (useLead) {
            groupOperation = Aggregation.group("score", "contract", "contractType", "declarer", "lead");
       } else {
            groupOperation = Aggregation.group("score", "contract", "contractType", "declarer");
        }
        matchOperation = Aggregation.match(Criteria.where("tournament.$id").is(new ObjectId(tournamentID)).andOperator(Criteria.where("dealIndex").is(dealIndex)));
        projectionOperation = Aggregation.project("score", "contract", "contractType", "declarer", "tricks", "result", "rank").and("cards").substring(0, 3).as("lead");
        groupOperation = groupOperation.addToSet("declarer").as("declarer").addToSet("tricks").as("tricks").addToSet("result").as("result").addToSet("rank").as("rank").count().as("nbPlayer");
        final TypedAggregation<? extends Game> aggGame = Aggregation.newAggregation(
                getAggregationGameEntity(),
                matchOperation,
                projectionOperation,
                groupOperation,
                Aggregation.sort(Sort.Direction.DESC, "score")
        );
        // Get the results
        final AggregationResults<GameGroupMapping> results = getMongoTemplate().aggregate(aggGame, GameGroupMapping.class);
        return results.getMappedResults();
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
            return (int) getMongoTemplate().count(q, getGameEntity());
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
            return (int) getMongoTemplate().count(q, getGameEntity());
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
            return (int) getMongoTemplate().count(q, getGameEntity());
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
            return (int) getMongoTemplate().count(q, getGameEntity());
        } catch (Exception e) {
            log.error("Failed to count game with same score and contract - tourID="+tourID+" - dealIndex="+dealIndex+" - score="+score+" - contract="+contract+" - contractType="+contractType, e);
        }
        return -1;
    }

    /**
     * Return results on deal for a player. Deal must be played by this player. Same contract are grouped. Separate game with different lead if the flag is enable
     * @param dealID
     * @param playerCache
     * @param useLead
     * @return
     * @throws FBWSException
     */
    public <T1 extends Game>WSResultDealTournament getWSResultDealTournamentGroupped(String dealID, PlayerCache playerCache, boolean useLead) throws FBWSException {
        Tournament tour = getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        Deal deal = tour.getDeal(dealID);
        if (deal == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // check deal played by this player
        Game gamePlayer = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
        if (gamePlayer == null) {
            log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
            throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
        }
        WSResultDealTournament resultDealTour = new WSResultDealTournament();
        //**************************************
        // TOURNAMENT Finished => data are in DB
        if (tour.isFinished()) {
            List<GameGroupMapping> listResultMapping = getResults(tour.getIDStr(), deal.index, useLead);
            if (listResultMapping == null) {
                log.error("listResultMapping is null !");
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
                if(FederationMgr.isCategoryFederation(tour.getCategory())){
                    result.setNbTotalPlayer(tour.getNbPlayersOnDeal(dealID));
                }
                result.setResultType(tour.getResultType());
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

            TournamentPlayer tp = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
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
            GenericMemTournament memTour = getMemoryMgr().getTournament(tour.getIDStr());
            if (memTour == null) {
                log.error("No memTour found for tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            GenericMemDeal memDeal = memTour.getMemDeal(dealID);
            if (memDeal == null) {
                log.error("No memDeal found for tour="+tour+" - dealID="+dealID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            int rankPlayer = -1;
            List<GenericMemDealPlayer> listResultMem = memDeal.getResultListOrderByScore();
            int nbTotalPlayer = listResultMem.size();
            // Map with nb game with same score and contract
            HashMap<String, Integer> mapRankNbPlayer = new HashMap<>(); /* Map to store the number of player for each score */
            for (GenericMemDealPlayer result : listResultMem) {
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
            for (GenericMemDealPlayer resPla : listResultMem) {
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
                    if (mapRankNbPlayer.get(key) != null) {
                        resDeal.setNbPlayerSameGame(mapRankNbPlayer.get(key));
                    }
                    resDeal.setNbTricks(resPla.nbTricks);
                    resDeal.setResultType(tour.getResultType());
                    resDeal.setScore(resPla.score);
                    resDeal.setResult(resPla.result);
                    resDeal.setLead(resPla.begins);
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
            resultDealTour.tournament = toWSTournament(tour, playerCache);
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_PLAYER_CONTRACT, playerContract));
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_INDEX_PLAYER, ""+idxPlayer));
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, ""+nbTotalPlayer));
            resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_DEAL_INDEX, ""+deal.index));
        }
        return resultDealTour;
    }

    /**
     * Return the number of player on tournament
     * @param tour
     * @param listFollower count only among these player
     * @param onlyFinisher
     * @return
     */
    public int getNbPlayersOnTournament(Tournament tour, List<Long> listFollower, boolean onlyFinisher) {
        if (tour.isFinished()) {
            if (listFollower != null && !listFollower.isEmpty()) {
                return (int) getMongoTemplate().count(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tour.getIDStr())).andOperator(Criteria.where("playerID").in(listFollower))), getTournamentPlayerEntity());
            } else {
                return tour.getNbPlayers();
            }
        } else {
            return getMemoryMgr().getNbPlayerOnTournament(tour.getIDStr(), listFollower, onlyFinisher);
        }
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
    public List<WSResultTournamentPlayer> getListWSResultTournamentPlayer(Tournament tour, int offset, int nbMaxResult, List<Long> listFollower, long playerAsk, boolean useRankingFinished, WSResultTournamentPlayer resultPlayerAsk) {
        List<WSResultTournamentPlayer> listResult = new ArrayList<>();
        GenericMemTournament memTour = getMemoryMgr().getTournament(tour.getIDStr());
        int idxPlayer = -1;
        if (memTour != null) {
            List<GenericMemTournamentPlayer> listRanking = memTour.getRanking(offset, nbMaxResult, listFollower, useRankingFinished);
            boolean includePlayerAskResult = false;
            if (resultPlayerAsk != null && resultPlayerAsk.getNbDealPlayed() < memTour.getNbDealsToPlay() && useRankingFinished) {
                // case of ranking with only players finished and player asked has not finished => include the player result in the list and increment nb player
                includePlayerAskResult = true;
            }
            boolean resultPlayerAskAdded = false;
            for (GenericMemTournamentPlayer e : listRanking) {
                // only player with at leat one deal finished
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
                if (!listResult.isEmpty()) {
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
            if (listFollower != null && !listFollower.isEmpty()) {
                cTour.andOperator(Criteria.where("playerID").in(listFollower));
            }
            q.addCriteria(cTour);
            q.limit(nbMaxResult).skip(offset);
            q.with(new Sort(Sort.Direction.ASC, "rank"));
            List<TournamentPlayer> listTP = (List<TournamentPlayer>) getMongoTemplate().find(q, getTournamentPlayerEntity());
            if (listTP != null && !listTP.isEmpty()) {
                for (TournamentPlayer tp : listTP) {
                    WSResultTournamentPlayer rp = tp.toWSResultTournamentPlayer(playerCacheMgr.getPlayerCache(tp.getPlayerID()), playerAsk);
                    if (rp.getPlayerID() == playerAsk) {
                        idxPlayer = listResult.size();
                    }
                    listResult.add(rp);
                }
            }
        }
        if (resultPlayerAsk != null && !listResult.isEmpty() && idxPlayer != -1) {
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
        Tournament tour = getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        Deal deal = tour.getDeal(dealID);
        if (tour == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // check deal played by this player
        Game gamePlayer = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
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
            List<Game> listGame = listFinishedGameOnTournamentAndDeal(tour.getIDStr(), deal.index, listPlaFilter, offset, nbMax);
            if (listGame != null) {
                for (Game tg : listGame) {
                    if (listResult.isEmpty()) {
                        if (tg.getRank() <= offsetPlayer) {
                            firstElementScoreBestCompareToPlayer = true;
                        }
                        if (offset < nbMax && tg.getScore() == gamePlayer.getScore()) {
                            firstElementScoreBestCompareToPlayer = true;
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
                    if(FederationMgr.isCategoryFederation(tour.getCategory())){
                        result.setNbTotalPlayer(tour.getNbPlayersOnDeal(dealID));
                    }
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
                    if(FederationMgr.isCategoryFederation(tour.getCategory())){
                        nbTotalPlayer = ((TourFederationDeal)deal).getNbPlayers();
                    }
                }
            }

        }
        //**************************************
        // TOURNAMENT NOT FINISHED => FIND RESULT IN MEMORY
        else {
            GenericMemTournament memTour = getMemoryMgr().getTournament(tour.getIDStr());
            if (memTour == null) {
                log.error("No memTour found for tour="+tour);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            GenericMemDeal memDeal = memTour.getMemDeal(dealID);
            if (memDeal == null) {
                log.error("No memDeal found for tour="+tour+" - dealID="+dealID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            GenericMemDealPlayer resPlayer = memDeal.getResultPlayer(playerCache.ID);
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
            List<GenericMemDealPlayer> listResultMem = memDeal.getResultListOrderByScore();
            if (nbMax > 0 && offset < listResultMem.size()) {
                listResultMem = listResultMem.subList(offset, (offset+nbMax) < listResultMem.size()?(offset+nbMax):listResultMem.size());
            }
            for (GenericMemDealPlayer resPla : listResultMem) {
                if (listResult.isEmpty()) {
                    if (resPla.nbPlayerBetterScore < offsetPlayer) {
                        firstElementScoreBestCompareToPlayer = true;
                    }
                    if (offset < nbMax && resPla.score == gamePlayer.getScore()) {
                        firstElementScoreBestCompareToPlayer = true;
                    }
                }
                if (listPlaFilter != null && !listPlaFilter.contains(resPla.playerID)) {
                    continue;
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
                Game gameToReplace = getFirstGameOnTournamentAndDealWithScoreAndContract(tour.getIDStr(), deal.index, gamePlayer.getScore(), gamePlayer.getContract(), gamePlayer.getContractType());
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
        else if (indexSameRank != -1 && idxPlayer == -1 && firstElementScoreBestCompareToPlayer && indexSameRank < listResult.size()) {
            // replace some data at this index with player data
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
     * Return the first game with these parameters
     * @param tourID
     * @param dealIndex
     * @param score
     * @param contract
     * @param contractType
     * @return
     */
    public Game getFirstGameOnTournamentAndDealWithScoreAndContract(String tourID, int dealIndex, int score, String contract, int contractType) {
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
            return getMongoTemplate().findOne(q, getGameEntity());
        } catch (Exception e) {
            log.error("Failed to find game for tournament deal and playerID", e);
        }
        return null;
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
        Game replayGame = createReplayGame(player.getID(), gamePlayed.getTournament(), gamePlayed.getDealIndex(), conventionProfil, conventionValue, cardsConventionProfil, cardsConventionValue);
        if (replayGame == null) {
            log.error("Failed to create replay game - player="+player+" - gamePlayed="+gamePlayed);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        replayGame.setReplay(true);
        synchronized (this) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                log.error("Exception to sleep to generate new replay gameID", e);
            }
            replayGame.setReplayGameID("" + System.currentTimeMillis());
        }
        replayGame.setConventionSelection(conventionProfil, conventionValue);
        replayGame.setCardsConventionSelection(cardsConventionProfil, cardsConventionValue);
        table.setGame(replayGame);
        return table;
    }

    public abstract Game createReplayGame(long playerID, Tournament tour, int dealIndex, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue);

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
            TypedAggregation aggGame = Aggregation.newAggregation(
                    getGameEntity(),
                    Aggregation.match(criteria),
                    Aggregation.group().avg("score").as("scoreAverage").count().as("nbScore")
            );
            AggregationResults<Document> results = getMongoTemplate().aggregate(aggGame, Document.class);
            if (!results.getMappedResults().isEmpty()) {
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
     * Build the resultDeal for the game most played
     * @param tourID
     * @param dealIndex
     * @return
     */
    private WSResultDeal buildWSMostPlayedResult(String tourID, int dealIndex, int nbPlayer, int resultType) {
        WSResultDeal result = new WSResultDeal();
        try {
            Criteria criteria = new Criteria().andOperator(Criteria.where("tournament.$id").is(new ObjectId(tourID)), Criteria.where("dealIndex").is(dealIndex), Criteria.where("finished").is(true));
            TypedAggregation aggGame = Aggregation.newAggregation(
                    getGameEntity(),
                    Aggregation.match(criteria),
                    Aggregation.project("score", "contract","contractType","declarer","tricks","result","rank").andExpression("substr($cards,0,3)").as("lead"),
                    Aggregation.group("score", "contract", "contractType").addToSet("declarer").as("declarer").addToSet("tricks").as("tricks").addToSet("result").as("result").addToSet("rank").as("rank").count().as("nbPlayer"),
                    Aggregation.sort(Sort.Direction.DESC, "nbPlayer"),
                    Aggregation.limit(1)
            );
            AggregationResults<GameGroupMapping> results = getMongoTemplate().aggregate(aggGame, GameGroupMapping.class);
            if (!results.getMappedResults().isEmpty()) {
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
        Game replayGame = session.getCurrentGameTable().getGame();
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
        Game originalGame = getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        if (originalGame == null) {
            log.error("Original game not found ! dealID="+dealID+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }

        int nbTotalPlayer = -1;
        double resultOriginal = 0;
        int rankOriginal = 0;
        GenericMemTournament memTournament = memoryMgr.getTournament(tourID);
        if (memTournament != null) {
            GenericMemDeal memDeal = memTournament.getMemDeal(dealID);
            if (memDeal == null) {
                log.error("No memDeal found for dealID="+dealID+" - on memTour="+memTournament);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            GenericMemDealPlayer memDealPlayer = memDeal.getResultPlayer(playerID);
            if (memDealPlayer == null) {
                log.error("No memDealPlayer found for player="+playerID+" on memTour="+memTournament+" - memDeal="+memDeal+" - replayGame="+replayGame);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            nbTotalPlayer = memDeal.getNbPlayers();
            resultOriginal = memDealPlayer.result;
            rankOriginal = memDealPlayer.nbPlayerBetterScore+1;
        } else {
            nbTotalPlayer = replayGame.getTournament().getNbPlayersOnDeal(dealID);
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
            } else if (replayGame.getTournament().getResultType() == Constantes.TOURNAMENT_RESULT_IMP && resultScoreAverage != null) {
                resultMP = Constantes.computeResultIMP(resultScoreAverage.scoreAverage, mostPlayedResult.getScore());
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

    public WSResultTournamentPlayer getWSResultTournamentPlayer(String tourID, PlayerCache playerCache, boolean useRankingFinished) {
        GenericMemTournament memTournament = memoryMgr.getTournament(tourID);
        if (memTournament != null) {
            return memTournament.getWSResultPlayer(playerCache, useRankingFinished);
        }
        TournamentPlayer tourPlayer = getTournamentPlayer(tourID, playerCache.ID);
        if (tourPlayer != null) {
            return tourPlayer.toWSResultTournamentPlayer(playerCache, playerCache.ID);
        }
        return null;
    }
}
