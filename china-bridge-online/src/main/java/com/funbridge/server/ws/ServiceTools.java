package com.funbridge.server.ws;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.tournament.data.Tournament;
import com.gotogames.common.session.Session;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Calendar;

public class ServiceTools {
	private static Log log = LogFactory.getLog(ServiceTools.class);
	
	/**
	 * Check the sessionID is valid
	 * @param sessionID
	 * @throws FBWSException Exception if sessionID is unknown
	 */
	public static void checkSession(String sessionID) throws FBWSException {
		if (!ContextManager.getPresenceMgr().isSessionValid(sessionID)) {
            if (log.isDebugEnabled()) {
                log.debug("checkSession - SessionID not found : FAILED for sessionID=" + sessionID);
            }
			throw new FBWSException(FBExceptionType.SESSION_INVALID_SESSION_ID);
		}
	}
	
//	public static Fault createFault(FBWSException e) {
//		return new Fault(e);
//	}
	
	/**
	 * Get and check the session object for this ID. If session is valid, touch it (update date last activity)
	 * @param sessionID
	 * @return
	 * @throws FBWSException Exception if session is not found
	 */
	public static FBSession getAndCheckSession(String sessionID) throws FBWSException {
		// check if server is in maintenance
		if (ContextManager.getPresenceMgr().isServiceMaintenance()) {
			throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
		}
		// retrieve session and touch it
		Session session = ContextManager.getPresenceMgr().getSessionAndTouch(sessionID);
		if (session == null) {
			throw new FBWSException(FBExceptionType.SESSION_INVALID_SESSION_ID);
		}
		if (session instanceof FBSession) {
			FBSession fbs = (FBSession) session;
            // check if maintenance for device type
            if (ContextManager.getPresenceMgr().isServiceMaintenanceForDevice(((FBSession) session).getDeviceType())) {
                throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
            }
			if (fbs.getPlayer() == null) {
				log.error("Player is null ! sessionID="+fbs.getLoginID()+" - login="+fbs.getLogin());
				throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
			return fbs;
		} else {
			log.error("No FBSession !! sessionID="+session.getLoginID()+" - login="+session.getLogin());
			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
	}
	
//	/**
//	 * Get and check the player object for this ID
//	 * @param playerID
//	 * @return
//	 * @throws FBWSException
//	 */
//	public static Player getAndCheckPlayer(long playerID) throws FBWSException {
//		Player player = ContextManager.getPlayerMgr().getPlayer(playerID);
//		if (player == null) {
//			throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR, "PLAYER NOT FOUND FOR THIS ID : "+playerID);
//		}
//		return player;
//	}
	
	/**
	 * Get and check the Tournament object for this ID
	 * @param tournamentID
	 * @return
	 * @throws FBWSException
	 */
	public static Tournament getAndCheckTournament(long tournamentID) throws FBWSException {
		Tournament tour = ContextManager.getTournamentMgr().getTournament(tournamentID);
		if (tour == null) {
			throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
		}
		if (tour.isFinished()) {
            if (log.isDebugEnabled()) {
                log.debug("TOURNAMENT IS CLOSED ! - tour=" + tour);
            }
			throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
		}
		// check current date is valid for tournament
		if (!tour.isDateValid(Calendar.getInstance().getTimeInMillis())) {
            if (log.isDebugEnabled()) {
                log.debug("TOURNAMENT IS CLOSED ! - tour=" + tour);
            }
			throw new FBWSException(FBExceptionType.TOURNAMENT_CLOSED);
		}
		return tour;
	}
}
