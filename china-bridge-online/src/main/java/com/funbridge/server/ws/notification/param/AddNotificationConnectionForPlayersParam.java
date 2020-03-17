package com.funbridge.server.ws.notification.param;

import com.funbridge.server.ws.notification.WSNotifData;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 04/12/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class AddNotificationConnectionForPlayersParam {
    public List<Long> selectionPlayers = new ArrayList<>();
    public WSNotifData notifData;
    public String operationName;

    public String toString() {
        return "operationName="+operationName+" - selection players size="+(selectionPlayers!=null?selectionPlayers.size():0)+" - notifData={"+notifData+"}";
    }

    public boolean isValid() {
        if (operationName == null || operationName.length() == 0) {
            return false;
        }
        if (!notifData.isValid()) {
            return false;
        }
        return selectionPlayers != null && selectionPlayers.size() != 0;
    }
}
