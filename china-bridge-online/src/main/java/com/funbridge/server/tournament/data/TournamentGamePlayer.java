package com.funbridge.server.tournament.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.ws.game.WSGamePlayer;

public class TournamentGamePlayer {
	private boolean isHuman = false;
	private char position;
	private int status = Constantes.PLAYER_STATUS_ABSENT;
	private long playerID = -1;
	private String pseudo = "";
	private boolean avatar = false;
    private String lang = "";
	private String countryCode = "";
	
	public TournamentGamePlayer(boolean isHuman, char position, long playerID, String pseudo, String lang, String countryCode) {
		this.isHuman = isHuman;
		if (!isHuman) {
			// ROBOT ALWAYS PRESENT
			status = Constantes.PLAYER_STATUS_PRESENT;
		}
		this.position = position;
		this.playerID = playerID;
		this.pseudo = pseudo;
        this.lang = lang;
		this.countryCode = countryCode;
	}
	
	public String toString() {
		return "position="+ position +" - human="+isHuman;
	}
	
	public boolean isHuman() {
		return isHuman;
	}
	
	/**
	 * If tablePlayer != null (human) return playerID. Else (robot) -1
	 * @return
	 */
	public long getPlayerID() {
		if (isHuman) {
			return playerID;
		}
		return -1;
	}
	
	/**
	 * If robot always ready. If human, check status = PRESENT
	 * @return
	 */
	public boolean isReady() {
		if (isHuman) {
			return status == Constantes.PLAYER_STATUS_PRESENT;
		}
		return true;
	}
	
	public char getPosition() {
		return position;
	}
	
	/**
	 * Transform to WSGamePlayer
	 * @return
	 */
	public WSGamePlayer toWSGamePlayer() {
		WSGamePlayer wsPlayer = null;
		if (isHuman) {
			wsPlayer = WSGamePlayer.createGamePlayerHuman(playerID,  pseudo, avatar, lang, countryCode);
		} else {
			wsPlayer = WSGamePlayer.createGamePlayerRobot();
		}
		return wsPlayer;
		
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	
}
