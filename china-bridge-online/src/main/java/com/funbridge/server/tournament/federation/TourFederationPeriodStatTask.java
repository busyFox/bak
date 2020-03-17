package com.funbridge.server.tournament.federation;

import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class TourFederationPeriodStatTask implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TourFederationStatPeriodMgr tourFederationStatPeriodMgr = ContextManager.getTourFederationStatPeriodMgr();
        try {
            tourFederationStatPeriodMgr.getLogger().warn("Begin task to init stat period ...");
            boolean processOK = tourFederationStatPeriodMgr.initStatPeriod();
            tourFederationStatPeriodMgr.getLogger().warn("End task to init stat period. Process OK : " + processOK);
        } catch (Exception e) {
            tourFederationStatPeriodMgr.getLogger().error("Exception to initStatPeriod", e);
        }
    }
}
