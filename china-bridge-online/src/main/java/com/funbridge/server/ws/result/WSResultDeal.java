package com.funbridge.server.ws.result;


public class WSResultDeal {
    private long dealID = 0;
    private String dealIDstr = "";
    private int dealIndex = 0;
    private int rank = 0;
    private double result = 0;
    private int resultType = 0;
    private String contract = "";
    private String declarer = "";
    private int nbTricks = 0;
    private int score = 0;
    private int nbPlayerSameGame = 0;
    private int nbTotalPlayer = 0;
    private boolean played = false;
    private long gameID = -1; // used to review game
    private String gameIDstr = ""; // used to review game
    private String playerPseudo = "";
    private long playerID = -1;
    private boolean guest = false;
    private boolean avatarPresent = false;
    private String countryCode = "";
    private boolean connected = false;
    private String lead = "";
    private int nbUnreadMessages = 0;

    public long getDealID() {
        return dealID;
    }

    public void setDealID(long dealID) {
        this.dealID = dealID;
        this.dealIDstr = "" + dealID;
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

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getDeclarer() {
        return declarer;
    }

    public void setDeclarer(String declarer) {
        this.declarer = declarer;
    }

    public int getNbTricks() {
        return nbTricks;
    }

    public void setNbTricks(int nbTricks) {
        this.nbTricks = nbTricks;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getNbPlayerSameGame() {
        return nbPlayerSameGame;
    }

    public void setNbPlayerSameGame(int nbPlayer) {
        this.nbPlayerSameGame = nbPlayer;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }

    public int getDealIndex() {
        return dealIndex;
    }

    public void setDealIndex(int dealIndex) {
        this.dealIndex = dealIndex;
    }

    public int getNbTotalPlayer() {
        return nbTotalPlayer;
    }

    public void setNbTotalPlayer(int nbTotalPlayer) {
        this.nbTotalPlayer = nbTotalPlayer;
    }

    public String toString() {
        return rank + " - " + result + " - " + contract + " - " + score + " - " + nbPlayerSameGame;
    }

    public boolean isPlayed() {
        return played;
    }

    public void setPlayed(boolean played) {
        this.played = played;
    }

    public long getGameID() {
        return gameID;
    }

    public void setGameID(long gameID) {
        this.gameID = gameID;
        this.gameIDstr = "" + gameID;
    }

    public String getPlayerPseudo() {
        return playerPseudo;
    }

    public void setPlayerPseudo(String playerPseudo) {
        this.playerPseudo = playerPseudo;
    }

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public boolean isAvatarPresent() {
        return avatarPresent;
    }

    public void setAvatarPresent(boolean avatarPresent) {
        this.avatarPresent = avatarPresent;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getDealIDstr() {
        return dealIDstr;
    }

    public void setDealIDstr(String dealIDstr) {
        this.dealIDstr = dealIDstr;
    }

    public String getGameIDstr() {
        return gameIDstr;
    }

    public void setGameIDstr(String gameIDstr) {
        this.gameIDstr = gameIDstr;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getLead() {
        return lead;
    }

    public void setLead(String lead) {
        this.lead = lead;
    }

    public int getNbUnreadMessages() {
        return nbUnreadMessages;
    }

    public void setNbUnreadMessages(int nbUnreadMessages) {
        this.nbUnreadMessages = nbUnreadMessages;
    }
}
