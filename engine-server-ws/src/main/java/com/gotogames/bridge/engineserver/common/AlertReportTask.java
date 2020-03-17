package com.gotogames.bridge.engineserver.common;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Calendar;

/**
 * Created by pserent on 28/12/2015.
 */
public class AlertReportTask implements Job{
    public AlertReportTask() {}
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (!ContextManager.getAlertMgr().reportTaskRunning) {
            ContextManager.getAlertMgr().reportTaskRunning = true;
            Calendar curDate = Calendar.getInstance();
            curDate.add(Calendar.DAY_OF_YEAR, -1);
            String paramDate = Constantes.timestamp2StringDate(curDate.getTimeInMillis());
            ContextManager.getAlertMgr().getLog().info("Begin process report for date=" + paramDate);
            ContextManager.getAlertMgr().sendMailReport(paramDate);
            ContextManager.getAlertMgr().getLog().info("End process report for date=" + paramDate);
            ContextManager.getAlertMgr().reportTaskRunning = false;
        }
    }
}
