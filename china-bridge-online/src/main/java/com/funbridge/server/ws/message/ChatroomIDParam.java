package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by bplays on 07/11/16.
 */
public class ChatroomIDParam {
    public String chatroomID;

    @JsonIgnore
    public boolean isValid(){
        return chatroomID != null && !chatroomID.isEmpty();
    }

    @JsonIgnore
    public String toString(){
        return "chatroomID="+chatroomID;
    }
}
