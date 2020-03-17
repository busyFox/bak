package com.funbridge.server.tournament.federation.cbo;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.federation.TourFederationEnableTask;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.federation.cbo.data.TourCBOTournament;

/**
 * Created by ldelbarre on 21/12/2017.
 * Call method to enable tournament
 */
public class TourCBOEnableTask extends TourFederationEnableTask<TourCBOTournament> {
    @Override
    public TourFederationMgr getTourFederationMgr() {
        return ContextManager.getTourCBOMgr();
    }
}
