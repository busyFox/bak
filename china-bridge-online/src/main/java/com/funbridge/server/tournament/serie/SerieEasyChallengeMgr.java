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
import java.util.stream.Collectors;

@Component(value="serieEasyChallengeMgr")
@Scope(value="singleton")
public class SerieEasyChallengeMgr extends TournamentGenericMgr implements ITournamentMgr {

    private static final String CONFIG_NAME = "SERIE_EASY_CHALLENGE";

    @Resource(name="mongoSerieTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name="tourSerieMgr")
    private TourSerieMgr serieMgr;

    private LockWeakString lockCreateGame = new LockWeakString();
    private LockWeakString lockCreateTournamentPlayer = new LockWeakString();
    private TourSeriePeriod periodEasyChallenge = null;
    private TourSeriePeriod periodSerie = null;
    private boolean periodChangeProcessing = false;
    private boolean enable = false;

    @Override
    @PostConstruct
    public void init() {
        this.log.info("init");
        this.gameMgr = new GameMgr(this);
    }

    @Override
    @PreDestroy
    public void destroy() {
        this.log.info("destroy");
        if (this.gameMgr != null) {
            this.gameMgr.destroy();
        }
    }

    @Override
    public void startUp() {
        // Initialize current period. Must be run after startup of SerieMgr.
        this.initPeriod();
    }

    @Override
    public String getTournamentCategoryName() {
        return Constantes.TOURNAMENT_CATEGORY_NAME_SERIE_EASY_CHALLENGE;
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
        checkSerieEasyChallengeEnable();
    }

    /**
     * Called by game mgr when game is finished. Save game data in DB.
     * @param session the user session.
     */
    @Override
    public void updateGameFinished(final FBSession session) throws FBWSException {
        if (session == null) {
            log.error("Session parameter is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        final Table currentGameTable = session.getCurrentGameTable();
        if (currentGameTable == null) {
            log.error("Table in session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        final Game game = session.getCurrentGameTable().getGame();
        if(!(game instanceof SerieEasyChallengeGame)) {
            log.error("Game in session not valid ! - table=" + currentGameTable + " - game=" + game);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        final SerieEasyChallengeGame serieEasyChallengeGame = (SerieEasyChallengeGame) game;
        if (!serieEasyChallengeGame.isReplay()) {
            if (serieEasyChallengeGame.isFinished()) {
                // Compute results and ranks on the game and the tournament player
                this.setRankResultForGameAndTournamentPlayer(serieEasyChallengeGame, true);
                // Remove the game from the session
                session.removeGame();
                // Remove the player from the set
                this.gameMgr.removePlayerRunning(session.getPlayer().getID());
            } else {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Nothing to do ... game not finished - game="+game);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Nothing to do ... replay game - game="+game);
            }
        }
    }

    /**
     * Compute rank & result for game and for tournament player.
     * Save the object into the DB.
     * @param game the game to compute.
     * @param updateGame update flag.
     * @throws FBWSException if there is any issue during the execution.
     */
    private void setRankResultForGameAndTournamentPlayer(final SerieEasyChallengeGame game, final boolean updateGame) throws FBWSException {
        if (game != null) {
            // Compute results and ranks on the game
            final int nbBest = this.serieMgr.countGameWithBestScore(game.getTournament().getIDStr(), game.getDealIndex(), game.getScore());
            game.setRank(nbBest + 1); // +1 : The curent player is behind the better players
            final int nbSame = this.serieMgr.countGameWithSameScore(game.getTournament().getIDStr(), game.getDealIndex(), game.getScore()) + 1; // +1 : include the current player
            game.setResult(Constantes.computeResultPaire(game.getTournament().getNbPlayers() + 1, nbBest, nbSame));  // +1 : add the current player to the overall number of players
            // Update the data of the game and table in DB if required
            if (updateGame) {
                this.updateGameDB(game);
            }
            // Compute the result and rank on the tournament player
            final SerieEasyChallengeTournamentPlayer tournamentPlayer = this.getTournamentPlayer(game.getTournament().getIDStr(), game.getPlayerID());
            if (tournamentPlayer != null) {
                final double playerResult = ((tournamentPlayer.getResult() * tournamentPlayer.getNbPlayedDeals()) + game.getResult()) / (tournamentPlayer.getNbPlayedDeals() + 1);
                tournamentPlayer.setResult(playerResult);
                final int playerRank = this.serieMgr.countTournamentPlayerWithBetterResult(game.getTournament().getIDStr(), tournamentPlayer.getResult()) + 1;
                tournamentPlayer.setRank(playerRank);
                tournamentPlayer.addPlayedDeals(game.getDealID());
                tournamentPlayer.setCurrentDealIndex(-1);
                tournamentPlayer.setLastDate(System.currentTimeMillis());
                if (tournamentPlayer.getNbPlayedDeals() == game.getTournament().getNbDeals()) {
                    tournamentPlayer.setFinished(true);
                }
                this.updateTournamentPlayerInDB(tournamentPlayer);
            }
        }
    }

    /**
     * Save the tournament into the DB
     * @param tournament the tournament.
     */
    @Override
    public void updateTournamentDB(final Tournament tournament) {
        // Nothing to do in this mode.
    }

    /**
     * Build the deal ID using the tournamentID and deal index
     * @param tournamentID the identifier of the tournament.
     * @param index the index of the deal in the tournament.
     * @return a String corresponding to the identifier of the deal.
     */
    @Override
    public String _buildDealID(final String tournamentID, final int index) {
        return TourSerieMgr.buildDealID(tournamentID, index);
    }

    /**
     * Extract the tournament identifier from the deal identifier.
     * @param dealID the identifier of the deal.
     * @return a String corresponding to the identifier of the tournament.
     */
    @Override
    public String _extractTourIDFromDealID(final String dealID) {
        return TourSerieMgr.extractTourIDFromDealID(dealID);
    }

    /**
     * Extract the deal index from the deal identifier.
     * @param dealID the identifier of the deal.
     * @return a String corresponding to the identifier of the tournament.
     */
    @Override
    public int _extractDealIndexFromDealID(final String dealID) {
        return TourSerieMgr.extractDealIndexFromDealID(dealID);
    }
    /**
     * Find the tournament in the DB.
     * @param tournamentID the identifier of the tournament.
     * @return the tournament from the DB.
     */
    @Override
    public TourSerieTournament getTournament(final String tournamentID) {
        return this.mongoTemplate.findById(new ObjectId(tournamentID), TourSerieTournament.class);
    }

    /**
     * Find the game in the DB.
     * @param gameID the identifier of the game.
     * @return the tournament from the DB.
     */
    @Override
    public SerieEasyChallengeGame getGame(final String gameID) {
        return this.mongoTemplate.findById(new ObjectId(gameID), SerieEasyChallengeGame.class);
    }

    public boolean isEnable() {
        if (getConfigIntValue("enable", 1) != 1) {
            return false;
        }
        return this.enable;
    }

    public void setEnable(boolean value) {
        this.enable = value;
    }

    /**
     * Initialize the period.
     */
    public void initPeriod() {
        this.log.warn("Init period");
        this.periodEasyChallenge = serieMgr.getPreviousPeriod(1);
        if (this.periodEasyChallenge != null) {
            this.periodSerie = this.serieMgr.getCurrentPeriod();
            if (periodSerie != null) {
                this.log.warn("Used period Easy Challenge=" + periodEasyChallenge.getPeriodID() + " - period Serie=" + periodSerie.getPeriodID());
                this.enable = true;
            } else {
                this.log.error("Current period from serie is null");
            }
        } else {
            this.log.error("Previous period from serie is null !!");
            this.enable = false;
        }
    }

    public TourSeriePeriod getPeriodEasyChallenge() {
        return this.periodEasyChallenge;
    }

    public TourSeriePeriod getPeriodSerie() {
        return this.periodSerie;
    }

    public void checkSerieEasyChallengeEnable() throws FBWSException {
        if (!isEnable()) {
            throw new FBWSException(FBExceptionType.SERIE_EASY_CHALLENGE_CLOSED);
        }
        if (this.periodChangeProcessing) {
            throw new FBWSException(FBExceptionType.SERIE_EASY_CHALLENGE_PERIOD_PROCESSING);
        }
        if (this.periodEasyChallenge == null || this.periodSerie == null) {
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    public void setPeriodChangeProcessing(final boolean value) {
        this.periodChangeProcessing = value;
    }

    public boolean isPeriodChangeProcessing() {
        return this.periodChangeProcessing;
    }

    /**
     * Thread that changes the period.
     */
    private class FinishPeriodThread implements Runnable {
        @Override
        public void run() {
            changePeriod();
        }
    }

    /**
     * Initialize and start a thread to change the period.
     */
    public void startThreadChangePeriod() {
        final Thread threadChangePeriod = new Thread(new FinishPeriodThread());
        threadChangePeriod.start();
    }

    /**
     * Close the current period and switch to the next one.
     */
    public void changePeriod() {
        if (this.periodChangeProcessing) {
            this.log.error("Period change already in process ...");
        } else {
            // Disable the current period
            this.setPeriodChangeProcessing(true);
            this.log.warn("Begin change period of Serie Easy Challenge - periodEasyChallenge=" + this.periodEasyChallenge + " - periodSerie=" + this.periodSerie);
            // Finished all the games and tournament player in progress
            final Query q = Query.query(Criteria.where("periodID").is(this.periodEasyChallenge.getPeriodID()).andOperator(Criteria.where("finished").is(false)));
            final List<SerieEasyChallengeTournamentPlayer> listTourPlayerNotFinished = this.mongoTemplate.find(q, SerieEasyChallengeTournamentPlayer.class);
            this.log.warn("Nb tournament player in progress = " + listTourPlayerNotFinished.size());
            for (final SerieEasyChallengeTournamentPlayer tp : listTourPlayerNotFinished) {
                try {
                    this.processLeaveTournament(null, tp.getTournament(), tp.getPlayerID(), 0);
                } catch (Exception e) {
                    this.log.error("Failed to leave tournament for player - tp=" + tp, e);
                }
            }
            this.log.warn("End leave all tournament not finished.");
            // Change the periodID
            this.initPeriod();
            // Enable the new current period
            this.setPeriodChangeProcessing(false);
        }
    }

    @Override
    public GameMgr getGameMgr() {
        return gameMgr;
    }

    @Override
    public Class<SerieEasyChallengeGame> getGameEntity() {
        return SerieEasyChallengeGame.class;
    }

    @Override
    public Class<TourSerieGame> getAggregationGameEntity() {
        return TourSerieGame.class;
    }

    @Override
    public String getGameCollectionName() {
        return "easy_serie_game";
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
     * @param session the current user session.
     * @param conventionProfil the convention profile of the user.
     * @param conventionValue the convention value of the user.
     * @param cardsConventionProfil the cards convention profile of the user.
     * @param cardsConventionValue the cards convention value of the user.
     * @return an object corresponding to the table in the tournament for the WS.
     * @throws FBWSException if there is any issue during the execution.
     */
    public WSTableTournament playTournament(final FBSession session, final int conventionProfil, final String conventionValue, final int cardsConventionProfil, final String cardsConventionValue) throws FBWSException {
        if (session == null) {
            this.log.error("Session is null");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        final Player player = session.getPlayer();
        Table table = null;
        TourSerieTournament tournament = null;
        SerieEasyChallengeTournamentPlayer tournamentPlayer = null;
        // TOURNAMENT
        // Check for tournament in progress in the session
        if(session.getCurrentGameTable() != null &&
                !session.getCurrentGameTable().isReplay() &&
                session.getCurrentGameTable().getTournament() != null &&
                session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE &&
                session.getCurrentGameTable().getGame() != null &&
                !session.getCurrentGameTable().getGame().isFinished()){
            table = session.getCurrentGameTable();
            tournament = (TourSerieTournament) table.getTournament();
        }
        // Check for tournament in progress in the DB if the tournament wasn't in session
        if(tournament == null){
            tournament = getTournamentInProgressForPlayer(player.getID());
        }
        // If there's no tournament in progress, find a new one to play
        if(tournament == null){
            tournament = getNextTournamentToPlayForPlayer(player.getID());
            // No tournament found : there is no tournament anymore for the current period
            if (tournament == null) {
                throw new FBWSException(FBExceptionType.SERIE_EASY_CHALLENGE_ALL_PLAYED);
            }
        }
        // TABLE
        if(table == null){
            tournament.setCategory(Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE);
            table = new Table(session.getPlayer(), tournament);
            tournamentPlayer = this.getTournamentPlayer(tournament.getIDStr(), session.getPlayer().getID());
            if(tournamentPlayer != null){
                table.setPlayedDeals(tournamentPlayer.getPlayedDeals());
            }
            // Retrieve the game if it exists
            final SerieEasyChallengeGame game = this.getNotFinishedGameForTournamentAndPlayer(tournament.getIDStr(), session.getPlayer().getID());
            if(game != null){
                table.setGame(game);
            }
        }
        session.setCurrentGameTable(table);
        // GAME
        SerieEasyChallengeGame game;
        // If the table already has a game
        if(table.getGame() != null &&
                !table.getGame().isReplay() &&
                !table.getGame().isFinished() &&
                table.getGame().getTournament().getIDStr().equals(tournament.getIDStr())){
            game = (SerieEasyChallengeGame) table.getGame();
        } else {
            // If no game is associated to the table, look for an existing one in DB
            game = this.getNotFinishedGameForTournamentAndPlayer(tournament.getIDStr(), session.getPlayer().getID());
            if(game == null){
                // Check the player credit
                this.playerMgr.checkPlayerCredit(player, tournament.getNbCreditsPlayDeal());
                // TODO : Ici je vais chercher le tournamentPlayer en base pour avoir le lastIndexPlayed
                int lastIndexPlayed = 0;
                if (tournamentPlayer == null) {
                    tournamentPlayer = this.getTournamentPlayer(tournament.getIDStr(), session.getPlayer().getID());
                }
                if(tournamentPlayer != null){
                    lastIndexPlayed = tournamentPlayer.getNbPlayedDeals();
                    if(lastIndexPlayed >= tournament.getNbDeals()){
                        this.log.error("Player already played all " + tournament.getNbDeals() + " tournament deals, can't play another one ! player=" + session.getPlayer() + " - nbPlayedDeals=" + tournamentPlayer.getNbPlayedDeals() + " - tour=" + tournament);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    }
                }
                int bidsProfil = conventionProfil;
                int cardsProfil = cardsConventionProfil;
                if (bidsProfil == 0) {
                    bidsProfil = this.gameMgr.getArgineEngineMgr().getDefaultProfile();
                }
                if (cardsProfil == 0) {
                    cardsProfil = this.gameMgr.getArgineEngineMgr().getDefaultProfileCards();
                }
                // Create a new game
                game = this.createGame(session.getPlayer().getID(), tournament, lastIndexPlayed + 1, bidsProfil, conventionValue, cardsProfil, cardsConventionValue, session.getDeviceID());
                if (game != null) {
                    ContextManager.getPlayerMgr().updatePlayerCreditDeal(player, tournament.getNbCreditsPlayDeal(), 1);
                } else {
                    this.log.error("Failed to create game - player=" + player.getID() + " - tour=" + tournament);
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE, 1);
            }
            table.setGame(game);
        }
        // Add the player in the tournament or update it
        if (tournamentPlayer == null) {
            tournamentPlayer = this.getOrCreateTournamentPlayer(session.getPlayer().getID(), tournament);
        }
        if (tournamentPlayer.getCurrentDealIndex() != game.getDealIndex()) {
            tournamentPlayer.setCurrentDealIndex(game.getDealIndex());
            tournamentPlayer.setLastDate(System.currentTimeMillis());
            this.updateTournamentPlayerInDB(tournamentPlayer);
        }
        // Check if a thread is running for this game
        final GameThread robotThread = this.gameMgr.getThreadPlayRunning(game.getIDStr());
        if(robotThread != null){
            if(this.log.isDebugEnabled()){
                this.log.debug("A thread is currently running for gameID=" + game.getIDStr() + " - stop it !");
            }
            robotThread.interruptRun();
        }
        // Build the data to return as a WS result
        final WSTableTournament tableTournament = new WSTableTournament();
        tableTournament.tournament = this.serieTournamentToWS(tournament, session.getPlayerCache());
        tableTournament.tournament.currentDealIndex = game.getDealIndex();
        tableTournament.currentDeal = new WSGameDeal();
        tableTournament.currentDeal.setDealData(game.getDeal(), tournament.getIDStr());
        tableTournament.currentDeal.setGameData(game);
        tableTournament.table = table.toWSTableGame();
        tableTournament.gameIDstr = game.getIDStr();
        tableTournament.conventionProfil = game.getConventionProfile();
        tableTournament.creditAmount = session.getPlayer().getTotalCreditAmount();
        tableTournament.nbPlayedDeals = session.getPlayer().getNbPlayedDeals();
        tableTournament.replayEnabled = this.playerMgr.isReplayEnabledForPlayer(session);
        tableTournament.freemium = session.isFreemium();
        return tableTournament;
    }

    /**
     * Create a game for the player in the tournament (at dealIndex).
     * @param playerID the identifier of the player.
     * @param tournament the tournament.
     * @param dealIndex the index of the deal in the tournament
     * @param conventionProfil the convention profile of the user.
     * @param conventionData the convention data of the user.
     * @param cardsConventionProfil the cards convention value of the user.
     * @param cardsConventionData the cards convention data value of the user.
     * @param deviceID the device identifier of the user.
     * @return an object corresponding to the game in the tournament for the WS.
     * @throws FBWSException if an existing game existed for player, tournament and dealIndex or if an exception occurs
     */
    private SerieEasyChallengeGame createGame(final long playerID, final TourSerieTournament tournament, final int dealIndex, final int conventionProfil, final String conventionData, final int cardsConventionProfil, final String cardsConventionData, final long deviceID) throws FBWSException {
        synchronized (this.lockCreateGame.getLock(tournament.getIDStr() + "-" + playerID)) {
            if (this.getGameOnTournamentAndDealForPlayer(tournament.getIDStr(), dealIndex, playerID) != null) {
                this.log.error("A game already exist on tour=" + tournament + " - dealIndex=" + dealIndex + " - playerID=" + playerID);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
            try {
                final SerieEasyChallengeGame game = new SerieEasyChallengeGame(playerID, tournament, dealIndex);
                game.setStartDate(Calendar.getInstance().getTimeInMillis());
                game.setLastDate(Calendar.getInstance().getTimeInMillis());
                game.setConventionSelection(conventionProfil, conventionData);
                game.setCardsConventionSelection(cardsConventionProfil, cardsConventionData);
                game.setDeviceID(deviceID);
                // Try to find the bug of "invalid game" ...
                if (game.isFinished() && game.getContractType() == 0 && game.getBids().length() == 0) {
                    throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                }
                this.mongoTemplate.insert(game);
                return game;
            } catch (final Exception e) {
                this.log.error("Exception to create game player=" + playerID + " - tour=" + tournament + " - dealIndex=" + dealIndex, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Leave the current tournament. All deals not played are set to leave
     * @param session the session of the user.
     * @param tourIDstr the tournament identifier (as a String)
     * @return the remaining credit of the player.
     * @throws FBWSException if there is an issue during the execution.
     */
    @Override
    public int leaveTournament(final FBSession session, final String tourIDstr) throws FBWSException {
        if (session == null) {
            this.log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        final Table table = session.getCurrentGameTable();
        if (table == null || table.getGame() == null) {
            this.log.error("Game or table is null in session table=" + table + " - player=" + session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!(table.getTournament() instanceof TourSerieTournament)) {
            this.log.error("Tournament on table is not TourSerieTournament table=" + table + " - player=" + session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        final SerieEasyChallengeGame game = (SerieEasyChallengeGame) session.getCurrentGameTable().getGame();
        if (game == null) {
            this.log.error("Game is null in session loginID=" + session.getLoginID());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!(table.getGame() instanceof SerieEasyChallengeGame)) {
            this.log.error("Game on table is not SerieEasyChallengeGame table=" + table + " - player=" + session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // No leave for replay
        if (game.isReplay()) {
            this.log.error("No leave for replay game=" + game);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        if (!game.getTournament().getIDStr().equals(tourIDstr)) {
            this.log.error("Tournament of current game is not same as tourIDstr=" + tourIDstr + " - game=" + game);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        this.checkGame(game);
        final Player p = session.getPlayer();
        // Check if the player has enough credit to leave the tournament
        final int nbDealToLeave = game.getTournament().getNbDeals() - table.getNbPlayedDeals() - 1; // -1 because credit already decrement for current game !
        if (nbDealToLeave > 0) {
            this.playerMgr.checkPlayerCredit(p, game.getTournament().getNbCreditsPlayDeal() * nbDealToLeave);
        }
        table.addPlayedDeal(game.getDealID());
        final List<SerieEasyChallengeGame> listGameLeaved = processLeaveTournament(game, game.getTournament(), p.getID(), session.getDeviceID());
        if (listGameLeaved != null) {
            for (final SerieEasyChallengeGame g : listGameLeaved) {
                table.addPlayedDeal(g.getDealID());
            }
        }
        // Remove the game from the session
        session.removeGame();
        // Remove the player from the set
        this.gameMgr.removePlayerRunning(p.getID());
        // Update the player data
        if (listGameLeaved != null && !listGameLeaved.isEmpty()) {
            this.playerMgr.updatePlayerCreditDeal(p,  game.getTournament().getNbCreditsPlayDeal()*listGameLeaved.size(), listGameLeaved.size());
            session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE, listGameLeaved.size());
        }
        // Return the remaining credit of player
        return p.getCreditAmount();
    }

    /**
     * Process leave on a tournament and return list of games created with leave value.
     * If a current game exist, this game is not added to the result list.
     * If the game parameter is null, try to find the game in progress.
     * @param game the game.
     * @param tour the tournament.
     * @param playerID the identifier of the player.
     * @param deviceID the device identifier of the player.
     * @return a list of games created with leave value.
     * @throws FBWSException if there is an issue during the execution.
     */
    private List<SerieEasyChallengeGame> processLeaveTournament(SerieEasyChallengeGame game, final TourSerieTournament tour, final long playerID, final long deviceID) throws FBWSException{
        if (game == null) {
            game = this.getNotFinishedGameForTournamentAndPlayer(tour.getIDStr(), playerID);
        }
        int lastDealIndexPlay = 0;
        if (game != null) {
            // Leave the current game
            synchronized (this.getGameMgr().getLockOnGame(game.getIDStr())) {
                if (game.getBidContract() == null) {
                    // No contract : leave
                    game.setLeaveValue();
                } else {
                    // Contract exist : claim with 0 trick
                    game.claimForPlayer(BridgeConstantes.POSITION_SOUTH, 0);
                    game.setFinished(true);
                    game.setLastDate(Calendar.getInstance().getTimeInMillis());
                    // Compute the score
                    GameMgr.computeScore(game);
                }
                // set rank, result for game & tournament player
                this.setRankResultForGameAndTournamentPlayer(game, true);
                lastDealIndexPlay = game.getDealIndex();
            }
        }
        // Leave the game for other deals
        final List<SerieEasyChallengeGame> listGameLeaved = new ArrayList<>();
        for (int i = lastDealIndexPlay+1; i <= tour.getNbDeals(); i++) {
            if (this.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), i, playerID) == null) {
                final SerieEasyChallengeGame g = new SerieEasyChallengeGame(playerID, tour, i);
                g.setStartDate(Calendar.getInstance().getTimeInMillis());
                g.setLastDate(Calendar.getInstance().getTimeInMillis());
                g.setDeviceID(deviceID);
                g.setLeaveValue();
                this.setRankResultForGameAndTournamentPlayer(g, false);
                listGameLeaved.add(g);
            }
        }
        if (!listGameLeaved.isEmpty()) {
            try {
                // Try to find the bug of "invalid game" ...
                if (this.getConfigIntValue("findBugGameNotValid", 0) == 1) {
                    for (final SerieEasyChallengeGame g : listGameLeaved) {
                        if (g.isFinished() && g.getContractType() == 0 && g.getBids().length() == 0) {
                            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                        }
                    }
                }
                this.mongoTemplate.insertAll(listGameLeaved);
            } catch (Exception e) {
                this.log.error("Exception to create leave games for playerID=" + playerID + " - tour=" + tour + " - listGameLeaved size=" + listGameLeaved.size(), e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
        // Finish tournament player if needed
        final SerieEasyChallengeTournamentPlayer tournamentPlayer = getTournamentPlayer(tour.getIDStr(), playerID);
        if (tournamentPlayer != null && !tournamentPlayer.isFinished()) {
            tournamentPlayer.setFinished(true);
            this.updateTournamentPlayerInDB(tournamentPlayer);
        }
        return listGameLeaved;
    }

    /**
     * Prepare the data to replay a deal.
     * @param player the player.
     * @param gamePlayed the game.
     * @param conventionProfil the convention profile of the user.
     * @param conventionValue the convention value of the user.
     * @param cardsConventionProfil the cards convention profile of the user.
     * @param cardsConventionValue the cards convention value of the user.
     * @return the table used in the replay mode.
     * @throws FBWSException if there is any issue during the execution.
     */
    @Override
    public Table createReplayTable(final Player player, final Game gamePlayed, final int conventionProfil, final String conventionValue, final int cardsConventionProfil, final String cardsConventionValue) throws FBWSException {
        if (player == null) {
            this.log.error("Param player is null");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // Create a table
        final Table table = new Table(player, gamePlayed.getTournament());
        table.setReplay(true);
        // Create a game
        final SerieEasyChallengeGame replayGame = new SerieEasyChallengeGame(player.getID(), (TourSerieTournament)gamePlayed.getTournament(), gamePlayed.getDealIndex());
        replayGame.setReplay(true);
        replayGame.setReplayGameID(player.getID() + "-" + System.nanoTime()); // Should take care of any concurrency issue
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
     * Return the gameView object for this game with game data and table.
     * The player must have played the deal of the game.
     * @param gameID the game identifier.
     * @param player the player.
     * @return a WSGameView for the WS.
     * @throws FBWSException if there is any issue during the execution.
     */
    @Override
    public WSGameView viewGame(final String gameID, final Player player) throws FBWSException {
        // Find the game in SerieEasyChallengeGame
        Game game = this.getGame(gameID);
        if (game == null) {
            // Not found ... try to find it in serie game
            game = this.serieMgr.getGame(gameID);
            if (game == null) {
                this.log.error("GameID not found - gameID=" + gameID + " - player=" + player);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
        }
        // Check if the player has played this deal
        if ((game.getPlayerID() != player.getID()) && this.getGameOnTournamentAndDealForPlayer(game.getTournament().getIDStr(), game.getDealIndex(), player.getID()) == null) {
            this.log.error("Deal not played by player - gameID=" + gameID + " - playerID=" + player.getID() + " - tour=" + game.getTournament() + " - deal index=" + game.getDealIndex());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (game.getPlayerID() == player.getID() && !game.isFinished()) {
            this.log.error("Deal not finished by player - gameID=" + gameID + " - playerID=" + player.getID());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        final WSTableGame table = new WSTableGame();
        table.tableID = -1;
        table.playerSouth = WSGamePlayer.createGamePlayerHuman(game.getPlayerID() == player.getID() ? player : this.playerMgr.getPlayer(game.getPlayerID()), Constantes.PLAYER_STATUS_PRESENT, player.getID());
        table.playerWest = WSGamePlayer.createGamePlayerRobot();
        table.playerNorth = WSGamePlayer.createGamePlayerRobot();
        table.playerEast = WSGamePlayer.createGamePlayerRobot();
        final WSGameDeal gameDeal = new WSGameDeal();
        gameDeal.setDealData(game.getDeal(), game.getTournament().getIDStr());
        gameDeal.setGameData(game);
        final WSGameView gameView = new WSGameView();
        gameView.game = gameDeal;
        gameView.table = table;
        gameView.tournament = this.serieTournamentToWS((TourSerieTournament) game.getTournament(), this.playerCacheMgr.getPlayerCache(player.getID()));
        return gameView;
    }

    /**
     * Return a gameView for the deal, score and contract.
     * If player has played this deal with this contract and score, return the game of this player else return the game of unknown player
     * @param dealID the deal identifier.
     * @param score the score of the game.
     * @param contractString the contract of the game.
     * @param player the player.
     * @return a GameView for the WS.
     * @throws FBWSException if there is any issue during the execution.
     */
    public WSGameView viewGameForDealScoreAndContract(final String dealID, final int score, final String contractString, final String lead, final Player player) throws FBWSException {
        final TourSerieTournament tour = this.serieMgr.getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        final TourSerieDeal deal = (TourSerieDeal) tour.getDeal(dealID);
        if (tour == null) {
            this.log.error("No deal found with this ID=" + dealID + " - tour=" + tour);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // Check if the player has already played the deal of this game
        Game game = this.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, player.getID());
        if (game == null) {
            this.log.error("PLAYER DOESN'T PLAY THIS DEAL : dealID="+dealID+" - playerID="+player.getID());
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        // Check the deal
        boolean bLoadGameForUnknown = true;
        if (game.getContractWS().equals(contractString) && game.getScore() == score) {
            if (lead != null && lead.length() > 0) {
                if (game.getBegins().equals(lead)) {
                    bLoadGameForUnknown = false;
                }
            } else {
                bLoadGameForUnknown = false;
            }
        }
        // No game found for this contract and score for the player : find from any player
        if (bLoadGameForUnknown) {
            String contract = Constantes.contractStringToContract(contractString);
            int contractType = Constantes.contractStringToType(contractString);
            if (contractType == Constantes.CONTRACT_TYPE_PASS) {
                contract = "";
            }
            final List<Criteria> listCriteria = new ArrayList<>();
            listCriteria.add(Criteria.where("tournament.$id").is(new ObjectId(tour.getIDStr())));
            listCriteria.add(Criteria.where("dealIndex").is(deal.index));
            listCriteria.add(Criteria.where("finished").is(true));
            listCriteria.add(Criteria.where("score").is(score));
            listCriteria.add(Criteria.where("contract").is(contract));
            listCriteria.add(Criteria.where("contractType").is(contractType));
            if (lead != null && lead.length() > 0) {
                listCriteria.add(Criteria.where("cards").regex("^"+lead));
            }
            final Query q = Query.query(new Criteria().andOperator(listCriteria.toArray(new Criteria[listCriteria.size()])));
            q.limit(1);
            try {
                game = this.mongoTemplate.findOne(q, TourSerieGame.class);
            } catch (Exception e) {
                this.log.error("Error to retrieve data for deal="+dealID+" score="+score+" and contract="+contract,e);
            }
        }
        if (game == null) {
            this.log.error("NO GAME FOUND FOR THIS DEAL, SCORE AND CONTRACT - dealID=" + dealID + " - score=" + score + " - contract=" + contractString);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        final WSTableGame table = new WSTableGame();
        table.tableID = -1;
        table.playerSouth = WSGamePlayer.createGamePlayerHuman( game.getPlayerID()==player.getID() ? player : this.playerMgr.getPlayer(game.getPlayerID()), Constantes.PLAYER_STATUS_PRESENT, player.getID());
        table.playerWest = WSGamePlayer.createGamePlayerRobot();
        table.playerNorth = WSGamePlayer.createGamePlayerRobot();
        table.playerEast = WSGamePlayer.createGamePlayerRobot();
        final WSGameDeal gameDeal = new WSGameDeal();
        gameDeal.setDealData(game.getDeal(), game.getTournament().getIDStr());
        gameDeal.setGameData(game);
        final WSGameView gameView = new WSGameView();
        gameView.game = gameDeal;
        gameView.table = table;
        gameView.tournament = this.serieTournamentToWS((TourSerieTournament) game.getTournament(), this.playerCacheMgr.getPlayerCache(player.getID()));
        return gameView;
    }

    /**
     * Return summary at end replay game
     * @param session the session of the user.
     * @param dealID the deal identifier.
     * @return the replay summary of the deal for the WS.
     * @throws FBWSException if there is any issue during the execution.
     */
    @Override
    public WSResultReplayDealSummary resultReplaySummary(FBSession session, String dealID) throws FBWSException {
        if (session == null) {
            this.log.error("Session is null !");
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        SerieEasyChallengeGame replayGame = null;
        if (session.getCurrentGameTable() != null && session.getCurrentGameTable().getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
            replayGame = (SerieEasyChallengeGame) session.getCurrentGameTable().getGame();
        }
        if (replayGame == null) {
            this.log.error("No replay game in session !");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (!replayGame.isReplay()) {
            this.log.error("Game in session is not a replay game! - game=" + replayGame);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }

        if (!replayGame.getDealID().equals(dealID)) {
            this.log.error("Replay game in session is for dealID=" + replayGame.getDealID() + " - and not for dealID=" + dealID);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        final String tourID = replayGame.getTournament().getIDStr();
        final int dealIndex = replayGame.getDealIndex();
        final long playerID = session.getPlayer().getID();
        final SerieEasyChallengeGame originalGame = this.getGameOnTournamentAndDealForPlayer(tourID, dealIndex, playerID);
        if (originalGame == null) {
            this.log.error("Original game not found ! dealID=" + dealID + " - player=" + session.getPlayer());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        final int nbTotalPlayer = replayGame.getTournament().getNbPlayers() + 1;
        final double resultOriginal = originalGame.getResult();
        final int rankOriginal = originalGame.getRank();
        // Original result
        final WSResultDeal resultPlayer = new WSResultDeal();
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
        // Replay result
        final WSResultDeal resultReplay = new WSResultDeal();
        resultReplay.setDealIDstr(dealID);
        resultReplay.setDealIndex(dealIndex);
        resultReplay.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
        resultReplay.setNbTotalPlayer(nbTotalPlayer+1); // + 1 to count replay game !
        resultReplay.setContract(replayGame.getContractWS());
        resultReplay.setDeclarer(Character.toString(replayGame.getDeclarer()));
        resultReplay.setNbTricks(replayGame.getTricks());
        resultReplay.setScore(replayGame.getScore());
        final int nbPlayerWithBestScoreReplay = this.serieMgr.countGameWithBestScore(tourID, dealIndex, replayGame.getScore());
        resultReplay.setRank(nbPlayerWithBestScoreReplay + 1);
        resultReplay.setNbPlayerSameGame(this.serieMgr.countGameWithSameScoreAndContract(tourID, dealIndex, replayGame.getScore(), replayGame.getContract(), replayGame.getContractType()));
        int nbPlayerSameScore = this.serieMgr.countGameWithSameScore(tourID, dealIndex, replayGame.getScore());
        final double replayResult = Constantes.computeResultPaire(nbTotalPlayer, nbPlayerWithBestScoreReplay, nbPlayerSameScore);
        resultReplay.setResult(replayResult);
        resultReplay.setLead(replayGame.getBegins());
        // Result for the most played games
        final WSResultDeal resultMostPlayed = this.serieMgr.buildWSResultGameMostPlayed(tourID, dealIndex, nbTotalPlayer);
        if (resultMostPlayed == null) {
            this.log.error("No result for most played game for tourID="+tourID+" - dealIndex="+dealIndex);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        resultMostPlayed.setDealIDstr(dealID);
        resultMostPlayed.setDealIndex(replayGame.getDealIndex());
        resultMostPlayed.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
        resultMostPlayed.setNbTotalPlayer(nbTotalPlayer);
        if (!replayGame.getTournament().isFinished()) {
            final int nbPlayerWithBestScoreMP = this.serieMgr.countGameWithBestScore(tourID, dealIndex, resultMostPlayed.getScore());
            resultMostPlayed.setRank(nbPlayerWithBestScoreMP + 1);
            nbPlayerSameScore = this.serieMgr.countGameWithSameScore(tourID, dealIndex, resultMostPlayed.getScore());
            resultMostPlayed.setResult(Constantes.computeResultPaire(nbTotalPlayer, nbPlayerWithBestScoreMP, nbPlayerSameScore));
        }
        final WSResultReplayDealSummary replayDealSummary = new WSResultReplayDealSummary();
        replayDealSummary.setResultPlayer(resultPlayer);
        replayDealSummary.setResultMostPlayed(resultMostPlayed);
        replayDealSummary.setResultReplay(resultReplay);
        replayDealSummary.getAttributes().add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, "" + (nbTotalPlayer + 1)));
        replayDealSummary.getAttributes().add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_RESULT_TYPE, "" + Constantes.TOURNAMENT_RESULT_PAIRE));
        return replayDealSummary;
    }

    /**
     * Get the next tournament available for the player.
     * @param playerID the player identifier.
     * @return the next tournament object.
     */
    private TourSerieTournament getNextTournamentToPlayForPlayer(final long playerID) {
        if (this.periodEasyChallenge != null) {
            final List<TourSerieTournament> serieEasyTournaments = this.listTournamentAvailable(playerID);
            for (final TourSerieTournament t : serieEasyTournaments) {
                final SerieEasyChallengeTournamentPlayer tp = getTournamentPlayer(t.getIDStr(), playerID);
                if (tp == null) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Return tournamentPlayer object for a player on a tournament
     * @param tournamentID the tournament identifier.
     * @param playerID the player identifier.
     * @return the tournament player object.
     */
    @Override
    public SerieEasyChallengeTournamentPlayer getTournamentPlayer(final String tournamentID, final long playerID){
        return this.mongoTemplate.findOne(Query.query(Criteria.where("tournament.$id").is(new ObjectId(tournamentID)).andOperator(Criteria.where("playerID").is(playerID))), SerieEasyChallengeTournamentPlayer.class);
    }

    /**
     * Return a tournament started by the player but not finished yet
     * @param playerID the player identifier.
     * @return return the tournament in progress for the player.
     */
    public TourSerieTournament getTournamentInProgressForPlayer(long playerID) {
        final SerieEasyChallengeTournamentPlayer tp = this.mongoTemplate.findOne(Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("finished").is(false))), SerieEasyChallengeTournamentPlayer.class);
        if (tp == null) {
            return null;
        } else {
            return tp.getTournament();
        }
    }

    /**
     * Return or create a tournamentPlayer object for a player on a tournament
     * @param playerID the player identifier.
     * @param tournament the tournament.
     * @return the tournament player object.
     */
    public SerieEasyChallengeTournamentPlayer getOrCreateTournamentPlayer(final long playerID, final TourSerieTournament tournament){
        if (this.periodEasyChallenge != null) {
            synchronized (this.lockCreateTournamentPlayer.getLock(playerID + "-" + tournament.getIDStr())) {
                SerieEasyChallengeTournamentPlayer tp = getTournamentPlayer(tournament.getIDStr(), playerID);
                if (tp == null) {
                    tp = new SerieEasyChallengeTournamentPlayer();
                    tp.setPlayerID(playerID);
                    tp.setTournament(tournament);
                    tp.setPeriodID(this.periodEasyChallenge.getPeriodID());
                    tp.setCreationDateISO(new Date());
                    tp.setStartDate(System.currentTimeMillis());
                    this.mongoTemplate.insert(tp);
                }
                return tp;
            }
        } else {
            this.log.error("Period EasyChallenge is null !");
        }
        return null;
    }

    /**
     * Get a not finished game on a tournament for a player
     * @param tournamentID the tournament identifier.
     * @param playerID the player identifier.
     * @return a game in progress in the tournament for the player.
     */
    private SerieEasyChallengeGame getNotFinishedGameForTournamentAndPlayer(String tournamentID, long playerID) throws FBWSException{
        try {
            final Query q = new Query();
            final Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
            final Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            final Criteria cFinished = Criteria.where("finished").is(false);
            q.addCriteria(new Criteria().andOperator(cTourID, cPlayerID, cFinished));
            return this.mongoTemplate.findOne(q, SerieEasyChallengeGame.class);
        } catch (Exception e) {
            this.log.error("Failed to find game for tournament="+tournamentID+" and playerID="+playerID, e);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * List the games of the player for a tournament.
     * Used by the support.
     * @param tournamentID the tournament identifier.
     * @param playerID the player identifier.
     * @return the list of games.
     */
    public List<SerieEasyChallengeGame> listGameForPlayerOnTournament(String tournamentID, long playerID) {
        final Query q = new Query();
        final Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
        final Criteria cPlayerID = Criteria.where("playerID").is(playerID);
        q.addCriteria(new Criteria().andOperator(cTourID, cPlayerID));
        q.with(new Sort(Sort.Direction.ASC, "dealIndex"));
        return this.mongoTemplate.find(q, SerieEasyChallengeGame.class);
    }

    /**
     * Return the game associated to the tournament, deal index and player
     * @param tournamentID the tournament identifier.
     * @param dealIndex the deal index.
     * @param playerID the player identifier.
     * @return the game corresponding to the parameters.
     */
    @Override
    public SerieEasyChallengeGame getGameOnTournamentAndDealForPlayer(String tournamentID, int dealIndex, long playerID){
        try {
            final Query q = new Query();
            final Criteria cTourID = Criteria.where("tournament.$id").is(new ObjectId(tournamentID));
            final Criteria cDealIndex = Criteria.where("dealIndex").is(dealIndex);
            final Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            q.addCriteria(new Criteria().andOperator(cTourID, cPlayerID, cDealIndex));
            return this.mongoTemplate.findOne(q, SerieEasyChallengeGame.class);
        } catch (Exception e) {
            this.log.error("Failed to find game for tournament deal and playerID", e);
        }
        return null;
    }


    /**
     * Save the game into the DB. If it already exists an update is done, otherwise an insertion.
     * @param game the game to save
     * @throws FBWSException if there is any issue during the process.
     */
    public void updateGameDB(final SerieEasyChallengeGame game) throws FBWSException {
        if (game != null && !game.isReplay()) {
            // Try to find the bug of "invalid game" ...
            if (this.getConfigIntValue("findBugGameNotValid", 0) == 1) {
                if (game.isFinished() && game.getContractType() == 0 && game.getBids().length() == 0) {
                    log.error("Game not valid !!! - game=" + game);
                    final Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                    if (this.getConfigIntValue("blockBugGameNotValid", 0) == 1) {
                        this.log.error("Game not save ! game="+game);
                        return;
                    }
                }
            }
            try {
                this.mongoTemplate.save(game);
            } catch (Exception e) {
                this.log.error("Exception to save game="+game, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Read string value for parameter in name (tournament."+CONFIG_NAME+".paramName) in config file
     * @param paramName the name of the parameter.
     * @param defaultValue its default value.
     * @return the string value for the parameter.
     */
    @Override
    public String getConfigStringValue(final String paramName, final String defaultValue) {
        return FBConfiguration.getInstance().getStringValue("tournament." + CONFIG_NAME + "." + paramName, defaultValue);
    }

    @Override
    public String getStringResolvEnvVariableValue(String paramName, String defaultValue) {
        return FBConfiguration.getInstance().getStringResolvEnvVariableValue("tournament." + CONFIG_NAME + "." + paramName, defaultValue);
    }

    /**
     * Read int value for parameter in name (tournament."+CONFIG_NAME+".paramName) in config file
     * @param paramName the name of the parameter.
     * @param defaultValue its default value.
     * @return the int value for the parameter.
     */
    @Override
    public int getConfigIntValue(String paramName, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("tournament." + CONFIG_NAME + "." + paramName, defaultValue);
    }

    /**
     * Update the tournament player in the DB.
     * @param tournamentPlayer the tournament player object.
     */
    private void updateTournamentPlayerInDB(final SerieEasyChallengeTournamentPlayer tournamentPlayer) {
        if (tournamentPlayer != null) {
            try {
                this.mongoTemplate.save(tournamentPlayer);
            } catch (Exception e) {
                this.log.error("Failed to save tournament player", e);
            }
        }
    }

    /**
     * Update the games into the DB
     * @param listGame the list of games to update.
     * @throws FBWSException if there is any issue during the execution.
     */
    @Override
    public <T extends Game> void updateListGameDB(final List<T> listGame) throws FBWSException {
        if (listGame != null && !listGame.isEmpty()) {
            try {
                final long ts = System.currentTimeMillis();
                for (final T g : listGame) {
                    // Try to find the bug of "invalid game" ...
                    if (this.getConfigIntValue("findBugGameNotValid", 0) == 1
                            && g.isFinished() && g.getContractType() == 0 && g.getBids().length() == 0) {
                        this.log.error("Game not valid !!! - game=" + g);
                        final Throwable t = new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                        this.log.error("Stacktrace BugGameNotValid=" + ExceptionUtils.getFullStackTrace(t));
                        if (getConfigIntValue("blockBugGameNotValid", 0) == 1) {
                            this.log.error("Game not save ! game="+g);
                            continue;
                        }
                    }
                    this.mongoTemplate.save(g);
                }
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Time to updateListGameDB : size=" + listGame.size( )+ " - ts=" + (System.currentTimeMillis() - ts));
                }
            } catch (Exception e) {
                this.log.error("Exception to save listGame", e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }
        }
    }

    /**
     * Count the number of tournament player finished on the period.
     * @param periodID the period identifier.
     * @param playerID the player identifier.
     * @return the number of tournament player finished on the period.
     */
    public int countTournamentPlayerFinishedOnPeriod(final String periodID, final long playerID) {
        final Query q = new Query();
        final Criteria cPeriodID = Criteria.where("periodID").is(periodID);
        final Criteria cPlayerID = Criteria.where("playerID").is(playerID);
        final Criteria cFinished = Criteria.where("finished").is(true);
        q.addCriteria(new Criteria().andOperator(cPeriodID, cPlayerID, cFinished));
        return (int) this.mongoTemplate.count(q, SerieEasyChallengeTournamentPlayer.class);
    }

    @Override
    public int countTournamentPlayerForTournament(final String tournamentID) {
        final Query q = new Query(Criteria.where("tournament.$id").is(new ObjectId(tournamentID)));
        return (int) this.mongoTemplate.count(q, SerieEasyChallengeTournamentPlayer.class);
    }

    /**
     * Count the tournament player in DB.
     * @param playerID the player identifier.
     * @return the number of tournament player object found for the id.
     */
    public int countTournamentPlayerForPlayer(final long playerID) {
        final Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (getConfigIntValue("resultArchiveNbDayBefore", 45)));
        final long dateRef = dateBefore.getTimeInMillis();
        final Query q = new Query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef)));
        return (int) this.mongoTemplate.count(q, SerieEasyChallengeTournamentPlayer.class);
    }

    @Override
    public List<SerieEasyChallengeTournamentPlayer> listTournamentPlayerForTournament(final String tournamentID, final int offset, final int nbMax) {
        final Query q = new Query(Criteria.where("tournament.$id").is(new ObjectId(tournamentID)));
        q.with(new Sort(Sort.Direction.DESC, "startDate"));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return this.mongoTemplate.find(q, SerieEasyChallengeTournamentPlayer.class);
    }

    public List<SerieEasyChallengeTournamentPlayer> listTournamentPlayerForPlayer(final long playerID, final int offset, final int nbMax) {
        final Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (this.getConfigIntValue("resultArchiveNbDayBefore", 45)));
        final long dateRef = dateBefore.getTimeInMillis();
        final Query q = new Query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef)));
        q.with(new Sort(Sort.Direction.DESC, "startDate"));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return this.mongoTemplate.find(q, SerieEasyChallengeTournamentPlayer.class);
    }

    public ResultListTournamentArchive listTournamentArchive(final long playerID, final int offset, final int nbMax) {
        final ResultListTournamentArchive result = new ResultListTournamentArchive();
        result.archives = new ArrayList<>();
        final Calendar dateBefore = Calendar.getInstance();
        dateBefore.add(Calendar.DAY_OF_YEAR, - (this.getConfigIntValue("resultArchiveNbDayBefore", 45)));
        final long dateRef = dateBefore.getTimeInMillis();
        result.nbTotal = (int) this.mongoTemplate.count(Query.query(Criteria.where("playerID").is(playerID).andOperator(Criteria.where("lastDate").gt(dateRef))), SerieEasyChallengeTournamentPlayer.class);
        final List<SerieEasyChallengeTournamentPlayer> listTourPlayer = this.listTournamentPlayerForPlayer(playerID, offset, nbMax);
        for (final SerieEasyChallengeTournamentPlayer e : listTourPlayer) {
            final WSTournamentArchive wst = new WSTournamentArchive();
            wst.date = e.getStartDate();
            wst.nbPlayers = e.getTournament().getNbPlayers()+1;
            wst.rank = e.getRank();
            wst.result = e.getResult();
            wst.resultType = e.getTournament().getResultType();
            wst.tournamentID = e.getTournament().getIDStr();
            wst.name = TourSerieMgr.SERIE_11;
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
     * Return a list of result deal in the tournament for the player
     * @param tour the tournament object.
     * @param playerID the player identifier.
     * @return the list of deals for the WS.
     */
    public List<WSResultDeal> resultListDealForTournamentForPlayer(final TourSerieTournament tour, final long playerID) throws FBWSException {
        if (tour == null) {
            this.log.error("Parameter not valid");
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        final List<WSResultDeal> listResultDeal = new ArrayList<>();
        for (final TourSerieDeal deal : tour.listDeal) {
            final String dealID = deal.getDealID(tour.getIDStr());
            final WSResultDeal resultPlayer = new WSResultDeal();
            resultPlayer.setDealIDstr(dealID);
            resultPlayer.setDealIndex(deal.index);
            resultPlayer.setResultType(Constantes.TOURNAMENT_RESULT_PAIRE);
            resultPlayer.setNbTotalPlayer(tour.getNbPlayers() + 1);
            final SerieEasyChallengeGame game = this.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerID);
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
     * Count the number of contracts in the tournament for a deal
     * @param tourID the tournament identifier.
     * @param dealIndex the deal index.
     * @param useLead if true, use the lead as a criteria to differentiate the contracts.
     * @return the number of different contracts.
     */
    public int countContractGroup(final String tourID, final int dealIndex, final boolean useLead, final int playerScore, final String playerContract, final int playerContractType) {
        int counter = 1;
        final List<GameGroupMapping> results = this.getResults(tourID, dealIndex, useLead);
        if (!results.isEmpty()) {
            counter = results.size();
            for (final GameGroupMapping e : results) {
                if ( (e.isLeaved() && playerScore == Constantes.GAME_SCORE_LEAVE)
                        || (e.score == playerScore && e.contract != null && e.contract.equals(playerContract) && e.contractType == playerContractType)) {
                    counter += 1;
                    break;
                }
            }
        }
        return counter;
    }

    /**
     * Count the number of games of a player
     * @param playerID the player identifier.
     * @return the number of games of the player.
     */
    @Override
    public int countGamesForPlayer(final long playerID) {
        try {
            final Query q = new Query();
            final Criteria cPlayerID = Criteria.where("playerID").is(playerID);
            final Criteria cFinished = Criteria.where("finished").is(true);
            q.addCriteria(new Criteria().andOperator(cPlayerID, cFinished));
            return (int) this.mongoTemplate.count(q, SerieEasyChallengeGame.class);
        } catch (Exception e) {
            this.log.error("Failed to count games for player - playerID="+ playerID, e);
        }
        return 0;
    }

    /**
     * Return result deals for a player.
     * The deal must have been played by this player.
     * Same contract are grouped
     * @param dealID the deal identifier.
     * @param playerCache the player cache.
     * @param useLead if true, use the lead as a criteria to differentiate the contracts.
     * @return the result deals for a player.
     * @throws FBWSException if there is any issue during the execution.
     */
    public WSResultDealTournament resultDealGroup(final String dealID, final PlayerCache playerCache, final boolean useLead) throws FBWSException {
        final TourSerieTournament tour = this.serieMgr.getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        final TourSerieDeal deal = (TourSerieDeal) tour.getDeal(dealID);
        if (deal == null) {
            this.log.error("No deal found with this ID="+dealID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        final SerieEasyChallengeTournamentPlayer tournamentPlayer = this.getTournamentPlayer(tour.getIDStr(), playerCache.ID);
        if (tournamentPlayer == null) {
            this.log.error("No tournament player found for playerID="+playerCache.ID+" - tour="+tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // Check if the deal has been played by this player
        final SerieEasyChallengeGame gamePlayer = this.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
        if (gamePlayer == null) {
            this.log.error("Game not found for tour="+tour+" - deal index="+deal.index+" - playerID="+playerCache.ID);
            throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
        }
        final WSResultDealTournament resultDealTour = new WSResultDealTournament();
        // List all the games from the DB ordered by score
        final List<TourSerieGame> listGame = this.serieMgr.listGameFinishedOnTournamentAndDeal(tour.getIDStr(), deal.index, 0, 0);
        // Map with nb game with same score and contract
        final Map<String, Integer> mapRankNbPlayer = new HashMap<>(); /* Map to store the number of player for each score and contract */
        final Map<Integer, Integer> mapScoreNbPlayer = new HashMap<>(); /* Map to store nb player for each score */
        for (final TourSerieGame g : listGame) {
            int temp = 0;
            String key = g.getScore() + "-" + g.getContractWS();
            if (useLead && g.getBegins() != null) {
                key += "-" + g.getBegins();
            }
            if (mapRankNbPlayer.get(key) != null){
                temp = mapRankNbPlayer.get(key);
            }
            mapRankNbPlayer.put(key, temp + 1);
            if (mapScoreNbPlayer.get(g.getScore()) != null) {
                mapScoreNbPlayer.put(g.getScore(), mapScoreNbPlayer.get(g.getScore()) + 1);
            } else {
                mapScoreNbPlayer.put(g.getScore(), 1);
            }
        }
        // Add the game score of the player to the map
        int tempNbPlayer = 0;
        String keyGamePlayer = gamePlayer.getScore() + "-" + gamePlayer.getContractWS();
        if (useLead && gamePlayer.getBegins() != null) {
            keyGamePlayer += "-" + gamePlayer.getBegins();
        }
        if (mapRankNbPlayer.get(keyGamePlayer) != null) {
            tempNbPlayer = mapRankNbPlayer.get(keyGamePlayer);
        }
        mapRankNbPlayer.put(keyGamePlayer, tempNbPlayer + 1);
        if (mapScoreNbPlayer.get(gamePlayer.getScore()) != null) {
            mapScoreNbPlayer.put(gamePlayer.getScore(), mapScoreNbPlayer.get(gamePlayer.getScore())+1);
        } else {
            mapScoreNbPlayer.put(gamePlayer.getScore(), 1);
        }
        final List<WSResultDeal> listResult = new ArrayList<>();
        final List<String> listScoreContract = new ArrayList<>();
        int idxPlayer = -1;
        if(listGame.isEmpty()) {
            idxPlayer = 0;
        }
        boolean playerDone = gamePlayer.getScore() == Constantes.CONTRACT_LEAVE;
        for (final TourSerieGame g : listGame) {
            if (g.getScore() == Constantes.CONTRACT_LEAVE) {
                continue;
            }
            // need to add game player now ?
            if (!playerDone && gamePlayer.getScore() >= g.getScore()) {
                playerDone = true;
                String key = gamePlayer.getScore() + "-" + gamePlayer.getContractWS();
                if (useLead && gamePlayer.getBegins() != null) {
                    key += "-" + gamePlayer.getBegins();
                }
                if (!listScoreContract.contains(key)) {
                    listScoreContract.add(key);
                    final WSResultDeal resDeal = new WSResultDeal();
                    resDeal.setContract(gamePlayer.getContractWS());
                    resDeal.setDealIDstr(dealID);
                    resDeal.setDealIndex(deal.index);
                    resDeal.setDeclarer("" + gamePlayer.getDeclarer());
                    resDeal.setRank(gamePlayer.getRank());
                    resDeal.setNbTotalPlayer(tour.getNbPlayers() + 1);
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
            String key = g.getScore() + "-" + g.getContractWS();
            if (useLead && g.getBegins() != null) {
                key += "-" + g.getBegins();
            }
            if (!listScoreContract.contains(key)) {
                listScoreContract.add(key);
                final WSResultDeal resDeal = new WSResultDeal();
                resDeal.setContract(g.getContractWS());
                resDeal.setDealIDstr(dealID);
                resDeal.setDealIndex(deal.index);
                resDeal.setDeclarer("" + g.getDeclarer());
                resDeal.setRank(g.getRank());
                // Change the rank if necessary
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
            final WSResultDeal wsResultDeal = buildWSResultDeal(gamePlayer, playerCache, true);
            wsResultDeal.setNbPlayerSameGame(1);
            listResult.add(wsResultDeal);
        }
        resultDealTour.listResultDeal = listResult;
        resultDealTour.totalSize = listResult.size();
        final WSTournament wstour = serieTournamentToWS(tour, playerCache);
        resultDealTour.tournament = wstour;
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_PLAYER_CONTRACT, gamePlayer.getContractWS()));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_INDEX_PLAYER, "" + idxPlayer));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, "" + (tour.getNbPlayers() + 1)));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_DEAL_INDEX, "" + deal.index));
        return resultDealTour;
    }

    /**
     * Return the result deals for a player.
     * The deal must have been played by this player.
     * @param dealID the deal identifier.
     * @param playerCache the player cache.
     * @param listPlaFilter the list of players.
     * @param offset offset for the query
     * @param nbMax max number of elements retrieved in the query
     * @return the result deals for a player.
     * @throws FBWSException if there is any issue during the execution.
     */
    public WSResultDealTournament resultDealNotGroup(final String dealID, final PlayerCache playerCache, final List<Long> listPlaFilter, int offset, final int nbMax) throws FBWSException {
        final TourSerieTournament tour = this.serieMgr.getTournamentWithDeal(dealID);
        if (tour == null) {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        final TourSerieDeal deal = (TourSerieDeal) tour.getDeal(dealID);
        if (deal == null) {
            this.log.error("No deal found with this ID=" + dealID + " - tour=" + tour);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
        // Check the deals played by this player
        final SerieEasyChallengeGame gamePlayer = this.getGameOnTournamentAndDealForPlayer(tour.getIDStr(), deal.index, playerCache.ID);
        if (gamePlayer == null) {
            this.log.error("Game not found for tour=" + tour + " - deal index=" + deal.index + " - playerID=" + playerCache.ID);
            throw new FBWSException(FBExceptionType.RESULT_DEAL_NOT_PLAYED);
        }
        final WSResultDealTournament resultDealTour = new WSResultDealTournament();
        final List<WSResultDeal> listResult = new ArrayList<>();
        // List all the games from DB ordered by score
        final List<TourSerieGame> listGame = this.serieMgr.listGameFinishedOnTournamentAndDeal(tour.getIDStr(), deal.index, 0, 0);
        final Map<Integer, Integer> mapScoreNbPlayer = new HashMap<>(); /* Map to store nb player for each score */
        for (final TourSerieGame g : listGame) {
            if (mapScoreNbPlayer.get(g.getScore()) != null) {
                mapScoreNbPlayer.put(g.getScore(), mapScoreNbPlayer.get(g.getScore())+1);
            } else {
                mapScoreNbPlayer.put(g.getScore(), 1);
            }
        }
        // Add the game score of the player to the map
        if (mapScoreNbPlayer.get(gamePlayer.getScore()) != null) {
            mapScoreNbPlayer.put(gamePlayer.getScore(), mapScoreNbPlayer.get(gamePlayer.getScore())+1);
        } else {
            mapScoreNbPlayer.put(gamePlayer.getScore(), 1);
        }
        int idxPlayer = -1;
        for (final TourSerieGame g : listGame) {
            if (listPlaFilter != null && !listPlaFilter.contains(g.getPlayerID())) {
                continue;
            }
            if (idxPlayer == -1 && gamePlayer.getScore() >= g.getScore()) {
                listResult.add(buildWSResultDeal(gamePlayer, playerCache, true));
                idxPlayer = listResult.size() - 1;
            }
            final WSResultDeal resDeal = this.buildWSResultDeal(g, this.playerCacheMgr.getPlayerCache(g.getPlayerID()), this.presenceMgr.isSessionForPlayerID(g.getPlayerID()));
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
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_INDEX_PLAYER, "" + idxPlayer));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_DEAL_INDEX, "" + deal.index));
        resultDealTour.attributes.add(new WSResultAttribut(Constantes.RESULT_ATTRIBUT_TOTAL_PLAYER, "" + resultDealTour.totalSize));
        return resultDealTour;
    }

    private WSResultDeal buildWSResultDeal(final Game g, final PlayerCache playerCache, final boolean connected) {
        final WSResultDeal resDeal = new WSResultDeal();
        resDeal.setContract(g.getContractWS());
        resDeal.setDealIDstr(g.getDealID());
        resDeal.setDealIndex(g.getDealIndex());
        resDeal.setDeclarer("" + g.getDeclarer());
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
     * @param periodID the period identifier.
     * @return the period corresponding to the periodID.
     */
    private TourSeriePeriod getNextPeriod(final String periodID) {
        if (periodID != null && this.periodEasyChallenge != null) {
            if (periodID.equals(this.periodEasyChallenge.getPeriodID())) {
                return this.periodSerie;
            }
            final Query q = Query.query(Criteria.where("periodID").gt(periodID));
            q.with(new Sort(Sort.Direction.ASC, "periodID"));
            return this.mongoTemplate.findOne(q, TourSeriePeriod.class);
        }
        return null;
    }

    /**
     * Build a WSTournament associated to the serie tournament and change some value : category, nbTotalPlayer, beginDate, endDate & remainingTime
     * @param tour the Serie tournament.
     * @param playerCache the player cache.
     * @return a tournament for the WS.
     */
    public WSTournament serieTournamentToWS(final TourSerieTournament tour, final PlayerCache playerCache) {
        final WSTournament wsTour = tour.toWS();
        wsTour.categoryID = Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE;
        wsTour.nbTotalPlayer = tour.getNbPlayers() + 1;
        final SerieEasyChallengeTournamentPlayer tournamentPlayer = getTournamentPlayer(tour.getIDStr(), playerCache.ID);
        if (tournamentPlayer != null) {
            wsTour.resultPlayer = tournamentPlayer.toWSResultTournamentPlayer(playerCache, tour, playerCache.ID);
            if (wsTour.resultPlayer != null) {
                wsTour.playerOffset = wsTour.resultPlayer.getRank();
            }
            wsTour.currentDealIndex = tournamentPlayer.getCurrentDealIndex();
        }
        final TourSeriePeriod tempPeriod = this.getNextPeriod(tour.getPeriod());
        if (tempPeriod != null) {
            wsTour.beginDate = tempPeriod.getTsDateStart();
            wsTour.endDate = tempPeriod.getTsDateEnd();
            if (tempPeriod.getTsDateEnd() > System.currentTimeMillis()) {
                wsTour.remainingTime = tempPeriod.getTsDateEnd() - System.currentTimeMillis();
            }
        }
        return wsTour;
    }

    public List<WSResultTournamentPlayer> resultListTournament(final TourSerieTournament tour, final int offset, final int nbMaxResult, final List<Long> listFollower, final long playerAsk, final WSResultTournamentPlayer resultPlayerAsk) {
        final List<WSResultTournamentPlayer> listResult = new ArrayList<>();
        // Get the data from the Serie DB
        final Query q= new Query(Criteria.where("tournament.$id").is(new ObjectId(tour.getIDStr())));
        q.with(new Sort(Sort.Direction.ASC, "rank"));
        final List<TourSerieTournamentPlayer> listSerieTP = this.mongoTemplate.find(q, TourSerieTournamentPlayer.class);
        boolean playerDone = false;
        if (!listSerieTP.isEmpty()) {
            for (final TourSerieTournamentPlayer tp : listSerieTP) {
                if (!playerDone && resultPlayerAsk != null && tp.getResult() <= resultPlayerAsk.getResult()) {
                    listResult.add(resultPlayerAsk);
                    playerDone = true;
                }
                boolean addPlayer = true;
                if (listFollower != null && !listFollower.contains(tp.getPlayerID())) {
                    addPlayer = false;
                }
                if (addPlayer) {
                    final WSResultTournamentPlayer rp = tp.toWSResultTournamentPlayer(this.playerCacheMgr.getPlayerCache(tp.getPlayerID()), playerAsk);
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
     * Count the number of available tournament (serie S11, period and nbPlayers > limit)
     * @return the number of available tournament.
     */
    public int countTournamentAvailable(final long playerID) {
        return this.listTournamentAvailable(playerID).size();
    }

    /**
     * List tournament available (serie S11, period and nbPlayers > limit)
     * @return
     */
    private List<TourSerieTournament> listTournamentAvailable(final long playerID) {
        if (this.periodEasyChallenge != null) {
            final String periodID = this.periodEasyChallenge.getPeriodID();
            Query q = new Query();
            final Criteria cSerie = Criteria.where("serie").is(TourSerieMgr.SERIE_11);
            Criteria cPeriod = Criteria.where("period").is(periodID);
            final Criteria cNotEmpty = Criteria.where("nbPlayer").gte(getConfigIntValue("nbPlayerLimit", 5));
            q.addCriteria(new Criteria().andOperator(cSerie, cPeriod, cNotEmpty));
            final List<TourSerieTournament> allTournaments =  this.mongoTemplate.find(q, TourSerieTournament.class);
            q = new Query();
            final Criteria cPlayer = Criteria.where("playerID").is(playerID);
            cPeriod = Criteria.where("periodID").is(periodID);
            q.addCriteria(new Criteria().andOperator(cPlayer, cPeriod));
            final List<TourSerieTournament> playerTournaments =  this.mongoTemplate.find(q, TourSerieTournamentPlayer.class).
                    stream().
                    map(TourSerieTournamentPlayer::getTournament).
                    collect(Collectors.toList());
            allTournaments.removeAll(playerTournaments);
            return allTournaments;
        }
        return new ArrayList<>();
    }

    /**
     * Count the tournament player for current period
     * @param finished only with status finished true
     * @return the number of tournament players.
     */
    public int countTournamentPlayerForCurrentPeriod(final boolean finished) {
        if (this.periodEasyChallenge != null) {
            final String periodID = this.periodEasyChallenge.getPeriodID();
            if (!finished) {
                final Query q = Query.query(Criteria.where("periodID").is(periodID));
                return (int) this.mongoTemplate.count(q, SerieEasyChallengeTournamentPlayer.class);
            }
            final Query q = Query.query(Criteria.where("periodID").is(periodID).andOperator(Criteria.where("finished").is(true)));
            return (int) this.mongoTemplate.count(q, SerieEasyChallengeTournamentPlayer.class);
        }
        return -1;
    }

    @Override
    public void updateGamePlayArgineFinished(final Game game) {
        // Not used in this mode.
    }

    @Override
    public void updateGamePlayArgine(Game game) {
        // Not used in this mode.
    }
}
