package com.funbridge.server.tournament.privatetournament;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.GenericChatroom;
import com.funbridge.server.message.data.MessageNotif;
import com.funbridge.server.message.data.MessageNotifGroup;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.texts.TextUIData;
import com.funbridge.server.tournament.DealGenerator;
import com.funbridge.server.tournament.IDealGeneratorCallback;
import com.funbridge.server.tournament.game.*;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.generic.memory.GenericMemDeal;
import com.funbridge.server.tournament.generic.memory.GenericMemDealPlayer;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer;
import com.funbridge.server.tournament.privatetournament.data.*;
import com.funbridge.server.tournament.privatetournament.memory.PrivateMemDeal;
import com.funbridge.server.tournament.privatetournament.memory.PrivateMemTournament;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.tournament.WSPrivateTournament;
import com.funbridge.server.ws.tournament.WSPrivateTournamentProperties;
import com.funbridge.server.ws.tournament.WSTableTournament;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.lock.LockWeakString;
import org.apache.commons.collections.ListUtils;
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
import java.util.Calendar;
import java.util.*;

/**
 * Created by pserent on 23/01/2017.
 */
@Component(value="privateTournamentMgr")
public class PrivateTournamentMgr extends TournamentGenericMgr implements IDealGeneratorCallback {

    @Resource(name="mongoPrivateTournamentTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name="privateTournamentChatMgr")
    private PrivateTournamentChatMgr chatMgr;
    @Resource(name="messageNotifMgr")
    private MessageNotifMgr notifMgr;
    @Resource(name="presenceMgr")
    private PresenceMgr presenceMgr;

    private Scheduler schedulerTask = null;
    private JobDetail taskStartup = null, taskFinish = null, taskRemove = null;
    public boolean startupTaskRunning = false, finishTaskRunning = false, removeTaskRunning = false;
    private LockWeakString lockCreateGame = new LockWeakString();
    private LockWeakString lockProperties = new LockWeakString();

    public static final String RECURRENCE_ONCE = "once", RECURRENCE_DAILY = "daily", RECURRENCE_WEEKLY = "weekly";
    public static final String ACCESS_RULE_PUBLIC = "public", ACCESS_RULE_PASSWORD = "password";

    public PrivateTournamentMgr() {
        super(Constantes.TOURNAMENT_CATEGORY_PRIVATE);
    }

    @Override
    @PostConstruct
    public void init() {
        log.info("init");
        super.init(PrivateMemTournament.class, GenericMemTournamentPlayer.class, PrivateMemDeal.class, GenericMemDealPlayer.class);
    }

    @Override
    @PreDestroy
    public void destroy() {
        log.info("destroy");
        if (schedulerTask != null) {
            try {
                schedulerTask.shutdown(true);
            } catch (Exception e) {
                log.error("Failed to stop scheduler", e);
            }
        }
        super.destroy();
    }

    @Override
    public void startUp() {
        log.info("startup");

        // load tournament not finished
        List<PrivateTournament> listTour = listTournamentNotFinished();
        if (listTour != null && listTour.size() > 0) {
            log.warn("Nb tournament PRIVATE to load : "+listTour.size());
            int nbLoadFromFile = 0, nbLoadFromDB = 0;
            // loop on each tournament
            for (PrivateTournament tour : listTour) {
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
                    }
                } else {
                    loadFromDB = true;
                }
                // load from DB
                if (loadFromDB) {
                    GenericMemTournament memTour = memoryMgr.addTournament(tour);
                    if (memTour != null) {
                        List<PrivateGame> listGame = listGameOnTournament(tour.getIDStr());
                        if (listGame != null) {
                            // load each game in memory
                            for (PrivateGame tourGame : listGame) {
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
            log.warn("Nb Tournament loaded : "+listTour.size()+" - loadFromFile="+nbLoadFromFile+" - loadFromDB="+nbLoadFromDB);
        } else {
            log.warn("No tournament PRIVATE to load");
        }

        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            schedulerTask = schedulerFactory.getScheduler();

            // Startup task - trigger every 5 mn
            taskStartup = JobBuilder.newJob(PrivateTournamentStartupTask.class).withIdentity("taskStartup", "PrivateTournament").build();
            CronTrigger triggerStartup = TriggerBuilder.newTrigger().withIdentity("triggerStartupTask", "PrivateTournament").withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?")).build();
            Date dateNextJobEnable = schedulerTask.scheduleJob(taskStartup, triggerStartup);
            log.warn("Sheduled for job=" + taskStartup.getKey() + " run at="+dateNextJobEnable+" - cron expression=" + triggerStartup.getCronExpression() + " - next fire=" + triggerStartup.getNextFireTime());

            // finish task - trigger every 5 mn
            taskFinish = JobBuilder.newJob(PrivateTournamentFinishTask.class).withIdentity("taskFinish", "PrivateTournament").build();
            CronTrigger triggerFinish = TriggerBuilder.newTrigger().withIdentity("triggerFinishTask", "PrivateTournament").withSchedule(CronScheduleBuilder.cronSchedule("0 0/5 * * * ?")).build();
            Date dateNextJobFinish = schedulerTask.scheduleJob(taskFinish, triggerFinish);
            log.warn("Sheduled for job=" + taskFinish.getKey() + " run at="+dateNextJobFinish+" - cron expression=" + triggerFinish.getCronExpression() + " - next fire=" + triggerFinish.getNextFireTime());

            // remove task - trigger every day
            taskRemove = JobBuilder.newJob(PrivateTournamentRemoveTask.class).withIdentity("taskRemove", "PrivateTournament").build();
            CronTrigger triggerRemove = TriggerBuilder.newTrigger().withIdentity("triggerRemoveTask", "PrivateTournament").withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?")).build();
            Date dateNextJobRemove = schedulerTask.scheduleJob(taskRemove, triggerRemove);
            log.warn("Sheduled for job=" + taskFinish.getKey() + " run at="+dateNextJobRemove+" - cron expression=" + triggerFinish.getCronExpression() + " - next fire=" + triggerFinish.getNextFireTime());

            // start scheduler
            schedulerTask.start();

        } catch (Exception e) {
            log.error("Exception to init scheduler", e);
        }
    }

    /**
     * Return next TS for startup JOB
     * @return
     */
    public long getDateNextJobStartup() {
        try {
            Trigger trigger = schedulerTask.getTrigger(TriggerKey.triggerKey("triggerStartupTask", "PrivateTournament"));
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
     * @return
     */
    public long getDateNextJobFinish() {
        try {
            Trigger trigger = schedulerTask.getTrigger(TriggerKey.triggerKey("triggerFinishTask", "PrivateTournament"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    /**
     * Return next TS for remove JOB
     * @return
     */
    public long getDateNextJobRemove() {
        try {
            Trigger trigger = schedulerTask.getTrigger(TriggerKey.triggerKey("triggerRemoveTask", "PrivateTournament"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    public PrivateTournamentChatMgr getChatMgr(){
        return chatMgr;
    }

    @Override
    public void updateTournamentDB(Tournament tour) throws FBWSException{
        if (tour != null && tour instanceof PrivateTournament) {
            PrivateTournament tourPrivate = (PrivateTournament)tour;
            try {
                mongoTemplate.save(tourPrivate);
            } catch (Exception e) {
                log.error("Exception to save tour=" + tour, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        } else {
            log.error("Tour not valid ! tour="+tour);
        }
    }

    @Override
    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    @Override
    public void checkTournamentMode(boolean isReplay, long playerId) throws FBWSException {}

    public Class<? extends Game> getGameEntity() {
        return PrivateGame.class;
    }

    public Class<PrivateTournament> getTournamentEntity() {
        return PrivateTournament.class;
    }

    public Class<PrivateTournamentPlayer> getTournamentPlayerEntity() {
        return PrivateTournamentPlayer.class;
    }

    @Override
    public String getGameCollectionName() {
        return "private_game";
    }

    @Override
    public void updateGameFinished(FBSession session) throws FBWSException {
        if (session == null) {
            log.error("Session parameter is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (session.getCurrentGameTable() == null || session.getCurrentGameTable().getGame() == null || !(session.getCurrentGameTable().getGame() instanceof PrivateGame)) {
            log.error("Table or game in session not valid ! - table="+session.getCurrentGameTable());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        PrivateGame game = (PrivateGame)session.getCurrentGameTable().getGame();
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
     * Transform tournament to WS object
     * @param tournament
     * @param playerID
     * @return
     */
    public WSPrivateTournament toWSPrivateTournament(PrivateTournament tournament, long playerID, List<Long> listFriendID) {
        WSPrivateTournament ws = new WSPrivateTournament();
        if (tournament != null) {
            ws.tournamentID = tournament.getIDStr();
            ws.owner = playerMgr.playerToWSPlayerLight(tournament.getOwnerID(), playerID);
            GenericMemTournament memTournament = getMemoryMgr().getTournament(tournament.getIDStr());
            if (memTournament != null) {
                GenericMemTournamentPlayer memTournamentPlayer = memTournament.getTournamentPlayer(playerID);
                if (memTournamentPlayer != null) {
                    ws.listPlayedDeals.addAll(memTournamentPlayer.playedDeals);
                    Collections.sort(ws.listPlayedDeals);
                }
                if (ws.listPlayedDeals.size() > 0) {
                    ws.playerStarted = true;
                }
                else {
                    ws.playerStarted = !listGameOnTournamentForPlayer(tournament.getIDStr(), playerID).isEmpty();
                }
                ws.nbPlayers = memTournament.getListPlayer().size();
                ws.nbFriends = ListUtils.intersection(memTournament.getListPlayer(), listFriendID).size();
            } else if (tournament.isFinished()){
                ws.playerStarted = true;
                for (Deal d : tournament.getDeals()) {
                    ws.listPlayedDeals.add(d.getDealID(tournament.getIDStr()));
                }
                ws.nbPlayers = tournament.getNbPlayers();
            }
            PrivateTournamentProperties properties = getPrivateTournamentProperties(tournament.getPropertiesID());
            if (properties != null) {
                ws.properties = toWSPrivateTournamentProperties(properties, listFriendID);
                ws.favorite = properties.isPlayerFavorite(playerID);
                if (properties.getAccessRule() != null && properties.getAccessRule().equals(ACCESS_RULE_PASSWORD)) {
                    if (tournament.getOwnerID() == playerID) {
                        ws.accessGranted = true;
                    } else {
                        if (memTournament != null) {
                            if (memTournament.getTournamentPlayer(playerID) != null) {
                                ws.accessGranted = true;
                            }
                        } else {
                            PrivateTournamentPlayer privateTournamentPlayer = getTournamentPlayer(tournament.getIDStr(), playerID);
                            if (privateTournamentPlayer != null) {
                                ws.accessGranted = true;
                            }
                        }
                    }
                }
            }
            ws.startDate = tournament.getStartDate();
            ws.endDate = tournament.getEndDate();
            if(tournament.getChatroomID() != null){
                ws.nbUnreadMessages = chatMgr.getNbUnreadMessagesForPlayerAndChatroom(playerID, tournament.getChatroomID(), false);
                for(PrivateDeal deal : tournament.getDeals()){
                    if(deal.getChatroomID() != null){
                        ws.nbUnreadMessages += chatMgr.getNbUnreadMessagesForPlayerAndChatroom(playerID, deal.getChatroomID(), false);
                    }
                }
            }
        }
        return ws;
    }

    /**
     * Transform tournament to WS object
     * @param memTournament
     * @param playerID
     * @param listFriendID
     * @return
     */
    public WSPrivateTournament toWSPrivateTournament(GenericMemTournament memTournament, long playerID, List<Long> listFriendID) {
        WSPrivateTournament ws = new WSPrivateTournament();
        if (memTournament != null) {
            // TODO add field to genericMemTournament to avoid load tournament from DB !
            PrivateTournament tournament = (PrivateTournament)getTournament(memTournament.tourID);
            if (tournament != null) {
                ws.tournamentID = memTournament.tourID;
                ws.owner = playerMgr.playerToWSPlayerLight(tournament.getOwnerID(), playerID);
                GenericMemTournamentPlayer memTournamentPlayer = memTournament.getTournamentPlayer(playerID);
                if (memTournamentPlayer != null) {
                    ws.listPlayedDeals.addAll(memTournamentPlayer.playedDeals);
                    Collections.sort(ws.listPlayedDeals);
                }
                if (ws.listPlayedDeals.size() > 0) {
                    ws.playerStarted = true;
                } else {
                    ws.playerStarted = !listGameOnTournamentForPlayer(tournament.getIDStr(), playerID).isEmpty();
                }
                ws.nbPlayers = memTournament.getListPlayer().size();
                ws.nbFriends = ListUtils.intersection(memTournament.getListPlayer(), listFriendID).size();
                PrivateTournamentProperties properties = getPrivateTournamentProperties(tournament.getPropertiesID());
                if (properties != null) {
                    ws.properties = toWSPrivateTournamentProperties(properties, listFriendID);
                    ws.favorite = properties.isPlayerFavorite(playerID);
                    if (properties.getAccessRule() != null && properties.getAccessRule().equals(ACCESS_RULE_PASSWORD)) {
                        if (tournament.getOwnerID() == playerID) {
                            ws.accessGranted = true;
                        } else {
                            if (memTournament != null) {
                                if (memTournament.getTournamentPlayer(playerID) != null) {
                                    ws.accessGranted = true;
                                }
                            } else {
                                PrivateTournamentPlayer privateTournamentPlayer = getTournamentPlayer(tournament.getIDStr(), playerID);
                                if (privateTournamentPlayer != null) {
                                    ws.accessGranted = true;
                                }
                            }
                        }
                    }
                }
                ws.startDate = tournament.getStartDate();
                ws.endDate = tournament.getEndDate();
                if(tournament.getChatroomID() != null){
                    ws.nbUnreadMessages = chatMgr.getNbUnreadMessagesForPlayerAndChatroom(playerID, tournament.getChatroomID(), false);
                    for(PrivateDeal deal : tournament.getDeals()){
                        if(deal.getChatroomID() != null){
                            ws.nbUnreadMessages += chatMgr.getNbUnreadMessagesForPlayerAndChatroom(playerID, deal.getChatroomID(), false);
                        }
                    }
                }
            }
        }
        return ws;
    }

    /**
     * Transform properties to WS object
     * @param properties
     * @return
     */
    public WSPrivateTournamentProperties toWSPrivateTournamentProperties(PrivateTournamentProperties properties, List<Long> listFriendID) {
        WSPrivateTournamentProperties ws = new WSPrivateTournamentProperties();
        if (properties != null) {
            ws.propertiesID = properties.getIDStr();
            ws.name = properties.getName();
            ws.nbDeals = properties.getNbDeals();
            ws.rankingType = properties.getRankingType();
            ws.duration = properties.getDuration();
            ws.recurrence = properties.getRecurrence();
            ws.accessRule = properties.getAccessRule();
            ws.startDate = properties.getStartDate();
            ws.description = properties.getDescription();
            ws.nbPlayersFavorite = properties.getPlayersFavorite().size();
            if (listFriendID != null && listFriendID.size() > 0) {
                ws.nbFriendsFavorite = ListUtils.intersection(listFriendID, properties.getPlayersFavorite()).size();
            }
        }
        return ws;
    }

    /**
     * List of properties favorite for player. Only properties enable are selected.
     * @param playerID
     * @return
     */
    public List<PrivateTournamentProperties> listPropertiesFavoriteForPlayer(long playerID) {
        return mongoTemplate.find(Query.query(Criteria.where("playersFavorite").is(playerID).andOperator(Criteria.where("enable").is(true))), PrivateTournamentProperties.class);
    }

    public class ResultListTournamentForPlayer {
        public List<WSPrivateTournament> tournaments = new ArrayList<>();
        public int tournamentsTotalSize = 0;
    }

    /**
     * List tournament where player has set as favorite and tournament not finished !
     * @param playerID
     * @param offset
     * @param nbMax
     * @return
     */
    public ResultListTournamentForPlayer listTournamentForPlayer(long playerID, int offset, int nbMax) {
        ResultListTournamentForPlayer result = new ResultListTournamentForPlayer();
        List<Long> listFriendID = playerMgr.listFriendIDForPlayer(playerID);
        List<PrivateTournamentProperties> favorites = listPropertiesFavoriteForPlayer(playerID);
        List<String> listFavoritesID = new ArrayList<>();
        for (PrivateTournamentProperties e : favorites) {
            listFavoritesID.add(e.getIDStr());
        }

        // Total size
        TypedAggregation<PrivateTournament> aggCount = Aggregation.newAggregation(
                PrivateTournament.class,
                Aggregation.match(Criteria.where("propertiesID").in(listFavoritesID).andOperator(Criteria.where("endDate").gt(System.currentTimeMillis()))),
                Aggregation.group("propertiesID"),
                Aggregation.group().count().as("count")
        );
        AggregationResults<Document> aggCountResults = mongoTemplate.aggregate(aggCount, Document.class);
        if (aggCountResults != null && aggCountResults.getMappedResults().size() == 1) {
            result.tournamentsTotalSize = aggCountResults.getUniqueMappedResult().getInteger("count");
        }

        // get result
        List<String> listTourID = new ArrayList<>();
        TypedAggregation<PrivateTournament> aggResult = Aggregation.newAggregation(
                PrivateTournament.class,
                Aggregation.match(Criteria.where("propertiesID").in(listFavoritesID).andOperator(Criteria.where("endDate").gt(System.currentTimeMillis()))),
                Aggregation.sort(Sort.Direction.ASC, "startDate"),
                Aggregation.project("propertiesID").and(Aggregation.ROOT).as("tour"),
                Aggregation.group("propertiesID").first("tour").as("tour"),
                Aggregation.sort(Sort.Direction.ASC, "tour.endDate"),
                Aggregation.skip((long)offset),
                Aggregation.limit(nbMax)
        );
        AggregationResults<Document> aggResultResults = mongoTemplate.aggregate(aggResult, Document.class);
        if (aggResultResults != null) {
            for (Document e : aggResultResults.getMappedResults()) {
                PrivateTournament tour = mongoTemplate.getConverter().read(PrivateTournament.class, (Document)e.get("tour"));
                result.tournaments.add(toWSPrivateTournament(tour, playerID, listFriendID));
                listTourID.add(tour.getIDStr());
            }
        }

        // add tournament in progress
        List<GenericMemTournament> listMemTournaments = memoryMgr.listMemTournamentInProgressForPlayer(playerID);
        if (listMemTournaments != null && listMemTournaments.size() > 0) {
            for (GenericMemTournament e : listMemTournaments) {
                if (!listTourID.contains(e.tourID)) {
                    result.tournaments.add(toWSPrivateTournament(e, playerID, listFriendID));
                    result.tournamentsTotalSize ++;
                }
            }

            Collections.sort(result.tournaments, new Comparator<WSPrivateTournament>() {
                @Override
                public int compare(WSPrivateTournament o1, WSPrivateTournament o2) {
                    // these 2 tournaments are not started
                    if (o1.startDate > System.currentTimeMillis() && o2.startDate > System.currentTimeMillis()) {
                        return Long.compare(o1.startDate, o2.startDate);
                    }
                    // o2 is already started and not o1 => o2 is before o1
                    if (o1.startDate > System.currentTimeMillis() && o2.startDate < System.currentTimeMillis()) {
                        return 1;
                    }
                    // o1 is already started and not o2 => o1 is before o2
                    if (o1.startDate < System.currentTimeMillis() && o2.startDate > System.currentTimeMillis()) {
                        return -1;
                    }
                    // these 2 tournament are started => compare endate
                    return Long.compare(o1.endDate, o2.endDate);
                }
            });
        }
        if (result.tournaments.size() > nbMax) {
            result.tournaments = result.tournaments.subList(0, nbMax);
        }

        return result;
    }

    public class ListTournamentResult {
        public List<PrivateTournament> tournamentList;
        public int count;
    }

    /**
     * List tournaments
     * @param search
     * @param nbDealMin
     * @param nbDealMax
     * @param rankingType
     * @param accessRule
     * @param countryCode
     * @param favorite
     * @param playerID
     * @param offset
     * @param nbMax
     * @return
     */
    public ListTournamentResult listTournament(String search, long ownerID, int nbDealMin, int nbDealMax, int rankingType, String accessRule, String countryCode, boolean favorite, long playerID, int offset, int nbMax) {
        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("endDate").gt(System.currentTimeMillis()));
        // search
        if (search != null && search.length() > 0) {
            List<Player> players = playerMgr.searchPlayerStartingPseudoOrName(search);
            List<Long> playersID = new ArrayList<>();
            for (Player p : players) {
                playersID.add(p.getID());
            }
            Criteria criteriaSearch = new Criteria().orOperator(Criteria.where("ownerID").in(playersID), Criteria.where("name").regex(search, "i"));
            criteriaList.add(criteriaSearch);
        }
        if (ownerID != 0) {
            criteriaList.add(Criteria.where("ownerID").is(ownerID));
        }
        // dealMin & dealMax
        if (nbDealMin > 0 && nbDealMax == 0) {
            criteriaList.add(Criteria.where("countDeals").gt(nbDealMin));
        } else if (nbDealMin > 0 && nbDealMax > 0) {
            criteriaList.add(Criteria.where("countDeals").gte(nbDealMin).lte(nbDealMax));
        } else if (nbDealMax > 0) {
            criteriaList.add(Criteria.where("countDeals").lt(nbDealMax));
        }
        // rankingType
        if (rankingType > 0) {
            criteriaList.add(Criteria.where("resultType").is(rankingType));
        }
        // access rule
        if (accessRule != null && accessRule.length() > 0) {
            criteriaList.add(Criteria.where("accessRule").is(accessRule));
        }
        // country code
        if (countryCode != null && countryCode.length() > 0) {
            criteriaList.add(Criteria.where("countryCode").is(countryCode));
        }
        // favorite
        if (favorite) {
            List<PrivateTournamentProperties> playerFavorites = listPropertiesFavoriteForPlayer(playerID);
            List<String> playerFavoritesID = new ArrayList<>();
            for (PrivateTournamentProperties e : playerFavorites) {
                playerFavoritesID.add(e.getIDStr());
            }
            criteriaList.add(Criteria.where("propertiesID").in(playerFavoritesID));
        }

        ListTournamentResult result = new ListTournamentResult();
        Query q = new Query();
        if (criteriaList.size() == 1) {
            q.addCriteria(criteriaList.get(0));
        } else {
            q.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()])));
        }
        result.count = (int)mongoTemplate.count(q, PrivateTournament.class);
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        q.with(new Sort(Sort.Direction.ASC, "startDate"));
        result.tournamentList = mongoTemplate.find(q, PrivateTournament.class);
        return result;
    }

    public PrivateTournamentProperties getPrivateTournamentProperties(String id) {
        return mongoTemplate.findById(new ObjectId(id), PrivateTournamentProperties.class);
    }

    public List<PrivateTournamentProperties> getPrivateTournamentPropertiesForOwner(long ownerID, boolean onlyEnable) {
        Query query = Query.query(Criteria.where("ownerID").is(ownerID));
        if (onlyEnable) {
            query.addCriteria(Criteria.where("enable").is(true));
            query.addCriteria(Criteria.where("tsSetRemove").is(0));
        }
        query.with(new Sort(Sort.Direction.ASC, "createDateISO"));
        return mongoTemplate.find(query, PrivateTournamentProperties.class);
    }

    /**
     * Return tournament with name
     * @param name
     * @return
     */
    public PrivateTournamentProperties getTournamentPropertiesWithName(String name) {
        return mongoTemplate.findOne(Query.query(Criteria.where("name").regex("^"+name+"$", "i").andOperator(Criteria.where("enable").is(true))), PrivateTournamentProperties.class);
    }

    /**
     * Count properties enable created by player
     * @param playerID
     * @return
     */
    public int countPropertiesEnableForOwner(long playerID) {
        try {
            List<Criteria> criterias = new ArrayList<>();
            criterias.add(Criteria.where("ownerID").is(playerID));
            criterias.add(Criteria.where("enable").is(true));
            criterias.add(Criteria.where("tsSetRemove").is(0));
            Query query = new Query(new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()])));
            return (int) mongoTemplate.count(query, PrivateTournamentProperties.class);
        } catch (Exception e) {
            log.error("Failed to count properties enable for playerID=" + playerID, e);
        }
        return -1;
    }

    /**
     * List all properties enable created by player
     * @param playerID
     * @return
     */
    public List<PrivateTournamentProperties> listPropertiesEnableForOwner(long playerID) {
        try {
            List<Criteria> criterias = new ArrayList<>();
            criterias.add(Criteria.where("ownerID").is(playerID));
            criterias.add(Criteria.where("enable").is(true));
            criterias.add(Criteria.where("tsSetRemove").is(0));
            Query query = new Query(new Criteria().andOperator(criterias.toArray(new Criteria[criterias.size()])));
            query.with(new Sort(Sort.Direction.ASC, "creationDateISO"));
            return mongoTemplate.find(query, PrivateTournamentProperties.class);
        } catch (Exception e) {
            log.error("Failed to count properties enable for playerID=" + playerID, e);
        }
        return null;
    }

    /**
     * Count all available tournaments
     * @return
     */
    public int countTournamentAvailable() {
        Query q = Query.query(Criteria.where("endDate").gt(System.currentTimeMillis()));
        return (int)mongoTemplate.count(q, PrivateTournament.class);
    }

    /**
     * Check if player credit is valid for create tournament
     * @param dateSubscriptionExpiration
     * @param creditDealsRemaining
     * @return
     */
    public boolean isCreditValidForPlayer(long dateSubscriptionExpiration , int creditDealsRemaining) {
        if (dateSubscriptionExpiration > 0 && dateSubscriptionExpiration > System.currentTimeMillis()) {
            return true;
        }
        return creditDealsRemaining > getConfigIntValue("creditDealsRemaining", 100);
    }

    /**
     * Create a new private tournament properties
     * @param wsPrivateTournamentProperties
     * @param owner
     * @return
     */
    public PrivateTournamentProperties createProperties(WSPrivateTournamentProperties wsPrivateTournamentProperties, Player owner) throws FBWSException {
        if (wsPrivateTournamentProperties != null && owner != null) {
            // check player has not too many private tournament
            int nbPropertiesForPlayer = countPropertiesEnableForOwner(owner.getID());
            if (nbPropertiesForPlayer > getConfigIntValue("nbMaxTournamentByOwner", 3)) {
                if (log.isDebugEnabled()) {
                    log.debug("Nb tournament created for ownerID=" + owner.getID() + " - nbPropertiesForPlayer=" + nbPropertiesForPlayer);
                }
                throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_TOO_MANY_CREATION);

            }
            // check name not yet existing
            if (getTournamentPropertiesWithName(wsPrivateTournamentProperties.name) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Tournament properties with name:" + wsPrivateTournamentProperties.name + " already existing ! - properties=" + wsPrivateTournamentProperties + " - existing=" + getTournamentPropertiesWithName(wsPrivateTournamentProperties.name));
                }
                throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_NAME_ALREADY_USED);
            }
            // check date
            if (wsPrivateTournamentProperties.startDate < System.currentTimeMillis()) {
                log.error("Start date is not valid (< current time) - properties="+wsPrivateTournamentProperties+" - owner="+owner);
                throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_INVALID_DATE);
            }

            PrivateTournamentProperties properties = new PrivateTournamentProperties();
            properties.setName(wsPrivateTournamentProperties.name);
            properties.setNbDeals(wsPrivateTournamentProperties.nbDeals);
            properties.setRankingType(wsPrivateTournamentProperties.rankingType);
            properties.setStartDate(wsPrivateTournamentProperties.startDate);
            properties.setDuration(wsPrivateTournamentProperties.duration);
            properties.setOwnerID(owner.getID());
            properties.setRecurrence(wsPrivateTournamentProperties.recurrence);
            properties.setAccessRule(wsPrivateTournamentProperties.accessRule);
            properties.setPassword(wsPrivateTournamentProperties.password);
            properties.addPlayerFavorite(owner.getID());
            properties.setCountryCode(owner.getDisplayCountryCode());
            properties.setDescription(wsPrivateTournamentProperties.description);
            mongoTemplate.insert(properties);

            // create the next tournament
            createPrivateTournament(properties, true);
            return properties;
        } else {
            log.error("Parameter properties or owner are null");
        }
        return null;
    }

    /**
     * Create a new private tournament properties for funbridge
     * @param wsPrivateTournamentProperties
     * @return
     */
    public PrivateTournamentProperties createPropertiesForFunbridge(WSPrivateTournamentProperties wsPrivateTournamentProperties) throws FBWSException {
        if (wsPrivateTournamentProperties != null) {
            // check name not yet existing
            if (getTournamentPropertiesWithName(wsPrivateTournamentProperties.name) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Tournament properties with name:" + wsPrivateTournamentProperties.name + " already existing ! - properties=" + wsPrivateTournamentProperties + " - existing=" + getTournamentPropertiesWithName(wsPrivateTournamentProperties.name));
                }
                throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_NAME_ALREADY_USED);
            }
            // check date
            if (wsPrivateTournamentProperties.startDate < System.currentTimeMillis()) {
                log.error("Start date is not valid (< current time) - properties="+wsPrivateTournamentProperties);
                throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_INVALID_DATE);
            }

            PrivateTournamentProperties properties = new PrivateTournamentProperties();
            properties.setName(wsPrivateTournamentProperties.name);
            properties.setNbDeals(wsPrivateTournamentProperties.nbDeals);
            properties.setRankingType(wsPrivateTournamentProperties.rankingType);
            properties.setStartDate(wsPrivateTournamentProperties.startDate);
            properties.setDuration(wsPrivateTournamentProperties.duration);
            properties.setOwnerID(Constantes.PLAYER_FUNBRIDGE_ID);
            properties.setRecurrence(wsPrivateTournamentProperties.recurrence);
            properties.setAccessRule(wsPrivateTournamentProperties.accessRule);
            properties.setPassword(wsPrivateTournamentProperties.password);
            properties.setCountryCode(Constantes.PLAYER_FUNBRIDGE_COUNTRY);
            properties.setDescription(wsPrivateTournamentProperties.description);
            mongoTemplate.insert(properties);

            // create the next tournament
            createPrivateTournament(properties, true);
            return properties;
        } else {
            log.error("Parameter properties or owner are null");
        }
        return null;
    }

    /**
     * Remove properties => remove tournament not yet in progress and set flag on proerties to remove it later
     * @param propertiesID
     * @param playerID
     * @return
     * @throws FBWSException
     */
    public boolean removeProperties(String propertiesID, long playerID) throws FBWSException {
        PrivateTournamentProperties properties = getPrivateTournamentProperties(propertiesID);
        if (properties == null) {
            log.error("No properties found for ID="+propertiesID);
            throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_PROPERTIES_NOT_FOUND);
        }
        if (playerID != Constantes.PLAYER_FUNBRIDGE_ID && properties.getOwnerID() != playerID) {
            log.error("Properties owner is not player="+playerID+" - properties="+properties);
            throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_OWNER_NOT_PLAYER);
        }
        synchronized (lockProperties.getLock(propertiesID)) {
            List<PrivateTournament> tours = getCurrentAndNextTournamentForProperties(propertiesID);
            if (tours == null) {
                log.error("Error tour list is null ! - propertiesID=" + propertiesID + " - playerID=" + playerID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            // remove tournament not yet in progress
            boolean propertiesInUse = false;
            for (PrivateTournament t : tours) {
                if (t.getStartDate() > (System.currentTimeMillis() + Constantes.TIMESTAMP_MINUTE)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Tournament not yet started => remove tournament=" + t);
                    }
                    memoryMgr.removeTournament(t.getIDStr());
                    mongoTemplate.remove(t);
                } else {
                    propertiesInUse = true;
                }
            }
            if (!propertiesInUse) {
                Query q = Query.query(Criteria.where("propertiesID").is(propertiesID).andOperator(Criteria.where("finished").is(true)));
                long nbTournamentWithProperties = mongoTemplate.count(q, PrivateTournament.class);
                if (nbTournamentWithProperties > 0) {
                    propertiesInUse = true;
                }
            }
            if (propertiesInUse) {
                // set TsSetRemove with current TS to indicate that this properties must be remove after the last tournament finished
                properties.setTsSetRemove(System.currentTimeMillis());
                properties.setEnable(false);
                mongoTemplate.save(properties);
            } else {
                // no tournament in progress for this properties => remove it
                mongoTemplate.remove(properties);
            }
        }
        return true;
    }

    /**
     * Remove all private tournaments created by the player
     * @param playerID
     * @return
     */
    public void removePrivateTournament(long playerID){
        List<PrivateTournament> tours = getAllTournamentByPlayer(playerID);

        // remove tournament not yet in progress
        for (PrivateTournament t : tours) {
                memoryMgr.removeTournament(t.getIDStr());
                mongoTemplate.remove(t);
        }
    }


    /**
     * Create tournament associated to this properties object
     * @param properties
     * @param firstCreation
     * @return
     */
    public PrivateTournament createPrivateTournament(PrivateTournamentProperties properties, boolean firstCreation) {
        if (properties != null) {
            if (!properties.isValid()) {
                log.error("Properties not valid ! properties="+properties);
            } else {
                DealGenerator.DealGenerate[] tabDeal = dealGenerator.generateDeal(properties.getNbDeals(), 0);
                if (tabDeal != null && tabDeal.length == properties.getNbDeals()) {
                    Calendar calendar = Calendar.getInstance();
                    long startDate = getNextStartDateForProperties(properties, firstCreation);
                    if (startDate == 0) {
                        log.error("Failed to get start date for properties="+properties);
                    } else {
                        PrivateTournament tournament = getTournamentWithPropertiesAndStartDate(properties.getIDStr(), startDate);
                        if (tournament == null) {
                            calendar.setTimeInMillis(startDate);
                            tournament = new PrivateTournament();
                            tournament.setStartDate(calendar.getTimeInMillis());
                            calendar.add(Calendar.MINUTE, properties.getDuration());
                            tournament.setEndDate(calendar.getTimeInMillis());
                            tournament.setName(properties.getName());
                            tournament.setOwnerID(properties.getOwnerID());
                            tournament.setPropertiesID(properties.getIDStr());
                            tournament.setCreationDateISO(new Date());
                            tournament.setResultType(properties.getRankingType());
                            tournament.setDealGenerate(tabDeal);
                            tournament.setCountryCode(properties.getCountryCode());
                            tournament.setAccessRule(properties.getAccessRule());

                            // create chatrooms
                            GenericChatroom generalChatroom = chatMgr.createChatroom(null, "general", null, Constantes.CHATROOM_TYPE_TOURNAMENT);
                            tournament.setChatroomID(generalChatroom.getIDStr());

                            for(PrivateDeal deal : tournament.getDeals()){
                                GenericChatroom dealChatroom = chatMgr.createChatroom(null, "deal_comment:" + deal.index, null, Constantes.CHATROOM_TYPE_TOURNAMENT);
                                deal.setChatroomID(dealChatroom.getIDStr());
                            }

                            // persist date
                            mongoTemplate.insert(tournament);
                            if (log.isDebugEnabled()) {
                                log.debug("Success to create tournament for properties=" + properties + " - tour=" + tournament);
                            }

                            return tournament;
                        } else {
                            log.error("Tournament already exist for properties=["+properties+"] and startDate="+Constantes.timestamp2StringDateHour(startDate)+" - tournament="+tournament);
                        }
                    }
                } else {
                    log.error("Tab deal generate is null ! - properties="+properties);
                }
            }
        } else {
            log.error("Properties parameter is null !");
        }
        return null;
    }

    /**
     * Compute next startDate for properties
     * @param properties
     * @param firstCreation
     * @return
     */
    public long getNextStartDateForProperties(PrivateTournamentProperties properties, boolean firstCreation) {
        if (properties != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(properties.getStartDate());
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.set(Calendar.SECOND, 0);

            if (!firstCreation) {
                // DAILY => return the next future date (same hour & minute)
                if (properties.getRecurrence().equals(RECURRENCE_DAILY)) {
                    while (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                    }
                }
                // WEEKLY => return the next future date (same dayOfWeek, hour & minute)
                if (properties.getRecurrence().equals(RECURRENCE_WEEKLY)) {
                    while (calendar.getTimeInMillis() < System.currentTimeMillis() || (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek)) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                    }
                }
                // ONCE => nothing to do, only first creation !!
            }
            if (log.isDebugEnabled()) {
                log.debug("NextStartDate for properties="+properties+" - firstCreation="+firstCreation+" - next date="+Constantes.timestamp2StringDateHour(calendar.getTimeInMillis()));
            }
            return calendar.getTimeInMillis();
        }
        log.error("Properties param is null !");
        return 0;
    }

    class StartupTournamentsResult {
        List<PrivateTournament> tournamentsAddInMemory = new ArrayList<>();
        List<PrivateTournament> nextTournaments = new ArrayList<>();
        public String toString() {
            return "tournamentsAddInMemory size="+tournamentsAddInMemory.size()+" - nextTournaments size="+nextTournaments;
        }
    }

    /**
     * Scan all tournament with startDate <= currentTS && endDate > currentTS && startupDone=false => add tournament in memory and generate next tournament in DB
     * @return
     */
    public StartupTournamentsResult startupTournaments() {
        StartupTournamentsResult result = new StartupTournamentsResult();
        long currentTS = System.currentTimeMillis();
        List<PrivateTournament> listTour = null;
        try {
            // list of tournament with startDate <= currentTS && endDate > currentTS && startupDone=false
            List<Criteria> criteriaList = new ArrayList<>();
            criteriaList.add(Criteria.where("startDate").lte(currentTS));
            criteriaList.add(Criteria.where("endDate").gt(currentTS));
            criteriaList.add(Criteria.where("startupDone").is(false));
            listTour = mongoTemplate.find(Query.query(new Criteria().andOperator(criteriaList.toArray(new Criteria[criteriaList.size()]))), PrivateTournament.class);
        } catch (Exception e) {
            log.error("Exception to list tournament for startup - currentTS="+Constantes.timestamp2StringDateHour(currentTS), e);
        }

        if (listTour != null && listTour.size() > 0) {
            for (PrivateTournament t : listTour) {
                if (!t.isFinished()) {
                    try {
                        // add tournament in memory ?
                        boolean tournamentAlreadyInMemory = false;
                        GenericMemTournament memTournament = memoryMgr.getTournament(t.getIDStr());
                        if (memTournament == null) {
                            memoryMgr.addTournament(t);
                            result.tournamentsAddInMemory.add(t);
                        } else {
                            tournamentAlreadyInMemory = true;
                        }

                        // get properties
                        PrivateTournamentProperties properties = getPrivateTournamentProperties(t.getPropertiesID());
                        if (properties != null) {
                            // add notif for favorite players
                            if (!tournamentAlreadyInMemory) {
                                MessageNotifGroup notif = createAndSetNotifGroupTournamentStart(properties.getPlayersFavorite(), t);
                                if (notif != null) {
                                    for (Long l : properties.getPlayersFavorite()) {
                                        FBSession session = presenceMgr.getSessionForPlayerID(l);
                                        if (session != null) {
                                            session.pushEvent(notifMgr.buildEvent(notif, l));
                                        }
                                    }
                                } else {
                                    log.error("Failed to create notifGroup ... tour=" + t + " - properties=" + properties);
                                }
                            } else {
                                log.warn("Tournament already in memory => no notif to send ... already done ! properties="+properties);
                            }
                            // check recurrent and properties not removed by player
                            if (properties.isRecurrent() && properties.getTsSetRemove() == 0 && properties.isEnable()) {
                                // chek owner credit is valid
                                PrivateTournament nextTour = createPrivateTournament(properties, false);
                                if (nextTour != null) {
                                    result.nextTournaments.add(nextTour);
                                } else {
                                    log.error("Failed to create next tournament for properties=" + properties);
                                }
                            }
                        } else {
                            log.error("Failed to find properties with id="+t.getPropertiesID()+" - tour="+t);
                        }

                        t.setStartupDone(true);
                        mongoTemplate.save(t);
                    } catch (Exception e) {
                        log.error("Exception to process startup for tournament="+t, e);
                    }
                }
            }
        }
        log.warn("Result startup=" + result);
        return result;
    }

    /**
     * Get a tournament with exactly propertiesID and startDate
     * @param propertiesID
     * @param startDate
     * @return
     */
    public PrivateTournament getTournamentWithPropertiesAndStartDate(String propertiesID, long startDate) {
        return mongoTemplate.findOne(new Query(Criteria.where("propertiesID").is(propertiesID).andOperator(Criteria.where("startDate").is(startDate))), PrivateTournament.class);
    }

    /**
     * Count tournament for properties
     * @param propertiesID
     * @return
     */
    public long countTournamentForProperties(String propertiesID) {
        return mongoTemplate.count(new Query(Criteria.where("propertiesID").is(propertiesID)), PrivateTournament.class);
    }

    /**
     * Return the next tournament (startDate > current time) with this properties
     * @param propertiesID
     * @return
     */
    public PrivateTournament getNextTournamentForProperties(String propertiesID) {
        Query query = Query.query(Criteria.where("propertiesID").is(propertiesID).andOperator(Criteria.where("startDate").gt(System.currentTimeMillis())));
        query.with(new Sort(Sort.Direction.ASC, "startDate"));
        return mongoTemplate.findOne(query, PrivateTournament.class);
    }

    /**
     * Return current and next tournament (endDate > current time) with this properties
     * @param propertiesID
     * @return
     */
    public List<PrivateTournament> getCurrentAndNextTournamentForProperties(String propertiesID) {
        Query query = Query.query(Criteria.where("propertiesID").is(propertiesID).andOperator(Criteria.where("endDate").gt(System.currentTimeMillis())));
        query.with(new Sort(Sort.Direction.ASC, "startDate"));
        return mongoTemplate.find(query, PrivateTournament.class);
    }

    /**
     * Nizar
     * @param playerId
     * @return
     */
    public List<PrivateTournament> getAllTournamentByPlayer(long playerId) {
        Query query = Query.query(Criteria.where("ownerID").is(playerId));
        return mongoTemplate.find(query, PrivateTournament.class);
    }

    /**
     * Add or remove player favorite on tournament properties
     * @param propertiesID
     * @param playerID
     * @param favorite
     * @return
     */
    public boolean setTournamentFavoriteForPlayer(String propertiesID, long playerID, boolean favorite) {
        synchronized (lockProperties.getLock(propertiesID)) {
            PrivateTournamentProperties properties = getPrivateTournamentProperties(propertiesID);
            if (properties != null) {
                if (favorite) {
                    properties.addPlayerFavorite(playerID);
                } else {
                    properties.removePlayerFavorite(playerID);
                }
                mongoTemplate.save(properties);
                return true;
            }
            return false;
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
    private PrivateGame createGame(long playerID, PrivateTournament tour, int dealIndex, int conventionProfil, String conventionData, int cardsConventionProfil, String cardsConventionValue, long deviceID) throws FBWSException {
        synchronized (lockCreateGame.getLock(tour.getIDStr()+"-"+playerID)) {
            if (getGameOnTournamentAndDealForPlayer(tour.getIDStr(), dealIndex, playerID) != null) {
                log.error("A game already exist on tour="+tour+" - dealIndex="+dealIndex+" - playerID="+playerID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            try {
                PrivateGame game = new PrivateGame(playerID, tour, dealIndex);
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
        PrivateTournament tour = null;
        PrivateMemTournament memTour = null;

        //***********************
        // Get Tournament -> check table from session
        if (session.getCurrentGameTable() != null &&
                !session.getCurrentGameTable().isReplay() &&
                session.getCurrentGameTable().getTournament() != null &&
                session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_PRIVATE &&
                session.getCurrentGameTable().getTournament().getIDStr().equals(tournamentID)) {
            table = session.getCurrentGameTable();
            tour = (PrivateTournament)table.getTournament();
            if (tour == null) {
                log.error("No tournament found on table ... tourID="+tournamentID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        else {
            tour = (PrivateTournament)getTournament(tournamentID);
            if (tour == null) {
                log.error("No tournament found with this ID="+tournamentID);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
        }

        // check player is not blocked by creator
        if (tour.getOwnerID() != Constantes.PLAYER_FUNBRIDGE_ID && playerMgr.isPlayer1BlockedByPlayer2(session.getPlayer().getID(), tour.getOwnerID())) {
            if (log.isDebugEnabled()) {
                log.debug("Creator has blocked this player! - owner=" + tour.getOwnerID() + " - playerID=" + session.getPlayer().getID() + " - tourID=" + tournamentID);
            }
            throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_BLOCKED);
        }

        //*************************
        // Check tournament
        if (tour.isFinished()) {
            throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
        }
        if (!tour.isDateValid(System.currentTimeMillis())) {
            throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
        }
        memTour = (PrivateMemTournament)memoryMgr.getTournament(tour.getIDStr());
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
            PrivateGame game = getGameNotFinishedOnTournamentForPlayer(tour.getIDStr(), session.getPlayer().getID());
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
        PrivateGame game = null;
        if (table.getGame() != null &&
                !table.getGame().isReplay() &&
                !table.getGame().isFinished() &&
                table.getGame().getTournament().getIDStr().equals(tournamentID)) {
            game = (PrivateGame)table.getGame();
        } else {
            // need to retrieve an existing game
            int lastIndexPlayed = 0;
            List<PrivateGame> listGame = listGameOnTournamentForPlayer(tour.getIDStr(), session.getPlayer().getID());
            if (listGame != null) {
                for (PrivateGame g : listGame) {
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
                session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_PRIVATE, 1);
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
        if (table.getTournament() == null || !(table.getTournament() instanceof PrivateTournament)) {
            log.error("Touranment on table is not TIMEZONE table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!(table.getGame() instanceof PrivateGame)) {
            log.error("Game on table is not TIMEZONE table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        PrivateGame game = (PrivateGame)table.getGame();
        if (!table.getTournament().getIDStr().equals(tournamentID)) {
            log.error("TournamentID  of current game ("+table.getTournament().getIDStr()+") is not same as tournamentID="+tournamentID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        GenericMemTournament memTour = memoryMgr.getTournament(tournamentID);
        if (memTour == null) {
            log.error("No TimezoneMemTournament found for tournamentID="+tournamentID);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        checkGame(game);
        Player p = session.getPlayer();
        PrivateTournament tour = game.getTournament();
        // check if player has enough credit to leave tournament
        int nbDealToLeave = tour.getNbDeals() - table.getNbPlayedDeals() - 1; // -1 because credit already decrement for current game !
        if (nbDealToLeave > 0) {
            playerMgr.checkPlayerCredit(p, tour.getNbCreditsPlayDeal() * nbDealToLeave);
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
            memoryMgr.updateResult(game);
        }
        // leave game for other deals
        int nbDealNotPlayed = 0;
        for (int i= 1; i<= tour.getNbDeals(); i++) {
            if (getGameOnTournamentAndDealForPlayer(tournamentID, i, p.getID()) == null) {
                PrivateGame g = new PrivateGame(p.getID(), tour, i);
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
            session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_PRIVATE, nbDealNotPlayed);
        }

        // return credit remaining of player
        return p.getTotalCreditAmount();
    }

    /**
     * List tournament with finished = false & startDate < current time
     * @return
     */
    public List<PrivateTournament> listTournamentNotFinished() {
        Query q = Query.query(Criteria.where("finished").is(false).andOperator(Criteria.where("startDate").lt(System.currentTimeMillis())));
        q.with(new Sort(Sort.Direction.ASC, "startDate"));
        return mongoTemplate.find(q, PrivateTournament.class);
    }

    /**
     * Count tournament with finished flag false and endDate < current date
     * @return
     */
    public int countTournamentNotFinishedAndExpired() {
        Query q = Query.query(Criteria.where("finished").is(false).andOperator(Criteria.where("endDate").lt(System.currentTimeMillis())));
        return (int)mongoTemplate.count(q, PrivateTournament.class);
    }

    /**
     * Tournament with finished flag false and endDate < current date
     * @return
     */
    public List<PrivateTournament> listTournamentNotFinishedAndExpired(int nbMax) {
        Query q = Query.query(Criteria.where("finished").is(false).andOperator(Criteria.where("endDate").lt(System.currentTimeMillis())));
        q.with(new Sort(Sort.Direction.ASC, "startDate"));
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return mongoTemplate.find(q, PrivateTournament.class);
    }

    /**
     * List all expired tournament and finish them.
     */
    public int finishExpiredTournament() {
        List<PrivateTournament> listTour = listTournamentNotFinishedAndExpired(getConfigIntValue("finishExpiredTournamentNbMax", 10));
        int nbFinish = 0;
        if (listTour != null && listTour.size() > 0) {
            for (PrivateTournament tour : listTour) {
                try {
                    if (finishTournament(tour)) {
                        nbFinish++;
                    }
                } catch (Exception e) {
                    log.error("Exception to finish tournament="+tour);
                }
            }
        }
        return nbFinish;
    }

    /**
     * Finish the tournament
     * @param tour
     */
    public boolean finishTournament(Tournament tour) {
        long tsBegin = System.currentTimeMillis();
        if (tour != null && !tour.isFinished()) {
            if (tour instanceof PrivateTournament) {
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
                                List<PrivateGame> listGameNotFinished = listGameOnTournamentForDealNotFinished(memTour.tourID, memDeal.dealIndex);
                                if (listGameNotFinished != null) {
                                    for (PrivateGame game : listGameNotFinished) {
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
                                List<PrivateGame> listGameToInsert = new ArrayList<>();
                                for (long pla : listPlaNoResult) {
                                    PrivateGame g = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), memDeal.dealIndex, pla);
                                    if (g == null) {
                                        g = new PrivateGame(pla, (PrivateTournament)tour, memDeal.dealIndex);
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
                                List<PrivateGame> listGameToUpdate = new ArrayList<>();
                                for (long pla : listPlayer) {
                                    GenericMemDealPlayer resPla = memDeal.getResultPlayer(pla);
                                    if (resPla != null) {
                                        PrivateGame game = getGameOnTournamentAndDealForPlayer(memTour.tourID, memDeal.dealIndex, pla);
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
                            List<PrivateTournamentPlayer> listTourPlay = new ArrayList<>();
                            for (GenericMemTournamentPlayer mtp : (List<GenericMemTournamentPlayer>)memTour.getListMemTournamentPlayer()) {
                                PrivateTournamentPlayer tp = new PrivateTournamentPlayer();
                                tp.setPlayerID(mtp.playerID);
                                tp.setTournament((PrivateTournament)tour);
                                tp.setRank(mtp.ranking);
                                tp.setResult(mtp.result);
                                tp.setLastDate(mtp.dateLastPlay);
                                tp.setStartDate(mtp.dateStart);
                                tp.setResultType(memTour.resultType);
                                tp.setCreationDateISO(new Date());
                                listTourPlay.add(tp);
                            }
                            if (listTourPlay.size() > 0) {
                                mongoTemplate.insertAll(listTourPlay);
                            }

                            //******************************************
                            // set tournament finished & update tournament
                            tour.setFinished(true);
                            tour.setNbPlayers(listPlayer.size());
                            updateTournamentDB(tour);

                            // properties occurence ONCE => set
                            PrivateTournamentProperties properties = getPrivateTournamentProperties(((PrivateTournament)tour).getPropertiesID());
                            if (properties != null) {
                                boolean saveProperties = false;
                                if (getConfigIntValue("removeTournamentWithNotEnoughPlayer", 1) == 1 && properties.isRecurrent()) {
                                    // not enough player => increment nb times
                                    int thresholdNotEnoughPlayer = getConfigIntValue("thresholdNotEnoughPlayer", 3);
                                    int nbTimesNotEnoughPlayerBeforeRemove = getConfigIntValue("nbTimesNotEnoughPlayerBeforeRemove", 3);

                                    if (tour.getNbPlayers() < thresholdNotEnoughPlayer) {
                                        boolean removeIt = false;
                                        properties.incrementNbTimesNotEnoughPlayer();
                                        // Too many times consecutively with not enough player => remove it
                                        if (properties.getNbTimesNotEnoughPlayer() >= nbTimesNotEnoughPlayerBeforeRemove) {
                                            removeIt = true;
                                            properties.setTsSetRemove(System.currentTimeMillis());
                                        }
                                        saveProperties = true;
                                        // send notification to tournament creator
                                        MessageNotif notif = createNotifForTournamentWithNotEnoughPlayer(properties.getOwnerID(), properties.getName(), thresholdNotEnoughPlayer, nbTimesNotEnoughPlayerBeforeRemove, removeIt);
                                        log.warn("Create notif for owner - not enough player - notif=" + notif + " removeIt=" + removeIt + " - tour NbPlayers=" + tour.getNbPlayers());
                                        FBSession sessionOwner = presenceMgr.getSessionForPlayerID(properties.getOwnerID());
                                        if (sessionOwner != null) {
                                            sessionOwner.pushEvent(notifMgr.buildEvent(notif, properties.getOwnerID()));
                                        }
                                    }
                                    // reset nb times with not enough player
                                    else {
                                        if (properties.getNbTimesNotEnoughPlayer() > 0) {
                                            properties.setNbTimesNotEnoughPlayer(0);
                                            saveProperties = true;
                                        }
                                    }
                                }
                                // no recurrent => disable it and set tsSetRemove
                                if (!properties.isRecurrent()) {
                                    properties.setTsSetRemove(System.currentTimeMillis());
                                    properties.setEnable(false);
                                    saveProperties = true;
                                    if (log.isDebugEnabled()) {
                                        log.debug("Set tsSetRemove and enable to false for properties="+properties);
                                    }
                                }

                                if (saveProperties) {
                                    mongoTemplate.save(properties);
                                }
                            } else {
                                log.error("Failed to find properties for tournament="+tour);
                            }

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
                    log.error("No memTour found for tour="+tour);
                    List<Game> listGameOnTourNotFinish = listGameOnTournament(tour.getIDStr());
                    if (listGameOnTourNotFinish != null && listGameOnTourNotFinish.isEmpty()) {
                        try {
                            // no game found => set finish !
                            tour.setFinished(true);
                            updateTournamentDB(tour);
                            log.warn("Set tournament finish, no game found for tournament="+tour);
                        } catch (Exception e) {
                            log.error("Exception to set finished (no game found !) for tournament=" + tour, e);
                        }
                    }
                }
            } else {
                log.error("Tournament not instance of PrivateTournament !! tour="+tour);
            }
        } else {
            log.error("Tournament null or already finished - tour=" + tour);
        }
        return false;
    }

    /**
     * Check if player can open private tournament
     * @param tourID
     * @param playerID
     * @param password
     * @throws FBWSException
     */
    public void openTournament(String tourID, long playerID, String password) throws FBWSException {
        PrivateTournament tournament = (PrivateTournament)getTournament(tourID);
        if (tournament == null) {
            log.error("No tournament found with tourID="+tourID+" - playerID="+playerID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (tournament.getStartDate() > System.currentTimeMillis()) {
            log.error("Tournament is not yet started ! - tournament="+tournament+" - playerID="+playerID);
            throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_NOT_AVAILABLE);
        }
        PrivateTournamentProperties properties = getPrivateTournamentProperties(tournament.getPropertiesID());
        if (properties == null) {
            log.error("No properties found for tournament=" + tournament + " - playerID=" + playerID);
            throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_PROPERTIES_NOT_FOUND);
        }
        if (tournament.getOwnerID() != Constantes.PLAYER_FUNBRIDGE_ID && tournament.getOwnerID() != playerID) {

            // check player is not blocked by creator
            if (playerMgr.isPlayer1BlockedByPlayer2(playerID, tournament.getOwnerID())) {
                if (log.isDebugEnabled()) {
                    log.debug("Creator has blocked this player! - owner=" + tournament.getOwnerID() + " - playerID=" + playerID + " - tourID=" + tourID);
                }
                throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_BLOCKED);
            }

            if (properties.getAccessRule() != null && properties.getAccessRule().equals(ACCESS_RULE_PASSWORD)) {
                GenericMemTournament memTournament = getMemoryMgr().getTournament(tourID);
                if (memTournament != null) {
                    if (memTournament.getTournamentPlayer(playerID) == null) {
                        // check password
                        if (password == null || !password.equals(properties.getPassword())) {
                            throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_INVALID_PASSWORD);
                        }
                        // password is correct => add player in tournament
                        memTournament.addPlayer(playerID, System.currentTimeMillis());
                    }
                    // player has already open this tournament => no check password
                } else {
                    PrivateTournamentPlayer privateTournamentPlayer = getTournamentPlayer(tourID, playerID);
                    if (privateTournamentPlayer == null) {
                        log.error("No tournament player found for tournament=" + tournament + " - playerID=" + playerID);
                        throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_NO_ACCESS);
                    }
                }
            }
        }
    }

    /**
     * Change the password on tournament properties
     * @param playerID
     * @param propertiesID
     * @param password
     * @throws FBWSException
     */
    public void changePasswordForProperties(long playerID, String propertiesID, String password) throws FBWSException {
        if (password == null || password.length() == 0) {
            log.error("Password is not valid - password="+propertiesID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        PrivateTournamentProperties properties = getPrivateTournamentProperties(propertiesID);
        if (properties == null) {
            log.error("No properties found with ID="+propertiesID);
            throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_PROPERTIES_NOT_FOUND);
        }
        if (properties.getOwnerID() != playerID) {
            log.error("Player is not the owner of this properties="+properties+" - playerID="+playerID);
            throw new FBWSException(FBExceptionType.TOURNAMENT_PRIVATE_OWNER_NOT_PLAYER);
        }
        if (properties.getAccessRule() == null || !properties.getAccessRule().equals(ACCESS_RULE_PASSWORD)) {
            log.error("Access rule is not password on properties="+properties);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        properties.setPassword(password);
        mongoTemplate.save(properties);
    }

    public void updateProperties(PrivateTournamentProperties properties) {
        mongoTemplate.save(properties);
    }

    @Override
    public Game createReplayGame(long playerID, Tournament tour, int dealIndex, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) {
        if (tour instanceof PrivateTournament) {
            PrivateGame replayGame = new PrivateGame(playerID, (PrivateTournament)tour, dealIndex);
            return replayGame;
        }
        else {
            log.error("Tournament is not instance of PrivateTournament - tour="+tour+" - playerID="+playerID);
            return null;
        }
    }

    /**
     * List of properties with enable is false and field tsSetRemove > 0 & < tsSetRemoveLimit
     * @return
     */
    public List<PrivateTournamentProperties> listExpiredProperties(long tsSetRemoveLimit) {
        Query q = Query.query(Criteria.where("enable").is(false).andOperator(Criteria.where("tsSetRemove").gt(0).lt(tsSetRemoveLimit)));
        q.with(new Sort(Sort.Direction.ASC, "tsSetRemove"));
        return mongoTemplate.find(q, PrivateTournamentProperties.class);
    }

    /**
     * Try to remove expired properties => check no more tournament existing for these properties
     * @param nbDaysRemoveLimit 0 to use valur from configuration
     * @return
     */
    public List<PrivateTournamentProperties> removeExpiredProperties(int nbDaysRemoveLimit) {
        List<PrivateTournamentProperties> listRemove = new ArrayList<>();
        try {
            if (nbDaysRemoveLimit == 0) {
                nbDaysRemoveLimit = getConfigIntValue("removeExpiredPropertiesNbDays", 60);
            }
            long tsLimit = System.currentTimeMillis() - nbDaysRemoveLimit * Constantes.TIMESTAMP_DAY;
            List<PrivateTournamentProperties> expiredProperties = listExpiredProperties(tsLimit);
            log.warn("Nb expired properties for tsLimit=" + Constantes.timestamp2StringDateHour(tsLimit) + " - list size=" + expiredProperties.size());
            for (PrivateTournamentProperties e : expiredProperties) {
                // if no more tournament link with this properties => remove it
                if (countTournamentForProperties(e.getIDStr()) == 0) {
                    log.warn("Remove expired properties e="+e);
                    mongoTemplate.remove(e);
                    listRemove.add(e);
                }
            }
            log.warn("Nb properties remove for tsLimit=" + Constantes.timestamp2StringDateHour(tsLimit));
        } catch (Exception e) {
            log.error("Failed to remove expired properties for nbDaysRemoveLimit="+nbDaysRemoveLimit, e);
        }
        return listRemove;
    }

    /**
     * Send notif to players at tournament start
     * @param listPlayerID
     * @return
     */
    public MessageNotifGroup createAndSetNotifGroupTournamentStart(List<Long> listPlayerID, PrivateTournament tournament) {
        if (tournament != null && notifMgr.isNotifEnable() && listPlayerID.size() > 0) {
            TextUIData notifTemplate = notifMgr.getTemplate("privateTournamentStart");
            if (notifTemplate != null) {
                MessageNotifGroup notif = new MessageNotifGroup(notifTemplate.name);
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_PRIVATE;
                notif.dateExpiration = tournament.getEndDate();
                notif.templateParameters.put("TOUR_NAME", tournament.getName());
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                if (notifMgr.persistNotif(notif)) {
                    if (notifMgr.setNotifGroupForPlayer(notif, listPlayerID)) {
                        return notif;
                    } else {
                        log.error("Failed to set notif group for players - notif="+notif+" - list player size="+listPlayerID.size());
                    }
                } else {
                    log.error("Failed to persist notifGroup="+notif);
                }
            } else {
                log.error("No template found for name privateTournamentStart");
            }
        }  else {
            log.warn("Notif is disable ot listPlayerID empty !");
        }
        return null;
    }

    /**
     * Send notif to owner when not enough players finish tournament
     * @param recipientID
     * @param tourName
     * @param threshlodNbPlayers
     * @param nbTimesNotEnoughPlayer
     * @param remove
     * @return
     */
    public MessageNotif createNotifForTournamentWithNotEnoughPlayer(long recipientID, String tourName, int threshlodNbPlayers, int nbTimesNotEnoughPlayer, boolean remove) {
        if (notifMgr.isNotifEnable()) {
            TextUIData notifTemplate = null;
            if (remove) {
                notifTemplate = notifMgr.getTemplate("privateTournamentNotEnoughPlayerRemove");
            } else {
                notifTemplate = notifMgr.getTemplate("privateTournamentNotEnoughPlayer");
            }
            if (notifTemplate != null) {
                MessageNotif notif = new MessageNotif(notifTemplate.name);
                notif.recipientID = recipientID;
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_PRIVATE;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                notif.dateExpiration = System.currentTimeMillis() + MessageNotifMgr.DURATION_7J;
                notif.templateParameters.put("TOUR_NAME", tourName);
                notif.templateParameters.put("THRESHOLD_NB_PLAYERS", ""+threshlodNbPlayers);
                notif.templateParameters.put("NB_TIMES_NOT_ENOUGH_PLAYER", ""+nbTimesNotEnoughPlayer);
                if (notifMgr.persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("No template found for name privateTournamentNotEnoughPlayer - remove="+remove);
            }
        }  else {
            log.warn("Notif is disable ot listPlayerID empty !");
        }
        return null;
    }

    /**
     * Send notif to owner with subscription date expiration will next expire
     * @param recipientID
     * @param tsSubscriptionExpiration
     * @return
     */
    public MessageNotif createNotifForOwnerSubscriptionExpirationNext(long recipientID, long tsSubscriptionExpiration) {
        if (notifMgr.isNotifEnable()) {
            TextUIData notifTemplate = notifMgr.getTemplate("privateTournamentSubscriptionExpirationNext");
            if (notifTemplate != null) {
                MessageNotif notif = new MessageNotif(notifTemplate.name);
                notif.recipientID = recipientID;
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_PRIVATE;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                notif.dateExpiration = System.currentTimeMillis() + MessageNotifMgr.DURATION_7J;
                notif.templateParameters.put("DATE_END_SUBSCRIPTION", Constantes.timestamp2StringDate(tsSubscriptionExpiration));
                if (notifMgr.persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("No template found for name privateTournamentSubscriptionExpirationNext");
            }
        }  else {
            log.warn("Notif is disable ot listPlayerID empty !");
        }
        return null;
    }

    /**
     * Send notif to owner with soon not enough credit
     * @param recipientID
     * @param credit
     * @return
     */
    public MessageNotif createNotifForOwnerCreditSoonNotEnough(long recipientID, int credit) {
        if (notifMgr.isNotifEnable()) {
            TextUIData notifTemplate = notifMgr.getTemplate("privateTournamentCreditSoonNotEnough");
            if (notifTemplate != null) {
                MessageNotif notif = new MessageNotif(notifTemplate.name);
                notif.recipientID = recipientID;
                notif.category = MessageNotifMgr.MESSAGE_CATEGORY_PRIVATE;
                notif.addExtraField(MessageNotifMgr.NOTIF_PARAM_ACTION, MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE);
                notif.dateExpiration = System.currentTimeMillis() + MessageNotifMgr.DURATION_7J;
                notif.templateParameters.put("{PLAYER_CREDIT}", ""+credit);
                if (notifMgr.persistNotif(notif)) {
                    return notif;
                }
            } else {
                log.error("No template found for name privateTournamentCreditSoonNotEnough");
            }
        }  else {
            log.warn("Notif is disable ot listPlayerID empty !");
        }
        return null;
    }

    @Override
    public void updateGamePlayArgineFinished(Game game) throws FBWSException {

    }

    @Override
    public void updateGamePlayArgine(Game game) {

    }
}
