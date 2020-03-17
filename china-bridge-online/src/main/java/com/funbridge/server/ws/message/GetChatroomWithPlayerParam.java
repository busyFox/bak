package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by bplays on 08/11/16.
 */
public class GetChatroomWithPlayerParam {
    public long playerID;

    @JsonIgnore
    public boolean isValid(){
        return playerID != 0;
    }

    @JsonIgnore
    public String toString(){
        return "playerID="+playerID;
    }

}
