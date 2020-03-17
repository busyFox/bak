package com.funbridge.server.ws.team.response;

import com.funbridge.server.ws.team.WSTeamLight;

import java.util.List;

/**
 * Created by pserent on 29/11/2016.
 */
public class ListTeamsResponse {
    public List<WSTeamLight> teams;
    public int totalSize = 0;
    public int offset = 0;
}
