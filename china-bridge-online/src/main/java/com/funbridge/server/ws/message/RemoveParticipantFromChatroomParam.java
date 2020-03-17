package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by bplays on 08/11/16.
 */
public class RemoveParticipantFromChatroomParam {
    public String chatroomID;
    public long playerID;

    @JsonIgnore
    public boolean isValid(){
        if(chatroomID == null || chatroomID.isEmpty()) return false;
        return playerID != 0;
    }

    @JsonIgnore
    public String toString(){
        return "chatroomID="+chatroomID+" - playerID="+playerID;
    }

}
