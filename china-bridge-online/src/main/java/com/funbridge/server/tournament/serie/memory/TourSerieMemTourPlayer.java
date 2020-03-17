package com.funbridge.server.tournament.serie.memory;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.generic.memory.GenericMemTournamentPlayer;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by pserent on 09/06/2014.
 * Bean to store in memory the player ranking on tournament
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourSerieMemTourPlayer extends GenericMemTournamentPlayer implements Comparable<TourSerieMemTourPlayer> {

    // index of deals played by this player
    public Set<String> dealsPlayed = new HashSet<>();
    public long dateStartPlay = 0;

    public String toString() {
        return "playerID="+playerID+" - result="+result+" - ranking="+ranking+" - dateStartPlay="+ Constantes.timestamp2StringDateHour(dateStartPlay)+" - dateLastPlay="+Constantes.timestamp2StringDateHour(dateLastPlay)+" - nbDealsPlayed="+getNbDealsPlayed();
    }

    public int getNbDealsPlayed() {
        return dealsPlayed.size();
    }

    /**
     * Return the result on tournament for player. Only the field nbTotalPlayer and pseudo are not set.
     * @return
     */
    public WSResultTournamentPlayer toWSResultTournamentPlayer() {
        WSResultTournamentPlayer resultPlayer = new WSResultTournamentPlayer();
        if (dateLastPlay == 0) {
            resultPlayer.setDateLastPlay(dateStartPlay);
        } else {
            resultPlayer.setDateLastPlay(dateLastPlay);
        }
        resultPlayer.setNbDealPlayed(getNbDealsPlayed());
        resultPlayer.setPlayerID(playerID);
        resultPlayer.setRank(rankingFinished);
        resultPlayer.setNbPlayerFinishWithBestResult(nbPlayerFinishWithBestResult);
        resultPlayer.setRankHidden(ranking);
        resultPlayer.setResult(result);
        return resultPlayer;
    }

    public void addDealPlayed(String dealID) {
        dealsPlayed.add(dealID);
    }

    public void removeDealPlayed(String dealID) {
        dealsPlayed.remove(dealID);
    }

    @Override
    public int compareTo(TourSerieMemTourPlayer o) {
        if (this.getNbDealsPlayed()==0 && o.getNbDealsPlayed() == 0) {
            return 0;
        }
        if (this.getNbDealsPlayed() == 0) {
            return -1;
        }
        if (o.getNbDealsPlayed() == 0) {
            return 1;
        }
        // result of this object is greater so ranking is smaller ! Big result => ranking small
        if (o.result > this.result) {
            return 1;
        } else if (o.result == this.result) {
            return 0;
        }
        return -1;
    }
}
