package com.funbridge.server.tournament.team.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.team.cache.TeamCache;
import com.funbridge.server.tournament.game.TournamentPlayer;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by pserent on 08/11/2016.
 */
@Document(collection="team_tournament_player")
public class TeamTournamentPlayer extends TournamentPlayer {
    @Id
    private ObjectId ID;

    private String teamID;

    @DBRef
    private TeamTournament tournament;
    private String periodTour;
    private int points = 0;

    public ObjectId getID() {
        return ID;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public String getTeamID() {
        return teamID;
    }

    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }

    public TeamTournament getTournament() {
        return tournament;
    }

    public void setTournament(TeamTournament tournament) {
        this.tournament = tournament;
    }

    public String getPeriodTour() {
        return periodTour;
    }

    public void setPeriodTour(String periodTour) {
        this.periodTour = periodTour;
    }
    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getLastDate() {
        return lastDate;
    }

    public void setLastDate(long lastDate) {
        this.lastDate = lastDate;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public WSResultTournamentPlayer toWSResultTournamentPlayer(PlayerCache pc, long playerAsk) {
        WSResultTournamentPlayer resPla = new WSResultTournamentPlayer();
        resPla.setDateLastPlay(lastDate);
        resPla.setNbDealPlayed(tournament.getNbDeals());
        if (pc != null) {
            resPla.setPlayerID(pc.ID);
            resPla.setPlayerPseudo(pc.getPseudo());
            resPla.setConnected(ContextManager.getPresenceMgr().isSessionForPlayerID(pc.ID));
            if (pc.ID == playerAsk) {
                resPla.setAvatarPresent(pc.avatarPresent);
            } else {
                resPla.setAvatarPresent(pc.avatarPublic);
            }
            resPla.setPlayerSerie(pc.serie);
            resPla.setCountryCode(pc.countryCode);
        }
        resPla.setRank(rank);
        resPla.setResult(result);
        resPla.setNbTotalPlayer(getTournament().getNbPlayers());
        resPla.setTeamPoints(points);
        resPla.setTeamID(teamID);
        TeamCache teamCache = ContextManager.getTeamCacheMgr().getOrLoadTeamCache(teamID);
        if(teamCache != null){
            resPla.setTeamName(teamCache.name);
            resPla.setCaptain(playerID == teamCache.captainID);
        }
        return resPla;
    }
}
