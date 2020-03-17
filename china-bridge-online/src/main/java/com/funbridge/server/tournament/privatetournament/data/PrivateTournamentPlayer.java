package com.funbridge.server.tournament.privatetournament.data;

import com.funbridge.server.tournament.game.TournamentPlayer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 23/01/2017.
 */
@Document(collection="private_tournament_player")
public class PrivateTournamentPlayer extends TournamentPlayer{
    @Id
    private ObjectId ID;

    @DBRef
    private PrivateTournament tournament;

    private int resultType = 0;

    public ObjectId getID() {
        return ID;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public PrivateTournament getTournament() {
        return tournament;
    }

    public void setTournament(PrivateTournament tournament) {
        this.tournament = tournament;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }
}
