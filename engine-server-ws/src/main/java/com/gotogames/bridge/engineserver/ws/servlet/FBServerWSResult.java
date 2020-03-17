package com.gotogames.bridge.engineserver.ws.servlet;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pserent on 03/04/2017.
 */
public class FBServerWSResult {
    public String command = "";
    public Map<String, Object> data = new HashMap<>();
    public String toString() {
        return "command="+command+" - nb data="+(data !=null? data.size():"null");
    }

}
