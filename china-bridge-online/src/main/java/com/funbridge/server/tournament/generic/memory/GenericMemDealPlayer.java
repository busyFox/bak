package com.funbridge.server.tournament.generic.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.ws.result.WSResultDeal;

/**
 * Created by pserent on 27/01/2017.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class GenericMemDealPlayer {

    public long playerID;
    public int score = 0;
    public int nbPlayerBetterScore = 0;
    public int nbPlayerSameScore = 1;
    public String contract = "";
    public int contractType = 0;
    public String declarer = "";
    public int nbTricks = 0;
    public double result = 0;
    public String gameID;
    public String begins = null;

    public String getContractWS() {
        return Constantes.contractToString(contract, contractType);
    }

    public WSResultDeal toWSResultDeal(GenericMemDeal deal){
        WSResultDeal resultPlayer = new WSResultDeal();
        resultPlayer.setDealIDstr(deal.dealID);
        resultPlayer.setDealIndex(deal.dealIndex);
        resultPlayer.setResultType(deal.getMemTour().resultType);
        resultPlayer.setPlayed(true);
        resultPlayer.setContract(getContractWS());
        resultPlayer.setDeclarer(declarer);
        resultPlayer.setNbTricks(nbTricks);
        resultPlayer.setScore(score);
        resultPlayer.setRank(nbPlayerBetterScore+1);
        resultPlayer.setResult(result);
        resultPlayer.setNbTotalPlayer(deal.getNbPlayers());
        resultPlayer.setLead(begins);
        return resultPlayer;
    }
}
