package com.funbridge.server.ws.team.response;

import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import com.funbridge.server.ws.team.WSTeamResult;

import java.util.List;

/**
 * Created by bplays on 28/11/16.
 */
public class GetRankingResponse {
    public List<WSTeamResult> teamRanking;
    public WSTeamResult resultTeam;
    public List<WSResultTournamentPlayer> playerRanking;
    public WSResultTournamentPlayer resultPlayer;
    public int offset;
    public int totalSize;
}
