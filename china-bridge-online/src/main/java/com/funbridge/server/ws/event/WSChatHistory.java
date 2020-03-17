package com.funbridge.server.ws.event;

/**
 * Created by pserent on 25/05/2015.
 */
public class WSChatHistory {
    public long playerID;
    public String pseudo;
    public boolean avatar;
    public boolean connected;
    public String lastMessageText;
    public long lastMessageDate;
    public boolean lastMessageRead;
    public String countryCode;
}
