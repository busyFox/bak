package com.funbridge.server.operation.connection;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.presence.FBSession;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

/**
 * Created by pserent on 02/12/2015.
 */
@Document(collection="operation_connection")
public abstract class OperationConnection {
    @Id
    public ObjectId ID;
    @Indexed
    public boolean enable;
    @Indexed
    public String type;

    public String name;
    @Indexed
    public long dateExpiration; // date expiration of operation
    public long dateStart; // date start of operation
    public long dateLastConnectionBefore; // the date of last connection of player must be before this date
    public String onlyPlayer;
    public int executionOrder = 0;

    public OperationConnection(String type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return "ID="+(ID!=null?ID.toString():"null")+" - type="+type+" - name="+name+" - enable="+enable+
                " - dateStart="+(dateStart!=0?Constantes.timestamp2StringDateHour(dateStart):0)+" - checkDateStart="+checkDateStart()+
                " - dateExpiration="+(dateExpiration!=0?Constantes.timestamp2StringDateHour(dateExpiration):0)+" - checkDateExpiration="+checkDateExpiration()+
                " - dateLastConnectionBefore="+(dateLastConnectionBefore != 0?Constantes.timestamp2StringDateHour(dateLastConnectionBefore):0)+
                " - onlyPlayer="+onlyPlayer;
    }

    /**
     * Check playerID is in selection of onlyPlayer (onlyPlayer empty => all)
     * @param playerID
     * @return
     */
    public boolean checkPlayer(long playerID) {
        if (onlyPlayer != null && onlyPlayer.length() > 0) {
            String[] temp = onlyPlayer.split(Constantes.SEPARATOR_VALUE);
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
     * Check current date < param dateExpiration
     * @return
     */
    public boolean checkDateExpiration() {
        if (dateExpiration > 0) {
            return System.currentTimeMillis() < dateExpiration;
        }
        return true;
    }

    /**
     * Check if player date last connection < date last connection (only player not connected since the date specified by the param dateLastConnection)
     * @return
     */
    public boolean checkDateLastConnection(long playerDateLastConnection) {
        return dateLastConnectionBefore == 0 || playerDateLastConnection < dateLastConnectionBefore;
    }

    /**
     * Check if current time > dateStart
     * @return
     */
    public boolean checkDateStart() {
        if (dateStart > 0) {
            return System.currentTimeMillis() > dateStart;
        }
        return true;
    }

    /**
     * Execute process operation on session
     * @param session
     * @return
     */
    public boolean process(FBSession session) {
        if (session != null) {
            if (checkDateStart() &&
                    checkDateExpiration() &&
                    checkPlayer(session.getPlayer().getID()) &&
                    // need to check date last connection ? TRUE => call checkDateLastConnection
                    (!needToCheckDateLastConnectionAtFirstProcess() || checkDateLastConnection(session.getDateLastConnection())) &&
                    areSpecificParameterValid()) {
                return processStep2(session);
            }
        }
        return false;
    }

    /**
     * Should dateLastConnection be check at firstProcess ?
     * @return
     */
    public abstract boolean needToCheckDateLastConnectionAtFirstProcess();

    /**
     * Execute the step2 on the user define by the session
     * @param session
     * @return
     */
    public abstract boolean processStep2(FBSession session);

    /**
     * Set values for specific parameters (parameters defined in kind class)
     * @param parameters
     * @return
     */
    public abstract boolean setSpecificParametersValues(Map<String, String> parameters);

    /**
     * Return a string with all parameters separated with a ';'
     * @return
     */
    public abstract String getSpecificParametersValues();

    public abstract String getSpecificParametersNames();

    /**
     * Check if specific parameters are correctly set (not empty or null)
     * @return
     */
    public abstract boolean areSpecificParameterValid();
}
