package com.funbridge.server.tournament.federation;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;

/**
 * Created by ldelbarre on 02/08/2017.
 */
public abstract class TourFederationFinishTask<TTournament> implements Job {

    public abstract TourFederationMgr getTourFederationMgr();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TourFederationMgr mgr = this.getTourFederationMgr();
        if (!mgr.finishTaskRunning && mgr.getConfigIntValue("taskFinish", 1) == 1) {
            mgr.finishTaskRunning = true;
            try {
                mgr.getLogger().warn("Begin task to finish tournament ...");
                List<TTournament> listTour = (List<TTournament>)mgr.processFinishTournament();
                mgr.getLogger().warn("End task to finish tournament - listTour=" + (listTour != null ? listTour.size() : "null"));
            } catch (Exception e) {
                ContextManager.getAlertMgr().addAlert(mgr.getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to processFinishTournament", e.getMessage(), null);
            }
            mgr.finishTaskRunning = false;
        } else {
            ContextManager.getAlertMgr().addAlert(mgr.getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for processFinishTournament", null, null);
        }

    }
}
