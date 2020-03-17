package com.funbridge.server.ws.support;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.message.data.GenericMessage;

/**
 * Created by pserent on 10/03/2017.
 */
public class WSSupChatroomMessage {
    public String messageID;
    public String chatroomID;
    public long authorID;
    public String body;
    public String date;
    public String type;

    public WSSupChatroomMessage() {}

    public WSSupChatroomMessage(GenericMessage msg) {
        this.messageID = msg.getIDStr();
        this.chatroomID = msg.getChatroomID();
        this.authorID = msg.getAuthorID();
        this.body = msg.getBody();
        this.date = Constantes.timestamp2StringDateHour(msg.getDate());
        this.type = msg.getType();
    }
}
