package com.funbridge.server.tournament.data;

import javax.persistence.CascadeType;
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

@Entity
@Table(name="tournament_deal")
@NamedQueries({
	@NamedQuery(name="tournamentDeal.selectForTournamentAndIndex", query="select deal from TournamentDeal deal where deal.tournament.ID=:tourID and deal.index=:dealIndex"),
	@NamedQuery(name="tournamentDeal.selectForTournament", query="select deal from TournamentDeal deal where deal.tournament.ID=:tourID order by deal.index asc"),
	@NamedQuery(name="tournamentDeal.deleteForTournament", query="delete from TournamentDeal deal where deal.tournament.ID=:tourID")
})
public class TournamentDeal {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@Column(name="idx_tour", nullable=false)
	private int index;
	
	@ManyToOne
	@JoinColumn(name="tour_id")
	private Tournament tournament;
	
	@ManyToOne(cascade={CascadeType.REMOVE})
	@JoinColumn(name="distrib_id")
	private TournamentDistribution distribution;
	
	@Column(name="nb_player")
	private int nbPlayer = 0;
	
	@Column(name="score_average")
	private double scoreAverage = 0;
	
	public long getID() {
		return ID;
	}
	public void setID(long iD) {
		ID = iD;
	}
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public Tournament getTournament() {
		return tournament;
	}
	public void setTournament(Tournament tournament) {
		this.tournament = tournament;
	}
	public TournamentDistribution getDistribution() {
		return distribution;
	}
	public void setDistribution(TournamentDistribution distribution) {
		this.distribution = distribution;
	}
	public int getNbPlayer() {
		return nbPlayer;
	}
	public void setNbPlayer(int nbPlayer) {
		this.nbPlayer = nbPlayer;
	}
	public double getScoreAverage() {
		return scoreAverage;
	}
	public void setScoreAverage(double scoreAverage) {
		this.scoreAverage = scoreAverage;
	}
	public String toString() {
		return "[ID="+ID+" - Tournament="+tournament.toString()+" - index="+index+" - distribution="+distribution.toString()+"]";
	}
}
