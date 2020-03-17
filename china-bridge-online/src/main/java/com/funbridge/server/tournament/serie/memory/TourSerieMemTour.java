package com.funbridge.server.tournament.serie.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import com.funbridge.server.tournament.serie.data.TourSerieGame;
import com.funbridge.server.tournament.serie.data.TourSerieGameLoadData;
import com.funbridge.server.tournament.serie.data.TourSerieTournament;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import com.funbridge.server.ws.tournament.WSTournament;
import com.gotogames.common.lock.LockWeakString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pserent on 09/06/2014.
 * Bean to store in memory the tournament result
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourSerieMemTour extends GenericMemTournament<TourSerieMemTourPlayer,
        TourSerieMemDeal,
        TourSerieMemDealPlayer> {
    public String serie;
    public String periodID;
    // MAP playerID => tour ranking
    public Map<Long, TourSerieMemTourPlayer> ranking = new ConcurrentHashMap<>();

    public Set<Long> listPlayerForRanking = new HashSet<>();

    @JsonIgnore
    private LockWeakString lockAddPlayer = new LockWeakString();
    @JsonIgnore
    private Logger log = LogManager.getLogger(this.getClass());

    public TourSerieMemTour() {}

    public TourSerieMemTour(String tourID, String serie, String periodID, int nbDeal) {
        this.tourID = tourID;
        this.serie = serie;
        this.periodID = periodID;
        deals = new TourSerieMemDeal[nbDeal];
        for (int i = 0; i < nbDeal; i++){
            deals[i] = new TourSerieMemDeal(this, i+1);
        }
    }

    public void initAfterLoadJSON() {
        for (TourSerieMemDeal d : deals) {
            d.setMemTour(this);
        }
        boolean updatePlayerData = ContextManager.getTourSerieMgr().getConfigIntValue("updateDataPlayOnMemTourLoad", 0) == 1;
        TourSerieMemPeriodRanking serieRanking = null;
        if (updatePlayerData) {
            serieRanking = ContextManager.getTourSerieMgr().getSerieRanking(serie);
        }
        for (TourSerieMemTourPlayer e : ranking.values()) {
            if (e.getNbDealsPlayed() >= 1) {
                listPlayerForRanking.add(e.playerID);
            }
            if (e.getNbDealsPlayed() == deals.length) {
                listPlayerFinishAll.add(e.playerID);
                // update tour result data for player
                if (updatePlayerData && serieRanking != null) {
                    TourSerieMemPeriodRankingPlayer rankingPlayer = serieRanking.getPlayerRank(e.playerID);
                    if (rankingPlayer != null) {
                        rankingPlayer.setTourResult(tourID, e.result, e.rankingFinished, e.dateLastPlay, true);
                    }
                }
            } else {
                // remove tour result data for player
                if (updatePlayerData && serieRanking != null) {
                    TourSerieMemPeriodRankingPlayer rankingPlayer = serieRanking.getPlayerRank(e.playerID);
                    if (rankingPlayer != null) {
                        TourSerieMemPeriodRankingPlayerData data = rankingPlayer.getResultForTournament(tourID);
                        if (data != null) {
                            rankingPlayer.listData.remove(data);
                        }
                    }
                }
            }
        }
        computeResult();
        computeRanking(true);
    }

    @Override
    public String toString() {
        return "serie="+serie+" - tourID="+tourID+" - periodID="+periodID+" - nbDeals="+getNbDeals()+" - nbPlayer="+getNbPlayers()+" - nbPlayerFinish="+getNbPlayersFinish();
    }

    public int getNbDeals() {
        return deals.length;
    }

    public TourSerieMemTour(TourSerieTournament tour) {
        this(tour.getIDStr(), tour.getSerie(), tour.getPeriod(), tour.getNbDeals());
    }

    /**
     * Return ranking for player on this tournament : existing or create a new one if not found
     * @param playerID
     * @return
     */
    public TourSerieMemTourPlayer getOrCreateRankingPlayer(long playerID) {
        synchronized (lockAddPlayer.getLock(""+playerID)) {
            TourSerieMemTourPlayer plaRank = ranking.get(playerID);
            if (plaRank == null) {
                plaRank = new TourSerieMemTourPlayer();
                plaRank.playerID = playerID;
                plaRank.dateStartPlay = System.currentTimeMillis();
                plaRank.dateLastPlay = System.currentTimeMillis();
                ranking.put(playerID, plaRank);
            }
            return plaRank;
        }
    }

    /**
     * Return the ranking of this player on tournament
     * @param playerID
     * @return
     */
    public TourSerieMemTourPlayer getRankingPlayer(long playerID) {
        return ranking.get(playerID);
    }

    /**
     * Return the number of players on tournament (players start and finish it)
     * @return
     */
    public int getNbPlayers() {
        return ranking.size();
    }

    public int getNbPlayersForRanking() {
        return listPlayerForRanking.size();
    }

    /**
     * Return the number of players finish tournament : all deals played
     * @return
     */
    public int getNbPlayersFinish() {
        return listPlayerFinishAll.size();
    }

    public int getNbPlayers(List<Long> listPlaID, boolean onlyFinisher) {
        if (listPlaID != null && !listPlaID.isEmpty()) {
            int nbPlayer = 0;
            for (TourSerieMemTourPlayer e : ranking.values()) {
                if (!listPlaID.contains(e.playerID)) {
                    continue;
                }
                if (onlyFinisher) {
                    if (e.getNbDealsPlayed() == getNbDeals()) {
                        nbPlayer++;
                    }
                } else {
                    if (e.getNbDealsPlayed() >= 1) {
                        nbPlayer++;
                    }
                }
            }
            return nbPlayer;
        } else {
            if (onlyFinisher) {
                return getNbPlayersFinish();
            }
            return getNbPlayersForRanking();
        }
    }

    /**
     * Return the array of dealID on this tournament
     * @return
     */
    public String[] getArrayDealID() {
        String[] result = new String[deals.length];
        for (int i = 0; i <  deals.length; i++) {
            result[i] = deals[i].dealID;
        }
        return result;
    }

    /**
     * Return the WSResult on tournamentID for this player
     * @param playerCache
     * @param useRankingFinished
     * @return
     */
    @Override
    public WSResultTournamentPlayer getWSResultPlayer(PlayerCache playerCache, boolean useRankingFinished) {
        if (playerCache != null) {
            // Get ranking player
            TourSerieMemTourPlayer rankingPla = getRankingPlayer(playerCache.ID);
            if (rankingPla != null) {
                WSResultTournamentPlayer res = rankingPla.toWSResultTournamentPlayer();
                int nbTotalPlayer = getNbPlayersForRanking();
                if (useRankingFinished) {
                    nbTotalPlayer = getNbPlayersFinish();
                    if (res.getNbDealPlayed() == getNbDeals()) {
                        res.setRank(rankingPla.rankingFinished);
                    } else {
                        if (getNbPlayersFinish() > 0) {
                            res.setRank(rankingPla.nbPlayerFinishWithBestResult+1);
                            nbTotalPlayer++;
                        } else {
                            res.setRank(-1);
                        }
                    }
                } else {
                    res.setRank(rankingPla.ranking);
                }
                res.setNbTotalPlayer(nbTotalPlayer);
                res.setPlayerID(playerCache.ID);
                res.setPlayerPseudo(playerCache.getPseudo());
                res.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(playerCache.ID));
                res.setAvatarPresent(playerCache.avatarPresent);
                res.setCountryCode(playerCache.countryCode);
                res.setPlayerSerie(playerCache.serie);
                return res;
            }
        }
        return null;
    }

    @Override
    public int getCurrentDealForPlayer(long playerID) {
        // Get ranking player
        TourSerieMemTourPlayer rankingPla = getRankingPlayer(playerID);
        if (rankingPla != null) {
            return rankingPla.currentDealIndex;
        }
        return -1;
    }

    /**
     * Return the result for a deal
     * @param dealID
     * @return
     */
    public TourSerieMemDeal getResultDeal(String dealID) {
        for (int i = 0; i < deals.length; i++) {
            if (deals[i] != null && deals[i].dealID.equals(dealID)) {
                return deals[i];
            }
        }
        return null;
    }

    /**
     * Return the memDeal for deal at index
     * @param dealIndex 1 <= idx <= nbDeal
     * @return
     */
    private TourSerieMemDeal getResultDeal(int dealIndex) {
        if (dealIndex >= 1 && dealIndex <= deals.length) {
            return deals[dealIndex-1];
        }
        return null;
    }

    public boolean removeResult(int dealIndex, long playerID) {
        TourSerieMemTourPlayer plaRank = getRankingPlayer(playerID);
        if (plaRank != null) {
            TourSerieMemDeal memDeal = getResultDeal(dealIndex);
            if (memDeal != null) {
                if (memDeal.removeResultPlayer(playerID)) {
                    plaRank.removeDealPlayed(memDeal.dealID);
                    if (plaRank.getNbDealsPlayed() == 0) {
                        // remove player from ranking
                        ranking.remove(playerID);
                    }
                    listPlayerForRanking.remove(playerID);
                    boolean computeFinished = listPlayerFinishAll.remove(playerID);
                    computeResult();
                    computeRanking(computeFinished);
                    return true;
                } else {
                    log.error("No result to remove for playerID=" + playerID + " - dealIndex=" + dealIndex);
                }
            } else {
                log.error("No memDeal found for this dealIndex=" + dealIndex);
            }
        } else {
            log.error("No player rank found on this tournament for playerID="+playerID+" - tourID="+tourID);
        }
        return false;
    }

    public TourSerieMemTourPlayer loadResult(TourSerieGameLoadData gameData) {
        if (gameData != null) {
            if (gameData.finished) {
                TourSerieMemDeal resultDeal = getResultDeal(gameData.dealIndex);
                if (resultDeal != null) {
                    // add result of player for this deal
                    TourSerieMemDealPlayer resDealPla = resultDeal.setResultPlayerFromLoadData(gameData);

                    if (resDealPla != null) {
                        TourSerieMemTourPlayer plaRank = getOrCreateRankingPlayer(gameData.playerID);
                        plaRank.addDealPlayed(resultDeal.dealID);
                        plaRank.dateLastPlay = gameData.lastDate;
                        listPlayerForRanking.add(gameData.playerID);
                        if (plaRank.getNbDealsPlayed() == deals.length) {
                            listPlayerFinishAll.add(gameData.playerID);
                        }
                        return plaRank;
                    } else {
                        log.error("Failed on loadResult - tourID=" + tourID + " - gameData=" + gameData);
                    }
                } else {
                    log.error("No result deal found - gameData=" + gameData + " - tourID=" + tourID);
                }
            } else {
                TourSerieMemTourPlayer mtp = getOrCreateRankingPlayer(gameData.playerID);
                mtp.currentDealIndex = gameData.dealIndex;
                return mtp;
            }
        }
        return null;
    }

    /**
     * Add a result
     * @param game
     * @param computeResultRanking if true, the result and ranking are computed else the result and ranking are set with value from game object
     * @return true if deal exist and result for a player is added with success
     */
    public TourSerieMemTourPlayer addResult(TourSerieGame game, boolean computeResultRanking) {
        if (game != null && game.getTournament().getIDStr().equals(tourID) && game.getDealIndex() <= deals.length) {
            TourSerieMemDeal resultDeal = getResultDeal(game.getDealIndex());
            // a result exist for this deal
            if (resultDeal != null) {
                // add result of player for this deal
                TourSerieMemDealPlayer resDealPla = resultDeal.setResultPlayer(game);

                if (resDealPla != null) {
                    TourSerieMemTourPlayer plaRank = getOrCreateRankingPlayer(game.getPlayerID());
                    plaRank.addDealPlayed(resultDeal.dealID);
                    plaRank.dateLastPlay = game.getLastDate();
                    boolean computeFinished = false;
                    listPlayerForRanking.add(game.getPlayerID());
                    if (plaRank.getNbDealsPlayed() == deals.length) {
                        computeFinished = listPlayerFinishAll.add(game.getPlayerID());
                    }

                    // update result & ranking only if method param is set
                    if (computeResultRanking) {
                        // update result for all player
                        computeResult();

                        // update ranking of tournament
                        computeRanking(computeFinished);
                    }

                    return plaRank;
                } else {
                    log.error("Failed on setResultPlayer for player - tourID=" + tourID + " - game=" + game);
                }
            } else {
                log.error("No result deal found - game=" + game + " - tourID=" + tourID);
            }
        } else {
            log.error("Parameter no valid ... game="+game+" - tourID="+tourID);
        }
        return null;
    }

    public TourSerieMemTourPlayer addResultGameNotPlayed(TourSerieMemDeal memDeal, long playerID) {
        if (memDeal != null) {
            // add result of player for this deal
            TourSerieMemDealPlayer resDealPla = memDeal.setResultPlayerNotPlayed(playerID);

            if (resDealPla != null) {
                TourSerieMemTourPlayer plaRank = getOrCreateRankingPlayer(playerID);
                plaRank.addDealPlayed(memDeal.dealID);
                listPlayerForRanking.add(playerID);
                if (plaRank.getNbDealsPlayed() == deals.length) {
                    listPlayerFinishAll.add(playerID);
                }
                return plaRank;
            } else {
                log.error("Failed on setResultPlayer for player - tourID=" + tourID);
            }
        } else {
            log.error("Parameter no valid ... memDeal="+memDeal+" - tourID="+tourID);
        }
        return null;
    }

    /**
     * Return the list of result deal for player on tournament.
     * @param playerID
     * @return
     */
    @Override
    public List<Double> getListResultForPlayer(long playerID) {
        List<Double> listResult = new ArrayList<>();
        for (TourSerieMemDeal memDeal : deals) {
            TourSerieMemDealPlayer resDealPla = memDeal.getResultPlayer(playerID);
            if (resDealPla != null) {
                listResult.add(resDealPla.result);
            } else {
                break;
            }
        }
        return listResult;
    }

    /**
     * Compute result on tournament for all players
     */
    @Override
    public void computeResult() {
        for (TourSerieMemTourPlayer tourRankPla : ranking.values()) {
            // get list of all result for player and computer the average
            List<Double> listResultPla = getListResultForPlayer(tourRankPla.playerID);
            if (listResultPla != null && !listResultPla.isEmpty()) {
                double tempVal = 0;
                for (final Double resultPla : listResultPla) {
                    tempVal += resultPla;
                }
                tourRankPla.result = tempVal / listResultPla.size();
            } else {
                tourRankPla.result = 0;
            }
        }
    }

    /**
     * Return playerID list ordered by result
     * @return
     */
    @Override
    public List<Long> getListPlayerOrderResult() {
        List<Long> listKeyPla = new ArrayList<>(ranking.keySet());
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                TourSerieMemTourPlayer temp1 = ranking.get(o1);
                TourSerieMemTourPlayer temp2 = ranking.get(o2);
                return temp1.compareTo(temp2);
            }
        });
        return listKeyPla;
    }

    /**
     * Return the list of player with at first players who finish the tournament
     * @return
     */
    @Override
    public List<Long> getListPlayerOrderFinishedResult() {
        List<Long> listKeyPla = new ArrayList<>(listPlayerFinishAll);
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                TourSerieMemTourPlayer e1 = ranking.get(o1);
                if (e1 == null) {
                    return -1;
                }
                TourSerieMemTourPlayer e2 = ranking.get(o2);
                if (e2 == null) {
                    return 1;
                }
                return e1.compareTo(e2);
            }
        });
        return listKeyPla;
    }

    /**
     * Return playerID list (brut)
     * @return
     */
    @Override
    public List<Long> getListPlayer() {
        return new ArrayList<>(ranking.keySet());
    }

    /**
     * Return list of ranking (brut)
     * @return
     */
    public List<TourSerieMemTourPlayer> getListRanking() {
        return new ArrayList<>(ranking.values());
    }

    /**
     * Return list of ranking only player finished
     * @return
     */
    public List<TourSerieMemTourPlayer> getListRankingOnlyFinished() {
        List<TourSerieMemTourPlayer> listRanking = new ArrayList<>();
        if (listPlayerFinishAll != null && !listPlayerFinishAll.isEmpty()) {
            for (Long l : listPlayerFinishAll) {
                TourSerieMemTourPlayer mtr = ranking.get(l);
                // at least one deal played !
                if (mtr != null && mtr.getNbDealsPlayed() == getNbDeals()) {
                    listRanking.add(mtr);
                }
            }
        }
        return listRanking;
    }

    public String getPeriodID(){
        return this.periodID;
    }

    /**
     * Order the list of player on tournament with the result and update the ranking
     * @param computeFinished compute the ranking for finisher player
     * @return
     */
    @Override
    public List<Long> computeRanking(boolean computeFinished) {
        List<Long> listKeyPla = getListPlayerOrderResult();
        // do the original process if list null
        int curRank = 0;
        int nbPlayerBefore = 0;
        double curResult = 0;
        int nbPlayerFinishedBefore = 0;
        for (Long key : listKeyPla) {
            TourSerieMemTourPlayer rankingElem = ranking.get(key);
            if (rankingElem != null && rankingElem.getNbDealsPlayed() > 0) {
                // fisrt on rank
                if (curRank == 0) {
                    curRank = 1;
                    rankingElem.ranking = 1;
                    nbPlayerBefore = 1;
                    curResult = rankingElem.result;
                } else {
                    if (rankingElem.result != curResult) {
                        curResult = rankingElem.result;
                        curRank = nbPlayerBefore+1;
                    }
                    rankingElem.ranking = curRank;
                    nbPlayerBefore++;
                }
                // update the number of player finished with best result
                rankingElem.nbPlayerFinishWithBestResult = nbPlayerFinishedBefore;
                if (rankingElem.getNbDealsPlayed() == getNbDeals()) {
                    nbPlayerFinishedBefore++;
                }
            }
        }

        if (computeFinished) {
            computeRankingFinished();
        }
        return listKeyPla;
    }

    /**
     * Compute the ranking with at first players who finish all deals of tournament
     * @return
     */
    @Override
    public void computeRankingFinished() {
        List<Long> listKeyPla = getListPlayerOrderFinishedResult();
        int curRank = 0;
        int nbPlayerBefore = 0;
        double curResult = 0;
        for (Long key : listKeyPla) {
            TourSerieMemTourPlayer rankingElem = ranking.get(key);

            if (rankingElem != null) {
                // first on rank
                if (curRank == 0) {
                    curRank = 1;
                    rankingElem.rankingFinished = 1;
                    nbPlayerBefore = 1;
                    curResult = rankingElem.result;
                } else {
                    if (rankingElem.result != curResult) {
                        curResult = rankingElem.result;
                        curRank = nbPlayerBefore + 1;
                    }
                    rankingElem.rankingFinished = curRank;
                    nbPlayerBefore++;
                }
            }
        }
    }

    /**
     * Return a ranking in this tournament at offset with nb max element in list
     * @param offset
     * @param nbMax max value in list result or -1 to have all
     * @param listPlaFilter
     * @param useRankingFinished
     * @return
     */
    @Override
    public List<TourSerieMemTourPlayer> getRanking(int offset, int nbMax, List<Long> listPlaFilter, boolean useRankingFinished) {
        List<TourSerieMemTourPlayer> listRanking = new ArrayList<>();
        // sort ranking
        List<Long> listKeyPla = null;
        if (useRankingFinished) {
            listKeyPla = getListPlayerOrderFinishedResult();
        } else {
            listKeyPla = getListPlayerOrderResult();
        }
        if (listPlaFilter != null) {
            List<Long> listPla2 = new ArrayList<>();
            for (Long e : listKeyPla) {
                if (listPlaFilter.contains(e)) {
                    listPla2.add(e);
                }
            }
            listKeyPla = listPla2;
        }
        // return a list of ranking
        if (listKeyPla.size() > offset) {
            int nbElemValid = 0;
            for (final Long keyPla : listKeyPla) {
                if (nbMax >0 && listRanking.size() >= nbMax) {
                    break;
                }
                TourSerieMemTourPlayer mtr = ranking.get(keyPla);
                // at least one deal played !
                if (mtr != null && mtr.getNbDealsPlayed() > 0) {
                    nbElemValid++;
                    if (nbElemValid > offset) {
                        listRanking.add(mtr);
                    }
                }
            }
        }
        return listRanking;
    }

    public WSTournament toWSTournament() {
        WSTournament wst = new WSTournament();
        wst.tourIDstr = tourID;
        wst.categoryID = Constantes.TOURNAMENT_CATEGORY_NEWSERIE;
        wst.beginDate = TourSerieMgr.transformPeriodID2TS(periodID, true);
        wst.endDate = TourSerieMgr.transformPeriodID2TS(periodID, false);
        wst.periodID = wst.beginDate+";"+wst.endDate;
        wst.countDeal = getNbDeals();
        wst.name = serie;
        wst.resultType = Constantes.TOURNAMENT_RESULT_PAIRE;
        wst.nbTotalPlayer = getNbPlayersFinish();
        wst.arrayDealIDstr = new String[deals.length];
        for (int i=0; i < deals.length; i++) {
            wst.arrayDealIDstr[i] = deals[i].dealID;
        }
        return wst;
    }
}
