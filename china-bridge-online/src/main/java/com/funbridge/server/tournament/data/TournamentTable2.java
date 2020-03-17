package com.funbridge.server.tournament.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.ws.game.WSGamePlayer;
import com.funbridge.server.ws.game.WSTableGame;
import com.gotogames.common.bridge.BridgeConstantes;

@Entity
@Table(name="tournament_table2")
@NamedQueries({
	@NamedQuery(name="tournamentTable2.selectForTournamentAndPlayers", query="select t from TournamentTable2 t where t.tournament.ID=:tourID and ((playerSouth.ID=:plaID1 and playerNorth.ID=:plaID2) or (playerSouth.ID=:plaID2 and playerNorth.ID=:plaID1))"),
	@NamedQuery(name="tournamentTable2.selectForTournamentAndPlayer", query="select t from TournamentTable2 t where t.tournament.ID=:tourID and (playerSouth.ID=:plaID or playerNorth.ID=:plaID)"),
	@NamedQuery(name="tournamentTable2.listForTournamentNotFinished", query="select t from TournamentTable2 t where t.tournament.finished=0"),
	@NamedQuery(name="tournamentTable2.listForPlayer", query="select t from TournamentTable2 t where t.playerSouth.ID=:plaID or t.playerNorth.ID=:plaID"),
	@NamedQuery(name="tournamentTable2.deleteForPlayer", query="delete from TournamentTable2 t where t.playerSouth.ID=:plaID or t.playerNorth.ID=:plaID")
})
public class TournamentTable2 {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@ManyToOne
	@JoinColumn(name="tour_id")
	private Tournament tournament;
	
	@Column(name="creator_id")
	private long creatorID;
	
	@ManyToOne
	@JoinColumn(name="pla_south")
	private Player playerSouth;
	
	@ManyToOne
	@JoinColumn(name="pla_north")
	private Player playerNorth = null;
	
	@ManyToOne
	@JoinColumn(name="pla_west")
	private Player playerWest = null;
	
	@ManyToOne
	@JoinColumn(name="pla_east")
	private Player playerEast = null;
	
	@Column(name="nb_deal_played")
	private int nbDealPlayed = 0;

	@ManyToOne
	@JoinColumn(name="current_game")
	private TournamentGame2 currentGame = null;
	
	@Transient
	private int playerSouthStatus = Constantes.TABLE_PLAYER_STATUS_NOT_PRESENT;
	@Transient
	private boolean playerSouthPlay = false;
	@Transient
	private int playerNorthStatus = Constantes.TABLE_PLAYER_STATUS_NOT_PRESENT;
	@Transient
	private boolean playerNorthPlay = false;
	@Transient
	private int playerEastStatus = Constantes.TABLE_PLAYER_STATUS_PRESENT;
	@Transient
	private boolean playerEastPlay = true;
	@Transient
	private int playerWestStatus = Constantes.TABLE_PLAYER_STATUS_PRESENT;
	@Transient
	private boolean playerWestPlay = true;
	
	@Transient
	private long challengeID = -1; 
	
	// date of tournament finished - used in purgeMap
	@Transient
	private long dateFinishTournement = 0;
	
	public String toString() {
		return "{ID="+ID+" - tourID="+tournament.getID()+" - creatorID="+creatorID+" -game="+(currentGame!=null?currentGame.getID():"null")+" - challengeID="+challengeID+" - dateFinishTournement="+Constantes.timestamp2StringDateHour(dateFinishTournement)+"}";
	}
	
	public long getID() {
		return ID;
	}

	public void setID(long iD) {
		ID = iD;
	}

	public Tournament getTournament() {
		return tournament;
	}

	public void setTournament(Tournament tournament) {
		this.tournament = tournament;
	}

	public long getCreatorID() {
		return creatorID;
	}

	public void setCreatorID(long creatorID) {
		this.creatorID = creatorID;
	}

	public Player getPlayerSouth() {
		return playerSouth;
	}

	public void setPlayerSouth(Player playerSouth) {
		this.playerSouth = playerSouth;
	}

	public Player getPlayerNorth() {
		return playerNorth;
	}

	public void setPlayerNorth(Player playerNorth) {
		this.playerNorth = playerNorth;
	}

	public Player getPlayerWest() {
		return playerWest;
	}

	public void setPlayerWest(Player playerWest) {
		this.playerWest = playerWest;
	}

	public Player getPlayerEast() {
		return playerEast;
	}

	public void setPlayerEast(Player playerEast) {
		this.playerEast = playerEast;
	}

