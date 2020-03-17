package com.funbridge.server.tournament.timezone.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.timezone.TimezoneMgr;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by pserent on 15/04/2015.
 */
@Document(collection="timezone_game")
public class TimezoneGame extends Game {
    @Id
    private ObjectId ID;

    @DBRef
    private TimezoneTournament tournament;
    private Date creationDateISO = null;

    public TimezoneGame() {}

    public TimezoneGame(long playerID, TimezoneTournament tour, int dealIndex) {
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
        return TimezoneMgr.buildDealID(tournament.getIDStr(), dealIndex);
    }

    @Override
    public TimezoneTournament getTournament() {
        return tournament;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }
}
