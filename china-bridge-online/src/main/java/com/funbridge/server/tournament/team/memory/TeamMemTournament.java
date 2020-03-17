package com.funbridge.server.tournament.team.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.tournament.generic.memory.GenericMemTournament;
import com.funbridge.server.tournament.team.data.TeamGame;
import com.funbridge.server.tournament.team.data.TeamTournament;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object to store player result on tournament
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class TeamMemTournament extends GenericMemTournament<TeamMemTournamentPlayer,
        TeamMemDeal,
        TeamMemDealPlayer> {
    public String tourID;
    public String name;
    public String division;
    public long endDate;
    public String group;
    // array of resultDeal
    public TeamMemDeal[] deals;
    // Map to store player result on tournament : PLAYER_ID => MEM_TOURNAMENT_PLAYER
    public Map<Long, TeamMemTournamentPlayer> mapTourPlayer = new ConcurrentHashMap<>();

    public Set<Long> listPlayerFinishAll = new HashSet<>();
    public Set<Long> listPlayerForRanking = new HashSet<>();
    public int nbTeams = 0;
    @JsonIgnore
    private Logger log = LogManager.getLogger(this.getClass());

    public TeamMemTournament() {
    }

    public TeamMemTournament(TeamTournament tour) {
        this.tourID = tour.getIDStr();
        this.endDate = tour.getEndDate();
        this.name = tour.getName();
        this.division = tour.getDivision();
        this.group = tour.getGroup();
        deals = new TeamMemDeal[tour.getNbDeals()];
        for (int i = 0; i < deals.length; i++) {
            deals[i] = new TeamMemDeal(this, i + 1);
        }
    }

    public String toString() {
        return "tourID=" + tourID + " - division=" + division + " - group=" + group + " - nbTeams=" + nbTeams + " - nbPlayers=" + getNbPlayers();
    }

    public TeamMemDeal getDealWithID(String dealID) {
        for (TeamMemDeal e : deals) {
            if (e.dealID.equals(dealID)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Call after load memTour JSON file.
     */
    public void initAfterLoad() {
        for (TeamMemDeal d : deals) {
            d.setMemTour(this);
            d.computeScoreData();
            d.computeRanking();
        }
        for (TeamMemTournamentPlayer e : mapTourPlayer.values()) {
            e.memTour = this;
            if (e.hasPlayedOneDeal()) {
                listPlayerForRanking.add(e.playerID);
            }
            if (e.isPlayerFinish()) {
                listPlayerFinishAll.add(e.playerID);
            }
        }
        computeResult();
        computeRanking(true);
    }

    /**
     * Compute result on tournament for all players
     */
    public void computeResult() {
        for (TeamMemTournamentPlayer tp : mapTourPlayer.values()) {
            tp.result = computeResultForPlayer(tp.playerID);
        }
    }

    /**
     * Return the sum of result for player on all deals played
     *
     * @param plaID
     * @return
     */
    private double computeResultForPlayer(long plaID) {
        double result = 0;
        for (TeamMemDeal memDeal : deals) {
            TeamMemDealPlayer dealPlayer = memDeal.getResultPlayer(plaID);
            if (dealPlayer != null) {
                result += dealPlayer.result;
            }
        }
        return result;
    }

    /**
     * Compute the ranking for players : ranking order result
     *
     * @param computeFinished flag to compute rankingFinished too
     * @return likst of playerID order by result
     */
    public List<Long> computeRanking(boolean computeFinished) {
        List<Long> listKeyPla = getListPlayerOrderResult();
        int curRank = 0;
        int nbPlayerBefore = 0;
        double curResult = 0;
        int nbPlayerFinishedBefore = 0;
        Map<Double, Integer> mapNbPlayerSameResult = new HashMap<>(); // map to store for each result the nb of player
        for (Long key : listKeyPla) {
            TeamMemTournamentPlayer e = mapTourPlayer.get(key);
            if (e.hasPlayedOneDeal()) {
                int temp = mapNbPlayerSameResult.get(e.result) != null ? mapNbPlayerSameResult.get(e.result).intValue() : 0;
                temp++;
                mapNbPlayerSameResult.put(e.result, temp);
            }
        }
        for (Long key : listKeyPla) {
            TeamMemTournamentPlayer e = mapTourPlayer.get(key);
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
                        curRank = nbPlayerBefore + 1;
                    }
                    e.ranking = curRank;
                    nbPlayerBefore++;
                }
                // compute points
                int nbPlayerSameResult = mapNbPlayerSameResult.get(e.result) != null ? mapNbPlayerSameResult.get(e.result).intValue() : 1;
                e.points = nbTeams - curRank - ((nbPlayerSameResult - 1) / 2);
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
            TeamMemTournamentPlayer e = mapTourPlayer.get(key);
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
     * Return the list of player order by result (Best result at first)
     *
     * @return
     */
    public List<Long> getListPlayerOrderResult() {
        List<Long> listKeyPla = new ArrayList<Long>(mapTourPlayer.keySet());
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                TeamMemTournamentPlayer temp1 = mapTourPlayer.get(o1);
                TeamMemTournamentPlayer temp2 = mapTourPlayer.get(o2);
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
     *
     * @return
     */
    @Override
    public List<Long> getListPlayerOrderFinishedResult() {
        List<Long> listKeyPla = new ArrayList<>(listPlayerFinishAll);
        Collections.sort(listKeyPla, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                TeamMemTournamentPlayer temp1 = mapTourPlayer.get(o1);
                TeamMemTournamentPlayer temp2 = mapTourPlayer.get(o2);
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
     * Add a result
     *
     * @param game
     * @param computeResultRanking if true, the result and ranking are computed else the result and ranking are set with value from game object
     * @return true if deal exist and result for a player is added with success
     */
    public TeamMemTournamentPlayer addResult(TeamGame game, boolean computeResultRanking) {
        if (game != null && game.getTournament().getIDStr().equals(tourID) && game.getDealIndex() >= 1 && game.getDealIndex() <= deals.length) {
            TeamMemDeal resultDeal = deals[game.getDealIndex() - 1];
            // a result exist for this deal
            if (resultDeal != null) {
                // add result of player for this deal
                TeamMemDealPlayer resDealPla = resultDeal.setResultPlayer(game);

                if (resDealPla != null) {
                    TeamMemTournamentPlayer plaRank = getOrCreateTournamentPlayer(game.getPlayerID(), game.getTeamID(), System.currentTimeMillis());
                    plaRank.addDealPlayed(resultDeal.dealID);
                    plaRank.dateLastPlay = game.getLastDate();
                    boolean computeFinished = false;
                    listPlayerForRanking.add(game.getPlayerID());
                    if (plaRank.isPlayerFinish()) {
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
            log.error("Parameter no valid ... game=" + game + " - tourID=" + tourID);
        }
        return null;
    }

    public TeamMemTournamentPlayer addResultGameNotPlayed(TeamMemDeal memDeal, long playerID) {
        if (memDeal != null) {
            // add result of player for this deal
            TeamMemDealPlayer resDealPla = memDeal.setResultPlayerNotPlayed(playerID);

            if (resDealPla != null) {
                TeamMemTournamentPlayer tourPla = getTournamentPlayer(playerID);
                if (tourPla != null) {
                    tourPla.addDealPlayed(memDeal.dealID);
                    listPlayerForRanking.add(playerID);
                    if (tourPla.isPlayerFinish()) {
                        listPlayerFinishAll.add(playerID);
                    }
                    return tourPla;
                } else {
                    log.error("Failed to get tournamentPlayer for playerID=" + playerID + " - tourID=" + tourID);
                }
            } else {
                log.error("Failed on setResultPlayer for player - tourID=" + tourID);
            }
        } else {
            log.error("Parameter no valid ... memDeal=" + memDeal + " - tourID=" + tourID);
        }
        return null;
    }

    /**
     * Return tournamentPlayer for player on this tournament : existing or create a new one if not found
     *
     * @param playerID
     * @param tsDate
     * @return
     */
    public TeamMemTournamentPlayer getOrCreateTournamentPlayer(long playerID, String teamID, long tsDate) {
        TeamMemTournamentPlayer plaRank = mapTourPlayer.get(playerID);
        if (plaRank == null) {
            plaRank = new TeamMemTournamentPlayer();
            plaRank.memTour = this;
            plaRank.playerID = playerID;
            plaRank.teamID = teamID;
            plaRank.dateStart = tsDate;
            plaRank.dateLastPlay = tsDate;
            mapTourPlayer.put(playerID, plaRank);
        }
        return plaRank;
    }

    /**
     * Return the result of this player on tournament
     *
     * @param playerID
     * @return
     */
    public TeamMemTournamentPlayer getTournamentPlayer(long playerID) {
        return mapTourPlayer.get(playerID);
    }

    /**
     * Return the tournamentPlayer for this team on tournament
     *
     * @param teamID
     * @return
     */
    public TeamMemTournamentPlayer getTournamentPlayerForTeam(String teamID) {
        for (TeamMemTournamentPlayer e : mapTourPlayer.values()) {
            if (e.teamID.equals(teamID)) {
                return e;
            }
        }
        return null;
    }

    public int getNbDeals() {
        return deals.length;
    }

    /**
     * Return the number of players on tournament (players start and finish it)
     *
     * @return
     */
    public int getNbPlayers() {
        return mapTourPlayer.size();
    }

    /**
     * Return the number of players for ranking (players who started the tournament)
     *
     * @return
     */
    public int getNbPlayersForRanking() {
        return listPlayerForRanking.size();
    }

    /**
     * Return the number of players who finished the tournament : all deals played
     *
     * @return
     */
    public int getNbPlayersWhoFinished() {
        return listPlayerFinishAll.size();
    }

    /**
     * Return the WSResult on tournamentID for this player
     *
     * @param playerCache
     * @return
     */
    public WSResultTournamentPlayer getWSResultPlayer(PlayerCache playerCache, boolean useRankFinished) {
        if (playerCache != null) {
            // Get ranking player
            TeamMemTournamentPlayer tourPlayer = getTournamentPlayer(playerCache.ID);
            if (tourPlayer != null) {
                WSResultTournamentPlayer res = tourPlayer.toWSResultTournamentPlayer(useRankFinished);
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
     *
     * @param playerID
     * @return
     */
    public int getCurrentDealForPlayer(long playerID) {
        // Get ranking player
        TeamMemTournamentPlayer tourPlayer = getTournamentPlayer(playerID);
        if (tourPlayer != null) {
            return tourPlayer.currentDealIndex;
        }
        return -1;
    }

    /**
     * Return list of ranking (brut)
     *
     * @return
     */
    public List<TeamMemTournamentPlayer> getTourPlayers() {
        return new ArrayList<TeamMemTournamentPlayer>(mapTourPlayer.values());
    }

    /**
     * Return playerID list (brut)
     *
     * @return
     */
    public List<Long> getListPlayer() {
        return new ArrayList<Long>(mapTourPlayer.keySet());
    }

    /**
     * Return list of TourPlayer only player finished
     *
     * @return
     */
    public List<TeamMemTournamentPlayer> getListTourPlayerOnlyFinished() {
        List<TeamMemTournamentPlayer> lisTourPlayer = new ArrayList<TeamMemTournamentPlayer>();
        if (listPlayerFinishAll != null && listPlayerFinishAll.size() > 0) {
            for (Long l : listPlayerFinishAll) {
                TeamMemTournamentPlayer mtr = mapTourPlayer.get(l);
                // at least one deal played !
                if (mtr != null && mtr.isPlayerFinish()) {
                    lisTourPlayer.add(mtr);
                }
            }
        }
        return lisTourPlayer;
    }

    @Override
    public List<TeamMemTournamentPlayer> getRanking(int offset, int nbMax, List<Long> listPlaFilter, boolean onlyFinished) {
        List<TeamMemTournamentPlayer> listRanking = new ArrayList<>();
        // sort ranking
        List<Long> listKeyPla;
        if (onlyFinished) {
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
            for (int i = 0; i < listKeyPla.size(); i++) {
                if (nbMax > 0 && listRanking.size() >= nbMax) {
                    break;
                }
                TeamMemTournamentPlayer mtr = mapTourPlayer.get(listKeyPla.get(i));
                // at least one deal played !
                if (mtr != null && mtr.getNbPlayedDeals() > 0) {
                    nbElemValid++;
                    if (nbElemValid > offset) {
                        listRanking.add(mtr);
                    }
                }
            }
        }
        return listRanking;
    }
}
