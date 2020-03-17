package com.funbridge.server.tournament.category;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.PlayerMgr.PlayerUpdateType;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.FilterEvent;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.tournament.TournamentChallengeMgr;
import com.funbridge.server.tournament.TournamentGame2Mgr;
import com.funbridge.server.tournament.dao.TournamentGame2DAO;
import com.funbridge.server.tournament.dao.TournamentTable2DAO;
import com.funbridge.server.tournament.data.*;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.event.Event;
import com.funbridge.server.ws.game.WSGameDeal;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.game.WSGameView;
import com.funbridge.server.ws.game.WSTableGame;
import com.gotogames.common.bridge.BridgeConstantes;
import com.gotogames.common.bridge.GameBridgeRule;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

@Component(value="tournamentTrainingPartnerMgr")
@Scope(value="singleton")
public class TournamentTrainingPartnerMgr extends TournamentType {
	@Resource(name="tourTable2DAO")
	private TournamentTable2DAO tourTableDAO = null;
	@Resource(name="presenceMgr")
	private PresenceMgr presenceMgr = null;
	@Resource(name="playerMgr")
	private PlayerMgr playerMgr = null;
	@Resource(name="tournamentGame2Mgr")
	private TournamentGame2Mgr tournamentGame2Mgr = null;
	@Resource(name="tournamentGame2DAO")
	private TournamentGame2DAO tournamentGame2DAO = null;
	@Resource(name="tournamentChallengeMgr")
	private TournamentChallengeMgr tournamentChallengeMgr = null;

	private ScheduledExecutorService schedulerPurgeMap = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> schedulerPurgeMapFuture = null;
	private PurgeMapTableTask purgeMapTask = null;
	private ConcurrentHashMap<Long, TournamentTable2> mapTable = new ConcurrentHashMap<Long, TournamentTable2>();
	private Object objFinishSynchro = new Object();
	private long dateLastPurge = 0;
	
	/**
	 * Task to purge challenge
	 * @author pascal
	 *
	 */
	public class PurgeMapTableTask implements Runnable {
		@Override
		public void run() {
			purgeMapTable();
		}
	}
	
	public TournamentTrainingPartnerMgr() {
		super(Constantes.TOURNAMENT_CATEGORY_NAME_TRAINING_PARTNER);
		finishTaskEnable = true; // enable finisher task
	}

	@Override
	public void onEventFinishTask() {
		if (isFinishEnable()) {
			// For training, be careful, tournament can be closed by aonther method when nb max player is reached
			synchronized (objFinishSynchro) {
				List<Tournament> listTour = tournamentMgr.getTournamentListForCategoryNotFinishedAndDateExpired(Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER);
				int nbMax = getConfigIntValue("finishNbMax", 5);
				long ts = System.currentTimeMillis();
				int nbTourFinished = 0;
				for (Tournament tour : listTour) {
					if (nbTourFinished >= nbMax) {
						break;
					}
					try {
						if (super.finishTournament(tour.getID())) {
							nbTourFinished++;
						}
					} catch (Exception e) {
						log.error("Exception to finish tournament TOURDAY tourID="+tour.getID(), e);
					}
				}
				if (System.currentTimeMillis() - ts > 2000) {
					log.error("Nb tour to finish="+listTour.size()+" nbTourFinished="+nbTourFinished+" - ts="+(System.currentTimeMillis() - ts));
				}
			}
		}
	}

	@Override
	public void onEventCleanTask() {
		if (isCleanEnable()) {
			int nbMaxTour = getConfigIntValue("cleanNbMaxTour", 100);
			int nbDaysBefore = getConfigIntValue("cleanNbDayBefore", 10);
			cleanTournament(nbMaxTour, nbDaysBefore);
		}
	}

	@Override
	public void startUp() {
		createTimer();
		loadAllTable();
		purgeMapTask = new PurgeMapTableTask();
		schedulerPurgeMapFuture = schedulerPurgeMap.scheduleWithFixedDelay(purgeMapTask, 10, 10, TimeUnit.MINUTES);
		log.info("Schedule purge map - next run at "+Constantes.getStringDateForNextDelayScheduler(schedulerPurgeMapFuture)+" - period (minutes)="+10);
	}

