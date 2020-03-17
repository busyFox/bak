package com.funbridge.server.tournament.duel.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by pserent on 07/07/2015.
 */
@Document(collection="duel_game")
public class DuelGame extends Game {
    @Id
    private ObjectId ID;
    @DBRef
    private DuelTournament tournament;

    private String tournamentPlayerID = null;
    private Date creationDateISO = null;
    private boolean arginePlayAlone = false;

    public DuelGame() {}

    public DuelGame(long playerID, DuelTournament tour, int dealIndex) {
        this.playerID = playerID;
        this.tournament = tour;
        this.dealIndex = dealIndex;
        this.creationDateISO = new Date();
        setEngineVersion(ContextManager.getArgineEngineMgr().getEngineVersion(tour.getCategory()));
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
    public DuelTournament getTournament() {
        return tournament;
    }

    public String getDealID() {
        return ContextManager.getDuelMgr().buildDealID(tournament.getIDStr(), dealIndex);
    }

    public String getTournamentPlayerID() {
        return tournamentPlayerID;
    }

    public void setTournamentPlayerID(String tournamentPlayerID) {
        this.tournamentPlayerID = tournamentPlayerID;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public boolean isArginePlayAlone() {
        return arginePlayAlone;
    }

    public void setArginePlayAlone(boolean arginePlayAlone) {
        this.arginePlayAlone = arginePlayAlone;
    }
}
