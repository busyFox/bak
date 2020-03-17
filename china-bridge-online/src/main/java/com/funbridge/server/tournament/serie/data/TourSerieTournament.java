package com.funbridge.server.tournament.serie.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.DealGenerator;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.ws.tournament.WSTournament;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Created by pserent on 28/05/2014.
 * Objet to define a tournament for serie
 */
@Document(collection="serie_tournament")
public class TourSerieTournament extends Tournament{
    @Id
    private ObjectId ID;
    private String serie;
    private String period;
    private long tsDateStart = 0;
    private long tsDateEnd = 0;

    public List<TourSerieDeal> listDeal = new ArrayList<>();
    private Date creationDateISO = null;

    public TourSerieTournament() {
        this.category = Constantes.TOURNAMENT_CATEGORY_NEWSERIE;
    }

    @Override
    public List<TourSerieDeal> getDeals() {
        return listDeal;
    }

    private synchronized void computeTsDate() {
        if (period != null) {
            tsDateStart = TourSerieMgr.transformPeriodID2TS(period, true);
            tsDateEnd = TourSerieMgr.transformPeriodID2TS(period, false);
        }
    }

    @Override
    public String toString() {
        return super.toString()+ "- serie="+serie+" - period="+period+" - dateStart="+Constantes.timestamp2StringDateHour(tsDateStart)+" - dateEnd="+Constantes.timestamp2StringDateHour(tsDateEnd);
    }

    public void setDealGenerate(DealGenerator.DealGenerate[] tabDealGen) {
        listDeal.clear();
        if (tabDealGen != null) {
            for (DealGenerator.DealGenerate dealGen : tabDealGen) {
                TourSerieDeal deal = new TourSerieDeal();
                deal.index = listDeal.size()+1;
                deal.dealer = dealGen.dealer;
                deal.vulnerability = dealGen.vulnerability;
                deal.cards = dealGen.distrib;
                deal.paramGenerator = dealGen.paramGenerator;
                listDeal.add(deal);
            }
        }
    }

    public long getTsDateStart() {
        if (tsDateStart == 0) {
            computeTsDate();
        }
        return tsDateStart;
    }

    public long getTsDateEnd() {
        if (tsDateEnd == 0) {
            computeTsDate();
        }
        return tsDateEnd;
    }

    public void setTsDateStart(long tsDateStart) {
        this.tsDateStart = tsDateStart;
    }

    public void setTsDateEnd(long tsDateEnd) {
        this.tsDateEnd = tsDateEnd;
    }

    public ObjectId getID() {
        return ID;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public List<TourSerieDeal> getListDeal() {
        return listDeal;
    }

    public void setListDeal(List<TourSerieDeal> listDeal) {
        this.listDeal = listDeal;
    }

    public String getIDStr() {
        return ID.toString();
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    /**
     * Return a tournament object for WS. !! Field not set : resultPlayer, currentDealIndex, nbTotalPlayer (for tournament in progress)
     * @return
     */
    public WSTournament toWS() {
        WSTournament wsTour = new WSTournament();
        wsTour.tourIDstr = getIDStr();
        wsTour.beginDate = getTsDateStart();
        wsTour.categoryID = getCategory();
        wsTour.countDeal = getNbDeals();
        wsTour.endDate = getTsDateEnd();
        wsTour.periodID = wsTour.beginDate+";"+wsTour.endDate;
        wsTour.name = serie;
        wsTour.resultType = Constantes.TOURNAMENT_RESULT_PAIRE;
        wsTour.nbTotalPlayer = nbPlayers;
        wsTour.arrayDealIDstr = new String[listDeal.size()];
        for (int i=0; i < listDeal.size(); i++) {
            wsTour.arrayDealIDstr[i] = listDeal.get(i).getDealID(getIDStr());
        }
        return wsTour;
    }

    public int getResultType() {
        return Constantes.TOURNAMENT_RESULT_PAIRE;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getPeriod() {
        return period;
    }

    public boolean isDateValid(long dateTS) {
        if (tsDateStart == 0 || tsDateEnd == 0) {
            computeTsDate();
        }
        return (tsDateStart < dateTS) && (dateTS < tsDateEnd);
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    @Override
    public long getStartDate() {
        return getTsDateStart();
    }

    @Override
    public long getEndDate() {
        return getTsDateEnd();
    }

    @Override
    public String getName() {
        return "SERIE "+serie+"-"+period+"-"+getIDStr();
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TourSerieTournament that = (TourSerieTournament) o;
        return tsDateStart == that.tsDateStart &&
                tsDateEnd == that.tsDateEnd &&
                ID.equals(that.ID) &&
                serie.equals(that.serie) &&
                period.equals(that.period) &&
                creationDateISO.equals(that.creationDateISO);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID, serie, period, tsDateStart, tsDateEnd, creationDateISO);
    }
}
