package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

/**
 * Created by bplays on 08/11/16.
 */
public class GetMessagesParam {
    public String chatroomID;
    public int categoryID=-1;
    public int offset;
    public int nbMax;
    public Date minDate;
    public Date maxDate;

    @JsonIgnore
    public boolean isValid(){
        return chatroomID != null && !chatroomID.isEmpty();
    }

    @JsonIgnore
    public String toString(){
        return "chatroomID="+chatroomID+" - categoryID="+categoryID+" - offset="+offset+" - nbMax="+nbMax+" - minDate="+minDate+" - maxDate="+maxDate;
    }

}
