package com.funbridge.server.player.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by bplays on 05/03/2015.
 */

@Document(collection="player_handicap")
public class PlayerHandicap {
    @Id
    private ObjectId ID;
    @Indexed
    private long playerId;
    private double handicap;
    private double topHandicap;
    private int nbDeals;
    private double packet0;
    private double packet1;
    private double packet2;
    private double packet3;
    private double packet4;
    private double packet5;
    private long lastUpdate;

    public static double getAveragePerformanceMP(double handicap) {
        double value = 0.5 - handicap ;
        if (value < 0) {
            value = 0;
        }
        if (value > 1) {
            value = 1;
        }
        return value;
    }

    public static double getAveragePerformanceIMP(double handicap) {
        return (50 - (handicap * 100) - 50) / 7;
    }

    public double getAveragePerformanceMP() {
//        return 0.5 - handicap;
        return getAveragePerformanceMP(getHandicap());
    }

    public double getAveragePerformanceIMP() {
        return getAveragePerformanceIMP(getHandicap());
//        return (50 - (handicap * 100) - 50) / 7;
    }

    public double getWSAveragePerformanceMP() {
        return getAveragePerformanceMP() * 100;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public double getHandicap() {
        if(nbDeals < 200) return 0;
        else return handicap;
    }

    public void setHandicap(double handicap) {
        this.handicap = handicap;
    }

    public double getTopHandicap() {
        return topHandicap;
    }

    public void setTopHandicap(double topHandicap) {
        this.topHandicap = topHandicap;
    }

    public int getNbDeals() {
        return nbDeals;
    }

    public void setNbDeals(int nbDeals) {
        this.nbDeals = nbDeals;
    }

    public double getPacket0() {
        return packet0;
    }

    public void setPacket0(double packet0) {
        this.packet0 = packet0;
    }

    public double getPacket1() {
        return packet1;
    }

    public void setPacket1(double packet1) {
        this.packet1 = packet1;
    }

    public double getPacket2() {
        return packet2;
    }

    public void setPacket2(double packet2) {
        this.packet2 = packet2;
    }

    public double getPacket3() {
        return packet3;
    }

    public void setPacket3(double packet3) {
        this.packet3 = packet3;
    }

    public double getPacket4() {
        return packet4;
    }

    public void setPacket4(double packet4) {
        this.packet4 = packet4;
    }

    public double getPacket5() {
        return packet5;
    }

    public void setPacket5(double packet5) {
        this.packet5 = packet5;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
