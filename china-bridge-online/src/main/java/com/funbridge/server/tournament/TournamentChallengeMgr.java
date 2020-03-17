package com.funbridge.server.tournament;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.presence.FilterEvent;
import com.funbridge.server.message.MessageMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.tournament.dao.TournamentChallengeDAO;
import com.funbridge.server.tournament.data.TournamentChallenge;
import com.funbridge.server.tournament.data.TournamentSettings;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;
import com.funbridge.server.ws.event.Event;
import com.gotogames.common.lock.LockData;
import com.gotogames.common.lock.LockMgr;

@Component(value="tournamentChallengeMgr")
@Scope(value="singleton")
public class TournamentChallengeMgr extends FunbridgeMgr {
	@Resource(name="tournamentChallengeDAO")
	private TournamentChallengeDAO tournamentChallengeDAO = null;
	@Resource(name="presenceMgr")
	private PresenceMgr presenceMgr = null;
	@Resource(name="playerMgr")
	private PlayerMgr playerMgr = null;
	@Resource(name="messageMgr")
	private MessageMgr messageMgr = null;
	@Resource(name="tournamentMgr")
	private TournamentMgr tournamentMgr = null;
	
	private ConcurrentHashMap<Long, TournamentChallenge> mapChallenge = new ConcurrentHashMap<Long, TournamentChallenge>();
	private ScheduledExecutorService schedulerPurgeChallenge = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> schedulerPurgeChallengeFuture = null;
	private PurgeTournamentChallengeTask purgeChallengeTask = null;
	private LockMgr createChallengeLockMgr = new LockMgr();
	private long dateLastPurge = 0;
	
	/**
	 * Task to purge challenge
	 * @author pascal
	 *
	 */
	public class PurgeTournamentChallengeTask extends TimerTask {
		@Override
		public void run() {
			ContextManager.getTournamentChallengeMgr().purgeChallenge();
		}
	}
	
	/**
	 * Call by spring on initialisation of bean
	 */
	@PostConstruct
	@Override
	public void init() {
	}
	
	@PreDestroy
	@Override
	public void destroy() {
		stopScheduler(schedulerPurgeChallenge);
		mapChallenge.clear();
	}
	
	@Override
	public void startUp() {
		loadAllChallenge();
		purgeChallengeTask = new PurgeTournamentChallengeTask();
		schedulerPurgeChallengeFuture = schedulerPurgeChallenge.scheduleWithFixedDelay(purgeChallengeTask, 5, 10, TimeUnit.MINUTES);
        if (log.isDebugEnabled()) {
            log.debug("Schedule purge challenge - next run at " + Constantes.getStringDateForNextDelayScheduler(schedulerPurgeChallengeFuture) + " - period (minutes)=10");
        }
	}
	
