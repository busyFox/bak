package com.funbridge.server.tournament.serie.memory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.tournament.generic.memory.GenericMemDealPlayer;

/**
 * Created by pserent on 09/06/2014.
 * Bean to store in memory the result of player on a deal
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class TourSerieMemDealPlayer extends GenericMemDealPlayer implements Comparable<TourSerieMemDealPlayer>{

    public int nbPlayerBestScore = 0;

    @Override
    public String getContractWS() {
        return Constantes.contractToString(contract, contractType);
    }

    @Override
    public int compareTo(TourSerieMemDealPlayer o) {
        if (o.score < this.score) {
            return -1;
        } else if (o.score == this.score) {
            if (o.contract.length() > 0 && this.contract.length() > 0) {
                return o.contract.substring(0, 1).compareTo(this.contract.substring(0, 1));
            }
            return 0;
        }
        return 1;
    }
}
