package com.gotogames.bridge.engineserver.request;

import com.gotogames.bridge.engineserver.common.ContextManager;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class QueueCompareRemoveOldest implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        QueueMgr queueMgr = ContextManager.getQueueMgr();
        if (!queueMgr.taskRemoveOldestCompareRunning && EngineConfiguration.getInstance().getIntValue("user.engineForCompare.taskRemoveOldestEnable", 1) == 1) {
            queueMgr.taskRemoveOldestCompareRunning = true;
            try {
                queueMgr.removeOldestQueueCompareDataList();
            } catch (Exception e) {
                queueMgr.getLog().error("QueueCompareRemoveOldest - exception to removeOldestQueueCompareDataList", e);
            }
            queueMgr.taskRemoveOldestCompareRunning = false;
        }
    }
}
