package com.funbridge.server.tournament.team.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.team.cache.TeamCache;
import com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Data for tournament result for a player
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TeamMemTournamentPlayer extends GenericMemTournamentPlayer {
    public long playerID;
    public String teamID;
    public double result = 0;
    public int ranking = -1;
    public int rankingFinished = -1;
    public int points;
    public long dateLastPlay = 0;
    public long dateStart = 0;
    public int nbPlayerFinishWithBestResult = 0;
    public int currentDealIndex = -1;
    public List<String> playedDeals = new ArrayList<>();

    @JsonIgnore
    public TeamMemTournament memTour = null;

    public String toString() {
        return "playerID="+playerID+" - teamID="+teamID+" - result="+result+" - ranking="+ranking+" - points="+points+" - dateStart="+ Constantes.timestamp2StringDateHour(dateStart)+" - dateLastPlay="+Constantes.timestamp2StringDateHour(dateLastPlay)+" - nbDealsPlayed="+ getNbPlayedDeals();
    }

    /**
     * Check if this player has played all deals
     * @return
     */
    public boolean isPlayerFinish() {
        return playedDeals.size() == memTour.getNbDeals();
    }

    /**
     * Check if this player has played at least one deal
     * @return
     */
    public boolean hasPlayedOneDeal() {
        return (playedDeals.size() > 0);
    }

    public void addDealPlayed(String dealID) {
        if (!playedDeals.contains(dealID)) {
            playedDeals.add(dealID);
        }
    }

    public void removePlayedDeal(String dealID) {
        playedDeals.remove(dealID);
    }

    public int getNbPlayedDeals() {
        return playedDeals.size();
    }

    /**
     * Return the result on tournament for player.
     * @param useRankFinished
     * @return
     */
    public WSResultTournamentPlayer toWSResultTournamentPlayer(boolean useRankFinished) {
        WSResultTournamentPlayer resultPlayer = new WSResultTournamentPlayer();
        if (dateLastPlay == 0) {
            resultPlayer.setDateLastPlay(dateStart);
        } else {
            resultPlayer.setDateLastPlay(dateLastPlay);
        }
        resultPlayer.setNbDealPlayed(getNbPlayedDeals());
        resultPlayer.setPlayerID(playerID);
        PlayerCache playerCache = ContextManager.getPlayerCacheMgr().getOrLoadPlayerCache(playerID);
        if(playerCache != null){
            resultPlayer.setPlayerPseudo(playerCache.getPseudo());
            resultPlayer.setAvatarPresent(playerCache.avatarPresent);
            resultPlayer.setCountryCode(playerCache.countryCode);
        }
        resultPlayer.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(playerID));
        int nbTotalPlayer = memTour.getNbPlayersForRanking();
        if (useRankFinished) {
            nbTotalPlayer = memTour.getNbPlayersWhoFinished();
        }
        if (useRankFinished) {
            // rank only if player finish tournament
            if (isPlayerFinish()) {
                resultPlayer.setRank(rankingFinished);
            } else {
                if (memTour.getNbPlayersWhoFinished() > 0) {
                    resultPlayer.setRank(nbPlayerFinishWithBestResult+1);
                    nbTotalPlayer++;
                } else {
                    resultPlayer.setRank(-1);
                }
            }
        } else {
            resultPlayer.setRank(ranking);
        }
        resultPlayer.setNbTotalPlayer(nbTotalPlayer);
        resultPlayer.setResult(result);
        resultPlayer.setNbPlayerFinishWithBestResult(nbPlayerFinishWithBestResult);
        resultPlayer.setRankHidden(ranking);
        resultPlayer.setTeamPoints(points);
        resultPlayer.setTeamID(teamID);
        TeamCache teamCache = ContextManager.getTeamCacheMgr().getOrLoadTeamCache(teamID);
        if(teamCache != null){
            resultPlayer.setTeamName(teamCache.name);
            resultPlayer.setCaptain(playerID == teamCache.captainID);
        }
        return resultPlayer;
    }
}
