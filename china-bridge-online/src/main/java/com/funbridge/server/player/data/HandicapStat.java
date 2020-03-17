package com.funbridge.server.player.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Document(collection="handicap_stat")
public class HandicapStat {
    @Id
    public long playerID;
    public String countryCode;

    /* Map for period -> result */
    public Map<String, HandicapStatResult> resultPeriod = new HashMap<>();

    public HandicapStat(long playerID) {
        this.playerID = playerID;
    }
}
