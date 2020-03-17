package com.funbridge.server.tournament.federation.cbo;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.federation.TourFederationGeneratorTask;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.federation.cbo.data.TourCBOTournament;

/**
 * Created by ldelbarre on 21/12/2017.
 * Call method to generate tournament
 */
public class TourCBOGeneratorTask extends TourFederationGeneratorTask<TourCBOTournament> {
    @Override
    public TourFederationMgr getTourFederationMgr() {
        return ContextManager.getTourCBOMgr();
    }
}
