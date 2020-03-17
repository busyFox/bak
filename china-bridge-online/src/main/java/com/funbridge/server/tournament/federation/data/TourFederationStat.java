package com.funbridge.server.tournament.federation.data;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.annotation.Id;

import java.util.HashMap;
import java.util.Map;

public class TourFederationStat {
    @Id
    public long playerID;
    public long dateLastTournament = 0;
    public String countryCode;
    public TourFederationStatResult total = new TourFederationStatResult();

    /* Map for period -> result */
    public Map<String, TourFederationStatResult> resultPeriod = new HashMap<>();

    /* Map for period -> cumul Funbridge Points */
    public Map<String, TourFederationStatResult> totalPeriod = new HashMap<>();

    public void update(long funbridgePoints, double federationPoints, long tourDate, String periodID) {
        dateLastTournament = tourDate;
        TourFederationStatResult rp = resultPeriod.get(periodID);
        if (rp == null) {
            rp = new TourFederationStatResult();
            resultPeriod.put(periodID, rp);
        }
        rp.nbTournaments++;
        rp.funbridgePoints += funbridgePoints;
        rp.federationPoints += federationPoints;

        total.nbTournaments++;
        total.funbridgePoints += funbridgePoints;
        total.federationPoints += federationPoints;

        TourFederationStatResult tp = totalPeriod.get(periodID);
        if (tp == null) {
            tp = new TourFederationStatResult();
            resultPeriod.put(periodID, tp);
        }
        tp.nbTournaments++;
        tp.funbridgePoints += funbridgePoints;
        tp.federationPoints += federationPoints;
    }

    public void setData(TourFederationPlayerPoints playerPoints, long tourDate, String countryCode, String periodID) {
        dateLastTournament = tourDate;
        total.funbridgePoints = (playerPoints != null)?playerPoints.funbridgePoints:0;
        total.federationPoints = (playerPoints != null)?playerPoints.points:0;
        total.nbTournaments = (playerPoints != null)?playerPoints.nbTournaments:0;
        if (StringUtils.isNotBlank(countryCode)) {
            this.countryCode = countryCode;
        }

        // Create totalPeriod, but don't update existing ones
        TourFederationStatResult tp = totalPeriod.get(periodID);
        if (tp == null) {
            tp = new TourFederationStatResult();
            tp.nbTournaments = total.nbTournaments;
            tp.funbridgePoints = total.funbridgePoints;
            tp.federationPoints = total.federationPoints;
            totalPeriod.put(periodID, tp);
        }
    }
    public void addOrUpdatePeriod(TourFederationPlayerPoints playerPoints, String periodID) {
        if (StringUtils.isNotBlank(periodID) && playerPoints != null) {
            TourFederationStatResult statResult = resultPeriod.get(periodID);
            if (statResult == null) {
                statResult = new TourFederationStatResult();
                resultPeriod.put(periodID, statResult);
            }
            statResult.funbridgePoints = playerPoints.funbridgePoints;
            statResult.federationPoints = playerPoints.points;
            statResult.nbTournaments = playerPoints.nbTournaments;
        }
    }

    public void updateCountry(String countryCode) {
        if (StringUtils.isNotBlank(countryCode)) {
            this.countryCode = countryCode;
        }
    }

    public TourFederationStatResult getStatResult(String periodIDAsk, String currentPeriodID) {
        if (StringUtils.isBlank(periodIDAsk)) {
            return totalPeriod.get(currentPeriodID);
        } else {
            return resultPeriod.get(periodIDAsk);
        }
    }
}
