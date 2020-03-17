package com.gotogames.bridge.engineserver.user;

import com.gotogames.bridge.engineserver.common.Constantes;

import java.util.concurrent.atomic.AtomicLong;

public abstract class UserVirtual {
	protected long ID;
	protected String login;
    protected String loginPrefix;
    private String password;
    private boolean test = false;
	private String challenge;
	private int timeout = -1; // timeout in seconds (-1 for no timeout)
	private boolean polling = false;
    private boolean useCache = true;
    protected AtomicLong dateLastActivity = new AtomicLong(0);
    protected long dateCreation = 0;

    public UserVirtual(String login, String loginPrefix, String password, long id) {
        this.login = login;
        this.loginPrefix = loginPrefix;
        this.password = password;
        this.ID = id;
        this.dateCreation = System.currentTimeMillis();
    }

	public long getID() {
		return ID;
	}
	public String getPassword() {
		return password;
	}
	public String getLoginID() {
		return login+"-"+ID;
	}
	public String getLoginPrefix() {
		return loginPrefix;
	}
	public String getLogin() {
        return login;
    }
	public abstract boolean isEngine();

	public boolean isTest() {
		return test;
	}
	public void setTest(boolean test) {
		this.test = test;
	}
	public String getChallenge() {
		return challenge;
	}
	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}
	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isPolling() {
		return polling;
	}
	public void setPolling(boolean val) {
		this.polling = val;
	}

    public boolean isUseCache() {
        return useCache;
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public String toString() {
        return "ID="+ID+" - login="+login+" - loginID="+getLoginID()+" - loginPrefix="+loginPrefix+" - useCache="+isUseCache()+" - timeout="+getTimeout()+" - dateCreation="+Constantes.timestamp2StringDateHour(dateCreation);
    }


    public long getDateLastActivity() {
        return dateLastActivity.get();
    }

    public void setDateLastActivity(long dateLastActivity) {
        this.dateLastActivity.set(dateLastActivity);
    }

    public long getDateCreation() {
        return dateCreation;
    }
}
