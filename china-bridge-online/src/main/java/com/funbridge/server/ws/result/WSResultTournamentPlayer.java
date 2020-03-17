package com.funbridge.server.ws.result;

public class WSResultTournamentPlayer {
	private int nbTotalPlayer = 0;
	private int rank = 0;
	private double result = 0;
	private int nbDealPlayed = 0;
	private long playerID = 0;
	private String playerPseudo = "";
	private long dateLastPlay = 0;
	private String countryCode;
	private String playerSerie;
	private boolean avatarPresent;
	private boolean guest = false;
    private int nbPlayerFinishWithBestResult;
    private int rankHidden = 0;
    private boolean connected = false;
    private double masterPoints = -1;
	private double fbPoints = -1;
	private int teamPoints = -1;
	private String teamID;
	private String teamName;
	private boolean captain = false;
	private String section = "";
	public String getCountryCode() {
		return countryCode;
	}
	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}
	public String getPlayerSerie() {
		return playerSerie;
	}
	public void setPlayerSerie(String playerSerie) {
		this.playerSerie = playerSerie;
	}
	public boolean isAvatarPresent() {
		return avatarPresent;
	}
	public void setAvatarPresent(boolean avatarPresent) {
		this.avatarPresent = avatarPresent;
	}
	public int getNbTotalPlayer() {
		return nbTotalPlayer;
	}
	public void setNbTotalPlayer(int nbTotalPlayer) {
		this.nbTotalPlayer = nbTotalPlayer;
	}
	public int getRank() {
		return rank;
	}
	public void setRank(int rank) {
		this.rank = rank;
	}
	public double getResult() {
		return result;
	}
	public void setResult(double result) {
		this.result = result;
	}
	public int getNbDealPlayed() {
		return nbDealPlayed;
	}
	public void setNbDealPlayed(int nbDealPlayed) {
		this.nbDealPlayed = nbDealPlayed;
	}
	public long getPlayerID() {
		return playerID;
	}
	public void setPlayerID(long playerID) {
		this.playerID = playerID;
	}
	public String getPlayerPseudo() {
		return playerPseudo;
	}
	public void setPlayerPseudo(String playerPseudo) {
		this.playerPseudo = playerPseudo;
	}
	public long getDateLastPlay() {
		return dateLastPlay;
	}
	public void setDateLastPlay(long dateLastPlay) {
		this.dateLastPlay = dateLastPlay;
	}
	public int getNbPlayerFinishWithBestResult() {
        return nbPlayerFinishWithBestResult;
    }

    public void setNbPlayerFinishWithBestResult(int nbPlayerFinishWithBestResult) {
        this.nbPlayerFinishWithBestResult = nbPlayerFinishWithBestResult;
    }

    public int getRankHidden() {
        return rankHidden;
    }

    public void setRankHidden(int rankHidden) {
        this.rankHidden = rankHidden;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public double getMasterPoints() {
        return masterPoints;
    }

    public void setMasterPoints(double masterPoints) {
        this.masterPoints = masterPoints;
    }

	public double getFbPoints() {
		return fbPoints;
	}

	public void setFbPoints(double fbPoints) {
		this.fbPoints = fbPoints;
	}

	public int getTeamPoints() {
		return teamPoints;
	}

	public void setTeamPoints(int teamPoints) {
		this.teamPoints = teamPoints;
	}

	public String getTeamID() {
		return teamID;
	}

	public void setTeamID(String teamID) {
		this.teamID = teamID;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public boolean isCaptain() {
		return captain;
	}

	public void setCaptain(boolean captain) {
		this.captain = captain;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}
}
