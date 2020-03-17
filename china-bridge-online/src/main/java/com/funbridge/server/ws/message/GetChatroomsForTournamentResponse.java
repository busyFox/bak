package com.funbridge.server.ws.message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bplays on 07/11/16.
 */
public class GetChatroomsForTournamentResponse {
    public List<WSChatroom> chatrooms = new ArrayList<>();
    public boolean canModerate;
}
