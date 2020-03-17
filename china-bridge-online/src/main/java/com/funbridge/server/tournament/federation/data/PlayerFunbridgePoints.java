package com.funbridge.server.tournament.federation.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by bplays on 30/03/2017.
 */
@Document(collection="player_funbridge_points")
public class PlayerFunbridgePoints {
    @Id
    public ObjectId ID;
    public long playerID;
    public long pointsEarned = 0;
    public int tournamentsPlayed = 0;

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public long getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(long pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public int getTournamentsPlayed() {
        return tournamentsPlayed;
    }

    public void setTournamentsPlayed(int tournamentsPlayed) {
        this.tournamentsPlayed = tournamentsPlayed;
    }
}
