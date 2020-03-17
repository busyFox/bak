package com.funbridge.server.tournament.federation.data;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="funbridge_points_stat")
public class TourFunbridgePointsStat extends TourFederationStat {

    public void update(long funbridgePoints, long tourDate, String periodID) {
        dateLastTournament = tourDate;
        TourFederationStatResult rp = resultPeriod.get(periodID);
        if (rp == null) {
            rp = new TourFederationStatResult();
            resultPeriod.put(periodID, rp);
        }
        rp.nbTournaments++;
        rp.funbridgePoints += funbridgePoints;

        total.nbTournaments++;
        total.funbridgePoints += funbridgePoints;

        TourFederationStatResult tp = totalPeriod.get(periodID);
        if (tp == null) {
            tp = new TourFederationStatResult();
            totalPeriod.put(periodID, tp);
        }
        tp.nbTournaments++;
        tp.funbridgePoints += funbridgePoints;
    }
}
