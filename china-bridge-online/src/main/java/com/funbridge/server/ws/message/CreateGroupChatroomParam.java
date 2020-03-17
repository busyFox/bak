package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bplays on 08/11/16.
 */
public class CreateGroupChatroomParam {
    public String name;
    public String imageID;
    public List<Long> players = new ArrayList<>();

    @JsonIgnore
    public boolean isValid(){
        if(name == null || name.isEmpty()) return false;
        return players.size() >= 1;
    }

    @JsonIgnore
    public String toString(){
        return "name="+name+" - mediaID="+ imageID +" - players.size="+players.size();
    }

}
