package com.funbridge.server.tournament.federation.cbo.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.federation.data.TourFederationTournament;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by ldelbarre on 21/12/2017.
 */
@Document(collection="cbo_tournament")
public class TourCBOTournament extends TourFederationTournament {

    public TourCBOTournament() {
        this.category = Constantes.TOURNAMENT_CATEGORY_TOUR_CBO;
    }

    @Override
    public int getCategory() {
        return Constantes.TOURNAMENT_CATEGORY_TOUR_CBO;
    }

    @Override
    public TourFederationMgr getTourFederationMgr() {
        return ContextManager.getTourCBOMgr();
    }
}
