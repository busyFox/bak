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

@Entity
@Table(name="player_link")
@NamedQueries({
//	@NamedQuery(name="playerLink.listLinkForOwner", query="select pl.plaLinked.ID, pl.plaLinked.pseudo, pl.plaLinked.serie, pl.plaLinked.countryCode from PlayerLink pl where pl.plaOwner.ID=:ownerID and pl.typeMask=:type order by pl.plaOwner.pseudo"),
//	@NamedQuery(name="playerLink.countLinkForOwner", query="select count(pl) from PlayerLink pl where pl.plaOwner.ID=:ownerID and pl.typeMask=:type"),
	@NamedQuery(name="playerLink.selectBetweenPlayer", query="select pl from PlayerLink pl where (pl.player1.ID=:plaID1 and pl.player2.ID=:plaID2) or (pl.player1.ID=:plaID2 and pl.player2.ID=:plaID1)"),
	@NamedQuery(name="playerLink.deleteLink", query="delete from PlayerLink pl where pl.ID=:plID")
})
public class PlayerLink implements PlayerLinkGroup{
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
	
	@Column(name="date_link")
	private long dateLink;
	
	@Column(name="type_mask1")
	private int typeMask1;
	
	@Column(name="type_mask2")
	private int typeMask2;
	
	@Column(name="date_msg_reset1")
	private long dateMessageReset1;
	
	@Column(name="date_msg_reset2")
	private long dateMessageReset2;
	
	@Column(name="message")
	private String message;

	@Column(name="date_last_message")
	private long dateLastMessage = 0;

//	@Column(name="options_flag1")
//	private int optionsFlag1 = 0;
//	
//	@Column(name="options_flag2")
//	private int optionsFlag2 = 0;
	
