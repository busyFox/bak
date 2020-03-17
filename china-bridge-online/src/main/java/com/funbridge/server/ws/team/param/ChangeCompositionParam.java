package com.funbridge.server.ws.team.param;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.team.data.TeamPlayer;

import java.util.List;

/**
 * Created by pserent on 19/10/2016.
 */
public class ChangeCompositionParam {
    public List<TeamPlayer> players;

    public String toString() {
        String result = "";
        for (TeamPlayer p : players) {
            result += "playerID=" + p.getPlayerID() + " substitute=" + p.isSubstitute() + " - ";
        }
        return result;
    }

    public boolean isValid() {
        if (players.size() < 1) return false;
        return players.size() <= ContextManager.getTeamMgr().getTeamSize();
    }
}
