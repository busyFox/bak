package com.funbridge.server.tournament.training;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.tournament.DealGenerator.DealGenerate;
import com.funbridge.server.tournament.game.*;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.generic.memory.GenericMemDeal;
import com.funbridge.server.tournament.generic.memory.GenericMemDealPlayer;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer;
import com.funbridge.server.tournament.training.data.TrainingGame;
import com.funbridge.server.tournament.training.data.TrainingPlayerHisto;
import com.funbridge.server.tournament.training.data.TrainingTournament;
import com.funbridge.server.tournament.training.data.TrainingTournamentPlayer;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.result.WSResultArchive;
import com.funbridge.server.ws.tournament.WSTableTournament;
import com.funbridge.server.ws.tournament.WSTournament;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.tools.StringTools;
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
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by pserent on 09/04/2015.
 */
@Component(value = "trainingMgr")
@Scope(value = "singleton")
public class TrainingMgr extends TournamentGenericMgr {

    @Resource(name="mongoTrainingTemplate")
    private MongoTemplate mongoTrainingTemplate;

    private LockWeakString lockCreateGame = new LockWeakString();
    private GenerateTournamentsTask generateTournamentsTask = new GenerateTournamentsTask();
    private ScheduledExecutorService generateTournamentsTaskScheduler = Executors.newScheduledThreadPool(1);
    private FinishTournamentsTask finishTournamentsTask = new FinishTournamentsTask();
    private ScheduledExecutorService finishTournamentsTaskScheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> generateTournamentsTaskScheduledFuture = null, finishTournamentsTaskSchedulerFuture = null;


    private class FinishTournamentsTask implements Runnable{
        boolean running = false;

        @Override
        public void run() {
            if (!running) {
                running = true;
                try {
                    if (isFinishTaskEnabled()) {
                        finishExpiredTournaments();
                    }
                } catch (Exception e) {
                    log.error("Failed to finishExpiredTournaments", e);
                    ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to finishExpiredTournaments", e.getMessage(), null);
                }
                running = false;
            } else {
                log.error("Task already running ...");
                ContextManager.getAlertMgr().addAlert(getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for finishExpiredTournaments", null, null);
            }
        }
    }

    private class GenerateTournamentsTask implements Runnable{
        boolean running = false;

        @Override
        public void run() {
            if(!running){
                running = true;
                if(isGenerateTaskEnabled()){
                    int nbTournamentsLimit = getConfigIntValue("taskGeneratorLimit", 5);
                    int nbNewTournaments = getConfigIntValue("taskGeneratorNew", 10);
                    // Generate IMP tournaments
                    int nbNotFullTournaments = countNbNotFullTournamentsInMemory(Constantes.TOURNAMENT_RESULT_IMP);
                    if(nbNotFullTournaments >= 0 && nbNotFullTournaments < nbTournamentsLimit){
                        List<TrainingTournament> listNewTournaments = generateTournament(nbNewTournaments, Constantes.TOURNAMENT_RESULT_IMP);
                        if(listNewTournaments == null){
                            log.error("listNewTournaments is null !");
                        }
                        else{
                            if (log.isDebugEnabled()) {
                                log.debug(listNewTournaments.size() + " new IMP tournaments generated");
                            }
                        }
                    }
                    else {
                        log.debug("No need to generate new IMP tournaments. nbNotFullTournaments="+nbNotFullTournaments);
                    }
                    // Generate PAR PAIRES tournaments
                    nbNotFullTournaments = countNbNotFullTournamentsInMemory(Constantes.TOURNAMENT_RESULT_PAIRE);
                    if(nbNotFullTournaments >= 0 && nbNotFullTournaments < nbTournamentsLimit){
                        List<TrainingTournament> listNewTournaments = generateTournament(nbNewTournaments, Constantes.TOURNAMENT_RESULT_PAIRE);
                        if(listNewTournaments == null){
                            log.error("listNewTournaments is null !");
                        }
                        else{
                            if (log.isDebugEnabled()) {
                                log.debug(listNewTournaments.size() + " new PAR PAIRES tournaments generated");
                            }
                        }
                    }
                    else {
                        log.debug("No need to generate new PAR PAIRES tournaments. nbNotFullTournaments="+nbNotFullTournaments);
                    }
                }
                running = false;
            } else {
                log.error("Task already running ...");
            }
        }
    }

    public TrainingMgr() {
        super(Constantes.TOURNAMENT_CATEGORY_TRAINING);
    }

    @Override
    @PostConstruct
    public void init() {
        super.init(GenericMemTournament.class, GenericMemTournamentPlayer.class, GenericMemDeal.class, GenericMemDealPlayer.class);
    }

