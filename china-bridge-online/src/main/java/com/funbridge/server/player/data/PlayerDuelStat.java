package com.funbridge.server.player.data;

import com.funbridge.server.common.Constantes;

public class PlayerDuelStat {
	public int nbPlayed = 0;
	public int nbWin = 0;
	public int nbLost = 0;
	public long dateLastDuel = 0;
	
	public String toString() {
		return "nbPlayed="+nbPlayed+" - nbWin="+nbWin+" - nbLost="+nbLost+" - dateLastDuel="+ Constantes.timestamp2StringDateHour(dateLastDuel);
	}
}
