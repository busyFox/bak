package com.gotogames.bridge.engineserver.ws.servlet;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by pserent on 25/10/2016.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class EngineWSCommandResult {
    public long computeID;
    public String answer;
    public int nbThread;
    public int maxThread;
    public int queueSize;
    public int computeTime;

    public String toString() {
        return "computeID="+computeID+" - result="+answer+" - nbThread="+nbThread+" - queueSize="+queueSize+" - computeTime="+computeTime;
    }
}
