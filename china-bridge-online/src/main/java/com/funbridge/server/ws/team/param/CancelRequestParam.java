package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 12/10/2016.
 */
public class CancelRequestParam {
    public String requestID;

    public String toString() {
        return "requestID=" + requestID;
    }

    public boolean isValid() {
        return requestID != null && !requestID.isEmpty();
    }
}
