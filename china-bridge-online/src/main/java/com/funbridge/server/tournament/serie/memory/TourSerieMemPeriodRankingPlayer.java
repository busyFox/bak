package com.funbridge.server.tournament.serie.memory;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.ws.result.WSRankingSeriePlayer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by pserent on 24/06/2014.
 * Bean
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourSerieMemPeriodRankingPlayer implements Comparable<TourSerieMemPeriodRankingPlayer> {
    public long playerID;
    public double result;
    public int rank = -1;
    public List<TourSerieMemPeriodRankingPlayerData> listData = new ArrayList<>();
    public int trend = 0;
    public String bestSerie = null;
    public int bestRank = -1;
    public String countryCode = "";

    public TourSerieMemPeriodRankingPlayer(){}

    public String toString() {
        return "playerID="+playerID+" - result="+result+" - rank="+rank+" - trend="+trend+" - nbTourPlayed="+getNbTournamentPlayed()+" - bestSerie="+bestSerie+" - bestRank="+bestRank;
    }

    public int getNbTournamentPlayed() {
        return listData.size();
    }

    public void orderListDataByDateAsc() {
        Collections.sort(listData, new Comparator<TourSerieMemPeriodRankingPlayerData>() {
            @Override
            public int compare(TourSerieMemPeriodRankingPlayerData o1, TourSerieMemPeriodRankingPlayerData o2) {
                if (o1.dateResult < o2.dateResult) {
                    return -1;
                } else if (o1.dateResult > o2.dateResult) {
                    return 1;
                }
                return 0;
            }
        });
    }

    @Override
    public int compareTo(TourSerieMemPeriodRankingPlayer o) {
        if (o.getNbTournamentPlayed() == 0 && this.getNbTournamentPlayed() == 0) {
            return 0;
        }
        if (this.getNbTournamentPlayed() == 0) {
            return 1;
        }
        if (o.getNbTournamentPlayed() == 0) {
            return -1;
        }
        // result of this object is greater so ranking is smaller ! Big result => ranking small
        if (o.result > this.result) {
            return 1;
        } else if (o.result == this.result) {
            return 0;
        }
        return -1;
    }

    /**
     * Compute the result of player on this serie.
     * @return
     */
    public void computeResult() {
        int nbRes = 0;
        double valRes = 0;
        List<Double> listResult = new ArrayList<>();
        for (TourSerieMemPeriodRankingPlayerData e : listData) {
            listResult.add(e.result);
        }
        Collections.sort(listResult);
        int nbResToRemove = 0;
        int bonusNbTour = ContextManager.getTourSerieMgr().getBonusNbTour();
        int bonusRemove = ContextManager.getTourSerieMgr().getBonusRemove();
        if (bonusNbTour > 0 && bonusRemove > 0) {
            nbResToRemove = (listResult.size() / bonusNbTour) * bonusRemove;
        }
        for (int i=nbResToRemove; i < listResult.size(); i++) {
            nbRes++;
            valRes += listResult.get(i);
        }

        if (nbRes > 0) {
            valRes = valRes / nbRes;
        }
        this.result = valRes;
    }

    /**
     * Return the result on tournament for player.
     * @param tourID
     * @return result element if player has played this tournament else null
     */
    public TourSerieMemPeriodRankingPlayerData getResultForTournament(String tourID) {
        for (TourSerieMemPeriodRankingPlayerData res : listData) {
            if (res.tourID.equals(tourID)) {
                return res;
            }
        }
        return null;
    }

    /**
     * Add or modify result on tournament for player
     * @param tourID
     * @param result
     * @param dateResult
     */
    public void setTourResult(String tourID, double result, int rank, long dateResult, boolean computeResult) {
        TourSerieMemPeriodRankingPlayerData res = getResultForTournament(tourID);
        if (res == null) {
            res = new TourSerieMemPeriodRankingPlayerData();
            listData.add(res);
        }
        res.tourID = tourID;
        res.result = result;
        res.dateResult = dateResult;
        res.rank = rank;
        if (computeResult) {
            computeResult();
        }
    }

    /**
     * Transform to WS object
     * @param playerCache playerCache of ranking for player. if null, load it from cache
     * @param playerAsk
     * @return
     */
    public WSRankingSeriePlayer toWSRankingSeriePlayer(PlayerCache playerCache, long playerAsk, boolean playerConnected) {
        WSRankingSeriePlayer wrsp = new WSRankingSeriePlayer();
        wrsp.setPlayerID(playerID);
        wrsp.setRank(rank);
        wrsp.setResult(result);
        wrsp.setNbTournamentPlayed(getNbTournamentPlayed());
        wrsp.setTrend(trend);
        wrsp.setConnected(playerConnected);
        if (playerCache != null) {
            if (playerAsk == playerID) {
                wrsp.setAvatarPresent(playerCache.avatarPresent);
            } else {
                wrsp.setAvatarPresent(playerCache.avatarPublic);
            }
            wrsp.setCountryCode(playerCache.countryCode);
            wrsp.setPlayerPseudo(playerCache.getPseudo());
        } else {
            PlayerCache pc = ContextManager.getPlayerCacheMgr().getPlayerCache(playerID);
            if (playerAsk == playerID) {
                wrsp.setAvatarPresent(pc.avatarPresent);
            } else {
                wrsp.setAvatarPresent(pc.avatarPublic);
            }
            wrsp.setCountryCode(pc.countryCode);
            wrsp.setPlayerPseudo(pc.getPseudo());
        }
        return wrsp;
    }


    /**
     * Return true if best result must be updated
     * @param currentSerie
     * @return
     */
    public boolean isToUpdateBestResult(String currentSerie) {
        if (currentSerie != TourSerieMgr.SERIE_NC) {
            // First time up or maintain in current serie
            if (bestSerie == null && (trend > 0 || trend == 0)) {
                return true;
            }
            if (bestSerie != null) {
                // same serie & rank is best
                if (bestSerie.equals(currentSerie) && rank < bestRank) {
                    return true;
                }
                // currentSerie is best than current bestSerie
                try {
                    if (TourSerieMgr.compareSerie(currentSerie, bestSerie) > 0) {
                        return true;
                    }
                } catch (Exception e) {
                    ContextManager.getTourSerieMgr().getLogger().error("Failed to compare serie : currentSerie="+currentSerie+" - bestSerie="+bestSerie, e);
                }
            }

        }
        return false;
    }

    /**
     * Return list data play order by date desc
     * @param offset
     * @param nbMax
     * @return
     */
    public List<TourSerieMemPeriodRankingPlayerData> listTourPlayOrderDate(int offset, int nbMax) {
        if (offset < 0) {
            offset = 0;
        }
        if (!listData.isEmpty() && offset < listData.size()) {
            List<TourSerieMemPeriodRankingPlayerData> l = new ArrayList(listData);
            Collections.sort(l, new Comparator<TourSerieMemPeriodRankingPlayerData>() {
                @Override
                public int compare(TourSerieMemPeriodRankingPlayerData o1, TourSerieMemPeriodRankingPlayerData o2) {
                    return Long.compare(o1.dateResult, o2.dateResult);
                }
            });
            return l.subList(offset, (offset+nbMax)<=l.size()?offset+nbMax:l.size());
        }
        return null;
    }
}
