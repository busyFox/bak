package com.funbridge.server.tournament.team.data;

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
 * Created by pserent on 08/11/2016.
 */
@Document(collection="team_tournament")
public class TeamTournament extends Tournament {
    @Id
    private ObjectId ID;
    private String periodTour; // Concaténation de la période et du tour auxquels le tournoi appartient
    private String division; // Division à laquelle le tournoi appartient
    private String group; // Groupe auquel le tournoi appartient A, B, C, D de chaque équipe (autant de groupes que de titulaires d'une équipe)
    private long startDate = 0;
    private long endDate = 0;

    public List<TeamDeal> deals = new ArrayList<>();
    private Date creationDateISO = null;

    public TeamTournament() {
        this.category = Constantes.TOURNAMENT_CATEGORY_TEAM;
    }

    public String toString() {
        return super.toString()+" - periodTour="+periodTour+" - division="+division+" - group="+group+" - startDate="+Constantes.timestamp2StringDateHour(startDate)+" - endDate="+Constantes.timestamp2StringDateHour(endDate);
    }

    @Override
    public List<TeamDeal> getDeals() {
        return deals;
    }

    @Override
    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    @Override
    public int getResultType() {
        return Constantes.TOURNAMENT_RESULT_IMP;
    }

    @Override
    public boolean isDateValid(long dateTS) {
        return (startDate < dateTS) && (dateTS < endDate);
    }


    public void setDealGenerate(DealGenerator.DealGenerate[] tabDealGen) {
        deals.clear();
        if (tabDealGen != null) {
            for (DealGenerator.DealGenerate dealGen : tabDealGen) {
                TeamDeal deal = new TeamDeal();
                deal.index = deals.size()+1;
                deal.dealer = dealGen.dealer;
                deal.vulnerability = dealGen.vulnerability;
                deal.cards = dealGen.distrib;
                deal.paramGenerator = dealGen.paramGenerator;
                deals.add(deal);
            }
        }
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getPeriodTour() {
        return periodTour;
    }

    public void setPeriodTour(String periodTour) {
        this.periodTour = periodTour;
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

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getName() {
        return periodTour+"-"+division+"-"+group;
    }

    /**
     * Return a tournament object for WS. !! Field not set : resultPlayer, currentDealIndex, nbTotalPlayer (for tournament in progress) and arrayDealIDstr is set to null
     * @return
     */
    public WSTournament toWS() {
        WSTournament wsTour = new WSTournament();
        wsTour.tourIDstr = getIDStr();
        wsTour.beginDate = startDate;
        wsTour.categoryID = getCategory();
        wsTour.countDeal = deals.size();
        wsTour.endDate = endDate;
        wsTour.name = getName();
        wsTour.resultType = Constantes.TOURNAMENT_RESULT_IMP;
        wsTour.nbTotalPlayer = nbPlayers;
        wsTour.arrayDealIDstr = new String[deals.size()];
        for (int i=0; i < deals.size(); i++) {
            wsTour.arrayDealIDstr[i] = deals.get(i).getDealID(getIDStr());
        }
        return wsTour;
    }
}
