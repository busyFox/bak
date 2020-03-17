package com.funbridge.server.tournament.training.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by pserent on 09/04/2015.
 */
@Document(collection="training_game")
public class TrainingGame extends Game {
    @Id
    private ObjectId ID;

    @DBRef
    private TrainingTournament tournament;
    private Date creationDateISO = null;

    public TrainingGame(){}

    public TrainingGame(long playerID, TrainingTournament tour, int dealIndex){
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

    public TrainingTournament getTournament() {
        return tournament;
    }

    public void setTournament(TrainingTournament tournament) {
        this.tournament = tournament;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }
}
