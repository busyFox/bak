package com.funbridge.server.engine;

public class EngineRestGetResultParam {
	public String deal;
	public String game;
	public String conventions;
	public String options;
	public int requestType;
    public boolean useCache = true;
    public String asyncID = null;
    public int nbTricksForClaim = 0;
    public String claimPlayer = null;

	public String toString() {
		return "deal=" + deal + " - game=" + game + " - conventions=" + conventions + " - options=" + options + " - requestType=" + requestType + " - useCache=" + useCache + " - asyncID=" + asyncID + " - nbTricksForClaim=" + nbTricksForClaim + " - claimPlayer=" + claimPlayer;
	}
}
