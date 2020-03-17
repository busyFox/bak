package com.funbridge.server.ws.presence;

import com.funbridge.server.ws.presence.PresenceServiceRest.LoginTrialParam;


/**
 * Data connection setting sent at the login process.
 * @author pserent
 *
 */
public class ConnectionData {
	public String clientVersion;
	public String deviceID;
	public String deviceInfo;
	public String deviceType;
	public String lang;
	public String displayLang;
	public String country;
    public String protocol;

    public ConnectionData() {}

	public ConnectionData(LoginData loginData) {
		this.clientVersion = loginData.clientVersion;
		this.deviceID = loginData.deviceID;
		this.deviceInfo = loginData.deviceInfo;
		this.deviceType = loginData.deviceType;
		this.lang = loginData.lang;
		this.displayLang = loginData.displayLang;
		this.country = loginData.country;
        this.protocol = loginData.protocol;
	}

	public ConnectionData(WXLoginData wxLoginData) {
		this.clientVersion = wxLoginData.clientVersion;
		this.deviceID = wxLoginData.deviceID;
		this.deviceInfo = wxLoginData.deviceInfo;
		this.deviceType = wxLoginData.deviceType;
		this.lang = wxLoginData.lang;
		this.displayLang = wxLoginData.displayLang;
		this.country = wxLoginData.country;
		this.protocol = wxLoginData.protocol;
	}
	
	public ConnectionData(LoginTrialParam loginData) {
		this.clientVersion = loginData.clientVersion;
		this.deviceID = loginData.deviceID;
		this.deviceType = loginData.deviceType;
		this.lang = loginData.lang;
		this.displayLang = loginData.displayLang;
	}
	
	public boolean isValid() {
		if (deviceID == null || deviceID.length() == 0) {
			return false;
		}
        return deviceType != null && deviceType.length() != 0;
    }
	
	public String toString() {
		return "clientVersion="+clientVersion+" - deviceID="+deviceID+" - deviceInfo="+deviceInfo+" - devicetype="+deviceType+" - lang="+lang+" - country="+country+" - protocol="+protocol;
	}
}
