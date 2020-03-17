package com.funbridge.server.ws.support;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.game.Deal;
import com.funbridge.server.tournament.serie.data.TourSerieDeal;

public class WSSupDeal {
	public long dealID;
    public String dealIDstr;
	public int index;
	public String dealer;
	public String vulnerability;
	public String paramGeneration;
	public String distribution;
    public int resultType;
	
	public WSSupDeal() {}
	
	public WSSupDeal(TourSerieDeal deal, String tourID) {
        this.dealIDstr = deal.getDealID(tourID);
        this.index = deal.index;
        this.dealer = Character.toString(deal.dealer);
        this.vulnerability = Character.toString(deal.vulnerability);
        this.paramGeneration = deal.paramGenerator;
        this.distribution = deal.cards;
        this.resultType = Constantes.TOURNAMENT_RESULT_PAIRE;
    }

    public WSSupDeal(Deal deal, String tourID, int resultType) {
        this.dealIDstr = deal.getDealID(tourID);
        this.index = deal.index;
        this.dealer = Character.toString(deal.dealer);
        this.vulnerability = Character.toString(deal.vulnerability);
        this.paramGeneration = deal.paramGenerator;
        this.distribution = deal.cards;
        this.resultType = resultType;
    }
}
