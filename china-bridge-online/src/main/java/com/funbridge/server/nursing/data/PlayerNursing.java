package com.funbridge.server.nursing.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by ldelbarre on 29/08/2018.
 */
@Document(collection="player_nursing")
public class PlayerNursing {
    @Id
    public ObjectId id;
    @Indexed
    public long playerID;
    @Indexed
    public String nursingName;
    @Indexed
    public long date;
    public boolean read = false;
    public String notifID;

    public PlayerNursing() {
    }

    public PlayerNursing(long playerID, String nursingName, long date, String notifID) {
        this.playerID = playerID;
        this.nursingName = nursingName;
        this.date = date;
        this.notifID = notifID;
        if (notifID == null) {
            read = true;
        }
    }
}
