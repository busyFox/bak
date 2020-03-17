package com.funbridge.server.ws.tournament;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.funbridge.server.tournament.data.TournamentSettings;
import com.funbridge.server.ws.result.WSResultTournamentPlayer;

@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.PUBLIC_ONLY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSTournament {
	public long ID = 0;
	public long tourID = 0;
    public String tourIDstr = "";
	public String name;
	public long beginDate = 0;
	public long endDate = 0;
	public long categoryID = 0;
	public int countDeal = 0;
	public int resultType = 0;
	public long number = 0;
	public long remainingTime = 0;
	public long[] arrayDealID = null;
    public String[] arrayDealIDstr = null;
	public WSResultTournamentPlayer resultPlayer = null;
    public int nbTotalPlayer = -1;
    public int currentDealIndex = -1;
	public TournamentSettings settings = null;
    public int engineVersion = 0;
    public int playerOffset = -1;
    public String periodID;

    public void setArrayDeal(long[] arrayDeal) {
        this.arrayDealID = arrayDeal;
        this.arrayDealIDstr = new String[arrayDeal.length];
        for (int i=0; i < arrayDeal.length; i++) {
            arrayDealIDstr[i] = ""+arrayDeal[i];
        }
    }

    public void setTourID(long tourID) {
        this.ID = tourID;
        this.tourID = tourID;
        this.tourIDstr = ""+tourID;
    }
}
