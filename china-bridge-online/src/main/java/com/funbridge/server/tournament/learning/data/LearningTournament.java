package com.funbridge.server.tournament.learning.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.ws.tournament.WSTournament;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ldelbarre on 30/05/2018.
 */
@Document(collection="learning_tournament")
public class LearningTournament extends Tournament {
    @Id
    private ObjectId ID;
    private String chapterID;
    private Date creationDate;
    private int resultType;
    private List<LearningDeal> deals = new ArrayList<>();

    public String toString() {
        return "ID="+getIDStr()+" - chapterID="+ chapterID +" - resultType="+resultType+" - nbDeals="+deals.size()+" - nbPlayers="+nbPlayers+" - finished="+finished;
    }

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    @Override
    public int getCategory() {
        return Constantes.TOURNAMENT_CATEGORY_LEARNING;
    }

    @Override
    public List<LearningDeal> getDeals() {
        return deals;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }

    public boolean isDateValid(long dateTS) {
        return true;
    }

    public String getChapterID() {
        return chapterID;
    }

    public void setChapterID(String chapterID) {
        this.chapterID = chapterID;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public long getStartDate() {
        return this.getCreationDate().getTime();
    }

    public long getEndDate() {
        return this.getCreationDate().getTime()+(Constantes.TIMESTAMP_DAY * 365 * 50);
    }

    public WSTournament toWS(){
        WSTournament wsTour = new WSTournament();
        wsTour.tourIDstr = getIDStr();
        wsTour.beginDate = getStartDate();
        wsTour.categoryID = getCategory();
        wsTour.countDeal = getNbDeals();
        wsTour.endDate = getEndDate();
        wsTour.resultType = resultType;
        wsTour.nbTotalPlayer = nbPlayers;
        wsTour.arrayDealIDstr = new String[deals.size()];
        for (int i=0; i < deals.size(); i++) {
            wsTour.arrayDealIDstr[i] = deals.get(i).getDealID(getIDStr());
        }
        return wsTour;
    }

    @Override
    public String getName() {
        return "LEARNING-"+ chapterID +"-"+getIDStr();
    }
}
