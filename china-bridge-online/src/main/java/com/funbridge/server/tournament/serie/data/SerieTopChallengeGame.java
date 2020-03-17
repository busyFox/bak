package com.funbridge.server.tournament.serie.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by bplays on 21/04/16.
 */
@Document(collection="serie_top_challenge_game")
public class SerieTopChallengeGame extends Game {

    @Id
    private ObjectId ID;
    @DBRef
    private TourSerieTournament tournament;
    private Date creationDateISO = null;

    public SerieTopChallengeGame() {}

    public SerieTopChallengeGame(long playerID, TourSerieTournament tour, int dealIndex) {
        this.playerID = playerID;
        this.tournament = tour;
        this.dealIndex = dealIndex;
        this.creationDateISO = new Date();
        setEngineVersion(ContextManager.getArgineEngineMgr().getEngineVersion(tour.getCategory()));
    }

    @Override
    public TourSerieTournament getTournament() {
        tournament.setCategory(Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE);
        return tournament;
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
}
