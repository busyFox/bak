package com.gotogames.bridge.engineserver.user;

import com.gotogames.bridge.engineserver.common.ContextManager;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class UserVirtualTaskCheckEngineNoResult implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        UserVirtualMgr mgr = ContextManager.getUserMgr();
        if (!mgr.taskCheckEngineNoResultRunning && EngineConfiguration.getInstance().getIntValue("user.checkEngineNoResult.taskEnable", 1) == 1) {
            mgr.taskCheckEngineNoResultRunning = true;
            try {
                mgr.processEngineWithNoResult();
            } catch (Exception e) {
                mgr.getLogger().error("UserVirtualTaskCheckEngineNoResult - exception to processEngineWithNoResult", e);
            }
            mgr.taskCheckEngineNoResultRunning = false;
        }
    }
}
