package com.funbridge.server.tournament.learning.data;

import com.funbridge.server.tournament.game.TournamentPlayer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by ldelbarre on 30/05/2018.
 */
@Document(collection="learning_tournament_player")
public class LearningTournamentPlayer extends TournamentPlayer {
    @Id
    private ObjectId ID;

    @DBRef
    private LearningTournament tournament;

    private int resultType = 0;

    public ObjectId getID() {
        return ID;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public LearningTournament getTournament() {
        return tournament;
    }

    public void setTournament(LearningTournament tournament) {
        this.tournament = tournament;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }
}
