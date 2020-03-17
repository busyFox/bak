package com.funbridge.server.tournament.team.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.team.TourTeamMgr;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by pserent on 08/11/2016.
 */
@Document(collection="team_game")
public class TeamGame extends Game {
    @Id
    private ObjectId ID;

    @DBRef
    private TeamTournament tournament;

    private String teamID;

    private Date creationDateISO = null;

    public TeamGame(){}

    public TeamGame(long playerID, TeamTournament tour, int dealIndex, String team) {
        this.playerID = playerID;
        this.tournament = tour;
        this.dealIndex = dealIndex;
        this.teamID = team;
        this.creationDateISO = new Date();
        setEngineVersion(ContextManager.getArgineEngineMgr().getEngineVersion(tour.getCategory()));
    }

    public ObjectId getID() {
        return ID;
    }

    @Override
    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        if (isReplay()) {
            return replayGameID;
        }
        return null;
    }

    @Override
    public TeamTournament getTournament() {
        return tournament;
    }

    public String getDealID() {
        return TourTeamMgr.buildDealID(tournament.getIDStr(), dealIndex);
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public String getTeamID() {
        return teamID;
    }

    public void setTeamID(String teamID) {
        this.teamID = teamID;
    }
}