	public int getNbDealPlayed() {
		return nbDealPlayed;
	}

	public void setNbDealPlayed(int nbDealPlayed) {
		this.nbDealPlayed = nbDealPlayed;
	}
	
	public boolean isValid() {
		return playerNorth != null && playerSouth != null;
	}
	
	public void setPlayerStatus(long plaID, int status) {
		if (plaID == playerSouth.getID()) {
			playerSouthStatus = status;
		}
		else if (plaID == playerNorth.getID()) {
			playerNorthStatus = status;
		}
	}
	
	public boolean isAllPlayerPresent() {
		return playerSouthStatus == Constantes.TABLE_PLAYER_STATUS_PRESENT &&
				playerNorthStatus == Constantes.TABLE_PLAYER_STATUS_PRESENT &&
				playerEastStatus == Constantes.TABLE_PLAYER_STATUS_PRESENT &&
				playerWestStatus == Constantes.TABLE_PLAYER_STATUS_PRESENT;
	}
	
	public int getPlayerStatus(long plaID) {
		if (plaID == playerSouth.getID()) {
			return playerSouthStatus;
		}
		if (plaID == playerNorth.getID()) {
			return playerNorthStatus;
		}
		return Constantes.TABLE_PLAYER_STATUS_NOT_PRESENT;
	}
	
	public void setPlayerPlay(long plaID, boolean val) {
		if (plaID == playerSouth.getID()) {
			playerSouthPlay = val;
		}
		else if (plaID == playerNorth.getID()) {
			playerNorthPlay = val;
		}
	}
	
	public void resetPlayerPlay() {
		playerSouthPlay = false;
		playerNorthPlay = false;
		
	}
	
	public boolean isAllPlayerPlay() {
		return playerSouthPlay && playerNorthPlay && playerEastPlay && playerWestPlay;
	}

	public TournamentGame2 getCurrentGame() {
		return currentGame;
	}

	public void setCurrentGame(TournamentGame2 currentGame) {
		this.currentGame = currentGame;
	}
	
	public Player getPartner(long playerID) {
		if (playerSouth != null && playerSouth.getID() == playerID) {
			return playerNorth;
		}
		if (playerNorth != null && playerNorth.getID() == playerID) {
			return playerSouth;
		}
		return null;
	}
	
	public boolean isPlayerTable(long playerID) {
		if (playerNorth != null && playerNorth.getID() == playerID) {
			return true;
		}
		return playerSouth != null && playerSouth.getID() == playerID;
	}

	public long getChallengeID() {
		return challengeID;
	}

	public void setChallengeID(long challengeID) {
		this.challengeID = challengeID;
	}
	
	public char getCurrentPosition() {
		if (currentGame != null) {
			return currentGame.getCurrentPlayer();
		}
		return BridgeConstantes.POSITION_NOT_VALID;
	}
	
	public boolean isCurrentPlayerHuman() {
		char cp = getCurrentPosition();
		return cp == BridgeConstantes.POSITION_NORTH || cp == BridgeConstantes.POSITION_SOUTH;
	}
	
	public char getPlayerPosition(long playerID) {
		if (playerSouth.getID() == playerID) {
			return BridgeConstantes.POSITION_SOUTH;
		}
		if (playerNorth.getID() == playerID) {
			return BridgeConstantes.POSITION_NORTH;
		}
		return BridgeConstantes.POSITION_NOT_VALID;
	}

	public Player getCreator() {
		if (playerSouth.getID() == creatorID) {
			return playerSouth;
		}
		if (playerNorth.getID() == creatorID) {
			return playerNorth;
		}
		return null;
	}

	public long getDateFinishTournement() {
		return dateFinishTournement;
	}

	public void setDateFinishTournement(long dateFinishTournement) {
		this.dateFinishTournement = dateFinishTournement;
	}
	
	public WSTableGame toWSTableGame(long playerAsk) {
		WSTableGame wst = new WSTableGame();
		wst.tableID = ID;
		wst.playerEast = WSGamePlayer.createGamePlayerRobot();
		wst.playerWest = WSGamePlayer.createGamePlayerRobot();
		wst.playerNorth = WSGamePlayer.createGamePlayerHuman(playerNorth, getPlayerStatus(playerNorth.getID()), playerAsk);
		wst.playerSouth = WSGamePlayer.createGamePlayerHuman(playerSouth, getPlayerStatus(playerSouth.getID()), playerAsk);
		wst.leaderID = creatorID;
		return wst;
	}
}
