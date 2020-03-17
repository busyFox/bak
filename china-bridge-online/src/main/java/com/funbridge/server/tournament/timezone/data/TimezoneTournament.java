package com.funbridge.server.tournament.timezone.data;

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
 * Created by pserent on 15/04/2015.
 */
@Document(collection="timezone_tournament")
public class TimezoneTournament extends Tournament {
    @Id
    private ObjectId ID;
    private int resultType;
    private long startDate = 0;
    private long endDate = 0;

    private List<TimezoneDeal> deals = new ArrayList<>();
    private Date creationDateISO = null;

    public TimezoneTournament() {
        this.category = Constantes.TOURNAMENT_CATEGORY_TIMEZONE;
    }

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    public String toString() {
        return "ID="+ID+" - resultType="+resultType+" - startDate="+ Constantes.timestamp2StringDateHour(startDate)+" - endDate="+ Constantes.timestamp2StringDateHour(endDate)+" - nbPlayers="+nbPlayers+" - finished="+finished;
    }

    @Override
    public int getCategory() {
        return Constantes.TOURNAMENT_CATEGORY_TIMEZONE;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
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

    @Override
    public List<TimezoneDeal> getDeals() {
        return deals;
    }

    @Override
    public String getName() {
        return "TIMEZONE-"+getIDStr();
    }

    public void setDealGenerate(DealGenerator.DealGenerate[] tabDealGen) {
        deals.clear();
        if (tabDealGen != null) {
            for (DealGenerator.DealGenerate dealGen : tabDealGen) {
                TimezoneDeal deal = new TimezoneDeal();
                deal.index = deals.size()+1;
                deal.dealer = dealGen.dealer;
                deal.vulnerability = dealGen.vulnerability;
                deal.cards = dealGen.distrib;
                deal.paramGenerator = dealGen.paramGenerator;
                deals.add(deal);
            }
        }
    }

    public boolean isDateValid(long dateTS) {
        return (startDate < dateTS) && (dateTS < endDate);
    }

    /**
     * Return a tournament object for WS. !! Field not set : resultPlayer, currentDealIndex, nbTotalPlayer (for tournament in progress)
     * @return
     */
    public WSTournament toWS() {
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

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }
}