    @Override
    @PreDestroy
    public void destroy() {
        stopScheduler(generateTournamentsTaskScheduler);
        stopScheduler(finishTournamentsTaskScheduler);
        super.destroy();
    }

    @Override
    public void startUp() {
        log.info("startUp");
        try{
            // load game not finished
            List<TrainingTournament> listTour = getTournamentsNotFinished();
            if (listTour != null && listTour.size() > 0) {
                log.error("Nb tournament TRAINING to load : " + listTour.size());
                int nbLoadFromFile = 0, nbLoadFromDB = 0;
                // loop on each tournament
                for (TrainingTournament tour : listTour) {
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
                        List<TrainingGame> listGame = getListGamesOnTournament(tour.getIDStr());
                        if (listGame != null) {
                            GenericMemTournament memTour = memoryMgr.addTournament(tour);
                            // load each game in memory
                            for (TrainingGame tourGame : listGame) {
                                if (tourGame.isFinished()) {
                                    memTour.addResult(tourGame, false, false);
                                } else {
                                    memTour.addPlayer(tourGame.getPlayerID(), tourGame.getStartDate());
                                }
                            }
                            // compute result & ranking
                            memTour.computeResult();
                            memTour.computeRanking(true);

                            // set nb ma players and last start date
                            memTour.nbMaxPlayers = FBConfiguration.getInstance().getIntValue("tournament.TRAINING.nbMaxPlayer", 100);
                            memTour.lastStartDate = tour.getStartDate() + (((long) getConfigIntValue("durationLast", 2820))*60*1000);
                        }
                        nbLoadFromDB++;
                    }
                }
                log.error("Nb Tournament loaded : "+listTour.size()+" - loadFromFile="+nbLoadFromFile+" - loadFromDB="+nbLoadFromDB);
            } else {
                log.info("No tournament TRAINING to load");
            }

            // start scheduler task generator
            try {
                int finishPeriod = getConfigIntValue("taskGeneratorPeriod", 300);
                int initDelay = getConfigIntValue("taskGeneratorInitDelay", 60);
                generateTournamentsTaskScheduledFuture = generateTournamentsTaskScheduler.scheduleWithFixedDelay(generateTournamentsTask, initDelay, finishPeriod, TimeUnit.SECONDS);
                log.info("Generate Tournament Scheduler - Next run at "+getStringDateNextGeneratorScheduler()+" period (second)="+finishPeriod);
            } catch (Exception e) {
                log.error("Exception to start finisher task", e);
            }
            // start scheduler task finish
            try {
                int finishPeriod = getConfigIntValue("finishPeriodSeconds", 300);
                int initDelay = getConfigIntValue("finishInitDelaySeconds", 60);
                finishTournamentsTaskSchedulerFuture = finishTournamentsTaskScheduler.scheduleWithFixedDelay(finishTournamentsTask, initDelay, finishPeriod, TimeUnit.SECONDS);
                log.info("Close tournament finisher - next run at "+getStringDateNextCloseScheduler()+" - period (second)="+finishPeriod);
            } catch (Exception e) {
                log.error("Exception to start finisher task", e);
            }
        }
        catch(Exception e){
            log.error("Error while launching the scheduler for the generate task");
        }
    }

    @Override
    public MongoTemplate getMongoTemplate() {
        return mongoTrainingTemplate;
    }

    @Override
    public Class<? extends Game> getGameEntity() {
        return TrainingGame.class;
    }

    @Override
    public String getGameCollectionName() {
        return "training_game";
    }

    @Override
    public Class<? extends Tournament> getTournamentEntity() {
        return TrainingTournament.class;
    }

    @Override
    public Class<? extends TournamentPlayer> getTournamentPlayerEntity() {
        return TrainingTournamentPlayer.class;
    }

    /**
     * Return the date of the next generator task run.
     * @return
     */
    public String getStringDateNextGeneratorScheduler() {
        return Constantes.getStringDateForNextDelayScheduler(generateTournamentsTaskScheduledFuture);
    }

    /**
     * Return the date of the next close task run.
     * @return
     */
    public String getStringDateNextCloseScheduler() {
        return Constantes.getStringDateForNextDelayScheduler(finishTournamentsTaskSchedulerFuture);
    }

    @Override
    public void checkTournamentMode(boolean isReplay, long playerId) throws FBWSException {}


