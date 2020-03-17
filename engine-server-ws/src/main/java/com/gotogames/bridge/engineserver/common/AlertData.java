package com.gotogames.bridge.engineserver.common;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 28/12/2015.
 */
@Document(collection="alert")
public class AlertData {
    @Id
    private ObjectId ID;

    @Indexed
    public long date;
    public String deal;
    @Indexed
    public int engineVersion;
    public String conventions;
    public int requestType;
    public String result;
    public String engineLogin;
    public String request;
    public String game;
    public String options;

    public String message;
    public String pbn;

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    public String toString() {
        return "ID="+getIDStr()+" - date="+Constantes.timestamp2StringDateHour(date)+" - request="+request+" - engineLogin="+engineLogin+" - message="+message;
    }
}
