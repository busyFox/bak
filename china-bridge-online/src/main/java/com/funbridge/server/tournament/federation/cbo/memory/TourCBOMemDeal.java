package com.funbridge.server.tournament.federation.cbo.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.tournament.federation.memory.TourFederationMemDeal;

/**
 * Created by ldelbarre on 21/12/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourCBOMemDeal extends TourFederationMemDeal<TourCBOMemDealPlayer> {

    public TourCBOMemDeal(){
        super();
    }

    public TourCBOMemDeal(TourCBOMemTournament memTour, int idx){
        super(memTour, idx);
    }
}