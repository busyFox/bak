package com.funbridge.server.tournament;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.engine.BridgeEngineParam;
import com.funbridge.server.engine.BridgeEngineResult;
import com.funbridge.server.engine.EngineRest;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.tournament.dao.TournamentGame2DAO;
import com.funbridge.server.tournament.data.SpreadGameData;
import com.funbridge.server.tournament.data.TournamentGame2;
import com.funbridge.server.tournament.data.TournamentSettings;
import com.funbridge.server.tournament.data.TournamentTable2;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.event.Event;
import com.funbridge.server.ws.event.EventField;
import com.gotogames.common.bridge.*;
import com.gotogames.common.lock.LockWeakString;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component(value="tournamentGame2Mgr")
@Scope(value="singleton")
public class TournamentGame2Mgr extends FunbridgeMgr {
	@Resource(name="tournamentGame2DAO")
	private TournamentGame2DAO tourGameDAO = null;
	
	private EngineRest engine = null;
	private ConcurrentHashMap<Long, GameThread> mapThreadGameRunning = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Long, SpreadGameData> mapSpreadGameWaiting = new ConcurrentHashMap<>();
	private ExecutorService threadPoolGame = null;
	private LockWeakString lockTable = new LockWeakString();
	
	/**
	 * Call by spring on initialisation of bean
	 */
	@PostConstruct
	@Override
	public void init() {
		engine = new EngineRest("TournamentGame2");
		threadPoolGame = Executors.newFixedThreadPool(FBConfiguration.getInstance().getIntValue("game2.nbThreadPoolGame", 5));
	}
	
	@PreDestroy
	@Override
	public void destroy() {
		mapThreadGameRunning.clear();
		mapSpreadGameWaiting.clear();
		// shutdown pool game
		if (threadPoolGame != null) {
			threadPoolGame.shutdown();
			try {
				if (threadPoolGame.awaitTermination(30, TimeUnit.SECONDS)) {
					threadPoolGame.shutdownNow();
				}
			} catch (InterruptedException e) {
				threadPoolGame.shutdownNow();
			}
		}
	}
	
	@Override
	public void startUp() {
		
	}
	
	public void addListThreadGameRunning(long gameID, GameThread thread) {
		mapThreadGameRunning.put(gameID, thread);
	}
	
	public GameThread getThreadGameRunning(long gameID) {
		return mapThreadGameRunning.get(gameID);
	}
	
	public void removeListThreadGameRunning(long gameID) {
		mapThreadGameRunning.remove(gameID);
	}
	
	public void removeListThreadGameRunning(long gameID, GameThread thread) {
		mapThreadGameRunning.remove(gameID, thread);
	}
	
	public boolean isThreadGameRunning(long gameID) {
		return mapThreadGameRunning.containsKey(gameID);
	}
	
	public void addSpreadGame(long gameID, SpreadGameData spreadData) {
		mapSpreadGameWaiting.put(gameID, spreadData);
	}
	
	public void removeSpreadGame(long gameID) {
		mapSpreadGameWaiting.remove(gameID);
	}
	
	public SpreadGameData getSpreadGame(long gameID) {
		return mapSpreadGameWaiting.get(gameID);
	}
	
	public boolean isSpreadForGame(long gameID) {
		return mapSpreadGameWaiting.containsKey(gameID);
	}
	
	public Object getLockDataForTable(long tableID) {
		return lockTable.getLock(""+tableID);
	}

	/**
	 * Start a thread to play the game by robot
	 * @param table
     * @param startGame
	 */
	public void startGameThread(TournamentTable2 table, boolean startGame) {
		if (table != null) {
			TournamentGame2 game = table.getCurrentGame();
			if (game != null) {
				if (!isThreadGameRunning(game.getID())) {
					GameThread gameThread = new GameThread(table, startGame);
					threadPoolGame.execute(gameThread);
				} else {
                    if (log.isDebugEnabled()) {
                        log.debug("Thread is already running on table=" + table);
                    }
				}
			} else {
				log.error("Game is null on table="+table);
			}
		} else {
			log.error("Table is null !");
		}
	}
	
