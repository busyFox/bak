package com.gotogames.bridge.engineserver.ws.servlet;

import com.gotogames.bridge.engineserver.ws.request.RequestService;

import java.util.Map;

/**
 * Created by pserent on 31/03/2017.
 */
public class FBServerWSCommand {
    public String command;
    public Map<String, Object> data;

    public String toString() {
        return "command="+command+" - nb data="+(data !=null? data.size():"null");
    }

    public RequestService.GetResultParam parseCommandGetResult() {
        RequestService.GetResultParam param = new RequestService.GetResultParam();
        if (data.containsKey("deal")) {
            param.deal = (String) data.get("deal");
        }
        if (data.containsKey("game")) {
            param.game = (String) data.get("game");
        }
        if (data.containsKey("conventions")) {
            param.conventions = (String) data.get("conventions");
        }
        if (data.containsKey("options")) {
            param.options = (String) data.get("options");
        }
        if (data.containsKey("requestType")) {
            param.requestType = (Integer) data.get("requestType");
        }
        if (data.containsKey("useCache")) {
            param.useCache = (Boolean) data.get("useCache");
        }
        if (data.containsKey("logStat")) {
            param.logStat = (Boolean) data.get("logStat");
        }
        if (data.containsKey("asyncID")) {
            param.asyncID = (String) data.get("asyncID");
        }
        return param;
    }
}
