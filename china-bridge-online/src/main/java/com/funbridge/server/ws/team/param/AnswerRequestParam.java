package com.funbridge.server.ws.team.param;

/**
 * Created by pserent on 12/10/2016.
 */
public class AnswerRequestParam {
    public String requestID;
    public boolean accept;

    public String toString() {
        return "requestID=" + requestID + " - accept=" + accept;
    }

    public boolean isValid() {
        return requestID != null && !requestID.isEmpty();
    }
}
