package com.funbridge.server.tournament.federation.data;

import org.springframework.data.annotation.Id;

/**
 * Created by luke on 29/12/2017.
 */
public class TourFederationPlayerPoints {
    @Id
    public long playerID;
    public int funbridgePoints = 0;
    public double points = 0;
    public int nbTournaments = 0;
}
