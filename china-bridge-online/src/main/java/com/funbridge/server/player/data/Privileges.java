package com.funbridge.server.player.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 21/10/2015.
 */
@Document(collection = "player_privileges")
public class Privileges {
    @Id
    public ObjectId ID;
    @Indexed
    public long playerID;
    public boolean serverChange = false;
    public boolean stats = false;
    public boolean sendStats = false;
    public int log = 0;
    public boolean lin = false;
    public boolean commentedTournaments = false;
    public boolean rpc = false;
    public boolean adminLogin = false;

    public String toString() {
        return "playerID="+playerID+" - serverChange="+serverChange+" - stats="+stats+" - log="+log+" - lin="+lin+" - commentedTournaments="+commentedTournaments+" - rpc="+rpc+" - adminLogin="+adminLogin;
    }
}
