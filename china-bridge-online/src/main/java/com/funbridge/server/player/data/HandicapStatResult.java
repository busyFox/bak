package com.funbridge.server.player.data;

public class HandicapStatResult {
    public int rank = 0;
    public double handicap = 0;

    public double getAveragePerformanceMP() {
        double value = 0.5 - handicap ;
        if (value < 0) {
            value = 0;
        }
        if (value > 1) {
            value = 1;
        }
        return value;
    }
}
