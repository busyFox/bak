package com.funbridge.server.ws.notification;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by pserent on 04/11/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSNotifResponse {

    private String exception;
    private Object data;

    public String getException() {
        return exception;
    }
    public void setException(String exception) {
        this.exception = exception;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }

    public boolean isResponseException() {
        return exception != null && exception.length() > 0;
    }

    public String toString() {
        return "exception="+exception+" - data="+data;
    }
}
