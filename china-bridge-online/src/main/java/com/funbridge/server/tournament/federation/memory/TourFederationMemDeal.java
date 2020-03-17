package com.funbridge.server.tournament.federation.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.tournament.federation.data.TourFederationGame;
import com.funbridge.server.tournament.game.Game;
import com.funbridge.server.tournament.generic.memory.GenericMemDeal;

/**
 * Created by ldelbarre on 02/08/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourFederationMemDeal<TFederationMemDealPlayer extends TourFederationMemDealPlayer> extends GenericMemDeal<TFederationMemDealPlayer>{

    public TourFederationMemDeal(){}

    public TourFederationMemDeal(TourFederationMemTournament memTour, int idx){
        super(memTour, idx);
    }

    /**
     * Set result for a player on this deal. Update of result for other player and
     * order the list of result for this deal (by score)
     * @param game
     * @return
     */
    public TFederationMemDealPlayer setResultPlayer(Game game) {
        TFederationMemDealPlayer resultPlayer = (TFederationMemDealPlayer) super.setResultPlayer(game);
        if(resultPlayer != null) resultPlayer.playerIndex = ((TourFederationGame)game).getPlayerIndex();
        return resultPlayer;
    }

}