package com.gotogames.bridge.engineserver.request;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class RequestWaitingMgr {
	
	private Logger log = LogManager.getLogger(this.getClass());
	private ConcurrentHashMap<Long, RequestWaitingResult> requestWaiting = new ConcurrentHashMap<Long, RequestWaitingResult>();
	private long index = 0;
	
	public void destroy() {
		log.info("Clear map of request waiting");
		requestWaiting.clear();
	}
	
	/**
	 * Return the next index
	 * @return
	 */
	private synchronized long getNextIndex() {
		index++;
		return index;
	}
	
	/**
	 * Return the current index value
	 * @return
	 */
	public long getCurrentIndex() {
		return index;
	}
	
	/**
	 * add a request waiting
	 * @return
	 */
	public RequestWaitingResult addRequestWaiting() {
		RequestWaitingResult rwr = new RequestWaitingResult();
		rwr.setDateCreation(System.currentTimeMillis());
		rwr.setID(getNextIndex());
		requestWaiting.put(rwr.getID(), rwr);
		log.debug("Add request waiting : "+rwr.toString());
		return rwr;
	}
	
	/**
	 * retrieve the request waiting with this ID
	 * @param requestWaitingID
	 * @return null if the object with this ID is not found
	 */
	public RequestWaitingResult getRequestWaiting(long requestWaitingID) {
		return requestWaiting.get(requestWaitingID);
	}
	
	/**
	 * Remove the object with this ID
	 * @param requestWaitingID
	 */
	public void removeRequestWaiting(long requestWaitingID) {
		RequestWaitingResult rwr = requestWaiting.remove(requestWaitingID);
		if (rwr != null) {
			log.debug("remove request waiting : "+rwr.toString());
		} else {
			log.debug("No request waiting found for ID="+requestWaitingID);
		}
	}
}
