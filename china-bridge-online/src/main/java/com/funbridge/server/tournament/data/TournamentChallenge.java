package com.funbridge.server.tournament.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.player.data.Player;

import javax.persistence.*;

@Entity
@Table(name="tournament_challenge")
@NamedQueries({
	@NamedQuery(name="tourChallenge.listAll", query="select tc from TournamentChallenge tc"),
	@NamedQuery(name="tourChallenge.listForPlayer", query="select tc from TournamentChallenge tc where creator.ID=:plaID or partner.ID=:plaID"),
	@NamedQuery(name="tourChallenge.deleteForPlayer", query="delete from TournamentChallenge tc where tc.creator.ID=:plaID or tc.partner.ID=:plaID"),
	@NamedQuery(name="tourChallenge.getForCreatorAndPartner", query="select tc from TournamentChallenge tc where creator.ID=:creator and partner.ID=:partner"),
	@NamedQuery(name="tourChallenge.getNotExpiredForCreatorAndPartner", query="select tc from TournamentChallenge tc where creator.ID=:creator and partner.ID=:partner and dateExpiration>:currentTS order by dateCreation desc"),
	@NamedQuery(name="tourChallenge.getNotExpiredForPlayers", query="select tc from TournamentChallenge tc where ((creator.ID=:pla1 and partner.ID=:pla2) or (creator.ID=:pla2 and partner.ID=:pla1)) and dateExpiration>:currentTS order by dateCreation desc"),
	@NamedQuery(name="tourChallenge.getNotExpiredForPartner", query="select tc from TournamentChallenge tc where partner.ID=:partner and dateExpiration>:currentTS order by dateCreation desc"),
	@NamedQuery(name="tourChallenge.getForTable", query="select tc from TournamentChallenge tc where table.ID=:tableID")
})
public class TournamentChallenge {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@Column(name="tournament_type")
	private long tournamentType;

	@ManyToOne
	@JoinColumn(name="pla_creator")
	private Player creator;
	
	@ManyToOne
	@JoinColumn(name="pla_partner")
	private Player partner;
	
	@Column(name="date_creation")
	private long dateCreation;
	
	@Column(name="date_expiration")
	private long dateExpiration;
	
	@Column(name="settings")
	private String settings;
	
	@Column(name="status")
	private int status;
	
	@ManyToOne
	@JoinColumn(name="tournament_table")
	private TournamentTable2 table = null;
	
	@Transient
	private long dateLastStatusChange = 0;
	
	public String toString() {
		return "ID="+ID+" - creator="+(creator!=null?creator.getID():"null")+" - partner="+(partner!=null?partner.getID():"null")+" - status="+status+" - expiration="+Constantes.timestamp2StringDateHour(dateExpiration);
	}
	
	public long getID() {
		return ID;
	}

	public void setID(long iD) {
		ID = iD;
	}

	public Player getCreator() {
		return creator;
	}

	public void setCreator(Player creator) {
		this.creator = creator;
	}

	public Player getPartner() {
		return partner;
	}

	public void setPartner(Player partner) {
		this.partner = partner;
	}

	public long getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(long dateCreation) {
		this.dateCreation = dateCreation;
	}

	public long getDateExpiration() {
		return dateExpiration;
	}

	public void setDateExpiration(long dateExpiration) {
		this.dateExpiration = dateExpiration;
	}

	public String getSettings() {
		return settings;
	}

	public void setSettings(String settings) {
		this.settings = settings;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
		// update time of last status changed
		this.dateLastStatusChange = System.currentTimeMillis();
	}
	
	/**
	 * Check if date expiration < current time
	 * @return
	 */
	public boolean isExpired() {
		return dateExpiration < System.currentTimeMillis();
	}
	
	/**
	 * Check if challenge is ended : status end or init & expired or expired
	 * @return
	 */
	public boolean isEnded() {
		if (getStatus() == Constantes.TOURNAMENT_CHALLENGE_STATUS_END) {
			return true;
		}
		if (getStatus() == Constantes.TOURNAMENT_CHALLENGE_STATUS_INIT && isExpired()) {
			return true;
		}
		return isExpired();
//		return false;
	}

	public TournamentTable2 getTable() {
		return table;
	}

	public void setTable(TournamentTable2 table) {
		this.table = table;
	}

	public long getDateLastStatusChange() {
		return dateLastStatusChange;
	}

	public void setDateLastStatusChange(long dateLastStatusChange) {
		this.dateLastStatusChange = dateLastStatusChange;
	}
}
