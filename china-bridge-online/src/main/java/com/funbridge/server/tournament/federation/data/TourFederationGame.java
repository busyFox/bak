package com.funbridge.server.tournament.federation.data;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.ws.result.WSResultDeal;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.Date;

/**
 * Created by ldelbarre on 02/08/2017.
 */
public class TourFederationGame extends Game {
    @Id
    private ObjectId ID;

    @DBRef
    private TourFederationTournament tournament;
    private Date creationDateISO = null;
    private int playerIndex = 0;

    public TourFederationGame(){}

    public TourFederationGame(long playerID, TourFederationTournament tour, int dealIndex) {
        this.playerID = playerID;
        this.tournament = tour;
        this.dealIndex = dealIndex;
        this.creationDateISO = new Date();
        setEngineVersion(ContextManager.getArgineEngineMgr().getEngineVersion(tour.getCategory()));
    }

    public TourFederationGame(long playerID, TourFederationTournament tour, int dealIndex, int playerIndex) {
        this.playerID = playerID;
        this.tournament = tour;
        this.dealIndex = dealIndex;
        this.creationDateISO = new Date();
        this.playerIndex = playerIndex;
        setEngineVersion(ContextManager.getArgineEngineMgr().getEngineVersion(tour.getCategory()));
    }

    @Override
    public TourFederationTournament getTournament() {
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

    @Override
    public WSResultDeal toWSResultDeal() {
        WSResultDeal resultDeal = super.toWSResultDeal();
        resultDeal.setDealIndex(getPlayerIndex());
        resultDeal.setNbTotalPlayer(getTournament().getNbPlayersOnDeal(getDealID()));
        return resultDeal;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public int getPlayerIndex() {
        return playerIndex;
    }

    public void setPlayerIndex(int playerIndex) {
        this.playerIndex = playerIndex;
    }
}
