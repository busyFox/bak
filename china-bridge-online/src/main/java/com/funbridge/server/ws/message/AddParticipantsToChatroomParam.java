package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bplays on 08/11/16.
 */
public class AddParticipantsToChatroomParam {
    public String chatroomID;
    public List<Long> players = new ArrayList<>();

    @JsonIgnore
    public boolean isValid(){
        if(chatroomID == null || chatroomID.isEmpty()) return false;
        return players.size() >= 1;
    }

    @JsonIgnore
    public String toString(){
        return "chatroomID="+chatroomID+" - players.size="+players.size();
    }

}
