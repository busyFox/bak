package com.funbridge.server.tournament.generic.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.game.Tournament;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import com.gotogames.common.lock.LockWeakString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pserent on 27/01/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class GenericMemTournament<TMemTourPlayer extends GenericMemTournamentPlayer,
        TMemDeal extends GenericMemDeal,
        TMemDealPlayer extends GenericMemDealPlayer> {
    public String tourID;
    public String name;
    public int engineVersion = 0;
    @JsonIgnore
    private Logger log = LogManager.getLogger(this.getClass());
    @JsonIgnore
    private LockWeakString lockAddPlayer = new LockWeakString();
    public int resultType;
    public long startDate;
    public long endDate;
    public long lastStartDate;
    // Map of ranking value for each playerID : plaID => rankingElement
    public Map<Long, TMemTourPlayer> tourPlayer = new ConcurrentHashMap<>();
    public Set<Long> listPlayerFinishAll = new HashSet<>();
    // array of resultDeal
    public TMemDeal[] deals;
    public int nbPlayerValid = 0, nbPlayerFinishAll = 0;
    public int nbMaxPlayers = 0;
    @JsonIgnore
    public boolean closeInProgress = false;
    @JsonIgnore
    public TournamentGenericMemoryMgr memoryMgr;

    public GenericMemTournament() {}

    public GenericMemTournament(Tournament tour, TournamentGenericMemoryMgr memoryMgr) {
        initData(tour, memoryMgr);
    }

    public TournamentGenericMemoryMgr getMemoryMgr() {
        return memoryMgr;
    }

    public void setMemoryMgr(TournamentGenericMemoryMgr memoryMgr) {
        this.memoryMgr = memoryMgr;
    }

    public String toString() {
        return "tourID="+tourID+" - startDate="+Constantes.timestamp2StringDateHour(startDate)+" - endDate="+Constantes.timestamp2StringDateHour(endDate)+" - nbDeal="+getNbDeal()+" - nbPlayer="+getNbPlayer()+" - nbPlayerFinishAll="+getNbPlayerFinishAll();
    }

    public void initData(Tournament tour, TournamentGenericMemoryMgr memoryMgr) {
        this.tourID = tour.getIDStr();
        this.name = tour.getName();
        this.resultType = tour.getResultType();
        this.startDate = tour.getStartDate();
        this.endDate = tour.getEndDate();
        this.memoryMgr = memoryMgr;
        deals = (TMemDeal[])Array.newInstance(memoryMgr.getClassEntityMemDeal(), tour.getNbDeals());
        for (int i = 0; i < deals.length; i++){
            try {
                deals[i] = (TMemDeal)memoryMgr.createMemDeal(this, i+1);
            } catch (Exception e) {
                log.error("Failed to create instance memDeal tour="+tour, e);
            }
        }
    }

    /**
     * Call after load memTour JSON file.
     */
    public void initAfterLoad() {
        for (GenericMemDeal d : deals) {
            d.setMemTour(this);
            d.computeScoreData();
            d.computeRanking();
        }
        for (GenericMemTournamentPlayer e : tourPlayer.values()) {
            e.memTour = this;
            if (e.isPlayerFinish()) {
                listPlayerFinishAll.add(e.playerID);
            }
        }

        computeResult();
        computeRanking(true);
    }

    public Logger getLog() {
        return log;
    }

    /**
     * Return the ranking of this player on tournament
     * @param playerID
     * @return
     */
    public GenericMemTournamentPlayer getTournamentPlayer(long playerID) {
        return tourPlayer.get(playerID);
    }

    /**
     * Return ranking for player on this tournament : existing or create a new one if not found
     * @param playerID
     * @return
     */
    public GenericMemTournamentPlayer getOrCreateTournamentPlayer(long playerID) {
        TMemTourPlayer plaRank = tourPlayer.get(playerID);
        if (plaRank == null) {
            plaRank = (TMemTourPlayer)memoryMgr.createMemTournamentPlayer(this);
            if (plaRank != null) {
                plaRank.playerID = playerID;
                plaRank.dateStart = System.currentTimeMillis();
                plaRank.dateLastPlay = System.currentTimeMillis();
                tourPlayer.put(playerID, plaRank);
            }
        }
        return plaRank;
    }

    public int getNbPlayerFinishAll() {
        return nbPlayerFinishAll;
    }

    /**
     * Return the number of player on this tournament. Only player how have played at least one deal
     * @return
     */
    public int getNbPlayer() {
        return nbPlayerValid;
    }

    /**
     * Return the number of deal
     * @return
     */
    public int getNbDeal() {
        return deals.length;
    }

    public int getNbDealsToPlay() {
        return deals.length;
    }

    /**
     * Return playerID list (brut)
     * @return
     */
    public List<Long> getListPlayer() {
        return new ArrayList<>(tourPlayer.keySet());
    }

    public List<TMemTourPlayer> getListMemTournamentPlayer() {
        return new ArrayList<>(tourPlayer.values());
    }

    /**
     * Return the WSResult on tournamentID for this player
     * @param playerCache
     * @return
     */
    public WSResultTournamentPlayer getWSResultPlayer(PlayerCache playerCache, boolean useRankFinished) {
        if (playerCache != null) {
            // Get ranking player
            GenericMemTournamentPlayer tournamentPlayer = getTournamentPlayer(playerCache.ID);
            if (tournamentPlayer != null) {
                WSResultTournamentPlayer res = tournamentPlayer.toWSResultTournamentPlayer(useRankFinished);
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

    /**
     * Return the current deal for this player
     * @param playerID
     * @return
     */
    public int getCurrentDealForPlayer(long playerID) {
        // Get ranking player
        GenericMemTournamentPlayer tournamentPlayer = getTournamentPlayer(playerID);
        if (tournamentPlayer != null) {
            return tournamentPlayer.currentDealIndex;
        }
        return -1;
    }

    /**
     * Return the memory result for deal
     * @param dealID
     * @return
     */
    public GenericMemDeal getMemDeal(String dealID) {
        for (int i = 0; i < deals.length; i++) {
            if (deals[i] != null && deals[i].dealID.equals(dealID)) {
                return deals[i];
            }
        }
        return null;
    }

    /**
     * Add game result for a player
     * @param game
     * @param computeResultRanking
     * @param finishProcess true if call by finish process
     * @return
     */
    public GenericMemTournamentPlayer addResult(Game game, boolean computeResultRanking, boolean finishProcess) {
        GenericMemDeal resultDeal = getMemDeal(game.getDealID());
        // a result exist for this deal
        if (resultDeal != null) {
            // add result of player for this deal
            long ts = System.currentTimeMillis();
            GenericMemDealPlayer resDealPla = resultDeal.setResultPlayer(game);
            if (resDealPla != null) {
                TMemTourPlayer resTourPla = tourPlayer.get(game.getPlayerID());
                if (resTourPla == null) {
                    resTourPla = (TMemTourPlayer)memoryMgr.createMemTournamentPlayer(this);
                    resTourPla.playerID = game.getPlayerID();
                    resTourPla.dateStart = game.getStartDate();
                    tourPlayer.put(game.getPlayerID(), resTourPla);
                }
                resTourPla.addPlayedDeal(resultDeal.dealID);
                if (!finishProcess && game.getLastDate() < endDate) {
                    resTourPla.dateLastPlay = game.getLastDate();
                }

                // first deal played by this player => increment nb player valid on tournament
                if (resTourPla.getNbPlayedDeals() == 1) {
                    nbPlayerValid++;
                }
                // player played all deal ? => increment nb player finish all
                boolean computeFinished = false;
                if (resTourPla.getNbPlayedDeals() == getNbDealsToPlay()) {
                    nbPlayerFinishAll++;
                    computeFinished = listPlayerFinishAll.add(game.getPlayerID());
                }

                // update result & ranking only if method param is set
                if (computeResultRanking) {
                    // update result for all player
                    computeResult();

                    // update ranking of tournament
                    computeRanking(computeFinished);
                }

                return resTourPla;
            } else {
                log.error("Set result player failed - game="+game);
            }
        } else {
            log.error("No result deal found for deal - game="+game);
        }
        return null;
    }

    /**
     * Compute result on tournament for all players
     */
    public void computeResult() {
        for (GenericMemTournamentPlayer tp : tourPlayer.values()) {
            List<Double> listResultPla = getListResultForPlayer(tp.playerID);
            if (listResultPla != null && !listResultPla.isEmpty()) {
                double tempVal = 0;
                for (final Double resultPla : listResultPla) {
                    tempVal += resultPla;
                }
                if (resultType == Constantes.TOURNAMENT_RESULT_IMP) {
                    tp.result = tempVal;
                } else if (resultType == Constantes.TOURNAMENT_RESULT_PAIRE) {
                    tp.result = tempVal / listResultPla.size();
                }
            } else {
                tp.result = 0;
            }
        }
    }

    /**
     * Return the list of player order by result (Best result at first)
     * @return
     */
    public List<Long> getListPlayerOrderResult() {
        List<Long> listKeyPla = new ArrayList<>(tourPlayer.keySet());
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                GenericMemTournamentPlayer temp1 = tourPlayer.get(o1);
                GenericMemTournamentPlayer temp2 = tourPlayer.get(o2);
                if (!temp1.hasPlayedOneDeal() && !temp2.hasPlayedOneDeal()) {
                    return 0;
                }
                if (!temp1.hasPlayedOneDeal()) {
                    return -1;
                }
                if (!temp2.hasPlayedOneDeal()) {
                    return 1;
                }
                // result of this object is greater so ranking is smaller ! Big result => ranking small
                if (temp2.result > temp1.result) {
                    return 1;
                } else if (temp2.result == temp1.result) {
                    return 0;
                }
                return -1;
            }
        });
        return listKeyPla;
    }

    /**
     * Return the list of player order by result with only players who finish the tournament
     * @return
     */
    public List<Long> getListPlayerOrderFinishedResult() {
        List<Long> listKeyPla = new ArrayList<>(listPlayerFinishAll);
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                GenericMemTournamentPlayer temp1 = tourPlayer.get(o1);
                GenericMemTournamentPlayer temp2 = tourPlayer.get(o2);
                // result of this object is greater so ranking is smaller ! Big result => ranking small
                if (temp2.result > temp1.result) {
                    return 1;
                } else if (temp2.result == temp1.result) {
                    return 0;
                }
                return -1;
            }
        });
        return listKeyPla;
    }

    /**
     * Compute the ranking for players : ranking order result
     * @param computeFinished flag to compute rankingFinished too
     * @return likst of playerID order by result
     */
    public List<Long> computeRanking(boolean computeFinished) {
        List<Long> listKeyPla = getListPlayerOrderResult();
        int curRank = 0;
        int nbPlayerBefore = 0;
        double curResult = 0;
        int nbPlayerFinishedBefore = 0;
        for (Long key : listKeyPla) {
            GenericMemTournamentPlayer e = tourPlayer.get(key);
            if (e.hasPlayedOneDeal()) {
                // fisrt on rank
                if (curRank == 0) {
                    curRank = 1;
                    e.ranking = 1;
                    nbPlayerBefore = 1;
                    curResult = e.result;
                } else {
                    if (e.result != curResult) {
                        curResult = e.result;
                        curRank = nbPlayerBefore+1;
                    }
                    e.ranking = curRank;
                    nbPlayerBefore++;
                }
                // update the number of player finished with best result
                e.nbPlayerFinishWithBestResult = nbPlayerFinishedBefore;
                if (e.isPlayerFinish()) {
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
     * Compute the ranking with only players who finish all deals of tournament
     */
    public void computeRankingFinished() {
        // ranking order result
        List<Long> listKeyPla = getListPlayerOrderFinishedResult();
        int curRank = 0;
        int nbPlayerBefore = 0;
        double curResult = 0;
        for (Long key : listKeyPla) {
            GenericMemTournamentPlayer e = tourPlayer.get(key);
            if (e.isPlayerFinish()) {
                // first on rank
                if (curRank == 0) {
                    curRank = 1;
                    e.rankingFinished = 1;
                    nbPlayerBefore = 1;
                    curResult = e.result;
                } else {
                    if (e.result != curResult) {
                        curResult = e.result;
                        curRank = nbPlayerBefore + 1;
                    }
                    e.rankingFinished = curRank;
                    nbPlayerBefore++;
                }
            }
        }
    }

    /**
     * Return the list of result value for a player for all deal of this tournament
     * @param plaID
     * @return
     */
    public List<Double> getListResultForPlayer(long plaID) {
        List<Double> listResult = new ArrayList<>();
        for (GenericMemDeal memDeal : deals) {
            GenericMemDealPlayer dealPlayer = memDeal.getResultPlayer(plaID);
            if (dealPlayer != null) {
                listResult.add(dealPlayer.result);
            }
        }
        return listResult;
    }

    /**
     * Compute the result on a deal for player
     * @param resultDeal
     * @param resultPlayer
     * @return
     */
    protected double computeResultDealPlayer(GenericMemDeal resultDeal, GenericMemDealPlayer resultPlayer) {
        if (resultDeal != null && resultPlayer != null) {
            if (resultType == Constantes.TOURNAMENT_RESULT_IMP) {
                return Constantes.computeResultIMP(resultDeal.getScoreAverage(), resultPlayer.score);
            }
            if (resultType == Constantes.TOURNAMENT_RESULT_PAIRE) {
                if (resultPlayer.score == Constantes.GAME_SCORE_LEAVE && FBConfiguration.getInstance().getIntValue("general.resultLeave0", 0) == 1) {
                    return 0;
                }
                int nbScore = resultDeal.nbScore;
                return Constantes.computeResultPaire(nbScore, resultPlayer.nbPlayerBetterScore, resultPlayer.nbPlayerSameScore);
            }
        }
        return 0;
    }

    /**
     * Return a ranking in this tournament at offset with nb max element in list
     * @param offset
     * @param nbMax max value in list result or -1 to have all
     * @param listPlaFilter
     * @param useRankingFinished
     * @return
     */
    public List<TMemTourPlayer> getRanking(int offset, int nbMax, List<Long> listPlaFilter, boolean useRankingFinished) {
        List<TMemTourPlayer> listRanking = new ArrayList<>();
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
            for (int i=0; i < listKeyPla.size(); i++) {
                if (nbMax >0 && listRanking.size() >= nbMax) {
                    break;
                }
                TMemTourPlayer playerResult = tourPlayer.get(listKeyPla.get(i));
                // at least one deal played !
                if (playerResult != null && playerResult.getNbPlayedDeals() > 0) {
                    nbElemValid++;
                    if (nbElemValid > offset) {
                        listRanking.add(playerResult);
                    }
                }
            }
        }
        return listRanking;
    }

    /**
     * Add this player to tournament. The player has just started the tournament.
     * @param playerID
     * @param dateStart
     */
    public void addPlayer(long playerID, long dateStart) {
        TMemTourPlayer memTourPla = tourPlayer.get(playerID);
        if (memTourPla == null) {
            memTourPla = (TMemTourPlayer)memoryMgr.createMemTournamentPlayer(this);
            memTourPla.playerID = playerID;
            memTourPla.dateStart = dateStart;
            tourPlayer.put(playerID, memTourPla);
        }
    }

    public boolean hasPlayerFinish(long playerID) {
        return listPlayerFinishAll.contains(playerID);
    }

    public void removeResultForPlayer(List<Long> listPlayerID) {
        for (GenericMemDeal memDeal : deals) {
            for (Long e : listPlayerID) {
                memDeal.removeResultPlayer(e);
            }
        }
        for (Long e : listPlayerID) {
            GenericMemTournamentPlayer mtp = tourPlayer.remove(e);
            listPlayerFinishAll.remove(e);
            if (mtp != null) {
                if (mtp.isPlayerFinish()) {
                    nbPlayerFinishAll--;
                }
                if (mtp.getNbPlayedDeals() >= 1) {
                    nbPlayerValid--;
                }
            }
        }
        computeResult();
        computeRanking(true);
    }

    public boolean isOpen(){
        return !(lastStartDate > 0 && System.currentTimeMillis() > (this.lastStartDate));
    }

    public boolean isFull(){
        return(nbMaxPlayers > 0 && tourPlayer.size() >= nbMaxPlayers);
    }
}
