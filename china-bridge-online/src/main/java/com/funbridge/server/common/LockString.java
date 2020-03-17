package com.funbridge.server.common;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class LockString {
	private final WeakHashMap<KeyWrapper, WeakReference<KeyWrapper>> mapWeak = new WeakHashMap<KeyWrapper, WeakReference<KeyWrapper>>();
	private Object mutex = new Object();
	
	private class KeyWrapper {
		private String key;
		public KeyWrapper(String k) {
			this.key = k;
		}
		
		public boolean equals(Object o) {
			if(o==null) {return false;}
			if(this.getClass()==o.getClass()) {
				return this.key.equals(o.toString());
			}
			return false;
		}
		
		public int hashCode() {
			return key.hashCode();
		}
		
		public String toString() {
			return key;
		}
	}
	
	public Object getLock(String key) {
		synchronized (mutex) {
			if (key == null || key.length() == 0) {
				//throw new Exception("Key is null or empty");
				return null;
			}
			KeyWrapper newkey = new KeyWrapper(key);
			WeakReference<KeyWrapper> ref = mapWeak.get(newkey);
			if (ref == null) {
				ref = new WeakReference<KeyWrapper>(newkey);
				mapWeak.put(newkey, ref);
			}
			return ref;
		}
	}
	
	public synchronized int size() {
		return mapWeak.size();
	}
	
	public void destroy() {
		mapWeak.clear();
	}
}
