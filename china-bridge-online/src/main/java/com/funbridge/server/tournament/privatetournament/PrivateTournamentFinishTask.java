package com.funbridge.server.tournament.privatetournament;

import com.funbridge.server.common.ContextManager;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by pserent on 08/02/2017.
 */
public class PrivateTournamentFinishTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        PrivateTournamentMgr tournamentMgr = ContextManager.getPrivateTournamentMgr();
        if (!tournamentMgr.finishTaskRunning && tournamentMgr.getConfigIntValue("finishTaskEnable", 1) == 1) {
            tournamentMgr.finishTaskRunning = true;
            tournamentMgr.finishExpiredTournament();
            tournamentMgr.finishTaskRunning = false;
        }
    }
}
