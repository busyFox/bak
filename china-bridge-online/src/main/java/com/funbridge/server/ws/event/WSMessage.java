package com.funbridge.server.ws.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public class WSMessage {
	public String ID = "";
	public boolean read = false;
	public int displayMask = 0;
	public int priority = 0;
	public int category = 0;
	public long senderID;
	public String senderPseudo;
	public boolean senderAvatar = false;
	public long dateReceive;
	public long dateExpiration;
	public String body = "";
	public List<WSMessageExtraField> extraFields;
	@JsonIgnore
	public boolean send = false;

	@JsonIgnore
	public String toString() {
		return "ID=" + ID + " - category=" + category + " - read=" + read + " - displayMask=" + displayMask + " - senderID=" + senderID + " - senderPseudo=" + senderPseudo + " - senderAvatar=" + senderAvatar + " - body=" + body;
	}

}
