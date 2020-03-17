package com.funbridge.server.tournament.duel.data;

import com.funbridge.server.common.Constantes;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 04/07/2017.
 */
@Document(collection="duel_argine_stat")
public class DuelArgineStat {
    @Id
    public long playerID;
    public long dateFirstDuel;
    public long dateLastDuel;
    public int nbPlayed;
    public int nbWon;
    public int nbLost;
    public int nbSuccessiveWin;
    public String historicResult; // "LWWLSSLLWLWWLLLLSLLL" L => lost, W => win, S => same
    public int bestNbSuccessiveWin;

    public String toString() {
        return "playerID="+playerID+" - dateFirstDuel="+ Constantes.timestamp2StringDateHour(dateFirstDuel)+" - dateLastDuel="+Constantes.timestamp2StringDateHour(dateLastDuel)+" - nbPlayed="+nbPlayed+" - nbWon="+nbWon+" - nbLost="+nbLost+" - nbSuccessiveWin="+nbSuccessiveWin+" - bestNbSuccessiveWin="+bestNbSuccessiveWin;
    }
    /**
     * if player win => result = 1, argine win => result = -1, nobody win => 0
     * @param result
     */
    public void addResult(int result) {
        nbPlayed++;
        if (historicResult == null) {
            historicResult = "";
        }
        if (result == 1) {
            historicResult += "W";
            nbSuccessiveWin++;
            nbWon++;
            if (nbSuccessiveWin > bestNbSuccessiveWin) {
                bestNbSuccessiveWin = nbSuccessiveWin;
            }
        }
        else if (result == -1) {
            historicResult += "L";
            nbLost++;
            nbSuccessiveWin = 0;
        }
        else if (result == 0) {
            historicResult += "S";
            nbSuccessiveWin = 0;
        }
    }
}
