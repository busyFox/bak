package com.funbridge.server.ws.message;

import com.funbridge.server.ws.player.WSPlayerLight;

/**
 * Created by bplays on 07/11/16.
 */
public class WSChatMessage {
    public String ID;
    public long date;
    public String body;
    public String mediaID;
    public String mediaSize;
    public String type;
    public WSPlayerLight author;
    public boolean read;
    public String chatroomID;
    public String gameID; // Useful to view a player's game directly from the deal chatroom
    public WSChatMessage quotedMessage;
    public String tempID;
}
