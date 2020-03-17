package com.funbridge.server.tournament.privatetournament.data;

import com.funbridge.server.message.data.GenericChatroom;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by bplays on 20/02/17.
 */
@Document(collection="private_chatroom")
public class PrivateTournamentChatroom extends GenericChatroom {
    @Id
    private ObjectId ID;

    @Override
    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

}
