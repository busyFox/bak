package com.gotogames.common.session;

public class Session {
	private String ID;
	private long dateCreation;
	private long dateLastActivity;
	private String login;
	private long loginID;
    private String loginIDstr;
	private int timeout = -1;
	
	public String getID() {
		return ID;
	}
	public void setID(String iD) {
		ID = iD;
	}
	public long getDateCreation() {
		return dateCreation;
	}
	public void setDateCreation(long dateCreation) {
		this.dateCreation = dateCreation;
	}
	public long getDateLastActivity() {
		return dateLastActivity;
	}
	public void setDateLastActivity(long dateLastActivity) {
		this.dateLastActivity = dateLastActivity;
	}
	public String getLogin() {
		return login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	public long getLoginID() {
		return loginID;
	}
	public void setLoginID(long loginID) {
		this.loginID = loginID;
	}

    public String getLoginIDstr() {
        return loginIDstr;
    }

    public void setLoginIDstr(String loginIDstr) {
        this.loginIDstr = loginIDstr;
    }

	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
}
