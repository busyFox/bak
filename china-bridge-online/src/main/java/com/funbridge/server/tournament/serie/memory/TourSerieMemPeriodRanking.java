package com.funbridge.server.tournament.serie.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.TourSeriePlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pserent on 24/06/2014.
 * Bean to store the player ranking on a serie during a period.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourSerieMemPeriodRanking {
    public String serie;
    public double thresholdResultUp;
    public double thresholdResultDown;
    public int thresholdNbUp;
    public int thresholdNbDown;
    public int nbPlayerSerie;

    // Map to store ranking player : playerID => ranking player
    public Map<Long, TourSerieMemPeriodRankingPlayer> mapRankingPlayer = new ConcurrentHashMap<>();

    @JsonIgnore
    private Logger log = LogManager.getLogger(this.getClass());

    public String toString() {
        return "serie="+serie+" - thresholdResultUp="+thresholdResultUp+" - thresholdResultDown="+thresholdResultDown+" - thresholdNbUp="+thresholdNbUp+" - thresholdNbDown="+thresholdNbDown;
    }

    public void clearData() {
        mapRankingPlayer.clear();
        nbPlayerSerie = 0;
    }

    /**
     * Update the nb player in serie
     * @param value
     */
    public void updateNbPlayerSerie(int value) {
        if (!serie.equals(TourSerieMgr.SERIE_NC)) {
            nbPlayerSerie = value;
        }
    }

    /**
     * Load the threshold values from configuration serieS01.thresholdUp or serie.thresholdUp / serieS01.thresholdDown or serie.thresholdDown
     */
    public void loadThresholdFromConfiguration() {
        // Threshold result UP
        double tempResultUp = ContextManager.getTourSerieMgr().getConfigDoubleValue("serie"+serie+".thresholdResultUp", -1);
        if (tempResultUp == -1) {
            tempResultUp = ContextManager.getTourSerieMgr().getConfigDoubleValue("serie.thresholdResultUp", 0.565);
        }
        thresholdResultUp = tempResultUp;
        // Threshold result DOWN
        double tempResultDown = ContextManager.getTourSerieMgr().getConfigDoubleValue("serie"+serie+".thresholdResultDown", -1);
        if (tempResultDown == -1) {
            tempResultDown = ContextManager.getTourSerieMgr().getConfigDoubleValue("serie.thresholdResultDown", 0.52);
        }
        thresholdResultDown = tempResultDown;
        // Threshold NB UP
        int tempNbUp = ContextManager.getTourSerieMgr().getConfigIntValue("serie"+serie+".thresholdNbUp", -1);
        if (tempNbUp == -1) {
            tempNbUp = ContextManager.getTourSerieMgr().getConfigIntValue("serie.thresholdNbUp", 5);
        }
        thresholdNbUp = tempNbUp;
        // Threshold NB DOWN
        int tempNbDown = ContextManager.getTourSerieMgr().getConfigIntValue("serie"+serie+".thresholdNbDown", -1);
        if (tempNbDown == -1) {
            tempNbDown = ContextManager.getTourSerieMgr().getConfigIntValue("serie.thresholdNbDown", 0);
        }
        thresholdNbDown = tempNbDown;
    }

    /**
     * For all ranking player, order data by date ASC
     */
    public void orderAllPlayerDataByDateAsc() {
        for (TourSerieMemPeriodRankingPlayer e : mapRankingPlayer.values()) {
            e.orderListDataByDateAsc();
        }
    }

    /**
     * Return the number of player in ranking map with countryCode. If param countryCode is null or empty, return the mapRankingPlayer size. At least 1 tournament played
     * @param countryCode
     * @param listPlaFilter
     * @return
     */
    public int getNbPlayerActive(String countryCode, List<Long> listPlaFilter) {
        if ((countryCode != null && countryCode.length() > 0) || (listPlaFilter != null && !listPlaFilter.isEmpty())) {
            int nbPlayer = 0;
            for (TourSerieMemPeriodRankingPlayer e : mapRankingPlayer.values()) {
                if (listPlaFilter != null && !listPlaFilter.isEmpty() && listPlaFilter.contains(e.playerID)) {
                    nbPlayer++;
                }
                if (e.countryCode != null && e.countryCode.equals(countryCode)) {
                    nbPlayer++;
                }
            }
            return nbPlayer;
        }
        return mapRankingPlayer.size();
    }

    /**
     * Return the number of player in serie (all player set in this serie exclude the reserve)
     * @return
     */
    public int getNbPlayerForRanking() {
        if (serie.equals(TourSerieMgr.SERIE_NC)) {
            return getNbPlayerActive(null, null);
        }
        return nbPlayerSerie;
    }

    /**
     * Add player result on tournament, update result for other players who played this tournament and compute ranking on this serie
     * @param memTour
     * @param memTourRankPlayer
     * @return
     */
    public TourSerieMemPeriodRankingPlayer addPlayerTournamentResult(TourSerieMemTour memTour, TourSerieMemTourPlayer memTourRankPlayer) {
        if (memTour != null && memTourRankPlayer != null) {
            TourSerieMemPeriodRankingPlayer rankPlayer = getOrCreatePlayerRank(memTourRankPlayer.playerID);
            if (rankPlayer != null) {
                // set result of player on tournament
                rankPlayer.setTourResult(memTour.tourID, memTourRankPlayer.result, memTourRankPlayer.rankingFinished, memTourRankPlayer.dateLastPlay, true);
                // update result for all other players who played (and finished) this tournament
                List<TourSerieMemTourPlayer> listRankTourPla = memTour.getListRankingOnlyFinished();
                for (TourSerieMemTourPlayer rankTourPla : listRankTourPla) {
                    if (rankTourPla.playerID == memTourRankPlayer.playerID) continue;
                    TourSerieMemPeriodRankingPlayer e = mapRankingPlayer.get(rankTourPla.playerID);
                    if (e != null) {
                        e.setTourResult(memTour.tourID, rankTourPla.result, rankTourPla.rankingFinished, rankTourPla.dateLastPlay, true);
                    }
                }

                // compute ranking
                computeRanking();
                return rankPlayer;
            }
        }
        return null;
    }

    /**
     * Remove data for player on tournament
     * @param memTour
     * @param playerID
     * @return true if tournament played by player
     */
    public boolean removePlayerTournamentResult(TourSerieMemTour memTour, long playerID) {
        if (memTour != null) {
            TourSerieMemPeriodRankingPlayer rankPlayer = getPlayerRank(playerID);
            if (rankPlayer != null) {
                TourSerieMemPeriodRankingPlayerData data = rankPlayer.getResultForTournament(memTour.tourID);
                if (data != null) {
                    rankPlayer.listData.remove(data);
                    // update result for all other players who played this tournament
                    List<TourSerieMemTourPlayer> listRankTourPla = memTour.getListRankingOnlyFinished();
                    for (TourSerieMemTourPlayer rankTourPla : listRankTourPla) {
                        TourSerieMemPeriodRankingPlayer e = mapRankingPlayer.get(rankTourPla.playerID);
                        if (e != null) {
                            e.setTourResult(memTour.tourID, rankTourPla.result, rankTourPla.rankingFinished, rankTourPla.dateLastPlay, true);
                        }
                    }
                    if (rankPlayer.getNbTournamentPlayed() == 0) {
                        mapRankingPlayer.remove(playerID);
                    }
                    // compute ranking
                    computeRanking();
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Return the player rank
     * @param playerID
     * @return
     */
    public TourSerieMemPeriodRankingPlayer getPlayerRank(long playerID) {
        return mapRankingPlayer.get(playerID);
    }

    /**
     * Return the player rank. If not yet existing, create a new one for player with default value (rank=-1 ...).
     * @param playerID
     * @return
     */
    public TourSerieMemPeriodRankingPlayer getOrCreatePlayerRank(long playerID) {
        TourSerieMemPeriodRankingPlayer rankPla = mapRankingPlayer.get(playerID);
        if (rankPla == null) {
            TourSeriePlayer seriePlayer = ContextManager.getTourSerieMgr().getTourSeriePlayer(playerID);
            rankPla = new TourSerieMemPeriodRankingPlayer();
            rankPla.playerID = playerID;
            PlayerCache playerCache = ContextManager.getPlayerCacheMgr().getPlayerCache(playerID);
            if (playerCache != null) {
                rankPla.countryCode = playerCache.countryCode;
            }
            if (seriePlayer != null) {
                rankPla.bestSerie = seriePlayer.getBestSerie();
                rankPla.bestRank = seriePlayer.getBestRank();
            }
            mapRankingPlayer.put(playerID, rankPla);
        }
        return rankPla;
    }

    /**
     * Return the list of player order by result
     * @return
     */
    public List<Long> getListPlayerOrderResult() {
        List<Long> listKeyPla = new ArrayList<>(mapRankingPlayer.keySet());
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                TourSerieMemPeriodRankingPlayer elem1 = mapRankingPlayer.get(o1);
                TourSerieMemPeriodRankingPlayer elem2 = mapRankingPlayer.get(o2);
                return elem1.compareTo(elem2);
            }
        });
        return listKeyPla;
    }

    /**
     * Return the list of player order by trend and result
     * @return
     */
    public List<Long> getListPlayerOrderTrendAndResult() {
        if (serie.equals(TourSerieMgr.SERIE_TOP) && ContextManager.getTourSerieMgr().getConfigIntValue("rankingTopNbTour", 0) == 1) {
            return getListPlayerOrderTrendAndResultForTOP();
        }
        List<Long> listKeyPla = new ArrayList<>(mapRankingPlayer.keySet());
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                TourSerieMemPeriodRankingPlayer elem1 = mapRankingPlayer.get(o1);
                TourSerieMemPeriodRankingPlayer elem2 = mapRankingPlayer.get(o2);
                if (elem1.trend > elem2.trend) {
                    return -1;
                }
                if (elem1.trend < elem2.trend) {
                    return 1;
                }
                return elem1.compareTo(elem2);
            }
        });
        return listKeyPla;
    }

    /**
     * Return the list of player order by trend and result
     * @return
     */
    public List<Long> getListPlayerOrderTrendAndResultForTOP() {
        List<Long> listKeyPla = new ArrayList<>(mapRankingPlayer.keySet());
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                TourSerieMemPeriodRankingPlayer elem1 = mapRankingPlayer.get(o1);
                TourSerieMemPeriodRankingPlayer elem2 = mapRankingPlayer.get(o2);
                if (elem1.trend > elem2.trend) {
                    return -1;
                }
                if (elem1.trend < elem2.trend) {
                    return 1;
                }
                // same trend 0 => compare nb tour played
                if (elem1.trend == 0 && elem2.trend == 0) {
                    if (elem1.getNbTournamentPlayed() >= thresholdNbUp && elem2.getNbTournamentPlayed() < thresholdNbUp) {
                        return -1;
                    }
                    if (elem2.getNbTournamentPlayed() >= thresholdNbUp && elem1.getNbTournamentPlayed() < thresholdNbUp) {
                        return 1;
                    }
                }
                return elem1.compareTo(elem2);
            }
        });
        return listKeyPla;
    }

    /**
     * Loop on all player ranking and set trend according nb tournament play and result.
     */
    private void computeTrend() {
        for (TourSerieMemPeriodRankingPlayer e : mapRankingPlayer.values()) {
            switch(serie){
                case TourSerieMgr.SERIE_NC:
                    // NC : stay or up
                    if (e.getNbTournamentPlayed() >= thresholdNbUp) {
                        e.trend = 1;
                    } else {
                        e.trend = 0;
                    }
                    break;
                case TourSerieMgr.SERIE_TOP:
                    // TOP : only down or stay
                    if (e.result >= thresholdResultDown && e.getNbTournamentPlayed() >= thresholdNbDown) {
                        e.trend = 0;
                    } else {
                        e.trend = -1;
                    }
                    break;
                case TourSerieMgr.SERIE_11:
                    // 11 : only up or maintain
                    if (e.result >= thresholdResultUp && e.getNbTournamentPlayed() >= thresholdNbUp) {
                        e.trend = 1;
                    } else {
                        e.trend = 0;
                    }
                    break;
                default:
                    if (e.result >= thresholdResultUp && e.getNbTournamentPlayed() >= thresholdNbUp) {
                        e.trend = 1;
                    } else if (e.result >= thresholdResultDown && e.getNbTournamentPlayed() >= thresholdNbDown) {
                        e.trend = 0;
                    } else {
                        e.trend = -1;
                    }
                    break;
            }
        }
    }
    /**
     * Compute the ranking for all player : first of all compute trend and next get list order trend and result
     */
    public void computeRanking() {
        // compute trend
        computeTrend();

        // get list order trend and result
        int curRank = 0;
        int curTrend = -1;
        int nbPlayerBefore = 0;
        double curResult = 0;
        List<Long> listKeyPla = getListPlayerOrderTrendAndResult();
        for (Long key : listKeyPla) {
            TourSerieMemPeriodRankingPlayer resPla = mapRankingPlayer.get(key);
            if (resPla != null && resPla.getNbTournamentPlayed() > 0) {
                // first element
                if (curRank == 0) {
                    resPla.rank = 1;
                    curRank = 1;
                    nbPlayerBefore = 1;
                    curResult = resPla.result;
                    curTrend = resPla.trend;
                } else {
                    // trend different => increment rank
                    if (resPla.trend != curTrend) {
                        curTrend = resPla.trend;
                        curResult = resPla.result;
                        curRank = nbPlayerBefore+1;
                    }
                    else {
                        // same trend and result different => increment rank
                        if (resPla.result != curResult) {
                            curResult = resPla.result;
                            curRank = nbPlayerBefore+1;
                        }
                    }
                    resPla.rank = curRank;
                    nbPlayerBefore++;
                }
            }
        }
    }

    /**
     * Return the list of player ordered by trend and result
     * @param offset
     * @param nbMax
     * @param listPlaFilter
     * @return
     */
    public List<TourSerieMemPeriodRankingPlayer> getRankingOrderTrendAndResult(int offset, int nbMax, List<Long> listPlaFilter, String countryCode) {
        List<TourSerieMemPeriodRankingPlayer> listResult = new ArrayList<>();
        List<Long> listPla = getListPlayerOrderTrendAndResult();
        if (countryCode != null && countryCode.length() > 0) {
            int nb = 0;
            for (Long e : listPla) {
                if (listResult.size() == nbMax) break;
                TourSerieMemPeriodRankingPlayer rankingPlayer = getPlayerRank(e);
                if (rankingPlayer.countryCode != null && rankingPlayer.countryCode.equals(countryCode)) {
                    if (nb >= offset) {
                        listResult.add(rankingPlayer);
                    }
                    nb++;
                }
            }
        }
        else {
            if (listPlaFilter != null &&  !listPlaFilter.isEmpty()) {
                List<Long> listPla2 = new ArrayList<>();
                for (Long e : listPlaFilter) {
                    if (listPla.contains(e)) {
                        listPla2.add(e);
                    }
                }
                listPla = listPla2;
            }
            int tempOffset = 0, tempNbMax = listPla.size();
            if (offset > 0) tempOffset = offset;
            if (nbMax > 0) tempNbMax = nbMax;
            for (int i = tempOffset; i < listPla.size(); i++) {
                if (listResult.size() == tempNbMax) break;
                listResult.add(getPlayerRank(listPla.get(i)));
            }
        }
        return listResult;
    }

    /**
     * Return the list of player ordered by trend and result
     * @param offset
     * @param nbMax
     * @param countryCode
     * @return
     */
    public List<TourSerieMemPeriodRankingPlayer> getRankingForCountry(int offset, int nbMax, String countryCode) {
        List<TourSerieMemPeriodRankingPlayer> listResult = new ArrayList<>();
        List<Long> listPla = getListPlayerOrderTrendAndResult();
        int nb = 0;
        for (Long e : listPla) {
            if (listResult.size() == nbMax) break;
            TourSerieMemPeriodRankingPlayer rankingPlayer = getPlayerRank(e);
            if (rankingPlayer.countryCode != null && rankingPlayer.countryCode.equals(countryCode)) {
                if (nb >= offset) {
                    listResult.add(rankingPlayer);
                }
                nb++;
            }
        }
        return listResult;
    }

    public List<TourSerieMemPeriodRankingPlayer> getAllResult() {
        return new ArrayList(mapRankingPlayer.values());
    }

    /**
     * count player with same country and best rank. if rank parameter is <= 0 -> return total nb player with same country
     * @param countryCode
     * @param rank
     * @param listPlaFilter
     * @return
     */
    public int countPlayerWithCountryAndBestRank(String countryCode, int rank, List<Long> listPlaFilter) {
        int nbPlayer = 0;
        List<Long> listPla = getListPlayerOrderTrendAndResult();
        if (listPlaFilter != null && !listPlaFilter.isEmpty()) {
            List<Long> listPla2 = new ArrayList<>();
            for (Long e : listPlaFilter) {
                if (listPla.contains(e)) {
                    listPla2.add(e);
                }
            }
            listPla = listPla2;
        }
        for (Long e : listPla) {
            TourSerieMemPeriodRankingPlayer rankingPlayer = getPlayerRank(e);
            if (rankingPlayer != null && rankingPlayer.countryCode != null && rankingPlayer.countryCode.equals(countryCode)) {
                if (rank > 0) {
                    if (rankingPlayer.rank < rank) {
                        nbPlayer++;
                    }
                } else {
                    nbPlayer++;
                }
            }
        }
        return nbPlayer;
    }
}
