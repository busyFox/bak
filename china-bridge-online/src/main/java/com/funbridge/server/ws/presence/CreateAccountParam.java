package com.funbridge.server.ws.presence;

public class CreateAccountParam {
	public String pseudo;
	public String mail;
	public String password;
	public String lang;
	public String displayLang;
	public String country;
	public String clientVersion;
	public String deviceID;
	public String deviceInfo;
	public String deviceType;

	public String toString() {
		return "pseudo="+pseudo+" - mail="+mail+" - password="+password+"lang="+lang+" - clientVersion="+clientVersion+" - deviceID="+deviceID+" - deviceInfo="+deviceInfo+" - deviceType="+deviceType;
	}

	public boolean isValid() {
		if (pseudo == null) {
			return false;
		}
		if (mail == null) {
			return false;
		}
		if (password == null) {
			return false;
		}
		if (deviceID == null || deviceID.length() == 0) {
			return false;
		}
		return deviceType != null && deviceType.length() != 0;
	}
}