	@PostConstruct
	@Override
	public void init() {
		super.init(Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER);
//		nbMaxPlayerTournament = getConfigIntValue("nbMaxPlayer", 100);
		nbCreditPlayDeal = getConfigIntValue("nbCreditPlayDeal", 1);
		if (nbCreditPlayDeal == 0) {
			log.error("Param nbCreditPlayDeal not found, use default value");
			nbCreditPlayDeal = 1;
		}
	}
	
	@PreDestroy
	@Override
	public void destroy() {
		stopScheduler(schedulerPurgeMap);
		mapTable.clear();
		super.destroy();
	}
	
	@Override
	public boolean finishTournament(long tourID) {
		synchronized (objFinishSynchro) {
			return super.finishTournament(tourID);
		}
	}

	/**
	 * Load all table with tournament not finished
	 */
	private void loadAllTable() {
		List<TournamentTable2> listTable = tourTableDAO.listForTournamentNotFinished();
		if (listTable != null) {
			for (TournamentTable2 table : listTable) {
				if (table.getCurrentGame() == null || !table.getCurrentGame().isFinished()) {
					mapTable.put(table.getID(), table);
				}
			}
		}
		log.debug("After loading all table map size="+mapTable.size());
	}
	
	public Tournament getTournamentToPlay(FBSession session, Player partner) {
		
		return null;
	}
	
	/**
	 * Return the table and check current game (get it from map)
	 * @param tableID
	 * @param gameID
	 * @return
	 */
	public TournamentTable2 getForTableAndGame(long tableID, long gameID) {
		TournamentTable2 table = mapTable.get(tableID);
		if (table != null) {
			if (table.getCurrentGame() != null) {
				if (table.getCurrentGame().getID() == gameID) {
					return table;
				} else {
					log.error("Table found for tableID="+tableID+" current gameID !="+gameID+" - table="+table);
				}
			} else {
				log.error("Table found for tableID="+tableID+" current game is null ! table="+table);
			}
		}
		return null;
	}

	/**
	 * View game
	 * @param gameID
	 * @param playerID
	 * @return
	 * @throws FBWSException
	 */
	public WSGameView viewGame(long gameID, long playerID) throws FBWSException {
		TournamentGame2 game = tournamentGame2DAO.getForID(gameID);
		if (game != null) {
			TournamentTable2 table = game.getTable(); 
			if (!table.isPlayerTable(playerID)) {
				log.error("Player is not on table ! table="+table+" - player="+playerID);
				throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
			}
			if (!game.isFinished()) {
				log.error("Game not finished game="+game);
				throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
			}
			WSGameView gv = new WSGameView();
			gv.table = table2WS(table, playerID);
			gv.game = new WSGameDeal();
			gv.game.setDealData(game.getDeal());
			gv.game.setGameData(game);
            gv.tournament = game.getDeal().getTournament().toWS();
			return gv;
		}
		return null;
	}

