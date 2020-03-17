package com.funbridge.server.tournament.game;

/**
 * Created by pserent on 22/07/2016.
 */
public class ComputeScoreAverageResult {
    public int scoreAverage;
    public int nbScore;
    public int computeAverageWithAddScore(int scoreToAdd) {
        int total = (scoreAverage * nbScore)+scoreToAdd;
        return total/(nbScore+1);
    }
}
