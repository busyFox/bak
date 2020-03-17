package com.funbridge.server.ws.result;

public class WSResultAllForPlayer {
	private String listResult = "";
	private double sumResult = 0;
	private int nbDealPlayed = 0;
	private int nbResultPositive = 0;
	public String getListResult() {
		return listResult;
	}
	public void setListResult(String listResult) {
		this.listResult = listResult;
	}
	public double getSumResult() {
		return sumResult;
	}
	public void setSumResult(double sumResult) {
		this.sumResult = sumResult;
	}
	public int getNbDealPlayed() {
		return nbDealPlayed;
	}
	public void setNbDealPlayed(int nbDealPlayed) {
		this.nbDealPlayed = nbDealPlayed;
	}
	public int getNbResultPositive() {
		return nbResultPositive;
	}
	public void setNbResultPositive(int nbResultPositive) {
		this.nbResultPositive = nbResultPositive;
	}
	
}
