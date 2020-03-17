package com.funbridge.server.tournament.federation.cbo;

import com.funbridge.server.common.ContextManager;
import com.funbridge.server.tournament.federation.TourFederationMgr;
import com.funbridge.server.tournament.federation.TourFederationMonthlyReportTask;

/**
 * Created by ldelbarre on 21/12/2017.
 */
public class TourCBOMonthlyReportTask extends TourFederationMonthlyReportTask {
    @Override
    public TourFederationMgr getTourFederationMgr() {
        return ContextManager.getTourCBOMgr();
    }
}
