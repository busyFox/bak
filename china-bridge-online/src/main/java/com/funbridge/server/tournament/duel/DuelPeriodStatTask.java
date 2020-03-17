package com.funbridge.server.tournament.duel;

import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class DuelPeriodStatTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        DuelMgr duelMgr = ContextManager.getDuelMgr();
        try {
            duelMgr.initStatPeriod();
        } catch (Exception e) {
            duelMgr.getLogger().error("Exception to initStatPeriod", e);
        }
    }
}
