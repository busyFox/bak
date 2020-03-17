package com.funbridge.server.operation;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.presence.FBSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by pserent on 13/03/2014.
 * Abstract object to define an operation to be execute on a session
 */
public abstract class Operation {
    private String name;
    protected Logger log = LogManager.getLogger(this.getClass());

    public String getName() {
        return name;
    }

    /**
     * Return the order of execution from config
     * @return
     */
    public int getExecutionOrder() {
        return getConfigInt("executionOrder", 0);
    }

    /**
     * Check if this operation is enable
     * @return
     */
    public boolean isEnable() {
        return getConfigInt("enable", 0) == 1;
    }

    /**
     * Check is this operation is to be called on connection
     * @return
     */
    public boolean isOnConnection() {
        return getConfigInt("onConnection", 0) == 1;
    }

    /**
     * read string value for param (key) from config file
     * @param param
     * @param defaultValue
     * @return
     */
    public String getConfigString(String param, String defaultValue) {
        return FBConfiguration.getInstance().getStringValue("operation."+name+"."+param, defaultValue);
    }

    /**
     * read int value for param (key) from config file
     * @param param
     * @param defaultValue
     * @return
     */
    public int getConfigInt(String param, int defaultValue) {
        return FBConfiguration.getInstance().getIntValue("operation." + name + "." + param, defaultValue);
    }

    /**
     * read long value for param (key) from config file
     * @param param
     * @param defaultValue
     * @return
     */
    public long getConfigLong(String param, long defaultValue) {
        return FBConfiguration.getInstance().getLongValue("operation." + name + "." + param, defaultValue);
    }

    @Override
    public String toString() {
        return "Operation name="+name+
                " - dateStart="+(getParamDateStart()!=0?Constantes.timestamp2StringDateHour(getParamDateStart()):0)+" - checkDateStart="+checkDateStart()+
                " - dateExpiration="+(getParamDateExpiration()!=0?Constantes.timestamp2StringDateHour(getParamDateExpiration()):0)+" - checkDateExpiration="+checkDateExpiration()+
                " - dateLastConnectionBefore="+(getParamDateLastConnecionBefore() != 0?Constantes.timestamp2StringDateHour(getParamDateLastConnecionBefore()):0)+
                " - onlyPlayer="+getParamOnlyPlayer();
    }

    /**
     * Execute the operation on the user define by the session
     * @param session
     * @return
     */
    public abstract boolean process(FBSession session);

    /**
     * Initialize the operation parameters
     * @param name
     */
    public void init(String name) {
        this.name = name;
    }

    /**
     * return param date start
     * @return
     */
    public long getParamDateStart() {
        return getConfigLong("dateStart", 0);
    }

    /**
     * Check if current time > dateStart
     * @return
     */
    public boolean checkDateStart() {
        long dateStart = getParamDateStart();
        if (dateStart > 0) {
            return System.currentTimeMillis() > dateStart;
        }
        return true;
    }

    /**
     * Return param dateLastConnection
     * @return
     */
    public long getParamDateLastConnecionBefore() {
        return getConfigLong("dateLastConnectionBefore", 0);
    }

    /**
     * Check if player date last connection < param date last connection (only player not connected since the date specified by the param dateLastConnection)
     * @return
     */
    public boolean checkDateLastConnection(long playerDateLastConnection) {
        long dateLastConnectionBefore = getParamDateLastConnecionBefore();
        return dateLastConnectionBefore == 0 || playerDateLastConnection < dateLastConnectionBefore;
    }

    /**
     * Return param onlyPlayer
     * @return
     */
    public String getParamOnlyPlayer() {
        return getConfigString("onlyPlayer", null);
    }

    /**
     * Check playerID is in selection of onlyPlayer (onlyPlayer empty => all)
     * @param playerID
     * @return
     */
    public boolean checkPlayer(long playerID) {
        String onlyPlayer = getParamOnlyPlayer();
        if (onlyPlayer != null && onlyPlayer.length() > 0) {
            String[] temp = onlyPlayer.split(",");
            for (String s : temp) {
                if (s.equals(""+playerID)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Return param dateExpiration
     * @return
     */
    public long getParamDateExpiration() {
        return getConfigLong("dateExpiration", 0);
    }

    /**
     * Check current date < param dateExpiration
     * @return
     */
    public boolean checkDateExpiration() {
        long dateExpiration = getParamDateExpiration();
        if (dateExpiration > 0) {
            return System.currentTimeMillis() < dateExpiration;
        }
        return true;
    }
}
