package com.funbridge.server.player.data;

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

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;

@Entity
@Table(name="player_duel")
@NamedQueries({
	@NamedQuery(name="playerDuel.selectBetweenPlayer", query="select pd from PlayerDuel pd where (pd.player1.ID=:plaID1 and pd.player2.ID=:plaID2) or (pd.player1.ID=:plaID2 and pd.player2.ID=:plaID1)"),
	@NamedQuery(name="playerDuel.deleteBetweenPlayer", query="delete from PlayerDuel pd where (pd.player1.ID=:plaID1 and pd.player2.ID=:plaID2) or (pd.player1.ID=:plaID2 and pd.player2.ID=:plaID1)"),
	@NamedQuery(name="playerDuel.listForPlayer", query="select pd from PlayerDuel pd where pd.player1.ID=:plaID or pd.player2.ID=:plaID"),
    @NamedQuery(name="playerDuel.countDuelsForPlayerWithNbPlayedSup0", query="select count(pd) from PlayerDuel pd where (pd.player1.ID=:plaID or pd.player2.ID=:plaID) and nbPlayed > 0"),
    @NamedQuery(name="playerDuel.countDuelsNoArgineForPlayerWithNbPlayedSup0", query="select count(pd) from PlayerDuel pd where pd.player1.ID != -2 and pd.player2.ID != -2 and (pd.player1.ID=:plaID or pd.player2.ID=:plaID) and nbPlayed > 0"),
	@NamedQuery(name="playerDuel.listWithStatusForPlayer", query="select pd from PlayerDuel pd where (pd.player1.ID=:playerID or pd.player2.ID=:playerID) and pd.status=:status"),
	@NamedQuery(name="playerDuel.listRequestWaitingForPlayer", query="select pd from PlayerDuel pd where (pd.player1.ID=:playerID and pd.status=:statusRequestPla2) or (pd.player2.ID=:playerID and pd.status=:statusRequestPla1)"),
	@NamedQuery(name="playerDuel.listResetWaitingForPlayer", query="select pd from PlayerDuel pd where (pd.player1.ID=:playerID and pd.resetRequest=:resetPla2) or (pd.player2.ID=:playerID and pd.resetRequest=:resetPla1)"),
	@NamedQuery(name="playerDuel.listRequestDateExpired", query="select pd from PlayerDuel pd where (pd.status=:requestPla1 or pd.status=:requestPla2) and pd.dateCreation < :dateCreationLimit"),
	@NamedQuery(name="playerDuel.updateRequestDateExpired", query="update PlayerDuel pd set pd.status=:statusNone where (pd.status=:statusRequestPla1 or pd.status=:statusRequestPla2) and pd.dateCreation < :dateCreationLimit"),
	@NamedQuery(name="playerDuel.countRequestDateExpired", query="select count(pd) from PlayerDuel pd where (pd.status=:statusRequestPla1 or pd.status=:statusRequestPla2) and pd.dateCreation < :dateCreationLimit"),
	@NamedQuery(name="playerDuel.deleteDuel", query="delete from PlayerDuel pd where pd.ID=:duelID")
})
public class PlayerDuel {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@ManyToOne
	@JoinColumn(name="player1", nullable=false)
	private Player player1;
	
	@ManyToOne
	@JoinColumn(name="player2", nullable=false)
	private Player player2;
	
	@Column(name="date_creation")
	private long dateCreation;
	
	@Column(name="duel_status")
	private int status = Constantes.PLAYER_DUEL_STATUS_NONE;
	
	@Column(name="reset_request")
	private int resetRequest = Constantes.PLAYER_DUEL_RESET_REQUEST_NONE; // used to remove the duel from the view
	
	@Column(name="nb_played")
	private int nbPlayed = 0;

	@Column(name="nb_win_pla1")
	private int nbWinPlayer1 = 0;

	@Column(name="nb_win_pla2")
	private int nbWinPlayer2 = 0;

	@Column(name="date_reset")
	private long dateReset = 0; // not more used ....

    @Column(name="date_last_duel")
    private long dateLastDuel = 0;
	
