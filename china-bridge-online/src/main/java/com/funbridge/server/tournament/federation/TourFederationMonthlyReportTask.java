package com.funbridge.server.tournament.federation;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Calendar;

/**
 * Created by ldelbarre on 02/08/2017.
 */
public abstract class TourFederationMonthlyReportTask implements Job {

    public abstract TourFederationMgr getTourFederationMgr();

    @Override
    public void execute(JobExecutionContext context) {
        TourFederationMgr mgr = this.getTourFederationMgr();
        if (!mgr.monthlyTaskRunning && mgr.getConfigIntValue("taskMonthlyReport", 1) == 1) {
            mgr.monthlyTaskRunning = true;
            try {
                mgr.getLogger().warn("Begin task to send monthly report to "+mgr.getFederationName());
                // Send monthly report for previous month
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, -1);
                mgr.generateMonthlyReport(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR), true);
                mgr.getLogger().warn("End task to send monthly report to "+mgr.getFederationName());
            } catch (Exception e) {
                ContextManager.getAlertMgr().addAlert(mgr.getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed", e.getMessage(), null);
            }
            mgr.monthlyTaskRunning = false;
        } else {
            ContextManager.getAlertMgr().addAlert(mgr.getTournamentCategoryName(), FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for monthly report", null, null);
        }

    }
}
