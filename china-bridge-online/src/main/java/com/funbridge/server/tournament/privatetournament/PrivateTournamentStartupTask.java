package com.funbridge.server.tournament.privatetournament;

import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by pserent on 30/01/2017.
 */
public class PrivateTournamentStartupTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        PrivateTournamentMgr tournamentMgr = ContextManager.getPrivateTournamentMgr();
        if (!tournamentMgr.startupTaskRunning && tournamentMgr.getConfigIntValue("startupTaskEnable", 1) == 1) {
            tournamentMgr.startupTaskRunning = true;
            tournamentMgr.startupTournaments();
            tournamentMgr.startupTaskRunning = false;
        }
    }
}
