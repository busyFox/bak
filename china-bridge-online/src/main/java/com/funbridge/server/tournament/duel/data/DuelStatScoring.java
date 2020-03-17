package com.funbridge.server.tournament.duel.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DuelStatScoring {
    private Map<Long, Double> bestEver;
    private Map<String, Map<Long, Double>> bestMonthly;
    private Map<String, Map<Long, Double>> bestWeekly;

    public void addBestScore(long playerId, Double score){
        Calendar date = Calendar.getInstance();
        String monthlyKey =  new SimpleDateFormat("yyyyMM").format(date.getTime());

        date.add(Calendar.DATE, - (date.get(Calendar.DAY_OF_WEEK) - date.getFirstDayOfWeek()));
        String weeklyKey =  new SimpleDateFormat("yyyyMMdd").format(date.getTime());

        if(bestEver == null){ bestEver = new HashMap<>(); }
        if(bestMonthly == null){ bestMonthly = new HashMap<>(); }
        if(bestWeekly == null){ bestWeekly = new HashMap<>(); }

        if(bestMonthly.get(monthlyKey) == null){ bestMonthly.put(monthlyKey,new HashMap()); }
        if(bestWeekly.get(weeklyKey) == null){ bestWeekly.put(weeklyKey,new HashMap()); }

        Double lastEverBestScore = bestEver.get(playerId);
        if(lastEverBestScore == null || lastEverBestScore < score){
            bestEver.put(playerId, score);
        }

        Double lastMonthlyBestScore = bestMonthly.get(monthlyKey).get(playerId);
        if(lastMonthlyBestScore == null || lastMonthlyBestScore < score){
            bestMonthly.get(monthlyKey).put(playerId, score);
        }

        Double lastWeeklyBestScore = bestWeekly.get(weeklyKey).get(playerId);
        if(lastWeeklyBestScore == null || lastWeeklyBestScore < score){
            bestWeekly.get(weeklyKey).put(playerId, score);
        }
    }

    public Map<Long, Double> getBestEver() { return bestEver; }
    public Map<String, Map<Long, Double>> getBestWeekly() { return bestWeekly; }
    public Map<String, Map<Long, Double>> getBestMonthly() { return bestMonthly; }
}
