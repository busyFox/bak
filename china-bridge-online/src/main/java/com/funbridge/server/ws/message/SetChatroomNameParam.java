package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by ldelbarre on 19/04/17.
 */
public class SetChatroomNameParam {
    public String chatroomID;
    public String name;

    @JsonIgnore
    public boolean isValid() {
        if(chatroomID == null || chatroomID.isEmpty()) return false;
        return name != null && !name.trim().isEmpty();
    }

    @JsonIgnore
    public String toString(){
        return "chatroomID="+chatroomID+" - name="+name;
    }
}
