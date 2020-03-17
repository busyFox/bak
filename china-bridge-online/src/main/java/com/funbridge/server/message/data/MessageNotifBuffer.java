package com.funbridge.server.message.data;

import com.funbridge.server.common.Constantes;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 12/05/2014.
 * Notif buffer to append many text in the same notif. Send to recipient after a delay or a max of update
 */
@Document(collection="message_notif_buffer")
public class MessageNotifBuffer {
    @Id
    public ObjectId ID;
    @Indexed
    public long recipientID; // recipient of the message
    @Indexed
    public String bufferType = ""; // type of notif buffer (friend duel result ...)
    @Indexed
    public long dateCreation = 0;
    @Indexed
    public int nbUpdate = 0;
    public String msgBodyFR = "";
    public String msgBodyEN = "";
    public String msgBodyNL = "";
    public String fieldName = "";
    public String fieldValue = "";

    public String toString() {
        return "ID="+ID.toString()+" - recipientID="+recipientID+" - bufferType="+bufferType+" - dateCreation="+ Constantes.timestamp2StringDateHour(dateCreation)+" - nbUpdate="+nbUpdate+" - msgBodyFR="+msgBodyFR;
    }
}
