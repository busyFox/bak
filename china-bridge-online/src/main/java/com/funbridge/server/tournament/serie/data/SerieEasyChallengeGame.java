package com.funbridge.server.tournament.serie.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@Document(collection="serie_easy_challenge_game")
public class SerieEasyChallengeGame extends Game {

    @Id
    private ObjectId id;
    @DBRef
    private TourSerieTournament tournament;
    private Date creationDateISO = new Date();

    public SerieEasyChallengeGame() {}

    public SerieEasyChallengeGame(long playerID, TourSerieTournament tour, int dealIndex) {
        this.playerID = playerID;
        this.tournament = tour;
        this.dealIndex = dealIndex;
        this.creationDateISO = new Date();
        this.setEngineVersion(ContextManager.getArgineEngineMgr().getEngineVersion(tour.getCategory()));
    }

    @Override
    public TourSerieTournament getTournament() {
        this.tournament.setCategory(Constantes.TOURNAMENT_CATEGORY_SERIE_EASY_CHALLENGE);
        return this.tournament;
    }

    @Override
    public String getIDStr() {
        if (this.id != null) {
            return this.id.toString();
        }
        if (isReplay()) {
            return this.replayGameID;
        }
        return null;
    }
}
