package com.funbridge.server.message.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by bplays on 04/11/16.
 */
@Document(collection="message")
public class Message extends GenericMessage{
    @Id
    private ObjectId ID;

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }
}
