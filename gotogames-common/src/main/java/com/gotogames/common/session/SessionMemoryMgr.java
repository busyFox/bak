package com.gotogames.common.session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SessionMemoryMgr extends SessionMgr{
	private ConcurrentHashMap<String, Session> mapSession = new ConcurrentHashMap<String, Session>();
	private ConcurrentHashMap<String, String> mapLogin = new ConcurrentHashMap<String, String>();
	private ConcurrentHashMap<Long, String> mapLoginID = new ConcurrentHashMap<Long, String>();
	private Logger log = LogManager.getLogger(this.getClass());
	
	public SessionMemoryMgr(String name) {
		super(name);
	}
	
	@Override
	protected void cleanData() {
		log.info("Clean mapLogin && mapSession");
		mapLogin.clear();
		mapLoginID.clear();
		mapSession.clear();
	}
	
	@Override
	public boolean deleteSession(String sessionID) {
		Session session = mapSession.remove(sessionID);
		if (session != null) {
			log.info("deleteSession - sessionID:"+session.getID()+" - login:"+session.getLogin()+" - loginID:"+session.getLoginID());
			invalidate(session);
			mapLogin.remove(session.getLogin());
			mapLoginID.remove(session.getLoginID());
			return true;
		}
		return false;
	}

	@Override
	public boolean deleteAllSession() {
		List<Session> listSession = new ArrayList<Session>();
		for (Iterator<Session> it = mapSession.values().iterator(); it.hasNext();) {
			Session session = it.next();
			listSession.add(session);
//			invalidate(session);
			mapLogin.remove(session.getLogin());
			mapLoginID.remove(session.getLoginID());
			it.remove();
		}
		if (listSession.size() > 0) {
			invalidateList(listSession);
		}
		return true;
	}
	
	@Override
	public Session getSession(String sessionID) {
		return mapSession.get(sessionID);
	}

	@Override
	public boolean putSession(Session session) {
		if (session != null) {
			mapSession.put(session.getID(), session);
			mapLogin.put(session.getLogin(), session.getID());
			mapLoginID.put(session.getLoginID(), session.getID());
			return true;
		}
		return false;
	}

	@Override
	public Session getSessionForLogin(String login) {
		String sessionID = mapLogin.get(login);
		if (sessionID != null) {
			return mapSession.get(sessionID);
		}
		return null;
	}
	
	@Override
	public Session getSessionForLoginID(long loginID) {
		String sessionID = mapLoginID.get(loginID);
		if (sessionID != null) {
			return mapSession.get(sessionID);
		}
		return null;
	}

    @Override
    public Session getSessionForLoginID(String loginID) {
        return null;
    }

    @Override
	public boolean isSessionExist(String sessionID) {
		return mapSession.containsKey(sessionID);
	}

	@Override
	protected int invalidateSession() {
        log.info("Begin - nbSession="+mapSession.size());
		long currentTS = Calendar.getInstance().getTimeInMillis();
		int nbSessionInvalidated = 0;
		List<Session> listSessionInvalidate = new ArrayList<Session>();
        try {
            for (Iterator<Session> it = mapSession.values().iterator(); it.hasNext(); ) {
                Session session = it.next();
                if (session.getTimeout() > 0 && ((session.getDateLastActivity() + session.getTimeout()) < currentTS)) {
                    log.info("timeout : " + session.getTimeout() + " - sessionID:" + session.getID() + " - login:" + session.getLogin() + " - loginID:" + session.getLoginID());
//				invalidate(session);
                    listSessionInvalidate.add(session);
                    nbSessionInvalidated++;
                    mapLogin.remove(session.getLogin());
                    mapLoginID.remove(session.getLoginID());
                    it.remove();
                }
                if (nbMaxSessionInvalide > 0) {
                    if (nbSessionInvalidated >= nbMaxSessionInvalide) {
                        log.error("nbMaxInvalid reached nbMax=" + nbMaxSessionInvalide + " - stop loop");
                        break;
                    }
                }
            }

            if (listSessionInvalidate.size() > 0) {
                invalidateList(listSessionInvalidate);
            }
        } catch (Exception e) {
            log.error("Failed to invalidateSession", e);
        }
        log.info("End - Nb sesion invalidated="+nbSessionInvalidated+" - nbSession="+mapSession.size());
		return nbSessionInvalidated;
	}

	@Override
	public List<Session> getAllCurrentSession() {
		ArrayList<Session> listSession = new ArrayList<Session>();
		listSession.addAll(mapSession.values());
		return listSession;
	}

	@Override
	public int getNbSession() {
		return mapSession.size();
	}

	
}
