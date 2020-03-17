package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by bplays on 07/11/16.
 */
public class ModerateMessageParam {
    public String tournamentID;
    public int categoryID;
    public String messageID;
    public boolean moderated=true;

    @JsonIgnore
    public boolean isValid(){
        if(tournamentID == null || tournamentID.isEmpty()) return false;
        return messageID != null && !messageID.isEmpty();
    }

    @JsonIgnore
    public String toString(){
        return "messageID="+messageID+" - tournamentID="+tournamentID+" - categoryID="+categoryID+" - moderated="+moderated;
    }
}
