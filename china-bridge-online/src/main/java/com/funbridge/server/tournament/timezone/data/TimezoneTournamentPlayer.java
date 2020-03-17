package com.funbridge.server.tournament.timezone.data;

import com.funbridge.server.tournament.game.TournamentPlayer;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 15/04/2015.
 */
@Document(collection="timezone_tournament_player")
public class TimezoneTournamentPlayer extends TournamentPlayer{
    @Id
    private ObjectId ID;

    @DBRef
    private TimezoneTournament tournament;

    private int resultType = 0;

    public ObjectId getID() {
        return ID;
    }

    public void setID(ObjectId ID) {
        this.ID = ID;
    }

    public TimezoneTournament getTournament() {
        return tournament;
    }

    public void setTournament(TimezoneTournament tournament) {
        this.tournament = tournament;
    }

    public int getResultType() {
        return resultType;
    }

    public void setResultType(int resultType) {
        this.resultType = resultType;
    }
}
