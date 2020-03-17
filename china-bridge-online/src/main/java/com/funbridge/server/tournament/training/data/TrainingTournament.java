package com.funbridge.server.tournament.training.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.DealGenerator;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.ws.tournament.WSTournament;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by pserent on 09/04/2015.
 */
@Document(collection="training_tournament")
public class TrainingTournament extends Tournament {
    @Id
    private ObjectId ID;
    private long startDate;
    private long endDate;
    private int resultType = Constantes.TOURNAMENT_RESULT_UNKNOWN;
    private Date creationDateISO = null;

    private List<TrainingDeal> deals = new ArrayList<>();

    public TrainingTournament() {
        this.category = Constantes.TOURNAMENT_CATEGORY_TRAINING;
    }

    public String toString() {
        return super.toString()+" - startDate="+Constantes.timestamp2StringDateHour(startDate)+" - endDate="+Constantes.timestamp2StringDateHour(endDate);
    }

    public void setDealGenerate(DealGenerator.DealGenerate[] tabDealGen) {
        deals.clear();
        if (tabDealGen != null) {
            for (DealGenerator.DealGenerate dealGen : tabDealGen) {
                TrainingDeal deal = new TrainingDeal();
                deal.index = deals.size()+1;
                deal.dealer = dealGen.dealer;
                deal.vulnerability = dealGen.vulnerability;
                deal.cards = dealGen.distrib;
                deal.paramGenerator = dealGen.paramGenerator;
                deals.add(deal);
            }
        }
    }

    public WSTournament toWS(){
        WSTournament wsTour = new WSTournament();
        wsTour.tourIDstr = getIDStr();
        wsTour.beginDate = startDate;
        wsTour.categoryID = getCategory();
        wsTour.countDeal = getNbDeals();
        wsTour.endDate = endDate;
        wsTour.resultType = resultType;
        wsTour.nbTotalPlayer = nbPlayers;
        wsTour.arrayDealIDstr = new String[deals.size()];
        for (int i=0; i < deals.size(); i++) {
            wsTour.arrayDealIDstr[i] = deals.get(i).getDealID(getIDStr());
        }
        return wsTour;
    }

    @Override
    public List<TrainingDeal> getDeals() {
        return deals;
    }

    @Override
    public int getCategory() {
        return Constantes.TOURNAMENT_CATEGORY_TRAINING;
    }

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    @Override
    public int getResultType() {
        return resultType;
    }

    @Override
    public boolean isDateValid(long dateTS) {
        return (startDate < dateTS) && (dateTS < endDate);
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    @Override
    public String getName() {
        return "TRAINING-"+getIDStr();
    }
}
