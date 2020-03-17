package com.funbridge.server.ws.notification.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by pserent on 17/11/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class NotificationDefaultResponse {
    public boolean result = false;
    public String log;

    public String toString() {
        return "result="+result+" - log="+log;
    }
}
