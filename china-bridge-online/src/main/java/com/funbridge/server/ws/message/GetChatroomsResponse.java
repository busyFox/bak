package com.funbridge.server.ws.message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bplays on 07/11/16.
 */
public class GetChatroomsResponse {
    public List<WSChatroom> chatrooms = new ArrayList<>();
    public int totalSize;
    public int offset;
}
