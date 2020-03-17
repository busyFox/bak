package com.gotogames.common.lock;

public class LockKey<KEY> {
	private KEY key;
	private int hashCode;
	
	public LockKey(KEY k) {
		this.key = k;
	}
	
	public boolean equals(Object o) {
		if(o==null) {return false;}
		if (o instanceof LockKey) {
			return ((LockKey) o).key.equals(key);
		}
		return false;
	}
	
	public int hashCode() {
		return hashCode;
	}
	
	public String toString() {
		return key.toString();
	}
}
