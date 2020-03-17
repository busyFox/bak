package com.funbridge.server.tournament.federation.cbo.data;

import com.funbridge.server.tournament.federation.data.TourFederationGame;
import com.funbridge.server.tournament.federation.data.TourFederationTournament;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by ldelbarre on 21/12/2017.
 */
@Document(collection="cbo_game")
public class TourCBOGame extends TourFederationGame {

    public TourCBOGame(){
        super();
    }

    public TourCBOGame(long playerID, TourFederationTournament tour, int dealIndex) {
        super(playerID, tour, dealIndex);
    }

    public TourCBOGame(long playerID, TourFederationTournament tour, int dealIndex, int playerIndex) {
        super(playerID, tour, dealIndex, playerIndex);
    }
}
