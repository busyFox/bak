package com.gotogames.bridge.engineserver.request;

import com.gotogames.bridge.engineserver.common.ContextManager;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class QueueTestRemoveOldest implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        QueueMgr queueMgr = ContextManager.getQueueMgr();
        if (!queueMgr.taskRemoveOldestTestRunning && EngineConfiguration.getInstance().getIntValue("user.engineForTest.taskRemoveOldestEnable", 1) == 1) {
            queueMgr.taskRemoveOldestTestRunning = true;
            try {
                queueMgr.removeOldestTestQueueDataList();
            } catch (Exception e) {
                queueMgr.getLog().error("QueueTestRemoveOldest - exception to removeOldestTestQueueDataList", e);
            }
            queueMgr.taskRemoveOldestTestRunning = false;
        }
    }
}
