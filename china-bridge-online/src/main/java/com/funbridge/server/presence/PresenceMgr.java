package com.funbridge.server.presence;

import com.funbridge.server.Utils.RedisUtils;
import com.funbridge.server.common.*;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.data.*;
import com.funbridge.server.tournament.duel.DuelMgr;
import com.funbridge.server.tournament.duel.data.DuelGame;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.game.ITournamentMgr;
import com.funbridge.server.tournament.generic.TournamentGenericMgr;
import com.funbridge.server.tournament.learning.data.LearningGame;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.SerieEasyChallengeGame;
import com.funbridge.server.tournament.serie.data.SerieTopChallengeGame;
import com.funbridge.server.tournament.serie.data.TourSerieGame;
import com.funbridge.server.tournament.team.data.TeamGame;
import com.funbridge.server.tournament.timezone.data.TimezoneGame;
import com.funbridge.server.tournament.training.TrainingMgr;
import com.funbridge.server.tournament.training.data.TrainingGame;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.player.WSContextInfo;
import com.funbridge.server.ws.player.WSServerParameters;
import com.funbridge.server.ws.presence.WXAccessToken;
import com.funbridge.server.ws.presence.WXUserInfo;
import com.gotogames.common.session.Session;
import com.gotogames.common.session.SessionListener;
import com.gotogames.common.session.SessionMgr;
import com.gotogames.common.tools.JSONTools;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import com.sun.org.apache.bcel.internal.generic.RET;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

@Component(value="presenceMgr")
@Scope(value="singleton")
public class PresenceMgr extends FunbridgeMgr implements SessionListener{
	private SessionMgr sessionMgr = null;
	@Resource(name="playerMgr")
	private PlayerMgr playerMgr = null;
    @Resource(name="tourSerieMgr")
    private TourSerieMgr tourSerieMgr;
    @Resource(name="trainingMgr")
    private TrainingMgr trainingMgr;
    @Resource(name="duelMgr")
    private DuelMgr duelMgr;

    @Resource(name="httpClientPool")
    private HttpClientPool clientPool;

    private boolean serviceMaintenance = false;
    private JSONTools jsonTools = new JSONTools();
	private final Object objSynchroInvalidateSession = new Object();
	
	
	public PresenceMgr() {
        sessionMgr = SessionMgr.createSessionMgr("FUNBRIDGE-SERVER",SessionMgr.SESSION_TYPE_MEMORY);
		sessionMgr.setSessionListener(this);
	}
	
	public Logger getLogger() {
		return log;
	}
	
	/**
	 * Call by spring on initialisation of bean
	 */
	@PostConstruct
	@Override
	public void init() {
		// start session clean scheduler
		sessionMgr.startCleanScheduler(FBConfiguration.getInstance().getIntValue("presence.cleanSessionTaskPeriod", 30));
	}

	@PreDestroy
	@Override
	public void destroy() {
		if (sessionMgr != null) {
			sessionMgr.destroy();
		}
	}
	
	@Override
	public void startUp() {
		
	}

	public boolean isServiceMaintenance() {
		if (FBConfiguration.getInstance().getIntValue("general.maintenance", 0) == 1) {
			return true;
		}
		return serviceMaintenance;
	}

    public boolean isServiceMaintenanceForDevice(String deviceType) {
        if (isServiceMaintenance()) {
            return true;
        }
        return FBConfiguration.getInstance().getIntValue("general.maintenance.device." + deviceType, 0) == 1;
    }
	
	public void setServiceMaintenance(boolean value) {
		serviceMaintenance = value;
	}
	
