package com.funbridge.server.ws.tournament;

import java.util.Comparator;

import com.funbridge.server.common.Constantes;

public class WSDuelHistoryComparable implements Comparator<WSDuelHistory>{

	/**
	 * if o1 > o2 return -1, o1 = o2 return 0, o1 < o2 return 1
	 * 1 - D�fi re�u
	 * 2 - D�fi en cours non termin�
	 * 3 - D�fi en cours termin�
	 * 4 - Demande de RAZ (envoy�e)
	 * 5 - Amis connect�s
	 * 6 - Amis non connect�s
	 * 7 - D�fi envoy�
	 * 8 - Mes amis entre eux
	 */
	@Override
	public int compare(WSDuelHistory o1, WSDuelHistory o2) {
		if (o1 != null && o2 != null) {
			// duel request
			if (o1.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2) {
				if (o2.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2) {
					return o1.player2.pseudo.toLowerCase().compareTo(o2.player2.pseudo.toLowerCase());
				} else {
					return -1;
				}
			}
			else if (o2.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2) {
				return 1;
			}
			
			// duel in progress not finish
			else if (o1.status == Constantes.PLAYER_DUEL_STATUS_PLAYING) {
				if (o2.status == Constantes.PLAYER_DUEL_STATUS_PLAYING) {
					return o1.player2.pseudo.toLowerCase().compareTo(o2.player2.pseudo.toLowerCase());
				} else {
					return -1;
				}
			}
			else if (o2.status == Constantes.PLAYER_DUEL_STATUS_PLAYING) {
				return 1;
			}
			
			// duel in progress finish
			else if (o1.status == Constantes.PLAYER_DUEL_STATUS_PLAYED) {
				if (o2.status == Constantes.PLAYER_DUEL_STATUS_PLAYED) {
					return o1.player2.pseudo.toLowerCase().compareTo(o2.player2.pseudo.toLowerCase());
				} else {
					return -1;
				}
			}
			else if (o2.status == Constantes.PLAYER_DUEL_STATUS_PLAYED) {
				return 1;
			}
			
			// Reset in progres
			else if (o1.status == Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER2) {
				if (o2.status == Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER2) {
					return o1.player2.pseudo.toLowerCase().compareTo(o2.player2.pseudo.toLowerCase());
				} else {
					return -1;
				}
			}
			else if (o2.status == Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER2) {
				return 1;
			}
			
			// Friend connected
			else if (o1.player2.connected) {
				if (o2.player2.connected) {
					return o1.player2.pseudo.toLowerCase().compareTo(o2.player2.pseudo.toLowerCase());
				} else {
					return -1;
				}
			}
			else if (o2.player2.connected) {
				return 1;
			}
			
			// Friend not connected
			else if (!o1.player2.connected) {
				if (!o2.player2.connected) {
					return o1.player2.pseudo.toLowerCase().compareTo(o2.player2.pseudo.toLowerCase());
				} else {
					return -1;
				}
			}
		}
		return 0;
	}

}
