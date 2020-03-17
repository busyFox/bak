package com.gotogames.bridge.engineserver.common;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class AlertCheckTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        AlertMgr alertMgr = ContextManager.getAlertMgr();
        if (alertMgr.isCheckTaskEnable() && !alertMgr.checkTaskRunning) {
            alertMgr.checkTaskRunning = true;
            alertMgr.processCheckAlert();
            alertMgr.checkTaskRunning = false;
        }
    }
}