	/**
	 * Create an event of game category and add it to the list.
	 * @param table
	 * @param type
	 * @param typeData
	 * @param events
	 */
	public static void addEventGameToList(TournamentTable2 table, String type, String typeData, EventField[] events, List<Event> listEventToPush) {
		if (table != null) {
			Logger log = ContextManager.getTournamentGame2Mgr().getLogger();
			if (log.isDebugEnabled()) {
				log.debug("add game event : type="+type+" - typeData="+typeData);
			}
			Event evt = new Event();
			evt.timestamp = System.currentTimeMillis();
			evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
			String gameStep = null;
			if (table.getCurrentGame() != null) {
				gameStep = ""+table.getCurrentGame().getStep();
			}
			evt.addFieldCategory(Constantes.EVENT_CATEGORY_GAME, gameStep);
			evt.addFieldType(type, typeData);
			evt.addField(new EventField(Constantes.EVENT_FIELD_TABLE_ID, ""+table.getID(), null));
			if (table.getCurrentGame() != null) {
				evt.addField(new EventField(Constantes.EVENT_FIELD_GAME_ID, ""+table.getCurrentGame().getID(), null));
			}
			if (events != null) {
				for (int i = 0; i < events.length; i++) {
					evt.addField(events[i]);
				}
			}
			listEventToPush.add(evt);
			// sleep 1 ms to have event with timestamp different
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				log.error("Interruption !", e);
			}
		}
	}
	
	/**
	 * Push a duplicate event list to session1 & session2  
	 * @param listEventToPush
	 * @param session1
	 * @param session2
	 */
	public static void pushListEvent(List<Event> listEventToPush, FBSession session1, FBSession session2) {
		if (session1 != null) {
			session1.pushListEvent(duplicateEventList(listEventToPush));
		}
		if (session2 != null) {
			session2.pushListEvent(duplicateEventList(listEventToPush));
		}
		// important ! remove all events to not send duplicate event
		listEventToPush.clear();
	}
	
	/**
	 * Duplicate list event
	 * @param listEvent
	 * @return
	 */
	private static List<Event> duplicateEventList(List<Event> listEvent) {
		List<Event> list = new ArrayList<Event>();
		for (Event e : listEvent) {
			list.add((Event)e.clone());
		}
		return list;
	}
	
	/**
	 * Push list event to all table
	 * @param events
	 * @param table
	 */
	public static void pushListEventToTable(List<Event> events, TournamentTable2 table) {
		if (table != null && events != null && events.size() > 0) {
			FBSession sessionSouth = ContextManager.getPresenceMgr().getSessionForPlayer(table.getPlayerSouth());
			FBSession sessionNorth = ContextManager.getPresenceMgr().getSessionForPlayer(table.getPlayerNorth());
			pushListEvent(events, sessionSouth, sessionNorth);
		}
	}
	
	/**
	 * Add game in DB
	 * @param game
	 * @return
	 * @throws FBWSException
	 */
	@Transactional
	public TournamentGame2 persistGame(TournamentGame2 game) throws FBWSException {
		game = tourGameDAO.persistGame(game);
		if (game == null) {
			log.error("Error to persist game !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		return game;
	}
	
	/**
	 * Update game data in DB
	 * @param game
	 * @return
	 * @throws FBWSException
	 */
	@Transactional
	public TournamentGame2 updateGame(TournamentGame2 game) throws FBWSException {
		game = tourGameDAO.updateGame(game);
		if (game == null) {
			log.error("Error to update game !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		return game;
	}
	
	/**
	 * Update the data of table & game in DB
	 * @param table
	 * @throws FBWSException
	 */
	public void updateTournamentData(TournamentTable2 table) throws FBWSException {
		if (table != null) {
			
			// if game finished, update tournament
			if (table.getCurrentGame() != null && table.getCurrentGame().isFinished()) {
				// only 1 deal and 1 player => set finished on table
				table.setDateFinishTournement(System.currentTimeMillis());
				// update game data
				ContextManager.getTournamentGame2Mgr().updateGame(table.getCurrentGame());
				// update table data
				ContextManager.getTournamentTrainingPartnerMgr().updateTable(table);
				// update challenge status
				ContextManager.getTournamentChallengeMgr().updateChallengeStatus(table.getChallengeID(), Constantes.TOURNAMENT_CHALLENGE_STATUS_END);
			}
		}
	}
	
	/**
	 * Set value for leave game
	 * @param game
	 */
	public void setGameValueLeave(TournamentGame2 game) {
		if (game != null) {
			// set end of game
			game.setEndBid(true);
			game.setFinished(true);
			// set score
			game.setScore(Constantes.GAME_SCORE_LEAVE);
			game.setLastDate(Calendar.getInstance().getTimeInMillis());
		}
	}
	
	public EngineRest getEngine() {
		return engine;
	}
	
	/**
	 * Play the bid and set the game data
	 * @param table
	 * @param bid
	 * @param listEvent
	 * @return true if no error else false
	 */
	public boolean playValidBid(TournamentTable2 table, BridgeBid bid, List<Event> listEvent) {
		if (table != null && table.getCurrentGame() != null && bid != null) {
			TournamentGame2 game = table.getCurrentGame();
			// game is finished => no update to do !
			if (game.isFinished()) {
                if (log.isDebugEnabled()) {
                    log.debug("Game is already finished !");
                }
				return true;
			}
			
			// game bids is finished
			if (game.isEndBid()) {
				log.error("Bids are already finished ! - gameID="+game.getID()+" - bid="+bid+" - bids="+game.getBidsWS());
				return false;
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Play bid : tableID="+table.getID()+" - bid="+bid);
			}
			
			// IMPORTANT add the card after adding event (step value)
			// the bid is valid => add to the played bids
			game.addBid(bid);
			
			// push event play bid
			addEventGameToList(table, Constantes.EVENT_TYPE_GAME_BID, bid.toString()+bid.getOwner(), null, listEvent);
								
			// is it the end of bid ?
			if (GameBridgeRule.isBidsFinished(game.getListBid())) {
				game.setEndBid(true);
				BridgeBid higherBid = GameBridgeRule.getHigherBid(game.getListBid());
				String msgEvent;
				if (higherBid.isPass()) {
					game.setContractType(Constantes.CONTRACT_TYPE_PASS);
					msgEvent = game.getContractWS();
				} else{
					game.setContract(higherBid.toString());
					game.setDeclarer(GameBridgeRule.getWinnerBids(game.getListBid()));
					if (GameBridgeRule.isX2(game.getListBid())) {
						game.setContractType(Constantes.CONTRACT_TYPE_X2);
					} else if (GameBridgeRule.isX1(game.getListBid())) {
						game.setContractType(Constantes.CONTRACT_TYPE_X1);
					} else {
						game.setContractType(Constantes.CONTRACT_TYPE_NORMAL);
					}
					msgEvent = game.getContractWS()+"-"+game.getDeclarer();
				}
				// push event end of bids
				addEventGameToList(table, Constantes.EVENT_TYPE_GAME_END_BIDS, msgEvent, null, listEvent);
				
				// bid is PASS
				if (higherBid.isPass()) {
					updateAtEndGame(table, listEvent);
				} else {
					// change current position
					addEventGameToList(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null, listEvent);
				}
			} else {
				// change current position
				addEventGameToList(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null, listEvent);
			}
			game.setLastDate(Calendar.getInstance().getTimeInMillis());
			return true;
		}
		log.error("A PARAMETER IS NULL ! : bid="+bid);
		return false;
	}
	
	/**
	 * Play the card and set the game data
	 * @param table
	 * @param card
	 * @param listEvent
	 * @return true if all is correct else false
	 */
	public boolean playValidCard(TournamentTable2 table, BridgeCard card, List<Event> listEvent) {
		if (table != null && table.getCurrentGame() != null && card != null) {
			TournamentGame2 game = table.getCurrentGame();
			// game is finished => no update to do !
			if (game.isFinished()) {
                if (log.isDebugEnabled()) {
                    log.debug("Game is already finished !");
                }
				return true;
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Play card : tableID="+table.getID()+" - card="+card);
			}
			
			// the card is valid => add to the played card
			game.addCard(card);
						
			// push event play bid
			addEventGameToList(table, Constantes.EVENT_TYPE_GAME_CARD, card.toString()+card.getOwner(),null, listEvent);
			
			// IMPORTANT add the card after adding event (step value)
			
						
			// is it the end of trick ?
			if (GameBridgeRule.isEndTrick(game.getListCard())) {
				BridgeCard trickWinner = GameBridgeRule.getLastWinnerTrick(game.getListCard(), game.getBidContract());
				// add winner to game
				game.addTrickWinner(trickWinner.getOwner());
				// push event end of bids
				addEventGameToList(table, Constantes.EVENT_TYPE_GAME_END_TRICK, Character.toString(trickWinner.getOwner()),	null, listEvent);
				
				// is it the end of game ?
				if (GameBridgeRule.isEndGame(game.getListCard())) {
					updateAtEndGame(table, listEvent);
				} else {
					// end of trick => test if spread can be claimed
					if ((game.getNbTricks() >= FBConfiguration.getInstance().getIntValue("game.nbTricksForSpread", 5)) &&
						(game.getNbTricks() != 12)) {
						
						BridgeGame bg = BridgeGame.create(game.getDeal().getDistribution().getString(),
								game.getListBid(),
								game.getListCard());
						boolean bClaim = false;
						if (bg != null) {
							char claimPosition = GameBridgeRule.claimGame(bg);
							if (claimPosition != BridgeConstantes.POSITION_NOT_VALID) {
								addSpreadGame(game.getID(), new SpreadGameData(claimPosition, false, 1));
								// a spread claim is possible. Send event to ask player agreement
								addEventGameToList(table, Constantes.EVENT_TYPE_GAME_SPREAD, Character.toString(claimPosition),	null, listEvent);
								bClaim = true;
							}
						} else {
							log.error("Error to create BridgeGame : deal="+game.getDeal().getDistribution().getString()+" game="+game.toString());
						}
						if (!bClaim) {
							// no spread claim .. continue normal game : send event current position
							addEventGameToList(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null, listEvent);
						}
					} else {
						// change current position - next player = trick winner 
						addEventGameToList(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null, listEvent);
					}
				}
			}
			// not end of trick
			else {
				// change current position - next player
				addEventGameToList(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(game.getCurrentPlayer()), null, listEvent);
			}
			game.setLastDate(Calendar.getInstance().getTimeInMillis());
			return true;
		}
		log.error("A PARAMETER IS NULL !");
		return false;
	}
	
	/**
	 * update the game at the end of game. The game must be ended. No check is done !
	 * @param table
	 * @param listEvent
	 */
	private void updateAtEndGame(TournamentTable2 table, List<Event> listEvent) {
		if (table != null && table.getCurrentGame() != null) {
			TournamentGame2 game = table.getCurrentGame();
			removeSpreadGame(game.getID());
			game.setFinished(true);
			computeScore(game);
			addEventGameToList(table, Constantes.EVENT_TYPE_GAME_END_GAME, ""+game.getScore(), null, listEvent);
		}
	}
	
	/**
	 * Compute the score on this game
	 * @param game
	 */
	public static void computeScore(TournamentGame2 game) {
		if (game.isFinished()) {
			if (game.getBidContract().isPass()) {
				game.setScore(Constantes.GAME_SCORE_PASS);
				game.setTricks(0);
			} else {
				int contreValue = GameBridgeRule.isX2(game.getListBid())?2:(GameBridgeRule.isX1(game.getListBid())?1:0);
				int score = GameBridgeRule.getGameScore(game.getNbTricksWinByPlayerAndPartenaire(game.getDeclarer()),
						game.getBidContract(),
						contreValue,
						game.getDeal().getDistribution().getVulnerability());
				if (!GameBridgeRule.isPartenaire(game.getBidContract().getOwner(), BridgeConstantes.POSITION_SOUTH)) {
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
	 * Return a string info for this bid according to the current game
	 * @param bids
	 * @return
	 * @throws FBWSException
	 */
	public String getBidInfo(TournamentTable2 table, String bids) throws FBWSException {
		if (table == null) {
			log.error("table is null !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		synchronized (getLockDataForTable(table.getID())) {
			try {
				TournamentGame2 game = table.getCurrentGame();
				if (game == null) {
					log.error("Game is null in table="+table);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				String bidList="";
				if (bids != null && bids.length() > 0) {
					String[] temp = bids.split("-");
					for (int i = 0; i < temp.length; i++) {
						bidList += temp[i].substring(0, 2);
					}
				}
				
				// if bids sequence not valid => no bid info !
				List<BridgeBid> listBid = GameBridgeRule.convertPlayBidsStringToList(bidList, 'N');
				if (listBid == null) {
					log.warn("Bids sequence not valid ! bids="+bids+" - bidList="+bidList);
					throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
				}
				if (!GameBridgeRule.isBidsSequenceValid(listBid)) {
					log.warn("Bids sequence is not valid ! bids="+bids+" - bidList="+bidList);
					throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
				}
				
				String info = "";
                boolean bFixBUG4PA = false;
                if (FBConfiguration.getInstance().getIntValue("game.fixBug4PA", 0) == 1) {
                    if (bidList.equals("PAPAPAPA")) {
                        bFixBUG4PA = true;
                        log.warn("BUG 4PA => Exception");
                        info = "0;13;0;13;0;13;0;13;0;40;0;40;#faible;0";
                    }
                }
                if (!bFixBUG4PA) {
                    BridgeEngineParam paramEngine = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_BID_INFO);
                    paramEngine.setCardList("");
                    paramEngine.setBidList(bidList);
                    BridgeEngineResult result = engine.getBidInformation(paramEngine);
                    if (result.isError()) {
                        log.error("Error on result : " + result.getContent());
                    } else {
                        info = result.getContent();
                    }
                }
				log.debug("Bid info="+info);
				if (info == null || info.length()==0 || info.equals(Constantes.ENGINE_RESULT_NULL)) {
					log.error("Info for bid not valid : info="+info+" - gameID="+game.getID()+" - bids="+bids);
					throw new FBWSException(FBExceptionType.GAME_BID_INFO_ERROR);
				}
				return info;
			} catch (FBWSException e) {
				throw e;
			} catch (Exception e) {
				log.error("Unknown exception !", e);
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	/**
	 * Play a bid. Check if the bid is valid and can be played.
	 * @param table
	 * @param bidStr The bid as string (2C or PA ...)
	 * @param playerID
     * @param step
	 * @return true if game is not finished and next player must played, false if game is finished
	 * @throws FBWSException
	 */
	public boolean playBid(TournamentTable2 table, String bidStr, long playerID, int step) throws FBWSException {
		if (table == null) {
			log.error("Table is null !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		synchronized (getLockDataForTable(table.getID())) {
			try {
				TournamentGame2 game = table.getCurrentGame();
				if (game == null) {
					log.error("Game is null in table="+table);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				// check if a thread is not running for this game
				if (isThreadGameRunning(game.getID())) {
					log.error("A thread is currently running for gameID="+game.getID());
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
                if (FBConfiguration.getInstance().getIntValue("general.checkStep", 1) == 1 && (step != -1) && game.getStep() != step) {
                    if (FBConfiguration.getInstance().getIntValue("general.checkStepException", 0) == 1) {
                        log.error("Game step is not the same ! gameStep=" + game.getStep() + " - param step=" + step + " - game=" + game);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    } else {
                        return !game.isFinished();
                    }
                }
                // check current position
                char playerPosition = table.getPlayerPosition(playerID);
                if (playerPosition == BridgeConstantes.POSITION_NOT_VALID) {
                    log.warn("Player is not registered at table !");
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (table.getCurrentPosition() != playerPosition) {
                    log.warn("CURRENT POSITION IS NOT PLAYER POSITION");
                    throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
                }
				// Check game
				if (game.isEndBid()) {
					log.warn("BIDS ARE COMPLETED");
					throw new FBWSException(FBExceptionType.GAME_BID_NOT_VALID);
				}
				// check bid value
				BridgeBid bid = BridgeBid.createBid(bidStr, playerPosition);
				if (bid == null) {
					log.warn("BID UNKNOWN : "+bidStr+" - position : "+playerPosition);
					throw new FBWSException(FBExceptionType.GAME_BID_NOT_VALID);
				}
				if (!GameBridgeRule.isBidValid(game.getListBid(), bid)) {
					log.warn("THE BID PLAYED IS NOT AUTHORIZED - gameID="+game.getID()+" - plaID="+playerID+" - bid:"+bid.toString()+" - current bid played:"+game.getBidListStrWithoutPosition());
					throw new FBWSException(FBExceptionType.GAME_BID_NOT_VALID);
				}
				// Every is checked ! so play the bid !
				List<Event> listEvent = new ArrayList<Event>();
				if (playValidBid(table, bid, listEvent)) {
					updateTournamentData(table);
					pushListEventToTable(listEvent, table);
				} else {
					log.error("ERROR WHEN PLAYING BID");
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				return !game.isFinished();
			} catch (FBWSException e) {
				throw e;
			} catch (Exception e) {
				log.error("Unknown exception !", e);
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	/**
	 * Play a card. Check if the card is valid and can be played.
	 * @param table
	 * @param cardStr
	 * @param playerID
     * @param step
	 * @return true if game is not finished and next player must played, false if game is finished
	 * @throws FBWSException
	 */
	public boolean playCard(TournamentTable2 table, String cardStr, long playerID, int step) throws FBWSException {
		if (table == null) {
			log.error("table is null !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		synchronized (getLockDataForTable(table.getID())) {
			try {
				TournamentGame2 game = table.getCurrentGame();
				if (game == null) {
					log.error("Game is null in table="+table);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				// check if a thread is not running for this game
				if (isThreadGameRunning(game.getID())) {
					log.error("A thread is currently running for gameID="+game.getID());
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
                if (FBConfiguration.getInstance().getIntValue("general.checkStep", 1) == 1 && (step != -1) && game.getStep() != step) {
                    if (FBConfiguration.getInstance().getIntValue("general.checkStepException", 0) == 1) {
                        log.error("Game step is not the same ! gameStep=" + game.getStep() + " - param step=" + step + " - game=" + game);
                        throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
                    } else {
                        return !game.isFinished();
                    }
                }
				// check if waiting for spread
				if (isSpreadForGame(game.getID())) {
					log.error("A SPREAD IS WAITING FOR THIS GAME="+game.getID());
					throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
				}
				// Check game
				if (!game.isEndBid()) {
					log.warn("BIDS ARE NOT COMPLETED");
					throw new FBWSException(FBExceptionType.GAME_PLAY_NOT_VALID);
				}
				if (game.isFinished()) {
					log.warn("GAME IS ENDED");
					throw new FBWSException(FBExceptionType.GAME_PLAY_NOT_VALID);
				}
				// check current position
				char playerPosition = table.getPlayerPosition(playerID);
				if (playerPosition == BridgeConstantes.POSITION_NOT_VALID) {
					log.error("Player is not register at table !");
					throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
				}
				if (table.getCurrentPosition() != playerPosition) {
					// Partner is playing : position is in declarer side and current player is a robot and player is partner of current player
					boolean bPartnerAuth = (GameBridgeRule.isPositionInDeclarerSide(playerPosition, game.getDeclarer()) && GameBridgeRule.isPartenaire(playerPosition, table.getCurrentPosition()));
					if (!bPartnerAuth){
						log.error("CURRENT POSITION IS NOT PLAYER POSITION : current="+table.getCurrentPosition()+" and not "+playerPosition+" - gameID="+game.getID());
						throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
					}
				} else {
					// player is in declarer side but not the declarer => the death !
					if (GameBridgeRule.isPositionInDeclarerSide(playerPosition, game.getDeclarer()) && playerPosition != game.getDeclarer()) {
						log.error("CURRENT PLAYER IS THE DEAD player="+playerPosition+" - declarer="+game.getDeclarer());
						throw new FBWSException(FBExceptionType.GAME_PLAY_NOT_VALID);
					}
				}
				// check card value
				BridgeCard card = BridgeCard.createCard(cardStr, table.getCurrentPosition());
				if (card == null) {
					log.error("CARD UNKNOWN : "+cardStr+" - position : "+table.getCurrentPosition());
					throw new FBWSException(FBExceptionType.GAME_CARD_NOT_VALID);
				}
				if (!GameBridgeRule.isCardValid(game.getListCard(), card, GameBridgeRule.convertCardDealToList(game.getDeal().getDistribution().getCards()))) {
					log.error("THE CARD PLAYED IS NOT AUTHORIZED - gameID="+game.getID()+" - plaID="+playerID+" - card:"+card.toString()+" - current card played:"+game.getCardListStrWithoutPosition()+" - distrib="+game.getDeal().getDistribution().getCards());
					throw new FBWSException(FBExceptionType.GAME_CARD_NOT_VALID);
				}
		
				// Every is checked ! so play the card !
				List<Event> listEvent = new ArrayList<Event>();
				if (playValidCard(table, card, listEvent)) {
					updateTournamentData(table);
					pushListEventToTable(listEvent, table);
				} else {
					log.error("ERROR WHEN PLAYING CARD");
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				return !game.isFinished();
			} catch (FBWSException e) {
				throw e;
			} catch (Exception e) {
				log.error("Unknown exception !", e);
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	/**
	 * Receive response for claim spread for this game
	 * @param table
	 * @param playerID
	 * @param response
	 * @return true if game is not finished and next player must played, false if game is finished
	 * @throws FBWSException
	 */
	public boolean setClaimSpreadResponse(TournamentTable2 table, long playerID, boolean response) throws FBWSException{
		if (table == null) {
			log.error("Table is null !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		synchronized (getLockDataForTable(table.getID())) {
			try {
				TournamentGame2 game = table.getCurrentGame();
				if (game == null) {
					log.error("Game is null in table="+table);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				// check player is on table
				if (!table.isPlayerTable(playerID)) {
					log.error("Player is not register at table ! table="+table+" - playerID="+playerID);
					throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
				}
				// check spread data status
				SpreadGameData spreadData = getSpreadGame(game.getID());
				if (spreadData == null) {
					log.error("No spread data waiting for gameID="+game.getID());
					throw new FBWSException(FBExceptionType.GAME_NO_CLAIM_TO_SPREAD);
				}
				boolean bPlayNextPlayer = false;
				synchronized (spreadData) {
					if (spreadData.isAllResponseReceived()) {
						log.error("All response received for this spread data for gameID="+game.getID());
						throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
					}
					// update response for spread data
					spreadData.incrementResponseReceived();
					if (response) {
						spreadData.incrementResponseOK();
					}
					List<Event> listEvent = new ArrayList<Event>();
					if (spreadData.isAllResponseReceived()) {
						if (spreadData.isAllResponseOK()) {
							addEventGameToList(table, Constantes.EVENT_TYPE_GAME_SPREAD_RESULT, Character.toString(spreadData.getRequester()), null, listEvent);
							// accept to spread
							game.claimAllForPlayer(spreadData.getRequester());
							updateAtEndGame(table, listEvent);
							game.setLastDate(Calendar.getInstance().getTimeInMillis());
							updateTournamentData(table);
							bPlayNextPlayer = false;
						} else {
							// remove game from list spread waiting
							removeSpreadGame(game.getID());
							// claim spread is refused => continue normal play : send event current position
							addEventGameToList(table, Constantes.EVENT_TYPE_GAME_CURRENT_PLAYER, Character.toString(table.getCurrentPosition()),
									null, listEvent);
							bPlayNextPlayer = true;
						}
					}
					
					// push event
					pushListEventToTable(listEvent, table);
				}
				return bPlayNextPlayer;
			} catch (FBWSException e) {
				throw e;
			} catch (Exception e) {
				log.error("Unknown exception !", e);
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
		}
	}

	public void resetGame(TournamentTable2 table) throws FBWSException {
		if (table == null) {
			log.error("table is null !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		synchronized (getLockDataForTable(table.getID())) {
			try {
				TournamentGame2 game = table.getCurrentGame();
				if (game == null) {
					log.error("Game is null in table="+table);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				
				// reset game
				game.resetData();
				
				// reset date finish on table
				table.setDateFinishTournement(0);
				
				// update game in DB
				ContextManager.getTournamentGame2Mgr().updateGame(game);
				
			} catch (FBWSException e) {
				throw e;
			} catch (Exception e) {
				log.error("Unknown exception !", e);
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
		}
	}
	
	public int getGameRobotMode(TournamentGame2 g) {
		int mode = g.getModeRobot();
		if (mode == Constantes.GAME_MODE_ROBOT_UNKNOWN) {
			TournamentSettings ts = ContextManager.getTournamentMgr().getTournamentSettings(g.getDeal().getTournament());
			if (ts == null) {
				mode = Constantes.GAME_MODE_ROBOT_NORMAL;
			}
			else {
				if (ts.mode.equals(Constantes.TOURNAMENT_SETTINGS_MODE_ENCHERIE_A2)) {
					mode = Constantes.GAME_MODE_ROBOT_PASS;
				} else {
					mode = Constantes.GAME_MODE_ROBOT_NORMAL;
				}
			}
			g.setModeRobot(mode);
		}
		return mode;
	}

	/**
	 * check if claim can be done on this game
	 * @param nbTricks
	 * @return
	 * @throws FBWSException 
	 */
	public boolean checkClaim(TournamentTable2 table, long playerID, int nbTricks) throws FBWSException {
		if (table == null) {
			log.error("table is null !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		synchronized (getLockDataForTable(table.getID())) {
			try {
				TournamentGame2 game = table.getCurrentGame();
				if (game == null) {
					log.error("Game is null in table="+table);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				// check if a thread is not running for this game
				if (isThreadGameRunning(game.getID())) {
					log.error("A thread is currently running for game="+game);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				// check if waiting for spread
				if (isSpreadForGame(game.getID())) {
					log.error("A SPREAD IS WAITING FOR THIS GAME="+game);
					throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
				}
				if (game.isFinished()) {
					log.error("GAME IS ENDED game="+game);
					throw new FBWSException(FBExceptionType.GAME_PLAY_NOT_VALID);
				}
				// check current position
				char playerPosition = table.getPlayerPosition(playerID);
				if (playerPosition == BridgeConstantes.POSITION_NOT_VALID) {
					log.error("Player is not register at table ! - table="+table+" - playerID="+playerID);
					throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
				}
				if (table.getCurrentPosition() != playerPosition) {
					// Partner is playing : position is in declarer side and current player is a robot and player is partner of current player
					boolean bPartnerAuth = (GameBridgeRule.isPositionInDeclarerSide(playerPosition, game.getDeclarer()) && GameBridgeRule.isPartenaire(playerPosition, table.getCurrentPosition()));
					if (!bPartnerAuth){
						log.error("CURRENT POSITION IS NOT PLAYER POSITION : current="+table.getCurrentPosition()+" and not "+playerPosition+" - game="+game);
						throw new FBWSException(FBExceptionType.GAME_NOT_CURRENT_PLAYER);
					}
				}
				boolean result = false;
				if (nbTricks < 0 || nbTricks > 13) {
					log.error("nbTricks not valid ("+nbTricks+") game="+game);
				} else {
					BridgeGame bg = BridgeGame.create(game.getDeal().getDistribution().getString(),
							game.getListBid(),
							game.getListCard());
					result = checkClaimGame(bg, nbTricks);
				}
				if (result) {
					// accept to claim
					List<Event> listEvent = new ArrayList<Event>();
					addEventGameToList(table, Constantes.EVENT_TYPE_GAME_CLAIM, playerPosition +""+nbTricks, null, listEvent);
					game.claimForPlayer(playerPosition, nbTricks);
					updateAtEndGame(table, listEvent);
					game.setLastDate(Calendar.getInstance().getTimeInMillis());
					updateTournamentData(table);
					// push event
					pushListEventToTable(listEvent, table);
				}
				return result;
			} catch (FBWSException e) {
				throw e;
			} catch (Exception e) {
				log.error("Unknown exception !", e);
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
		}
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
                if (log.isDebugEnabled()) {
                    log.debug("Claim always possible for nbTricks=0");
                }
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
}