	public String toString() {
		return "ID="+ID+" - player1={"+player1+"} - player2={"+player2+"} - status="+status+" - resetRequest="+resetRequest;
	}
	
	public long getID() {
		return ID;
	}

	public void setID(long iD) {
		ID = iD;
	}

	public Player getPlayer1() {
		return player1;
	}

	public void setPlayer1(Player player1) {
		this.player1 = player1;
	}

	public Player getPlayer2() {
		return player2;
	}

	public void setPlayer2(Player player2) {
		this.player2 = player2;
	}

	public long getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(long dateCreation) {
		this.dateCreation = dateCreation;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getResetRequest() {
		return resetRequest;
	}

	public void setResetRequest(int resetRequest) {
		this.resetRequest = resetRequest;
	}

	public int getNbPlayed() {
		return nbPlayed;
	}

	public void setNbPlayed(int value) {
		this.nbPlayed = value;
	}
	
	public int getNbWinPlayer1() {
		return nbWinPlayer1;
	}

	public void setNbWinPlayer1(int nbWinPlayer1) {
		this.nbWinPlayer1 = nbWinPlayer1;
	}

	public int getNbWinPlayer2() {
		return nbWinPlayer2;
	}

	public void setNbWinPlayer2(int nbWinPlayer2) {
		this.nbWinPlayer2 = nbWinPlayer2;
	}

    /**
	 * change status value for request duel from playerID
	 * @param playerID
	 */
	public void requestFromPlayer(long playerID) {
		if (player1 != null && player1.getID() == playerID) {
			this.status = Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1;
		}
		else if (player2 != null && player2.getID() == playerID) {
			this.status = Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2;
		}
        // set resetRequest value to 0
        setResetRequest(0);
	}
	
	/**
	 * Return playerID who made request duel
	 * @return
	 */
	public long getPlayerRequestDuel() {
		if (this.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1) {
			if (player1 != null) {
				return player1.getID();
			}
		}
		else if (this.status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2) {
			if (player2 != null) {
				return player2.getID();
			}
		}
		return 0;
	}
	
	/**
	 * change reset request value for request duel from playerID
	 * @param playerID
	 */
	public void resetFromPlayer(long playerID) {
		if (player1 != null && player1.getID() == playerID) {
		    this.resetRequest = this.resetRequest | Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER1;
//			this.resetRequest = Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER1;
		}
		else if (player2 != null && player2.getID() == playerID) {
            this.resetRequest = this.resetRequest | Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER2;
//			this.resetRequest = Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER2;
		}
	}

	public boolean isPlayerRequestResetForPlayer(long playerID) {
        if ((this.resetRequest & Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER1) == Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER1) {
            if (isPlayer1(playerID)) {
                return true;
            }
        }
        if ((this.resetRequest & Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER2) == Constantes.PLAYER_DUEL_RESET_REQUEST_PLAYER2) {
            return isPlayer2(playerID);
        }
        return false;
    }
	
	/**
	 * Return the player1 for player ask
	 * @param playerAsk
	 * @return
	 */
	public Player getPlayer1ForAsk(long playerAsk) {
		if (player1 != null && player1.getID() == playerAsk) {
			return player1;
		}
		return player2;
	}
	
	/**
	 * Return the player2 for player ask
	 * @param playerAsk
	 * @return
	 */
	public Player getPlayer2ForAsk(long playerAsk) {
		if (player2 != null && player2.getID() == playerAsk) {
			return player1;
		}
		return player2;
	}
	
	/**
	 * Return the value of duel satus for player
	 * @param playerAsk
	 * @return
	 */
	public int getStatusForAsk(long playerAsk) {
		long playerRequest = getPlayerRequestDuel();
		if (playerRequest != 0) {
			if (playerRequest == playerAsk) {
				return Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1;
			}
			return Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2;
		}
		return status;
	}
	
	/**
	 * Check playerID is player1 or player2
	 * @param playerID
	 * @return true if playerID=player1 or player2, else false
	 */
	public boolean isPlayerDuel(long playerID) {
		if (player1 != null && player1.getID() == playerID) {
			return true;
		}
        return player2 != null && player2.getID() == playerID;
    }

	public boolean isDuelArgine() {
	    return isPlayerDuel(Constantes.PLAYER_ARGINE_ID);
    }
	
	/**
	 * Return the number of duel draw
	 * @return
	 */
	public int getNbDuelDraw() {
		return nbPlayed - nbWinPlayer1 - nbWinPlayer2;
	}
	
	/**
	 * Return the number of win for player. Return value nbWinPlayer1 or nbWinPlayer2.
	 * @param playerID
	 * @return
	 */
	public int getNbWinForPlayer(long playerID) {
		if (player1 != null && player1.getID() == playerID) {
			return nbWinPlayer1;
		}
		if (player2 != null && player2.getID() == playerID) {
			return nbWinPlayer2;
		}
		return 0;
	}
	
	/**
	 * Return the number of duel win by player1 for player ask
	 * @param playerAsk
	 * @return
	 */
	public int getNbWinForPlayer1(long playerAsk) {
		if (player1 != null && player1.getID() == playerAsk) {
			return nbWinPlayer1;
		}
		return nbWinPlayer2;
	}
	
	/**
	 * Return the number of duel win by player2 for player ask
	 * @param playerAsk
	 * @return
	 */
	public int getNbWinForPlayer2(long playerAsk) {
		if (player2 != null && player2.getID() == playerAsk) {
			return nbWinPlayer1;
		}
		return nbWinPlayer2;
	}
	
	/**
	 * increment the value of nb win for player and the value of nbPlayed
	 * @param playerWinner id of the winner or 0 if it is a draw
	 */
	public void incrementNbWinForPlayer(long playerWinner) {
		if (getPlayer1() != null && getPlayer1().getID() == playerWinner) {
			nbWinPlayer1++;
		} else if (getPlayer2() != null && getPlayer2().getID() == playerWinner) {
			nbWinPlayer2++;
		}
		// increment value of nb tournament played
		nbPlayed++;
	}
	
	/**
	 * Check if player with playerID is player1
	 * @param playerID
	 * @return
	 */
	public boolean isPlayer1(long playerID) {
		return (player1.getID() == playerID);
	}
	
	/**
	 * Check if player with playerID is player2
	 * @param playerID
	 * @return
	 */
	public boolean isPlayer2(long playerID) {
		return (player2.getID() == playerID);
	}
	
	/**
	 * Return the partner of the player for this duel
	 * @param playerID
	 * @return
	 */
	public Player getPartner(long playerID) {
		if (isPlayer1(playerID)) {
			return player2;
		}
		return player1;
	}

    public Player getPlayerWithID(long playerID) {
        if (isPlayer1(playerID)) {
            return player1;
        }
        if (isPlayer2(playerID)) {
            return player2;
        }
        return null;
    }

	/**
	 * Check if status is status_playing
	 * @return
	 */
	public boolean isPlaying() {
		return status == Constantes.PLAYER_DUEL_STATUS_PLAYING;
	}

	public long getDateReset() {
		return dateReset;
	}

	public void setDateReset(long dateReset) {
		this.dateReset = dateReset;
	}
	
	/**
	 * Check if status ivalue is REQUEST_PLAYER1 or REQUEST_PLAYER2
	 * @return
	 */
	public boolean isRequest() {
		return status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER1 || status == Constantes.PLAYER_DUEL_STATUS_REQUEST_PLAYER2;
	}
	
	/**
	 * If isRequest check if request is expired
	 * @return true if no request or request not expired, false if request and expired
	 */
	public boolean isRequestExpired() {
		if (isRequest()) {
            return (dateCreation + ContextManager.getDuelMgr().getDuelRequestDuration()) < System.currentTimeMillis();
        }
		return true;
	}

    public long getDateLastDuel() {
        return dateLastDuel;
    }

    public void setDateLastDuel(long dateLastDuel) {
        this.dateLastDuel = dateLastDuel;
    }
}
