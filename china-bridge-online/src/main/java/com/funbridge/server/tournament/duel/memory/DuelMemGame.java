package com.funbridge.server.tournament.duel.memory;

import com.funbridge.server.common.Constantes;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by pserent on 09/07/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class DuelMemGame {
    public long playerID;
    public int score = 0;
    public String contract = "";
    public int contractType = 0;
    public String declarer = "";
    public int nbTricks = 0;
    public double result = 0;
    public String gameID;
    public int nbPlayersBetterScore = 0;
    public String begins = null;

    public String getContractWS() {
        return Constantes.contractToString(contract, contractType);
    }

    public String toString() {
        return "playerID="+playerID+" - score="+score+" - contract="+getContractWS()+" - result="+result;
    }

    public int getRank() {
        return nbPlayersBetterScore+1;
    }

    public boolean isLeaved() {
        return (score == Constantes.GAME_SCORE_LEAVE);
    }
}
