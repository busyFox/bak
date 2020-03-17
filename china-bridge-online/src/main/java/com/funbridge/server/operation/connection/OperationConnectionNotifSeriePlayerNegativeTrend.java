package com.funbridge.server.operation.connection;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.presence.FBSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ldelbarre on 12/09/2017.
 */
public class OperationConnectionNotifSeriePlayerNegativeTrend extends OperationConnection{
    public String notifGroupID;

    @Override
    public boolean needToCheckDateLastConnectionAtFirstProcess() {
        return true;
    }

    public static boolean isEnable() {
        return FBConfiguration.getInstance().getConfigBooleanValue("tournament.NEWSERIE.notifPlayerNegativeTrend.enable", true);
    }

    public static int getRemainingTime1() {
        return FBConfiguration.getInstance().getIntValue("tournament.NEWSERIE.notifPlayerNegativeTrend.remainingTime1", 144);
    }

    public static int getRemainingTime2() {
        return FBConfiguration.getInstance().getIntValue("tournament.NEWSERIE.notifPlayerNegativeTrend.remainingTime2", 48);
    }

    public OperationConnectionNotifSeriePlayerNegativeTrend(String name) {
        super(getOperationType(), name);
    }

    public String toString() {
        return "["+super.toString()+"] notifGroupID="+notifGroupID;
    }

    @Override
    public boolean areSpecificParameterValid() {
        return notifGroupID != null && notifGroupID.length() > 0;
    }

    public static String getOperationType() {
        return OperationConnectionNotifSeriePlayerNegativeTrend.class.getSimpleName();
    }

    public static List<String> getListSpecificParameters() {
        List<String> temp = new ArrayList<String>();
        temp.add("notifGroupID");
        return temp;
    }

    public static String getStringSpecificParameters() {
        List<String> temp = getListSpecificParameters();
        String result = "";
        for (String e : temp) {
            if (result.length() > 0) { result += Constantes.SEPARATOR_VALUE;}
            result+=e;
        }
        return result;
    }

    @Override
    public boolean setSpecificParametersValues(Map<String, String> parameters) {
        if (parameters != null) {
            if (parameters.containsKey("notifGroupID")) {
                notifGroupID = parameters.get("notifGroupID");
                return true;
            }
        }
        return false;
    }

    @Override
    public String getSpecificParametersNames() {
        return "notifGroupID";
    }

    @Override
    public String getSpecificParametersValues() {
        return notifGroupID;
    }

    @Override
    public boolean processStep2(FBSession session) {
        return true;
    }
}
