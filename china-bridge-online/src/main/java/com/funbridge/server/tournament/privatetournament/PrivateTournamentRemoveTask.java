package com.funbridge.server.tournament.privatetournament;

import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by pserent on 08/03/2017.
 */
public class PrivateTournamentRemoveTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        PrivateTournamentMgr tournamentMgr = ContextManager.getPrivateTournamentMgr();
        if (!tournamentMgr.removeTaskRunning && tournamentMgr.getConfigIntValue("removeTaskEnable", 1) == 1) {
            tournamentMgr.removeTaskRunning = true;
            tournamentMgr.removeExpiredProperties(0);
            tournamentMgr.removeTaskRunning = false;
        }
    }
}
