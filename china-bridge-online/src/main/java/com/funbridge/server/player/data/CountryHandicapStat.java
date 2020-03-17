package com.funbridge.server.player.data;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

@Document(collection="country_handicap_stat")
public class CountryHandicapStat {
    @Id
    public String countryCode;

    /* Map for period -> result */
    public Map<String, CountryHandicapStatResult> resultPeriod = new HashMap<>();

    public CountryHandicapStat(String countryCode) {
        this.countryCode = countryCode;
    }
}
