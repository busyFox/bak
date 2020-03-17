package com.funbridge.server.ws.message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 08/03/2017.
 */
public class SendMessageToChatroomResponse {
    public WSChatMessage message;
    public List<WSChatMessage> unreadMessages = new ArrayList<>();
}
