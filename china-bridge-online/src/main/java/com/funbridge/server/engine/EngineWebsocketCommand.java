package com.funbridge.server.engine;

import com.funbridge.server.ws.engine.SetResultParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pserent on 31/03/2017.
 */
public class EngineWebsocketCommand {
    public String command;
    public Map<String, Object> data = new HashMap<>();

    public String toString() {
        return "command="+command+" - nb data="+(data !=null? data.size():"null");
    }

    public SetResultParam parseCommandSetResult() {
        SetResultParam param = new SetResultParam();
        if (data.containsKey("result")) {
            param.result = (String) data.get("result");
        }
        if (data.containsKey("listAsyncID")) {
            param.listAsyncID = (List<String>)data.get("listAsyncID");
        }
        return param;
    }
}