    public List<TrainingTournament> generateTournament(int nbTournaments, int resultType){
        List<TrainingTournament> listNewTournaments = new ArrayList<>();
        try{
            // Get/init some configuration values
            int nbDeals = getConfigIntValue("nbDeal", 1);
            int duration = getConfigIntValue("durationEnd", 2880);
            if (duration <= 0) {
                log.error("duration is not valid : "+duration);
                return null;
            }
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            long startDate = cal.getTimeInMillis();
            cal.add(Calendar.MINUTE, duration);
            long endDate = cal.getTimeInMillis();
            int nbCreditsToPlayDeal = getConfigIntValue("nbCreditPlayDeal", 1);
            for(int i=0; i<nbTournaments; i++){
                // Generate the deals
                Random r = new Random(System.nanoTime());
                int offsetIndex = r.nextInt(20);
                DealGenerate[] dealArray = dealGenerator.generateDeal(nbDeals, offsetIndex);
                if(dealArray != null && dealArray.length == nbDeals){
                    // Create tournament
                    TrainingTournament tournament = new TrainingTournament();
                    tournament.setResultType(resultType);
                    tournament.setDealGenerate(dealArray);
                    tournament.setNbCreditsPlayDeal(nbCreditsToPlayDeal);
                    tournament.setStartDate(startDate);
                    tournament.setEndDate(endDate);
                    tournament.setCreationDateISO(new Date());
                    try{
                        // Insert in DB
                        mongoTrainingTemplate.insert(tournament);
                        // Add in memory
                        GenericMemTournament memTour = memoryMgr.addTournament(tournament);
                        memTour.nbMaxPlayers = FBConfiguration.getInstance().getIntValue("tournament.TRAINING.nbMaxPlayer", 100);
                        memTour.lastStartDate = tournament.getStartDate() + (((long) getConfigIntValue("durationLast", 2820))*60*1000);
                        listNewTournaments.add(tournament);
                    }
                    catch(Exception e){
                        log.error("Failed to insert new tournament in DB : tournament="+tournament, e);
                    }
                }
                else{
                    log.error("Deal generation failed : resultType="+resultType);
                }
            }
        }
        catch (Exception e) {
            log.error("Error when trying to generate new tournaments : nbTournament="+nbTournaments+" - resultType="+resultType, e);
        }
        return listNewTournaments;
    }

    /**
     * Return list of tournament not finished
     * @return
     */
    public List<TrainingTournament> getTournamentsNotFinished() {
        return mongoTrainingTemplate.find(new Query(Criteria.where("finished").is(false)), TrainingTournament.class);
    }

    @Override
    public Game createReplayGame(long playerID, Tournament tour, int dealIndex, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) {
        if (tour instanceof TrainingTournament) {
            TrainingGame replayGame = new TrainingGame(playerID, (TrainingTournament)tour, dealIndex);
            return replayGame;
        }
        else {
            log.error("Tournament is not instance of TrainingTournament - tour="+tour+" - playerID="+playerID);
            return null;
        }
    }

    public int countNbNotFullTournamentsInMemory(int resultType) {
        int result = 0;
        List<GenericMemTournament> listNotFullTournaments = memoryMgr.listTournament();
        for(GenericMemTournament tournament : listNotFullTournaments){
            if(tournament.resultType == resultType && tournament.isOpen() && !tournament.isFull()){
                result++;
            }
        }
        return result;
    }

    public void finishExpiredTournaments(){
        int nbMax = getConfigIntValue("finishNbMax", 5);
        List<TrainingTournament> expiredTournaments = getExpiredTournaments(nbMax);
        if(expiredTournaments != null && expiredTournaments.size() > 0){
            for(TrainingTournament tournament : expiredTournaments){
                finishTournament(tournament);
            }
        }
    }

    public void finishTournament(String tournamentId) {
        finishTournament(getTournament(tournamentId));
    }

