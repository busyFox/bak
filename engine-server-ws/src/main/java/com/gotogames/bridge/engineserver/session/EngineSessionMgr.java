package com.gotogames.bridge.engineserver.session;

import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import com.gotogames.bridge.engineserver.user.UserVirtual;
import com.gotogames.bridge.engineserver.user.UserVirtualMgr;
import com.gotogames.bridge.engineserver.ws.ServiceException;
import com.gotogames.bridge.engineserver.ws.ServiceExceptionType;
import com.gotogames.common.session.Session;
import com.gotogames.common.session.SessionListener;
import com.gotogames.common.session.SessionMgr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;

@Component(value="sessionMgr")
@Scope(value="singleton")
public class EngineSessionMgr implements SessionListener{
	private SessionMgr sessionMgr;
	private Logger log = LogManager.getLogger(this.getClass());
	private int nbEngineConnected = 0;
	
	@Resource(name="userVirtualMgr")
	private UserVirtualMgr userVirtualMgr;
	
	public EngineSessionMgr() {
		sessionMgr = SessionMgr.createSessionMgr("ENGINE-SERVER", SessionMgr.SESSION_TYPE_MEMORY);
		sessionMgr.setSessionListener(this);
	}
	
	@PreDestroy
	public void destroy() {
		if (sessionMgr != null) {
			sessionMgr.destroy();
		}
        userVirtualMgr.destroy();
	}
	
	@PostConstruct
	public void init() {
		if (userVirtualMgr == null || sessionMgr == null) {
			log.error("Parameters null : userVirtualMgr | sessionMgr");
		} else {
			int cleanSessionPeriod = EngineConfiguration.getInstance().getIntValue("session.cleanSessionPeriod", 30);
			log.warn("Start scheduler to clean session - period="+cleanSessionPeriod);
			sessionMgr.startCleanScheduler(cleanSessionPeriod);
		}
	}
	
//	public void setUserVirtualMgr(UserVirtualMgr userVirtualMgr) {
//		this.userVirtualMgr = userVirtualMgr;
//	}
	
	public void incrementNbEngineConnected() {
		synchronized (this) {
			nbEngineConnected++;
		}
	}
	
	public void decrementNbEngineConnected() {
		synchronized (this) {
			if (nbEngineConnected > 0) {
				nbEngineConnected--;
			}
		}
	}
	
	/**
	 * Return the number of engine currently connected
	 * @return
	 */
	public int getNbEngineConnected() {
		return nbEngineConnected;
	}
	
	@Override
	public void invalidateSession(Session session) {
		if (session != null) {
			synchronized (sessionMgr) {
				UserVirtual u = userVirtualMgr.getUserVirtual(session.getLogin());
				if (u != null) {
					if (u.isEngine()) {
						decrementNbEngineConnected();
					}
                    //userVirtualMgr.removeUser(u, true);
				} else {
				    if (EngineConfiguration.getInstance().getIntValue("general.log.engineSessionMgr.invalidateSession", 1) == 1) {
				        log.warn("No user found for session.getLogin()="+session.getLogin()+" - session="+session);
                    }
                }
                userVirtualMgr.deleteUserVirtual(session.getLogin());
			}
		}
	}

	@Override
	public void invalidateListSession(List<Session> listSession) {
		if (listSession != null && listSession.size() > 0) {
			for (Session s : listSession) {
				UserVirtual u = userVirtualMgr.getUserVirtual(s.getLogin());
				if (u != null) {
					if (u.isEngine()) {
						decrementNbEngineConnected();
					}
//					userVirtualMgr.removeUser(u, true);
	 			} else {
                    if (EngineConfiguration.getInstance().getIntValue("general.log.engineSessionMgr.invalidateSession", 1) == 1) {
                        log.warn("No user found for session.getLogin()="+s.getLogin()+" - session="+s);
                    }
                }
                userVirtualMgr.deleteUserVirtual(s.getLogin());
			}
		}
	}
	
	/**
	 * Check if session exist, so already active !
	 * @param sessionID
	 * @throws ServiceException
	 */
	public void checkSession(String sessionID) throws ServiceException {
		if (!sessionMgr.isSessionExist(sessionID)) {
            if (log.isDebugEnabled()) {
                log.debug("SessionID not found : sessionID=" + sessionID);
            }
			throw new ServiceException(ServiceExceptionType.SESSION_INVALID_SESSION_ID, "SessionID not existing");
		}
	}
	
	/**
	 * Return the session associated to this sessionID
	 * @param sessionID
	 * @return
	 * @throws ServiceException
	 */
	public Session getAndCheckSession(String sessionID) throws ServiceException {
		Session session = getSession(sessionID);
		if (session == null) {
			throw new ServiceException(ServiceExceptionType.SESSION_INVALID_SESSION_ID, "SessionID not existing");
		}
		session.setDateLastActivity(System.currentTimeMillis());
		return session;
	}

	public Session getSession(String sessionID) {
		return sessionMgr.getSession(sessionID);
	}
	
	/**
	 * Set session active
	 * @param sessionID
	 */
	public void touchSession(String sessionID) {
		sessionMgr.touchSession(sessionID);
	}

	/**
	 * Close the session, delete it
	 * @param sessionID
	 * @return
	 */
	public boolean closeSession(String sessionID) {
		Session s = sessionMgr.getSession(sessionID);
		if (s != null) {
			invalidateSession(s);
		}
		
		return sessionMgr.deleteSession(sessionID);
	}

	/**
	 * Close all session : destro all session and all virtual use
	 */
	public void closeAllSession() {
        sessionMgr.deleteAllSession();
        nbEngineConnected = 0;
	}
	
	/**
	 * Check if session exist
	 * @param sessionID
	 * @return
	 */
	public boolean isSessionValid(String sessionID) {
		return sessionMgr.isSessionExist(sessionID);
	}

	/**
	 * Create a session associated to this user
	 * @param u
	 * @return
	 * @throws ServiceException
	 */
	public Session createSession(UserVirtual u) throws ServiceException {
		if (u == null) {
			log.error("error user is null !");
			throw new ServiceException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR);
		}
		synchronized (sessionMgr) {
			Session s = sessionMgr.getSessionForLogin(u.getLoginID());
			if (s != null) {
				sessionMgr.deleteSession(s.getID());
			}
			s = SessionMgr.createSession(u.getLoginID(), u.getID(), u.getTimeout());
			if (s == null) {
				log.error("error creating session for login="+u.getLogin()+" - userID="+u.getID());
				throw new ServiceException(ServiceExceptionType.COMMON_INTERNAL_SERVER_ERROR);
			}
			sessionMgr.putSession(s);
			if (u.isEngine()) {
				incrementNbEngineConnected();
			}
			
			return s;
		}
	}

	
	/**
	 * Check if a user engine is connected
	 * @return
	 */
	public boolean isUserEngineConnected() {
//		List<Session> listSession = sessionMgr.getAllCurrentSession();
//		for (Session s : listSession) {
//			UserVirtual u = userVirtualMgr.getUserVirtual(s.getLogin());
//			if (u != null && u.isEngine()) {
//				return true;
//			}
//		}
//		return false;
		return nbEngineConnected > 0;
	}
	
	/**
	 * Return all current session active
	 * @return
	 */
	public List<Session> getAllCurrentSession() {
		return sessionMgr.getAllCurrentSession();
	}
}
