package com.funbridge.server.ws.event;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "messageCount")
public class MessageCountResult {
	public int mask;
	public int count;

	public String toString() {
		return "{mask=" + mask + " - count=" + count + "}";
	}
}
