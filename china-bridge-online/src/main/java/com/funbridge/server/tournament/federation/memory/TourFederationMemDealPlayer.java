package com.funbridge.server.tournament.federation.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.tournament.generic.memory.GenericMemDeal;
import com.funbridge.server.tournament.generic.memory.GenericMemDealPlayer;
import com.funbridge.server.ws.result.WSResultDeal;

/**
 * Created by ldelbarre on 02/08/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourFederationMemDealPlayer extends GenericMemDealPlayer{
    public int playerIndex = 0;

    public TourFederationMemDealPlayer(){ super(); }

    public WSResultDeal toWSResultDeal(GenericMemDeal deal){
        WSResultDeal resultPlayer = super.toWSResultDeal(deal);
        resultPlayer.setDealIndex(playerIndex);
        return resultPlayer;
    }
}