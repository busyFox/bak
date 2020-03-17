package com.funbridge.server.ws.support;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.TourSeriePeriodResult;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="periodResult")
public class WSSupPeriodResult {
	public String periodStart;
	public String periodEnd;
	public long periodID;
    public String periodIDstr;
	public int rank;
	public int nbPlayer;
	public double result;
	public String serie;
	public int evolution;
    public int nbTournamentPlayed;
	
	public WSSupPeriodResult() {}
//	public WSSupPeriodResult(TournamentPeriodResult tpr, TournamentPeriod period) {
//		if (tpr != null && period != null) {
//			periodID = period.getID();
//            periodIDstr = ""+period.getID();
//			periodStart = Constantes.timestamp2StringDate(period.getBeginDate());
//			periodEnd = Constantes.timestamp2StringDate(period.getEndDate()-1);
//			rank = tpr.getRank();
//			nbPlayer = tpr.getNbPlayer();
//			result = tpr.getResult();
//			serie = tpr.getSerie();
//			evolution = tpr.getEvolution();
//            nbTournamentPlayed = tpr.getNbTournamentPlayed();
//		}
//	}

    public WSSupPeriodResult(TourSeriePeriodResult tpr) {
        periodIDstr = tpr.getPeriodID();
        periodStart = Constantes.timestamp2StringDate(TourSerieMgr.transformPeriodID2TS(tpr.getPeriodID(), true));
        periodEnd = Constantes.timestamp2StringDate(TourSerieMgr.transformPeriodID2TS(tpr.getPeriodID(), false));
        rank = tpr.getRank();
        nbPlayer = tpr.getNbPlayer();
        result = tpr.getResult();
        serie = tpr.getSerie();
        evolution = tpr.getEvolution();
        nbTournamentPlayed = tpr.getNbTournamentPlayed();
    }
}
