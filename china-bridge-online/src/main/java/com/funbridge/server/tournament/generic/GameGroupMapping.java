package com.funbridge.server.tournament.generic;

import com.funbridge.server.common.Constantes;
import com.gotogames.common.bridge.BridgeConstantes;

/**
 * Created by pserent on 13/02/2017.
 */
public class GameGroupMapping {
    public String contract = "";
    public int contractType = -1;
    public char declarer = BridgeConstantes.POSITION_NOT_VALID;
    public int tricks = 0;
    public int score = 0;
    public int rank = -1;
    public double result = 0;
    public int nbPlayer = 0;
    public String lead = "";

    public String getContractWS() {
        String contractWS = "";
        if (!isLeaved()) {
            contractWS = Constantes.contractToString(contract, contractType);
        }
        return contractWS;
    }

    public boolean isLeaved() {
        return (score == Constantes.GAME_SCORE_LEAVE);
    }
}
