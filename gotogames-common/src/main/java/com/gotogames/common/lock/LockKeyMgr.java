package com.gotogames.common.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Get lock on transcient object
 * @author pserent
 *
 * @param <KEY>
 */
public class LockKeyMgr<KEY> {
//	private final WeakHashMap<LockKey<KEY>, WeakReference<LockKey<KEY>>> mapWeak = new WeakHashMap<LockKey<KEY>, WeakReference<LockKey<KEY>>>();
	private ConcurrentHashMap<KEY, ReentrantLock> lockMap = new ConcurrentHashMap<KEY, ReentrantLock>();
	
	public void lockKey(KEY k) {
		ReentrantLock lock = new ReentrantLock();
		ReentrantLock currentlock = lockMap.putIfAbsent(k, lock);
		if (currentlock != null) {
			lock = currentlock;
		}
		lock.lock();
	}
	
	public void unlockKey(KEY k) {
		ReentrantLock lock = lockMap.get(k);
		if (lock != null) {
			lock.unlock();
		}
	}
	
	
//	/**
//	 * Return a object to lock on (synchronize) for the given key
//	 * @param key
//	 * @return
//	 */
//	public synchronized Object getLock(KEY key) {
//		if (key == null) {
//			throw new NullPointerException();
//		}
//		
//		LockKey<KEY> newkey = new LockKey<KEY>(key);
//		WeakReference<LockKey<KEY>> ref = mapWeak.get(newkey);
//		if (ref == null) {
//			ref = new WeakReference<LockKey<KEY>>(newkey);
//			mapWeak.put(newkey, ref);
//		}
//		return ref;
//	}
//	
//	/**
//	 * return map of synchronized object
//	 * @return
//	 */
//	public synchronized int size() {
//		return mapWeak.size();
//	}
//	
//	/**
//	 * clear map of synchronized object
//	 */
//	public synchronized void clear() {
//		mapWeak.clear();
//	}
}
