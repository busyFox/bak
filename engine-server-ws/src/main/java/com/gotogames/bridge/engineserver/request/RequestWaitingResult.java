package com.gotogames.bridge.engineserver.request;

public class RequestWaitingResult {
	private String resultValue = "";
	private long dateCreation = 0;
	private long ID = 0;
	
	public String getResultValue() {
		return resultValue;
	}
	public void setResultValue(String resultValue) {
		this.resultValue = resultValue;
	}
	public long getDateCreation() {
		return dateCreation;
	}
	public void setDateCreation(long dateCreation) {
		this.dateCreation = dateCreation;
	}
	public long getID() {
		return ID;
	}
	public void setID(long iD) {
		ID = iD;
	}
	public String toString() {
		return "ID="+ID+" - dateCreation="+dateCreation+" - result="+resultValue;
	}
}
