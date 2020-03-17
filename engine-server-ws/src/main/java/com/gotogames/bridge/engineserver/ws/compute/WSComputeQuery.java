package com.gotogames.bridge.engineserver.ws.compute;

import com.gotogames.common.tools.StringTools;

public class WSComputeQuery {
	private long computeID = -1;
	private String deal = "";
	private String game = "";
	private String conventions = "";
	private String options = "";
	private int queryType = 0;
	private int pollingValue = 0;
	private int nbTricksForClaim = 0;
	private String claimPlayer = null;
	
	public long getComputeID() {
		return computeID;
	}
	public void setComputeID(long computeID) {
		this.computeID = computeID;
	}
	public String getDeal() {
		return deal;
	}
	public void setDeal(String deal) {
		this.deal = deal;
	}
	public String getGame() {
		return game;
	}
	public void setGame(String game) {
		this.game = game;
	}
	public String getConventions() {
		return conventions;
	}
	public void setConventions(String conventions) {
		this.conventions = conventions;
	}
	public int getQueryType() {
		return queryType;
	}
	public void setQueryType(int queryType) {
		this.queryType = queryType;
	}
	public String getOptions() {
		return options;
	}
	public void setOptions(String options) {
		this.options = options;
	}
	public void appendPollingValue(int value) {
		this.options += StringTools.byte2String(new byte[]{(byte)(value & 0xFF)});
	}
	public int getPollingValue() {
		return pollingValue;
	}
	public void setPollingValue(int pollingValue) {
		this.pollingValue = pollingValue;
	}

    public int getNbTricksForClaim() {
        return nbTricksForClaim;
    }

    public void setNbTricksForClaim(int nbTricksForClaim) {
        this.nbTricksForClaim = nbTricksForClaim;
    }

    public String getClaimPlayer() {
        return claimPlayer;
    }

    public void setClaimPlayer(String claimPlayer) {
        this.claimPlayer = claimPlayer;
    }

    public String toString() {
		return "computeID="+computeID+" - deal="+deal+" - game="+game+" - conv="+conventions+" - options="+options+" - queryType="+queryType+" - pollingValue="+pollingValue+" - nbTricksForClaim="+nbTricksForClaim+" - claimPlayer="+claimPlayer;
	}
}
