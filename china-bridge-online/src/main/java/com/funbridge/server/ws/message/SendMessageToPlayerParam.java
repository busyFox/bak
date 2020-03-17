package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gotogames.common.tools.StringTools;

/**
 * Created by bplays on 07/11/16.
 */
public class SendMessageToPlayerParam {
    public long playerID;
    public String body;
    public String mediaID;
    public String mediaSize;
    public String tempID;

    @JsonIgnore
    public boolean isValid(){
        if(playerID == 0) return false;
        return (body != null && !body.isEmpty()) || (mediaID != null && !mediaID.isEmpty());
    }

    @JsonIgnore
    public String toString(){
        return "playerID="+playerID+" - body="+ StringTools.truncate(body, 20, "...")+" - mediaID="+ mediaID +" - mediaSize="+ mediaSize;
    }
}
