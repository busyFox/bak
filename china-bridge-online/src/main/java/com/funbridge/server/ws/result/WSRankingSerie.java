package com.funbridge.server.ws.result;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Include ranking of a serie
 *
 * @author pascal
 */
@XmlRootElement(name = "rankingSerie")
public class WSRankingSerie {
    public WSRankingSeriePlayer rankingPlayer;
    public List<WSRankingSeriePlayer> listRankingPlayer;
    public int offset;
    public int totalSize;
    public int nbPlayerSerie;

}
