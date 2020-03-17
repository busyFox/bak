package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by bplays on 08/11/16.
 */
public class GetChatroomParam {
    public String chatroomID;
    public boolean includeParticipants = false;

    @JsonIgnore
    public boolean isValid(){
        return chatroomID != null && !chatroomID.isEmpty();
    }

    @JsonIgnore
    public String toString(){
        return "chatroomID="+chatroomID+" - includeParticipants="+includeParticipants;
    }

}
