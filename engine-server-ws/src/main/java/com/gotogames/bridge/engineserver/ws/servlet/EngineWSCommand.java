package com.gotogames.bridge.engineserver.ws.servlet;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class EngineWSCommand {
    public String command;
    public Map<String, Object> params;

    public String toString() {
        return "command="+command+" - nb data="+(params!=null?params.size():"null");
    }

    public EngineWSCommandStats parseCommandStats() {
        EngineWSCommandStats cmd = new EngineWSCommandStats();
        if (params.containsKey("filter")) {
            cmd.filter = (String)params.get("filter");
        }
        if (params.containsKey("nbThread")) {
            cmd.nbThread = (Integer) params.get("nbThread");
        }
        if (params.containsKey("maxThread")) {
            cmd.maxThread = (Integer) params.get("maxThread");
        }
        if (params.containsKey("computeTime")) {
            cmd.computeTime = ((Double) params.get("computeTime")).intValue();
        }
        if (params.containsKey("queueSize")) {
            cmd.queueSize = (Integer) params.get("queueSize");
        }
        if (params.containsKey("engineList")) {
            cmd.engineList = (String) params.get("engineList");
        }
        if (params.containsKey("version")) {
            cmd.version = (String)params.get("version");
        }
        if (params.containsKey("discard")) {
            cmd.discard = (List<Long>) params.get("discard");
        }
        if (params.containsKey("values")) {
            cmd.values = new EngineWSCommandStatsValues();
            Map<String, Object> valuesMap = (Map<String, Object>)params.get("values");
            if (valuesMap.containsKey("start")) {
                cmd.values.start = (Long)valuesMap.get("start");
            }
            if (valuesMap.containsKey("elapsed")) {
                Object  o = valuesMap.get("elapsed");
                if (o instanceof Long) {
                    cmd.values.elapsed = (Long) o;
                } else if (o instanceof Integer) {
                    cmd.values.elapsed = (Integer) o;
                }
            }
            if (valuesMap.containsKey("count")) {
                Object  o = valuesMap.get("count");
                if (o instanceof Long) {
                    cmd.values.count = (Long) o;
                } else if (o instanceof Integer) {
                    cmd.values.count = (Integer) o;
                }
            }
            if (valuesMap.containsKey("compute")) {
                Object  o = valuesMap.get("compute");
                if (o instanceof Long) {
                    cmd.values.compute = (Long) o;
                } else if (o instanceof Integer) {
                    cmd.values.compute = (Integer) o;
                }
            }
        }
        return cmd;
    }

    public EngineWSCommandResult parseCommandResult() {
        EngineWSCommandResult cmd = new EngineWSCommandResult();
        if (params.containsKey("computeID")) {
            Object  o = params.get("computeID");
            if (o instanceof Long) {
                cmd.computeID = (Long) o;
            } else if (o instanceof Integer) {
                cmd.computeID = (Integer) o;
            }
        }
        if (params.containsKey("answer")) {
            cmd.answer = (String)params.get("answer");
        }
        if (params.containsKey("nbThread")) {
            cmd.nbThread = (Integer) params.get("nbThread");
        }
        if (params.containsKey("maxThread")) {
            cmd.maxThread = (Integer) params.get("maxThread");
        }
        if (params.containsKey("queueSize")) {
            cmd.queueSize = (Integer) params.get("queueSize");
        }
        if (params.containsKey("computeTime")) {
            cmd.computeTime = ((Double) params.get("computeTime")).intValue();
        }
        return cmd;
    }

    public static EngineWSCommand buildCommandCompute(long computeID, String deal, String game, String conventions, String options, int queryType, int nbTricksForClaim, String claimPlayer) {
        EngineWSCommand cmd = new EngineWSCommand();
        cmd.command = "compute";
        cmd.params = new HashMap<String, Object>();
        cmd.params.put("computeID", computeID);
        cmd.params.put("deal", deal);
        cmd.params.put("game", game);
        cmd.params.put("conventions", conventions);
        cmd.params.put("options", options);
        cmd.params.put("queryType", queryType);
        cmd.params.put("nbTricksForClaim", nbTricksForClaim);
        cmd.params.put("claimPlayer", claimPlayer);
        return cmd;
    }

    public static EngineWSCommand buildCommandStop() {
        EngineWSCommand cmd = new EngineWSCommand();
        cmd.command = "stop";
        cmd.params = new HashMap<String, Object>();
        return cmd;
    }

    public static EngineWSCommand buildCommandRestart(int delay) {
        EngineWSCommand cmd = new EngineWSCommand();
        cmd.command = "restart";
        cmd.params = new HashMap<String, Object>();
        cmd.params.put("delay", delay);
        return cmd;
    }

    public static EngineWSCommand buildCommandUpdate(String downloadURL) {
        EngineWSCommand cmd = new EngineWSCommand();
        cmd.command = "update";
        cmd.params = new HashMap<String, Object>();
        cmd.params.put("downloadURL", downloadURL);
        return cmd;
    }

    public static EngineWSCommand buildCommandReboot() {
        EngineWSCommand cmd = new EngineWSCommand();
        cmd.command = "reboot";
        cmd.params = new HashMap<String, Object>();
        return cmd;
    }

    public static EngineWSCommand buildCommandInit(String httpEngineURL) {
        EngineWSCommand cmd = new EngineWSCommand();
        cmd.command = "init";
        cmd.params = new HashMap<String, Object>();
        cmd.params.put("httpEngineURL", httpEngineURL);
        return cmd;
    }
}
