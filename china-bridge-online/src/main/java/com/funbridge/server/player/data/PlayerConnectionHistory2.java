package com.funbridge.server.player.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pserent on 04/06/2014.
 * Bean with data connection for player
 */
@Document(collection = "player_connection_history")
public class PlayerConnectionHistory2 {
    @Id
    private ObjectId ID;
    @Indexed
    private long dateLogin;
    private long dateLogout;
    @Indexed
    private long playerID;
    @Indexed
    private long deviceID;
    @Indexed
    private String deviceType;
    private String deviceInfo;
    private String clientVersion = "0.0.0";
    private int nbDealPlayed = 0;
    private int nbDealReplayed = 0;
    private String lang;
    private String country;
    private long deviceCreationDate = 0;
    private String protocol;
    private Date creationDateISO = null;

    private Map<Integer, Integer> mapCategoryPlay = new HashMap<>();
    private Map<Integer, Integer> mapCategoryReplay = new HashMap<>();
    private String playerActions = "";
    private boolean freemium = false;
    private boolean avatarPresent = false;
    private boolean playerInTeam = false;
    private String playerSerie = null;
    private int nbCallStoreGetProducts = 0;
    private int nbFriend = 0;
    private int nbFollowing = 0;
    private int nbFollowers = 0;
    private int creditDeal = 0;
    private int creditSubscriptionDay = 0;
    private int totalNbDealPlayed = 0;
    private int nbMessagesSent = 0;
    private double handicap;
    private String segmentation = null;
    private Map<String, Object> applicationStats;
    private int nbLearningDeals = 0;
    private String nursing = "";

    public ObjectId getID() {
        return ID;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public long getDateLogin() {
        return dateLogin;
    }

    public void setDateLogin(long dateLogin) {
        this.dateLogin = dateLogin;
    }

    public long getDateLogout() {
        return dateLogout;
    }

    public void setDateLogout(long dateLogout) {
        this.dateLogout = dateLogout;
    }

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public long getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(long deviceID) {
        this.deviceID = deviceID;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public int getNbDealPlayed() {
        return nbDealPlayed;
    }

    public void setNbDealPlayed(int nbDealPlayed) {
        this.nbDealPlayed = nbDealPlayed;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public long getDeviceCreationDate() {
        return deviceCreationDate;
    }

    public void setDeviceCreationDate(long deviceCreationDate) {
        this.deviceCreationDate = deviceCreationDate;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Map<Integer, Integer> getMapCategoryPlay() {
        return mapCategoryPlay;
    }

    public void setMapCategoryPlay(Map<Integer, Integer> mapCategoryPlay) {
        this.mapCategoryPlay = mapCategoryPlay;
    }

    public Map<Integer, Integer> getMapCategoryReplay() {
        return mapCategoryReplay;
    }

    public void setMapCategoryReplay(Map<Integer, Integer> mapCategoryReplay) {
        this.mapCategoryReplay = mapCategoryReplay;
    }

    public int getNbDealReplayed() {
        return nbDealReplayed;
    }

    public void setNbDealReplayed(int nbDealReplayed) {
        this.nbDealReplayed = nbDealReplayed;
    }

    public String getPlayerActions() {
        return playerActions;
    }

    public void setPlayerActions(String playerActions) {
        this.playerActions = playerActions;
    }

    public boolean isFreemium() {
        return freemium;
    }

    public void setFreemium(boolean freemium) {
        this.freemium = freemium;
    }

    public boolean isAvatarPresent() {
        return avatarPresent;
    }

    public void setAvatarPresent(boolean avatarPresent) {
        this.avatarPresent = avatarPresent;
    }

    public boolean isPlayerInTeam() {
        return playerInTeam;
    }

    public void setPlayerInTeam(boolean playerInTeam) {
        this.playerInTeam = playerInTeam;
    }

    public String getPlayerSerie() {
        return playerSerie;
    }

    public void setPlayerSerie(String playerSerie) {
        this.playerSerie = playerSerie;
    }

    public int getNbCallStoreGetProducts() {
        return nbCallStoreGetProducts;
    }

    public void setNbCallStoreGetProducts(int nbCallStoreGetProducts) {
        this.nbCallStoreGetProducts = nbCallStoreGetProducts;
    }

    public int getNbFriend() {
        return nbFriend;
    }

    public void setNbFriend(int nbFriend) {
        this.nbFriend = nbFriend;
    }

    public int getNbFollowing() {
        return nbFollowing;
    }

    public void setNbFollowing(int nbFollowing) {
        this.nbFollowing = nbFollowing;
    }

    public int getNbFollowers() {
        return nbFollowers;
    }

    public void setNbFollowers(int nbFollowers) {
        this.nbFollowers = nbFollowers;
    }

    public int getCreditDeal() {
        return creditDeal;
    }

    public void setCreditDeal(int creditDeal) {
        this.creditDeal = creditDeal;
    }

    public int getCreditSubscriptionDay() {
        return creditSubscriptionDay;
    }

    public void setCreditSubscriptionDay(int creditSubscriptionDay) {
        this.creditSubscriptionDay = creditSubscriptionDay;
    }

    public int getTotalNbDealPlayed() {
        return totalNbDealPlayed;
    }

    public void setTotalNbDealPlayed(int totalNbDealPlayed) {
        this.totalNbDealPlayed = totalNbDealPlayed;
    }

    public int getNbMessagesSent() {
        return nbMessagesSent;
    }

    public void setNbMessagesSent(int nbMessagesSent) {
        this.nbMessagesSent = nbMessagesSent;
    }

    public double getHandicap() {
        return handicap;
    }

    public void setHandicap(double handicap) {
        this.handicap = handicap;
    }

    public String getSegmentation() {
        return segmentation;
    }

    public void setSegmentation(String segmentation) {
        this.segmentation = segmentation;
    }

    public Map<String, Object> getApplicationStats() {
        return applicationStats;
    }

    public void setApplicationStats(Map<String, Object> applicationStats) {
        this.applicationStats = applicationStats;
    }

    public int getNbLearningDeals() {
        return nbLearningDeals;
    }

    public void setNbLearningDeals(int nbLearningDeals) {
        this.nbLearningDeals = nbLearningDeals;
    }

    public String getNursing() {
        return nursing;
    }

    public void setNursing(String nursing) {
        this.nursing = nursing;
    }
}
