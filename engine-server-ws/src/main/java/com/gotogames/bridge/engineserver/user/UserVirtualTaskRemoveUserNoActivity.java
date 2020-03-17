package com.gotogames.bridge.engineserver.user;

import com.gotogames.bridge.engineserver.common.ContextManager;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class UserVirtualTaskRemoveUserNoActivity implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        UserVirtualMgr mgr = ContextManager.getUserMgr();
        if (!mgr.taskRemoveUserNoActivityRunning && EngineConfiguration.getInstance().getIntValue("user.removeUserNoActivity.taskEnable", 1) == 1) {
            mgr.taskRemoveUserNoActivityRunning = true;
            try {
                mgr.processRemoseUserNoActivity();
            } catch (Exception e) {
                mgr.getLogger().error("UserVirtualTaskRemoveUserNoActivity - exception to processRemoseUserNoActivity", e);
            }
            mgr.taskRemoveUserNoActivityRunning = false;
        }
    }
}
