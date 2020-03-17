package com.funbridge.server.tournament.federation.cbo.data;

import com.funbridge.server.tournament.federation.data.TourFederationTournamentPlayer;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by ldelbarre on 21/12/2017.
 */
@Document(collection="cbo_tournament_player")
public class TourCBOTournamentPlayer extends TourFederationTournamentPlayer{
}
