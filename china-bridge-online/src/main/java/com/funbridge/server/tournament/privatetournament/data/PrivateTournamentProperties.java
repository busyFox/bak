package com.funbridge.server.tournament.privatetournament.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by pserent on 23/01/2017.
 */
@Document(collection="private_tournament_properties")
public class PrivateTournamentProperties {
    @Id
    private ObjectId ID;
    private long ownerID;
    private String name;
    private int nbDeals;
    private long startDate;
    private String recurrence;
    private String accessRule;
    private String password;
    private int rankingType;
    private int duration;
    private String countryCode;
    private List<Long> playersFavorite = new ArrayList<>();
    private Date creationDateISO = null;
    private boolean enable = true; // true when tournament(s) in progress. set false on finish process when no more occurence
    private long tsSetRemove = 0; // timestamp when creator delete tournament or when tournament with occurence ONCE is finished
    private String description;
    private int nbTimesNotEnoughPlayer = 0;

    public String toString() {
        return "ID="+getIDStr()+" - name="+name+" - ownerID="+ownerID+" - nbDeals="+nbDeals+" - recurrence="+recurrence+" - accessRule="+accessRule+" - enable="+enable+" - tsSetRemove="+Constantes.timestamp2StringDateHour(tsSetRemove)+" - playersFavorite size="+playersFavorite.size();
    }

    public ObjectId getID() {
        return ID;
    }

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public long getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(long ownerID) {
        this.ownerID = ownerID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNbDeals() {
        return nbDeals;
    }

    public void setNbDeals(int nbDeals) {
        this.nbDeals = nbDeals;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    public String getAccessRule() {
        return accessRule;
    }

    public void setAccessRule(String accessRule) {
        this.accessRule = accessRule;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRankingType() {
        return rankingType;
    }

    public void setRankingType(int rankingType) {
        this.rankingType = rankingType;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public boolean isValid() {
        if (name == null || name.length() == 0) {
            return false;
        }
        if (nbDeals < 3 || nbDeals > 40) {
            return false;
        }
        if (rankingType != Constantes.TOURNAMENT_RESULT_IMP && rankingType != Constantes.TOURNAMENT_RESULT_PAIRE) {
            return false;
        }
        if (duration <= 0) {
            return false;
        }
        if (!recurrence.equals(PrivateTournamentMgr.RECURRENCE_DAILY) && !recurrence.equals(PrivateTournamentMgr.RECURRENCE_ONCE) && !recurrence.equals(PrivateTournamentMgr.RECURRENCE_WEEKLY)) {
            return false;
        }
        if (!accessRule.equals(PrivateTournamentMgr.ACCESS_RULE_PASSWORD) && !accessRule.equals(PrivateTournamentMgr.ACCESS_RULE_PUBLIC)) {
            return false;
        }
        if (accessRule.equals(PrivateTournamentMgr.ACCESS_RULE_PASSWORD) && (password == null || password.length() == 0)) {
            return false;
        }
        return startDate != 0;
    }

    public List<Long> getPlayersFavorite() {
        return playersFavorite;
    }

    public boolean isPlayerFavorite(long playerID) {
        return playersFavorite.contains(playerID);
    }

    public void addPlayerFavorite(long playerID) {
        if (!playersFavorite.contains(playerID)) {
            playersFavorite.add(playerID);
        }
    }

    public void removePlayerFavorite(long playerID) {
        playersFavorite.remove(playerID);
    }

    public boolean isRecurrent() {
        return recurrence != null && (recurrence.equals(PrivateTournamentMgr.RECURRENCE_DAILY) || recurrence.equals(PrivateTournamentMgr.RECURRENCE_WEEKLY));
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public long getTsSetRemove() {
        return tsSetRemove;
    }

    public void setTsSetRemove(long tsSetRemove) {
        this.tsSetRemove = tsSetRemove;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getNbTimesNotEnoughPlayer() {
        return nbTimesNotEnoughPlayer;
    }

    public void setNbTimesNotEnoughPlayer(int nbTimesNotEnoughPlayer) {
        this.nbTimesNotEnoughPlayer = nbTimesNotEnoughPlayer;
    }

    public void incrementNbTimesNotEnoughPlayer() {
        nbTimesNotEnoughPlayer++;
    }
}
