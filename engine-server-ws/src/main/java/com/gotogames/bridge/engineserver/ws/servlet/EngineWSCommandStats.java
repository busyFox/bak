package com.gotogames.bridge.engineserver.ws.servlet;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;

/**
 * Created by pserent on 25/10/2016.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class EngineWSCommandStats {
    public String filter;
    public int maxThread;
    public int nbThread;
    public int queueSize;
    public List<Long> discard;
    public EngineWSCommandStatsValues values;
    public String engineList;
    public String version;
    public int computeTime;

    public String toString() {
        return "version="+version+" - filter="+filter+" - nbThread="+nbThread+" - queueSize="+queueSize+" - computeTime="+computeTime+" - discard size="+(discard!=null?discard.size():"null");
    }
}
