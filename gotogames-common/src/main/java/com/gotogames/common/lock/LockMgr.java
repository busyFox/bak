package com.gotogames.common.lock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class LockMgr {
	private Logger log = LogManager.getLogger(this.getClass());
	private ConcurrentHashMap<String, LockData> mapLockDataKeyString;
	private ConcurrentHashMap<Long, LockData> mapLockDataKeyID;
	
	public LockMgr() {
		mapLockDataKeyString = new ConcurrentHashMap<String, LockData>();
		mapLockDataKeyID = new ConcurrentHashMap<Long, LockData>();
	}
	
	public void destroy() {
		mapLockDataKeyString.clear();
		mapLockDataKeyID.clear();
	}
	
	/**
	 * Get an object for string key to lock. Call it just before a synchronized section. 
	 * If at the same time a thread already works request, synchronize with it using this object
	 * @param dataKey
	 * @return LockData object associated to this key if already exists, otherwise a new object. The count number is incremented
	 */
	public LockData getLockDataKeyString(String dataKey) {
		LockData data = mapLockDataKeyString.get(dataKey);
		if (data == null) {
			// data for request not existing ...
			LockData newData = new LockData(dataKey);
			data = mapLockDataKeyString.putIfAbsent(dataKey, newData);
			if (data == null) {
				data = newData;
			}
		}
		synchronized (data) {
			data.incrementCount();
		}
		return data;
	}
	
	/**
	 * Indicate the end of synchronized section on this lock object for string key. 
	 * The count user is decremented and if count = 0, data is removed and set to null. 
	 * DO NOT USE data object after this method.
	 * @param data
	 * @param dataKey
	 */
	public void endLockDataKeyString(LockData data, String dataKey) {
		if (data != null && dataKey != null) {
			synchronized (data) {
				data.decrementCount();
				if (data.getCount() == 0) {
					mapLockDataKeyString.remove(dataKey);
					data = null;
				}
			}
		} else if (dataKey != null){
			log.error("data is null and not dataKey : remove dataKey from map");
			mapLockDataKeyString.remove(dataKey);
		} else {
			log.error("data and dataKey null !");
		}
	}
	
	/**
	 * Get an object for ID key (request long format) to lock. Call it just before a synchronized section. 
	 * If at the same time a thread already works request, synchronize with it using this object
	 * @param dataKey
	 * @return LockData object associated to this key if already exists, otherwise a new object. The count number is incremented
	 */
	public LockData getLockDataKeyID(long dataKey) {
		LockData data = mapLockDataKeyID.get(dataKey);
		if (data == null) {
			// data for ID not existing ...
			LockData newData = new LockData(dataKey);
			data = mapLockDataKeyID.putIfAbsent(dataKey, newData);
			if (data == null) {
				data = newData;
			}
		}
		synchronized (data) {
			data.incrementCount();
		}
		return data;
	}
	
	/**
	 * Indicate the end of synchronized section on this lock object for ID key (request long format). 
	 * The count user is decremented and if count = 0, data is removed and set to null. 
	 * DO NOT USE data object after this method.
	 * @param data
	 * @param dataKey
	 */
	public void endLockDataKeyID(LockData data, long dataKey) {
		if (data != null) {
			synchronized (data) {
				data.decrementCount();
				if (data.getCount() == 0) {
					mapLockDataKeyID.remove(dataKey);
					data = null;
				}
			}
		} else {
			log.error("data is null : remove dataKey from map");
			mapLockDataKeyString.remove(dataKey);
		}
	}
	
	/**
	 * Return the list of lock data for key ID
	 * @return
	 */
	public List<LockData> getLockDataKeyIDList(){
		List<LockData> listData = new ArrayList<LockData>(mapLockDataKeyID.values());
		return listData;
	}
	
	/**
	 * Return the list of lock data for key Request
	 * @return
	 */
	public List<LockData> getLockDataKeyStringList(){
		List<LockData> listData = new ArrayList<LockData>(mapLockDataKeyString.values());
		return listData;
	}
	
	/**
	 * Remove a lock data of key ID
	 * @param dataKey
	 */
	public void removeLockDataKeyID(long dataKey) {
		mapLockDataKeyID.remove(dataKey);
	}
	
	/**
	 * Remove a lock data of key Request
	 * @param dataKey
	 */
	public void removeLockDataKeyString(String dataKey) {
		mapLockDataKeyString.remove(dataKey);
	}
}
