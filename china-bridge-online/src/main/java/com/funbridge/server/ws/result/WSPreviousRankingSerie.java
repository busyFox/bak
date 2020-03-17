package com.funbridge.server.ws.result;

import java.util.List;

/**
 * Created by pserent on 04/08/2014.
 */
public class WSPreviousRankingSerie {
    public WSRankingSeriePlayer rankingPlayer;
    public List<WSRankingSeriePlayer> listRankingPlayer;
    public int offset;
    public int totalSize;
    public int nbPlayerSerie;
    public long periodStart;
    public long periodEnd;
    public String serie;
}
