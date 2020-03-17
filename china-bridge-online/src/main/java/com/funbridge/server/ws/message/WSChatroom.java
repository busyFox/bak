package com.funbridge.server.ws.message;

import com.funbridge.server.ws.player.WSPlayerLight;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bplays on 07/11/16.
 */
public class WSChatroom {
    public String ID;
    public String name;
    public String nameTemplate;
    public long updateDate;
    public WSChatMessage lastMessage;
    public List<WSPlayerLight> participants = new ArrayList<>();
    public boolean read;
    public int nbMessages;
    public int nbUnreadMessages;
    public boolean muted;
    public String imageID;
    public String type;
    public List<Long> administrators = new ArrayList<>();
    public String lang;
}
