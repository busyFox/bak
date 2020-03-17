package com.funbridge.server.tournament.serie;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.tournament.game.*;
import com.funbridge.server.tournament.generic.GameGroupMapping;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.serie.data.*;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.game.WSGameView;
import com.funbridge.server.ws.game.WSTableGame;
import com.funbridge.server.ws.result.*;
import com.funbridge.server.ws.tournament.WSTableTournament;
import com.funbridge.server.ws.tournament.WSTournament;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.lock.LockWeakString;
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

/**
 * Created by bplays on 21/04/16.
 */
@Component(value="serieTopChallengeMgr")
@Scope(value="singleton")
public class SerieTopChallengeMgr extends TournamentGenericMgr implements ITournamentMgr {

    private static final String CONFIG_NAME = "SERIE_TOP_CHALLENGE";

    @Resource(name="mongoSerieTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name="tourSerieMgr")
    private TourSerieMgr serieMgr;

    private LockWeakString lockCreateGame = new LockWeakString();
    private LockWeakString lockCreateTournamentPlayer = new LockWeakString();
    private TourSeriePeriod periodTopChallenge = null;
    private TourSeriePeriod periodSerie = null;
    private boolean periodChangeProcessing = false;
    private boolean enable = false;

    @Override
    @PostConstruct
    public void init() {
        log.info("init");
        gameMgr = new GameMgr(this);

    }

    @Override
    @PreDestroy
    public void destroy() {
        log.info("destroy");
        if (gameMgr != null) {
            gameMgr.destroy();
        }
    }

    @Override
    public void startUp() {
        // initialize current period => must be run after startup of SerieMgr
        initPeriod();
    }

    @Override
    public String getTournamentCategoryName() {
        return Constantes.TOURNAMENT_CATEGORY_NAME_SERIE_TOP_CHALLENGE;
    }

    @Override
    public boolean isBidAnalyzeEnable() {
        return getConfigIntValue("engineAnalyzeBid", 1) == 1;
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
    public void checkGame(Game game) throws FBWSException {
        if (game == null) {
            log.error("Game is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void checkTournamentMode(boolean isReplay, long playerId) throws FBWSException {
        checkSerieTopChallengeEnable();
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
        if (session.getCurrentGameTable() == null || session.getCurrentGameTable().getGame() == null || !(session.getCurrentGameTable().getGame() instanceof SerieTopChallengeGame)) {
            log.error("Table or game in session not valid ! - table="+session.getCurrentGameTable());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        SerieTopChallengeGame game = (SerieTopChallengeGame)session.getCurrentGameTable().getGame();
        if (!game.isReplay()) {
            if (game.isFinished()) {
                // compute result & rank on game & tournament player
                setRankResultForGameAndTournamentPlayer(game, true);
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
     * Compute rank & result for game and for tournament player. Save object to DB.
     * @param game
     * @throws FBWSException
     */
    private void setRankResultForGameAndTournamentPlayer(SerieTopChallengeGame game, boolean updateGame) throws FBWSException {
        if (game != null) {
            // compute result & rank on game
            int nbBest = serieMgr.countGameWithBestScore(game.getTournament().getIDStr(), game.getDealIndex(), game.getScore());
            game.setRank(nbBest+1);
            int nbSame = serieMgr.countGameWithSameScore(game.getTournament().getIDStr(), game.getDealIndex(), game.getScore()) + 1; // +1 to include score of playuer
            game.setResult(Constantes.computeResultPaire(game.getTournament().getNbPlayers()+1, nbBest, nbSame));

            // update data game and table in DB
            if (updateGame) {
                updateGameDB(game);
            }

            // compute result & rank on tournament player
            SerieTopChallengeTournamentPlayer tournamentPlayer = getTournamentPlayer(game.getPlayerID(), game.getTournament().getIDStr());
            if (tournamentPlayer != null) {
                tournamentPlayer.setResult(
                        ((tournamentPlayer.getResult() * tournamentPlayer.getNbPlayedDeals()) + game.getResult()) / (tournamentPlayer.getNbPlayedDeals()+1)
                );
                tournamentPlayer.setRank(serieMgr.countTournamentPlayerWithBetterResult(game.getTournament().getIDStr(), tournamentPlayer.getResult())+1);
                tournamentPlayer.addPlayedDeals(game.getDealID());
                tournamentPlayer.setCurrentDealIndex(-1);
                tournamentPlayer.setLastDate(System.currentTimeMillis());
                if (tournamentPlayer.getNbPlayedDeals() == game.getTournament().getNbDeals()) {
                    tournamentPlayer.setFinished(true);
                }
                updateTournamentPlayerInDB(tournamentPlayer);
            }
        }
    }

    /**
     * Save the tournament to DB => nothing is done in this mode !!
     * @param tour
     */
    @Override
    public void updateTournamentDB(Tournament tour) throws FBWSException {
    }

    /**
     * Build the deal ID using tourID and deal index
     * @param tourID
     * @param index
     * @return
     */
    public String _buildDealID(String tourID, int index) {
        return TourSerieMgr.buildDealID(tourID,index);
    }

    /**
     * Extract tourID string from dealID
     * @param dealID
     * @return
     */
    public String _extractTourIDFromDealID(String dealID) {
        return TourSerieMgr.extractTourIDFromDealID(dealID);
    }

    /**
     * Extract dealIndex from dealID
     * @param dealID
     * @return
     */
    public int _extractDealIndexFromDealID(String dealID) {
        return TourSerieMgr.extractDealIndexFromDealID(dealID);
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
    public SerieTopChallengeGame getGame(String gameID) {
        return mongoTemplate.findById(new ObjectId(gameID), SerieTopChallengeGame.class);
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

    public void initPeriod() {
        log.warn("Init period");
        periodTopChallenge = serieMgr.getPreviousPeriod(1);
        if (periodTopChallenge != null) {
            periodSerie = serieMgr.getCurrentPeriod();
            if (periodSerie != null) {
                log.warn("Used period Top Challenge="+periodTopChallenge.getPeriodID()+" - period Serie="+periodSerie.getPeriodID());
                enable = true;
            } else {
                log.error("Current period from serie is null");
            }
        } else {
            log.error("Previous period from serie is null !!");
            enable = false;
        }
    }

    public TourSeriePeriod getPeriodTopChallenge() {
        return periodTopChallenge;
    }

    public TourSeriePeriod getPeriodSerie() {
        return periodSerie;
    }

    public void checkSerieTopChallengeEnable() throws FBWSException {
        if (!isEnable()) {
            throw new FBWSException(FBExceptionType.SERIE_TOP_CHALLENGE_CLOSED);
        }
        if (periodChangeProcessing) {
            throw new FBWSException(FBExceptionType.SERIE_TOP_CHALLENGE_PERIOD_PROCESSING);
        }
        if (periodTopChallenge == null || periodSerie == null) {
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    public void setPeriodChangeProcessing(boolean value) {
        periodChangeProcessing = value;
    }

    public boolean isPeriodChangeProcessing() {
        return periodChangeProcessing;
    }

    private class FinishPeriodThread implements Runnable {
        @Override
        public void run() {
            changePeriod();
        }
    }

    public void startThreadChangePeriod() {
        Thread threadChangePeriod = new Thread(new FinishPeriodThread());
        threadChangePeriod.start();
    }

    public void changePeriod() {
        if (periodChangeProcessing) {
            log.error("Period change already in process ...");
        } else {
            // disable current period
            setPeriodChangeProcessing(true);
            log.warn("Begin change period of Serie Top Challenge - periodTopChallenge="+periodTopChallenge+" - periodSerie="+periodSerie);

            // finished all games and tournament player in progress
            Query q = Query.query(Criteria.where("periodID").is(periodTopChallenge.getPeriodID()).andOperator(Criteria.where("finished").is(false)));
            List<SerieTopChallengeTournamentPlayer> listTourPlayerNotFinished = mongoTemplate.find(q, SerieTopChallengeTournamentPlayer.class);
            log.warn("Nb tournament player in progress = "+listTourPlayerNotFinished.size());
            for (SerieTopChallengeTournamentPlayer tp : listTourPlayerNotFinished) {
                try {
                    processLeaveTournament(null, tp.getTournament(), tp.getPlayerID(), 0);
                } catch (Exception e) {
                    log.error("Failed to leave tournament for player - tp="+tp, e);
                }
            }
            log.warn("End leave all tournament not finished.");
            // change periodID
            initPeriod();

            // enable current period
            setPeriodChangeProcessing(false);
        }
    }

    public GameMgr getGameMgr() {
        return gameMgr;
    }

    @Override
    public Class<SerieTopChallengeGame> getGameEntity() {
        return SerieTopChallengeGame.class;
    }

    @Override
    public Class<TourSerieGame> getAggregationGameEntity() {
        return TourSerieGame.class;
    }

    @Override
    public String getGameCollectionName() {
        return "top_serie_game";
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
        Table table = null;
        TourSerieTournament tournament = null;
        SerieTopChallengeTournamentPlayer tournamentPlayer = null;

        /* TOURNAMENT */
        // Check for tournament in progress in session
        if(session.getCurrentGameTable() != null &&
                !session.getCurrentGameTable().isReplay() &&
                session.getCurrentGameTable().getTournament() != null &&
                session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE &&
                session.getCurrentGameTable().getGame() != null &&
                !session.getCurrentGameTable().getGame().isFinished()){
            table = session.getCurrentGameTable();
            tournament = (TourSerieTournament) table.getTournament();
        }
        // Check for tournament in progress in DB
        if(tournament == null){
            tournament = getTournamentInProgressForPlayer(player.getID());
        }
        // If there's no tournament in progress, find a new one to play
        if(tournament == null){
            tournament = getNextTournamentToPlayForPlayer(player.getID());

            // no tournament found => all is played
            if (tournament == null) {
                throw new FBWSException(FBExceptionType.SERIE_TOP_CHALLENGE_ALL_PLAYED);
            }
        }
        
        /* TABLE */
        if(table == null){
            tournament.setCategory(Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE);
            table = new Table(session.getPlayer(), tournament);
            tournamentPlayer = getTournamentPlayer(session.getPlayer().getID(), tournament.getIDStr());
            if(tournamentPlayer != null){
                table.setPlayedDeals(tournamentPlayer.getPlayedDeals());
            }
            // Retrieve game if existing
            SerieTopChallengeGame game = getNotFinishedGameForTournamentAndPlayer(tournament.getIDStr(), session.getPlayer().getID());
            if(game != null){
                table.setGame(game);
            }
        }
        session.setCurrentGameTable(table);
        
        /* GAME */
        SerieTopChallengeGame game = null;
        // If the table already has a game
        if(table.getGame() != null &&
                !table.getGame().isReplay() &&
                !table.getGame().isFinished() &&
                table.getGame().getTournament().getIDStr().equals(tournament.getIDStr())){
            game = (SerieTopChallengeGame) table.getGame();
        } else {
            // If no game is associated to the table, look for an existing one in DB
            game = getNotFinishedGameForTournamentAndPlayer(tournament.getIDStr(), session.getPlayer().getID());
            if(game == null){
                // check player credit
                playerMgr.checkPlayerCredit(player, tournament.getNbCreditsPlayDeal());
                // TODO: Ici je vais chercher le tournamentPlayer en base pour avoir le lastIndexPlayed
                int lastIndexPlayed = 0;
                if (tournamentPlayer == null) {
                    tournamentPlayer = getTournamentPlayer(session.getPlayer().getID(), tournament.getIDStr());
                }
                if(tournamentPlayer != null){
                    lastIndexPlayed = tournamentPlayer.getNbPlayedDeals();
                    if(lastIndexPlayed >= tournament.getNbDeals()){
                        log.error("Player already played all " + tournament.getNbDeals() + " tournament deals, can't play another one ! player=" + session.getPlayer() + " - nbPlayedDeals=" + tournamentPlayer.getNbPlayedDeals() + " - tour=" + tournament);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                }
                int bidsProfil = conventionProfil;
                int cardsProfil = cardsConventionProfil;
                if (bidsProfil == 0) {
                    bidsProfil = gameMgr.getArgineEngineMgr().getDefaultProfile();
                }
                if (cardsProfil == 0) {
                    cardsProfil = gameMgr.getArgineEngineMgr().getDefaultProfileCards();
                }
                // create new game
                game = createGame(session.getPlayer().getID(), tournament, lastIndexPlayed+1, bidsProfil, conventionValue, cardsProfil, cardsConventionValue, session.getDeviceID());

                if (game != null) {
                    ContextManager.getPlayerMgr().updatePlayerCreditDeal(player, tournament.getNbCreditsPlayDeal(), 1);
                } else {
                    log.error("Failed to create game - player="+player.getID()+" - tour="+tournament);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }

                session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE, 1);
            }
            table.setGame(game);
        }

        // Add player on tournament or update it
        if (tournamentPlayer == null) {
            tournamentPlayer = getOrCreateTournamentPlayer(session.getPlayer().getID(), tournament);
        }
        if (tournamentPlayer.getCurrentDealIndex() != game.getDealIndex()) {
            tournamentPlayer.setCurrentDealIndex(game.getDealIndex());
            tournamentPlayer.setLastDate(System.currentTimeMillis());
            updateTournamentPlayerInDB(tournamentPlayer);
        }
        
        // Check if a thread is running for this game
        GameThread robotThread = gameMgr.getThreadPlayRunning(game.getIDStr());
        if(robotThread != null){
            if(log.isDebugEnabled()){
                log.debug("A thread is currently running for gameID=" + game.getIDStr() + " - stop it !");
            }
            robotThread.interruptRun();
        }

        // Build data to return
        WSTableTournament tableTournament = new WSTableTournament();
        tableTournament.tournament = serieTournamentToWS(tournament, session.getPlayerCache());
        tableTournament.tournament.currentDealIndex = game.getDealIndex();
        tableTournament.currentDeal = new WSGameDeal();
        tableTournament.currentDeal.setDealData(game.getDeal(), tournament.getIDStr());
        tableTournament.currentDeal.setGameData(game);
        tableTournament.table = table.toWSTableGame();
        tableTournament.gameIDstr = game.getIDStr();
        tableTournament.conventionProfil = game.getConventionProfile();
        tableTournament.creditAmount = session.getPlayer().getCreditAmount();
        tableTournament.nbPlayedDeals = session.getPlayer().getNbPlayedDeals();
        tableTournament.replayEnabled = playerMgr.isReplayEnabledForPlayer(session);
        tableTournament.freemium = session.isFreemium();
        return tableTournament;
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
    private SerieTopChallengeGame createGame(long playerID, TourSerieTournament tournament, int dealIndex, int conventionProfil, String conventionData, int cardsConventionProfil, String cardsConventionData, long deviceID) throws FBWSException {
        synchronized (lockCreateGame.getLock(tournament.getIDStr()+"-"+playerID)) {
            if (getGameOnTournamentAndDealForPlayer(tournament.getIDStr(), dealIndex, playerID) != null) {
                log.error("A game already exist on tour="+ tournament +" - dealIndex="+dealIndex+" - playerID="+playerID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            try {
                SerieTopChallengeGame game = new SerieTopChallengeGame(playerID, tournament, dealIndex);
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
                mongoTemplate.insert(game);
                return game;
            } catch (Exception e) {
                log.error("Exception to create game player="+playerID+" - tour="+ tournament +" - dealIndex="+dealIndex, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
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
        if (!(table.getTournament() instanceof TourSerieTournament)) {
            log.error("Tournament on table is not TourSerieTournament table="+table+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        SerieTopChallengeGame game = (SerieTopChallengeGame)session.getCurrentGameTable().getGame();
        if (game == null) {
            log.error("Game is null in session loginID="+session.getLoginID());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!(table.getGame() instanceof SerieTopChallengeGame)) {
            log.error("Game on table is not SerieTopChallengeGame table="+table+" - player="+session.getPlayer());
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
        table.addPlayedDeal(game.getDealID());
        List<SerieTopChallengeGame> listGameLeaved = processLeaveTournament(game, game.getTournament(), p.getID(), session.getDeviceID());
        if (listGameLeaved != null) {
            for (SerieTopChallengeGame g : listGameLeaved) {
                table.addPlayedDeal(g.getDealID());
            }
        }

        // remove game from session
        session.removeGame();
        // remove player from set
        gameMgr.removePlayerRunning(p.getID());

        // update player data
        if (listGameLeaved != null && !listGameLeaved.isEmpty()) {
            playerMgr.updatePlayerCreditDeal(p, game.getTournament().getNbCreditsPlayDeal()*listGameLeaved.size(), listGameLeaved.size());
            session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE, listGameLeaved.size());
        }

        // return credit remaining of player
        return p.getCreditAmount();
    }

    /**
     * Process leave on tournament and return list of game created with leave value. If a current game exist, this games is not add to result list. If game parameter is null, try to find current game in progress.
     * @param game
     * @param tour
     * @param playerID
     * @param deviceID
     * @return
     * @throws FBWSException
     */
    private List<SerieTopChallengeGame> processLeaveTournament(SerieTopChallengeGame game, TourSerieTournament tour, long playerID, long deviceID) throws FBWSException{
        if (game == null) {
            game = getNotFinishedGameForTournamentAndPlayer(tour.getIDStr(), playerID);
        }
        int lastDealIndexPlay = 0;
        if (game != null) {
            // leave current game
            synchronized (getGameMgr().getLockOnGame(game.getIDStr())) {
                if (game.getBidContract() == null) {
                    // no contract => leave
                    game.setLeaveValue();
                } else {
                    // contract exist => claim with 0 tricks
                    game.claimForPlayer(BridgeConstantes.POSITION_SOUTH, 0);
                    game.setFinished(true);
                    game.setLastDate(Calendar.getInstance().getTimeInMillis());
                    // compute score
                    GameMgr.computeScore(game);
                }
                // set rank, result for game & tournament player
                setRankResultForGameAndTournamentPlayer(game, true);
                lastDealIndexPlay = game.getDealIndex();
            }
        }

        // leave game for other deals
        List<SerieTopChallengeGame> listGameLeaved = new ArrayList<>();
        for (int i = lastDealIndexPlay+1; i <= tour.getNbDeals(); i++) {
            if (getGameOnTournamentAndDealForPlayer(tour.getIDStr(), i, playerID) == null) {
                SerieTopChallengeGame g = new SerieTopChallengeGame(playerID, tour, i);
                g.setStartDate(Calendar.getInstance().getTimeInMillis());
                g.setLastDate(Calendar.getInstance().getTimeInMillis());
                g.setDeviceID(deviceID);
                g.setLeaveValue();
                setRankResultForGameAndTournamentPlayer(g, false);
                listGameLeaved.add(g);
            }
        }
        if (!listGameLeaved.isEmpty()) {
            try {
                // try to find bug of not valid game ...
                if (getConfigIntValue("findBugGameNotValid", 0) == 1) {
                    for (SerieTopChallengeGame g : listGameLeaved) {
                        if (g.isFinished() && g.getContractType() == 0 && g.getBids().length() == 0) {
                            log.error("Game not valid !!! - game=" + g);
                            Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                            log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                        }
                    }
                }
                mongoTemplate.insertAll(listGameLeaved);
            } catch (Exception e) {
                log.error("Exception to create leave games for playerID=" + playerID + " - tour=" + tour + " - listGameLeaved size=" + listGameLeaved.size(), e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }

        // Finish tournament player if needed
        SerieTopChallengeTournamentPlayer tournamentPlayer = getTournamentPlayer(playerID, tour.getIDStr());
        if (tournamentPlayer != null && !tournamentPlayer.isFinished()) {
            tournamentPlayer.setFinished(true);
            updateTournamentPlayerInDB(tournamentPlayer);
        }

        return listGameLeaved;
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
        SerieTopChallengeGame replayGame = new SerieTopChallengeGame(player.getID(), (TourSerieTournament)gamePlayed.getTournament(), gamePlayed.getDealIndex());
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
     * Return the gameView object for this game with game data and table. The player must have played the deal of the game.
     * @param gameID
     * @param player
     * @return
     * @throws FBWSException
     */
    public WSGameView viewGame(String gameID, Player player) throws FBWSException {
        // find game in SerieTopChallengeGame
        Game game = getGame(gameID);
        if (game == null) {
            // not found ... try to find it in serie game
            game = serieMgr.getGame(gameID);
            if (game == null) {
                log.error("GameID not found - gameID=" + gameID + " - player=" + player);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
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
        gameView.tournament = serieTournamentToWS((TourSerieTournament) game.getTournament(), playerCacheMgr.getPlayerCache(player.getID()));

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
        TourSerieTournament tour = serieMgr.getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        TourSerieDeal deal = (TourSerieDeal)tour.getDeal(dealID);
        if (tour == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // check player played the deal of this game
        Game game = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, player.getID());
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
        gameView.tournament = serieTournamentToWS((TourSerieTournament)game.getTournament(), playerCacheMgr.getPlayerCache(player.getID()));

        return gameView;
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
        SerieTopChallengeGame replayGame = null;
        if (session.getCurrentGameTable() != null && session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
            replayGame = (SerieTopChallengeGame)session.getCurrentGameTable().getGame();
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
        SerieTopChallengeGame originalGame = getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        if (originalGame == null) {
            log.error("Original game not found ! dealID="+dealID+" - player="+session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }

        int nbTotalPlayer = -1;
        double resultOriginal = 0;
        int rankOriginal = 0;

        nbTotalPlayer = replayGame.getTournament().getNbPlayers()+1;
        resultOriginal = originalGame.getResult();
        rankOriginal = originalGame.getRank();

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
        int nbPlayerWithBestScoreReplay = serieMgr.countGameWithBestScore(tourID, dealIndex, replayGame.getScore());
        resultReplay.setRank(nbPlayerWithBestScoreReplay + 1);
        resultReplay.setNbPlayerSameGame(serieMgr.countGameWithSameScoreAndContract(tourID, dealIndex, replayGame.getScore(), replayGame.getContract(), replayGame.getContractType()));
        double replayResult = -1;
        int nbPlayerSameScore = serieMgr.countGameWithSameScore(tourID, dealIndex, replayGame.getScore());
        replayResult = Constantes.computeResultPaire(nbTotalPlayer, nbPlayerWithBestScoreReplay, nbPlayerSameScore);
        resultReplay.setResult(replayResult);
        resultReplay.setLead(replayGame.getBegins());

        // result for most played
        WSResultDeal resultMostPlayed = serieMgr.buildWSResultGameMostPlayed(tourID, dealIndex, nbTotalPlayer);
        if (resultMostPlayed == null) {
            log.error("No result for most played game for tourID="+tourID+" - dealIndex="+dealIndex);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        resultMostPlayed.setDealIDstr(dealID);
        resultMostPlayed.setDealIndex(replayGame.getDealIndex());
        resultMostPlayed.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
        resultMostPlayed.setNbTotalPlayer(nbTotalPlayer);
        if (!replayGame.getTournament().isFinished()) {
            int nbPlayerWithBestScoreMP = serieMgr.countGameWithBestScore(tourID, dealIndex, resultMostPlayed.getScore());
            resultMostPlayed.setRank(nbPlayerWithBestScoreMP + 1);
            nbPlayerSameScore = serieMgr.countGameWithSameScore(tourID, dealIndex, resultMostPlayed.getScore());
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

    public TourSerieTournament getNextTournamentToPlayForPlayer(long playerID) {
        if (periodTopChallenge != null) {
            List<TourSerieTournament> serieTopTournaments = listTournamentAvailable();
            for (TourSerieTournament t : serieTopTournaments) {
                SerieTopChallengeTournamentPlayer tp = getTournamentPlayer(playerID, t.getIDStr());
                if (tp == null) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Return tournamentPlayer object for a player on a tournament
     * @param tournamentID
     * @param playerID
     * @return
     */
    public SerieTopChallengeTournamentPlayer getTournamentPlayer(long playerID, String tournamentID){
        return mongoTemplate.findOne(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tournamentID)).andOperator(Criteria.where("playerID").is(playerID))), SerieTopChallengeTournamentPlayer.class);
    }

    /**
     * Return a tournament started by the player but not finished yet
     * @param playerID
     * @return
     */
    public TourSerieTournament getTournamentInProgressForPlayer(long playerID){
        //TODO: Il faut penser à ajouter un script de cloture des tournois au changement de période car ici comme sur les games je ne vérifie pas la période
        SerieTopChallengeTournamentPlayer tp = mongoTemplate.findOne(Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("finished").is(false))), SerieTopChallengeTournamentPlayer.class);
        if(tp == null) return null;
        else{
            return tp.getTournament();
        }
    }

    /**
     * Return or create tournamentPlayer object for a player on a tournament
     * @param playerID
     * @param tournament
     * @return
     */
    public SerieTopChallengeTournamentPlayer getOrCreateTournamentPlayer(long playerID, TourSerieTournament tournament){
        if (periodTopChallenge != null) {
            //TODO: le lock est il nécessaire et bien placé ?
            synchronized (lockCreateTournamentPlayer.getLock(playerID + "-" + tournament.getIDStr())) {
                SerieTopChallengeTournamentPlayer tp = getTournamentPlayer(playerID, tournament.getIDStr());
                if (tp == null) {
                    tp = new SerieTopChallengeTournamentPlayer();
                    tp.setPlayerID(playerID);
                    tp.setTournament(tournament);
                    tp.setPeriodID(periodTopChallenge.getPeriodID());
                    tp.setCreationDateISO(new Date());
                    tp.setStartDate(System.currentTimeMillis());
                    mongoTemplate.insert(tp);
                }
                return tp;
            }
        } else {
            log.error("Period TopChallenge is null !");
        }
        return null;
    }

    /**
     * Get a not finished game on a tournament for a player
     * @param tournamentID
     * @param playerID
     * @return
     */
    private SerieTopChallengeGame getNotFinishedGameForTournamentAndPlayer(String tournamentID, long playerID) throws FBWSException{
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
            Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            Criteria cFinished = Criteria.where("finished").is(false);
            q.addCriteria(new Criteria().andOperator(cTourID, cPlayerID, cFinished));
            return mongoTemplate.findOne(q, SerieTopChallengeGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament="+tournamentID+" and playerID="+playerID, e);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    public List<SerieTopChallengeGame> listGameForPlayerOnTournament(String tournamentID, long playerID) {
        Query q = new Query();
        Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
        Criteria cPlayerID = Criteria.where("playerID").is(playerID);
        q.addCriteria(new Criteria().andOperator(cTourID, cPlayerID));
        q.with(new Sort(Sort.Direction.ASC, "dealIndex"));
        return mongoTemplate.find(q, SerieTopChallengeGame.class);
    }

    public List<SerieTopChallengeGame> listGameForTournamentAndDeal(String tournamentID, int dealIndex) {
        Query q = new Query();
        Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
        Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
        q.addCriteria(new Criteria().andOperator(cTourID, cDealIndex));
        q.with(new Sort(Sort.Direction.ASC, "startDate"));
        return mongoTemplate.find(q, SerieTopChallengeGame.class);
    }

    /**
     * Return game associated to this tournament, deal index and player
     * @param tournamentID
     * @param dealIndex
     * @param playerID
     * @return
     */
    public SerieTopChallengeGame getGameOnTournamentAndDealForPlayer(String tournamentID, int dealIndex, long playerID){
        try {
            Query q = new Query();
            Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
            Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            q.addCriteria(new Criteria().andOperator(cTourID, cPlayerID, cDealIndex));
            return mongoTemplate.findOne(q, SerieTopChallengeGame.class);
        } catch (Exception e) {
            log.error("Failed to find game for tournament deal and playerID", e);
        }
        return null;
    }


    /**
     * Save the game to DB. if already existing an update is done, else an insert
     * @param game
     * @throws FBWSException
     */
    public void updateGameDB(SerieTopChallengeGame game) throws FBWSException {
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
     * Read string value for parameter in name (tournament."+CONFIG_NAME+".paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    @Override
    public String getConfigStringValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringValue("tournament." + CONFIG_NAME + "." + paramName, defaultValue);
    }

    @Override
    public String getStringResolvEnvVariableValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringResolvEnvVariableValue("tournament." + CONFIG_NAME + "." + paramName, defaultValue);
    }

    /**
     * Read int value for parameter in name (tournament."+CONFIG_NAME+".paramName) in config file
     * @param paramName
     * @param defaultValue
     * @return
     */
    public int getConfigIntValue(String paramName, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("tournament." + CONFIG_NAME + "." + paramName, defaultValue);
    }

    private void updateTournamentPlayerInDB(SerieTopChallengeTournamentPlayer tournamentPlayer) {
        if (tournamentPlayer != null) {
            try {
                mongoTemplate.save(tournamentPlayer);
            } catch (Exception e) {
                log.error("Failed to save tournament player", e);
            }
        }
    }

    /**
     * Update games to DB
     * @param listGame
     * @throws FBWSException
     */
    public <T extends Game> void updateListGameDB(List<T> listGame) throws FBWSException {
        if (listGame != null && !listGame.isEmpty()) {
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
     * Count number of tournament player finished on period
     * @param periodID
     * @param playerID
     * @return
     */
    public int countTournamentPlayerFinishedOnPeriod(String periodID, long playerID) {
        Query q = new Query();
        Criteria cPeriodID = Criteria.where("periodID").is(periodID);
        Criteria cPlayerID = Criteria.where("playerID").is(playerID);
        Criteria cFinished = Criteria.where("finished").is(true);
        q.addCriteria(new Criteria().andOperator(cPeriodID, cPlayerID, cFinished));
        return (int)mongoTemplate.count(q, SerieTopChallengeTournamentPlayer.class);
    }

    @Override
    public int countTournamentPlayerForTournament(String tournamentID) {
        Query q = new Query(Criteria.where("tournament.$id").is(new ObjectId(tournamentID)));
        return (int)mongoTemplate.count(q, SerieTopChallengeTournamentPlayer.class);
    }

    public int countTournamentPlayerForPlayer(long playerID) {
        Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (getConfigIntValue("resultArchiveNbDayBefore", 45)));
        long dateRef = dateBefore.getTimeInMillis();
        Query q = new Query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef)));
        return (int)mongoTemplate.count(q, SerieTopChallengeTournamentPlayer.class);
    }

    @Override
    public List<SerieTopChallengeTournamentPlayer> listTournamentPlayerForTournament(String tournamentID, int offset, int nbMax) {
        Query q = new Query(Criteria.where("tournament.$id").is(new ObjectId(tournamentID)));
        q.with(new Sort(Sort.Direction.DESC, "startDate"));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return mongoTemplate.find(q, SerieTopChallengeTournamentPlayer.class);
    }

    public List<SerieTopChallengeTournamentPlayer> listTournamentPlayerForPlayer(long playerID, int offset, int nbMax) {
        Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (getConfigIntValue("resultArchiveNbDayBefore", 45)));
        long dateRef = dateBefore.getTimeInMillis();
        Query q = new Query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef)));
        q.with(new Sort(Sort.Direction.DESC, "startDate"));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return mongoTemplate.find(q, SerieTopChallengeTournamentPlayer.class);
    }

    public ResultListTournamentArchive listTournamentArchive(long playerID, int offset, int nbMax) {
        ResultListTournamentArchive result = new ResultListTournamentArchive();
        result.archives = new ArrayList<>();
        Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (getConfigIntValue("resultArchiveNbDayBefore", 45)));
        long dateRef = dateBefore.getTimeInMillis();
        result.nbTotal = (int)mongoTemplate.count(Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef))), SerieTopChallengeTournamentPlayer.class);

        List<SerieTopChallengeTournamentPlayer> listTourPlayer = listTournamentPlayerForPlayer(playerID, offset, nbMax);
        for (SerieTopChallengeTournamentPlayer e : listTourPlayer) {
            WSTournamentArchive wst = new WSTournamentArchive();
            wst.date = e.getStartDate();
            wst.nbPlayers = e.getTournament().getNbPlayers()+1;
            wst.rank = e.getRank();
            wst.result = e.getResult();
            wst.resultType = e.getTournament().getResultType();
            wst.tournamentID = e.getTournament().getIDStr();
            wst.name = TourSerieMgr.SERIE_TOP;
            wst.finished = e.isFinished();
            wst.countDeal = e.getTournament().getNbDeals();
            if (e.getPlayedDeals() != null) {
                wst.listPlayedDeals = new ArrayList<>(e.getPlayedDeals());
                Collections.sort(wst.listPlayedDeals);
            }
            result.archives.add(wst);
        }
        return result;
    }

    /**
     * Return list of result deal on tournament for player
     * @param tour
     * @param playerID
     * @return
     */
    public List<WSResultDeal> resultListDealForTournamentForPlayer(TourSerieTournament tour, long playerID) throws FBWSException {
        if (tour == null) {
            log.error("Parameter not valid");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        List<WSResultDeal> listResultDeal = new ArrayList<WSResultDeal>();
        for (TourSerieDeal deal : tour.listDeal) {
            String dealID = deal.getDealID(tour.getIDStr());
            WSResultDeal resultPlayer = new WSResultDeal();
            resultPlayer.setDealIDstr(dealID);
            resultPlayer.setDealIndex(deal.index);
            resultPlayer.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
            resultPlayer.setNbTotalPlayer(tour.getNbPlayers() + 1);
            SerieTopChallengeGame game = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerID);
            if (game != null && game.isFinished()) {
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
            listResultDeal.add(resultPlayer);
        }
        return listResultDeal;
    }

    /**
     * Count nb contrat on tourID for deal
     * @param tourID
     * @param dealIndex
     * @param useLead
     * @return
     */
    public int countContractGroup(String tourID, int dealIndex, boolean useLead, int playerScore, String playerContract, int playerContractType) {
        int counter = 1;
        final List<GameGroupMapping> results = this.getResults(tourID, dealIndex, useLead);
        if (!results.isEmpty()) {
            counter = results.size();
            for (GameGroupMapping e : results) {
                if ( (e.isLeaved() && playerScore == Constantes.GAME_SCORE_LEAVE) || (e.score == playerScore && e.contract != null && e.contract.equals(playerContract) && e.contractType == playerContractType)) {
                    counter += 1;
                    break;
                }
            }
        }
        return counter;
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
            return (int) mongoTemplate.count(q, SerieTopChallengeGame.class);
        } catch (Exception e) {
            log.error("Failed to count games for player - playerID="+ playerID, e);
        }
        return 0;
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
        TourSerieTournament tour = serieMgr.getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        TourSerieDeal deal = (TourSerieDeal)tour.getDeal(dealID);
        if (tour == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        SerieTopChallengeTournamentPlayer tournamentPlayer = getTournamentPlayer(playerCache.ID, tour.getIDStr());
        if (tournamentPlayer == null) {
            log.error("No tournament player found for playerID="+playerCache.ID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // check deal played by this player
        SerieTopChallengeGame gamePlayer = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
        if (gamePlayer == null) {
            log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
            throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
        }
        WSResultDealTournament resultDealTour = new WSResultDealTournament();

        // list all games from DB order by score
        List<TourSerieGame> listGame = serieMgr.listGameFinishedOnTournamentAndDeal(tour.getIDStr(), deal.index, 0, 0);
        // Map with nb game with same score and contract
        HashMap<String, Integer> mapRankNbPlayer = new HashMap<>(); /* Map to store the number of player for each score and contract */
        HashMap<Integer, Integer> mapScoreNbPlayer = new HashMap<>(); /* Map to store nb player for each score */
        for (TourSerieGame g : listGame) {
            int temp = 0;
            String key = g.getScore()+"-"+g.getContractWS();
            if (useLead && g.getBegins() != null) {
                key += "-" + g.getBegins();
            }
            if (mapRankNbPlayer.get(key) != null){
                temp = mapRankNbPlayer.get(key);
            }
            mapRankNbPlayer.put(key, temp + 1);
            if (mapScoreNbPlayer.get(g.getScore()) != null) {
                mapScoreNbPlayer.put(g.getScore(), mapScoreNbPlayer.get(g.getScore())+1);
            } else {
                mapScoreNbPlayer.put(g.getScore(), 1);
            }
        }
        // add game score of player to the map
        int tempNbPlayer = 0;
        String keyGamePlayer = gamePlayer.getScore()+"-"+gamePlayer.getContractWS();
        if (useLead && gamePlayer.getBegins() != null) {
            keyGamePlayer += "-" + gamePlayer.getBegins();
        }
        if (mapRankNbPlayer.get(keyGamePlayer) != null) {
            tempNbPlayer = mapRankNbPlayer.get(keyGamePlayer);
        }
        mapRankNbPlayer.put(keyGamePlayer, tempNbPlayer+1);
        if (mapScoreNbPlayer.get(gamePlayer.getScore()) != null) {
            mapScoreNbPlayer.put(gamePlayer.getScore(), mapScoreNbPlayer.get(gamePlayer.getScore())+1);
        } else {
            mapScoreNbPlayer.put(gamePlayer.getScore(), 1);
        }

        List<WSResultDeal> listResult = new ArrayList<>();
        List<String> listScoreContract = new ArrayList<>();
        int idxPlayer = -1;
        if(listGame.isEmpty()) {
            idxPlayer = 0;
        }
        boolean playerDone = gamePlayer.getScore() == Constantes.CONTRACT_LEAVE;
        for (TourSerieGame g : listGame) {
            if (g.getScore() == Constantes.CONTRACT_LEAVE) {
                continue;
            }
            // need to add game player now ?
            if (!playerDone && gamePlayer.getScore() >= g.getScore()) {
                playerDone = true;
                String key = gamePlayer.getScore()+"-"+gamePlayer.getContractWS();
                if (useLead && gamePlayer.getBegins() != null) {
                    key += "-" + gamePlayer.getBegins();
                }
                if (!listScoreContract.contains(key)) {
                    listScoreContract.add(key);
                    WSResultDeal resDeal = new WSResultDeal();
                    resDeal.setContract(gamePlayer.getContractWS());
                    resDeal.setDealIDstr(dealID);
                    resDeal.setDealIndex(deal.index);
                    resDeal.setDeclarer(""+gamePlayer.getDeclarer());
                    resDeal.setRank(gamePlayer.getRank());
                    resDeal.setNbTotalPlayer(tour.getNbPlayers()+1);
                    resDeal.setLead(gamePlayer.getBegins());
                    if (mapRankNbPlayer.get(key) != null) {
                        resDeal.setNbPlayerSameGame(mapRankNbPlayer.get(key));
                    }
                    resDeal.setNbTricks(gamePlayer.getTricks());
                    resDeal.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
                    resDeal.setScore(gamePlayer.getScore());
                    resDeal.setResult(gamePlayer.getResult());
                    listResult.add(resDeal);
                    idxPlayer = listResult.size() - 1;
                }
            }
            String key = g.getScore()+"-"+g.getContractWS();
            if (useLead && g.getBegins() != null) {
                key += "-" + g.getBegins();
            }
            if (!listScoreContract.contains(key)) {
                listScoreContract.add(key);
                WSResultDeal resDeal = new WSResultDeal();
                resDeal.setContract(g.getContractWS());
                resDeal.setDealIDstr(dealID);
                resDeal.setDealIndex(deal.index);
                resDeal.setDeclarer(""+g.getDeclarer());
                resDeal.setRank(g.getRank());
                // change rank if necessary
                if (g.getScore() < gamePlayer.getScore()) {
                    resDeal.setRank(g.getRank() + 1);
                }
                resDeal.setNbTotalPlayer(tour.getNbPlayers()+1);
                resDeal.setLead(g.getBegins());
                if (mapRankNbPlayer.get(key) != null) {
                    resDeal.setNbPlayerSameGame(mapRankNbPlayer.get(key));
                }
                resDeal.setNbTricks(g.getTricks());
                resDeal.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
                resDeal.setScore(g.getScore());
                // rebuild result
                if (mapScoreNbPlayer.get(g.getScore()) != null) {
                    resDeal.setResult(Constantes.computeResultPaire(tour.getNbPlayers() + 1, resDeal.getRank() - 1, mapScoreNbPlayer.get(g.getScore())));
                } else {
                    resDeal.setResult(g.getResult());
                }
                listResult.add(resDeal);
            }
        }
        if (!playerDone) {
            WSResultDeal wsResultDeal = buildWSResultDeal(gamePlayer, playerCache, true);
            wsResultDeal.setNbPlayerSameGame(1);
            listResult.add(wsResultDeal);
        }

        resultDealTour.listResultDeal = listResult;
        resultDealTour.totalSize = listResult.size();
        WSTournament wstour = serieTournamentToWS(tour, playerCache);
        resultDealTour.tournament = wstour;
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_PLAYER_CONTRACT, gamePlayer.getContractWS()));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_INDEX_PLAYER, ""+idxPlayer));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, ""+(tour.getNbPlayers()+1)));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_DEAL_INDEX, ""+deal.index));
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
    public WSResultDealTournament resultDealNotGroup(String dealID, PlayerCache playerCache, List<Long> listPlaFilter, int offset, int nbMax) throws FBWSException {
        TourSerieTournament tour = serieMgr.getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        TourSerieDeal deal = (TourSerieDeal)tour.getDeal(dealID);
        if (tour == null) {
            log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // check deal played by this player
        SerieTopChallengeGame gamePlayer = getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
        if (gamePlayer == null) {
            log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
            throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
        }
        WSResultDealTournament resultDealTour = new WSResultDealTournament();
        List<WSResultDeal> listResult = new ArrayList<>();

        // list all games from DB order by score
        List<TourSerieGame> listGame = serieMgr.listGameFinishedOnTournamentAndDeal(tour.getIDStr(), deal.index, 0, 0);
        HashMap<Integer, Integer> mapScoreNbPlayer = new HashMap<>(); /* Map to store nb player for each score */
        for (TourSerieGame g : listGame) {
            if (mapScoreNbPlayer.get(g.getScore()) != null) {
                mapScoreNbPlayer.put(g.getScore(), mapScoreNbPlayer.get(g.getScore())+1);
            } else {
                mapScoreNbPlayer.put(g.getScore(), 1);
            }
        }
        // add game score of player to the map
        if (mapScoreNbPlayer.get(gamePlayer.getScore()) != null) {
            mapScoreNbPlayer.put(gamePlayer.getScore(), mapScoreNbPlayer.get(gamePlayer.getScore())+1);
        } else {
            mapScoreNbPlayer.put(gamePlayer.getScore(), 1);
        }
        int idxPlayer = -1;
        for (TourSerieGame g : listGame) {
            if (listPlaFilter != null) {
                if (!listPlaFilter.contains(g.getPlayerID())) {
                    continue;
                }
            }
            if (idxPlayer == -1 && gamePlayer.getScore() >= g.getScore()) {
                listResult.add(buildWSResultDeal(gamePlayer, playerCache, true));
                idxPlayer = listResult.size() - 1;
            }
            WSResultDeal resDeal = buildWSResultDeal(g, playerCacheMgr.getPlayerCache(g.getPlayerID()), presenceMgr.isSessionForPlayerID(g.getPlayerID()));
            // change rank if necessary
            if (g.getScore() < gamePlayer.getScore()) {
                resDeal.setRank(g.getRank() + 1);
            }
            // rebuild result
            if (mapScoreNbPlayer.get(g.getScore()) != null) {
                resDeal.setResult(Constantes.computeResultPaire(tour.getNbPlayers() + 1, resDeal.getRank() - 1, mapScoreNbPlayer.get(g.getScore())));
            } else {
                resDeal.setResult(g.getResult());
            }
            listResult.add(resDeal);
        }
        offset = 0;
        if (idxPlayer == -1) {
            listResult.add(buildWSResultDeal(gamePlayer, playerCache, true));
            idxPlayer = listResult.size() - 1;
        }
        resultDealTour.offset = offset;
        resultDealTour.totalSize = listResult.size();
        resultDealTour.listResultDeal = listResult;
        resultDealTour.tournament = serieTournamentToWS(tour, playerCache);
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_PLAYER_CONTRACT, gamePlayer.getContractWS()));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_INDEX_PLAYER, ""+idxPlayer));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_DEAL_INDEX, ""+deal.index));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, ""+resultDealTour.totalSize));
        return resultDealTour;
    }

    private WSResultDeal buildWSResultDeal(Game g, PlayerCache playerCache, boolean connected) {
        WSResultDeal resDeal = new WSResultDeal();
        resDeal.setContract(g.getContractWS());
        resDeal.setDealIDstr(g.getDealID());
        resDeal.setDealIndex(g.getDealIndex());
        resDeal.setDeclarer(""+g.getDeclarer());
        resDeal.setRank(g.getRank());
        resDeal.setNbTotalPlayer(g.getTournament().getNbPlayers()+1);
        resDeal.setNbTricks(g.getTricks());
        resDeal.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
        resDeal.setScore(g.getScore());
        resDeal.setResult(g.getResult());
        resDeal.setPlayerID(g.getPlayerID());
        resDeal.setLead(g.getBegins());
        if (playerCache != null) {
            resDeal.setPlayerPseudo(playerCache.getPseudo());
            resDeal.setCountryCode(playerCache.countryCode);
            resDeal.setConnected(connected);
            resDeal.setAvatarPresent(playerCache.avatarPublic);
        }
        resDeal.setGameIDstr(g.getIDStr());
        return resDeal;
    }

    /**
     * Return the period just after this periodID
     * @param periodID
     * @return
     */
    private TourSeriePeriod getNextPeriod(String periodID) {
        if (periodID != null && periodTopChallenge != null) {
            if (periodID.equals(periodTopChallenge.getPeriodID())) {
                return periodSerie;
            }
            Query q = Query.query(Criteria.where("periodID").gt(periodID));
            q.with(new Sort(Sort.Direction.ASC, "periodID"));
            return mongoTemplate.findOne(q, TourSeriePeriod.class);
        }
        return null;
    }

    /**
     * Build WSTournament associated to this serie tournament and change some value : category, nbTotalPlayer, beginDate, endDate & remainingTime
     * @param tour
     * @return
     */
    public WSTournament serieTournamentToWS(TourSerieTournament tour, PlayerCache playerCache) {
        WSTournament wsTour = tour.toWS();
        wsTour.categoryID = Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE;
        wsTour.nbTotalPlayer = tour.getNbPlayers()+1;
        SerieTopChallengeTournamentPlayer tournamentPlayer = getTournamentPlayer(playerCache.ID, tour.getIDStr());
        if (tournamentPlayer != null) {
            wsTour.resultPlayer = tournamentPlayer.toWSResultTournamentPlayer(playerCache, tour, playerCache.ID);
            if (wsTour.resultPlayer != null) {
                wsTour.playerOffset = wsTour.resultPlayer.getRank();
            }
            wsTour.currentDealIndex = tournamentPlayer.getCurrentDealIndex();
        }
        TourSeriePeriod tempPeriod = getNextPeriod(tour.getPeriod());
        if (tempPeriod != null) {
            wsTour.beginDate = tempPeriod.getTsDateStart();
            wsTour.endDate = tempPeriod.getTsDateEnd();
            if (tempPeriod.getTsDateEnd() > System.currentTimeMillis()) {
                wsTour.remainingTime = tempPeriod.getTsDateEnd() - System.currentTimeMillis();
            }
        }
        return wsTour;
    }

    public List<WSResultTournamentPlayer> resultListTournament(TourSerieTournament tour, int offset, int nbMaxResult, List<Long> listFollower, long playerAsk, WSResultTournamentPlayer resultPlayerAsk) {
        List<WSResultTournamentPlayer> listResult = new ArrayList<>();
        // get data from Serie DB
        Query q= new Query(Criteria.where("tournament.$id").is(new ObjectId(tour.getIDStr())));
        q.with(new Sort(Sort.Direction.ASC, "rank"));
        List<TourSerieTournamentPlayer> listSerieTP = mongoTemplate.find(q, TourSerieTournamentPlayer.class);
        boolean playerDone = false;
        if (!listSerieTP.isEmpty()) {
            for (TourSerieTournamentPlayer tp : listSerieTP) {
                if (!playerDone && resultPlayerAsk != null && tp.getResult() <= resultPlayerAsk.getResult()) {
                    listResult.add(resultPlayerAsk);
                    playerDone = true;
                }
                boolean addPlayer = true;
                if (listFollower != null && !listFollower.contains(tp.getPlayerID())) {
                    addPlayer = false;
                }
                if (addPlayer) {
                    WSResultTournamentPlayer rp = tp.toWSResultTournamentPlayer(playerCacheMgr.getPlayerCache(tp.getPlayerID()), playerAsk);
                    if (resultPlayerAsk != null && rp.getResult() < resultPlayerAsk.getResult()) {
                        rp.setRank(rp.getRank()+1);
                    }
                    rp.setNbTotalPlayer(rp.getNbTotalPlayer()+1);
                    listResult.add(rp);
                }
            }
        }
        if (!playerDone && listResult.size() < nbMaxResult) {
            listResult.add(resultPlayerAsk);
        }
        return listResult;
    }

    /**
     * Count number of tournament available (serie TOP, period and nbPlayers > limit)
     * @return
     */
    public int countTournamentAvailable() {
        if (periodTopChallenge != null) {
            String periodID = periodTopChallenge.getPeriodID();
            Query q = new Query();
            Criteria cSerie = Criteria.where("serie").is(TourSerieMgr.SERIE_TOP);
            Criteria cPeriod = Criteria.where("period").is(periodID);
            Criteria cNotEmpty = Criteria.where("nbPlayer").gte(getConfigIntValue("nbPlayerLimit", 5));
            q.addCriteria(new Criteria().andOperator(cSerie, cPeriod, cNotEmpty));
            return (int)mongoTemplate.count(q, TourSerieTournament.class);
        }
        return 0;
    }

        /**
     * List tournament available (serie TOP, period and nbPlayers > limit)
     * @return
     */
    public List<TourSerieTournament> listTournamentAvailable() {
        if (periodTopChallenge != null) {
            String periodID = periodTopChallenge.getPeriodID();
            Query q = new Query();
            Criteria cSerie = Criteria.where("serie").is(TourSerieMgr.SERIE_TOP);
            Criteria cPeriod = Criteria.where("period").is(periodID);
            Criteria cNotEmpty = Criteria.where("nbPlayer").gte(getConfigIntValue("nbPlayerLimit", 5));
            q.addCriteria(new Criteria().andOperator(cSerie, cPeriod, cNotEmpty));
            return mongoTemplate.find(q, TourSerieTournament.class);
        }
        return null;
    }

    /**
     * Count tournament player for current period
     * @param finished only with status finished true
     * @return
     */
    public int countTournamentPlayerForCurrentPeriod(boolean finished) {
        if (periodTopChallenge != null) {
            String periodID = periodTopChallenge.getPeriodID();
            if (!finished) {
                Query q = Query.query(Criteria.where("periodID").is(periodID));
                return (int)mongoTemplate.count(q, SerieTopChallengeTournamentPlayer.class);
            }
            Query q = Query.query(Criteria.where("periodID").is(periodID).andOperator(Criteria.where("finished").is(true)));
            return (int)mongoTemplate.count(q, SerieTopChallengeTournamentPlayer.class);
        }
        return -1;
    }

    @Override
    public void updateGamePlayArgineFinished(Game game) throws FBWSException {

    }

    @Override
    public void updateGamePlayArgine(Game game) {

    }
}
