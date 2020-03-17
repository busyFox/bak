package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by bplays on 07/11/16.
 */
public class DeleteMessageParam {
    public String messageID;
    public int categoryID;

    @JsonIgnore
    public boolean isValid(){
        return messageID != null && !messageID.isEmpty();
    }

    @JsonIgnore
    public String toString(){
        return "messageID="+messageID;
    }
}
