package com.funbridge.server.message;

import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class NotifRankingAllPodiumsTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        MessageNotifMgr notifMgr = ContextManager.getMessageNotifMgr();
        try {
            notifMgr.getLogger().warn("Begin task to notif ranking all podiums ...");
            notifMgr.processNotifRanking();
            notifMgr.getLogger().warn("End task to notif ranking all podiums.");
        } catch (Exception e) {
            notifMgr.getLogger().error("Exception to process notif ranking", e);
        }
    }
}
