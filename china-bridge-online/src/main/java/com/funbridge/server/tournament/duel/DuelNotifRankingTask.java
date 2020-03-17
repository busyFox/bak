package com.funbridge.server.tournament.duel;

import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class DuelNotifRankingTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        DuelMgr duelMgr = ContextManager.getDuelMgr();
        try {
            duelMgr.getLogger().warn("Begin task to notif ranking ...");
            duelMgr.processNotifRanking();
            duelMgr.getLogger().warn("End task to notif ranking.");
        } catch (Exception e) {
            duelMgr.getLogger().error("Exception to process notif ranking", e);
        }
    }
}
