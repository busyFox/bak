package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by ldelbarre on 19/04/17.
 */
public class SetChatroomImageParam {
    public String chatroomID;
    public String imageID;

    @JsonIgnore
    public boolean isValid() {
        return chatroomID != null && !chatroomID.isEmpty();
    }

    @JsonIgnore
    public String toString(){
        return "chatroomID="+chatroomID+" - mediaID="+ imageID;
    }
}
