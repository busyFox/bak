package com.funbridge.server.tournament.team;

import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by pserent on 21/11/2016.
 */
public class TourTeamProcessTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        TourTeamMgr mgr = ContextManager.getTourTeamMgr();
        if (mgr.getConfigIntValue("processThreadEnable", 1) == 1) {
            try {
                mgr.runProcessForPeriod();
            } catch (Exception e) {
                mgr.getLogger().error("Failed to run process for period !", e);

            }
        }
    }
}
