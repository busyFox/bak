package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gotogames.common.tools.StringTools;

/**
 * Created by bplays on 07/11/16.
 */
public class SendMessageToChatroomParam {
    public String chatroomID;
    public int categoryID=-1;
    public String body;
    public String mediaID;
    public String mediaSize;
    public String lang;
    public String quotedMessageID;
    public String tempID;

    @JsonIgnore
    public boolean isValid(){
        if(chatroomID == null || chatroomID.isEmpty()) return false;
        return (body != null && !body.isEmpty()) || (mediaID != null && !mediaID.isEmpty());
    }

    @JsonIgnore
    public String toString(){
        return "chatroomID="+chatroomID+" - categoryID="+categoryID+" - body="+ StringTools.truncate(body, 20, "...")+" - mediaID="+ mediaID +" - mediaSize="+ mediaSize +" - lang="+ lang+" - quotedMessageID="+quotedMessageID;
    }
}
