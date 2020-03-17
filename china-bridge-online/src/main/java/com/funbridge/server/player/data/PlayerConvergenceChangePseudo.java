package com.funbridge.server.player.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 14/11/2014.
 */
@Document(collection = "player_convergence_change_pseudo")
public class PlayerConvergenceChangePseudo {
    @Id
    public ObjectId ID;
    @Indexed
    public long playerID;
    @Indexed
    public String mail;
    @Indexed
    public String nickname;
    public String password;

    public String toString() {
        return "playerID="+playerID+" - mail="+mail+" - nickname="+nickname+" - password="+password;
    }
}
