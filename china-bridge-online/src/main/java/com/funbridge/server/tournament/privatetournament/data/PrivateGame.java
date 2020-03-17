package com.funbridge.server.tournament.privatetournament.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.privatetournament.PrivateTournamentMgr;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by pserent on 23/01/2017.
 */
@Document(collection="private_game")
public class PrivateGame extends Game {
    @Id
    private ObjectId ID;

    @DBRef
    private PrivateTournament tournament;
    private Date creationDateISO = null;

    public PrivateGame() {}

    public PrivateGame(long playerID, PrivateTournament tour, int dealIndex) {
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

    public String getDealID() {
        return PrivateTournamentMgr.buildDealID(tournament.getIDStr(), dealIndex);
    }

    @Override
    public PrivateTournament getTournament() {
        return tournament;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }
}
