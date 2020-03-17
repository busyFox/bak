package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by bplays on 08/11/16.
 */
public class SetMessageReadParam {
    public String messageID;
    public int categoryID=-1;

    @JsonIgnore
    public boolean isValid(){
        return messageID != null && !messageID.isEmpty();
    }

    @JsonIgnore
    public String toSring(){
        return "messageID="+messageID+" - categoryID="+categoryID;
    }
}
