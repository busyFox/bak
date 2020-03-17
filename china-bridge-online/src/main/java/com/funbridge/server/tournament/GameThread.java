package com.funbridge.server.tournament;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.engine.BridgeEngineParam;
import com.funbridge.server.engine.BridgeEngineResult;
import com.funbridge.server.tournament.data.SpreadGameData;
import com.funbridge.server.tournament.data.TournamentGame2;
import com.funbridge.server.tournament.data.TournamentTable2;
import com.funbridge.server.ws.event.Event;
import com.gotogames.common.bridge.BridgeBid;
import com.gotogames.common.bridge.BridgeCard;
import com.gotogames.common.bridge.GameBridgeRule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class GameThread implements Runnable {
	private Logger log = LogManager.getLogger(this.getClass());
	private boolean interrupt = false;
	private String threadName;
	private TournamentGame2Mgr mgr = ContextManager.getTournamentGame2Mgr();
	private TournamentTable2 table;
    private boolean startGame;
	
	public synchronized void interruptRun() {
		interrupt = true;
	}
	
	public synchronized boolean isInterrupt() {
		return interrupt;
	}
	
	public String getThreadName() {
		return threadName;
	}
	
	public GameThread(TournamentTable2 table, boolean startGame) {
		this.table = table;
        this.startGame = startGame;
		if (table != null && table.getCurrentGame() != null) {
			threadName = "RobotGameThread for table="+table.getID()+" - gameID="+table.getCurrentGame().getID();
            if (log.isDebugEnabled()) {
                log.debug("TournamentRobotGameThread - create instance :" + threadName);
            }
		} else {
			log.error("table or current game on table is null - table="+table);
		}
	}
	
	@Override
	public void run() {
		if (table != null && table.getCurrentGame() != null) {
			TournamentGame2 game = table.getCurrentGame();
			// add to list of thread game running
			mgr.addListThreadGameRunning(game.getID(), this);
			
			// if start game => event begin game & current player
            if (startGame) {
                if (FBConfiguration.getInstance().getIntValue("tournament.gameEventAfterStart.enable", 1) == 1) {
                    int delay = FBConfiguration.getInstance().getIntValue("tournament.gameEventAfterStart.delayBeforeEvent", 500);
                    if (delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (Exception e) {
                        }
                    }
                    List<Event> listEvent = new ArrayList<Event>();
                    SpreadGameData spreadData = mgr.getSpreadGame(game.getID());
                    boolean sendBeginGameWithCurrentPlayer = true;
                    if (spreadData != null) {
                        // waiting for spread ?
                        if (!spreadData.isWaitingForCheckSpread()) {
                            // send BEGIN GAME without current player
                            TournamentGame2Mgr.addEventGameToList(table, Constantes.EVENT_TYPE_GAME_BEGIN_GAME, null, null, listEvent);
                            // a spread claim is possible. Send event to ask player agreement
                            TournamentGame2Mgr.addEventGameToList(table, Constantes.EVENT_TYPE_GAME_SPREAD, Character.toString(spreadData.getRequester()),
                                    null, listEvent);
                            sendBeginGameWithCurrentPlayer = false;
                        }
                    }
                    if (sendBeginGameWithCurrentPlayer) {
                        // send event BEGIN_GAME with current player
                        TournamentGame2Mgr.addEventGameToList(table, Constantes.EVENT_TYPE_GAME_BEGIN_GAME, Character.toString(game.getCurrentPlayer()), null, listEvent);
                    }

                    TournamentGame2Mgr.pushListEventToTable(listEvent,table);
                }
            }

			boolean bContinue = true;
			List<Event> listEvent = new ArrayList<Event>();
			try {
				while (bContinue) {
					bContinue = playRobotGameThread(table, listEvent);
					if (!isInterrupt()) {
						TournamentGame2Mgr.pushListEventToTable(listEvent, table);
					} else {
						break;
					}
				}
			}
			catch (Exception e) {
				log.error("Exception on loop run - exception="+e.getMessage(), e);
			}
			// remove to list of thread game running
			mgr.removeListThreadGameRunning(game.getID(), this);
		} else {
			// no game in session
			log.warn("Session or game on session is null ! threadName="+threadName);
		}
	}
	
	/**
	 * Check if config param game.playDefaultValueOnEngineError is enabled
	 * @return
	 */
	public boolean isConfigPlayDefaultValueOnEngineError() {
		return FBConfiguration.getInstance().getIntValue("game.playDefaultValueOnEngineError", 0) == 1;
	}
	
	/**
	 * The run method of thread robotGameThread call this method to perform the play of robot
	 * @param table
	 * @param listEvent
	 * @return
	 * @throws Exception
	 */
	public boolean playRobotGameThread(TournamentTable2 table, List<Event> listEvent) throws Exception {
		boolean bNextPlayerEngine = false;
		if (table == null) {
			log.error("Parameter table is null !");
			return false;
		}
		TournamentGame2 game = table.getCurrentGame();
		if (game == null) {
			log.error("Parameter game on table is null ! table="+table);
			return false;
		}
		BridgeEngineParam param = null;
		BridgeEngineResult result = null;
		boolean bReqBid = false;
		String resContent = null;
		boolean bReqMotor = false;
		long gameID = -1;
		int gameNbPlayed = 0;
		
		try {
			//-----------------------------------------
			// PREPARE REQUEST MOTOR
			synchronized (mgr.getLockDataForTable(table.getID())) {
					if (isInterrupt()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Interrupt current thread ! " + getThreadName());
                        }
						return false;
					}
					gameID = game.getID();
					table = game.getTable();
					if (table == null) {
						log.error("Game parameter table is null !");
						return false;
					}
					gameNbPlayed = game.getNbBidCardPlayed();
					bNextPlayerEngine = isRobotToPlay(table, game);
					if (bNextPlayerEngine) {
						bNextPlayerEngine = false;
						bReqBid = !game.isEndBid();
						// BIDS
						if (bReqBid) {
							if (mgr.getGameRobotMode(game) == Constantes.GAME_MODE_ROBOT_PASS) {
								resContent = "PA";
							} else {
								bReqMotor = true;
								param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_BID);
							}
						}
						// CARDS
						else {
							if (!game.isFinished()) {
								BridgeCard card = null;
								// OPTIMISATIONS - Last trick - simply play last card
								if (game.getNbTricks() == 12) {
									// IF LAST TRICK, PLAY THE LAST CARD !
									card = GameBridgeRule.getLastCardForPlayer(game.getListCard(),
										GameBridgeRule.convertCardDealToList(game.getDeal().getDistribution().getCards()), 
										table.getCurrentPosition());
									if (card == null) {
										log.error("Last trick : No card found for player="+table.getCurrentPosition()+" - gameID="+gameID);
									}
								}
								// PLAYER HAS ONLY ONE CARD IN THIS COLOR => PLAY IT !
								else {
									// not at begin trick !
									if (!GameBridgeRule.isBeginTrick(game.getListCard())) {
										BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
										if (firstcardTrick != null) {
											card = GameBridgeRule.getOnlyOneCardForPlayerAndColor(game.getListCard(), 
												GameBridgeRule.convertCardDealToList(game.getDeal().getDistribution().getCards()),
												table.getCurrentPosition(), firstcardTrick.getColor());
										}
									}
								}
								if (card == null) {
									bReqMotor = true;
									param = BridgeEngineParam.createParam(game, Constantes.ENGINE_REQUEST_TYPE_CARD);
								} else {
									if (!mgr.playValidCard(table, card, listEvent)) {
										throw new Exception("Method playValidCard return false !");
									}
									mgr.updateTournamentData(table);
									return isRobotToPlay(table, game);
								}
							}
						}
					} else {
                        if (log.isDebugEnabled()) {
                            log.debug("It is not robot to play : position=" + table.getCurrentPosition());
                        }
						return false;
					}
			}// end of synchronization
			
			//----------------------------------------------
			// SEND REQUEST TO MOTOR
			if (isInterrupt()) {
                if (log.isDebugEnabled()) {
                    log.debug("Interrupt current thread ! " + getThreadName());
                }
				return false;
			}
			if (bReqMotor && param != null) {
				String operation = "";
				if (bReqBid) {
					operation = "nextBid";
					result = mgr.getEngine().getNextBid(param);
				} else {
					operation = "nextCard";
					result = mgr.getEngine().getNextCard(param);
				}
				if (result != null && !result.isError()) {
					resContent = result.getContent();
				} else {
					log.error("Error operation="+operation+" - game2="+game.toString()+" - engineParam="+param);
				}
				
				if (resContent == null && !isConfigPlayDefaultValueOnEngineError()) {
					throw new Exception("operation="+operation+" result is null - param="+param+" => throw exception");
				}
			}
			
			//-----------------------------------------------
			// TREAT RESPONSE
			if (isInterrupt()) {
                if (log.isDebugEnabled()) {
                    log.debug("Interrupt current thread ! " + getThreadName());
                }
				return false;
			}
			game = table.getCurrentGame();
			if (game != null && gameID == game.getID()) {
				synchronized (mgr.getLockDataForTable(table.getID())) {
						// check game status
						boolean bStatusOK = true;
						if (game.getNbBidCardPlayed() != gameNbPlayed) {
							bStatusOK = false;
							log.error("Error gameNbPlayed is not the same before sending request ! current="+game.getNbBidCardPlayed()+" - previous="+gameNbPlayed+" - game2="+game.toString());
						}
						if (!isRobotToPlay(table, game)) {
							bStatusOK = false;
							log.error("Error no robot to play ! position="+game.getCurrentPlayer()+" - game2="+game.toString());
						}
						if (!bStatusOK) {
							return false;
						}
						//------------------------------
						// NEXT BID
						if (bReqBid) {
							// result can contains many bids (anticipation) and length is always multiple 2
							if (resContent == null || resContent.length() % 2 != 0) {
								resContent = "PA";
								log.error("getNexBid not valid result="+resContent+" - game2="+game+"  => play PA - param="+param);
							}
						
							int idxRes = 0;
							boolean bContinue = true;
							// loop on each bid in anticipation
							while (idxRes < resContent.length() && bContinue) {
								String tempBid = resContent.substring(idxRes, idxRes+2);
								BridgeBid bid = BridgeBid.createBid(tempBid, table.getCurrentPosition());
								if (bid == null || !GameBridgeRule.isBidValid(game.getListBid(), bid)) {
									log.error("Bid computed by engine not valid - result="+tempBid+" - bid="+bid+" - game2="+game+" - engineParam="+param+" -> Play PA");
									bid = BridgeBid.createBid("PA", table.getCurrentPosition());
									bContinue = false;
								}
								if (!mgr.playValidBid(table, bid, listEvent)) {
									throw new Exception("Method playValidBid return false !");
								}
								// check it is always robot to play
								if (game.isEndBid() || !isRobotToPlay(table, game)) {
									bContinue = false;
								}
								idxRes += 2;
                                // not process info alert from argine
                                if (resContent.length() > idxRes && resContent.charAt(idxRes) == 'a') {
                                    idxRes++;
                                }
							}
						}
						// -----------------------
						// NEXT CARD
						else {
							BridgeCard card = null;
							// result can contains many cards (anticipation) and length is always multiple 2
							if (resContent == null) {
								// error => play the smallest card !
								BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
								card = GameBridgeRule.getSmallestCardForPlayerAndColor(game.getListCard(),
										GameBridgeRule.convertCardDealToList(game.getDeal().getDistribution().getCards()),
										table.getCurrentPosition(),
										(firstcardTrick!=null?firstcardTrick.getColor():null));
								log.error("getNextCard not valid result="+resContent+" - game2="+game+"  => play smallest card="+card+" - engineParam="+param);
								if (!mgr.playValidCard(table, card, listEvent)) {
									throw new Exception("Method playValidCard return false !");
								}
							} else {
								int idxRes = 0;
								boolean bContinue = true;
								// loop on each card in anticipation
								while (idxRes < resContent.length() && bContinue) {
									String tempCard = resContent.substring(idxRes, idxRes+2);
									card = BridgeCard.createCard(tempCard, table.getCurrentPosition());
									if (card == null  || !GameBridgeRule.isCardValid(game.getListCard(), card, GameBridgeRule.convertCardDealToList(game.getDeal().getDistribution().getCards()))) {
										// error => play the smallest card !
										BridgeCard firstcardTrick = GameBridgeRule.getFirstCardOnCurrentTrick(game.getListCard());
										card = GameBridgeRule.getSmallestCardForPlayerAndColor(game.getListCard(),
												GameBridgeRule.convertCardDealToList(game.getDeal().getDistribution().getCards()),
												table.getCurrentPosition(),
												(firstcardTrick!=null?firstcardTrick.getColor():null));
										log.error("Card computed by engine not valid - result="+tempCard+" - game2="+game+" - engineParam="+param+" => play smallest card="+card);
										bContinue = false;
									}
									if (!mgr.playValidCard(table, card, listEvent)) {
										throw new Exception("Method playValidCard return false !");
									}
									// check it is always robot to play
									if (game.isFinished() || !isRobotToPlay(table, game)) {
										bContinue = false;
									}
									idxRes += 2;
									// not process info spread from argine
                                    if (resContent.length() > idxRes && resContent.charAt(idxRes) == 's') {
                                        idxRes++;
                                    }
								}
							}
						}
						// after play card or bid, update tournament
						mgr.updateTournamentData(table);
						bNextPlayerEngine = isRobotToPlay(table, game);
				} // end of synchronization
			}
		} catch (Exception e) {
			log.error("Exception on thread playRobotGame - game2="+game, e);
			// send event
			TournamentGame2Mgr.addEventGameToList(table, Constantes.EVENT_TYPE_GAME_ENGINE_ERROR, null, null, listEvent);
		}
		
		return bNextPlayerEngine;
	}
	
	/**
	 * Check if it is engine to play
	 * @param table
	 * @param game
	 * @return
	 */
	private boolean isRobotToPlay(TournamentTable2 table, TournamentGame2 game) {
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
		if (table.getTournament().isFinished()) {
            if (log.isDebugEnabled()) {
                log.debug("Tournament is finished !");
            }
			return false;
		}
		// all player must be present
			// only for robot player
			if (!table.isCurrentPlayerHuman()) {
				if (mgr.isSpreadForGame(game.getID())) {
					// waiting for spread ...
                    if (log.isDebugEnabled()) {
                        log.debug("There is a spread on this game game=" + game);
                    }
					return false;
				}
				return true;
			}
		return false;
	}
}
