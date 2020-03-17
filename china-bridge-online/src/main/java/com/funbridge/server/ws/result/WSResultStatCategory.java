package com.funbridge.server.ws.result;

public class WSResultStatCategory {
	private String listResult = "";
	private double computeResult = 0;
	private int nbPlayed = 0;
	private int nbPositive = 0;
	
	public String getListResult() {
		return listResult;
	}
	public void setListResult(String listResult) {
		this.listResult = listResult;
	}
	public double getComputeResult() {
		return computeResult;
	}
	public void setComputeResult(double computeResult) {
		this.computeResult = computeResult;
	}
	public int getNbPlayed() {
		return nbPlayed;
	}
	public void setNbPlayed(int nbPlayed) {
		this.nbPlayed = nbPlayed;
	}
	public void incrementNbPlayed() {
		nbPlayed++;
	}
	public void appendListResult(double res) {
		if (listResult.length() > 0) {
			listResult += ";";
		}
		listResult+=res;
	}
	public int getNbPositive() {
		return nbPositive;
	}
	public void setNbPositive(int nbPositive) {
		this.nbPositive = nbPositive;
	}
	
}
