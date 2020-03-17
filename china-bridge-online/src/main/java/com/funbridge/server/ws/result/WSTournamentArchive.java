package com.funbridge.server.ws.result;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.common.Constantes;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pserent on 28/04/2016.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSTournamentArchive {
    public int categoryID = Constantes.TOURNAMENT_CATEGORY_SERIE_TOP_CHALLENGE;
    public long date;
    public int rank;
    public double result;
    public int resultType;
    public int nbPlayers;
    public String tournamentID;
    public boolean finished;
    public List<String> listPlayedDeals = new ArrayList<>();
    public int countDeal;
    public String periodID;
    public String name;
    public double masterPoints = -1;
    public String teamPeriodID;
    public int teamPoints = -1;
    public int tourID;
}
