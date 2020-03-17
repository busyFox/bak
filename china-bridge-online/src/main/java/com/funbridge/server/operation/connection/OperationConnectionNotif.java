package com.funbridge.server.operation.connection;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.message.data.MessageNotifGroup;
import com.funbridge.server.presence.FBSession;
import com.gotogames.common.tools.StringVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by pserent on 02/12/2015.
 */
public class OperationConnectionNotif extends OperationConnection {
    public String notifGroupID;
    public String playerLang;
    public String deviceType;
    public String playerCountry;
    public String clientVersion;
    public String playerClient;
    public String playerSegmentation;

    public OperationConnectionNotif(String name) {
        super(getOperationType(), name);
    }

    public String toString() {
        return "["+super.toString()+"] playerLang="+playerLang+" - deviceType="+deviceType+" - playerCountry="+playerCountry+" - clientVersion="+ clientVersion +" - playerSegmentation="+playerSegmentation+" - playerClient="+playerClient+" - notifGroupID="+notifGroupID;
    }

    public static String getOperationType() {
        return OperationConnectionNotif.class.getSimpleName();
    }

    public static List<String> getListSpecificParameters () {
        List<String> temp = new ArrayList<String>();
        temp.add("notifGroupID");
        temp.add("playerLang");
        temp.add("deviceType");
        temp.add("playerCountry");
        temp.add("clientVersion");
        temp.add("playerClient");
        temp.add("playerSegmentation");
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
    public String getSpecificParametersNames() {
        String result = "notifGroupID";
        if (playerLang != null && playerLang.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+"playerLang";
        }
        if (deviceType != null && deviceType.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+"deviceType";
        }
        if (playerCountry != null && playerCountry.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+"playerCountry";
        }
        if (clientVersion != null && clientVersion.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+"clientVersion";
        }
        if (playerClient != null && playerClient.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+"playerClient";
        }
        if (playerSegmentation != null && playerSegmentation.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+"playerSegmentation";
        }
        return result;
    }

    @Override
    public String getSpecificParametersValues() {
        String result = notifGroupID;
        if (playerLang != null && playerLang.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+playerLang;
        }
        if (deviceType != null && deviceType.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+deviceType;
        }
        if (playerCountry != null && playerCountry.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+playerCountry;
        }
        if (clientVersion != null && clientVersion.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+ clientVersion;
        }
        if (playerClient != null && playerClient.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+ playerClient;
        }
        if (playerSegmentation != null && playerSegmentation.length() > 0) {
            result += Constantes.SEPARATOR_VALUE+ playerSegmentation;
        }
        return result;
    }

    @Override
    public boolean setSpecificParametersValues(Map<String, String> parameters) {
        if (parameters != null) {
            if (parameters.containsKey("notifGroupID")) {
                notifGroupID = parameters.get("notifGroupID");
                if (parameters.containsKey("playerLang")) {
                    playerLang = parameters.get("playerLang");
                }
                if (parameters.containsKey("deviceType")) {
                    deviceType = parameters.get("deviceType");
                }
                if (parameters.containsKey("playerCountry")) {
                    playerCountry = parameters.get("playerCountry");
                }
                if (parameters.containsKey("clientVersion")) {
                    clientVersion = parameters.get("clientVersion");
                }
                if (parameters.containsKey("playerClient")) {
                    playerClient = parameters.get("playerClient");
                }
                if (parameters.containsKey("playerSegmentation")) {
                    playerSegmentation = parameters.get("playerSegmentation");
                }
                return true;
            }
        }
        return false;
    }

    /**
     * At least notifGroupID param must be defined
     * @return
     */
    @Override
    public boolean areSpecificParameterValid() {
        return notifGroupID != null && notifGroupID.length() > 0;
    }

    /**
     * Check lang value. If no playerLang is defined => no ckeck => return true
     * @param valueLang
     * @return
     */
    public boolean checkPlayerLang(String valueLang) {
        if (playerLang != null && playerLang.length() > 0) {
            if (valueLang != null && valueLang.length() > 0) {
                // all value possible
                if (playerLang.equals("*")) {
                    return true;
                }
                // same value
                if (playerLang.equalsIgnoreCase(valueLang)) {
                    return true;
                }
                // different value if param starts with !
                if (playerLang.length() > 1 && playerLang.charAt(0) == '!') {
                    return !playerLang.substring(1).equalsIgnoreCase(valueLang);
                }
            }
            // value lang is null, empty or different
            return false;
        } else {
            // no lang specify => no check for lang
            return true;
        }
    }

    @Override
    public boolean needToCheckDateLastConnectionAtFirstProcess() {
        if (deviceType != null && deviceType.length() > 0) {
            return deviceType.equals("*");
        }
        return true;
    }

    /**
     * Check device value. If no deviceType is defined => no ckeck => return true
     * @param currentDeviceType
     * @param lastDeviceTypeUsed
     * @param dateLastConnection
     * @return
     */
    public boolean checkDeviceType(String currentDeviceType, String lastDeviceTypeUsed, long dateLastConnection) {
        if (deviceType != null && deviceType.length() > 0) {
            if (currentDeviceType != null && currentDeviceType.length() > 0) {
                // all value possible
                if (deviceType.equals("*")) {
                    return true;
                }
                // same value
                if (deviceType.equalsIgnoreCase(currentDeviceType)) {
                    if (checkDateLastConnection(dateLastConnection)) {
                        return true;
                    } else {
                        // check date failed but the device type is correct => check if previous device type is not another device type
                        if (lastDeviceTypeUsed != null && lastDeviceTypeUsed.length() > 0) {
                            if (!deviceType.equals(lastDeviceTypeUsed)) {
                                return true;
                            }
                        }
                    }
                }
                // different value if param starts with !
                if (deviceType.length() > 1 && deviceType.charAt(0) == '!') {
                    if (!deviceType.substring(1).equalsIgnoreCase(currentDeviceType)) {
                        if (checkDateLastConnection(dateLastConnection)) {
                            return true;
                        } else {
                            if (lastDeviceTypeUsed != null && lastDeviceTypeUsed.length() > 0) {
                                return deviceType.substring(1).equals(lastDeviceTypeUsed);
                            }
                        }
                    }
                }
            }
            // value type is null, empty or different
            return false;
        } else {
            // no device specify => no check for device
            return true;
        }
    }

    /**
     * Check client version. If no client version defined => no check. Client version must have format : <1.2.3 or >1.2.3 or =1.2.3
     * @param valueVersion must have format 1.2.3
     * @return
     */
    public boolean checkClientVersion(String valueVersion) {
        if (clientVersion != null && clientVersion.length() > 0) {
            if (valueVersion != null && valueVersion.length() > 0) {
                String versionToCompare = null;
                if (clientVersion.charAt(0) == '<') {
                    // value client must be < clientVersion
                    return StringVersion.compareVersion(valueVersion, clientVersion.substring(1)) < 0;
                }
                if (clientVersion.charAt(0) == '>') {
                    // value client must be > clientVersion
                    return StringVersion.compareVersion(valueVersion, clientVersion.substring(1)) > 0;
                }
                if (clientVersion.charAt(0) == '=') {
                    // value client must be = clientVersion
                    return StringVersion.compareVersion(valueVersion, clientVersion.substring(1)) == 0;
                }
            }
            return false;
        } else {
            // no client version specify => no check
            return true;
        }
    }

    /**
     * Check country value. If no playerCountry is defined => no ckeck => return true
     * @param valueCountry
     * @return
     */
    public boolean checkPlayerCountry(String valueCountry) {
        if (playerCountry != null && playerCountry.length() > 0) {
            if (valueCountry != null && valueCountry.length() > 0) {
                // all value possible
                if (playerCountry.equals("*")) {
                    return true;
                }
                // same value
                if (playerCountry.equalsIgnoreCase(valueCountry)) {
                    return true;
                }
                // different value if param starts with !
                if (playerCountry.length() > 1 && playerCountry.charAt(0) == '!') {
                    return !playerCountry.substring(1).equalsIgnoreCase(valueCountry);
                }
            }
            // value country is null, empty or different
            return false;
        } else {
            // no playerCountry specify => no check for country
            return true;
        }
    }

    @Override
    public boolean processStep2(FBSession session) {
        if (session != null &&
                checkPlayerLang(session.getPlayer().getDisplayLang()) &&
                checkDeviceType(session.getDeviceType(), session.getLastDeviceTypeUsed(), session.getDateLastConnection()) &&
                checkClientVersion(session.getClientVersion()) &&
                checkPlayerCountry(session.getPlayer().getDisplayCountryCode())) {
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
