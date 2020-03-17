package com.funbridge.server.tournament.training.data;

import com.funbridge.server.tournament.game.TournamentPlayer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 09/04/2015.
 */
@Document(collection="training_tournament_player")
public class TrainingTournamentPlayer extends TournamentPlayer {
    @Id
    private ObjectId ID;
    @DBRef
    private TrainingTournament tournament;
    private int resultType = 0;


    public TrainingTournament getTournament() {
        return tournament;
    }

    public void setTournament(TrainingTournament tournament) {
        this.tournament = tournament;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }
}
