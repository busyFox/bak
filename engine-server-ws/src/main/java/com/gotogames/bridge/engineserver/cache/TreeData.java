package com.gotogames.bridge.engineserver.cache;

import com.gotogames.bridge.engineserver.common.Constantes;

public class TreeData {
	private String deal;
	private String game;
	private String conventions;
	private String options;
	private int requestType=-1;
	
	public TreeData(String d, String o, String c, String g, int t) {
		deal = d; options = o; conventions = c; game = g; requestType = t;
	}
	public String getDeal() {
		return deal;
	}
	public String getGame() {
		return game;
	}
	public String getConventions() {
		return conventions;
	}
	public String getOptions() {
		return options;
	}
	public String getTreeDealKey() {
		return deal+options;
	}
	public String getCompleteKey() {
        return deal+options+conventions+game;
    }

	public void appendGameResult(String val) {
		game+=val;
	}
	public int getRequestType() {
		return requestType;
	}
	public boolean isRequestBidInfo() {
		return requestType == Constantes.REQUEST_TYPE_BID_INFO;
	}
	public boolean isRequestBidCard() {
		return requestType == Constantes.REQUEST_TYPE_CARD || requestType == Constantes.REQUEST_TYPE_BID;
	}
	public String toString() {
		return deal+";"+options+";"+conventions+";"+game+";"+requestType;
	}
}