    public boolean finishTournament(Tournament tournament){
        long tsBegin = System.currentTimeMillis();
        try{
            if(tournament != null){
                if(!tournament.isFinished()){
                    // Get the tournament from memory
                    GenericMemTournament memTournament = memoryMgr.getTournament(tournament.getIDStr());
                    if(memTournament != null) {
                        if (!memTournament.closeInProgress) {
                            memTournament.closeInProgress = true;
                            // Get the players from memTournament
                            List<Long> players = memTournament.getListPlayer();
                            // Prepare some useful lists
                            List<TrainingGame> listGamesToUpdate = new ArrayList<>();
                            List<TrainingGame> listGamesToInsert = new ArrayList<>();
                            List<TrainingGame> listNotFinishedGames = new ArrayList<>();
                            // Iterate through deals
                            for (GenericMemDeal memDeal : memTournament.deals) {
                                // Clear the games lists on each iteration
                                listGamesToUpdate.clear();
                                listGamesToInsert.clear();
                                listNotFinishedGames.clear();

                                // Automatically finish not finished games for this deal
                                listNotFinishedGames = listGameOnTournamentForDealNotFinished(memTournament.tourID, memDeal.dealIndex);
                                if (listNotFinishedGames != null) {
                                    for (TrainingGame game : listNotFinishedGames) {
                                        game.setFinished(true);
                                        if (game.getBidContract() != null) {
                                            game.claimForPlayer(BridgeConstantes.POSITION_SOUTH, 0);
                                            GameMgr.computeScore(game);
                                        } else {
                                            game.setLeaveValue();
                                        }
                                        memTournament.addResult(game, false, true);
                                        updateGameInDB(game);
                                    }
                                }

                                // Insert games for player who started the tournament but did not play this deal
                                List<Long> listPlayersWithoutResult = memDeal.getListPlayerStartedTournamentWithNoResult();
                                for (long playerId : listPlayersWithoutResult) {
                                    TrainingGame g = getGameOnTournamentAndDealForPlayer(tournament.getIDStr(), memDeal.dealIndex, playerId);
                                    if (g == null) {
                                        g = new TrainingGame(playerId, (TrainingTournament) tournament, memDeal.dealIndex);
                                        g.setLeaveValue();
                                        listGamesToInsert.add(g);
                                    }
                                    memTournament.addResult(g, false, true);
                                }

                                // Loop on all players to update games in DB
                                for (long playerID : players) {
                                    GenericMemDealPlayer resultPlayer = memDeal.getResultPlayer(playerID);
                                    if (resultPlayer != null) {
                                        // Get the game in DB
                                        TrainingGame game = getGameOnTournamentAndDealForPlayer(memTournament.tourID, memDeal.dealIndex, playerID);
                                        if (game != null) {
                                            listGamesToUpdate.add(game);
                                        } else {
                                            // Create game (forfeit)
                                            game = new TrainingGame(playerID, (TrainingTournament)tournament, memDeal.dealIndex);
                                            game.setLeaveValue();
                                            listGamesToInsert.add(game);
                                        }
                                        game.setRank(resultPlayer.nbPlayerBetterScore + 1);
                                        game.setResult(resultPlayer.result);
                                    } else {
                                        log.error("Failed to find a result on deal for player=" + playerID);
                                    }
                                }

                                // Update in DB
                                updateListGamesInDB(listGamesToUpdate);
                                insertListGamesInDB(listGamesToInsert);
                            }

                            // Compute tournament result and ranking
                            memTournament.computeResult();
                            memTournament.computeRanking(false);

                            // Insert tournament result in DB & update playerHisto
                            List<TrainingTournamentPlayer> listTournamentPlayer = new ArrayList<>();
                            for (GenericMemTournamentPlayer mtp : (List<GenericMemTournamentPlayer>)memTournament.getListMemTournamentPlayer()) {
                                TrainingTournamentPlayer tp = new TrainingTournamentPlayer();
                                tp.setPlayerID(mtp.playerID);
                                tp.setTournament((TrainingTournament)tournament);
                                tp.setResultType(tournament.getResultType());
                                tp.setRank(mtp.ranking);
                                tp.setResult(mtp.result);
                                tp.setLastDate(mtp.dateLastPlay);
                                tp.setStartDate(mtp.dateStart);
                                tp.setCreationDateISO(new Date());
                                listTournamentPlayer.add(tp);

                                // player histo
                                TrainingPlayerHisto tph = getPlayerHisto(mtp.playerID);
                                if (tph != null) {
                                    if (tournament.getResultType() == Constantes.TOURNAMENT_RESULT_IMP) {
                                        if (tph.getResetIMPDate() < mtp.dateStart) {
                                            tph.addResultIMP(mtp.result);
                                        }
                                    } else {
                                        if (tph.getResetPairesDate() < mtp.dateStart) {
                                            tph.addResultPaire(mtp.result);
                                        }
                                    }
                                    mongoTrainingTemplate.save(tph);
                                }
                            }
                            insertListTournamentPlayerInDB(listTournamentPlayer);

                            // Update tournament in DB
                            tournament.setFinished(true);
                            tournament.setNbPlayers(players.size());
                            updateTournamentDB(tournament);

                            // Remove tournament from memory
                            memoryMgr.removeTournament(tournament.getIDStr());
                            log.debug("Tournament " + tournament + "closed. Duration : " + (System.currentTimeMillis() - tsBegin) + " ms");
                            return true;
                        } else {
                            log.error("MemTournament close in progress - tournament=" + tournament);
                        }
                    } else {
                        log.error("No tournament found in memory for tournament=" + tournament);
                    }
                }
                else{
                    log.error("Tournament "+tournament+" is already finished !");
                }
            }
            else{
                log.error("Tournament is null !");
            }
        }
        catch(Exception e){
            log.error("Exception while trying to close tournament="+tournament, e);
        }
        return false;
    }

