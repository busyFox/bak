package com.funbridge.server.ws.message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bplays on 07/11/16.
 */
public class GetMessagesResponse {
    public List<WSChatMessage> messages = new ArrayList<>();
    public int totalSize = 0;
    public int offset = 0;
}
