package com.funbridge.server.tournament.duel.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by pserent on 05/07/2017.
 */
@Document(collection="duel_argine_progress")
public class DuelArgineInProgress {
    @Id
    public ObjectId ID;
    public String tourID;
    public long dateCreation;
    public String duelTourPlayerID = null;
    public long playerID = 0;
    public int currentDealIndexArgine = 0;
    public String currentGameID = null;
    public long dateLastUpdate;
    public Set<String> listDealsPlayedByArgine = new HashSet<>();
    public boolean argineFinish = false;
    public long tournamentExpirationDate = 0;

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    public int countDealsPlayedByArgine() {
        return listDealsPlayedByArgine.size();
    }

    public boolean isFree() {
        return (playerID == 0) && argineFinish;
    }
}
