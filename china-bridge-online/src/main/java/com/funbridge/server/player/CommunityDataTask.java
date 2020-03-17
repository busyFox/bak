package com.funbridge.server.player;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class CommunityDataTask implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        PlayerMgr playerMgr = ContextManager.getPlayerMgr();
        if (!playerMgr.isCommunityDataTaskRunning()) {
            playerMgr.setCommunityDataTaskRunning(true);
            try {
                int defaultValue = -1;
                int tempNbActivePlayers = defaultValue;
                int tempNbCountryCode = defaultValue;
                if (FBConfiguration.getInstance().getIntValue("player.community.computeEnable", 0) == 1) {
                    tempNbActivePlayers = playerMgr.getPlayerDAO().countActivePlayers(FBConfiguration.getInstance().getIntValue("player.community.nbMonthBefore", 6));
                    playerMgr.setCommunityListCountryPlayer(playerMgr.getPlayerDAO().getListCountryPlayer(FBConfiguration.getInstance().getIntValue("player.community.nbMonthBefore", 6)));
                    if (playerMgr.getCommunityListCountryPlayer() != null) {
                        tempNbCountryCode = playerMgr.getCommunityListCountryPlayer().size();
                    }
                } else {
                    // get data from config file
                    tempNbActivePlayers = FBConfiguration.getInstance().getIntValue("player.community.dataStaticNbActivePlayers", defaultValue);
                    tempNbCountryCode = FBConfiguration.getInstance().getIntValue("player.community.dataStaticNbCountryCode", defaultValue);
                    playerMgr.setCommunityListCountryPlayer(null);
                }

                // set values only if different from default value
                if (tempNbActivePlayers != defaultValue) {
                    playerMgr.setCommunityNbActivePlayers(tempNbActivePlayers);
                }
                if (tempNbCountryCode != defaultValue) {
                    playerMgr.setCommunityNbCountryCode(tempNbCountryCode);
                }
            } catch (Exception e) {
                ContextManager.getAlertMgr().addAlert("PLAYER_MGR", FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to process community data", e.getMessage(), null);
            }
            playerMgr.setCommunityDataTaskRunning(false);
        } else {
            playerMgr.getLogger().error("CommunityDataTask - Process already running ...");
            ContextManager.getAlertMgr().addAlert("PLAYER_MGR", FunbridgeAlertMgr.ALERT_LEVEL_MODERATE, "Task already running for process community data", null, null);
        }
    }
}
