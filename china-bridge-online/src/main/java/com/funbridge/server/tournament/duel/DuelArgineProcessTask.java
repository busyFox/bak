package com.funbridge.server.tournament.duel;

import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by pserent on 12/07/2017.
 */
public class DuelArgineProcessTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        DuelMgr duelMgr = ContextManager.getDuelMgr();
        if (duelMgr.getConfigIntValue("duelArgineThread.taskEnable", 1) == 1) {
            try {
                duelMgr.runThreadArgineDuelInProgress();
            } catch (Exception e) {
                duelMgr.getLogger().error("Exception to runThreadArgineDuelInProgress", e);
            }
        }
    }
}
