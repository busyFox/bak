package com.gotogames.common.session;

import com.gotogames.common.tools.StringTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class SessionMgr {
	/**
	 * Clean all data  - to call when terminated application
	 */
	protected abstract void cleanData();
	
	/**
	 * Add session
	 * @param session
	 * @return
	 */
	public abstract boolean putSession(Session session);
	
	/**
	 * Return the session assocatied to this ID
	 * @param sessionID
	 * @return
	 */
	public abstract Session getSession(String sessionID);
	
	/**
	 * Delete all current session
	 * @return
	 */
	public abstract boolean deleteAllSession();
	
	/**
	 * Delete the session assocatied to this ID
	 * @param sessionID
	 * @return true if session exist and deleted with success
	 */
	public abstract boolean deleteSession(String sessionID);
	
	/**
	 * Return the session assocatied to this login
	 * @param login
	 * @return
	 */
	public abstract Session getSessionForLogin(String login);
	
	/**
	 * Return the session assocatied to this loginID
	 * @param loginID
	 * @return
	 */
	public abstract Session getSessionForLoginID(long loginID);
    public abstract Session getSessionForLoginID(String loginID);
	
	/**
	 * Check if a session with this ID exists
	 * @param sessionID
	 * @return
	 */
	public abstract boolean isSessionExist(String sessionID);
	
	/**
	 * Return the list of session currently managed
	 * @return
	 */
	public abstract List<Session> getAllCurrentSession();
	
	/**
	 * Return the number of session
	 * @return
	 */
	public abstract int getNbSession();
	
	/**
	 * Callback method used by the clean scheduler to invalidate session with no activity since timeout of session
	 * @return
	 */
	protected abstract int invalidateSession();
	
	protected SessionListener sessionListener;
	private final ScheduledExecutorService schedulerCleanSession = Executors.newScheduledThreadPool(1);
	private Logger log = LogManager.getLogger(this.getClass());
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private String name;
	public static final int SESSION_TYPE_MEMORY = 0, SESSION_TYPE_MEMORY2 = 1;
	protected int nbMaxSessionInvalide = 0;
	protected int nbMaxDailySession = 0;
	protected Calendar lastUpdateDailySession = Calendar.getInstance();

	/**
	 * create a manager from type for session management
	 * @param type int value (SESSION_TYPE_MEMORY ...)
	 * @return
	 */
	public static SessionMgr createSessionMgr(String mgrName, int type) {
		if (type == SESSION_TYPE_MEMORY) {
			return new SessionMemoryMgr(mgrName);
		}
        if (type == SESSION_TYPE_MEMORY2) {
            return new SessionMemoryMgr2(mgrName);
        }
		return null;
	}
	
	protected SessionMgr(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setSessionListener(SessionListener listener) {
		this.sessionListener = listener;
	}
	
	/**
	 * Clean data in memory and stop clean scheduler
	 */
	public void destroy() {
		log.debug("Destroy on SessionMgr name="+name);
		stopCleanScheduler();
		cleanData();
	}
	
	/**
	 * Stop the clean scheduler 
	 */
	public void stopCleanScheduler() {
		log.debug("SessionMgr name="+name+" - Shutdown clean scheduler");
		schedulerCleanSession.shutdown();
		try {
			if (schedulerCleanSession.awaitTermination(60, TimeUnit.SECONDS)) {
				schedulerCleanSession.shutdownNow();
			}
		} catch (InterruptedException e) {
			schedulerCleanSession.shutdownNow();
		}
	}
	
	/**
	 * Start a timer thread to clean session with no activity since timeout value
	 * @param timeout nb of seconds for timeout session
	 * @param period nb of seconds between two execution of clean task
	 */
	public void startCleanScheduler(int period) {
		log.debug("SessionMgr name="+name+" - Start clean scheduler : period(seconds)="+period);
		schedulerCleanSession.scheduleWithFixedDelay(new SessionCleanTask(this), 0, period, TimeUnit.SECONDS);
	}
	
	private class SessionCleanTask implements Runnable {
		private SessionMgr sessionMgr;
		
		public SessionCleanTask(SessionMgr mgr) {
			this.sessionMgr = mgr;
		}
		
		@Override
		public void run() {
            log.info("Run task ... begin");
			if (sessionMgr != null) {
				sessionMgr.invalidateSession();
			}
            log.info("Run task ... end");
		}
	}
	
	/**
	 * The session will be invalidate (delete)
	 * @param session
	 */
	protected void invalidate(Session session) {
		if (sessionListener != null) {
			sessionListener.invalidateSession(session);
		}
	}

	/**
	 * All session of list are invalidate
	 * @param listSession
	 */
	protected void invalidateList(List<Session> listSession) {
		if (sessionListener != null) {
			sessionListener.invalidateListSession(listSession);
		}
	}
	
	/**
	 * Update the date of last activity for the session
	 * @param sessionID
	 */
	public void touchSession(String sessionID) {
		Session s = getSession(sessionID);
		if (s != null) {
			s.setDateLastActivity(Calendar.getInstance().getTimeInMillis());
		}
	}
	
	public void setMaxSessionInvalide(int value) {
		this.nbMaxSessionInvalide = value;
	}
	
	public int getMaxSessionInvalide() {
		return this.nbMaxSessionInvalide;
	}
	
	/**
	 * Generate a sessionID for this login
	 * current date + ":" + random string (length 10 char) 
	 * @param login
	 * @return sessionID in hexa format
	 */
	private static String generateSessionID(String login) {
		String curDate = sdf.format(Calendar.getInstance().getTime());
		String sessionID = curDate + ":"
				+ StringTools.generateRandomString(10);
		return StringTools.strToHexa(sessionID);
	}
	
	/**
	 * Create session with login, ID and timeout
	 * @param login
	 * @param loginID
	 * @param timeout value in second
	 * @return
	 */
	public static Session createSession(String login, long loginID, int timeout) {
		Session s = null;
		if (login != null) {
			s = new Session();
			s.setLogin(login);
			s.setLoginID(loginID);
			s.setTimeout(timeout * 1000);
			s.setID(generateSessionID(login));
			long curTime = Calendar.getInstance().getTimeInMillis();
			s.setDateCreation(curTime);
			s.setDateLastActivity(curTime);
		}
		return s;
	}

    /**
     * Create session with login, ID and timeout
     * @param login
     * @param loginID
     * @param timeout value in second
     * @return
     */
    public static Session createSession(String login, String loginID, int timeout) {
        Session s = null;
        if (login != null) {
            s = new Session();
            s.setLogin(login);
            s.setLoginIDstr(loginID);
            s.setTimeout(timeout * 1000);
            s.setID(generateSessionID(login));
            long curTime = Calendar.getInstance().getTimeInMillis();
            s.setDateCreation(curTime);
            s.setDateLastActivity(curTime);
        }
        return s;
    }

	public int getNbMaxDailySession() { return nbMaxDailySession; }

	public void setNbMaxDailySession(int nbMaxDailySession, Calendar cal) {
		this.nbMaxDailySession = nbMaxDailySession;
		this.lastUpdateDailySession = cal;
	}

	public void initDailyDate(Calendar cal){
    	if(cal.get(Calendar.DATE) != this.lastUpdateDailySession.get(Calendar.DATE)){
    		this.nbMaxDailySession = 0;
		}
	}
}