    /**
     * Get data to play a deal on tournament
     * @param session
     * @param resultType
     * @param conventionProfil
     * @param conventionValue
     * @param cardsConventionProfil
     * @param cardsConventionValue
     * @return
     * @throws FBWSException
     */
    public WSTableTournament playTournament(FBSession session, int resultType, int conventionProfil, String conventionValue, int cardsConventionProfil, String cardsConventionValue) throws FBWSException {
        if (session == null) {
            log.error("Session is null");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        Table table = null;
        TrainingTournament tournament = null;
        GenericMemTournament memTournament = null;

        //***********************
        // Get tournament in progress in session
        if (session.getCurrentGameTable() != null &&
                !session.getCurrentGameTable().isReplay() &&
                session.getCurrentGameTable().getTournament() != null &&
                session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TRAINING &&
                session.getCurrentGameTable().getTournament().getResultType() == resultType &&
                session.getCurrentGameTable().getGame() != null && !session.getCurrentGameTable().getGame().isFinished()) {
            table = session.getCurrentGameTable();
            tournament = (TrainingTournament) table.getTournament();
            memTournament = memoryMgr.getTournament(tournament.getIDStr());
        }
        // Else get it from memory
        else {
            List<GenericMemTournament> listTourInProgress = memoryMgr.listMemTournamentInProgressForPlayer(session.getPlayer().getID());
            if (listTourInProgress != null) {
                for (GenericMemTournament e : listTourInProgress) {
                    if (e.resultType == resultType) {
                        memTournament = e;
                        tournament = (TrainingTournament)getTournament(e.tourID);
                    }
                }
            }
        }
        // Check if a tournament has been found and is still valid
        if(tournament != null){
            if(tournament.isFinished() || !tournament.isDateValid(System.currentTimeMillis())){
                tournament = null;
            }
        }
        // If there's no tournament in progress for player
        if(tournament == null){
            // Search in memory tournaments that haven't been played by this player
            List<GenericMemTournament> memTournamentsToPlay = memoryMgr.findMemTournamentsToPlay(session.getPlayer().getID(), resultType, getConfigIntValue("useMemoryFindNbTourMax", 5), 0);

            Collections.shuffle(memTournamentsToPlay, new Random(System.currentTimeMillis()));
            for(GenericMemTournament memTour : memTournamentsToPlay){
                synchronized (getLockOnTournament(memTour.tourID)){
                    TrainingTournament tour = (TrainingTournament)getTournament(memTour.tourID);
                    if(tour == null){
                        log.error("Failed to find tournament for tournamentID="+memTour.tourID);
                        continue;
                    }
                    if(tour.isFinished()){
                        continue;
                    }
                    if(existGameOnTournamentForDevice(tour.getIDStr(), session.getDeviceID())){
                        if (log.isDebugEnabled()) {
                            log.debug("Tournament already played on this device. tournamentID=" + tour.getIDStr() + " deviceID=" + session.getDeviceID());
                        }
                        continue;
                    }
                    if(!memTour.isOpen() || memTour.isFull()){
                        if (log.isDebugEnabled()) {
                            log.debug("Tournament is full. tournamentID=" + memTour.tourID);
                        }
                        continue;
                    }
                    // This tournament is good
                    tournament = tour;
                    memTournament = memTour;
                    break;
                }
            }

            // No tournament found, let's generate a new one
            if(tournament == null){
                List<TrainingTournament> newTournaments = generateTournament(1, resultType);
                if(newTournaments != null && newTournaments.size() == 1){
                    tournament = newTournaments.get(0);
                    memTournament = memoryMgr.getTournament(tournament.getIDStr());
                }
                else{
                    log.error("Failed to generate a new tournament");
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
            }
        }


        //********************
        // Need to build table
        if (table == null) {
            long ts = System.currentTimeMillis();
            table = new Table(session.getPlayer(), tournament);
            // set nb deals played
            GenericMemTournamentPlayer memTourPlayer = memTournament.getTournamentPlayer(session.getPlayer().getID());
            if (memTourPlayer != null) {
                table.setPlayedDeals(memTourPlayer.playedDeals);
            }
            // retrieve game if exist
            TrainingGame game = getNotFinishedGameOnTournamentForPlayer(tournament.getIDStr(), session.getPlayer().getID());
            if (game != null) {
                table.setGame(game);
            }
            // if start playing this tournament => check device not yet played this tournament
            if (game == null) {
                if (existGameOnTournamentForDevice(tournament.getIDStr(), session.getDeviceID())) {
                    log.error("Tournament already played on this device by another player ! tourID="+tournament.getIDStr()+" - player="+session.getPlayer().getID()+" - deviceID="+session.getDeviceID());
                    throw new FBWSException(FBExceptionType.TOURNAMENT_ALREADY_PLAYED_DEVICE);
                }
            }
        }
        session.setCurrentGameTable(table);

        //*******************
        // retrieve game
        TrainingGame game = null;
        if (table.getGame() != null &&
                !table.getGame().isReplay() &&
                !table.getGame().isFinished() &&
                table.getGame().getTournament().getIDStr().equals(tournament.getIDStr())) {
            game = (TrainingGame)table.getGame();
        } else {
            // need to retrieve an existing game
            int lastIndexPlayed = 0;
            List<TrainingGame> listGame = listGameOnTournamentForPlayer(tournament.getIDStr(), session.getPlayer().getID());
            if (listGame != null) {
                for (TrainingGame g : listGame) {
                    // BUG with game finished, contractType=0 (PA) and bids = ""
                    if (g.isInitFailed() && g.isFinished() && g.getContractType() == 0 && g.getBids().length() == 0) {
                        log.error("BugGameNotValid - init failed => Reset game data g="+g);
                        g.resetData();
                        g.setConventionSelection(conventionProfil, conventionValue);
                        g.setDeviceID(session.getDeviceID());
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
                playerMgr.checkPlayerCredit(session.getPlayer(), tournament.getNbCreditsPlayDeal());
                // need to create a new game
                if (lastIndexPlayed >= tournament.getNbDeals()) {
                    log.error("lastIndexPlayed to big to play another deal ! player="+session.getPlayer()+" - lastIndexPlayed="+lastIndexPlayed+" - tour="+tournament);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                // create game
                game = createGame(session.getPlayer().getID(), tournament, lastIndexPlayed+1, conventionProfil, conventionValue, cardsConventionProfil, cardsConventionValue, session.getDeviceID());

                if (game != null) {
                    ContextManager.getPlayerMgr().updatePlayerCreditDeal(session.getPlayer(), tournament.getNbCreditsPlayDeal(), 1);
                } else {
                    log.error("Failed to create game - player="+session.getPlayer().getID()+" - tour="+tournament);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_TRAINING, 1);
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
        synchronized (getLockOnTournament(tournament.getIDStr())) {
            GenericMemTournamentPlayer memTourPlayer = memTournament.getOrCreateTournamentPlayer(session.getPlayer().getID());
            memTourPlayer.currentDealIndex = game.getDealIndex();
        }

        //*********************
        // Build data to return
        WSTableTournament tableTour = new WSTableTournament();
        tableTour.tournament = toWSTournament(tournament, session.getPlayerCache());
        tableTour.tournament.currentDealIndex = game.getDealIndex();
        tableTour.currentDeal = new WSGameDeal();
        tableTour.currentDeal.setDealData(game.getDeal(), tournament.getIDStr());
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
    public WSTournament toWSTournament(TrainingTournament tour, PlayerCache playerCache) {
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
            TrainingTournamentPlayer tourPlayer = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
            if (tourPlayer != null) {
                wst.resultPlayer = tourPlayer.toWSResultTournamentPlayer(playerCache, playerCache.ID);
            }
        }
        if (wst.resultPlayer != null) {
            wst.playerOffset = wst.resultPlayer.getRank();
        }
        return wst;
    }

    /**
     * Return the last tournaments played by player and with resultType. Only tournament after reset date
     * @param playerCache
     * @param resultType
     * @param resetDate
     * @param nbMax
     * @return
     * @throws FBWSException
     */
    public List<WSTournament> getListWSTournamentForPlayer(PlayerCache playerCache, int resultType, long resetDate, int nbMax) throws FBWSException {
        List<WSTournament> listWSTour = new ArrayList<WSTournament>();
        // compute date to list tour play
        Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (getConfigIntValue("resultArchiveNbDayBefore", 30)));
        long dateRef = dateBefore.getTimeInMillis();
        if (resetDate > dateRef) {
            dateRef = resetDate;
        }
        // get tournament not yet finished but played by the player
        List<GenericMemTournamentPlayer> listMemTourPlayer = memoryMgr.listMemTournamentForPlayer(playerCache.ID, true, resultType, dateRef);
        for (GenericMemTournamentPlayer e : listMemTourPlayer) {
            if (e.memTour.resultType != resultType || e.dateLastPlay <= dateRef) {
                continue;
            }
            if (listWSTour.size() >= nbMax) {
                break;
            }
            listWSTour.add(toWSTournament(getTournament(e.memTour.tourID), playerCache));
        }

        if (listWSTour.size() < nbMax) {
            // add tournament finished from BDD
            List<TrainingTournamentPlayer> listTourPlay = new ArrayList<>();
            try {
                Criteria cPlayerId = Criteria.where("playerID").is(playerCache.ID);
                Criteria cDateRef = Criteria.where("lastDate").gt(dateRef);
                Criteria cResultType = Criteria.where("resultType").is(resultType);
                Query q = new Query(new Criteria().andOperator(cPlayerId, cDateRef, cResultType));
                q.limit(nbMax - listWSTour.size());
                q.with(new Sort(Sort.Direction.DESC, "lastDate"));
                List<TrainingTournamentPlayer> results = mongoTrainingTemplate.find(q, TrainingTournamentPlayer.class);
                for (TrainingTournamentPlayer tp : results) {
                    if (tp.getTournament().getResultType() == resultType) {
                        listTourPlay.add(tp);
                    }
                }
            } catch (Exception e) {
                log.error("Exception to list tournament play playerID=" + playerCache.ID + " - dateRef=" + dateRef, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            for (TrainingTournamentPlayer tp : listTourPlay) {
                if (listWSTour.size() >= nbMax) {
                    break;
                }
                WSTournament temp = tp.getTournament().toWS();
                temp.resultPlayer = tp.toWSResultTournamentPlayer(playerCache, playerCache.ID);
                if (temp.resultPlayer != null) {
                    temp.playerOffset = temp.resultPlayer.getRank();
                }
                listWSTour.add(temp);
            }
        }
        return listWSTour;
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
            int nbTotal = (int) mongoTrainingTemplate.count(Query.query(Criteria.where("playerID").is(playerCache.ID).andOperator(Criteria.where("lastDate").gt(dateRef))), TrainingTournamentPlayer.class);
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
                List<TrainingTournamentPlayer> listTP = listTournamentPlayerAfterDate(playerCache.ID, dateRef, offsetBDD, count - listWSTour.size());
                if (listTP != null) {
                    for (TrainingTournamentPlayer tp :listTP) {
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
     * @throws FBWSException if an existing game existed for player, tournament and dealIndex or if an exception occurs
     */
    private TrainingGame createGame(long playerID, TrainingTournament tournament, int dealIndex, int conventionProfil, String conventionData, int cardsConventionProfil, String cardsConventionData, long deviceID) throws FBWSException {
        synchronized (lockCreateGame.getLock(tournament.getIDStr()+"-"+playerID)) {
            if (getGameOnTournamentAndDealForPlayer(tournament.getIDStr(), dealIndex, playerID) != null) {
                log.error("A game already exist on tour="+ tournament +" - dealIndex="+dealIndex+" - playerID="+playerID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            try {
                TrainingGame game = new TrainingGame(playerID, tournament, dealIndex);
                game.setStartDate(Calendar.getInstance().getTimeInMillis());
                game.setLastDate(Calendar.getInstance().getTimeInMillis());
                game.setConventionSelection(conventionProfil, conventionData);
                game.setCardsConventionSelection(cardsConventionProfil, cardsConventionData);
                game.setDeviceID(deviceID);
                // try to find bug of not valid game ...
                if (game.isFinished() && game.getContractType() == 0 && game.getBids().length() == 0) {
                    log.error("Game not valid !!! - game=" + game);
                    Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                mongoTrainingTemplate.insert(game);
                return game;
            } catch (Exception e) {
                log.error("Exception to create game player="+playerID+" - tour="+ tournament +" - dealIndex="+dealIndex, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Return the game not finished for a player on tournament.
     * @param tournamentID
     * @param playerID
     * @return
     */
    private TrainingGame getNotFinishedGameOnTournamentForPlayer(String tournamentID, long playerID) throws FBWSException{
        List<TrainingGame> listGames = listGameOnTournamentForPlayer(tournamentID, playerID);
        if (listGames != null) {
            for (TrainingGame g : listGames) {
                if (!g.isFinished()) {
                    return g;
                }
            }
        }
        return null;
    }

    /**
     * List of result on tounrmant play but not yet finished
     * @param playerID
     * @param resultType
     * @param dateRef
     * @return
     */
    public List<Double> getPlayerResultsOnNotFinishedTournaments(long playerID, int resultType, long dateRef) {
        List<Double> listResults = new ArrayList<>();
        List<GenericMemTournamentPlayer> listTourPlay = memoryMgr.listMemTournamentForPlayer(playerID, true, resultType, dateRef);
        if (listTourPlay != null) {
            for (GenericMemTournamentPlayer e: listTourPlay) {
                listResults.add(e.result);
            }
        }
        return listResults;
    }

    /**
     * Save the game to DB. if already existing an update is done, else an insert
     * @param game
     * @throws FBWSException
     */
    public void updateGameInDB(TrainingGame game) throws FBWSException {
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
            try {
                mongoTrainingTemplate.save(game);
            } catch (Exception e) {
                log.error("Exception to save game="+game, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    public void updateListGamesInDB(List<TrainingGame> listGames) throws FBWSException{
        for(TrainingGame game : listGames){
            updateGameInDB(game);
        }
    }

    /**
     * Insert games to DB
     * @param listGames
     * @throws FBWSException
     */
    public void insertListGamesInDB(List<TrainingGame> listGames) throws FBWSException {
        if (listGames != null && listGames.size() > 0) {
            try {
                mongoTrainingTemplate.insertAll(listGames);
            } catch (Exception e) {
                log.error("Exception to save listGame", e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Insert TournamentPlayer to DB
     * @param listTournamentPlayer
     * @throws FBWSException
     */
    public void insertListTournamentPlayerInDB(List<TrainingTournamentPlayer> listTournamentPlayer) throws FBWSException {
        if (listTournamentPlayer != null && listTournamentPlayer.size() > 0) {
            try {
                mongoTrainingTemplate.insertAll(listTournamentPlayer);
            } catch (Exception e) {
                log.error("Exception to save listTourPlay", e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * List game on a tournament
     * @param tourID
     * @return
     */
    public List<TrainingGame> getListGamesOnTournament(String tourID){
        try {
            Query q = Query.query(Criteria.where("tournament.$id").is(new ObjectId(tourID)));
            return mongoTrainingTemplate.find(q, TrainingGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament="+tourID, e);
        }
        return null;
    }

    public List<TrainingTournament> getExpiredTournaments(int nbMax){
        try{
            Query q = new Query();
            Criteria cFinished = Criteria.where("finished").is(false);
            Criteria cEndDate = Criteria.where("endDate").lt(System.currentTimeMillis());
            q.addCriteria(cFinished);
            q.addCriteria(cEndDate);
            if(nbMax>0) q.limit(nbMax);
            return mongoTrainingTemplate.find(q, TrainingTournament.class);
        }
        catch(Exception e){
            log.error("Error when trying to retrieve expired tournaments", e);
        }
        return null;
    }

    private boolean isFinishTaskEnabled() {
        return getConfigIntValue("taskFinishEnable", 1) != 0;
    }

    private boolean isGenerateTaskEnabled() {
        return getConfigIntValue("taskGeneratorEnable", 1) != 0;
    }

    @Override
    public void updateTournamentDB(Tournament tour) throws FBWSException {
        if (tour != null && tour instanceof TrainingTournament) {
            try {
                mongoTrainingTemplate.save(tour);
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
        if (session.getCurrentGameTable() == null || session.getCurrentGameTable().getGame() == null || !(session.getCurrentGameTable().getGame() instanceof TrainingGame)) {
            log.error("Table or game in session not valid ! - table="+session.getCurrentGameTable());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        TrainingGame game = (TrainingGame) session.getCurrentGameTable().getGame();
        if (!game.isReplay()) {
            if (game.isFinished()) {
                // update data game and table in DB
                updateGameInDB(game);

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

    public TrainingPlayerHisto getPlayerHisto(long playerID) {
        TrainingPlayerHisto tph = mongoTrainingTemplate.findOne(Query.query(Criteria.where("playerId").is(playerID)), TrainingPlayerHisto.class);
        if (tph == null) {
            tph = new TrainingPlayerHisto();
            tph.setPlayerId(playerID);
            tph.setNbResultIMP(0);
            mongoTrainingTemplate.insert(tph);
        }
        return tph;
    }

    public void resetPlayerHisto(long playerID, int resultType) {
        TrainingPlayerHisto tph = getPlayerHisto(playerID);
        if (tph != null) {
            if (resultType == Constantes.TOURNAMENT_RESULT_IMP) {
                tph.setResultIMP(0);
                tph.setResetIMPDate(System.currentTimeMillis());
                tph.setNbResultIMP(0);
            } else if (resultType == Constantes.TOURNAMENT_RESULT_PAIRE) {
                tph.setResultPaires(0);
                tph.setResetPairesDate(System.currentTimeMillis());
                tph.setNbResultPaires(0);
            }
            mongoTrainingTemplate.save(tph);
        }
    }

    public boolean deleteDataForPlayerList(List<Long> listPlaID) {
        boolean result = false;
        try {
            memoryMgr.deleteDataForPlayerList(listPlaID);
            mongoTrainingTemplate.remove(Query.query(Criteria.where("playerId").in(listPlaID)), TrainingPlayerHisto.class);
            mongoTrainingTemplate.remove(Query.query(Criteria.where("playerID").in(listPlaID)), TrainingGame.class);
            mongoTrainingTemplate.remove(Query.query(Criteria.where("playerID").in(listPlaID)), TrainingTournamentPlayer.class);
            result = true;
        }catch (Exception e) {
            log.error("Failed to delete player from list="+ StringTools.listToString(listPlaID), e);
        }
        return result;
    }

    @Override
    public void updateGamePlayArgineFinished(Game game) throws FBWSException {

    }

    @Override
    public void updateGamePlayArgine(Game game) {

    }

    public int leaveTournament(FBSession session, String tournamentID) throws FBWSException{
        // no leave tournament for training !
        throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
    }
}
