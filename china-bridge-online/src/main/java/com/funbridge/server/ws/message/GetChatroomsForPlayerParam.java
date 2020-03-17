package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by bplays on 08/11/16.
 */
public class GetChatroomsForPlayerParam {
    public int offset;
    public int number;
    public String search;

    @JsonIgnore
    public boolean isValid(){
        return true;
    }

    @JsonIgnore
    public String toString(){
        return "offset="+offset+" - number="+number+" - search="+search;
    }

}
