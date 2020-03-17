package com.funbridge.server.tournament.duel.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 28/05/2015.
 */
@Document(collection="duel_matchmaking_stat")
public class MatchMakingStat {
    @Id
    public ObjectId ID;

    @Indexed
    public long dateRequest = 0;
    @Indexed
    public long dateMatch = 0;
    @Indexed
    public long player1 = 0;
    @Indexed
    public long player2 = 0;

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }
}
