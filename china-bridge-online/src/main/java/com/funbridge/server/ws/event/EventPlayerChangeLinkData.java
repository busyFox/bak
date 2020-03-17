package com.funbridge.server.ws.event;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "eventPlayerChangeLink")
public class EventPlayerChangeLinkData {
    public long playerID;
    public int linkMask;
}
