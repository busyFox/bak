package com.funbridge.server.tournament.team;

import com.funbridge.server.alert.FunbridgeAlertMgr;
import com.funbridge.server.common.ContextManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 23/12/2016.
 */
public class TourTeamThreadPushMessage implements Runnable {
    private List<Long> listPlayerID = new ArrayList<>();
    private String messageTemplate;

    public TourTeamThreadPushMessage(String template) {
        this.messageTemplate = template;
    }

    public void addPlayers(List<Long> list) {
        if (list != null) {
            listPlayerID.addAll(list);
        }
    }

    @Override
    public void run() {
        try {
            if (listPlayerID != null && messageTemplate != null) {
                ContextManager.getTourTeamMgr().getLogger().warn("Nb players=" + listPlayerID.size() + " - template=" + messageTemplate);
            } else {
                ContextManager.getTourTeamMgr().getLogger().error("List playerID or template are null !");
            }
        } catch (Exception e) {
            ContextManager.getAlertMgr().addAlert("TEAM", FunbridgeAlertMgr.ALERT_LEVEL_HIGH, "Task failed to pushTeams", e.getMessage(), "messageTemplate="+messageTemplate+" - listPlayerID size="+(listPlayerID!=null?listPlayerID.size():null));
        }
    }
}
