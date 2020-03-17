package com.funbridge.server.tournament.federation;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;

/**
 * Created by ldelbarre on 02/08/2017.
 * Call method to enable tournament
 */
public abstract class TourFederationEnableTask<TTournament> implements Job {

    public abstract TourFederationMgr getTourFederationMgr();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TourFederationMgr mgr = this.getTourFederationMgr();
        if (!mgr.enableTaskRunning && mgr.getConfigIntValue("taskEnable", 1) == 1) {
            mgr.enableTaskRunning = true;
            try {
                mgr.getLogger().warn("Begin task to enable tournament ...");
                List<TTournament> listTour = (List<TTournament>)mgr.processPrepareTournament();
                mgr.getLogger().warn("End task to enable tournament - listTour=" + (listTour != null ? listTour.size() : "null"));
            } catch (Exception e) {
                ContextManager.getAlertMgr().addAlert(mgr.getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to processPrepareTournament", e.getMessage(), null);
            }
            mgr.enableTaskRunning = false;
        } else {
            ContextManager.getAlertMgr().addAlert(mgr.getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for processPrepareTournament", null, null);
        }
    }
}
