package com.funbridge.server.ws.notification.param;

import com.funbridge.server.ws.notification.WSNotifData;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by pserent on 07/12/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class AddNotificationConnectionParam {
    public WSNotifData notifData;
    public String operationName;
    public String playerLang;
    public String deviceType;
    public String playerCountry;
    public String playerSegmentation;

    public String toString() {
        return "operationName="+operationName+" - playerLang="+playerLang+" - deviceType="+deviceType+" - playerCountry="+playerCountry+" - playerSegmentation="+playerSegmentation+" - notifData={"+notifData+"}";
    }

    public boolean isValid() {
        if (operationName == null || operationName.length() == 0) {
            return false;
        }
        return notifData.isValid();
    }
}