	/**
	 * Create session for player and device
	 * @param p
	 * @param d
	 * @param clientVersion
     * @param dateLastConnection
     * @param lastDeviceTypeUsed
	 * @return
	 * @throws FBWSException
	 */
	public FBSession createSession(Player p, Device d, String clientVersion, long dateLastConnection, String lastDeviceTypeUsed) throws FBWSException {
		if (p == null || d == null) {
			log.error("Param player or device is null ! player="+p+" - device="+d+" - clientVersion="+clientVersion);
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}
		// retrieve session for this player. !! USE PLAYER MAIL AND NOT LOGIN !!
		Session s = sessionMgr.getSessionForLogin(p.getMail());
		if (s != null) {
            FBSession fbs = (FBSession)s;

            if (fbs.getDevice().getID() != d.getID()) {
                fbs.setDisconnectValue(Constantes.EVENT_VALUE_DISCONNECT_CONNECT_ANOTHER_DEVICE);
            }
			// a session already exist => delete it
			sessionMgr.deleteSession(s.getID());
		}

		// create a new session
        int timeoutSession = FBConfiguration.getInstance().getIntValue("presence.timeoutSession", 600); // value in second
        s = SessionMgr.createSession(p.getMail(), p.getID(), timeoutSession);
        if (s == null) {
            log.error("error creating session for player cert=" + p.getCert() + " - ID=" + p.getID());
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }

		FBSession fbs = new FBSession(s, p, clientVersion, d);
        fbs.setDateLastConnection(dateLastConnection);
        fbs.setLastDeviceTypeUsed(lastDeviceTypeUsed);

		// check player data for training
        trainingMgr.getPlayerHisto(p.getID());

        // check duel stat data
        duelMgr.synchronizeDuelStat(p.getID());

        // store this session
		if (!sessionMgr.putSession(fbs)) {
			log.error("createSession - error to put session for player="+p);
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		return fbs;
	}
	
	/**
	 * Get a session for this sessionID
	 * @param sessionID
	 * @return FBSession object or null if not found
	 */
	public Session getSession(String sessionID) {
		return sessionMgr.getSession(sessionID);
	}
	
	/**
	 * Get a session for this sessionID and touch it (update date of last activity). To be use 
	 * @param sessionID
	 * @return
	 */
	public Session getSessionAndTouch(String sessionID) {
		Session s = sessionMgr.getSession(sessionID);
		if (s != null) {
			s.setDateLastActivity(System.currentTimeMillis());
		}
		return s;
	}
	
	/**
	 * return the list of session currently connected
	 * @return
	 */
	public List<Session> getAllCurrentSession() {
		return sessionMgr.getAllCurrentSession();
	}
	
	/**
	 * Return the number of session connected
	 * @return
	 */
	public int getNbSession() {
		int nbSession = sessionMgr.getAllCurrentSession().size();
        return nbSession;
	}
	
	/**
     * Return Nb Session with protocol=https
     * @return
     */
    public int getNbSessionProtocolHTTPS() {
        int nbHTTPS = 0;
        List<Session> listSession = sessionMgr.getAllCurrentSession();
        for (Session s : listSession) {
            if (s instanceof FBSession) {
                FBSession fbs = (FBSession)s;
                if (fbs.protocol != null && fbs.protocol.equalsIgnoreCase("https")) nbHTTPS++;
            }
        }
        return nbHTTPS;
    }
	
	/**
	 * Check if sessionID is referenced and linked to a player
	 * @param sessionID
	 * @return true if the sessionID is valid or false
	 */
	public boolean isSessionValid(String sessionID) {
		return sessionMgr.isSessionExist(sessionID);
	}
	
	/**
	 * Delete the session
	 * @param sessionID
	 */
	public boolean closeSession(String sessionID, String disconnectValue) {
        if (disconnectValue != null && disconnectValue.length() > 0 && FBConfiguration.getInstance().getIntValue("presence.disconnectValueEnable", 1) == 1) {
            FBSession fbs = (FBSession)getSession(sessionID);
            if (fbs != null) {
                fbs.setDisconnectValue(disconnectValue);
            }
        }
		return sessionMgr.deleteSession(sessionID);
	}

    public boolean closeSessionForPlayer(long playerID, String disconnectValue) {
        FBSession session = getSessionForPlayerID(playerID);
        if (session != null) {
            if (disconnectValue != null && disconnectValue.length() > 0 && FBConfiguration.getInstance().getIntValue("presence.disconnectValueEnable", 1) == 1) {
                session.setDisconnectValue(disconnectValue);
            }
            return sessionMgr.deleteSession(session.getID());
        }
        return false;
    }

	/**
	 * Close all session
	 */
	public void closeAllSession(String disconnectValue) {
        if (disconnectValue != null && disconnectValue.length() > 0 && FBConfiguration.getInstance().getIntValue("presence.disconnectValueEnable", 1) == 1) {
            List<Session> listSession = sessionMgr.getAllCurrentSession();
            for (Session s : listSession) {
                if (s instanceof FBSession) {
                    FBSession fbs = (FBSession) s;
                    fbs.setDisconnectValue(disconnectValue);
                }
            }
        }
		sessionMgr.deleteAllSession();
        ContextManager.getDuelMgr().updateAllGameArgineInProgress(true);
		ContextManager.getClientWebSocketMgr().getMapWebSocket().clear();
	}
	
	/**
	 * Set the last of date activity to now
	 * @param sessionID
	 */
	public void touchSession(String sessionID) {
		sessionMgr.touchSession(sessionID);
	}

	@Override
	public void invalidateSession(Session s) {
		if (s != null && s instanceof FBSession) {
			FBSession fbs = (FBSession)s;
			try {
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + fbs.getPlayer().getID());
                }

                if (fbs.getDisconnectValue() != null && fbs.getDisconnectValue().length() > 0) {
                    fbs.pushEvent(ContextManager.getMessageMgr().buildEventGeneralDisconnect(fbs.getPlayer(), fbs.getDisconnectValue()));
                }
				// destroy websocket
				if (fbs.getWebSocket() != null) {
					ContextManager.getClientWebSocketMgr().removeWebSocketForSession(fbs.getID(), true);
				}

				// exit current game
				if (fbs.getCurrentGameTable() != null) {
					if (fbs.getCurrentGameTable().getGame() != null && !fbs.getCurrentGameTable().getGame().isReplay()) {
						Game game = fbs.getCurrentGameTable().getGame();
                        if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
                            ContextManager.getTourSerieMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getTourSerieMgr().updateGameDB((TourSerieGame)game);
                            ContextManager.getTourSerieMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
                            ContextManager.getSerieTopChallengeMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getSerieTopChallengeMgr().updateGameDB((SerieTopChallengeGame) game);
                            ContextManager.getSerieTopChallengeMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
                            ContextManager.getSerieEasyChallengeMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getSerieEasyChallengeMgr().updateGameDB((SerieEasyChallengeGame) game);
                            ContextManager.getSerieEasyChallengeMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
                            ContextManager.getTrainingMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getTrainingMgr().updateGameDB(game);
                            ContextManager.getTrainingMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
                            ContextManager.getTimezoneMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getTimezoneMgr().updateGameDB(game);
                            ContextManager.getTimezoneMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_DUEL) {
                            ContextManager.getDuelMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getDuelMgr().updateGameDB((DuelGame) game);
                            ContextManager.getDuelMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TEAM) {
                            ContextManager.getTourTeamMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getTourTeamMgr().updateGameDB((TeamGame) game);
                            ContextManager.getTourTeamMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_LEARNING) {
                            ContextManager.getTourLearningMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getTourLearningMgr().updateGameDB(game);
                            ContextManager.getTourLearningMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                        }
                        else {
                            TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(game.getTournament().getCategory());
                            if (tournamentMgr != null) {
                                tournamentMgr.getGameMgr().removeSpreadPlay(game.getIDStr());
                                tournamentMgr.updateGameDB(game);
                                tournamentMgr.getGameMgr().removePlayerRunning(game.getPlayerID());
                            } else {
                                log.error("Game category not supported to save game ! - category=" + Constantes.tourCategory2Name(game.getTournament().getCategory()) + " (" + game.getTournament().getCategory() + ")");
                            }
                        }
						fbs.removeGame();
					}
				}
				
                // set status not present on table
                if (FBConfiguration.getInstance().getIntValue("general.enableChangeStatusOnCloseSession", 0) == 1) {
                    long currentTableID = fbs.getCurrentTableID();
                    if (currentTableID != -1) {
                        ContextManager.getTournamentTrainingPartnerMgr().onEventPlayerStatusChange(currentTableID, fbs.getPlayer().getID(), Constantes.TABLE_PLAYER_STATUS_NOT_PRESENT);
                    }
                }

                // create connection history
                playerMgr.createConnectionHistory2(fbs, System.currentTimeMillis(), true);

                // send event for friend
                if (!isServiceMaintenance() && isEventFriendConnectedEnable()) {
                    MessageNotifMgr notifMgr = ContextManager.getMessageNotifMgr();
                    // get friends for player
                    List<Player> listFriend = playerMgr.listFriendForPlayer(fbs.getPlayer().getID());
                    if (listFriend != null && listFriend.size() > 0) {
                        for (Player plaFriend : listFriend) {
                            // check friend is connected
                            FBSession sessionFriend = getSessionForPlayerID(plaFriend.getID());
                            if (sessionFriend != null) {
                                // send event to friend (recipient is friend !)
                                sessionFriend.pushEvent(notifMgr.buildEventFriendConnected(plaFriend, fbs.getPlayer(), false));
                            }
                        }
                    }
                }
                ContextManager.getDuelMgr().disableMatchMakingForPlayer(fbs.getPlayer().getID());

				// Add deals played
                playerMgr.addDealsPlayed(fbs.getPlayer().getID(), fbs.getMapCategoryPlay());
			}
			catch (Exception e) {
				log.error("Exception to invalidate session for playerID="+fbs.getLoginID(), e);
			}
		} else {
			log.error("Session is null or no FBSession object - session="+s);
		}
	}

	@Override
	public void invalidateListSession(List<Session> listSession) {
		if (listSession != null && listSession.size() > 0) {
            List<TourSerieGame> listGameSerie = new ArrayList<TourSerieGame>();
            List<SerieTopChallengeGame> listGameSerieTopChallenge = new ArrayList<SerieTopChallengeGame>();
            List<SerieEasyChallengeGame> listGameSerieEasyChallenge = new ArrayList<SerieEasyChallengeGame>();
            List<TrainingGame> listGameTraining = new ArrayList<>();
            List<DuelGame> listGameDuel = new ArrayList<>();
            List<TeamGame> listGameTeam = new ArrayList<>();
            List<TimezoneGame> listGameTZ = new ArrayList<>();
            List<LearningGame> listGameLearning = new ArrayList<>();

            Map<Integer, List<Game>> mapGamesForCategory = new HashMap<>();
            mapGamesForCategory.put(Constantes.TOURNAMENT_CATEGORY_TIMEZONE, new ArrayList<Game>());
            mapGamesForCategory.put(Constantes.TOURNAMENT_CATEGORY_TOUR_CBO, new ArrayList<Game>());
            mapGamesForCategory.put(Constantes.TOURNAMENT_CATEGORY_PRIVATE, new ArrayList<Game>());

			List<PlayerConnectionHistory2> listConHist = new ArrayList<PlayerConnectionHistory2>();
            DuelMgr duelMgr = ContextManager.getDuelMgr();
			for (Session s : listSession) {
				FBSession fbs = (FBSession)s;
                if (log.isDebugEnabled()) {
                    log.debug("playerID=" + fbs.getPlayer().getID());
                }
                if (fbs.getDisconnectValue() != null && fbs.getDisconnectValue().length() > 0) {
                    fbs.pushEvent(ContextManager.getMessageMgr().buildEventGeneralDisconnect(fbs.getPlayer(), fbs.getDisconnectValue()));
                }
				// destroy websocket
				if (fbs.getWebSocket() != null) {
					ContextManager.getClientWebSocketMgr().removeWebSocketForSession(fbs.getID(), true);
				}

					// exit for current game
                if (fbs.getCurrentGameTable() != null) {
                    if (fbs.getCurrentGameTable().getGame() != null && !fbs.getCurrentGameTable().getGame().isReplay()) {
                        Game game = fbs.getCurrentGameTable().getGame();
                        if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_NEWSERIE) {
                            ContextManager.getTourSerieMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getTourSerieMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                            listGameSerie.add((TourSerieGame)game);
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE) {
                            ContextManager.getSerieTopChallengeMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getSerieTopChallengeMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                            listGameSerieTopChallenge.add((SerieTopChallengeGame) game);
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE) {
                            ContextManager.getSerieEasyChallengeMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getSerieEasyChallengeMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                            listGameSerieEasyChallenge.add((SerieEasyChallengeGame) game);
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TRAINING) {
                            ContextManager.getTrainingMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getTrainingMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                            listGameTraining.add((TrainingGame)game);
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TIMEZONE) {
                            ContextManager.getTimezoneMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getTimezoneMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                            listGameTZ.add((TimezoneGame) game);
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_DUEL) {
                            ContextManager.getDuelMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getDuelMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                            listGameDuel.add((DuelGame)game);
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_TEAM) {
                            ContextManager.getTourTeamMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getTourTeamMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                            listGameTeam.add((TeamGame)game);
                        }
                        else if (game.getTournament().getCategory() == Constantes.TOURNAMENT_CATEGORY_LEARNING) {
                            ContextManager.getTourLearningMgr().getGameMgr().removeSpreadPlay(game.getIDStr());
                            ContextManager.getTourLearningMgr().getGameMgr().removePlayerRunning(game.getPlayerID());
                            listGameLearning.add((LearningGame) game);
                        }
                        else {
                            ITournamentMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(game.getTournament().getCategory());
                            if (tournamentMgr != null) {
                                tournamentMgr.getGameMgr().removeSpreadPlay(game.getIDStr());
                                tournamentMgr.getGameMgr().removePlayerRunning(game.getPlayerID());
                                if(mapGamesForCategory.get(game.getTournament().getCategory()) == null){
                                    mapGamesForCategory.put(game.getTournament().getCategory(), new ArrayList<>());
                                }
                                mapGamesForCategory.get(game.getTournament().getCategory()).add(game);
                            } else {
                                log.error("Game category not supported to save game ! - category=" + Constantes.tourCategory2Name(game.getTournament().getCategory()) + " (" + game.getTournament().getCategory() + ")");
                            }
                        }
                    }
                }

                duelMgr.disableMatchMakingForPlayer(fbs.getPlayer().getID());

                // create connection history
                PlayerConnectionHistory2 temp = playerMgr.createConnectionHistory2(fbs, System.currentTimeMillis(), false);
                listConHist.add(temp);

                // Add deals played
                playerMgr.addDealsPlayed(fbs.getPlayer().getID(), fbs.getMapCategoryPlay());
			}
			
			try {
				if (listGameTraining.size() > 0) {
                    ContextManager.getTrainingMgr().updateListGameDB(listGameTraining);
				}
                if (listGameTZ.size() > 0) {
                    ContextManager.getTimezoneMgr().updateListGameDB(listGameTZ);
                }
                if (listGameDuel.size() > 0) {
                    ContextManager.getDuelMgr().updateListGameDB(listGameDuel);
                }
				if (listGameSerie.size() > 0) {
                    ContextManager.getTourSerieMgr().updateListGameDB(listGameSerie);
                }
                if (listGameSerieTopChallenge.size() > 0) {
                    ContextManager.getSerieTopChallengeMgr().updateListGameDB(listGameSerieTopChallenge);
                }
                if (listGameSerieEasyChallenge.size() > 0) {
                    ContextManager.getSerieEasyChallengeMgr().updateListGameDB(listGameSerieEasyChallenge);
                }
                if (listGameTeam.size() > 0) {
                    ContextManager.getTourTeamMgr().updateListGameDB(listGameTeam);
                }
                if (listGameLearning.size() > 0) {
                    ContextManager.getTourLearningMgr().updateListGameDB(listGameLearning);
                }
                for (int category : mapGamesForCategory.keySet()) {
                    TournamentGenericMgr tournamentMgr = ContextManager.getTournamentMgrForCategory(category);
                    if (tournamentMgr != null) {
                        tournamentMgr.updateListGameDB(mapGamesForCategory.get(category));
                    }
                }
				if (listConHist.size() > 0) {
                    playerMgr.saveDBListConnectionHistory2(listConHist);
				}
			} catch (Exception e) {
				log.error("Exception to update data !", e);
			}
			
			if (!isServiceMaintenance() && isEventFriendConnectedEnable()) {
				MessageNotifMgr notifMgr = ContextManager.getMessageNotifMgr();
				// send event for friend
				for (Session s : listSession) {
					FBSession fbs = (FBSession)s;
                    // get friends for player
                    List<Player> listFriend = playerMgr.listFriendForPlayer(fbs.getPlayer().getID());
                    if (listFriend != null && listFriend.size() > 0) {
                        for (Player plaFriend : listFriend) {
                            // check friend is connected
                            FBSession sessionFriend = getSessionForPlayerID(plaFriend.getID());
                            if (sessionFriend != null) {
                                // send event to friend (recipient is friend !)
                                sessionFriend.pushEvent(notifMgr.buildEventFriendConnected(plaFriend, fbs.getPlayer(), false));
                            }
                        }
                    }
				}
			}
		}
	}

    /**
     * return the list of game for category present in the session list
     * @param category
     * @param removeGameFromSession
     * @return
     */
	public <T extends Game> List<T> getListGameInSessionsForCategory(int category, boolean removeGameFromSession) {
        List<Session> listSession = sessionMgr.getAllCurrentSession();
        List<T> listGame = new ArrayList<>();
        if (listSession != null && listSession.size() > 0) {
            for (Session s : listSession) {
                FBSession fbs = (FBSession) s;
                if (fbs.getCurrentGameTable() != null && fbs.getCurrentGameTable().getGame() != null && !fbs.getCurrentGameTable().getGame().isReplay()) {
                    Game game = fbs.getCurrentGameTable().getGame();
                    if (game.getTournament().getCategory() == category) {
                        listGame.add((T)game);
                        if (removeGameFromSession) {
                            fbs.removeGame();
                            fbs.setCurrentGameTable(null);
                        }
                    }
                }
            }
        }
        return listGame;
    }

	/**
	 * Update the player object in all session by loading data from DB.
	 */
	public void updateAllPlayerSession() {
		synchronized (sessionMgr) {
			List<Session> listSession = sessionMgr.getAllCurrentSession();
			for (Session s : listSession) {
				if (s instanceof FBSession) {
					FBSession fbs = (FBSession)s;
					fbs.setPlayer(playerMgr.getPlayer(fbs.getPlayer().getID()));
				}
			}
		}
	}
	
	/**
	 * Update the player object in session for this playerID
	 * @param playerID
	 */
	public void updatePlayerSession(long playerID) {
		synchronized (sessionMgr) {
			Player p = playerMgr.getPlayer(playerID);
			if (p != null) {
				FBSession fbs = (FBSession)sessionMgr.getSessionForLogin(p.getMail());
				if (fbs != null) {
					fbs.setPlayer(p);
				}
			} else {
				log.error("No player found for playerID="+playerID);
			}
		}
	}
	
	/**
	 * Return session for player.
	 * @param p
	 * @return null if player not connected or current session
	 */
	public FBSession getSessionForPlayer(Player p) {
		if (p != null) {
			return getSessionForPlayerID(p.getID());
		}
		return null;
	}
	
	/**
	 * Check if there is a session for this player
	 * @param p
	 * @return
	 */
	public boolean isSessionForPlayer(Player p) {
		if (p != null) {
		    if (p.getID() == Constantes.PLAYER_ARGINE_ID) {
		        return false;
            }
            return sessionMgr.getSessionForLogin(p.getMail()) != null;
        }
	    return false;
	}
	
	/**
	 * Check if there is a session for this playerID
	 * @param playerID
	 * @return
	 */
	public boolean isSessionForPlayerID(long playerID) {
	    if (playerID == Constantes.PLAYER_ARGINE_ID) {
	        return true;
        }
		return sessionMgr.getSessionForLoginID(playerID) != null;
	}
	
	/**
	 * Return the session for player with ID
	 * @param playerID
	 * @return
	 */
	public FBSession getSessionForPlayerID(long playerID) {
		if (playerID == Constantes.PLAYER_ARGINE_ID) {
		    return null;
        }
	    return (FBSession)sessionMgr.getSessionForLoginID(playerID);
	}
	
	/**
	 * Build contextInfo objet for this session
	 * @param session
	 * @return
	 */
	public WSContextInfo getContextInfo(FBSession session) {
		WSContextInfo ctxInfo = null;
		if (session != null) {
			ctxInfo = new WSContextInfo();
			ctxInfo.nbPlayerConnected = getNbSession();
			ctxInfo.playerSerie = tourSerieMgr.buildWSSerie(session.getPlayerCache());
            ctxInfo.nbNewMessage = session.getNbNewMessage();
            // GENERAL EVENT FREQUENCY
			ctxInfo.frequencyEventFast = FBConfiguration.getInstance().getDoubleValue("general.eventFrequencyFast", Constantes.EVENT_FRQUENCY_FAST);
			ctxInfo.frequencyEventMedium = FBConfiguration.getInstance().getDoubleValue("general.eventFrequencyMedium", Constantes.EVENT_FRQUENCY_MEDIUM);
			ctxInfo.frequencyEventSlow = FBConfiguration.getInstance().getDoubleValue("general.eventFrequencySlow", Constantes.EVENT_FRQUENCY_SLOW);
			// CLAIM FUNCTION ACTIVE
            ctxInfo.enableClaim = FBConfiguration.getInstance().getIntValue("general.enableClaim", 0) == 1;
			ctxInfo.serverParam = buildServerParameters(session);
			
			// communauty data
			ctxInfo.nbActivePlayers = playerMgr.getCommunityNbActivePlayers();
			ctxInfo.nbCountryCode = playerMgr.getCommunityNbCountryCode();
			ctxInfo.nbDuelCommunity = ContextManager.getDuelMgr().getMemoryMgr().countDuelTournament();
		}
		return ctxInfo;
	}
	
	public WSServerParameters buildServerParameters(FBSession session) {
		WSServerParameters param = new WSServerParameters();
		param.duelRequestDuration = (int)(ContextManager.getDuelMgr().getDuelRequestDuration() / ((long)60*60*1000));
		param.webSocketsEnabled = FBConfiguration.getInstance().getIntValue("general.webSocketsEnable", 1) == 1;
		param.httpsWebSockets = FBConfiguration.getInstance().getIntValue("general.httpsWebSockets", 1) == 1;
		if (session.isRpcEnabled()) {
            String[] arrayServices = FBConfiguration.getInstance().getStringValue("general.rpcServices", "").split(";");
            if (arrayServices.length > 0) {
                for (String cmd : arrayServices) {
                    if (cmd.length() > 0) {
                        param.rpcServices.add(cmd);
                    }
                }
            }
        }
		return param;
	}
	
	public void updateMaxSessionInvalidate() {
		int nbMaxSessionInvalidate = FBConfiguration.getInstance().getIntValue("presence.nbMaxSessionInvalidate", 0);
		sessionMgr.setMaxSessionInvalide(nbMaxSessionInvalidate);
	}
	
	public int getMaxSessionInvalidate() {
		return sessionMgr.getMaxSessionInvalide();
	}
	
	public boolean isEventFriendConnectedEnable() {
		return FBConfiguration.getInstance().getIntValue("presence.eventFriendConnected", 0) == 1;
	}


    /**
     * send SMS code
     */
	public boolean sendSMSCode(String phone){

        String Url = "http://106.ihuyi.com/webservice/sms.php?method=Submit";
        String APII = "C82921887" ;
        String APIKEY = "78773ceebd96b42cd7f8f9a265e625c7" ;

        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(Url);
        client.getParams().setContentCharset("UTF-8");
        method.setRequestHeader("ContentType","application/x-www-form-urlencoded;charset=GBK");

        Jedis jedis = RedisUtils.getJedis() ;

        int mobile_code = (int)((Math.random()*9+1)*100000);

        //save code to redis
        jedis.set(phone, String.valueOf(mobile_code));

        if(jedis.get(phone) == null || jedis.get(phone).equals("") ){
            log.error("redis---code save fail,phone:{},code:{}",phone,mobile_code);
            RedisUtils.returnResource(jedis);
            return false ;
        }

        //set code effective time
        jedis.expire(phone,Constantes.SMSCODE_VALID_TIME);

        String content = new String("您的验证码是：" + mobile_code + "。请不要把验证码泄露给其他人。");

        NameValuePair[] data = {
                new NameValuePair("account", APII),
                new NameValuePair("password", APIKEY),
                new NameValuePair("mobile", phone),
                new NameValuePair("content", content),
        };
        method.setRequestBody(data);

        RedisUtils.returnResource(jedis);
        try {
            client.executeMethod(method);

            String SubmitResult =method.getResponseBodyAsString();

            Document doc = DocumentHelper.parseText(SubmitResult);
            Element root = doc.getRootElement();

            String code = root.elementText("code");


            if("2".equals(code)){

                return true ;
            }
            log.error("The SMS Code send fail , code：{},root:{}",code,root);
        } catch (HttpException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

	    return false;
    }

    /**
     * wx get info
     */
    public WXAccessToken getAccessToken(String code) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("appid=%s", Constantes.APPID));
        sb.append(String.format("&secret=%s", Constantes.SECRET));
        sb.append(String.format("&code=%s", code));
        sb.append(String.format("&grant_type=%s", "authorization_code"));
        sb.append(String.format("&connect_redirect=%d", 1));

        String url = String.format("%s?%s", Constantes.ACCESSTOKEN_URL, sb.toString());
        String result = clientPool.sendGet(url);
        log.info("pjh--1");

        if (null == result || result.isEmpty()){
            WXAccessToken wxAccessToken = new WXAccessToken();
            wxAccessToken.setErrcode(-100);
            wxAccessToken.setErrmsg("连接服务器超时!");
            return wxAccessToken;
        }
        if (log.isDebugEnabled()){
            log.debug("URL:{} result:{}", url, result);
        }
        return jsonTools.mapData(result,WXAccessToken.class);
    }

    public WXUserInfo getUserInfo(String accessToken, String openId) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("access_token=%s", accessToken));
        sb.append(String.format("&openid=%s", openId));
        sb.append(String.format("&lang=%s", "zh_CN"));

        String url = String.format("%s?%s", Constantes.USERINFO_URL, sb.toString());
        String result = clientPool.sendGet(url);

        log.info("pjh--2");

        if (null == result || result.isEmpty()){
            WXUserInfo wxUserInfo = new WXUserInfo();
            wxUserInfo.setErrcode(-100);
            wxUserInfo.setErrmsg("连接服务器超时!");
            return wxUserInfo;
        }
        if (log.isDebugEnabled()) {
            log.debug("URL:{} result:{}", url, result);
        }
        return jsonTools.mapData(result,WXUserInfo.class);
    }
}
