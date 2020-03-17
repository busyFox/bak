package com.funbridge.server.operation.connection;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotifGroup;
import com.funbridge.server.presence.FBSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by pserent on 02/12/2015.
 */
public class OperationConnectionNotifPlayerList extends OperationConnection{
    public List<Long> listPlayerID = new ArrayList<>();
    public String notifGroupID;

    @Override
    public boolean needToCheckDateLastConnectionAtFirstProcess() {
        return true;
    }

    public OperationConnectionNotifPlayerList(String name) {
        super(getOperationType(), name);
    }

    public String toString() {
        return "["+super.toString()+"] notifGroupID="+notifGroupID+" - listPlayerID size="+(listPlayerID!=null?listPlayerID.size():"null");
    }

    @Override
    public boolean areSpecificParameterValid() {
        return notifGroupID != null && notifGroupID.length() > 0;
    }

    public static String getOperationType() {
        return OperationConnectionNotifPlayerList.class.getSimpleName();
    }

    public static List<String> getListSpecificParameters() {
        List<String> temp = new ArrayList<String>();
        temp.add("notifGroupID");
        temp.add("listPlayerID");
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
                if (parameters.containsKey("listPlayerID")) {
                    try {
                        String tempListPlayerID = parameters.get("listPlayerID");
                        listPlayerID.clear();
                        String[] tempValue = tempListPlayerID.split("-");
                        for (String e : tempValue) {
                            listPlayerID.add(Long.parseLong(e));
                        }
                    }catch (Exception e) {}
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public String getSpecificParametersNames() {
        String result = "notifGroupID";
        if (listPlayerID.size() < 100) {
            result += Constantes.SEPARATOR_VALUE+"listPlayerID";
        }
        return result;
    }

    @Override
    public String getSpecificParametersValues() {
        if (listPlayerID.size() < 100) {
            StringBuffer temp = new StringBuffer();
            for (Long e : listPlayerID) {
                if (temp.length() > 0) {
                    temp.append("-");
                }
                temp.append(e);
            }
            return notifGroupID+Constantes.SEPARATOR_VALUE+temp.toString();
        } else {
            return notifGroupID;
        }
    }

    @Override
    public boolean processStep2(FBSession session) {
        if (session != null && listPlayerID != null && listPlayerID.size() > 0 && listPlayerID.contains(session.getPlayer().getID())) {
            MessageNotifMgr notifMgr = ContextManager.getMessageNotifMgr();
            if (notifGroupID != null) {
                // retrieve the notifGroup
                MessageNotifGroup notifGroup = notifMgr.getNotifGroup(notifGroupID);
                // check notif not expired
                if (notifGroup != null && !notifGroup.isExpiredForDate(System.currentTimeMillis())) {
                    // check player has not yet this notif
                    if (notifMgr.getNotifGroupReadForPlayer(notifGroupID, session.getPlayer().getID()) == null) {
                        // create a NotifGroupReader
                        notifMgr.insertNotifGroupReadForPlayer(notifGroup, session.getPlayer().getID());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
