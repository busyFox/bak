package com.funbridge.server.player.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="inactiveDevice")
public class InactiveDevice {
	public String deviceToken;
	public long tsDate;
}
