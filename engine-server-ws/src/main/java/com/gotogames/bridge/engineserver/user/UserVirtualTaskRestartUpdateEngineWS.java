package com.gotogames.bridge.engineserver.user;

import com.gotogames.bridge.engineserver.common.ContextManager;
import com.gotogames.bridge.engineserver.common.EngineConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by pserent on 27/02/2017.
 */
public class UserVirtualTaskRestartUpdateEngineWS implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        UserVirtualMgr mgr = ContextManager.getUserMgr();
        if (!mgr.taskRestartUpdateEngineWSRunning && EngineConfiguration.getInstance().getIntValue("user.restartUpdateEngineWS.taskEnable", 1) == 1) {
            mgr.taskRestartUpdateEngineWSRunning = true;
            try {
                mgr.processRestartUpdateEngineWS();
            } catch (Exception e) {
                mgr.getLogger().error("UserVirtualTaskRestartEngineWS - exception to processRestartUpdateEngineWS", e);
            }
            mgr.taskRestartUpdateEngineWSRunning = false;
        }
    }
}