	public String toString() {
		return "ID="+ID+" - player1="+player1.getID()+" - player2="+player2.getID()+" - typeMask1="+typeMask1+" - typeMask2="+typeMask2+" - message="+message;
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

	public long getDateLink() {
		return dateLink;
	}

	public void setDateLink(long dateLink) {
		this.dateLink = dateLink;
	}

	public int getTypeMask1() {
		return typeMask1;
	}

	public void setTypeMask1(int val) {
		this.typeMask1 = val;
	}

	public int getTypeMask2() {
		return typeMask2;
	}

	public void setTypeMask2(int val) {
		this.typeMask2 = val;
	}
	
	public long getDateMessageReset1() {
		return dateMessageReset1;
	}

	public void setDateMessageReset1(long dateMessageReset) {
		this.dateMessageReset1 = dateMessageReset;
	}
	
	public long getDateMessageReset2() {
		return dateMessageReset2;
	}

	public void setDateMessageReset2(long dateMessageReset) {
		this.dateMessageReset2 = dateMessageReset;
	}

	public long getDateLastMessage() {
		return dateLastMessage;
	}

	public void setDateLastMessage(long dateLastMessage) {
		this.dateLastMessage = dateLastMessage;
	}

	/**
	 * Check if mask is present in typeMask1
	 * @param mask
	 * @return
	 */
	private boolean hasTypeMask1(int mask) {
		return ((typeMask1 & mask) == mask);
	}
	
	public boolean hasTypeMask(long plaID, int mask) {
		if (plaID == player1.getID()) {
			return hasTypeMask1(mask);
		} else if (plaID == player2.getID()) {
			return hasTypeMask2(mask);
		}
		return false;
	}
	
	/**
	 * Remove a mask value from typeMask1. Return true if value of typeMask1 has changed
	 * @param mask
	 * @return true if typeMask1 changed after removing value
	 */
	private boolean removeTypeMask1(int mask) {
		if ((typeMask1 & mask) == mask) {
			typeMask1 = typeMask1 & (~mask);
			return true;
		}
		return false;
	}
	
	public boolean removeTypeMask(long plaID, int mask) {
		if (plaID == player1.getID()) {
			return removeTypeMask1(mask);
		} else if (plaID == player2.getID()) {
			return removeTypeMask2(mask);
		}
		return false;
	}
	
	public boolean removeTypeMask(int mask) {
		return removeTypeMask1(mask) | removeTypeMask2(mask);
	}
	
	/**
	 * Add mask value to typeMask1. Return true if value of typeMask1 has changed.
	 * @param mask
	 * @return true if typeMask1 changed after adding value
	 */
	private boolean addTypeMask1(int mask) {
		if ((typeMask1 & mask) != mask) {
			typeMask1 = typeMask1 | mask;
			return true;
		}
		return false;
	}
	
	public boolean addTypeMask(long plaID, int mask) {
		if (plaID == player1.getID()) {
			return addTypeMask1(mask);
		} else if (plaID == player2.getID()) {
			return addTypeMask2(mask);
		}
		return false;
	}
	
	public boolean addTypeMask(int mask) {
		return addTypeMask1(mask) | addTypeMask2(mask);
	}
	
	/**
	 * Check if mask is present in typeMask2
	 * @param mask
	 * @return
	 */
	private boolean hasTypeMask2(int mask) {
		return ((typeMask2 & mask) == mask);
	}
	
	/**
	 * Remove a mask value from typeMask2. Return true if value of typeMask2 has changed
	 * @param mask
	 * @return true if typeMask2 changed after removing value
	 */
	private boolean removeTypeMask2(int mask) {
		if ((typeMask2 & mask) == mask) {
			typeMask2 = typeMask2 & (~mask);
			return true;
		}
		return false;
	}
	
	/**
	 * Add mask value to typeMask2. Return true if value of typeMask2 has changed.
	 * @param mask
	 * @return true if typeMask2 changed after adding value
	 */
	private boolean addTypeMask2(int mask) {
		if ((typeMask2 & mask) != mask) {
			typeMask2 = typeMask2 | mask;
			return true;
		}
		return false;
	}
	
	public int getLinkMaskForPlayer(long plaID) {
		if (player1.getID() == plaID) {
			return getLinkMaskFor1();
		}
		if (player2.getID() == plaID) {
			return getLinkMaskFor2();
		}
		return 0;
	}
	
	public int getLinkMaskFor1() {
		int value = typeMask1;
		if (hasTypeMask1(Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING)) {
			value = value | Constantes.PLAYER_LINK_TYPE_WAY;
		}
		if (hasTypeMask2(Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING)) {
			value = value | Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING;
		}
		if (hasTypeMask2(Constantes.PLAYER_LINK_TYPE_FRIEND)) {
			value = value | Constantes.PLAYER_LINK_TYPE_FRIEND;
		}
		return value;
	}
	
	public int getLinkMaskFor2() {
		int value = typeMask2;
		if (hasTypeMask2(Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING)) {
			value = value | Constantes.PLAYER_LINK_TYPE_WAY;
		}
		if (hasTypeMask1(Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING)) {
			value = value | Constantes.PLAYER_LINK_TYPE_FRIEND_PENDING;
		}
		if (hasTypeMask1(Constantes.PLAYER_LINK_TYPE_FRIEND)) {
			value = value | Constantes.PLAYER_LINK_TYPE_FRIEND;
		}
		return value;
	}
	
	public boolean isLinkFriend() {
		return hasTypeMask1(Constantes.PLAYER_LINK_TYPE_FRIEND);
	}

	public boolean hasBlocked() {
		return hasTypeMask1(Constantes.PLAYER_LINK_TYPE_BLOCKED) || hasTypeMask2(Constantes.PLAYER_LINK_TYPE_BLOCKED);
	}

	public boolean hasBlocked(long plaID) {
		return hasTypeMask(plaID, Constantes.PLAYER_LINK_TYPE_BLOCKED);
	}
	
	public void setDateMessageReset(long dateReset, long playerID) {
		if (playerID == player1.getID()) {
			this.dateMessageReset1 = dateReset;
		} else if (playerID == player2.getID()){
			this.dateMessageReset2 = dateReset;
		}
	}

	public long getDateMessageReset(long playerID) {
		if (playerID == player1.getID()) {
			return this.dateMessageReset1;
		} else if (playerID == player2.getID()){
			return this.dateMessageReset2;
		}
		return 0;
	}
	
	public String getMessage() {
		return message;
	}

	public void setMessage(String value) {
		if (value != null) {
			if (value.length() > Constantes.PLAYER_LINK_MESSAGE_MAX_LENGTH) {
				value = value.substring(0, Constantes.PLAYER_LINK_MESSAGE_MAX_LENGTH);
			}
			this.message = value;
			this.setDateLastMessage(System.currentTimeMillis());
		} else {
			this.message = value;
		}
	}

//	public int getOptionsFlag1() {
//		return optionsFlag1;
//	}
//
//	public void setOptionsFlag1(int optionsFlag1) {
//		this.optionsFlag1 = optionsFlag1;
//	}
//
//	public int getOptionsFlag2() {
//		return optionsFlag2;
//	}
//
//	public void setOptionsFlag2(int optionsFlag2) {
//		this.optionsFlag2 = optionsFlag2;
//	}
//	
//	/**
//	 * Check if options flag for player included this flag
//	 * @param playerID
//	 * @param flag
//	 * @return
//	 */
//	public boolean hasOptionsFlag(long playerID, int flag) {
//		int optionPlayer = 0;
//		if (player1 != null && player1.getID() == playerID) {
//			optionPlayer = optionsFlag1;
//		} else if (player2 != null && player2.getID() == playerID) {
//			optionPlayer = optionsFlag2;
//		}
//		return ((optionPlayer & flag) == flag);
//	}
//	
//	/**
//	 * Modify the value of optionsFlag for player by adding this flag
//	 * @param playerID
//	 * @param flag
//	 */
//	public void addOptionsFlag(long playerID, int flag) {
//		if (player1 != null && player1.getID() == playerID) {
//			optionsFlag1 = optionsFlag1 | flag;
//		} else if (player2 != null && player2.getID() == playerID) {
//			optionsFlag2 = optionsFlag2 | flag;
//		}
//	}
//	
//	/**
//	 * Modify the value of optionsFlag for player by removing this flag
//	 * @param playerID
//	 * @param flag
//	 */
//	public void removeOptionsFlag(long playerID, int flag) {
//		if (player1 != null && player1.getID() == playerID) {
//			optionsFlag1 = optionsFlag1 & (~flag);
//		} else if (player2 != null && player2.getID() == playerID) {
//			optionsFlag2 = optionsFlag2 & (~flag);
//		}
//	}
}
