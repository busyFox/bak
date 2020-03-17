package com.funbridge.server.ws.result;

import java.util.ArrayList;
import java.util.List;

import com.funbridge.server.ws.tournament.WSTournament;

public class WSResultDealTournament {
	public List<WSResultDeal> listResultDeal = new ArrayList<>();
	public List<WSResultAttribut> attributes = new ArrayList<WSResultAttribut>();
	public WSTournament tournament;
	public int offset;
	public int totalSize;
}
