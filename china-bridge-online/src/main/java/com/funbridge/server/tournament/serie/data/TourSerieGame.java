package com.funbridge.server.tournament.serie.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.serie.TourSerieMgr;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Created by pserent on 11/06/2014.
 * Objet to define a game player on a deal
 */
@Document(collection="serie_game")
public class TourSerieGame extends Game {
    @Id
    private ObjectId ID;

    @DBRef
    private TourSerieTournament tournament;

    private Date creationDateISO = null;

    public TourSerieGame(){}

    public TourSerieGame(long playerID, TourSerieTournament tour, int dealIndex) {
        this.playerID = playerID;
        this.tournament = tour;
        this.dealIndex = dealIndex;
        this.creationDateISO = new Date();
        setEngineVersion(ContextManager.getArgineEngineMgr().getEngineVersion(tour.getCategory()));
    }

    public ObjectId getID() {
        return ID;
    }

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        if (isReplay()) {
            return replayGameID;
        }
        return null;
    }

    public TourSerieTournament getTournament() {
        return tournament;
    }

    @Override
    public String getDealID() {
        return TourSerieMgr.buildDealID(tournament.getIDStr(), dealIndex);
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }
}
