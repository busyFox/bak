package com.funbridge.server.message.data;

import com.funbridge.server.common.Constantes;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 30/01/2015.
 */
@Document(collection="message_player")
public class MessagePlayer2 {
    @Id
    public ObjectId ID;
    @Indexed
    public long senderID;
    @Indexed
    public long recipientID;
    @Indexed
    public long dateMsg;
    @Indexed
    public long dateExpiration = 0;
    public String text;
    public long dateRead = 0;

    public static String MSG_TYPE = "message";

    public boolean resetSender = false;
    public boolean resetRecipient = false;

    public String toString() {
        return "ID="+getIDStr()+" - senderID="+senderID+" - recipientID="+recipientID+" - dateMsg="+ Constantes.timestamp2StringDateHour(dateMsg)+" - dateRead="+Constantes.timestamp2StringDateHour(dateRead)+" - resetSender="+resetSender+" - resetRecipient="+resetRecipient;
    }

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }
}
