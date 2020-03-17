package com.funbridge.server.tournament.federation;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;

/**
 * Created by ldelbarre on 02/08/2017.
 * Call method to generate tournament
 */
public abstract class TourFederationGeneratorTask<TTournament> implements Job {

    public abstract TourFederationMgr getTourFederationMgr();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TourFederationMgr mgr = this.getTourFederationMgr();
        if (!mgr.generatorTaskRunning && mgr.getConfigIntValue("taskGenerator", 1) == 1) {
            mgr.generatorTaskRunning = true;
            try {
                mgr.getLogger().warn("Begin task to generate new tournament ...");
                List<TTournament> listTour = (List<TTournament>)mgr.checkTournamentToGenerate();
                mgr.getLogger().warn("End task to generate new tournament - listTour=" + (listTour != null ? listTour.size() : "null"));
            } catch (Exception e) {
                ContextManager.getAlertMgr().addAlert(mgr.getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to checkTournamentToGenerate", e.getMessage(), null);
            }
            mgr.generatorTaskRunning = false;
        } else {
            ContextManager.getAlertMgr().addAlert(mgr.getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for checkTournamentToGenerate", null, null);
        }

    }
}
