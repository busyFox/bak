package com.funbridge.server.player.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ldelbarre on 29/08/2018.
 */
@Document(collection="player_deals")
public class PlayerDeals {
    @Id
    public long playerID;
    public int nbPlayedDeals = 0;
    public Map<Integer,Integer> nbPlayedDealsCategory = new HashMap<>();

    public PlayerDeals() {
    }

    public PlayerDeals(long playerID) {
        this.playerID = playerID;
    }

    public void update(Map<Integer, Integer> mapCategoryPlay) {
        for (Map.Entry<Integer, Integer> entry : mapCategoryPlay.entrySet()) {
            int category = entry.getKey();
            int nbDeals = entry.getValue();
            if (nbPlayedDealsCategory.containsKey(category)) {
                nbPlayedDealsCategory.put(category, nbPlayedDealsCategory.get(category)+nbDeals);
            } else {
                nbPlayedDealsCategory.put(category, nbDeals);
            }
            nbPlayedDeals+=nbDeals;
        }

    }
}
