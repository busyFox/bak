package com.funbridge.server.ws.message;

import com.funbridge.server.ws.player.WSPlayerLight;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 08/03/2017.
 */
public class GetSuggestedParticipantsResponse {
    public List<WSPlayerLight> players = new ArrayList<>();
}
