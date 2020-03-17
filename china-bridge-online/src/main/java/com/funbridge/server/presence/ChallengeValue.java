package com.funbridge.server.presence;

public class ChallengeValue {
	public String login;
	public long deviceID;
	public String clientVersion;
	public long creationDate;
	
	public String toString() {
		return "login="+login+" - deviceID="+deviceID+" - date="+creationDate;
	}
}
