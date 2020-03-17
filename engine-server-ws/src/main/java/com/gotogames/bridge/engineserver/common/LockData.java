package com.gotogames.bridge.engineserver.common;

public class LockData {
	private int countUser = 0;
	private String dataValue = "";
	private long timestamp = 0;
	
	public LockData(long value) {
		dataValue = ""+value;
		timestamp = System.currentTimeMillis();
	}
	
	public LockData(String value) {
		dataValue = value;
		timestamp = System.currentTimeMillis();
	}
	
	public String getDataValue() {
		return dataValue;
	}
	
	protected void incrementCount() {
		countUser++;
	}
	
	protected void decrementCount() {
		countUser--;
	}
	
	public int getCount(){
		return countUser;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
}
