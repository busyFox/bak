package com.funbridge.server.ws.notification.param;

import com.funbridge.server.ws.notification.WSNotifData;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 04/11/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class SendNotificationNowParam {
    public boolean allPlayers = false;
    public List<Long> selectionPlayers = new ArrayList<>();
    public WSNotifData notifData;

    public String toString() {
        return "allPlayers="+allPlayers+" - selection players size="+(selectionPlayers!=null?selectionPlayers.size():0)+" - notifData={"+notifData+"}";
    }

    public boolean isValid() {
        if (!notifData.isValid()) {
            return false;
        }
        return allPlayers || (selectionPlayers != null && selectionPlayers.size() != 0);
    }
}
