package com.funbridge.server.tournament.learning.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by ldelbarre on 30/05/2018.
 */
@Document(collection="learning_game")
public class LearningGame extends Game {
    @Id
    private ObjectId ID;

    @DBRef
    private LearningTournament tournament;
    private Date creationDateISO = null;

    public LearningGame() {}

    public LearningGame(long playerID, LearningTournament tour, int dealIndex) {
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

    public ObjectId getID() {
        return ID;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public String getDealID() {
        return ContextManager.getTourLearningMgr()._buildDealID(tournament.getIDStr(), dealIndex);
    }

    @Override
    public LearningTournament getTournament() {
        return tournament;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }
}
