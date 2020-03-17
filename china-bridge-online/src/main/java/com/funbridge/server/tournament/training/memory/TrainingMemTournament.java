package com.funbridge.server.tournament.training.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.training.data.TrainingGame;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import com.gotogames.common.lock.LockWeakString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by bplays on 21/04/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TrainingMemTournament {
    public String tournamentID;
    public int resultType;
    public long startDate;
    public long lastStartDate;
    public long endDate;
    public long nbMaxPlayers = 0;
    public TrainingMemDeal[] deals;
    public Map<Long, TrainingMemTournamentPlayer> ranking = new ConcurrentHashMap<>();
    public int nbValidPlayers = 0;
    public Set<Long> playersWhoFinished = new HashSet<>();
    @JsonIgnore
    private LockWeakString lockAddPlayer = new LockWeakString();
    @JsonIgnore
    private Logger log = LogManager.getLogger(this.getClass());
    @JsonIgnore
    public boolean closeInProgress = false;

    public TrainingMemTournament(){}

    public TrainingMemTournament(String tournamentID, int resultType, long startDate, long endDate, long lastStartDate, long nbMaxPlayers, int nbDeals){
        this.tournamentID = tournamentID;
        this.resultType = resultType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.lastStartDate = lastStartDate;

        this.nbMaxPlayers = nbMaxPlayers;
        this.deals = new TrainingMemDeal[nbDeals];
        for(int i=0; i<nbDeals; i++){
            this.deals[i] = new TrainingMemDeal(this, i+1);
        }
        if (this.lastStartDate < this.startDate || this.lastStartDate > this.endDate) {
            this.lastStartDate = this.endDate;
        }

    }

    /**
     * Call after load memTour JSON file.
     */
    public void initAfterLoad() {
        for (TrainingMemDeal d : deals) {
            d.setMemTournament(this);
            d.computeScoreData();
            d.computeRanking();
        }
        nbValidPlayers = 0;
        for (TrainingMemTournamentPlayer e : ranking.values()) {
            e.setMemTournament(this);
            if (e.getNbPlayedDeals() >= 1) {
                nbValidPlayers++;
            }
            if (e.hasFinished()) {
                playersWhoFinished.add(e.getPlayerID());
            }
        }

        computeResult();
        computeRanking(true);
        lastStartDate = startDate + (((long)ContextManager.getTrainingMgr().getConfigIntValue("durationLast", 2820))*60*1000);
        if (this.lastStartDate < this.startDate || this.lastStartDate > this.endDate) {
            this.lastStartDate = this.endDate;
        }
    }

    public Logger getLog(){ return this.log; }

    public boolean isOpen(){
        return System.currentTimeMillis() <= (this.lastStartDate);
    }

    public boolean isFull(){
        return ranking.size() >= nbMaxPlayers;
    }

    /**
     * Return the number of deal
     * @return
     */
    public int getNbDeals() {
        return deals.length;
    }

    /**
     * Add this player to tournament. The player has just started the tournament.
     * @param playerID
     * @param dateStart
     */
    public void addPlayer(long playerID, long dateStart) {
        TrainingMemTournamentPlayer memTourPla = ranking.get(playerID);
        if (memTourPla == null) {
            memTourPla = new TrainingMemTournamentPlayer(this);
            memTourPla.setPlayerID(playerID);
            memTourPla.setStartDate(dateStart);
            ranking.put(playerID, memTourPla);
        }
    }

    public TrainingMemTournamentPlayer addResult(TrainingGame game, boolean computeResultRanking){
        TrainingMemDeal memDeal = getMemDeal(game.getDealID());
        // a result exist for this deal
        if (memDeal != null) {
            // add result of player for this deal
            TrainingMemDealPlayer resDealPla = memDeal.setResultForPlayer(game);

            if (resDealPla != null) {
                TrainingMemTournamentPlayer resTourPla = ranking.get(game.getPlayerID());
                if (resTourPla == null) {
                    resTourPla = new TrainingMemTournamentPlayer(this);
                    resTourPla.setPlayerID(game.getPlayerID());
                    resTourPla.setStartDate(game.getStartDate());
                    ranking.put(game.getPlayerID(), resTourPla);
                }
                resTourPla.addPlayedDeal(memDeal.getDealID());
                resTourPla.setLastPlayDate(game.getLastDate());

                // first deal played by this player => increment nb player valid on tournament
                if (resTourPla.getNbPlayedDeals() == 1) {
                    nbValidPlayers++;
                }
                // player played all deal ? => increment nb player finish all
                boolean computeFinisherRanking = false;
                if (resTourPla.getNbPlayedDeals() == deals.length) {
                    computeFinisherRanking = playersWhoFinished.add(game.getPlayerID());
                }

                // update result & ranking only if method param is set
                if (computeResultRanking) {
                    // update result for all player
                    computeResult();

                    // update ranking of tournament
                    computeRanking(computeFinisherRanking);
                }
                else {
                    resTourPla.setResult(game.getResult());
                    resTourPla.setRanking(game.getRank());
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

    public TrainingMemTournamentPlayer addNotPlayedResult(TrainingMemDeal memDeal, long playerID){
        if(memDeal != null){
            // Add a result for this player on this deal
            TrainingMemDealPlayer resultDealPlayer = memDeal.setNotPlayedResultForPlayer(playerID);

            if(resultDealPlayer != null){
                TrainingMemTournamentPlayer resTourPla = ranking.get(playerID);
                if (resTourPla == null) {
                    resTourPla = new TrainingMemTournamentPlayer(this);
                    resTourPla.setPlayerID(playerID);
                    ranking.put(playerID, resTourPla);
                }
                resTourPla.addPlayedDeal(memDeal.getDealID());

                // first deal played by this player => increment nb player valid on tournament
                if (resTourPla.getNbPlayedDeals() == 1) {
                    nbValidPlayers++;
                }
                // player played all deal ? => increment nb player finish all
                boolean computeFinisherRanking = false;
                if (resTourPla.getNbPlayedDeals() == deals.length) {
                    computeFinisherRanking = playersWhoFinished.add(playerID);
                }
            }
            else{
                log.error("Error on setNotPlayedResultForPlayer. playerID="+playerID+" - tournamentID="+tournamentID);
            }
        }
        else{
            log.error("Parameter not valid. memDeal="+memDeal+" - tournamentID="+tournamentID);
        }
        return null;
    }

    public TrainingMemDeal getMemDeal(String dealID){
        for (int i = 0; i < deals.length; i++) {
            if (deals[i] != null && deals[i].getDealID().equals(dealID)) {
                return deals[i];
            }
        }
        return null;
    }

    public List<TrainingMemTournamentPlayer> getResults(){
        return new ArrayList<>(ranking.values());
    }

    public int getNbPlayersWhoFinished(){
        return playersWhoFinished.size();
    }

    public boolean hasPlayerFinish(long playerID) {
        return playersWhoFinished.contains(playerID);
    }

    public int getNbPlayers(){
        return nbValidPlayers;
    }

    public TrainingMemTournamentPlayer getPlayerResult(long playerID){
        return ranking.get(playerID);
    }

    /**
     * Return the list of result value for a player for all deal of this tournament
     * @param plaID
     * @return
     */
    private List<Double> getListResultsForPlayer(long plaID) {
        List<Double> listResult = new ArrayList<Double>();
        for (TrainingMemDeal memDeal : deals) {
            TrainingMemDealPlayer dealPlayer = memDeal.getResultForPlayer(plaID);
            if (dealPlayer != null) {
                listResult.add(dealPlayer.result);
            }
        }
        return listResult;
    }

    /**
     * Return the current deal for this player
     * @param playerID
     * @return
     */
    public int getCurrentDealForPlayer(long playerID) {
        // Get ranking player
        TrainingMemTournamentPlayer tourPlayer = getTournamentPlayer(playerID);
        if (tourPlayer != null) {
            return tourPlayer.getCurrentDealIndex();
        }
        return -1;
    }

    public TrainingMemTournamentPlayer getTournamentPlayer(long playerID){
        return ranking.get(playerID);
    }

    /**
     * Return the WSResult on tournamentID for this player
     * @param playerCache
     * @param onlyFinishers
     * @return
     */
    public WSResultTournamentPlayer getWSResultPlayer(PlayerCache playerCache, boolean onlyFinishers) {
        if (playerCache != null) {
            // Get ranking player
            TrainingMemTournamentPlayer tourPlayer = getTournamentPlayer(playerCache.ID);
            if (tourPlayer != null) {
                WSResultTournamentPlayer res = tourPlayer.toWSResultTournamentPlayer(onlyFinishers);
                res.setPlayerID(playerCache.ID);
                res.setPlayerPseudo(playerCache.pseudo);
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
     * Return ranking for player on this tournament : existing or create a new one if not found
     * @param playerID
     * @return
     */
    public TrainingMemTournamentPlayer getOrCreateTournamentPlayer(long playerID) {
            TrainingMemTournamentPlayer plaRank = ranking.get(playerID);
            if (plaRank == null) {
                plaRank = new TrainingMemTournamentPlayer(this);
                plaRank.setPlayerID(playerID);
                plaRank.setStartDate(System.currentTimeMillis());
                plaRank.setLastPlayDate(System.currentTimeMillis());
                ranking.put(playerID, plaRank);
            }
            return plaRank;
    }

    /**
     * Compute result on tournament for all players
     */
    public void computeResult() {
        for (TrainingMemTournamentPlayer playerRanking : ranking.values()) {
            List<Double> listResultPla = getListResultsForPlayer(playerRanking.getPlayerID());
            if (listResultPla != null && listResultPla.size() > 0) {
                double tempVal = 0;
                for (int i = 0; i < listResultPla.size(); i++) {
                    tempVal += listResultPla.get(i);
                }
                if (resultType == Constantes.TOURNAMENT_RESULT_IMP) {
                    playerRanking.setResult(tempVal);
                } else if (resultType == Constantes.TOURNAMENT_RESULT_PAIRE) {
                    playerRanking.setResult(tempVal / listResultPla.size());
                }
            } else {
                playerRanking.setResult(0);
            }
        }
    }

    /**
     * Compute the ranking for players : ranking order result
     * @param computeFinisherRanking flag to compute finisherRanking too
     * @return list of playerID ordered by result
     */
    public List<Long> computeRanking(boolean computeFinisherRanking) {
        List<Long> listKeyPla = getPlayersOrderedByResult();
        int curRank = 0;
        int nbPlayerBefore = 0;
        double curResult = 0;
        int nbPlayerFinishedBefore = 0; // use only for TIMEZONE (count nb player with best result before)
        for (Long key : listKeyPla) {
            TrainingMemTournamentPlayer e = ranking.get(key);
            if (e.hasPlayedOneDeal()) {
                // first on rank
                if (curRank == 0) {
                    curRank = 1;
                    e.setRanking(1);
                    nbPlayerBefore = 1;
                    curResult = e.getResult();
                } else {
                    if (e.getResult() != curResult) {
                        curResult = e.getResult();
                        curRank = nbPlayerBefore+1;
                    }
                    e.setRanking(curRank);
                    nbPlayerBefore++;
                }
                // update the number of player who finished with better result
                e.setNbPlayersWhoFinishedWithBetterResult(nbPlayerFinishedBefore);
                if (e.hasFinished()) {
                    nbPlayerFinishedBefore++;
                }
            }
        }
        if (computeFinisherRanking) {
            computeFinisherRanking();
        }
        return listKeyPla;
    }

    /**
     * Return a ranking in this tournament at offset with nb max element in list
     * @param offset
     * @param nbMax max value in list result or -1 to have all
     * @param listPlaFilter
     * @param onlyFinishers
     * @return
     */
    public List<TrainingMemTournamentPlayer> getRanking(int offset, int nbMax, List<Long> listPlaFilter, boolean onlyFinishers) {
        List<TrainingMemTournamentPlayer> listRanking = new ArrayList<>();
        // sort ranking
        List<Long> listKeyPla = null;
        if (onlyFinishers) {
            listKeyPla = getPlayersWhoFinishedOrderedByResult();
        } else {
            listKeyPla = getPlayersOrderedByResult();
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
                TrainingMemTournamentPlayer playerResult = ranking.get(listKeyPla.get(i));
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
     * Compute the ranking with only players who finish all deals of tournament
     */
    public void computeFinisherRanking() {
        // ranking order result
        List<Long> listKeyPla = getPlayersWhoFinishedOrderedByResult();
        int curRank = 0;
        int nbPlayerBefore = 0;
        double curResult = 0;
        for (Long key : listKeyPla) {
            TrainingMemTournamentPlayer e = ranking.get(key);
            if (e.hasFinished()) {
                // first on rank
                if (curRank == 0) {
                    curRank = 1;
                    e.setRankingFinished(1);
                    nbPlayerBefore = 1;
                    curResult = e.getResult();
                } else {
                    if (e.getResult() != curResult) {
                        curResult = e.getResult();
                        curRank = nbPlayerBefore + 1;
                    }
                    e.setRankingFinished(curRank);
                    nbPlayerBefore++;
                }
            }
        }

    }

    /**
     * Return the list of players ordered by result (best result first)
     * @return
     */
    private List<Long> getPlayersOrderedByResult() {
        List<Long> listKeyPla = new ArrayList<Long>(ranking.keySet());
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                TrainingMemTournamentPlayer temp1 = ranking.get(o1);
                TrainingMemTournamentPlayer temp2 = ranking.get(o2);
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
                if (temp2.getResult() > temp1.getResult()) {
                    return 1;
                } else if (temp2.getResult() == temp1.getResult()) {
                    return 0;
                }
                return -1;
            }
        });
        return listKeyPla;
    }

    /**
     * Return the list of players who finished tournament ordered by result (best result first)
     * @return
     */
    private List<Long> getPlayersWhoFinishedOrderedByResult() {
        List<Long> listKeyPla = new ArrayList<Long>(playersWhoFinished);
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                TrainingMemTournamentPlayer temp1 = ranking.get(o1);
                TrainingMemTournamentPlayer temp2 = ranking.get(o2);
                // result of this object is greater so ranking is smaller ! Big result => ranking small
                if (temp2.getResult() > temp1.getResult()) {
                    return 1;
                } else if (temp2.getResult() == temp1.getResult()) {
                    return 0;
                }
                return -1;
            }
        });
        return listKeyPla;
    }

    /**
     * Compute the result on a deal for player
     * @param memDeal
     * @param resultPlayer
     * @return
     */
    protected double computeResultDealPlayer(TrainingMemDeal memDeal, TrainingMemDealPlayer resultPlayer) {
        if (memDeal != null && resultPlayer != null) {
            if (resultType == Constantes.TOURNAMENT_RESULT_IMP) {
                return Constantes.computeResultIMP(memDeal.getAverageScore(), resultPlayer.score);
            }
            if (resultType == Constantes.TOURNAMENT_RESULT_PAIRE) {
                if (resultPlayer.score == Constantes.GAME_SCORE_LEAVE && FBConfiguration.getInstance().getIntValue("general.resultLeave0", 0) == 1) {
                    return 0;
                }
                int nbScore = memDeal.nbScore;
                return Constantes.computeResultPaire(nbScore, resultPlayer.nbPlayersBetterScore, resultPlayer.nbPlayersSameScore);
            }
        }
        return 0;
    }

    public int getNbValidPlayers() {
        return nbValidPlayers;
    }

    public void setNbValidPlayers(int nbValidPlayers) {
        this.nbValidPlayers = nbValidPlayers;
    }

    public Set<Long> getPlayersWhoFinished() {
        return playersWhoFinished;
    }

    public void setPlayersWhoFinished(Set<Long> playersWhoFinished) {
        this.playersWhoFinished = playersWhoFinished;
    }

    public List<Long> getPlayers(){
        return new ArrayList<Long>(ranking.keySet());
    }

    public String getTournamentID() {
        return tournamentID;
    }

    public void setTournamentID(String tournamentID) {
        this.tournamentID = tournamentID;
    }

    public TrainingMemDeal[] getDeals() {
        return deals;
    }

    public void setDeals(TrainingMemDeal[] deals) {
        this.deals = deals;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
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

    public long getNbMaxPlayers() {
        return nbMaxPlayers;
    }

    public void setNbMaxPlayers(long nbMaxPlayers) {
        this.nbMaxPlayers = nbMaxPlayers;
    }

    public void setRanking(Map<Long, TrainingMemTournamentPlayer> ranking) {
        this.ranking = ranking;
    }

    public void removeResultForPlayer(List<Long> listPlayerID) {
        for (TrainingMemDeal memDeal : deals) {
            for (Long e : listPlayerID) {
                memDeal.removeResultPlayer(e);
            }
        }
        for (Long e : listPlayerID) {
            ranking.remove(e);
            if (playersWhoFinished.remove(e)) {
                nbValidPlayers--;
            }
        }
        computeResult();
        computeRanking(true);
    }
}
