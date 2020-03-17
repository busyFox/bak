package com.funbridge.server.ws.result;

import java.util.ArrayList;
import java.util.List;

public class WSResultReplayDealSummary {
	private WSResultDeal resultPlayer;
	private WSResultDeal resultReplay;
	private WSResultDeal resultMostPlayed;
	private List<WSResultAttribut> attributes = new ArrayList<WSResultAttribut>();
	
	public WSResultDeal getResultPlayer() {
		return resultPlayer;
	}
	public void setResultPlayer(WSResultDeal resultPlayer) {
		this.resultPlayer = resultPlayer;
	}
	public WSResultDeal getResultReplay() {
		return resultReplay;
	}
	public void setResultReplay(WSResultDeal resultReplay) {
		this.resultReplay = resultReplay;
	}
	public WSResultDeal getResultMostPlayed() {
		return resultMostPlayed;
	}
	public void setResultMostPlayed(WSResultDeal resultMostPlayed) {
		this.resultMostPlayed = resultMostPlayed;
	}
	public List<WSResultAttribut> getAttributes() {
		return attributes;
	}
	public void setAttributes(List<WSResultAttribut> attributes) {
		this.attributes = attributes;
	}
}
