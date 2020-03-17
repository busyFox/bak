package com.funbridge.server.tournament.training.memory;

import com.funbridge.server.common.Constantes;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * Created by bplays on 21/04/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TrainingMemDealPlayer implements Comparable<TrainingMemDealPlayer> {
    public long playerID;
    public int score = 0;
    public int nbPlayersBetterScore = 0;
    public int nbPlayersSameScore = 1;
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

    @Override
    public int compareTo(TrainingMemDealPlayer o) {
        if (o.score < this.score) {
            return -1;
        } else if (o.score == this.score) {
            //return 0;
            if (o.contract.length() > 0 && this.contract.length() > 0) {
                return o.contract.substring(0, 1).compareTo(this.contract.substring(0, 1));
            }
            return 0;
        }
        return 1;
    }
}