	/**
	 * Generate new tournament
	 * @return
	 */
	public Tournament generateTournament(TournamentSettings settings) {
		Tournament newTour = null;
		if (isGeneratorEnable()) {
			// check nb deal
			int nbDeal = getConfigIntValue("nbDeal", 1);
			if (nbDeal <= 0) {log.error("Nb Deal is not valid : "+nbDeal);return null;}
			// check durationEnd
			int durationEnd = getConfigIntValue("durationEnd", 0);
			if (durationEnd <= 0) {log.error("durationEnd is not valid : "+durationEnd);return null;}
			// check durationLast
			int durationLast = getConfigIntValue("durationLast", 0);
			if (durationLast <= 0) {log.error("durationLast is not valid : "+durationLast);return null;}
			// engine version
			int engineVersion = getConfigIntValue("engineVersion", 0);
			if (engineVersion == 0) log.warn("No engine version for this tournament - name="+name+" - use default version");
			
			Calendar calBegin = Calendar.getInstance();
			Calendar calEnd = Calendar.getInstance();
			Calendar calLastStart = Calendar.getInstance();
			calBegin.set(Calendar.MILLISECOND, 0);
			calBegin.set(Calendar.SECOND, 0);
			calBegin.set(Calendar.MINUTE, 0);
			calEnd.setTimeInMillis(calBegin.getTimeInMillis());
			calEnd.add(Calendar.MINUTE, durationEnd);
			calLastStart.setTimeInMillis(calBegin.getTimeInMillis());
			calLastStart.add(Calendar.MINUTE, durationLast);
			try {
				List<Tournament> listNewTour = new ArrayList<Tournament>();
				newTour = ContextManager.getTournamentGenerator().createTournamentForCategory(
						category,
						name,
						Constantes.SERIE_NOT_DEFINED /* no serie for training */,
						Constantes.TOURNAMENT_RESULT_PAIRE /* always IMP */,
						nbDeal,
						-1 /* offset index => random value*/,
						calBegin.getTimeInMillis(), calLastStart.getTimeInMillis(), calEnd.getTimeInMillis(),
						engineVersion,
						getConfigIntValue("nbMaxPlayer", 1), nbCreditPlayDeal, settings);
				if (newTour != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Generate new tournament with sucess. Tour=" + newTour.toString());
                    }
					listNewTour.add(newTour);
				} else {
					log.error("Generate new tournament is null !");
				}
			} catch (Exception e) {
				log.error("Exception to generate new tournament", e);
			}
		}
		return newTour;
	}
	
	/**
	 * Transform table2 to WSTableGame
	 * @param table
	 * @return
	 */
	public WSTableGame table2WS(TournamentTable2 table, long playerAsk) {
		if (table != null && table.isValid()) {
			WSTableGame wst = new WSTableGame();
			wst.tableID = table.getID();
			wst.playerEast = WSGamePlayer.createGamePlayerRobot();
			wst.playerWest = WSGamePlayer.createGamePlayerRobot();
			wst.playerNorth = WSGamePlayer.createGamePlayerHuman(table.getPlayerNorth(), table.getPlayerStatus(table.getPlayerNorth().getID()), playerAsk);
			wst.playerSouth = WSGamePlayer.createGamePlayerHuman(table.getPlayerSouth(), table.getPlayerStatus(table.getPlayerSouth().getID()), playerAsk);
			wst.leaderID = table.getCreatorID();
			return wst;
		} else {
			log.error("Table null or not valid ! table="+table);
		}
		return null;
	}
	
	/**
	 * A player join the challenge to play tournament
	 * @param tc
	 * @param p
	 * @return
	 * @throws FBWSException
	 */
	public TournamentTable2 playChallenge(TournamentChallenge tc, Player p) throws FBWSException {
		if (tc == null || p == null) {
			log.error("Parameters null : tc="+tc+" - player="+p);
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		// challenge expired
		if (tc.isEnded()) {
			log.error("Challenge has expired ! tc="+tc);
			throw new FBWSException(FBExceptionType.TOURNAMENT_CHALLENGE_EXPIRATION);
		}
		// check player is valid for this challenge
		if (tc.getPartner().getID() != p.getID() && tc.getCreator().getID() != p.getID()) {
			log.error("Player is not for this challenge ! challenge="+tc+" - player="+p.getID());
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}
		// check challenge status
		if (tc.getStatus() != Constantes.TOURNAMENT_CHALLENGE_STATUS_WAITING &&
			tc.getStatus() != Constantes.TOURNAMENT_CHALLENGE_STATUS_PLAY) {
			log.error("Challenge status not valid ! challenge="+tc+" - player="+p);
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		TournamentTable2 table = tc.getTable();
		if (table == null) {
			// create tournament
			Tournament tour = generateTournament(tournamentMgr.getTournamentSettings(tc.getSettings()));
			if (tour == null) {
				log.error("Error to create tournament for challenge="+tc);
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
			// create table
			table = ContextManager.getTournamentTrainingPartnerMgr().createTable(tour, tc.getCreator(), tc.getPartner());
			if (table == null) {
				log.error("Error to create table for challenge="+tc+" - tournament="+tour);
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
			tc.setTable(table);
			tc.setDateExpiration(tour.getEndDate());
		}
		else {
			tc.setStatus(Constantes.TOURNAMENT_CHALLENGE_STATUS_PLAY);
			// update table data
			table = getAndCheckTable2(table.getID());
			tc.setTable(table);
		}
		checkTournament(table);
		table.setChallengeID(tc.getID());
		// remove flag play for player (wait playDeal)
		table.setPlayerPlay(p.getID(), false);
		ContextManager.getTournamentChallengeMgr().updateChallenge(tc);
		
		return tc.getTable();
	}
	
	public TournamentTable2 getAndCheckTable2(long tableID) throws FBWSException {
		TournamentTable2 table = mapTable.get(tableID);
		if (table == null) {
			log.error("No table with id="+tableID);
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}
		return table;
	}
	
	public void purgeMapTable() {
		long currentTime = System.currentTimeMillis();
		dateLastPurge = currentTime;
		synchronized (mapTable) {
			// get list of game not finished and tournament closed => remove table from map
			List<TournamentGame2> listToRemove = tournamentGame2DAO.listNotFinishedAndTournamentClosed();
			if (listToRemove != null) {
				for (TournamentGame2 g : listToRemove) {
					if (log.isDebugEnabled()) {
						log.debug("Tournament on table is finished ! => remove table="+g.getTable());
					}
					mapTable.remove(g.getTable().getID());
				}
			}
			
			// loop on map to remove table with flag tournament finish
			for (Iterator<Entry<Long, TournamentTable2>> it = mapTable.entrySet().iterator(); it.hasNext();) {
				Entry<Long, TournamentTable2> e = it.next();
				// check tournament is finished and since at least 30 min.
				if ((e.getValue().getDateFinishTournement() > 0) &&
					(e.getValue().getDateFinishTournement() < currentTime) &&
					(currentTime - e.getValue().getDateFinishTournement() > (30*60*1000))) {
					if (log.isDebugEnabled()) {
						log.debug("Tournament on table is finished ! => remove table="+e.getValue());
					}
					it.remove();
				}
			}
		}
		
	}
	
	/**
	 * Check if tournament of table is not finished and current date valid
	 * @param t
	 * @throws FBWSException
	 */
	public void checkTournament(TournamentTable2 t) throws FBWSException {
		if (t != null) {
			if (t.getTournament().isFinished()) {
				throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
			}
			if (!t.getTournament().isDateValid(System.currentTimeMillis())) {
				throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
			}
		} else {
			log.error("table is null !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Return the existing game for table or create a new one
	 * @param table
	 * @param session
	 * @return
	 * @throws FBWSException
	 */
	public TournamentGame2 getOrCreateGame(TournamentTable2 table, FBSession session) throws FBWSException {
		if (table == null || session == null) {
			log.error("Table or session is null ... table="+table+" - session="+session);
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		Player p = session.getPlayer();
		Player partner = table.getPartner(p.getID());
		if (partner == null) {
			log.error("No partner found on table="+table+" - player="+p.getID());
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		TournamentGame2 game = null;
		synchronized (tournamentGame2Mgr.getLockDataForTable(table.getID())) {
			game = table.getCurrentGame();
			// game exist and not finished
			if (game != null) {
				if (game.isFinished()) {
					log.error("Deal is finished ! tableID="+table);
					throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
				}
			}
			// need to create new game
			else {
				// check player credit
				playerMgr.checkPlayerCredit(p, table.getTournament().getNbCreditPlayDeal());
				try {
					playerMgr.checkPlayerCredit(partner, table.getTournament().getNbCreditPlayDeal());
				} catch (FBWSException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Partner has not enougth credit ! partner=" + partner);
                    }
					throw new FBWSException(FBExceptionType.GAME_PARTNER_CREDIT_NOT_ENOUGH);
				}
				TournamentDeal deal = tournamentMgr.getDealForTournamentAndIndex(table.getTournament(), 1);
				if (deal == null) {
					log.error("No deal found at index 1 for tour="+table.getTournament());
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				TournamentSettings tourSettings = tournamentMgr.getTournamentSettings(table.getTournament().getSettings());
				if (tourSettings == null) {
					log.error("Failed to build settings="+table.getTournament().getSettings());
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				// create new game
				game = new TournamentGame2();
				game.setTable(table);
				game.setDeal(deal);
				game.setStartDate(System.currentTimeMillis());
				game.setLastDate(System.currentTimeMillis());
				if (tourSettings.conventionValue == null) {
					game.setConventionSelection(Constantes.GAME_CONVENTION_ENGINE_COSTEL, tourSettings.convention, "");
				} else {
					game.setConventionSelection(Constantes.GAME_CONVENTION_ENGINE_ARGINE, tourSettings.convention, tourSettings.conventionValue);
				}
				game = ContextManager.getTournamentGame2Mgr().persistGame(game);
				
				if (game == null) {
					log.error("Game is null : creation failed - tableID="+table.getID()+" - dealID="+deal.getID()+" - player="+p.getID());
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				table.setCurrentGame(game);
				// WARNING do not affect table to result of updateTable !!!
				ContextManager.getTournamentTrainingPartnerMgr().updateTable(table);

				FBSession sessionPartner = presenceMgr.getSessionForPlayerID(partner.getID());

				// update credit amount of players
				p.decrementCreditAmount(deal.getTournament().getNbCreditPlayDeal());
				partner.decrementCreditAmount(deal.getTournament().getNbCreditPlayDeal());
				
				// increment the counter of deal played
				p.incrementNbDealPlayed(1);
				partner.incrementNbDealPlayed(1);
				
				// increment counter deal played of session
				session.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER, 1);
				if (sessionPartner != null) {
					sessionPartner.incrementNbDealPlayed(Constantes.TOURNAMENT_CATEGORY_TRAINING_PARTNER, 1);
				}
				
				// write all data in DB
				playerMgr.updatePlayerToDB(p, PlayerUpdateType.CREDIT_DEAL);
				playerMgr.updatePlayerToDB(partner, PlayerUpdateType.CREDIT_DEAL);
			}
		}
		return game;
	}
	
	/**
	 * 
	 * @param session
	 * @return
	 * @throws FBWSException 
	 */
	public TournamentChallenge resetGame(long tableID, long gameID, FBSession session) throws FBWSException {
		boolean tableFromMap = true;
		TournamentTable2 table = getForTableAndGame(tableID, gameID);
		if (table == null) {
			// table not in map ... already finish, try to find it in DB
			table = tourTableDAO.getTableForID(tableID);
			tableFromMap = false;
		}
		if (table == null) {
			log.error("No table found - tableID="+tableID);
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}
		if (table.getCurrentGame() == null) {
			log.error("Game on table is null ! table="+table);
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		if (table.getCurrentGame().getID() != gameID) {
			log.error("Game on table different from gameID="+gameID+" - table="+table);
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}
		// check player is creator
		if (table.getCreatorID() != session.getPlayer().getID()) {
			log.error("Player is not the creator of table ! table="+table+" - player="+session.getPlayer().getID());
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}
		// check if a thread is not running for this game
		if (tournamentGame2Mgr.isThreadGameRunning(table.getCurrentGame().getID())) {
			log.error("A thread is currently running for gameID="+table.getCurrentGame().getID());
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		// check if waiting for spread
		if (tournamentGame2Mgr.isSpreadForGame(table.getCurrentGame().getID())) {
			log.error("A SPREAD IS WAITING FOR THIS GAME="+table.getCurrentGame().getID());
			throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
		}
		// check current player
		if (!table.getCurrentGame().isFinished()) {
			char playerPosition = table.getPlayerPosition(session.getPlayer().getID());
			if (table.getCurrentPosition() != playerPosition) {
				if (table.getCurrentGame().isEndBid()) {
					// Partner is playing : position is in declarer side and current player is a robot and player is partner of current player
					boolean bPartnerAuth = (GameBridgeRule.isPositionInDeclarerSide(playerPosition, table.getCurrentGame().getDeclarer()) && GameBridgeRule.isPartenaire(playerPosition, table.getCurrentPosition()));
					if (!bPartnerAuth){
						log.error("CURRENT POSITION IS NOT PLAYER POSITION : current="+table.getCurrentPosition()+" and not "+playerPosition+" - gameID="+table.getCurrentGame().getID()+" - endBid=true");
						throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
					}
				} else {
					log.error("CURRENT POSITION IS NOT PLAYER POSITION : current="+table.getCurrentPosition()+" and not "+playerPosition+" - gameID="+table.getCurrentGame().getID()+" - endBid=false");
					throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
				}
			}
		}
		// purge existing event game
		FilterEvent filter = new FilterEvent();
		filter.receiverID = session.getPlayer().getID();
		filter.category = Constantes.EVENT_CATEGORY_GAME;
		session.purgeEvent(filter);
		// add table on map
		if (!tableFromMap) {
			mapTable.put(table.getID(), table);
		}
		// remove flag play from player
		table.resetPlayerPlay();
		// reset the game
		tournamentGame2Mgr.resetGame(table);
		// send challenge to partner
		TournamentChallenge tc = tournamentChallengeMgr.resetChallenge(table.getChallengeID(), table.getCreator(), table.getPartner(table.getCreatorID()));
		return tc;
//		return table;
	}

	/**
	 * Change player status on the table. Send event to other player. Call by getEvents
	 * @param tableID
	 * @param playerID
	 * @param status
	 */
	public void onEventPlayerStatusChange(long tableID, long playerID, int status) {
		TournamentTable2 table = mapTable.get(tableID);
		if (table != null) {
			synchronized (tournamentGame2Mgr.getLockDataForTable(table.getID())) {
				try {
					changePlayerStatus(table, playerID, status);
				} catch (Exception e) {
					log.error("Unknown exception !", e);
				}
			}
		}
	}
	
	/**
	 * Change player status on the table. Send event to other player
	 * @param table
	 * @param playerID
	 * @param status
	 */
	public void changePlayerStatus(TournamentTable2 table, long playerID, int status) {
		if (table != null && table.isPlayerTable(playerID)) {
			if (table.getPlayerStatus(playerID) != status) {
				List<Event> events = new ArrayList<Event>();
				TournamentGame2Mgr.addEventGameToList(table, 
						Constantes.EVENT_TYPE_GAME_CHANGE_PLAYER_STATUS_2,
						playerID+Constantes.SEPARATOR_VALUE+status,
						null, events);
				TournamentGame2Mgr.pushListEventToTable(events, table);
				table.setPlayerStatus(playerID, status);
			}
		} else {
			log.error("Table not valid or Player is not on the table ! table="+table+" - playerID="+playerID);
		}
	}
	
	/**
	 * Create a table2 for this tournament, creator and partner
	 * @param t
	 * @param creator
	 * @param partner
	 * @return
	 */
	@Transactional
	public TournamentTable2 createTable(Tournament t, Player creator, Player partner) {
		synchronized (mapTable) {
			if (t != null && creator != null && partner != null) {
				TournamentTable2 table = tourTableDAO.createTable(t, creator, partner);
				if (table != null) {
					mapTable.put(table.getID(), table);
				}
				return table;
			}
			return null;
		}
	}
	
	/**
	 * Update table data in DB
	 * @param table
	 * @return
	 * @throws FBWSException
	 */
	@Transactional
	public void updateTable(TournamentTable2 table) throws FBWSException {
		if (tourTableDAO.updateTable(table) == null) {
			log.error("Error to persist table !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
	}
	
	
	/**
	 * Leave the game on the table
	 * @param table
	 * @param p
	 * @return
	 * @throws FBWSException
	 */
	public boolean leaveGame(TournamentTable2 table, Player p) throws FBWSException {
		if (table == null) {
			log.error("Parameter table is null");
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}
		synchronized (tournamentGame2Mgr.getLockDataForTable(table.getID())) {
			try {
				// check player is on table
				if (!table.isPlayerTable(p.getID())) {
					log.error("Player is not on table ! table="+table+" - player="+p.getID());
					throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
				}
				
				// get game
				TournamentGame2 game = table.getCurrentGame();
				if (game == null) {
					log.error("No game found on table="+table);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				
				boolean actionAllowed = false;
				if (!game.isEndBid()) {
					// bid in progress => only creator
					actionAllowed = (table.getCreatorID() == p.getID());
				} else {
					// card => only declarer or creator if
					char plaPos = table.getPlayerPosition(p.getID());
					if (plaPos == BridgeConstantes.POSITION_NOT_VALID) {
						log.error("Player position is not valid table="+table+" - player="+p.getID());
						throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
					}
					char declarer = game.getDeclarer();
					if (declarer == BridgeConstantes.POSITION_NOT_VALID) {
						log.error("Declarer is not valid table="+table+" - game="+game);
						throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
					}
					if (declarer == plaPos) {
						actionAllowed = true;
					} else {
						actionAllowed = table.getCreatorID() == p.getID();
					}
					 
				}
				// check player is creator
				if (!actionAllowed) {
					log.error("Player is not allowed ! table="+table+" - player="+p.getID());
					throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
				}
				
				if (game.getBidContract() != null) {
					// contract exist => claim with 0 tricks
					game.claimForPlayer(table.getPlayerPosition(p.getID()), 0);
					game.setFinished(true);
					game.setLastDate(Calendar.getInstance().getTimeInMillis());
					// compute score
					TournamentGame2Mgr.computeScore(game);
				} else {
					// set all game value for leave 
					tournamentGame2Mgr.setGameValueLeave(game);
				}
				// update game data
				tournamentGame2Mgr.updateTournamentData(table);
				
				// send event leave game to table
				List<Event> listEvent = new ArrayList<Event>();
				TournamentGame2Mgr.addEventGameToList(table,
						Constantes.EVENT_TYPE_GAME_LEAVE,
						""+p.getID(),
						null, listEvent);
				TournamentGame2Mgr.pushListEventToTable(listEvent, table);
				
				return true;
			} catch (FBWSException e) {
				throw e;
			} catch (Exception e) {
				log.error("Unknown exception !", e);
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	/**
	 * Set tournament data before close it
	 * @param tour
	 * @return
	 */
	public int storeTournamentData(Tournament tour) {
		int nbPlayer = 0;
		if (tour != null) {
			List<TournamentDeal> listDeal = tournamentMgr.getDealForTournament(tour.getID());
			for (TournamentDeal td : listDeal) {
				List<TournamentGame2> listGame = tournamentGame2DAO.listForDeal(td.getID());
				nbPlayer++;
				// update game data
				for (TournamentGame2 g : listGame) {
					if (!g.isFinished()) {
						if (g.getBidContract() != null) {
							// contract exist => claim with 0 tricks
							g.claimForPlayer(BridgeConstantes.POSITION_SOUTH, 0);
							g.setFinished(true);
							g.setLastDate(Calendar.getInstance().getTimeInMillis());
							// compute score
							TournamentGame2Mgr.computeScore(g);
						} else {
							tournamentGame2Mgr.setGameValueLeave(g);
						}
						tournamentGame2DAO.updateGame(g);
					}
					// remove table
					mapTable.remove(g.getTable().getID());
				}
			}
		}
		return nbPlayer;
	}
	
	/**
	 * Return the map of table. Used by admin JSP pages. 
	 * @return
	 */
	public Map<Long, TournamentTable2> getMapTable() {
		return mapTable;
	}
	
	/**
	 * Return the date of last purge done by task
	 * @return
	 */
	public long getDateLastPurge() {
		return dateLastPurge;
	}
	
	public String getStringDateNextPurgeMapScheduler() {
		return Constantes.getStringDateForNextDelayScheduler(schedulerPurgeMapFuture);
	}
}
