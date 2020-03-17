package com.funbridge.server.ws.event;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "eventFriendConnected")
public class EventFriendConnected {
    public long playerID = -1;
    public boolean connected = false;
    public String msg = "";
}