	/**
	 * Add challenge in DB
	 * @param tc
	 * @throws FBWSException
	 */
	@Transactional
	public void persistChallenge(TournamentChallenge tc) throws FBWSException {
		if (!tournamentChallengeDAO.addChallenge(tc)) {
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Update challenge data in DB
	 * @param tc
	 * @throws FBWSException
	 */
	@Transactional
	public void updateChallenge(TournamentChallenge tc) throws FBWSException {
		if (tc == null) {
			log.error("TournamentChallenge param is null !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		TournamentChallenge tcDB = tournamentChallengeDAO.getForID(tc.getID());
		if (tcDB == null) {
			log.error("No tournament challenge found in DB tc="+tc);
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		tcDB.setDateExpiration(tc.getDateExpiration());
		tcDB.setSettings(tc.getSettings());
		tcDB.setStatus(tc.getStatus());
		tcDB.setTable(tc.getTable());
		if (tournamentChallengeDAO.updateChallenge(tcDB) == null) {
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Load all challenge in map
	 */
	private void loadAllChallenge() {
		synchronized (mapChallenge) {
			mapChallenge.clear();
			List<TournamentChallenge> listChallenge = tournamentChallengeDAO.listAll();
			for (TournamentChallenge tc : listChallenge) {
				tc.setDateLastStatusChange(System.currentTimeMillis());
				mapChallenge.put(tc.getID(), tc);
			}
			log.info("Load all challenge size="+mapChallenge.size());
		}
	}
	
	/**
	 * Return the challenge key associated to the 2 players
	 * @param plaID1
	 * @param plaID2
	 * @return
	 */
	private String getChallengeKey(long plaID1, long plaID2) {
		String plKey = "cha-";
		if (plaID1 < plaID2) {
			plKey += plaID1+"-"+plaID2;
		} else {
			plKey += plaID2+"-"+plaID1;
		}
		return plKey;
	}
	
	/**
	 * loop on map challenge to remove ended items. It is the only way to remove challenge from map
	 */
	public void purgeChallenge() {
		long currentTime = System.currentTimeMillis();
		dateLastPurge = currentTime;
		List<Long> listToDelete = new ArrayList<Long>();
		synchronized (mapChallenge) {
			for (Iterator<Entry<Long, TournamentChallenge>> it = mapChallenge.entrySet().iterator(); it.hasNext();) {
				Entry<Long, TournamentChallenge> e = it.next();
				if (e.getValue().isEnded()) {
					// check date last status change is earlier than 30 min
					if ((e.getValue().getDateLastStatusChange() < currentTime) &&
						((currentTime - e.getValue().getDateLastStatusChange()) > (30*60*1000))) {
						listToDelete.add(e.getValue().getID());
						it.remove();
					}
				}
			}
		}
		if (listToDelete.size() > 0) {
			boolean deleteResult = ContextManager.getTournamentChallengeMgr().deleteListChallenge(listToDelete);
            if (log.isDebugEnabled()) {
                log.debug("Delete challenge : " + listToDelete.size() + " - result=" + deleteResult);
            }
		}
	}
	
	/**
	 * Do the job to delete challenge with ID in the list
	 * @param listToDelete
	 * @return
	 */
	@Transactional
	public boolean deleteListChallenge(List<Long> listToDelete) {
		return tournamentChallengeDAO.deleteTournamentChallenge(listToDelete);
	}
	
	/**
	 * Return the current challenger not expired between two players
	 * @param player1
	 * @param player2
	 * @return
	 */
	public TournamentChallenge getCurrentChallengeNotExpiredForPlayers(long player1, long player2) {
		List<TournamentChallenge> listTC = tournamentChallengeDAO.getNotExpiredForPlayers(player1, player2);
		for (TournamentChallenge temp : listTC) {
			if (!temp.isEnded()) {
				return temp;
			}
		}
		return null;
	}
	
	/**
	 * Create a challenge between players. Send event to partner.
	 * @param creator
	 * @param partner
	 * @param settings
	 * @return
	 * @throws FBWSException
	 */
	public TournamentChallenge createChallenge(Player creator, Player partner, String settings) throws FBWSException {
		if (creator != null && partner != null) {
			// check players are friend !
			if (!playerMgr.isPlayerFriend(creator.getID(), partner.getID())) {
				log.error("creator and partner not friend ! creator="+creator+" - partner="+partner);
				throw new FBWSException(FBExceptionType.PLAYER_NOT_FRIEND);
			}
			
			// check player credit
			playerMgr.checkPlayerCredit(creator, 1);
			try {
				playerMgr.checkPlayerCredit(partner, 1);
			} catch (FBWSException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Partner has not enougth credit ! partner=" + partner);
                }
				throw new FBWSException(FBExceptionType.GAME_PARTNER_CREDIT_NOT_ENOUGH);
			}
			
			// check partner connected
			FBSession sessionPartner = presenceMgr.getSessionForPlayerID(partner.getID());
			if (sessionPartner == null) {
                if (log.isDebugEnabled()) {
                    log.debug("No session found for partner=" + partner.getID());
                }
				throw new FBWSException(FBExceptionType.PLAYER_NOT_CONNECTED);
			}
			sessionPartner.setDateLastActivity(System.currentTimeMillis());
			
			TournamentChallenge tc = null;
			String chaKey = getChallengeKey(creator.getID(), partner.getID());
			LockData lock = null;
			// synchro to be sure only one challenge at a time
			synchronized (lock = createChallengeLockMgr.getLockDataKeyString(chaKey)) {
				try {
					// get existing challenge
					List<TournamentChallenge> listTC = tournamentChallengeDAO.getNotExpiredForPlayers(creator.getID(), partner.getID());
					for (TournamentChallenge temp : listTC) {
						if (!temp.isEnded()) {
							tc=temp;
							break;
						}
					}
					if (tc == null || tc.isEnded()) {
						// check settings
						TournamentSettings ts = tournamentMgr.getTournamentSettings(settings);
						if (ts == null || !ts.isValid()) {
							log.error("Settins are not valid - settings="+settings);
							throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
						}
						if (tc != null) {
							log.debug("TournamentChallenge is ended tc="+tc);
						}
						int timerExpiration = FBConfiguration.getInstance().getIntValue("tournament.TRAINING_PARTNER.timerExpiration", 5*60);
						synchronized (mapChallenge) {
							// create challenge
							tc = new TournamentChallenge();
							tc.setCreator(creator);
							tc.setPartner(partner);
							tc.setDateCreation(System.currentTimeMillis());
							tc.setDateExpiration(System.currentTimeMillis() + timerExpiration*1000);
							tc.setSettings(settings);
							tc.setStatus(Constantes.TOURNAMENT_CHALLENGE_STATUS_INIT);
							ContextManager.getTournamentChallengeMgr().persistChallenge(tc);
							mapChallenge.put(tc.getID(), tc);
						}
					} else {
						log.error("Challenge already exist tc="+tc);
						throw new FBWSException(FBExceptionType.TOURNAMENT_CHALLENGE_ALREADY_EXIST);
					}
					// send event to partner
					Event evt = messageMgr.buildEventChallengeRequest(tc, false);
					if (evt == null) {
						log.error("Error to build event challenge !");
						throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
					} else {
						sessionPartner.pushEvent(evt);
					}
				} finally {
					createChallengeLockMgr.endLockDataKeyString(lock, chaKey);
				}
			}// end synchro this
			return tc;
		} else {
			log.error("Parameter null : creator="+creator+" - partner="+partner);
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}
	}

	/**
	 * Set player response for challenge
	 * @param player
	 * @param challengeID
	 * @param response
	 * @throws FBWSException
	 */
	public void setChallengeResponse(Player player, long challengeID, 	boolean response) throws FBWSException {
		// check challenge exist
		TournamentChallenge tc = mapChallenge.get(challengeID);
		if (tc == null) {
			log.error("No challenge with challengeID="+challengeID);
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		if (tc.isEnded()) {
			log.error("Challenge has expired ! tc="+tc);
			throw new FBWSException(FBExceptionType.TOURNAMENT_CHALLENGE_EXPIRATION);
		}
		
		// check player is partner
		if (tc.getPartner().getID() != player.getID()) {
			log.error("Player is not partner of challenge ! challenge="+tc+" - player="+player.getID());
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}

		// change challenge status
		if (response) {
			playerMgr.checkPlayerCredit(player, 1);
			tc.setStatus(Constantes.TOURNAMENT_CHALLENGE_STATUS_WAITING);
		} else {
			tc.setStatus(Constantes.TOURNAMENT_CHALLENGE_STATUS_END);
		}
		
		// get session from challenge creator
		FBSession sessionCreator = presenceMgr.getSessionForPlayerID(tc.getCreator().getID());
		if (sessionCreator == null) {
			log.error("No session found for creator="+tc.getCreator().getID());
			throw new FBWSException(FBExceptionType.PLAYER_NOT_CONNECTED);
		}
		
		// update challenge in DB
		ContextManager.getTournamentChallengeMgr().updateChallenge(tc);
		
		// send event to creator challenge
		Event evt = messageMgr.buildEventChallengeResponse(tc.getPartner(), tc.getCreator(), tc, response);
		if (evt == null) {
			log.error("Error to build event challenge !");
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		} else {
			sessionCreator.pushEvent(evt);
		}
	}
	
	/**
	 * Return list of challenge where player is creator or partner (from DB)
	 * @param playerID
	 * @return
	 */
	public List<TournamentChallenge> getChallengeForPlayer(long playerID) {
		return tournamentChallengeDAO.listForPlayer(playerID);
	}
	
	/**
	 * Return challenge with ID (from map)
	 * @param tourChallengeID
	 * @return
	 */
	public TournamentChallenge getChallengeForID(long tourChallengeID) {
		return mapChallenge.get(tourChallengeID);
	}
	
	/**
	 * Update challenge status : get it from map, update status value and update it to DB
	 * @param challengeID
	 * @param status
	 * @return
	 */
	@Transactional
	public boolean updateChallengeStatus(long challengeID, int status) {
		TournamentChallenge tc = null;
		boolean bResult = false;
		tc = mapChallenge.get(challengeID);
		if (tc == null) {
			log.error("No challenge found with ID="+challengeID);
		} else {
			tc.setStatus(status);
			try {
				updateChallenge(tc);
				bResult= true;
			} catch (FBWSException e) {
				log.error("Error to update challenge ! tc="+tc, e);
			}
		}
		return bResult;
	}
	
	/**
	 * Retrieve the challenge associated, change status and send event to partner
	 * @param challengeID
	 * @param creator
	 * @param partner
	 * @throws FBWSException
	 */
	public TournamentChallenge resetChallenge(long challengeID, Player creator, Player partner) throws FBWSException {
		String chaKey = getChallengeKey(creator.getID(), partner.getID());
		LockData lock = null;
		TournamentChallenge tc = null;
		// synchro to be sure only one challenge at a time
		synchronized (lock = createChallengeLockMgr.getLockDataKeyString(chaKey)) {
			try {
				// check challenge
				tc = mapChallenge.get(challengeID);
				if (tc == null) {
					log.error("No challenge found with ID="+challengeID);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				// check partner connected
				FBSession sessionPartner = presenceMgr.getSessionForPlayerID(partner.getID());
				if (sessionPartner == null) {
					log.error("No session found for partner="+partner);
					throw new FBWSException(FBExceptionType.PLAYER_NOT_CONNECTED);
				}
				sessionPartner.setDateLastActivity(System.currentTimeMillis());
				
				// change challenge status
				int timerExpiration = FBConfiguration.getInstance().getIntValue("tournament.TRAINING_PARTNER.timerExpiration", 5*60);
				tc.setDateCreation(System.currentTimeMillis());
				tc.setDateExpiration(System.currentTimeMillis() + timerExpiration*1000);
				tc.setStatus(Constantes.TOURNAMENT_CHALLENGE_STATUS_INIT);
				try {
					ContextManager.getTournamentChallengeMgr().updateChallenge(tc);
				} catch (Exception e) {
					log.error("Exception to update challenge="+tc);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				}
				
				// purge existing event game on session partner
				FilterEvent filter = new FilterEvent();
				filter.receiverID = sessionPartner.getPlayer().getID();
				filter.category = Constantes.EVENT_CATEGORY_GAME;
				sessionPartner.purgeEvent(filter);
				
				// send event to partner
				Event evt = messageMgr.buildEventChallengeRequest(tc, true);
				if (evt == null) {
					log.error("Error to build event challenge ! tc="+tc);
					throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
				} else {
					sessionPartner.pushEvent(evt);
				}
				
			} finally {
				createChallengeLockMgr.endLockDataKeyString(lock, chaKey);
			}
		}// end synchro this
		return tc;
	}
	
	/**
	 * Return list of challenge waiting response for partner (status = INIT)
	 * @param playerID
	 * @return
	 */
	public List<TournamentChallenge> listChallengeWaitingResponseForPartner(long playerID) {
		List<TournamentChallenge> listReturn = new ArrayList<TournamentChallenge>();
		List<TournamentChallenge> listNotExpired = tournamentChallengeDAO.getNotExpiredForPartner(playerID);
		for (TournamentChallenge tc : listNotExpired) {
			if (!tc.isEnded() && tc.getStatus() == Constantes.TOURNAMENT_CHALLENGE_STATUS_INIT) {
				listReturn.add(tc);
			}
		}
		return listReturn;
	}
	
	/**
	 * Return the map of challenge. Used by admin JSP pages.
	 * @return
	 */
	public Map<Long, TournamentChallenge> getMapChallenge() {
		return mapChallenge;
	}
	
	/**
	 * Return the date of last purge done by task
	 * @return
	 */
	public long getDateLastPurge() {
		return dateLastPurge;
	}
	
	public String getStringDateNextPurgeChallengeScheduler() {
		return Constantes.getStringDateForNextDelayScheduler(schedulerPurgeChallengeFuture);
	}
	
	/**
	 * Delete challenge for players
	 * @param listPlaID
	 */
	@Transactional
	public boolean deleteDataForPlayerList(List<Long> listPlaID) {
		if (listPlaID != null && listPlaID.size() > 0) {
			for (long plaID : listPlaID) {
				List<TournamentChallenge> listChallenge = tournamentChallengeDAO.listForPlayer(plaID);
				if (listChallenge != null && listChallenge.size() > 0) {
					for (TournamentChallenge tc : listChallenge) {
						mapChallenge.remove(tc.getID());
					}
				}
				tournamentChallengeDAO.deleteForPlayer(plaID);
			}
		}
		return true;
	}
}
