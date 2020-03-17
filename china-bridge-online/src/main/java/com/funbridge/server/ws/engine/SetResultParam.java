package com.funbridge.server.ws.engine;

import com.gotogames.common.tools.StringTools;

import java.util.List;

/**
 * Created by pserent on 13/04/2016.
 */
public class SetResultParam {
    public String result;
    public List<String> listAsyncID;

    public String toString() {
        return "result=" + result + " - listAsyncID=" + StringTools.listToString(listAsyncID);
    }
}
