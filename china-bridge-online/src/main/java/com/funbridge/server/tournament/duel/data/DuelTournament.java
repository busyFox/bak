package com.funbridge.server.tournament.duel.data;

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
 * Created by pserent on 07/07/2015.
 */
@Document(collection="duel_tournament")
public class DuelTournament extends Tournament {
    @Id
    private ObjectId ID;
    private long endDate = 0;
    private long creationDate = 0;

    private List<DuelDeal> deals = new ArrayList<>();
    private Date creationDateISO = null;
    private boolean argineDuel = false;

    public DuelTournament() {
        this.category = Constantes.TOURNAMENT_CATEGORY_DUEL;
    }

    public String toString() {
        return super.toString()+" - dateCreation="+Constantes.timestamp2StringDateHour(creationDate)+" - endDate="+Constantes.timestamp2StringDateHour(endDate);
    }

    @Override
    public String getName() {
        return "DUEL-"+getIDStr();
    }

    public ObjectId getID() {
        return ID;
    }

    public String getIDStr() {
        return ID.toString();
    }

    @Override
    public int getCategory() {
        return Constantes.TOURNAMENT_CATEGORY_DUEL;
    }

    @Override
    public List<DuelDeal> getDeals() {
        return deals;
    }

    @Override
    public int getResultType() {
        return Constantes.TOURNAMENT_RESULT_IMP;
    }

    @Override
    public boolean isDateValid(long dateTS) {
        return (creationDate < dateTS) && (dateTS < endDate);
    }

    @Override
    public long getStartDate() {
        return creationDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public void setDealGenerate(DealGenerator.DealGenerate[] tabDealGen) {
        deals.clear();
        if (tabDealGen != null) {
            for (DealGenerator.DealGenerate dealGen : tabDealGen) {
                DuelDeal deal = new DuelDeal();
                deal.index = deals.size()+1;
                deal.dealer = dealGen.dealer;
                deal.vulnerability = dealGen.vulnerability;
                deal.cards = dealGen.distrib;
                deal.paramGenerator = dealGen.paramGenerator;
                deals.add(deal);
            }
        }
    }

    public WSTournament toWS() {
        WSTournament wsTour = new WSTournament();
        wsTour.beginDate = creationDate;
        wsTour.categoryID = Constantes.TOURNAMENT_CATEGORY_DUEL;
        wsTour.countDeal = getNbDeals();
        wsTour.endDate = getEndDate();
        wsTour.tourIDstr = getIDStr();
        wsTour.name = Constantes.TOURNAMENT_CATEGORY_NAME_DUEL;
        wsTour.resultType = getResultType();
        wsTour.nbTotalPlayer = 2;
        wsTour.arrayDealIDstr = new String[deals.size()];
        for (int i=0; i < deals.size(); i++) {
            wsTour.arrayDealIDstr[i] = deals.get(i).getDealID(getIDStr());
        }
        return wsTour;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public boolean isArgineDuel() {
        return argineDuel;
    }

    public void setArgineDuel(boolean argineDuel) {
        this.argineDuel = argineDuel;
    }
}
