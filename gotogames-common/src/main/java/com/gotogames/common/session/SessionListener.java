package com.gotogames.common.session;

import java.util.List;

public interface SessionListener {
	/**
	 * Invalidate a session
	 * @param session
	 */
	void invalidateSession(Session session);
	
	/**
	 * Invalidate a list of session
	 * @param listSession
	 */
	void invalidateListSession(List<Session> listSession);
}
