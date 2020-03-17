package com.funbridge.server.tournament.federation.cbo.data;

import com.funbridge.server.tournament.federation.data.TourFederationStat;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="cbo_stat")
public class TourCBOStat extends TourFederationStat {
}
