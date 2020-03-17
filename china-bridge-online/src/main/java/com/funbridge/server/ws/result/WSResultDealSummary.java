package com.funbridge.server.ws.result;

import com.funbridge.server.ws.message.WSChatroom;

import java.util.ArrayList;
import java.util.List;

public class WSResultDealSummary {
	public String gameID;
    public List<WSResultDeal> mostPlayedContracts = new ArrayList<>();
    public int indexPlayerResult;
    public WSResultDealTournament result;
    public String cardPlay = "";
    public String par = "";
    public String analysis = "";
    public int nbPlayers = 0;
    public int nbContracts = 0;
    public WSChatroom chatroom;
}
